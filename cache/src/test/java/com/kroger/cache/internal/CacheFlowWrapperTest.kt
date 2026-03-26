package com.kroger.cache.internal

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kroger.cache.SnapshotPersistentCache
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CacheFlowWrapperTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    val testScope = CoroutineScope(CoroutineName("CacheFlowWrapperTest") + testDispatcher)
    val fileCache: SnapshotPersistentCache<String> = mockk()

    lateinit var cacheWrapper: CacheFlowWrapper<String>

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `GIVEN cache is still reading WHEN new value is set THEN set value will wait for read to finish`() = runTest {
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
    fun `GIVEN cache is done reading WHEN new value is set THEN set value will happen immediately`() = runTest {
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
    fun `GIVEN cache is writing values slowly WHEN new values are set in quick succession THEN all values are emitted on flow, and last value is saved to disk`() = runTest {
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
