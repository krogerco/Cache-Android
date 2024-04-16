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
import com.kroger.moshi.MoshiCacheSerializer
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.serializer
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
class CacheModule {
    @Provides
    @KotlinxCache
    fun provideKotlinCacheConfigFileCache(
        @ApplicationContext context: Context,
    ): SnapshotPersistentCache<CacheConfig> =
        SnapshotFileCacheBuilder.from(
            context = context,
            filename = "cacheConfigKotlinx.json",
            cacheSerializer = KotlinCacheSerializer(serializer = CacheConfig.serializer()),
        ).build()

    @Provides
    @KotlinxCache
    fun provideKotlinFileCache(
        @ApplicationContext context: Context,
    ): SnapshotPersistentCache<List<CacheEntry<String, String>>> =
        SnapshotFileCacheBuilder.from(
            context,
            filename = "cacheFileKotlinx.json",
            cacheSerializer = CacheEntryListSerializer(keySerializer = String.serializer(), valueSerializer = String.serializer()),
        ).build()

    @Provides
    @KotlinxCache
    fun provideKotlinSnapshotCacheFlowWrapper(
        @KotlinxCache snapshotCache: SnapshotPersistentCache<List<CacheEntry<String, String>>>,
    ): FlowPersistentCache<List<CacheEntry<String, String>>> =
        FlowPersistentCache(snapshotCache)

    @Provides
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @MoshiCache
    fun provideMoshiCacheConfigFileCache(
        @ApplicationContext context: Context,
        moshi: Moshi,
    ): SnapshotPersistentCache<CacheConfig> =
        SnapshotFileCacheBuilder.from(
            context = context,
            filename = "cacheConfigMoshi.json",
            cacheSerializer = MoshiCacheSerializer<CacheConfig>(moshi.adapter(CacheConfig::class.java)),
        ).build()

    @Provides
    @MoshiCache
    fun provideMoshiFileCache(
        @ApplicationContext context: Context,
        moshi: Moshi,
    ): SnapshotPersistentCache<List<CacheEntry<String, String>>> =
        SnapshotFileCacheBuilder.from(
            context,
            filename = "cacheFileMoshi.json",
            cacheSerializer = MoshiCacheSerializer(
                com.kroger.moshi.CacheEntryListSerializer(
                    com.kroger.moshi.CacheEntrySerializer<String, String>(
                        moshi,
                        keyAdapter = moshi.adapter(String::class.java),
                        valueAdapter = moshi.adapter(String::class.java),
                    ),
                ),
            ),
        ).build()

    @Provides
    @MoshiCache
    fun provideMoshiSnapshotCacheFlowWrapper(
        @MoshiCache snapshotCache: SnapshotPersistentCache<List<CacheEntry<String, String>>>,
    ): FlowPersistentCache<List<CacheEntry<String, String>>> =
        FlowPersistentCache(snapshotCache)
}

@Qualifier
annotation class MoshiCache

@Qualifier
annotation class KotlinxCache
