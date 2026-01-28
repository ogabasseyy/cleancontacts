package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class IosContactLauncher : ContactLauncher {
    override fun openContact(id: String) {
        // TODO: Implement iOS specific contact opening
        // Typically requires jumping to Swift or using CNContactViewController
        // For now, no-op to prevent crashes
        println("Open contact requested for ID: $id (iOS implementation pending)")
    }
}

@Composable
actual fun rememberContactLauncher(): ContactLauncher {
    return remember { IosContactLauncher() }
}
