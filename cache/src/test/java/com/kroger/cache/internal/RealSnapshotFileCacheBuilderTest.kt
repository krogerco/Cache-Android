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
import com.kroger.cache.SnapshotFileCacheBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class RealSnapshotFileCacheBuilderTest {
    @field:TempDir
    private lateinit var tempDir: File

    @Test
    fun `given snapshot file cache from builder when values written to cache then expected values read from cache`() = runTest {
        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            "testFile",
            String.serializer(),
            Int.serializer(),
        ).build()
        val entry1 = CacheEntry("1", 1, 0L, 0L)
        val entry2 = CacheEntry("2", 2, 0L, 0L)
        val entries = listOf(entry1, entry2)
        fileCache.save(entries)
        val readEntries = fileCache.read()
        assertThat(readEntries).containsExactlyElementsIn(entries).inOrder()

        fileCache.save(null)
        assertThat(fileCache.read()).isNull()
    }

    @Test
    fun `given file cache builder when build called successfully then file cache configured with correct values`() = runTest {
        val filename = "testFile"
        var readCalled = false
        val readFunc: (ByteArray) -> String? = {
            readCalled = true
            ""
        }
        var writeCalled = false
        val writeFunc: (String?) -> ByteArray = {
            writeCalled = true
            ByteArray(0)
        }

        val fileCache = SnapshotFileCacheBuilder.from(
            tempDir,
            filename,
            readFunc,
            writeFunc,
        ).build()

        fileCache.save("")
        assertThat(writeCalled).isTrue()
        fileCache.read()
        assertThat(readCalled).isTrue()
        assertThat(tempDir.resolve(filename).exists()).isTrue()
    }
}
