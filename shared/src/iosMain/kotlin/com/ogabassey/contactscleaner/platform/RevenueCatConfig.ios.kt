package com.ogabassey.contactscleaner.platform

/**
 * iOS RevenueCat configuration.
 */
actual object RevenueCatConfig {
    actual val apiKey: String = "test_cgwNwoQkAcPhlosEgrFpRcsNMpW"

    // Must match entitlement ID in RevenueCat dashboard
    actual val premiumEntitlementId: String = "premium"

    init {
        if (apiKey.contains("YOUR_REVENUECAT") || apiKey.isEmpty()) {
            println("⚠️ [RevenueCat] API key not configured. Billing features will not work.")
        }
    }
}
