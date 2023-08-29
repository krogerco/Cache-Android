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

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.kroger.cache.MemoryLevelCallbacks
import com.kroger.cache.MemoryLevelNotifier

/**
 * Listens for [ComponentCallbacks2.onTrimMemory] callbacks. When the trim level is
 * [ComponentCallbacks2.TRIM_MEMORY_MODERATE] or [ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW]
 * then [MemoryLevelCallbacks.onLowMemory] is called. When the trim level is [ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL]
 * or [ComponentCallbacks2.TRIM_MEMORY_COMPLETE] then [MemoryLevelCallbacks.onCriticalMemory] is called.
 * When [memoryLevelCallbacks] is null then [Context.unregisterComponentCallbacks] is called to avoid leaking
 * this object. Therefore, once the callbacks are no longer needed it is important to set [memoryLevelCallbacks] to null.
 *
 * @param context used to call [Context.getApplicationContext] and then [Context.registerComponentCallbacks]
 * to listen to trim memory events.
 */
public class AndroidMemoryLevelNotifier(
    context: Context,
) : MemoryLevelNotifier {
    private val appContext = context.applicationContext
    private var isRegistered = false

    private val componentCallbacks: ComponentCallbacks2 = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) { }

        override fun onLowMemory() { }

        override fun onTrimMemory(level: Int) {
            when (level) {
                ComponentCallbacks2.TRIM_MEMORY_MODERATE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                    memoryLevelCallbacks?.onLowMemory()
                }
                ComponentCallbacks2.TRIM_MEMORY_COMPLETE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                    memoryLevelCallbacks?.onCriticalMemory()
                }
                else -> Unit
            }
        }
    }

    override var memoryLevelCallbacks: MemoryLevelCallbacks? = null
        set(value) {
            field = value
            if (field != null && isRegistered.not()) {
                appContext.registerComponentCallbacks(componentCallbacks)
                isRegistered = true
            } else if (field == null && isRegistered) {
                appContext.unregisterComponentCallbacks(componentCallbacks)
                isRegistered = false
            }
        }
}
