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
package com.kroger.cache.sampleapp

import com.kroger.cache.SnapshotPersistentCache
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A wrapper around a [SnapshotPersistentCache] with a [data] [SharedFlow] that emits
 * the latest cached data each time [save] is called.
 */
class FlowPersistentCache<T> private constructor(
    private val snapshotPersistentCache: SnapshotPersistentCache<T>,
    initialValue: T? = null,
) : SnapshotPersistentCache<T> {
    private val _data: MutableSharedFlow<T?> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val data: SharedFlow<T?> = _data.asSharedFlow()
    init {
        _data.tryEmit(initialValue)
    }
    override suspend fun read(): T? = data.replayCache.firstOrNull()

    override suspend fun save(cachedData: T?) {
        snapshotPersistentCache.save(cachedData)
        _data.tryEmit(cachedData)
    }

    companion object {
        suspend fun <T> from(snapshotCache: SnapshotPersistentCache<T>): FlowPersistentCache<T> {
            val initialValue = snapshotCache.read()
            return FlowPersistentCache(snapshotCache, initialValue)
        }
    }
}
