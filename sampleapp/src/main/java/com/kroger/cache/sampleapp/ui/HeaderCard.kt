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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kroger.cache.sampleapp.R
import com.kroger.cache.sampleapp.ui.theme.CacheTheme

@Composable
internal fun HeaderCard(
    header: String,
    isExpandable: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = CacheTheme.dimensions.gutter)
            .fillMaxWidth(),
        elevation = 10.dp,
    ) {
        var isExpanded by rememberSaveable { mutableStateOf(true) }
        Column(Modifier.padding(CacheTheme.dimensions.padding)) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(enabled = isExpandable) {
                        isExpanded = !isExpanded
                    },
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.h5,
                    text = header,
                    textAlign = TextAlign.Center,
                )
                if (isExpandable) {
                    if (isExpanded) {
                        Icon(
                            Icons.Filled.ExpandLess,
                            stringResource(R.string.collapse),
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    } else {
                        Icon(
                            Icons.Filled.ExpandMore,
                            stringResource(R.string.expand),
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
            }
            Spacer(Modifier.height(CacheTheme.dimensions.padding))
            AnimatedVisibility(visible = isExpanded) {
                content()
            }
        }
    }
}
