package com.ogabassey.contactscleaner.platform

/**
 * Android RevenueCat configuration.
 *
 * The API key can be configured via environment variable (REVENUECAT_API_KEY)
 * or uses the default value. For CI/CD, set the environment variable.
 *
 * Note: RevenueCat public API keys are safe to embed in client apps.
 * The server/secret key should never be in source code.
 */
actual object RevenueCatConfig {
    actual val apiKey: String = run {
        // Try environment variable first, fall back to default
        System.getenv("REVENUECAT_API_KEY")?.takeIf { it.isNotEmpty() }
            ?: "goog_LygFhrickDOqQmVhowvVQTzfvSv"
    }

    // Must match entitlement ID in RevenueCat dashboard
    actual val premiumEntitlementId: String = "Contacts Cleaner Pro"

    init {
        if (apiKey.contains("YOUR_REVENUECAT") || apiKey.isEmpty()) {
            throw IllegalStateException(
                "RevenueCat API key not configured. " +
                "Set REVENUECAT_API_KEY environment variable or update the default value."
            )
        }
    }
}
