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
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import javax.inject.Inject

/**
 * An implementation of [JsonAdapter] for handling a list of [CacheEntry] objects
 *
 * @property cacheEntrySerializer [CacheEntrySerializer] for serializing individual [CacheEntry] objects
 */
public class CacheEntryListSerializer<K, V> @Inject constructor(private val cacheEntrySerializer: CacheEntrySerializer<K, V>) : JsonAdapter<List<CacheEntry<K, V>>>() {
    override fun fromJson(reader: JsonReader): List<CacheEntry<K, V>> {
        val entries = mutableListOf<CacheEntry<K, V>>()

        reader.beginArray()
        while (reader.hasNext()) {
            entries.add(cacheEntrySerializer.fromJson(reader))
        }
        reader.endArray()

        return entries.toList()
    }

    override fun toJson(writer: JsonWriter, cacheEntries: List<CacheEntry<K, V>>?) {
        cacheEntries?.let {
            writer.beginArray()
            cacheEntries.forEach { entry ->
                cacheEntrySerializer.toJson(writer, entry)
            }
            writer.endArray()
        } ?: writer.nullValue()
    }
}
