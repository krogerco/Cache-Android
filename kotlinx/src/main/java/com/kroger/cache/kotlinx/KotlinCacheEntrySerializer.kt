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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * [KSerializer] instance to map [CacheEntry] via kotlinx serialization
 */
public class KotlinCacheEntrySerializer<K, V>(private val keySerializer: KSerializer<K>, private val valueSerializer: KSerializer<V>) : KSerializer<CacheEntry<K, V>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CacheEntry") {
        element("key", keySerializer.descriptor)
        element("value", valueSerializer.descriptor)
        element("creationDate", PrimitiveSerialDescriptor("creationDate", PrimitiveKind.LONG))
        element("lastAccessDate", PrimitiveSerialDescriptor("lastAccessDate", PrimitiveKind.LONG))
    }

    override fun serialize(encoder: Encoder, value: CacheEntry<K, V>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, keySerializer, value.key)
            encodeSerializableElement(descriptor, 1, valueSerializer, value.value)
            encodeLongElement(descriptor, 2, value.creationDate)
            encodeLongElement(descriptor, 3, value.lastAccessDate)
        }
    }

    override fun deserialize(decoder: Decoder): CacheEntry<K, V> =
        decoder.decodeStructure(descriptor) {
            var key: K? = null
            var value: V? = null
            var creationDate = 0L
            var lastAccessDate = 0L
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> key = decodeSerializableElement(descriptor, 0, keySerializer)
                    1 -> value = decodeSerializableElement(descriptor, 1, valueSerializer)
                    2 -> creationDate = decodeLongElement(descriptor, 2)
                    3 -> lastAccessDate = decodeLongElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            require(key != null && value != null && creationDate != 0L && lastAccessDate != 0L)
            CacheEntry(key, value, creationDate, lastAccessDate)
        }
}
