package com.ogabassey.contactscleaner.platform

/**
 * Android RevenueCat configuration.
 *
 * 2026 Best Practice: Store API keys securely.
 * TODO: Replace with your actual RevenueCat Google Play API key from:
 * RevenueCat Dashboard -> Project Settings -> API Keys -> Google Play
 */
actual object RevenueCatConfig {
    // TODO: Replace with your RevenueCat Google Play public API key
    // Currently unused - using MockBillingRepository
    actual val apiKey: String = "goog_YOUR_REVENUECAT_GOOGLE_API_KEY"

    // Must match entitlement ID in RevenueCat dashboard
    actual val premiumEntitlementId: String = "premium"
}
