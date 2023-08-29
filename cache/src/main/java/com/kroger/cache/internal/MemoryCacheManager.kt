/**
 * MIT License
 *
 * Copyright (c) 2023 The Kroger Co. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.kroger.cache.internal

import com.kroger.cache.Cache
import com.kroger.cache.CachePolicy
import com.kroger.cache.SnapshotPersistentCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

internal typealias TimeProvider = () -> Long

internal class MemoryCacheManager<K, V>(
    private val snapshotPersistentCache: SnapshotPersistentCache<List<CacheEntry<K, V>>>?,
    private val cachePolicy: CachePolicy,
    private val timeProvider: TimeProvider,
    internal val coroutineScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    saveFrequency: Duration,
) : Cache<K, V> {
    private lateinit var memoryCache: MemoryCache<K, CacheEntry<K, V>>
    private val mutex = Mutex()
    private val initializerJob: Job
    private val cacheChanges = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val temporalExpirationMillisFunc: ((CacheEntry<K, V>) -> Long)?
    private val temporalLimitMillis: Long

    init {
        when {
            cachePolicy.hasTtiPolicy -> {
                temporalExpirationMillisFunc = CacheEntry<K, V>::lastAccessDate::get
                temporalLimitMillis = cachePolicy.entryTti.inWholeMilliseconds
            }
            cachePolicy.hasTtlPolicy -> {
                temporalExpirationMillisFunc = CacheEntry<K, V>::creationDate::get
                temporalLimitMillis = cachePolicy.entryTtl.inWholeMilliseconds
            }
            else -> {
                temporalExpirationMillisFunc = null
                temporalLimitMillis = CachePolicy.DEFAULT_DURATION_POLICY.inWholeMilliseconds
            }
        }

        initializerJob = coroutineScope.launch(dispatcher) {
            val sortByProperty = cachePolicy.sortByProperty<K, V>()

            // the new cache policy may require a new sort order
            val allEntries = snapshotPersistentCache?.read().orEmpty().sortedBy(sortByProperty)
            val initialCapacity = maxOf(allEntries.size, MemoryCache.DEFAULT_INITIAL_CAPACITY)
            memoryCache = MemoryCache(initialCapacity, accessOrder = cachePolicy.hasTtiPolicy)
            allEntries.forEach { memoryCache.put(it.key, it) }

            if (snapshotPersistentCache != null) {
                cacheChanges
                    .onEach {
                        delay(saveFrequency)
                        updateSnapshotPersistentCache()
                    }
                    .onCompletion {
                        updateSnapshotPersistentCache()
                    }
                    .flowOn(dispatcher)
                    .launchIn(coroutineScope)
            }

            trimToSize()
        }
    }

    override suspend fun get(key: K): V? = mutex.withLock {
        initializerJob.join()
        memoryCache.get(key)?.let { cachedEntry ->
            val lastAccessDate = timeProvider()
            if (isEntryExpired(cachedEntry, lastAccessDate)) {
                handleRemove(key)
                null
            } else {
                val newEntry = cachedEntry.copy(lastAccessDate = lastAccessDate)
                memoryCache.put(key, newEntry)
                cacheChanges.tryEmit(Unit)
                newEntry.value
            }
        }
    }

    override suspend fun put(key: K, value: V): Unit = mutex.withLock {
        initializerJob.join()
        insertOrUpdate(key, value)
        cacheChanges.tryEmit(Unit)
    }

    override suspend fun putAll(pairs: Iterable<Pair<K, V>>): Unit = mutex.withLock {
        initializerJob.join()
        pairs.forEach { (key, value) ->
            insertOrUpdate(key, value)
        }
        cacheChanges.tryEmit(Unit)
    }

    private suspend fun insertOrUpdate(key: K, value: V) {
        // An entry's insertion order is not affected by updating a value for a key that already exists in a
        // LinkedHashMap. Removing the entry first ensures an update moves an entry to the back of the LRU list.
        memoryCache.remove(key)
        val creationDate = timeProvider()
        val cachedEntry = CacheEntry(key, value, creationDate, lastAccessDate = creationDate)
        memoryCache.put(key, cachedEntry)
        trimToSize()
    }

    override suspend fun remove(key: K): Unit = mutex.withLock {
        initializerJob.join()
        handleRemove(key)
    }

    private suspend fun handleRemove(key: K) {
        memoryCache.remove(key)
        cacheChanges.tryEmit(Unit)
    }

    override suspend fun clear(): Unit = mutex.withLock {
        initializerJob.join()
        memoryCache.clear()
        cacheChanges.tryEmit(Unit)
    }

    /**
     * Applies the temporal policy from the [cachePolicy] if one exists to remove expired entries.
     */
    internal suspend fun trimMemory(): Unit = mutex.withLock {
        initializerJob.join()
        applyTemporalPolicy()
        cacheChanges.tryEmit(Unit)
    }

    /**
     * Ensures [CachePolicy.maxSize] is enforced if a max size is set.
     * Before force removing entries from the head of the cache to get below the max size,
     * [applyTemporalPolicy] is called first to remove any expired entries.
     */
    private fun trimToSize() {
        if (cachePolicy.hasMaxSizePolicy.not() || memoryCache.size() <= cachePolicy.maxSize) {
            return
        }
        applyTemporalPolicy()
        val entryCountOverLimit = memoryCache.size() - cachePolicy.maxSize
        if (entryCountOverLimit > 0) {
            memoryCache.getAll().iterator().drop(entryCountOverLimit)
        }
        cacheChanges.tryEmit(Unit)
    }

    /**
     * Removes entries from the head of the cache if they are expired according to [isEntryExpired].
     */
    private fun applyTemporalPolicy() {
        val currentTimeMillis = timeProvider()
        memoryCache.getAll().iterator()
            .dropWhile { (_, cacheEntry) ->
                isEntryExpired(cacheEntry, currentTimeMillis)
            }
    }

    private fun isEntryExpired(cacheEntry: CacheEntry<K, V>, currentTimeMillis: Long): Boolean {
        val temporalFunc = temporalExpirationMillisFunc ?: return false
        val expirationMillis = temporalFunc(cacheEntry) + temporalLimitMillis
        return expirationMillis < currentTimeMillis
    }

    private suspend fun updateSnapshotPersistentCache() {
        if (snapshotPersistentCache == null) {
            return
        }

        val cacheEntries = mutex.withLock {
            memoryCache.getAll().map { it.value }
        }

        snapshotPersistentCache.save(cacheEntries)
    }
}

private fun <T> MutableIterator<T>.dropWhile(predicate: (T) -> Boolean) {
    while (hasNext()) {
        val nextVal = next()
        if (predicate(nextVal)) {
            remove()
        } else {
            break
        }
    }
}

private fun <T> MutableIterator<T>.drop(n: Int) {
    var count = 0
    while (hasNext() && count < n) {
        next()
        remove()
        count++
    }
}

private fun <K, V> CachePolicy.sortByProperty(): (CacheEntry<K, V>) -> Long =
    if (hasTtiPolicy) {
        CacheEntry<K, V>::lastAccessDate::get
    } else {
        CacheEntry<K, V>::creationDate::get
    }
