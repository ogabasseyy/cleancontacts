package com.ogabassey.contactscleaner.ui.theme

/**
 * iOS implementation of platform-specific color adjustments.
 *
 * 2026 KMP Best Practice: Adjusted multipliers for iOS (P3 wide color space).
 * iOS renders alpha blending differently, requiring higher values for visibility.
 */
actual object PlatformColor {
    /**
     * iOS needs approximately 1.5x alpha for glass effects to match Android appearance.
     * This compensates for iOS's wider P3 color space and different blending behavior.
     */
    actual val glassAlphaMultiplier: Float = 1.5f

    /**
     * iOS surface alpha multiplier for better surface visibility.
     */
    actual val surfaceAlphaMultiplier: Float = 1.3f
}
