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
package com.kroger.cache.android.internal

import android.content.Context
import com.kroger.cache.MemoryLevelCallbacks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class AndroidMemoryLevelNotifierTest {
    private val memoryLevelCallbacks = object : MemoryLevelCallbacks { }

    @Test
    fun `given android memory level notifier when memory level callbacks set then component callbacks registered`() {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        justRun { context.registerComponentCallbacks(any()) }

        val memoryLevelNotifier = AndroidMemoryLevelNotifier(context)
        verify(exactly = 1) { context.applicationContext }

        // setting memoryLevelCallbacks calls registerComponentCallbacks
        memoryLevelNotifier.memoryLevelCallbacks = memoryLevelCallbacks
        verify(exactly = 1) { context.registerComponentCallbacks(any()) }

        // setting memoryLevelCallbacks again should not make another call to registerComponentCallbacks
        memoryLevelNotifier.memoryLevelCallbacks = memoryLevelCallbacks
        verify(exactly = 1) { context.registerComponentCallbacks(any()) }
        confirmVerified(context)
    }

    @Test
    fun `given android memory level notifier when memory level callbacks set to null then component callbacks unregistered`() {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        justRun { context.registerComponentCallbacks(any()) }
        justRun { context.unregisterComponentCallbacks(any()) }

        val memoryLevelNotifier = AndroidMemoryLevelNotifier(context)
        verify(exactly = 1) { context.applicationContext }

        memoryLevelNotifier.memoryLevelCallbacks = memoryLevelCallbacks
        verify(exactly = 1) { context.registerComponentCallbacks(any()) }

        memoryLevelNotifier.memoryLevelCallbacks = null
        verify(exactly = 1) { context.unregisterComponentCallbacks(any()) }

        // a duplicate set to null should not call unregisterComponentCallbacks again
        memoryLevelNotifier.memoryLevelCallbacks = null
        verify(exactly = 1) { context.unregisterComponentCallbacks(any()) }
        confirmVerified(context)
    }
}
