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

import com.kroger.cache.SnapshotFileCacheBuilder
import com.kroger.cache.SnapshotPersistentCache
import com.kroger.telemetry.Telemeter
import com.kroger.telemetry.relay.e
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class RealSnapshotFileCacheBuilder<T>(
    private val parentDirectory: File,
    private val filename: String,
    private val readDataFromFile: (ByteArray) -> T?,
    private val writeDataToFile: (T?) -> ByteArray,
) : SnapshotFileCacheBuilder<T> {
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var telemeter: Telemeter? = null

    override fun dispatcher(dispatcher: CoroutineDispatcher): RealSnapshotFileCacheBuilder<T> =
        apply {
            this.dispatcher = dispatcher
        }

    override fun telemeter(telemeter: Telemeter): RealSnapshotFileCacheBuilder<T> =
        apply {
            this.telemeter = telemeter
        }

    override suspend fun build(): Result<SnapshotPersistentCache<T>> = withContext(dispatcher) {
        runCatching {
            check(parentDirectory.exists() || parentDirectory.mkdirs()) {
                "Unable to create cache directory=${parentDirectory.absolutePath}"
            }

            val cacheFile = parentDirectory.resolve(filename)
            check(cacheFile.exists() || cacheFile.createNewFile()) {
                "Unable to create cache file at: directory=${parentDirectory.absolutePath}, filename=$filename"
            }

            SnapshotFileCache(
                cacheFile,
                readDataFromFile,
                writeDataToFile,
                telemeter,
                dispatcher,
            )
        }.onFailure {
            val errorMessage = "An error occurred while creating the cache at: directory=${parentDirectory.absolutePath}, filename=$filename"
            telemeter?.e("SnapshotFileCacheBuilder", errorMessage, it)
        }
    }
}
