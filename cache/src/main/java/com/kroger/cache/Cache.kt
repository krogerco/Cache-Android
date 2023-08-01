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

/**
 * A cache with operations to add, update, retrieve, and remove entries.
 */
public interface Cache<K, V> {
    /**
     * @return the value corresponding with the given [key], or null if the key is not present or the value is expired.
     */
    public suspend fun get(key: K): V?

    /**
     * Associates the given [key] with the given [value].
     * If [key] is already in the [Cache] the value will be overwritten by [value].
     */
    public suspend fun put(key: K, value: V)

    /**
     * Each pair will be inserted into the cache associating the [Pair.first] element with [Pair.second].
     * If [Pair.first] is already in the [Cache] the value will be overwritten by [Pair.second].
     *
     * @param pairs the key value pairs to insert into the cache.
     */
    public suspend fun putAll(pairs: Iterable<Pair<K, V>>)

    /**
     * Removes the entry for [key] from the [Cache].
     */
    public suspend fun remove(key: K)

    /**
     * Removes all entries from the [Cache].
     */
    public suspend fun clear()
}
