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
package com.kroger.cache.kotlinx

import com.kroger.cache.internal.CacheEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wrapper for [CacheEntry] to enable KotlinX Serialization
 * @property key the key for this entry
 * @property value the value for this entry
 * @property creationDate when this entry was created in milliseconds since epoch
 * @property lastAccessDate when this entry was last accessed in milliseconds since epoch
 */
@Serializable
@SerialName("CacheEntry")
public data class KotlinCacheEntry<K, V>(
    val key: K,
    val value: V,
    val creationDate: Long,
    val lastAccessDate: Long,
) {

    public fun toCacheEntry(): CacheEntry<K, V> = CacheEntry(key, value, creationDate, lastAccessDate)

    public companion object {
        public fun <K, V> build(cacheEntry: CacheEntry<K, V>): KotlinCacheEntry<K, V> =
            KotlinCacheEntry(
                cacheEntry.key,
                cacheEntry.value,
                cacheEntry.creationDate,
                cacheEntry.lastAccessDate,
            )
    }
}
