package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.platform.Logger
import com.ogabassey.contactscleaner.platform.RevenueCatConfig
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases

/**
 * RevenueCat SDK initializer for KMP.
 *
 * 2026 Best Practice: Initialize RevenueCat early in app lifecycle.
 * Call [initialize] from platform-specific app entry point.
 */
object RevenueCatInitializer {

    private const val TAG = "RevenueCat"
    private var isInitialized = false

    /**
     * Initialize the RevenueCat SDK.
     * Must be called before any billing operations.
     *
     * @param appUserId Optional user ID for identifying users. Pass null for anonymous users.
     * @param debugMode Enable verbose logging for development.
     */
    fun initialize(appUserId: String? = null, debugMode: Boolean = false) {
        if (isInitialized) {
            Logger.d(TAG, "RevenueCat already initialized, skipping")
            return
        }

        try {
            // Set log level before configuration
            Purchases.logLevel = if (debugMode) LogLevel.DEBUG else LogLevel.ERROR

            // Configure RevenueCat with platform-specific API key
            Purchases.configure(apiKey = RevenueCatConfig.apiKey) {
                this.appUserId = appUserId
            }

            isInitialized = true
            Logger.d(TAG, "RevenueCat SDK initialized successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize RevenueCat: ${e.message}")
        }
    }

    /**
     * Check if RevenueCat has been initialized.
     */
    fun isConfigured(): Boolean = isInitialized
}
