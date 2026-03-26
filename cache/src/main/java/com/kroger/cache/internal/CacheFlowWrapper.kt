package com.kroger.cache.internal

import com.kroger.cache.SnapshotPersistentCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
/**
 * A Wrapper class for a SnapshotPersistentCache that exposes changes to the cache via a flow.
 *
 * **Note this works best when used as a singleton
 *
 * @param cache the [com.kroger.cache.SnapshotPersistentCache] holding the value(s) on disk
 * @param scope the [kotlinx.coroutines.CoroutineScope] to run the flow on
 *
 */
public class CacheFlowWrapper<T>(
    private val cache: SnapshotPersistentCache<T>,
    private val scope: CoroutineScope,
) {
    /**
     * A reference to the coroutine job used for reading the first value from the [cache] and emitting it on [_cacheValueState]
     */
    private val initializerJob: Job

    /**
     * The private mutable state flow for the current value
     */
    private val _cacheValueState = MutableStateFlow<T?>(null)

    /**
     * publicly exposed read-only flow on which to read and observe changes to the current value
     */
    public val cacheValueFlow: StateFlow<T?> = _cacheValueState.asStateFlow()

    /**
     * Initialization block reads the value the [cache] and emits it on [_cacheValueState]
     *
     * This job also updates the value in [cache] for each new value emitted on the flow
     * except for the first, which is read from [cache]
     */
    init {
        initializerJob = scope.launch {
            _cacheValueState.value = cache.read()

            cacheValueFlow
                .drop(1)
                .onEach {
                    cache.save(it)
                }.launchIn(scope)
        }
    }

    /**
     * Updates the value of the StateFlow
     * Any update to the state flow will be persisted to the [cache]
     * Waits for initialization to finish reading the first value from the [cache]
     * before emitting a new value on the flow
     *
     * @param newValue The new value to be both emitted on the flow, and saved in the [cache]
     */
    public suspend fun setValue(newValue: T) {
        if (initializerJob.isActive) {
            initializerJob.join()
        }
        _cacheValueState.emit(newValue)
    }
}
