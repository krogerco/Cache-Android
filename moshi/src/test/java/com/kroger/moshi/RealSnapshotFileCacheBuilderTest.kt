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
import com.kroger.cache.SnapshotFileCacheBuilder
import com.kroger.cache.internal.CacheEntry
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class RealSnapshotFileCacheBuilderTest {
    @field:TempDir
    private lateinit var tempDir: File

    @Test
    fun `given snapshot file cache from builder when values written to cache then expected values read from cache for list`() = runTest {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi, moshi.adapter(String::class.java), moshi.adapter(Int::class.java))
        val entryListSerializer: CacheEntryListSerializer<String, Int> = CacheEntryListSerializer(cacheEntrySerializer = entrySerializer)

        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            "testFile",
            MoshiCacheSerializer(entryListSerializer),
        ).build()

        val entry1 = CacheEntry("1", 1, 1000L, 2000L)
        val entry2 = CacheEntry("2", 2, 3000L, 4000L)

        val entries = listOf(entry1, entry2)
        fileCache.save(entries)

        val readEntries = fileCache.read()
        assertThat(readEntries).containsExactlyElementsIn(entries).inOrder()

        fileCache.save(null)
        assertThat(fileCache.read()).isNull()
    }

    @Test
    fun `Given a snapshot file cache, When an empty list is written to the file, then an empty list should be returned`() = runTest {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi, moshi.adapter(String::class.java), moshi.adapter(Int::class.java))
        val entryListSerializer: CacheEntryListSerializer<String, Int> = CacheEntryListSerializer(cacheEntrySerializer = entrySerializer)

        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            "testFile",
            MoshiCacheSerializer(entryListSerializer),
        ).build()

        fileCache.save(emptyList())

        val readEntries = fileCache.read()
        assertThat(readEntries).isEqualTo(emptyList<CacheEntry<String, Int>>())

        fileCache.save(null)
        assertThat(fileCache.read()).isNull()
    }

    @Test
    fun `Given a snapshot file cache and list serializer, When null is written to the file, then null should be returned`() = runTest {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi, moshi.adapter(String::class.java), moshi.adapter(Int::class.java))
        val entryListSerializer: CacheEntryListSerializer<String, Int> = CacheEntryListSerializer(cacheEntrySerializer = entrySerializer)

        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            "testFile",
            MoshiCacheSerializer(entryListSerializer),
        ).build()

        fileCache.save(null)

        val readEntries = fileCache.read()
        assertThat(readEntries).isNull()
    }

    @Test
    fun `given snapshot file cache from builder when values written to cache then expected values read from cache for single item`() = runTest {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi, moshi.adapter(String::class.java), moshi.adapter(Int::class.java))

        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            "testFile",
            MoshiCacheSerializer(entrySerializer),
        ).build()

        val entry = CacheEntry("1", 1, 1000L, 2000L)
        fileCache.save(entry)

        val readEntry = fileCache.read()
        assertThat(readEntry!!).isEqualTo(entry)

        fileCache.save(null)
        assertThat(fileCache.read()).isNull()
    }

    @Test
    fun `Given a snapshot file cache, When null is written to the file, then null should be returned`() = runTest {
        val moshi = Moshi.Builder().build()
        val entrySerializer: CacheEntrySerializer<String, Int> = CacheEntrySerializer(moshi, moshi.adapter(String::class.java), moshi.adapter(Int::class.java))

        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            "testFile",
            MoshiCacheSerializer(entrySerializer),
        ).build()

        fileCache.save(null)

        val readEntries = fileCache.read()
        assertThat(readEntries).isNull()
    }
}
