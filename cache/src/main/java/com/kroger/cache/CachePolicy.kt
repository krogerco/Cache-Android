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

import kotlin.time.Duration

/**
 * A cache policy that optionally enforces a max size and temporal policy.
 *
 * @property maxSize the maximize number of entries the cache can hold.
 * @property entryTtl (time to live) how long an entry remains in the cache after insertion before being purged.
 * @property entryTti (time to idle) how long after an entry is last accessed before being purged.
 */
public data class CachePolicy internal constructor(
    public val maxSize: Int = DEFAULT_SIZE_POLICY,
    public val entryTtl: Duration = DEFAULT_DURATION_POLICY,
    public val entryTti: Duration = DEFAULT_DURATION_POLICY,
) {
    /**
     * true if [maxSize] is enforced, otherwise false.
     */
    val hasMaxSizePolicy: Boolean = maxSize != DEFAULT_SIZE_POLICY

    /**
     * true if [entryTtl] is enforced, otherwise false.
     */
    val hasTtlPolicy: Boolean = entryTtl != DEFAULT_DURATION_POLICY

    /**
     * true if [entryTti] is enforced, otherwise false.
     */
    val hasTtiPolicy: Boolean = entryTti != DEFAULT_DURATION_POLICY

    /**
     * Builder to create a [CachePolicy]. Only a single temporal policy can be enabled at a time,
     * either a time to live policy based on an entry's insertion time or a time to idle policy
     * based on an entry's last access time.
     */
    public class CachePolicyBuilder internal constructor() {
        private var maxSize: Int = DEFAULT_SIZE_POLICY
        private var entryTtl: Duration = DEFAULT_DURATION_POLICY
        private var entryTti: Duration = DEFAULT_DURATION_POLICY

        /**
         * @param maxSize the maximize number of entries the cache can hold.
         */
        public fun maxSize(maxSize: Int): CachePolicyBuilder = apply {
            require(maxSize > 0) {
                "maxSize must be > 0. maxSize = $maxSize"
            }
            this.maxSize = maxSize
        }

        /**
         * @param entryTtl (time to live) how long an entry remains in the cache after insertion before being purged.
         */
        public fun entryTtl(entryTtl: Duration): CachePolicyBuilder = apply {
            require(entryTtl.isPositive()) {
                "entryTtl must be > 0. entryTtl = ${entryTtl.toIsoString()}"
            }
            check(entryTti == DEFAULT_DURATION_POLICY) {
                "entryTtl cannot be set after entryTti has been set. entryTti = ${entryTti.toIsoString()}"
            }
            this.entryTtl = entryTtl
        }

        /**
         * @param entryTti (time to idle) how long after an entry is last accessed before being purged.
         */
        public fun entryTti(entryTti: Duration): CachePolicyBuilder = apply {
            require(entryTti.isPositive()) {
                "entryTti must be > 0. entryTti = ${entryTti.toIsoString()}"
            }
            check(entryTtl == DEFAULT_DURATION_POLICY) {
                "entryTti cannot be set after entryTtl has been set. entryTtl = ${entryTtl.toIsoString()}"
            }
            this.entryTti = entryTti
        }

        public fun build(): CachePolicy = CachePolicy(
            maxSize = maxSize,
            entryTtl = entryTtl,
            entryTti = entryTti,
        )
    }

    public companion object {
        internal const val DEFAULT_SIZE_POLICY = -1
        internal val DEFAULT_DURATION_POLICY = Duration.INFINITE

        public fun builder(): CachePolicyBuilder = CachePolicyBuilder()
    }
}
