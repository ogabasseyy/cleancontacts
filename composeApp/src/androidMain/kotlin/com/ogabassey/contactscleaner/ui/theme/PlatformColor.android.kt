package com.ogabassey.contactscleaner.ui.theme

/**
 * Android implementation of platform-specific color adjustments.
 *
 * 2026 KMP Best Practice: Default multipliers for Android (sRGB color space).
 */
actual object PlatformColor {
    /**
     * Android uses default alpha values (1.0 multiplier).
     */
    actual val glassAlphaMultiplier: Float = 1.0f

    /**
     * Android uses default surface alpha values.
     */
    actual val surfaceAlphaMultiplier: Float = 1.0f
}
