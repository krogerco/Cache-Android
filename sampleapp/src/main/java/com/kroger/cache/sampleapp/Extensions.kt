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
package com.kroger.cache.sampleapp

import com.kroger.cache.CachePolicy
import com.kroger.cache.internal.CacheEntry
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * @param cachePolicy [CachePolicy] to use to calculate the temporal age of the [CacheEntry]
 * @return the age of the [CacheEntry] as a [Duration]
 */
fun <K, V> CacheEntry<K, V>.temporalAge(cachePolicy: CachePolicy): Duration =
    when {
        cachePolicy.hasTtiPolicy -> (System.currentTimeMillis() - lastAccessDate).toDuration(DurationUnit.MILLISECONDS)
        cachePolicy.hasTtlPolicy -> (System.currentTimeMillis() - creationDate).toDuration(DurationUnit.MILLISECONDS)
        else -> Duration.INFINITE
    }

/**
 * @param cachePolicy [CachePolicy] to use when checking if the [CacheEntry] is expired
 * @return true if the [CacheEntry] is expired, false otherwise
 */
fun <K, V> CacheEntry<K, V>.isExpired(cachePolicy: CachePolicy): Boolean =
    when {
        cachePolicy.hasTtiPolicy -> temporalAge(cachePolicy) > cachePolicy.entryTti
        cachePolicy.hasTtlPolicy -> temporalAge(cachePolicy) > cachePolicy.entryTtl
        else -> false
    }

/**
 * @param config [CacheConfig] to convert into a [CachePolicy]
 */
fun CachePolicy.Companion.from(config: CacheConfig?): CachePolicy =
    builder().apply {
        if (config != null) {
            config.tti?.let { entryTti(it.toDuration(DurationUnit.SECONDS)) }
            config.ttl?.let { entryTtl(it.toDuration(DurationUnit.SECONDS)) }
            maxSize(config.maxSize)
        }
    }.build()
