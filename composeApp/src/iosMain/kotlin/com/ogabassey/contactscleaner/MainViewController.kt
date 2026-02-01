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
    // Initialize Koin for iOS
    initKoinIos()

    // Initialize RevenueCat for in-app purchases
    initRevenueCat()

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

private fun initRevenueCat() {
    // 2026 Best Practice: Initialize RevenueCat after Koin
    // Debug mode enabled for development builds
    RevenueCatInitializer.initialize(
        appUserId = null, // Anonymous user, RevenueCat generates ID
        debugMode = true  // TODO: Set to false for production
    )
}
