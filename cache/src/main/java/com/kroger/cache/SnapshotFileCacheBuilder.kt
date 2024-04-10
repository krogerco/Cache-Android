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

import com.kroger.cache.internal.RealSnapshotFileCacheBuilder
import com.kroger.telemetry.Telemeter
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File

/**
 * Builder for a [SnapshotPersistentCache] that persists data to a file.
 *
 * NOTE: The [SnapshotPersistentCache] is not thread safe and access from multiple threads must be synchronized.
 * For any single [File] only one [SnapshotPersistentCache] should exist at a time.
 */
public interface SnapshotFileCacheBuilder<T> {
    public fun build(): SnapshotPersistentCache<T>

    /**
     * The [CoroutineDispatcher] to use when doing [File] I/O.
     * Defaults to [Dispatchers.IO][kotlinx.coroutines.Dispatchers.IO].
     */
    public fun dispatcher(dispatcher: CoroutineDispatcher): SnapshotFileCacheBuilder<T>

    /**
     * The [Telemeter] to use for logging internal events and errors.
     * Defaults to null (no logging).
     */
    public fun telemeter(telemeter: Telemeter): SnapshotFileCacheBuilder<T>

    public companion object {
        /**
         * This is a convenience function to create a [SnapshotFileCacheBuilder] that uses an implementation of [CacheSerializer]
         * to read and write to the cache [File].
         *
         * @param parentDirectory the directory to create and save the cache file in. On Android it is
         * recommended to be somewhere in the cache directory such as Context.getCacheDir.
         * @param filename this should be in the form of "com.example.module.feature".
         * It is important for the filename to be unique across all [Cache] instances.
         * @param valueSerializer an implementation of [CacheSerializer]
         */
        public fun <T> from(
            parentDirectory: File,
            filename: String,
            valueSerializer: CacheSerializer<T>,
        ): SnapshotFileCacheBuilder<T> =
            RealSnapshotFileCacheBuilder(
                parentDirectory,
                filename,
                valueSerializer,
            )
    }
}
