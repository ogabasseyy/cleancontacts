package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable

interface ContactLauncher {
    fun openContact(id: String)
}

/**
 * 2026 Best Practice: ContactLauncher with optional refresh callback.
 * Called when user returns from editing a contact in the native Contacts app.
 */
@Composable
expect fun rememberContactLauncher(onReturn: () -> Unit): ContactLauncher
