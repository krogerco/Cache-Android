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
package com.kroger.moshi

import com.kroger.cache.CacheSerializer
import com.squareup.moshi.JsonAdapter
import javax.inject.Inject

/**
 * Implementation of [CacheSerializer] for [Moshi](https://github.com/square/moshi)
 *
 * @property adapter a moshi [JsonAdapter] for the desired type
 */
public class MoshiCacheSerializer<T> @Inject constructor(private val adapter: JsonAdapter<T>) : CacheSerializer<T> {
    override fun decodeFromByteArray(bytes: ByteArray?): T? = if (bytes == null || bytes.isEmpty()) {
        null
    } else {
        adapter.fromJson(bytes.decodeToString())
    }

    override fun toByteArray(data: T?): ByteArray = if (data == null) {
        ByteArray(0)
    } else {
        adapter.toJson(data).encodeToByteArray()
    }
}
