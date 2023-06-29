package com.kroger.cache.sampleapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val stroke: Dp = 1.dp,
    val paddingSmall: Dp = 4.dp,
    val padding: Dp = 8.dp,
    val paddingLarge: Dp = 16.dp,
    val paddingVeryLarge: Dp = 32.dp,
    val gutter: Dp = 16.dp,
    val buttonHeight: Dp = 48.dp,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
