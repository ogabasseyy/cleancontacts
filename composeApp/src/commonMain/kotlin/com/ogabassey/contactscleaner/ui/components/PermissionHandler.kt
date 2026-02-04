package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Composable

/**
 * Permission state for contacts access.
 *
 * 2026 KMP Best Practice: Abstract permission handling across platforms.
 */
data class ContactsPermissionState(
    val allPermissionsGranted: Boolean,
    val shouldShowRationale: Boolean = false,
    val launchRequest: () -> Unit = {}
)

/**
 * Platform-specific contacts permission handler.
 *
 * On Android: Uses Accompanist permissions
 * On iOS: Uses CNContactStore authorization
 */
@Composable
expect fun rememberContactsPermissionState(): ContactsPermissionState
