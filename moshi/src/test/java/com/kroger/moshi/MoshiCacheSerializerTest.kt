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

import com.google.common.truth.Truth.assertThat
import com.kroger.cache.internal.CacheEntry
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class MoshiCacheSerializerTest {

    @Test
    fun `Given a MoshiCacheSerializer, When toByteArray is called with null data, Then an empty Byte array should be returned`() {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi.adapter(String::class.java), moshi.adapter(Int::class.java))

        val serializer = MoshiCacheSerializer(entrySerializer)

        val result = serializer.toByteArray(null)

        assertThat(result).isInstanceOf(ByteArray::class.java)
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `Given a MoshiCacheSerializer, When toByteArray is called with en empty byte array, Then an empty Byte array should be returned`() {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi.adapter(String::class.java), moshi.adapter(Int::class.java))

        val serializer = MoshiCacheSerializer(entrySerializer)

        val result = serializer.decodeFromString(ByteArray(0))

        assertThat(result).isNull()
    }

    @Test
    fun `Given a MoshiCacheSerializer, When toByteArray is called with valid data, Then a CacheEntry should be returned`() {
        val cacheEntry = CacheEntry("Hello", 8, 12345L, 6789L)
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi.adapter(String::class.java), moshi.adapter(Int::class.java))

        val serializer = MoshiCacheSerializer(entrySerializer)

        val result = serializer.toByteArray(cacheEntry)

        assertThat(result).isInstanceOf(ByteArray::class.java)
        assertThat(serializer.decodeFromString(result)).isEqualTo(cacheEntry)
    }

    @Test
    fun `Given a KotlinCacheListSerializer, When toByteArray is called with null data, Then an empty Byte array should be returned`() {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi.adapter(String::class.java), moshi.adapter(Int::class.java))
        val entryListSerializer: CacheEntryListSerializer<String, Int> = CacheEntryListSerializer(cacheEntrySerializer = entrySerializer)

        val serializer = MoshiCacheSerializer(entryListSerializer)

        val result = serializer.toByteArray(null)

        assertThat(result).isInstanceOf(ByteArray::class.java)
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `Given a KotlinCacheListSerializer, When toByteArray is called with en empty byte array, Then an empty Byte array should be returned`() {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi.adapter(String::class.java), moshi.adapter(Int::class.java))
        val entryListSerializer: CacheEntryListSerializer<String, Int> = CacheEntryListSerializer(cacheEntrySerializer = entrySerializer)

        val serializer = MoshiCacheSerializer(entryListSerializer)

        val result = serializer.decodeFromString(ByteArray(0))

        assertThat(result).isNull()
    }

    @Test
    fun `Given a KotlinCacheListSerializer, When toByteArray is called with valid data, Then a CacheEntry should be returned`() {
        val cacheEntry1 = CacheEntry("Hello", 8, 12345L, 6789L)
        val cacheEntry2 = CacheEntry("Goodbye", 12, 6789L, 12345L)
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi.adapter(String::class.java), moshi.adapter(Int::class.java))
        val entryListSerializer: CacheEntryListSerializer<String, Int> = CacheEntryListSerializer(cacheEntrySerializer = entrySerializer)

        val serializer = MoshiCacheSerializer(entryListSerializer)

        val result = serializer.toByteArray(listOf(cacheEntry1, cacheEntry2))

        assertThat(result).isInstanceOf(ByteArray::class.java)

        val entries = serializer.decodeFromString(result)!!

        assertThat(entries[0]).isEqualTo(cacheEntry1)
        assertThat(entries[1]).isEqualTo(cacheEntry2)
    }
}
