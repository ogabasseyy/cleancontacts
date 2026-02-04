package com.ogabassey.contactscleaner

import androidx.compose.ui.window.ComposeUIViewController
import com.ogabassey.contactscleaner.data.repository.RevenueCatInitializer
import com.ogabassey.contactscleaner.di.KoinHelper
import com.ogabassey.contactscleaner.di.viewModelModule
import platform.UIKit.UIViewController

/**
 * iOS entry point for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Uses real iOS Contacts implementation via CNContactStore.
 */
fun MainViewController(): UIViewController {
    // Initialize SDK dependencies with defensive error handling
    try {
        // Initialize RevenueCat BEFORE Koin to ensure BillingRepository
        // can access Purchases.sharedInstance during its own initialization.
        initRevenueCat()
    } catch (e: Exception) {
        println("⚠️ RevenueCat initialization failed: ${e.message}")
        // Continue without RevenueCat - billing features will be unavailable
    }

    try {
        // Initialize Koin for iOS
        initKoinIos()
    } catch (e: Exception) {
        println("⚠️ Koin initialization failed: ${e.message}")
        // This is critical - app may not function properly
    }

    return ComposeUIViewController {
        App()
    }
}

private var koinInitialized = false
private var revenueCatInitialized = false

private fun initKoinIos() {
    if (!koinInitialized) {
        // 2026 Best Practice: Real implementations provided by iosModule
        // ContactRepository is provided by IosContactRepository using CNContactStore
        KoinHelper.initKoin(
            additionalModules = listOf(viewModelModule)
        )
        koinInitialized = true
    }
}

private fun initRevenueCat() {
    if (revenueCatInitialized) return
    // 2026 Best Practice: Initialize RevenueCat after Koin
    RevenueCatInitializer.initialize(
        apiKey = com.ogabassey.contactscleaner.platform.RevenueCatConfig.apiKey,
        appUserId = null, // Anonymous user, RevenueCat generates ID
        debugMode = false // Production ready
    )
    revenueCatInitialized = true
}
