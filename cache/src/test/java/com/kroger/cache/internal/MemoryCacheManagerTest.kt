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
import com.kroger.cache.MemoryLevelNotifier
import com.kroger.cache.fake.FakeTimeProvider
import com.kroger.cache.fake.InMemorySnapshotPersistentCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
internal class MemoryCacheManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val fakeTimeProvider = FakeTimeProvider()

    private var snapshotScope = CoroutineScope(StandardTestDispatcher(testScope.testScheduler, "snapshotScope"))

    @Test
    fun `given cache manager configured without persistent cache when initialized then no job to save to persistent cache`() = testScope.runTest {
        createMemoryCacheManager<String, String>(null)
        assertThat(snapshotScope.coroutineContext.job.children.count()).isEqualTo(0)
    }

    @Test
    fun `given cache manager configured with persistent cache when initialized then job setup to save to persistent cache`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        createMemoryCacheManager(snapshotPersistentCache)
        assertThat(snapshotScope.coroutineContext.job.children.count()).isEqualTo(1)
    }

    @Test
    fun `given persistent cache manager with values when initialized then memory cache contains values`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(listOf(cacheEntry1, cacheEntry2))
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        assertThat(cacheManager.get(cacheEntry1.key)).isEqualTo(cacheEntry1.value)
        assertThat(cacheManager.get(cacheEntry2.key)).isEqualTo(cacheEntry2.value)
    }

    @Test
    fun `given persistent cache manager when entry added then persistent cache contains value after delay`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        assertThat(cacheManager.get(cacheEntry1.key)).isNull()
        cacheManager.put(cacheEntry1.key, cacheEntry1.value)
        assertThat(cacheManager.get(cacheEntry1.key)).isEqualTo(cacheEntry1.value)
        assertThat(snapshotPersistentCache.read()).isNull()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
    }

    @Test
    fun `given persistent cache manager when entry added or retrieved then creationDate and lastAccessDate set accordingly`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        cacheManager.put(cacheEntry1.key, cacheEntry1.value)

        // advance so added entry is saved to persistent cache
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        val firstEntry = snapshotPersistentCache.read()!!.first()
        assertThat(firstEntry.creationDate).isEqualTo(0L)
        assertThat(firstEntry.lastAccessDate).isEqualTo(0L)
        assertThat(firstEntry.value).isEqualTo(cacheEntry1.value)

        // access element and advance so update to entry is saved to persistent cache
        assertThat(cacheManager.get(cacheEntry1.key)).isNotNull()
        advanceUntilIdle()
        val updatedFirstEntry = snapshotPersistentCache.read()!!.first()
        assertThat(firstEntry).isNotSameInstanceAs(updatedFirstEntry) // each read updates the CacheEntry lastAccessDate
        assertThat(updatedFirstEntry.creationDate).isEqualTo(0L)
        assertThat(updatedFirstEntry.lastAccessDate).isEqualTo(1L)
    }

    @Test
    fun `given persistent cache manager when entry updated then creationDate and lastAccessDate set accordingly`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        cacheManager.put(cacheEntry1.key, cacheEntry1.value)

        // advance so added entry is saved to persistent cache
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        val firstEntry = snapshotPersistentCache.read()!!.first()
        assertThat(firstEntry.creationDate).isEqualTo(0L)
        assertThat(firstEntry.lastAccessDate).isEqualTo(0L)
        assertThat(firstEntry.value).isEqualTo(cacheEntry1.value)

        // update the element causing the lastAccessDate and creationDate to change
        cacheManager.put(cacheEntry1.key, cacheEntry2.value)
        advanceUntilIdle()
        val updatedFirstEntry = snapshotPersistentCache.read()!!.first()
        assertThat(updatedFirstEntry.value).isEqualTo(cacheEntry2.value)
        assertThat(updatedFirstEntry.creationDate).isEqualTo(1L)
        assertThat(updatedFirstEntry.lastAccessDate).isEqualTo(1L)
    }

    @Test
    fun `given persistent cache manager when multiple entries added then persistent cache contains values after delay`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        assertThat(cacheManager.get(cacheEntry1.key)).isNull()
        cacheManager.putAll(
            listOf(
                cacheEntry1.key to cacheEntry1.value,
                cacheEntry2.key to cacheEntry2.value,
            ),
        )
        assertThat(cacheManager.get(cacheEntry1.key)).isEqualTo(cacheEntry1.value)
        assertThat(cacheManager.get(cacheEntry2.key)).isEqualTo(cacheEntry2.value)
        assertThat(snapshotPersistentCache.read()).isNull()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(2)
        val (first, second) = snapshotPersistentCache.read()!!.take(2)
        assertThat(first.creationDate).isEqualTo(0L)
        assertThat(first.lastAccessDate).isEqualTo(2L)
        assertThat(second.creationDate).isEqualTo(1L)
        assertThat(second.lastAccessDate).isEqualTo(3L)
    }

    @Test
    fun `given persistent cache manager when entry deleted then persistent cache removes value after delay`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(listOf(cacheEntry1))
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        assertThat(cacheManager.get(cacheEntry1.key)).isNotNull()
        cacheManager.remove(cacheEntry1.key)
        assertThat(cacheManager.get(cacheEntry1.key)).isNull()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).isEmpty()
    }

    @Test
    fun `given persistent cache manager when clear called then persistent cache removes all values after delay`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(listOf(cacheEntry1))
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        assertThat(cacheManager.get(cacheEntry1.key)).isEqualTo(cacheEntry1.value)
        cacheManager.clear()
        assertThat(cacheManager.get(cacheEntry1.key)).isNull()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(0)
    }

    @Test
    fun `given persistent cache manager when changes occur rapidly then only single save occurs after delay`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        cacheManager.put(cacheEntry1.key, cacheEntry1.value)
        cacheManager.put(cacheEntry2.key, cacheEntry2.value)
        cacheManager.remove(cacheEntry1.key)
        assertThat(snapshotPersistentCache.read()).isNull()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        assertThat(snapshotPersistentCache.saveCallCount).isEqualTo(1)
    }

    @Test
    fun `given persistent cache manager when coroutineScope cancelled before saveFrequency reached then entries still saved to persistent cache`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val scope = CoroutineScope(UnconfinedTestDispatcher(this.testScheduler))
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, scope)
        cacheManager.put(cacheEntry1.key, cacheEntry1.value)
        assertThat(snapshotPersistentCache.read()).isNull()
        scope.cancel()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
    }

    @Test
    fun `given persistent cache manager when coroutineScope cancelled then any further modifications after onCompletion runs are not saved to persistent cache`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache)
        cacheManager.put(cacheEntry1.key, cacheEntry1.value)
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        snapshotScope.cancel()
        // trigger onCompletion
        advanceUntilIdle()

        cacheManager.put(cacheEntry2.key, cacheEntry2.value) // add a second entry
        advanceUntilIdle()

        // size should still be 1 since cacheEntry2 was added after cancelling scope
        assertThat(snapshotPersistentCache.read()).hasSize(1)
    }

    @Test
    fun `given no persistent cache manager when initialized then memory cache still functions without persistence`() = testScope.runTest {
        val cacheManager: Cache<String, String> = createMemoryCacheManager(snapshotPersistentCache = null)
        cacheManager.put("1", "a")
        assertThat(cacheManager.get("1")).isEqualTo("a")
    }

    // region cache policy tests
    @Test
    fun `given cache manager with size policy when entry count exactly matches maxSize policy then nothing removed`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(maxSize = 1)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        assertThat(cacheManager.get("1")).isNotNull()
    }

    @Test
    fun `given cache manager with size policy when put violates policy then eldest inserted entry removed`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(maxSize = 1)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        cacheManager.put("2", "b")
        assertThat(cacheManager.get("1")).isNull()
        assertThat(cacheManager.get("2")).isEqualTo("b")
    }

    @Test
    fun `given cache manager with size policy when put updates existing entry and another new entry violates size policy then eldest inserted entry removed`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(maxSize = 2)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        cacheManager.put("2", "b")
        cacheManager.put("1", "u") // update moves to back of LRU queue
        cacheManager.put("3", "c") // removes entry "2" due to size policy
        assertThat(cacheManager.get("1")).isEqualTo("u")
        assertThat(cacheManager.get("2")).isNull()
        assertThat(cacheManager.get("3")).isEqualTo("c")
        snapshotScope.cancel()
    }

    @Test
    fun `given cache manager with size policy when putAll violates policy then eldest inserted entries removed`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(maxSize = 2)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.putAll(
            listOf(
                "1" to "a",
                "2" to "b",
                "3" to "c",
                "4" to "d",
            ),
        )
        assertThat(cacheManager.get("1")).isNull()
        assertThat(cacheManager.get("2")).isNull()
        assertThat(cacheManager.get("3")).isEqualTo("c")
        assertThat(cacheManager.get("4")).isEqualTo("d")
    }

    @Test
    fun `given cache manager with size and tti policy when entry violates policy then eldest entry last accessed removed`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(
            maxSize = 2,
            entryTti = 1.hours,
        )
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        cacheManager.put("2", "b")
        cacheManager.get("1") // moves to back of LRU list
        cacheManager.put("3", "c")
        assertThat(cacheManager.get("2")).isNull()
        assertThat(cacheManager.get("1")).isEqualTo("a")
        assertThat(cacheManager.get("3")).isEqualTo("c")
    }

    @Test
    fun `given cache manager with temporal policy when entry exactly matches ttl then entry is not removed`() = testScope.runTest {
        fakeTimeProvider.autoAdvanceIncrement = Duration.ZERO
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(entryTtl = 1.seconds)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        fakeTimeProvider.timeSource += 1.seconds
        assertThat(cacheManager.get("1")).isEqualTo("a")
    }

    @Test
    fun `given cache with size and temporal policy when size limit reached then temporal policy applied first`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(
            maxSize = 3,
            entryTtl = 5.seconds,
        )
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        cacheManager.put("2", "b")
        cacheManager.put("3", "c")
        fakeTimeProvider.timeSource += 6.seconds
        // although maxSize is only violated by one entry after this put, the resulting size
        // of the cache will only be 1 since the prior 3 entries violate the temporal policy
        // and the temporal policy is fully applied before trimming to the maxSize constraint.
        cacheManager.put("4", "d")
        advanceUntilIdle()
        assertThat(cacheManager.get("3")).isNull()
        assertThat(cacheManager.get("4")).isNotNull()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
    }

    @Test
    fun `given cache manager with ttl temporal policy when entry violates policy during retrieval then entry removed and null returned`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(
            entryTtl = 5.seconds,
        )
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        assertThat(cacheManager.get("1")).isNotNull()
        fakeTimeProvider.timeSource += 5.seconds
        assertThat(cacheManager.get("1")).isNull()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).isEmpty()
    }

    @Test
    fun `given cache manager with tti temporal policy when entry violates policy during retrieval then entry removed and null returned`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cachePolicy = CachePolicy(
            entryTti = 5.seconds,
        )
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        cacheManager.put("1", "a")
        assertThat(cacheManager.get("1")).isNotNull()
        fakeTimeProvider.timeSource += 5.seconds
        assertThat(cacheManager.get("1")).isNull()
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).isEmpty()
    }

    @Test
    fun `given cache manager with maxSize policy when cache first loaded then maxSize honored`() = testScope.runTest {
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(listOf(cacheEntry1, cacheEntry2))
        val cachePolicy = CachePolicy(maxSize = 1)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        advanceUntilIdle()
        assertThat(snapshotPersistentCache.read()).hasSize(1)
        assertThat(cacheManager.get(cacheEntry1.key)).isNull()
        assertThat(cacheManager.get(cacheEntry2.key)).isNotNull()
    }

    @Test
    fun `given cache manager with ttl policy when cache first loaded then ttl honored`() = testScope.runTest {
        fakeTimeProvider.autoAdvanceIncrement = Duration.ZERO
        fakeTimeProvider.timeSource += 2500.milliseconds
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(listOf(cacheEntry1, cacheEntry2))
        val cachePolicy = CachePolicy(entryTtl = 1.seconds)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)
        assertThat(cacheManager.get(cacheEntry1.key)).isNull() // entry 1 expired
        assertThat(cacheManager.get(cacheEntry2.key)).isNotNull() // entry 2 not expired
    }

    @Test
    fun `given cache manager with temporal policy when cache first loaded with different temporal policy then latest policy honored`() = testScope.runTest {
        fakeTimeProvider.autoAdvanceIncrement = Duration.ZERO
        fakeTimeProvider.timeSource += 6.seconds

        // entries initially ordered by ttl in persistent cache
        val cacheEntry1 = CacheEntry("a", "1", 1000, 5000)
        val cacheEntry2 = CacheEntry("b", "2", 2000, 2000)
        val snapshotPersistentCache = InMemorySnapshotPersistentCache(listOf(cacheEntry1, cacheEntry2))

        // cachePolicy specifies a tti policy with maxSize 1
        val cachePolicy = CachePolicy(maxSize = 1, entryTti = 1.hours)
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, cachePolicy = cachePolicy)

        // based on new policy and sorting the cacheEntry2 is removed and cacheEntry1 remains
        assertThat(cacheManager.get(cacheEntry1.key)).isNotNull()
        assertThat(cacheManager.get(cacheEntry2.key)).isNull()
    }

    @Test
    fun `given cache manager with saveFrequency when cache updated faster than saveFrequency then save still called`() = testScope.runTest {
        fakeTimeProvider.autoAdvanceIncrement = Duration.ZERO
        val snapshotPersistentCache = InMemorySnapshotPersistentCache<List<CacheEntry<String, String>>>()
        val cacheManager = createMemoryCacheManager(snapshotPersistentCache, saveFrequency = 3.seconds)
        cacheManager.put("a", "1")
        advanceTimeBy(1000)
        cacheManager.put("b", "2")
        advanceTimeBy(2000)
        cacheManager.put("c", "2")
        advanceTimeBy(2000) // 5 total seconds elapsed so persistent cache should have saved once

        assertThat(snapshotPersistentCache.saveCallCount).isEqualTo(1)
    }
    // endregion

    private fun <K, V> createMemoryCacheManager(
        snapshotPersistentCache: InMemorySnapshotPersistentCache<List<CacheEntry<K, V>>>?,
        scope: CoroutineScope = snapshotScope,
        dispatcher: CoroutineDispatcher = testDispatcher,
        cachePolicy: CachePolicy = CachePolicy(),
        saveFrequency: Duration = defaultSaveFrequency,
        memoryLevelNotifier: MemoryLevelNotifier? = null,
    ): Cache<K, V> {
        return RealMemoryCacheManagerBuilder(
            snapshotPersistentCache,
            fakeTimeProvider,
        ).dispatcher(dispatcher)
            .coroutineScope(scope)
            .cachePolicy(cachePolicy)
            .saveFrequency(saveFrequency)
            .memoryLevelNotifier(memoryLevelNotifier)
            .build()
    }

    companion object {
        val cacheEntry1 = CacheEntry("key1", "value1", 1000, 1000)
        val cacheEntry2 = CacheEntry("key2", "value2", 2000, 2000)
        val defaultSaveFrequency: Duration = 6.seconds
    }
}
