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
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class MemoryCacheTest {
    private val cache = MemoryCache<String, String>()

    @Test
    fun `when entry added then entry exists in cache`() = runBlocking {
        assertThat(cache.size()).isEqualTo(0)
        val (key, value) = Pair("key", "value")
        cache.put(key, value)
        assertThat(cache.get(key)).isEqualTo(value)
        assertThat(cache.size()).isEqualTo(1)
    }

    @Test
    fun `when multiple entries added then entries exist in cache`() = runBlocking {
        val pair1 = Pair("key1", "value1")
        val pair2 = Pair("key2", "value2")
        cache.putAll(listOf(pair1, pair2))
        assertThat(cache.get(pair1.first)).isEqualTo(pair1.second)
        assertThat(cache.get(pair2.first)).isEqualTo(pair2.second)
    }

    @Test
    fun `when entry updated then new value is in cache`() = runBlocking {
        val (key, value) = Pair("key", "value")
        cache.put(key, value)
        assertThat(cache.get(key)).isEqualTo(value)
        cache.put(key, "updated")
        assertThat(cache.get(key)).isEqualTo("updated")
    }

    @Test
    fun `when entry removed then entry is no longer in cache`() = runBlocking {
        val (key, value) = Pair("key", "value")
        cache.put(key, value)
        assertThat(cache.get(key)).isEqualTo(value)
        cache.remove(key)
        assertThat(cache.get(key)).isNull()
    }

    @Test
    fun `when clear called then cache becomes empty`() = runBlocking {
        val (key, value) = Pair("key", "value")
        cache.put(key, value)
        assertThat(cache.get(key)).isNotNull()
        cache.clear()
        assertThat(cache.get(key)).isNull()
        assertThat(cache.size()).isEqualTo(0)
    }

    @Test
    fun `when getAll called then all entries in cache returned`() = runBlocking {
        assertThat(cache.getAll()).isEmpty()
        val firstEntry = Pair("key1", "value1")
        val secondEntry = Pair("key2", "value2")
        cache.put(firstEntry.first, firstEntry.second)
        cache.put(secondEntry.first, secondEntry.second)

        val entries = cache.getAll().map { it.toPair() }
        assertThat(entries).hasSize(2)
        assertThat(entries).containsExactly(firstEntry, secondEntry).inOrder()
    }
}
