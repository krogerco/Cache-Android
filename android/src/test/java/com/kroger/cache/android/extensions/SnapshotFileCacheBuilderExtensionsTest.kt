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
import com.google.common.truth.Truth.assertThat
import com.kroger.cache.SnapshotFileCacheBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class SnapshotFileCacheBuilderExtensionsTest {
    @field:TempDir
    private lateinit var tempDir: File

    @Test
    fun whenGenericTypeBuilderUsedThenCacheCreatedSuccessfully() = runBlocking {
        val context = mockk<Context>()
        every { context.cacheDir } returns tempDir

        val cacheResult = SnapshotFileCacheBuilder.from(
            context,
            "com.kroger.cache.android.test",
            { "" },
            { it.orEmpty().encodeToByteArray() },
        ).build()
        assertThat(cacheResult.isSuccess).isTrue()
        verify(exactly = 1) { context.cacheDir }
    }

    @Test
    fun whenGenericCacheEntryBuilderUsedThenCacheCreatedSuccessfully() = runBlocking {
        val context = mockk<Context>()
        every { context.cacheDir } returns tempDir
        val cacheResult = SnapshotFileCacheBuilder.from(
            context,
            "com.kroger.cache.android.test",
            String.serializer(),
            String.serializer(),
        ).build()

        assertThat(cacheResult.isSuccess).isTrue()
        verify(exactly = 1) { context.cacheDir }
    }
}
