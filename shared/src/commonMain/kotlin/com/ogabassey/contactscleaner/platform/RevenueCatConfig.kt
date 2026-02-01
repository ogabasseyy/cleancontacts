package com.ogabassey.contactscleaner.platform

/**
 * RevenueCat configuration for KMP.
 *
 * 2026 Best Practice: Use expect/actual for platform-specific API keys.
 * Get your API keys from RevenueCat Dashboard -> Project Settings -> API Keys
 */
expect object RevenueCatConfig {
    /**
     * Platform-specific RevenueCat public API key.
     * - Android: Google Play API key
     * - iOS: App Store API key
     */
    val apiKey: String

    /**
     * Entitlement identifier for premium access.
     * Must match the entitlement ID configured in RevenueCat dashboard.
     */
    val premiumEntitlementId: String
}
