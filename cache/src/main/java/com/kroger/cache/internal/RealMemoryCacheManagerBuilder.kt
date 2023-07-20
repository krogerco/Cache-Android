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
import com.kroger.cache.MemoryCacheManagerBuilder
import com.kroger.cache.MemoryLevelNotifier
import com.kroger.cache.SnapshotPersistentCache
import com.kroger.telemetry.Telemeter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

internal class RealMemoryCacheManagerBuilder<K, V>(
    private val coroutineScope: CoroutineScope,
    private val snapshotPersistentCache: SnapshotPersistentCache<List<CacheEntry<K, V>>>?,
    private val timeProvider: TimeProvider,
) : MemoryCacheManagerBuilder<K, V> {
    private var saveFrequency: Duration = DEFAULT_SAVE_FREQUENCY
    private var cachePolicy: CachePolicy = DEFAULT_CACHE_POLICY
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var memoryLevelNotifier: MemoryLevelNotifier? = null
    private var telemeter: Telemeter? = null

    override fun cachePolicy(cachePolicy: CachePolicy): MemoryCacheManagerBuilder<K, V> =
        apply {
            this.cachePolicy = cachePolicy
        }

    override fun saveFrequency(saveFrequency: Duration): MemoryCacheManagerBuilder<K, V> =
        apply {
            require(saveFrequency.inWholeMilliseconds >= 0) {
                "saveFrequency must be >= 0: saveFrequency=${saveFrequency.inWholeMilliseconds} milliseconds"
            }
            this.saveFrequency = saveFrequency
        }

    override fun memoryLevelNotifier(memoryLevelNotifier: MemoryLevelNotifier?): MemoryCacheManagerBuilder<K, V> =
        apply {
            this.memoryLevelNotifier = memoryLevelNotifier
        }

    override fun telemeter(telemeter: Telemeter): MemoryCacheManagerBuilder<K, V> =
        apply {
            this.telemeter = telemeter
        }

    internal fun dispatcher(dispatcher: CoroutineDispatcher): MemoryCacheManagerBuilder<K, V> =
        apply {
            this.dispatcher = dispatcher
        }

    override suspend fun build(): Cache<K, V> = withContext(dispatcher) {
        val sortByProperty = cachePolicy.sortByProperty<K, V>()

        // the new cache policy may require a new sort order
        val allEntries = snapshotPersistentCache?.read().orEmpty().sortedBy(sortByProperty)
        val initialCapacity = maxOf(allEntries.size, MemoryCache.DEFAULT_INITIAL_CAPACITY)
        val memoryCache = MemoryCache<K, CacheEntry<K, V>>(initialCapacity, accessOrder = cachePolicy.hasTtiPolicy)
        allEntries.forEach { memoryCache.put(it.key, it) }

        val memoryCacheManager = MemoryCacheManager(
            memoryCache,
            snapshotPersistentCache,
            cachePolicy = cachePolicy,
            coroutineScope = coroutineScope,
            saveFrequency = saveFrequency,
            timeProvider = timeProvider,
            dispatcher = dispatcher,
        )

        return@withContext memoryLevelNotifier?.let {
            MemoryLevelCacheManagerDecorator(memoryCacheManager, it, telemeter)
        } ?: memoryCacheManager
    }

    companion object {
        val DEFAULT_SAVE_FREQUENCY: Duration = 5.seconds

        private val DEFAULT_ENTRY_TTL: Duration = 24.hours
        private const val DEFAULT_MAX_SIZE: Int = 100
        private val DEFAULT_CACHE_POLICY = CachePolicy.builder()
            .entryTtl(DEFAULT_ENTRY_TTL)
            .maxSize(DEFAULT_MAX_SIZE)
            .build()
    }
}

private fun <K, V> CachePolicy.sortByProperty(): (CacheEntry<K, V>) -> Long =
    if (hasTtiPolicy) {
        CacheEntry<K, V>::lastAccessDate::get
    } else {
        CacheEntry<K, V>::creationDate::get
    }
