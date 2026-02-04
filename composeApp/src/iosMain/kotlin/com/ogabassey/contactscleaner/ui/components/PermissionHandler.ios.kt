package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusDenied
import platform.Contacts.CNAuthorizationStatusNotDetermined
import platform.Contacts.CNAuthorizationStatusRestricted
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import platform.Foundation.NSURL
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

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
                    // 2026 Fix: Dispatch state update to main thread for thread safety
                    dispatch_async(dispatch_get_main_queue()) {
                        authStatus = checkAuthorizationStatus()
                    }
                }
            },
            openSettings = {
                val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                if (url != null) {
                    // 2026 Fix: Use newer openURL API with completion handler
                    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>()) { _ -> }
                }
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun isIOS18OrLater(): Boolean {
    return try {
        val currentVersion = NSProcessInfo.processInfo.operatingSystemVersion
        currentVersion.useContents { majorVersion >= 18L }
    } catch (e: Exception) {
        // Fallback: assume iOS 18+ if check fails on newer iOS versions
        true
    }
}

/**
 * Maps CNAuthorizationStatus to our cross-platform enum.
 * iOS 18+ adds CNAuthorizationStatusLimited for partial access.
 */
private fun checkAuthorizationStatus(): ContactsAuthorizationStatus {
    return try {
        val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
        when (status) {
            CNAuthorizationStatusAuthorized -> ContactsAuthorizationStatus.AUTHORIZED
            CNAuthorizationStatusDenied -> ContactsAuthorizationStatus.DENIED
            CNAuthorizationStatusRestricted -> ContactsAuthorizationStatus.DENIED // Parental controls or MDM
            CNAuthorizationStatusNotDetermined -> ContactsAuthorizationStatus.NOT_DETERMINED
            else -> {
                // iOS 18+ may return CNAuthorizationStatusLimited (value 4)
                if (isIOS18OrLater() && status.toLong() == 4L) {
                    ContactsAuthorizationStatus.LIMITED
                } else {
                    ContactsAuthorizationStatus.NOT_DETERMINED
                }
            }
        }
    } catch (e: Exception) {
        // Defensive fallback if authorization check fails
        println("⚠️ Error checking contacts authorization: ${e.message}")
        ContactsAuthorizationStatus.NOT_DETERMINED
    }
}
