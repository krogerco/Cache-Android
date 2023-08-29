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

/**
 * A wrapper around a [Map] to work as an in-memory cache.
 * This class is not thread-safe and callers must ensure access is synchronized.
 *
 * @param initialCapacity the initial capacity to use when creatig the [Map].
 */
internal class MemoryCache<K, V>(
    initialCapacity: Int,
    accessOrder: Boolean,
) : Cache<K, V> {
    private val cacheMap: MutableMap<K, V> = LinkedHashMap(
        initialCapacity,
        0.75f,
        accessOrder,
    )

    constructor(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY) :
        this(initialCapacity, false)

    override suspend fun get(key: K): V? {
        return cacheMap[key]
    }

    override suspend fun put(key: K, value: V) {
        cacheMap[key] = value
    }

    override suspend fun putAll(pairs: Iterable<Pair<K, V>>) {
        cacheMap.putAll(pairs)
    }

    override suspend fun remove(key: K) {
        cacheMap.remove(key)
    }

    override suspend fun clear() {
        cacheMap.clear()
    }

    /**
     * @return all entries in the [MemoryCache]
     */
    internal fun getAll(): MutableCollection<MutableMap.MutableEntry<K, V>> =
        cacheMap.entries

    /**
     * @return the current number of entries in [cacheMap]
     */
    internal fun size(): Int = cacheMap.size

    companion object {
        internal const val DEFAULT_INITIAL_CAPACITY: Int = 16
    }
}
