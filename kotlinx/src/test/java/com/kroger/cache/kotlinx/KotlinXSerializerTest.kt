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

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test

class KotlinXSerializerTest {

    @Test
    fun `Given a KotlinXCacheSerializer, When toByteArray is called with null data, Then an empty Byte array should be returned`() {
        val serializer = KotlinXCacheSerializer(serializer = KotlinCacheEntrySerializer<String, Int>(String.serializer(), Int.serializer()))

        val result = serializer.toByteArray(null)

        assertThat(result).isInstanceOf(ByteArray::class.java)
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `Given a KotlinXCacheListSerializer, When toByteArray is called with null data, Then an empty Byte array should be returned`() {
        val serializer = KotlinXCacheListSerializer(keySerializer = String.serializer(), valueSerializer = Int.serializer())

        val result = serializer.toByteArray(null)

        assertThat(result).isInstanceOf(ByteArray::class.java)
        assertThat(result.size).isEqualTo(0)
    }
}
