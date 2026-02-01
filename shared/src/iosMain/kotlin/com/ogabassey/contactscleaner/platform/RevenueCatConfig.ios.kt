package com.ogabassey.contactscleaner.platform

/**
 * iOS RevenueCat configuration.
 *
 * 2026 Best Practice: Store API keys securely.
 * TODO: Replace with your actual RevenueCat App Store API key from:
 * RevenueCat Dashboard -> Project Settings -> API Keys -> App Store
 */
actual object RevenueCatConfig {
    // TODO: Replace with your RevenueCat App Store public API key
    actual val apiKey: String = "appl_YOUR_REVENUECAT_APPLE_API_KEY"

    // Must match entitlement ID in RevenueCat dashboard
    actual val premiumEntitlementId: String = "premium"
}
