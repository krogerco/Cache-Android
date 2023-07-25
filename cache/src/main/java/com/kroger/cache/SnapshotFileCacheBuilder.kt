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

import com.kroger.cache.internal.CacheEntry
import com.kroger.cache.internal.RealSnapshotFileCacheBuilder
import com.kroger.telemetry.Telemeter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Builder for a [SnapshotPersistentCache] that persists data to a file.
 *
 * NOTE: The [SnapshotPersistentCache] is not thread safe and access from multiple threads must be synchronized.
 * For any single [File] only one [SnapshotPersistentCache] should exist at a time.
 */
public interface SnapshotFileCacheBuilder<T> {
    public suspend fun build(): Result<SnapshotPersistentCache<T>>

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
         * @param parentDirectory the directory to create and save the cache file in. On Android it is
         * recommended to be somewhere in the cache directory such as Context.getCacheDir.
         * @param filename this should be in the form of "com.example.module.feature".
         * It is important for the filename to be unique across all [Cache] instances.
         * @param readDataFromFile converter to transform a [ByteArray] read from the cache file into [T]
         * @param writeDataToFile converter to transform [T] into a [ByteArray] that is saved to the cache file
         */
        public fun <T> from(
            parentDirectory: File,
            filename: String,
            readDataFromFile: (ByteArray) -> T?,
            writeDataToFile: (T?) -> ByteArray,
        ): SnapshotFileCacheBuilder<T> =
            RealSnapshotFileCacheBuilder(
                parentDirectory,
                filename,
                readDataFromFile,
                writeDataToFile,
            )

        /**
         * This is a convenience function to create a [SnapshotFileCacheBuilder] that uses [StringFormat]
         * from the kotlinx serialization library and creates a [SnapshotPersistentCache] compatible with
         * a [MemoryCacheManagerBuilder].
         *
         * @param parentDirectory the directory to create and save the cache file in. On Android it is
         * recommended to be somewhere in the cache directory such as Context.getCacheDir.
         * @param filename this should be in the form of "com.example.module.feature".
         * It is important for the filename to be unique across all [Cache] instances.
         * @param keySerializer serializer and deserializer for cache keys
         * @param valueSerializer serializer and deserializer for cache values
         * @param formatter [StringFormat] to use when doing [File] I/O.
         * Defaults to [Json][kotlinx.serialization.json.Json].
         */
        public fun <K, V> from(
            parentDirectory: File,
            filename: String,
            keySerializer: KSerializer<K>,
            valueSerializer: KSerializer<V>,
            formatter: StringFormat = Json,
        ): SnapshotFileCacheBuilder<List<CacheEntry<K, V>>> {
            val cacheEntrySerializer = ListSerializer(CacheEntry.serializer(keySerializer, valueSerializer))
            return RealSnapshotFileCacheBuilder(
                parentDirectory,
                filename,
                DefaultFileReader(cacheEntrySerializer, formatter),
                DefaultFileWriter(cacheEntrySerializer, formatter),
            )
        }

        /**
         * This is a convenience function to create a [SnapshotFileCacheBuilder] that uses [StringFormat]
         * from the kotlinx serialization library to read and write to the cache [File].
         *
         * @param parentDirectory the directory to create and save the cache file in. On Android it is
         * recommended to be somewhere in the cache directory such as Context.getCacheDir.
         * @param filename this should be in the form of "com.example.module.feature".
         * It is important for the filename to be unique across all [Cache] instances.
         * @param valueSerializer serializer and deserializer for cache values
         * @param formatter [StringFormat] to use when doing [File] I/O.
         * Defaults to [Json][kotlinx.serialization.json.Json].
         */
        public fun <T> from(
            parentDirectory: File,
            filename: String,
            valueSerializer: KSerializer<T>,
            formatter: StringFormat = Json,
        ): SnapshotFileCacheBuilder<T> = from(
            parentDirectory,
            filename,
            DefaultFileReader(valueSerializer, formatter),
            DefaultFileWriter(valueSerializer, formatter),
        )
    }
}

private class DefaultFileReader<T>(
    private val serializer: KSerializer<T>,
    private val formatter: StringFormat,
) : (ByteArray?) -> T? {
    override fun invoke(bytes: ByteArray?): T? =
        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            formatter.decodeFromString(serializer, bytes.decodeToString())
        }
}

private class DefaultFileWriter<T>(
    private val serializer: KSerializer<T>,
    private val formatter: StringFormat,
) : (T?) -> ByteArray {
    override fun invoke(data: T?): ByteArray =
        if (data == null) {
            ByteArray(0)
        } else {
            formatter.encodeToString(serializer, data).encodeToByteArray()
        }
}
