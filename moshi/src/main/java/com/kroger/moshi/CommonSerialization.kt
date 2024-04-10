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
package com.kroger.moshi

import com.kroger.cache.internal.CacheEntry
import com.squareup.moshi.JsonAdapter

public fun <K, V> flattenCacheEntry(cacheEntry: CacheEntry<K, V>?, keyAdapter: JsonAdapter<K>, valueAdapter: JsonAdapter<V>): String = listOf(
    keyAdapter.toJson(cacheEntry?.key),
    valueAdapter.toJson(cacheEntry?.value),
    cacheEntry?.creationDate.toString(),
    cacheEntry?.lastAccessDate.toString(),
).joinToString(",")

public fun <K, V> hydrateCacheEntry(rawJson: String, keyAdapter: JsonAdapter<K>, valueAdapter: JsonAdapter<V>): CacheEntry<K, V> {
    val items = rawJson.split(",")
    require(items.size == 4)

    val key = keyAdapter.fromJson(items[0])
    val value = valueAdapter.fromJsonValue(items[1])
    val creationDate = items[2].toLong()
    val lastAccessDate = items[3].toLong()

    require(key != null && value != null && creationDate != 0L && lastAccessDate != 0L)

    return CacheEntry(key, value, creationDate, lastAccessDate)
}
