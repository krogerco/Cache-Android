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
package com.kroger.cache.sampleapp.di

import android.content.Context
import com.kroger.cache.SnapshotFileCacheBuilder
import com.kroger.cache.SnapshotPersistentCache
import com.kroger.cache.android.extensions.from
import com.kroger.cache.internal.CacheEntry
import com.kroger.cache.kotlinx.CacheEntryListSerializer
import com.kroger.cache.kotlinx.KotlinCacheSerializer
import com.kroger.cache.sampleapp.CacheConfig
import com.kroger.cache.sampleapp.FlowPersistentCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.serializer

@Module
@InstallIn(SingletonComponent::class)
class CacheModule {
    @Provides
    fun provideCacheConfigFileCache(
        @ApplicationContext context: Context,
    ): SnapshotPersistentCache<CacheConfig> =
        SnapshotFileCacheBuilder.from(
            context = context,
            filename = "cacheConfig.json",
            cacheSerializer = KotlinCacheSerializer(serializer = CacheConfig.serializer()),
        ).build()

    @Provides
    fun provideFileCache(
        @ApplicationContext context: Context,
    ): SnapshotPersistentCache<List<CacheEntry<String, String>>> =
        SnapshotFileCacheBuilder.from(
            context,
            filename = "cacheFile.json",
            cacheSerializer = CacheEntryListSerializer(keySerializer = String.serializer(), valueSerializer = String.serializer()),
        ).build()

    @Provides
    fun provideSnapshotCacheFlowWrapper(
        snapshotCache: SnapshotPersistentCache<List<CacheEntry<String, String>>>,
    ): FlowPersistentCache<List<CacheEntry<String, String>>> =
        FlowPersistentCache(snapshotCache)
}
