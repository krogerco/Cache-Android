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
@file:OptIn(ExperimentalFoundationApi::class)

package com.kroger.cache.sampleapp.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kroger.cache.CachePolicy
import com.kroger.cache.internal.CacheEntry
import com.kroger.cache.sampleapp.R
import com.kroger.cache.sampleapp.TemporalPolicy
import com.kroger.cache.sampleapp.isExpired
import com.kroger.cache.sampleapp.temporalAge
import com.kroger.cache.sampleapp.ui.theme.CacheTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SampleAppScreen(
    kotlinViewModel: SampleAppViewModelKotlin = viewModel(),
    moshiViewModel: SampleAppViewModelMoshi = viewModel(),
) {
    var tabIndex: Int by rememberSaveable { mutableStateOf(0) }
    val tabItems = listOf(SampleSerializer.Kotlin, SampleSerializer.Moshi)
    var serializer: SampleSerializer by remember { mutableStateOf(SampleSerializer.Kotlin) }
    val localViewModel: ViewModelContract = when (serializer) {
        is SampleSerializer.Kotlin -> kotlinViewModel
        is SampleSerializer.Moshi -> moshiViewModel
    }
    val uiState: SampleAppUiState by localViewModel.uiState.collectAsStateWithLifecycle()

    Column {
        TabRow(selectedTabIndex = tabIndex) {
            tabItems.forEachIndexed { index, tabItem ->
                Tab(
                    selected = index == tabIndex,
                    onClick = {
                        tabIndex = index
                        serializer = tabItem
                    },
                    text = { Text(text = tabItem.name) },
                )
            }
        }

        SampleAppScreenContent(
            uiState = uiState,
            temporalPolicy = localViewModel.temporalPolicy,
            maxSize = localViewModel.maxSize,
            isMaxSizeValid = localViewModel.isMaxSizeValid,
            temporalTime = localViewModel.temporalTime,
            isTemporalTimeValid = localViewModel.isTemporalTimeValid,
            updateMaxSize = localViewModel::updateMaxSize,
            onUpdateTemporalTime = localViewModel::updateTemporalTime,
            onUpdateTemporalPolicy = localViewModel::updateTemporalPolicy,
            onApplyCacheOptions = localViewModel::applyCacheOptions,
            onDeleteEntry = localViewModel::deleteEntry,
            onGetEntry = localViewModel::getEntry,
            onUpdateEntryWithRandomValue = localViewModel::updateEntryWithRandomValue,
            onAddRandomEntries = localViewModel::addRandomEntries,
        )
    }
}

@Composable
private fun SampleAppScreenContent(
    uiState: SampleAppUiState,
    temporalPolicy: TemporalPolicy = TemporalPolicy.NONE,
    maxSize: String = "0",
    isMaxSizeValid: Boolean = false,
    temporalTime: String = "0",
    isTemporalTimeValid: Boolean = false,
    updateMaxSize: (String) -> Unit = {},
    onUpdateTemporalTime: (String) -> Unit = {},
    onUpdateTemporalPolicy: (TemporalPolicy) -> Unit = {},
    onApplyCacheOptions: () -> Unit = {},
    onDeleteEntry: (String) -> Unit = {},
    onGetEntry: (String) -> Unit = { },
    onUpdateEntryWithRandomValue: (String) -> Unit = {},
    onAddRandomEntries: (Int) -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(vertical = CacheTheme.dimensions.gutter),
            verticalArrangement = Arrangement.spacedBy(CacheTheme.dimensions.gutter),
        ) {
            CacheOptions(
                temporalPolicy = temporalPolicy,
                maxSize = maxSize,
                isMaxSizeValid = isMaxSizeValid,
                temporalTime = temporalTime,
                isTemporalTimeValid = isTemporalTimeValid,
                updateMaxSize = updateMaxSize,
                updateTemporalTime = onUpdateTemporalTime,
                updateTemporalPolicy = onUpdateTemporalPolicy,
                onApplyCacheOptions = onApplyCacheOptions,
            )

            Row(
                modifier = Modifier.padding(start = CacheTheme.dimensions.gutter),
                horizontalArrangement = Arrangement.spacedBy(CacheTheme.dimensions.padding),
            ) {
                Button(onClick = { onAddRandomEntries(1) }) {
                    Text(stringResource(R.string.add_one))
                }
                Button(onClick = { onAddRandomEntries(10) }) {
                    Text(stringResource(R.string.add_ten))
                }
            }

            CacheEntries(
                uiState,
                onDeleteEntry,
                onGetEntry,
                onUpdateEntryWithRandomValue,
            )
        }
    }
}

@Composable
private fun CacheOptions(
    temporalPolicy: TemporalPolicy = TemporalPolicy.NONE,
    maxSize: String = "0",
    isMaxSizeValid: Boolean = false,
    temporalTime: String = "0",
    isTemporalTimeValid: Boolean = false,
    updateMaxSize: (String) -> Unit = {},
    updateTemporalTime: (String) -> Unit = {},
    updateTemporalPolicy: (TemporalPolicy) -> Unit = {},
    onApplyCacheOptions: () -> Unit = {},
) {
    HeaderCard(header = stringResource(R.string.cache_options), isExpandable = true) {
        Column {
            TextField(
                value = maxSize,
                onValueChange = updateMaxSize,
                modifier = Modifier.fillMaxWidth(),
                isError = isMaxSizeValid.not(),
                label = { Text(stringResource(R.string.max_size_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            if (isMaxSizeValid.not()) {
                ErrorText(stringResource(R.string.error_size))
            }
            Spacer(modifier = Modifier.height(CacheTheme.dimensions.paddingLarge))
            Text(
                text = stringResource(R.string.temporal_policy),
                style = MaterialTheme.typography.h6,
            )
            TextField(
                value = temporalTime,
                enabled = temporalPolicy != TemporalPolicy.NONE,
                onValueChange = updateTemporalTime,
                isError = isTemporalTimeValid.not(),
                label = { Text(stringResource(R.string.time_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            if (isTemporalTimeValid.not()) {
                ErrorText(stringResource(R.string.error_time))
            }
            Spacer(modifier = Modifier.height(CacheTheme.dimensions.padding))
            Column(
                Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(CacheTheme.dimensions.paddingSmall),
            ) {
                TextRadioButton(text = stringResource(R.string.temporal_policy_none), isSelected = temporalPolicy == TemporalPolicy.NONE) {
                    updateTemporalPolicy(TemporalPolicy.NONE)
                }
                TextRadioButton(text = stringResource(R.string.temporal_policy_tti), temporalPolicy == TemporalPolicy.TTI) {
                    updateTemporalPolicy(TemporalPolicy.TTI)
                }
                TextRadioButton(text = stringResource(R.string.temporal_policy_ttl), temporalPolicy == TemporalPolicy.TTL) {
                    updateTemporalPolicy(TemporalPolicy.TTL)
                }
            }

            Button(
                onClick = onApplyCacheOptions,
                modifier = Modifier.align(Alignment.End),
                enabled = isMaxSizeValid && isTemporalTimeValid,
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    }
}

@Composable
private fun CacheEntries(
    uiState: SampleAppUiState,
    onDeleteEntry: (String) -> Unit = { },
    onGetEntry: (String) -> Unit = { },
    onUpdateEntryWithRandomValue: (String) -> Unit = { },
) {
    HeaderCard(header = stringResource(R.string.cache_entries)) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(CacheTheme.dimensions.gutter),
        ) {
            items(uiState.cacheEntries, key = { it.key }) { entry ->
                CacheEntry(
                    entry,
                    uiState.cachePolicy,
                    onGetEntry,
                    onUpdateEntryWithRandomValue,
                    onDeleteEntry,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.CacheEntry(
    entry: CacheEntry<String, String>,
    cachePolicy: CachePolicy,
    onGetEntry: (String) -> Unit,
    onUpdateEntryWithRandomValue: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    var temporalAge by remember {
        mutableStateOf(entry.temporalAge(cachePolicy))
    }

    val isExpired by remember(temporalAge) {
        mutableStateOf(entry.isExpired(cachePolicy))
    }

    LaunchedEffect(entry, cachePolicy) {
        while (true) {
            delay(200.milliseconds)
            temporalAge = entry.temporalAge(cachePolicy)
        }
    }
    Column(
        Modifier
            .border(
                width = Dp.Hairline,
                color = Color.Black,
                shape = RoundedCornerShape(CacheTheme.dimensions.padding),
            )
            .background(
                color = if (isExpired) expiredBackground else MaterialTheme.colors.background,
                shape = RoundedCornerShape(CacheTheme.dimensions.padding),
            )
            .padding(start = CacheTheme.dimensions.gutter)
            .animateItemPlacement(),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onGetEntry(entry.key) }) {
                Icon(
                    Icons.Filled.SystemUpdateAlt,
                    stringResource(R.string.get),
                )
            }
            IconButton(onClick = { onUpdateEntryWithRandomValue(entry.key) }) {
                Icon(
                    Icons.Filled.Update,
                    stringResource(R.string.update_random_value),
                )
            }
            IconButton(onClick = { onDeleteEntry(entry.key) }) {
                Icon(
                    Icons.Filled.Delete,
                    stringResource(R.string.delete),
                )
            }
        }

        TextLabel(stringResource(R.string.created_at_label))
        TextValue(epochMillisFormatted(entry.creationDate))

        TextLabel(stringResource(R.string.last_accessed_label))
        TextValue(epochMillisFormatted(entry.lastAccessDate))

        TextLabel(stringResource(R.string.key_label))
        TextValue(entry.key)

        TextLabel(stringResource(R.string.value_label))
        TextValue(entry.value)

        if (cachePolicy.hasTtiPolicy || cachePolicy.hasTtlPolicy) {
            TextLabel(stringResource(R.string.temporal_age_label))
            TextValue(temporalAge.inWholeSeconds.toString())
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun epochMillisFormatted(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)

    return DateTimeFormatter.ISO_LOCAL_DATE_TIME
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

@Composable
private fun TextLabel(
    label: String,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = label,
        fontWeight = fontWeight,
        maxLines = 1,
    )
}

@Composable
private fun TextValue(
    value: String,
) {
    Text(
        value,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ErrorText(error: String) {
    Text(
        text = error,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.caption,
    )
}

@Composable
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun CacheOptionsPreview() {
    CacheTheme {
        val uiState = SampleAppUiState(
            listOf(
                CacheEntry("entry 1 key", "entry 1 value", 0L, 0L),
                CacheEntry("entry 2 key", "entry 2 value", 0L, 0L),
            ),
            CachePolicy.builder().build(),
        )
        SampleAppScreenContent(uiState = uiState)
    }
}

private val expiredBackground = Color(0xFFFF8886)
