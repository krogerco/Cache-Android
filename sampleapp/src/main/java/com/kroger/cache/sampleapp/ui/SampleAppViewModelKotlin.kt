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
package com.kroger.cache.sampleapp.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kroger.cache.Cache
import com.kroger.cache.CachePolicy
import com.kroger.cache.MemoryCacheManagerBuilder
import com.kroger.cache.SnapshotPersistentCache
import com.kroger.cache.android.extensions.from
import com.kroger.cache.internal.CacheEntry
import com.kroger.cache.sampleapp.CacheConfig
import com.kroger.cache.sampleapp.FlowPersistentCache
import com.kroger.cache.sampleapp.TemporalPolicy
import com.kroger.cache.sampleapp.di.Kotlinx
import com.kroger.cache.sampleapp.from
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SampleAppViewModelKotlin @Inject constructor(
    @Kotlinx private val configFileCache: SnapshotPersistentCache<CacheConfig>,
    @Kotlinx private val flowPersistentCache: FlowPersistentCache<List<CacheEntry<String, String>>>,
    private val application: Application,
) : ViewModel(), ViewModelContract {
    private lateinit var cache: Cache<String, String>
    private var cacheCoroutineScope: CoroutineScope? = null
    private val cacheConfigFlow: MutableStateFlow<CacheConfig?> = MutableStateFlow(null)
    private val cachePolicyFlow = cacheConfigFlow
        .map(CachePolicy.Companion::from)
        .onEach(::createNewCache)

    override val uiState: StateFlow<SampleAppUiState> = flowPersistentCache.data
        .combine(cachePolicyFlow) { cacheEntries, cachePolicy ->
            val sortedEntries = cacheEntries.orEmpty().sortedBy {
                if (cachePolicy.hasTtiPolicy) {
                    it.lastAccessDate
                } else {
                    it.creationDate
                }
            }
            SampleAppUiState(sortedEntries, cachePolicy)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SampleAppUiState(emptyList(), CachePolicy.builder().build()),
        )

    override var maxSize: String by mutableStateOf("")
        private set

    override val isMaxSizeValid by derivedStateOf {
        (maxSize.toIntOrNull() ?: -1) > 0
    }

    override var temporalTime: String by mutableStateOf("")
        private set

    override val isTemporalTimeValid by derivedStateOf {
        temporalPolicy == TemporalPolicy.NONE ||
                (temporalTime.toIntOrNull() ?: -1) > 0
    }

    override var temporalPolicy: TemporalPolicy by mutableStateOf(TemporalPolicy.NONE)
        private set

    init {
        viewModelScope.launch {
            val cacheConfig = configFileCache.read() ?: CacheConfig()
            cacheConfigFlow.update { cacheConfig }
            updateCacheConfigState(cacheConfig)
        }
    }

    override fun updateTemporalPolicy(temporalPolicy: TemporalPolicy) {
        this.temporalPolicy = temporalPolicy
    }

    override fun updateMaxSize(maxSize: String) {
        this.maxSize = maxSize
    }

    override fun updateTemporalTime(temporalTime: String) {
        this.temporalTime = temporalTime
    }

    override fun applyCacheOptions() {
        val temporalTime = temporalTime.toIntOrNull() ?: 0
        val tti = temporalTime.takeIf { temporalPolicy == TemporalPolicy.TTI }
        val ttl = temporalTime.takeIf { temporalPolicy == TemporalPolicy.TTL }
        val cacheConfig = CacheConfig(ttl, tti, maxSize.toIntOrNull() ?: 0)
        viewModelScope.launch {
            configFileCache.save(cacheConfig)
            cacheConfigFlow.update { cacheConfig }
        }
    }

    override fun addRandomEntries(count: Int) {
        viewModelScope.launch {
            repeat(count) {
                cache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString())
                delay(100.milliseconds)
            }
        }
    }

    override fun deleteEntry(key: String) {
        viewModelScope.launch { cache.remove(key) }
    }

    override fun getEntry(key: String) {
        viewModelScope.launch { cache.get(key) }
    }

    override fun updateEntryWithRandomValue(key: String) {
        viewModelScope.launch { cache.put(key, UUID.randomUUID().toString()) }
    }

    private fun updateCacheConfigState(cacheConfig: CacheConfig) {
        maxSize = cacheConfig.maxSize.toString()
        temporalTime = when {
            cacheConfig.tti != null -> {
                temporalPolicy = TemporalPolicy.TTI
                cacheConfig.tti.toString()
            }

            cacheConfig.ttl != null -> {
                temporalPolicy = TemporalPolicy.TTL
                cacheConfig.ttl.toString()
            }

            else -> {
                temporalPolicy = TemporalPolicy.NONE
                ""
            }
        }
    }

    private fun createNewCache(cachePolicy: CachePolicy) {
        cacheCoroutineScope?.cancel()
        cacheCoroutineScope = CoroutineScope(EmptyCoroutineContext).also { scope ->
            cache = MemoryCacheManagerBuilder.from(
                application,
                flowPersistentCache,
            )
                .coroutineScope(scope)
                .cachePolicy(cachePolicy)
                .saveFrequency(Duration.ZERO)
                .build()
        }
    }

    override fun onCleared() {
        cacheCoroutineScope?.cancel()
    }
}

