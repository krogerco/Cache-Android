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
import com.kroger.cache.Cache
import com.kroger.cache.CachePolicy
import com.kroger.cache.fake.FakeMemoryLevelNotifier
import com.kroger.cache.fake.FakeTelemeter
import com.kroger.cache.fake.FakeTimeProvider
import com.kroger.cache.fake.InMemorySnapshotPersistentCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
internal class MemoryLevelCacheManagerDecoratorTest {
    private val fakeMemoryLevelNotifier = FakeMemoryLevelNotifier()
    private val fakeTelemeter = FakeTelemeter()
    private val fakeTimeProvider = FakeTimeProvider()

    @Test
    fun `given memory level cache manager when coroutine scope cancelled then memory level callbacks set to null`() = runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val managerCoroutineScope = CoroutineScope(dispatcher)
        createMemoryCacheManager(snapshotPersistentCache, managerCoroutineScope, dispatcher)
        assertThat(fakeMemoryLevelNotifier.memoryLevelCallbacks).isNotNull()
        managerCoroutineScope.cancel()
        assertThat(fakeMemoryLevelNotifier.memoryLevelCallbacks).isNull()
    }

    @Test
    fun `given memory level cache manager when low memory triggered then expired entries purged`() = runTest {
        fakeTimeProvider.autoAdvanceIncrement = 0.seconds
        fakeTimeProvider.timeSource += 2.5.seconds
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(
            listOf(
                MemoryCacheManagerTest.cacheEntry1,
                MemoryCacheManagerTest.cacheEntry2,
            ),
        )

        val cachePolicy = CachePolicy(entryTtl = 1.seconds)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val managerCoroutineScope = CoroutineScope(dispatcher)
        createMemoryCacheManager(
            snapshotPersistentCache,
            managerCoroutineScope,
            dispatcher,
            cachePolicy = cachePolicy,
        )

        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(2)
        fakeMemoryLevelNotifier.triggerLowMemory() // trigger low memory event that should remove 1 entry
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        assertThat(fakeTelemeter.events).hasSize(1)
        assertThat(fakeTelemeter.events.first().description).contains("Memory has reached a low level causing the cache to remove all expired entries.")
    }

    @Test
    fun `given memory level cache manager when critical memory triggered then cache cleared`() = runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(
            listOf(
                MemoryCacheManagerTest.cacheEntry1,
                MemoryCacheManagerTest.cacheEntry2,
            ),
        )
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val managerCoroutineScope = CoroutineScope(dispatcher)
        createMemoryCacheManager(snapshotPersistentCache, managerCoroutineScope, dispatcher)
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(2)
        fakeMemoryLevelNotifier.triggerCriticalMemory()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(0)
        assertThat(fakeTelemeter.events).hasSize(1)
        assertThat(fakeTelemeter.events.first().description).contains("Memory has reached a critical level causing the cache to be cleared.")
    }

    private suspend fun <K, V> createMemoryCacheManager(
        snapshotPersistentCache: InMemorySnapshotPersistentCache<List<CacheEntry<K, V>>>?,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        cachePolicy: CachePolicy = CachePolicy(),
    ): Cache<K, V> {
        return RealMemoryCacheManagerBuilder<K, V>(
            fakeTimeProvider,
        ).dispatcher(dispatcher)
            .coroutineScope(scope)
            .telemeter(fakeTelemeter)
            .memoryLevelNotifier(fakeMemoryLevelNotifier)
            .cachePolicy(cachePolicy).also {
                if (snapshotPersistentCache != null) {
                    it.snapshotPersistentCache(snapshotPersistentCache)
                }
            }
            .build()
    }
}
