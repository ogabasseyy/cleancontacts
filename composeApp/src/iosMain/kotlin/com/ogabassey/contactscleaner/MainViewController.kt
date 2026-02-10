package com.ogabassey.contactscleaner

import androidx.compose.ui.window.ComposeUIViewController
import com.ogabassey.contactscleaner.data.repository.RevenueCatInitializer
import com.ogabassey.contactscleaner.di.KoinHelper
import com.ogabassey.contactscleaner.di.viewModelModule
import com.ogabassey.contactscleaner.platform.Logger
import platform.UIKit.UIViewController

/**
 * Initialize RevenueCat SDK.
 *
 * Per RevenueCat docs: "Configure Purchases once, early in your application lifecycle."
 * For SwiftUI, call this from App.init() — the earliest lifecycle point.
 * Exported so Swift can call: MainViewControllerKt.InitializeRevenueCat()
 */
fun InitializeRevenueCat() {
    if (RevenueCatInitializer.isConfigured()) return
    RevenueCatInitializer.initialize(
        appUserId = null, // Anonymous user, RevenueCat generates ID
        debugMode = false // Production ready
    )
}

/**
 * iOS entry point for Compose Multiplatform.
 *
 * 2026 KMP Best Practice: Uses real iOS Contacts implementation via CNContactStore.
 */
fun MainViewController(): UIViewController {
    // RevenueCat is already initialized from iOSApp.init() (earliest lifecycle point).
    // This is a safety fallback in case it wasn't called yet.
    InitializeRevenueCat()

    // Initialize Koin for iOS - critical for app functionality
    initKoinIos()

    return ComposeUIViewController {
        App()
    }
}

private var koinInitialized = false

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
