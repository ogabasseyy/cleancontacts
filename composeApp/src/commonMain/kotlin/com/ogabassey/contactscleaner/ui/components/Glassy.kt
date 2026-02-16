package com.ogabassey.contactscleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ogabassey.contactscleaner.ui.theme.withPlatformAlpha

// ⚡ Bolt Optimization: Pre-calculate constant resources to avoid allocation on every recomposition.
private val ShadowColor = Color.Black.copy(alpha = 0.8f)

private val GlassBackgroundBrush by lazy {
    Brush.verticalGradient(
        colors = listOf(
            Color.White.withPlatformAlpha(0.12f),
            Color.White.withPlatformAlpha(0.04f)
        )
    )
}

private val GlassBorderBrush by lazy {
    Brush.linearGradient(
        colors = listOf(
            Color.White.withPlatformAlpha(0.3f),
            Color.White.withPlatformAlpha(0.05f)
        )
    )
}

/**
 * Glassmorphism effect modifier.
 *
 * 2026 KMP Best Practice: Cross-platform UI effects with platform-specific adjustments.
 * Uses platform-specific alpha multipliers to ensure consistent appearance on iOS and Android.
 */
fun Modifier.glassy(
    radius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    color: Color = Color.Transparent,
    strokeColor: Color = Color.Transparent
): Modifier {
    val showCustomBorder = strokeColor != Color.Transparent
    // ⚡ Bolt Optimization: Reuse shape instance
    val shape = RoundedCornerShape(radius)

    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = ShadowColor,
            spotColor = ShadowColor
        )
        .clip(shape)
        .background(color) // Apply base tint
        .background(GlassBackgroundBrush)
        .border(
            width = borderWidth,
            brush = if (showCustomBorder) {
                SolidColor(strokeColor)
            } else {
                GlassBorderBrush
            },
            shape = shape
        )
}
