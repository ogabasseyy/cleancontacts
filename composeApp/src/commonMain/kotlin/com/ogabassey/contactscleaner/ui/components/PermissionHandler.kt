package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Composable

/**
 * Authorization status for contacts access.
 *
 * Apple Guideline 5.1.1: Apps must work with limited or no access.
 * iOS 18+ supports .limited authorization where users share only some contacts.
 */
enum class ContactsAuthorizationStatus {
    /** User hasn't been asked yet */
    NOT_DETERMINED,
    /** User denied all access */
    DENIED,
    /** iOS 18+: User granted access to some contacts only */
    LIMITED,
    /** Full access to all contacts */
    AUTHORIZED
}

/**
 * Permission state for contacts access.
 *
 * 2026 KMP Best Practice: Abstract permission handling across platforms.
 * Apple Guideline 5.1.1: Support limited access mode.
 */
data class ContactsPermissionState(
    val allPermissionsGranted: Boolean,
    val authorizationStatus: ContactsAuthorizationStatus = ContactsAuthorizationStatus.NOT_DETERMINED,
    val shouldShowRationale: Boolean = false,
    val launchRequest: () -> Unit = {},
    /** Opens system settings for the app (useful when denied) */
    val openSettings: () -> Unit = {}
)

/**
 * Platform-specific contacts permission handler.
 *
 * On Android: Uses Accompanist permissions
 * On iOS: Uses CNContactStore authorization with iOS 18+ limited access support
 */
@Composable
expect fun rememberContactsPermissionState(): ContactsPermissionState
