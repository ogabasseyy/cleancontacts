package com.ogabassey.contactscleaner

import androidx.compose.ui.window.ComposeUIViewController
import com.ogabassey.contactscleaner.data.repository.RevenueCatInitializer
import com.ogabassey.contactscleaner.di.KoinHelper
import com.ogabassey.contactscleaner.di.viewModelModule
import com.ogabassey.contactscleaner.platform.Logger
import platform.UIKit.UIViewController

/**
 * iOS entry point for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Uses real iOS Contacts implementation via CNContactStore.
 */
fun MainViewController(): UIViewController {
    // Initialize RevenueCat BEFORE Koin to ensure BillingRepository
    // can access Purchases.sharedInstance during its own initialization.
    // Note: RevenueCat failure is non-fatal - billing features will be unavailable
    try {
        initRevenueCat()
    } catch (e: IllegalStateException) {
        Logger.e("MainViewController", "RevenueCat initialization failed: ${e.message}")
    }

    // Initialize Koin for iOS - critical for app functionality
    initKoinIos()

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
    // RevenueCat initialized before Koin so BillingRepository can access Purchases.sharedInstance
    RevenueCatInitializer.initialize(
        appUserId = null, // Anonymous user, RevenueCat generates ID
        debugMode = false // Production ready
    )
    revenueCatInitialized = true
}
