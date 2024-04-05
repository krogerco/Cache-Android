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

import com.kroger.cache.CacheSerializer
import com.kroger.cache.SnapshotPersistentCache
import com.kroger.telemetry.Telemeter
import com.kroger.telemetry.relay.e
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "SnapshotFileCache"

internal class SnapshotFileCache<T>(
    private val file: File,
    private val cacheSerializer: CacheSerializer<T>,
    private val telemeter: Telemeter? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SnapshotPersistentCache<T> {

    private fun ensureCacheFileCreated() {
        file.parentFile?.let { parentDirectory ->
            check(parentDirectory.exists() || parentDirectory.mkdirs()) {
                "Unable to create cache directory at: absolutePath=${parentDirectory.absolutePath}"
            }
        }

        check(file.exists() || file.createNewFile()) {
            "Unable to create cache file at: absolutePath=${file.absolutePath}"
        }
    }

    override suspend fun read(): T? = withContext(dispatcher) {
        runCatching {
            ensureCacheFileCreated()
            cacheSerializer.decodeFromString(file.readBytes())
        }.onFailure {
            telemeter?.e(TAG, "An error occurred while reading from the cache file: ${file.absolutePath}", it)
        }.getOrNull()
    }

    override suspend fun save(cachedData: T?): Unit = withContext(dispatcher) {
        runCatching {
            ensureCacheFileCreated()
            val cachedDataBytes = cacheSerializer.toByteArray(cachedData)
            file.writeBytes(cachedDataBytes)
        }.onFailure {
            telemeter?.e(TAG, "An error occurred while writing to the cache file: ${file.absolutePath}", it)
        }
    }
}
