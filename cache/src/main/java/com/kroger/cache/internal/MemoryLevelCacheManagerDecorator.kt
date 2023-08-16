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

import com.kroger.cache.Cache
import com.kroger.cache.MemoryLevelCallbacks
import com.kroger.cache.MemoryLevelNotifier
import com.kroger.telemetry.Telemeter
import com.kroger.telemetry.relay.i
import com.kroger.telemetry.relay.w
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

private const val TAG = "MemoryLevelCacheManager"

/**
 * A decorator that adds functionality to listen for [MemoryLevelCallbacks] from the [MemoryLevelNotifier].
 * When [MemoryLevelCallbacks.onLowMemory] is triggered then expired entries will be purged from the [Cache].
 * When [MemoryLevelCallbacks.onCriticalMemory] is triggered then all entries will be purged from the [Cache].
 * @param cacheManager the [MemoryCacheManager] to apply the memory triggers on
 * @param memoryLevelNotifier [MemoryLevelNotifier] responsible for delivering [MemoryLevelCallbacks]
 * @param telemeter [Telemeter] to use for logging events and errors
 */
internal class MemoryLevelCacheManagerDecorator<K, V>(
    private val cacheManager: MemoryCacheManager<K, V>,
    private val memoryLevelNotifier: MemoryLevelNotifier,
    private val telemeter: Telemeter?,
) : Cache<K, V> by cacheManager {
    private val coroutineScope: CoroutineScope = cacheManager.coroutineScope

    private val memoryLevelCallbacks = object : MemoryLevelCallbacks {
        override fun onLowMemory() {
            coroutineScope.launch {
                cacheManager.trimMemory()
                telemeter?.i(TAG, "Memory has reached a low level causing the cache to remove all expired entries.")
            }
        }

        override fun onCriticalMemory() {
            coroutineScope.launch {
                cacheManager.clear()
                telemeter?.w(TAG, "Memory has reached a critical level causing the cache to be cleared.")
            }
        }
    }

    init {
        memoryLevelNotifier.memoryLevelCallbacks = memoryLevelCallbacks
        // when the coroutineScope of the cacheManager completes we need to make sure
        // the memoryLevelCallbacks are removed to avoid leaking the cache in use cases
        // like android where a callback is registered on the application context.
        coroutineScope.coroutineContext.job.invokeOnCompletion {
            memoryLevelNotifier.memoryLevelCallbacks = null
        }
    }
}
