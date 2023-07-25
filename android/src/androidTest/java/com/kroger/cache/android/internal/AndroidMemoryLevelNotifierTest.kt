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
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.kroger.cache.MemoryLevelCallbacks
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * These tests are in name ascending order because trim-memory cannot be set to
 * a higher trim level without a delay. The order avoids needing the delay.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
internal class AndroidMemoryLevelNotifierTest {
    @Test(timeout = 2000L)
    fun a_given_android_memory_level_callbacks_when_low_memory_then_on_low_memory_called() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryLevelNotifier = AndroidMemoryLevelNotifier(
            context,
        )

        val lowMemoryCalled = suspendCoroutine {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            memoryLevelNotifier.memoryLevelCallbacks = object : MemoryLevelCallbacks {
                override fun onLowMemory() {
                    it.resume(true)
                }
            }

            device.executeShellCommand("am send-trim-memory com.kroger.cache.android.test RUNNING_LOW")
        }

        Truth.assertThat(lowMemoryCalled).isTrue()
    }

    @Test(timeout = 2000L)
    fun b_given_android_memory_level_callbacks_when_critical_memory_then_on_critical_memory_called() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryLevelNotifier = AndroidMemoryLevelNotifier(
            context,
        )
        val criticalMemoryCalled = suspendCoroutine {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            memoryLevelNotifier.memoryLevelCallbacks = object : MemoryLevelCallbacks {
                override fun onCriticalMemory() {
                    it.resume(true)
                }
            }

            device.executeShellCommand("am send-trim-memory com.kroger.cache.android.test RUNNING_CRITICAL")
        }

        Truth.assertThat(criticalMemoryCalled).isTrue()
    }
}
