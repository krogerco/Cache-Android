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

import com.kroger.cache.CacheSerializer
import com.kroger.cache.internal.CacheEntry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * [KSerializer] instance to map a list of [CacheEntry] values via kotlinx serialization
 */
public class KotlinCacheListSerializer<K, V>(
    private val formatter: StringFormat = Json,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
) : CacheSerializer<List<CacheEntry<K, V>>> {
    private val listSerializer = ListSerializer(KotlinCacheEntrySerializer(keySerializer, valueSerializer))

    override fun decodeFromString(bytes: ByteArray?): List<CacheEntry<K, V>>? =
        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            formatter.decodeFromString(listSerializer, bytes.decodeToString())
        }

    override fun toByteArray(data: List<CacheEntry<K, V>>?): ByteArray = if (data == null) {
        ByteArray(0)
    } else {
        formatter.encodeToString(listSerializer, data).encodeToByteArray()
    }
}
