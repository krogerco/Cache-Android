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

import com.google.common.truth.Truth.assertThat
import com.kroger.cache.CachePolicy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.seconds

internal class CachePolicyBuilderTest {
    @Test
    fun `given cache policy builder when ttl negative then exception thrown`() {
        val ttl = (-1).seconds
        val exception = assertThrows<IllegalArgumentException> {
            CachePolicy.builder()
                .entryTtl(ttl)
                .build()
        }
        assertThat(exception.message).isEqualTo("entryTtl must be >= 0. entryTtl = ${ttl.toIsoString()}")
    }

    @Test
    fun `given cache policy builder when setting tti after ttl then exception thrown`() {
        val ttl = 1.seconds
        val exception = assertThrows<IllegalStateException> {
            CachePolicy.builder()
                .entryTtl(ttl)
                .entryTti(2.seconds)
                .build()
        }
        assertThat(exception.message).isEqualTo("entryTti cannot be set after entryTtl has been set. entryTtl = ${ttl.toIsoString()}")
    }

    @Test
    fun `given cache policy builder when tti negative then exception thrown`() {
        val tti = (-1).seconds
        val exception = assertThrows<IllegalArgumentException> {
            CachePolicy.builder()
                .entryTti(tti)
                .build()
        }
        assertThat(exception.message).isEqualTo("entryTti must be >= 0. entryTti = ${tti.toIsoString()}")
    }

    @Test
    fun `given cache policy builder when setting ttl after tti then exception thrown`() {
        val tti = 1.seconds
        val exception = assertThrows<IllegalStateException> {
            CachePolicy.builder()
                .entryTti(tti)
                .entryTtl(2.seconds)
                .build()
        }
        assertThat(exception.message).isEqualTo("entryTtl cannot be set after entryTti has been set. entryTti = ${tti.toIsoString()}")
    }

    @Test
    fun `given cache policy builder when setting maxSize less than 0 then exception thrown`() {
        val maxSize = -1
        val exception = assertThrows<IllegalArgumentException> {
            CachePolicy.builder()
                .maxSize(maxSize)
                .build()
        }
        assertThat(exception.message).isEqualTo("maxSize must be >= 0. maxSize = $maxSize")
    }

    @Test
    fun `given cache policy builder when custom values provided and build called then cache policy created uses all custom values`() {
        val maxSize = 200
        val tti = 5.seconds
        val ttl = 10.seconds

        val policy1 = CachePolicy.builder()
            .maxSize(maxSize)
            .entryTti(tti)
            .build()

        val policy2 = CachePolicy.builder()
            .entryTtl(ttl)
            .build()

        assertThat(policy1.maxSize).isEqualTo(maxSize)
        assertThat(policy1.entryTti).isEqualTo(tti)
        assertThat(policy1.hasMaxSizePolicy).isTrue()
        assertThat(policy1.hasTtiPolicy).isTrue()
        assertThat(policy1.hasTtlPolicy).isFalse()

        assertThat(policy2.entryTtl).isEqualTo(ttl)
        assertThat(policy2.hasTtlPolicy).isTrue()
        assertThat(policy2.hasTtiPolicy).isFalse()
        assertThat(policy2.hasMaxSizePolicy).isFalse()
    }
}
