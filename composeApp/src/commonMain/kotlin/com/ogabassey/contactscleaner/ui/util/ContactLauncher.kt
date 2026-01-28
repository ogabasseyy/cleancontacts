package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable

interface ContactLauncher {
    fun openContact(id: String)
}

@Composable
expect fun rememberContactLauncher(): ContactLauncher
