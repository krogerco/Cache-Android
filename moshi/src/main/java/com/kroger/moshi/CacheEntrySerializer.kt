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
import com.squareup.moshi.Moshi
import javax.inject.Inject

/**
 * An implementation of a Moshi [JsonAdapter] for the [CacheEntry] class
 *
 * @property moshi an instance of Moshi
 * @property keyAdapter the [JsonAdapter] for [CacheEntry.key]
 * @property valueAdapter the [JsonAdapter] for [CacheEntry.value]
 */
public class CacheEntrySerializer<K, V> @Inject constructor(
    private val moshi: Moshi,
    internal val keyAdapter: JsonAdapter<K>,
    internal val valueAdapter: JsonAdapter<V>,
) : JsonAdapter<CacheEntry<K, V>>() {
    private val options: JsonReader.Options
        get() = JsonReader.Options.of("key", "value", "creationDate", "lastAccessDate")

    private val nullableLongAdapter: JsonAdapter<Long?>
        get() = moshi.adapter(Long::class.javaObjectType)
    private val longAdapter: JsonAdapter<Long> = moshi.adapter(Long::class.java)

    override fun fromJson(reader: JsonReader): CacheEntry<K, V> {
        var key: K? = null
        var value: V? = null
        var creationDate: Long? = 0L
        var lastAccessDate: Long? = 0L

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> key = keyAdapter.fromJson(reader)
                1 -> value = valueAdapter.fromJson(reader)
                2 -> creationDate = nullableLongAdapter.fromJson(reader)
                3 -> lastAccessDate = nullableLongAdapter.fromJson(reader)
                -1 -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()
        require(key != null && value != null && creationDate != null && creationDate != 0L && lastAccessDate != null && lastAccessDate != 0L)

        return CacheEntry(key, value, creationDate, lastAccessDate)
    }

    public override fun toJson(writer: JsonWriter, cacheEntry: CacheEntry<K, V>?) {
        require(cacheEntry != null)

        writer.beginObject()
        writer.name("key")
        keyAdapter.toJson(writer, cacheEntry.key)
        writer.name("value")
        valueAdapter.toJson(writer, cacheEntry.value)
        writer.name("creationDate")
        longAdapter.toJson(writer, cacheEntry.creationDate)
        writer.name("lastAccessDate")
        longAdapter.toJson(writer, cacheEntry.lastAccessDate)
        writer.endObject()
    }
}
