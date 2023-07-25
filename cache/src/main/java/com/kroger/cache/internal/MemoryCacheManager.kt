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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration

internal typealias TimeProvider = () -> Long

internal class MemoryCacheManager<K, V>(
    private val memoryCache: MemoryCache<K, CacheEntry<K, V>>,
    private val snapshotPersistentCache: SnapshotPersistentCache<List<CacheEntry<K, V>>>?,
    private val cachePolicy: CachePolicy,
    private val timeProvider: TimeProvider,
    internal val coroutineScope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    saveFrequency: Duration,
) : Cache<K, V> {
    private val lock = Object()
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

    override fun get(key: K): V? = synchronized(lock) {
        memoryCache.get(key)?.let { cachedEntry ->
            val lastAccessDate = timeProvider()
            if (isEntryExpired(cachedEntry, lastAccessDate)) {
                remove(key)
                null
            } else {
                val newEntry = cachedEntry.copy(lastAccessDate = lastAccessDate)
                memoryCache.put(key, newEntry)
                cacheChanges.tryEmit(Unit)
                newEntry.value
            }
        }
    }

    override fun put(key: K, value: V): Unit = synchronized(lock) {
        insertOrUpdate(key, value)
        cacheChanges.tryEmit(Unit)
    }

    override fun putAll(pairs: Iterable<Pair<K, V>>): Unit = synchronized(lock) {
        pairs.forEach { (key, value) ->
            insertOrUpdate(key, value)
        }
        cacheChanges.tryEmit(Unit)
    }

    private fun insertOrUpdate(key: K, value: V): Unit = synchronized(lock) {
        // An entry's insertion order is not affected by updating a value for a key that already exists in a
        // LinkedHashMap. Removing the entry first ensures an update moves an entry to the back of the LRU list.
        memoryCache.remove(key)
        val creationDate = timeProvider()
        val cachedEntry = CacheEntry(key, value, creationDate, lastAccessDate = creationDate)
        memoryCache.put(key, cachedEntry)
        trimToSize()
    }

    override fun remove(key: K): Unit = synchronized(lock) {
        memoryCache.remove(key)
        cacheChanges.tryEmit(Unit)
    }

    override fun clear(): Unit = synchronized(lock) {
        memoryCache.clear()
        cacheChanges.tryEmit(Unit)
    }

    /**
     * Applies the temporal policy from the [cachePolicy] if one exists to remove expired entries.
     */
    internal fun trimMemory(): Unit = synchronized(lock) {
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

        val cacheEntries = synchronized(lock) {
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
