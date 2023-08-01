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
package com.kroger.cache

import com.kroger.cache.internal.CacheEntry
import com.kroger.cache.internal.RealMemoryCacheManagerBuilder
import com.kroger.telemetry.Telemeter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

/**
 * Builder for a thread safe [Cache] that internally manages a [MemoryCache][com.kroger.cache.internal.MemoryCache].
 * The cache will periodically persist its data to a [SnapshotPersistentCache] and will enforce the given [CachePolicy].
 * Once the [Cache] is no longer in use its [coroutineScope] should be cancelled.
 */
public interface MemoryCacheManagerBuilder<K, V> {
    public fun build(): Cache<K, V>

    /**
     * The [CachePolicy] to apply to the [MemoryCache][com.kroger.cache.internal.MemoryCache] and its entries.
     */
    public fun cachePolicy(cachePolicy: CachePolicy): MemoryCacheManagerBuilder<K, V>

    /**
     * Controls how soon a snapshot of the [MemoryCache][com.kroger.cache.internal.MemoryCache] is persisted after a change to the [Cache].
     * The [Duration] specified by [saveFrequency] will elapse between each persisted snapshot. This is in addition to the time
     * it takes to persist the snapshot itself.
     *
     * NOTE: Retrieving an entry is considered a change because [CacheEntry.lastAccessDate] is updated.
     */
    public fun saveFrequency(saveFrequency: Duration): MemoryCacheManagerBuilder<K, V>

    /**
     * Allows the cache to respond to low memory and critical memory notifications
     * according to the callback functions exposed on [MemoryLevelNotifier.memoryLevelCallbacks].
     */
    public fun memoryLevelNotifier(memoryLevelNotifier: MemoryLevelNotifier?): MemoryCacheManagerBuilder<K, V>

    /**
     * The [Telemeter] to use for logging internal events and errors.
     * Defaults to null (no logging).
     */
    public fun telemeter(telemeter: Telemeter): MemoryCacheManagerBuilder<K, V>

    /**
     * The [CoroutineDispatcher] to use when reading and writing to the [SnapshotPersistentCache].
     * Defaults to [Dispatchers.IO][kotlinx.coroutines.Dispatchers.IO].
     */
    public fun dispatcher(dispatcher: CoroutineDispatcher): MemoryCacheManagerBuilder<K, V>

    /**
     * It is important that the provided [coroutineScope] live as long as the
     * [MemoryCache][com.kroger.cache.internal.MemoryCache] is in use. Once the [coroutineScope]
     * is cancelled no further changes to the [Cache] will be saved to the [snapshotPersistentCache].
     *
     * @param coroutineScope scope to use when periodically updating the [snapshotPersistentCache].
     */
    public fun coroutineScope(coroutineScope: CoroutineScope): MemoryCacheManagerBuilder<K, V>

    /**
     * The [SnapshotPersistentCache] where entries are periodically saved. If a [SnapshotPersistentCache]
     * is provided a [coroutineScope] must be provided as well.
     */
    public fun snapshotPersistentCache(snapshotPersistentCache: SnapshotPersistentCache<List<CacheEntry<K, V>>>): MemoryCacheManagerBuilder<K, V>

    public companion object {
        /**
         * Creates a new [MemoryCacheManagerBuilder].
         */
        public operator fun <K, V> invoke(): MemoryCacheManagerBuilder<K, V> =
            RealMemoryCacheManagerBuilder {
                System.currentTimeMillis()
            }
    }
}
