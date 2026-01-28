package com.ogabassey.contactscleaner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Platform-specific color multipliers for glassmorphism effects.
 *
 * 2026 KMP Best Practice: expect/actual for platform-specific rendering adjustments.
 * iOS renders alpha blending differently than Android, requiring adjusted values.
 */
expect object PlatformColor {
    /**
     * Multiplier for alpha values in glass effects.
     * iOS typically needs higher alpha values to achieve the same visual effect.
     */
    val glassAlphaMultiplier: Float

    /**
     * Multiplier for surface alpha values.
     */
    val surfaceAlphaMultiplier: Float
}

/**
 * Apply platform-specific alpha adjustment to a color.
 */
fun Color.withPlatformAlpha(baseAlpha: Float): Color {
    val adjustedAlpha = (baseAlpha * PlatformColor.glassAlphaMultiplier).coerceIn(0f, 1f)
    return this.copy(alpha = adjustedAlpha)
}

/**
 * Apply platform-specific surface alpha adjustment to a color.
 */
fun Color.withPlatformSurfaceAlpha(baseAlpha: Float): Color {
    val adjustedAlpha = (baseAlpha * PlatformColor.surfaceAlphaMultiplier).coerceIn(0f, 1f)
    return this.copy(alpha = adjustedAlpha)
}
