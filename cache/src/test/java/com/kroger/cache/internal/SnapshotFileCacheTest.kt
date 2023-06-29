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
import com.kroger.cache.fake.FakeSnapshotFileCacheDataHandler
import com.kroger.cache.fake.FakeTelemeter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class SnapshotFileCacheTest {
    @field:TempDir
    private lateinit var tempDir: File

    private val fakeTelemeter = FakeTelemeter()

    @Test
    fun `given snapshot file cache when data is saved successfully then data can be read successfully`() = runTest {
        val fileCache = createSnapshotFileCache()
        val savedData = "Success :)"
        fileCache.save(savedData)
        assertThat(fileCache.read()).isEqualTo(savedData)
        assertThat(fakeTelemeter.events).isEmpty()
    }

    @Test
    fun `given snapshot file cache when null is saved successfully then file contains empty string`() = runTest {
        val tempFile = tempDir.resolve("testFile")
        tempFile.createNewFile()
        tempFile.writeText("Some data")

        val fileCache = createSnapshotFileCache(tempFile)
        fileCache.save(null)
        assertThat(tempFile.readText()).isEmpty()
        assertThat(fakeTelemeter.events).isEmpty()
    }

    @Test
    fun `given snapshot file cache when read with no data in file then null is returned`() = runTest {
        val tempFile = tempDir.resolve("testFile")
        tempFile.createNewFile()
        val fileCache = createSnapshotFileCache(tempFile)
        assertThat(fileCache.read()).isNull()
        assertThat(fakeTelemeter.events).hasSize(0)
    }

    @Test
    fun `given snapshot file cache when data save fails then no data saved and an error is logged`() = runTest {
        val fileCache = createSnapshotFileCache(tempDir.resolve("i/dont/exist"))
        fileCache.save("Failed :(")
        assertThat(fakeTelemeter.events).hasSize(1)
        assertThat(fakeTelemeter.events.first().description).contains("An error occurred while writing to the cache file")
    }

    @Test
    fun `given snapshot file cache when read fails then null is returned and an error is logged`() = runTest {
        val fileCache = createSnapshotFileCache(tempDir.resolve("i/dont/exist"))
        val readData = fileCache.read()

        assertThat(readData).isNull()
        assertThat(fakeTelemeter.events).hasSize(1)
        assertThat(fakeTelemeter.events.first().description).contains("An error occurred while reading from the cache file")
    }

    private fun TestScope.createSnapshotFileCache(
        file: File = tempDir.resolve("testFile"),
    ) = SnapshotFileCache(
        file,
        FakeSnapshotFileCacheDataHandler::readDataFromFile,
        FakeSnapshotFileCacheDataHandler::writeDataToFile,
        fakeTelemeter,
        UnconfinedTestDispatcher(testScheduler),
    )
}
