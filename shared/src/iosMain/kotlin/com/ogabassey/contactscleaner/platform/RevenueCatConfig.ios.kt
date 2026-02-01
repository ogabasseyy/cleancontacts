package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSBundle

/**
 * iOS RevenueCat configuration.
 *
 * The API key can be configured via Info.plist (REVENUECAT_API_KEY) or falls back
 * to the default value. For different environments, override via Info.plist.
 */
actual object RevenueCatConfig {
    actual val apiKey: String = run {
        // Try to read from Info.plist first, fall back to default
        val plistKey = NSBundle.mainBundle.objectForInfoDictionaryKey("REVENUECAT_API_KEY") as? String
        plistKey?.takeIf { it.isNotEmpty() } ?: "appl_vmizWISapAnNocTguqHShpbggNq"
    }

    // Must match entitlement ID in RevenueCat dashboard
    actual val premiumEntitlementId: String = "Contacts Cleaner Pro"

    // Defensive validation: Currently unreachable due to hardcoded fallback,
    // but kept as a safety check in case the fallback is removed or changed
    // to null/empty in the future. Ensures apiKey is never a placeholder.
    init {
        if (apiKey.contains("YOUR_REVENUECAT") || apiKey.isEmpty()) {
            throw IllegalStateException(
                "RevenueCat API key not configured. " +
                "Set REVENUECAT_API_KEY in Info.plist or update the default value."
            )
        }
    }
}
