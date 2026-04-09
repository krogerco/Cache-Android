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
package com.kroger.cache

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheFlowWrapperTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    val testScope = CoroutineScope(CoroutineName("CacheFlowWrapperTest") + testDispatcher)
    val fileCache: SnapshotPersistentCache<String> = mockk()

    lateinit var cacheWrapper: CacheFlowWrapper<String>

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @BeforeEach
    fun setup() {
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> throw e }
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun afterEach() {
        Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler)
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN cache is still reading WHEN new value is set THEN set value will wait for read to finish`() =
        runTest {
            val fileCacheValue = "File cache value"
            val newValue = "new value"
            coEvery { fileCache.read() } coAnswers {
                delay(1000)
                fileCacheValue
            }
            coEvery { fileCache.save(any()) } just runs
            cacheWrapper = CacheFlowWrapper(fileCache, testScope)
            cacheWrapper.cacheValueFlow.test {
                assertThat(awaitItem()).isEqualTo(null)
                cacheWrapper.setValue(newValue)
                assertThat(awaitItem()).isEqualTo(fileCacheValue)
                advanceTimeBy(1000)
                assertThat(awaitItem()).isEqualTo(newValue)
                cancelAndIgnoreRemainingEvents()
            }

            coVerifySequence {
                fileCache.read()
                fileCache.save(eq(newValue))
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN cache is done reading WHEN new value is set THEN set value will happen immediately`() =
        runTest {
            val fileCacheValue = "File cache value"
            val newValue = "new value"
            coEvery { fileCache.read() } coAnswers {
                fileCacheValue
            }
            coEvery { fileCache.save(any()) } just runs
            cacheWrapper = CacheFlowWrapper(fileCache, testScope)
            cacheWrapper.cacheValueFlow.test {
                assertThat(awaitItem()).isEqualTo(fileCacheValue)
                advanceTimeBy(1000)
                cacheWrapper.setValue(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
                cancelAndIgnoreRemainingEvents()
            }

            coVerifySequence {
                fileCache.read()
                fileCache.save(eq(newValue))
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN cache is writing values slowly WHEN new values are set in quick succession THEN all values are emitted on flow, and last value is saved to disk`() =
        runTest {
            val firstValue = "first new value"
            val secondValue = "second new value"
            val thirdValue = "third new value"
            val fourthValue = "Fourth new value"
            coEvery { fileCache.read() } returns null
            coEvery { fileCache.save(any()) } coAnswers {
                delay(1000)
            }
            cacheWrapper = CacheFlowWrapper(fileCache, testScope)
            cacheWrapper.cacheValueFlow.test {
                assertThat(awaitItem()).isEqualTo(null)
                cacheWrapper.setValue(firstValue)
                cacheWrapper.setValue(secondValue)
                cacheWrapper.setValue(thirdValue)
                cacheWrapper.setValue(fourthValue)
                advanceTimeBy(2000)
                assertThat(awaitItem()).isEqualTo(firstValue)
                assertThat(awaitItem()).isEqualTo(secondValue)
                assertThat(awaitItem()).isEqualTo(thirdValue)
                assertThat(awaitItem()).isEqualTo(fourthValue)
                cancelAndIgnoreRemainingEvents()
            }

            coVerifySequence {
                fileCache.read()
                fileCache.save(eq(firstValue)) // start writing the first value
                // second and third should be skipped since first isn't done writing yet
                fileCache.save(eq(fourthValue)) // fourth and final value is written
            }
        }
}
