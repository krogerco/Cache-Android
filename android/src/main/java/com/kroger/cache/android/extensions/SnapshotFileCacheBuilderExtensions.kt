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
package com.kroger.cache.android.extensions

import android.content.Context
import com.kroger.cache.CacheSerializer
import com.kroger.cache.SnapshotFileCacheBuilder

private val SnapshotFileCacheBuilder.Companion.defaultCacheDir: String
    get() = "com.kroger.cache"

/**
 * Convenience function to create a [SnapshotFileCacheBuilder] in the application's cache directory.
 * This will create the cache file at {[Context.getCacheDir]}/com.kroger.cache/{[filename]}
 * @see [com.kroger.cache.SnapshotFileCacheBuilder.Companion.from]
 */
public fun <T> SnapshotFileCacheBuilder.Companion.from(
    context: Context,
    filename: String,
    readDataFromFile: (ByteArray) -> T?,
    writeDataToFile: (T?) -> ByteArray,
): SnapshotFileCacheBuilder<T> =
    from(
        context.cacheDir.resolve(defaultCacheDir),
        filename,
        readDataFromFile,
        writeDataToFile,
    )

/**
 * Convenience function to create a [SnapshotFileCacheBuilder] in the application's cache directory.
 * This will create the cache file at {[Context.getCacheDir]}/com.kroger.cache/{[filename]}
 * @see [com.kroger.cache.SnapshotFileCacheBuilder.Companion.from]
 */
public fun <T> SnapshotFileCacheBuilder.Companion.from(
    context: Context,
    filename: String,
    valueSerializer: CacheSerializer<T>,
): SnapshotFileCacheBuilder<T> =
    from(
        context.cacheDir.resolve(defaultCacheDir),
        filename,
        valueSerializer,
    )
