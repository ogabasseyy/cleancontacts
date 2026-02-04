package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusDenied
import platform.Contacts.CNAuthorizationStatusNotDetermined
import platform.Contacts.CNAuthorizationStatusLimited
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS implementation using CNContactStore.
 *
 * Apple Guideline 5.1.1: Supports iOS 18+ limited access mode.
 * When limited, app can only access contacts the user explicitly shared.
 */
@Composable
actual fun rememberContactsPermissionState(): ContactsPermissionState {
    var authStatus by remember { mutableStateOf(ContactsAuthorizationStatus.NOT_DETERMINED) }

    // Initial check
    LaunchedEffect(Unit) {
        authStatus = checkAuthorizationStatus()
    }

    return remember(authStatus) {
        ContactsPermissionState(
            allPermissionsGranted = authStatus == ContactsAuthorizationStatus.AUTHORIZED,
            authorizationStatus = authStatus,
            shouldShowRationale = false,
            launchRequest = {
                val store = CNContactStore()
                store.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { _, _ ->
                    // Re-check status after request (could be limited or full)
                    authStatus = checkAuthorizationStatus()
                }
            },
            openSettings = {
                val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                if (url != null) {
                    UIApplication.sharedApplication.openURL(url)
                }
            }
        )
    }
}

/**
 * Maps CNAuthorizationStatus to our cross-platform enum.
 * iOS 18+ adds CNAuthorizationStatusLimited for partial access.
 */
private fun checkAuthorizationStatus(): ContactsAuthorizationStatus {
    val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
    return when (status) {
        CNAuthorizationStatusAuthorized -> ContactsAuthorizationStatus.AUTHORIZED
        CNAuthorizationStatusLimited -> ContactsAuthorizationStatus.LIMITED
        CNAuthorizationStatusDenied -> ContactsAuthorizationStatus.DENIED
        CNAuthorizationStatusNotDetermined -> ContactsAuthorizationStatus.NOT_DETERMINED
        else -> ContactsAuthorizationStatus.NOT_DETERMINED
    }
}
