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
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * CNAuthorizationStatusLimited value for iOS 18+ partial contact access.
 *
 * This constant represents the raw value of CNAuthorizationStatusLimited (4),
 * which is not yet available in Kotlin/Native platform bindings as of 2026.
 *
 * Reference: Apple Documentation - CNAuthorizationStatus
 * @see <a href="https://developer.apple.com/documentation/contacts/cnauthorizationstatus/limited">CNAuthorizationStatusLimited</a>
 *
 * TODO: Remove this constant when Kotlin/Native bindings include CNAuthorizationStatusLimited
 */
private const val CN_AUTHORIZATION_STATUS_LIMITED = 4L

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

/**
 * Check if running iOS 18+
 */
@OptIn(ExperimentalForeignApi::class)
private fun isIOS18OrLater(): Boolean {
    val version = NSProcessInfo.processInfo.operatingSystemVersion
    return version.useContents { majorVersion >= 18L }
}

/**
 * Maps CNAuthorizationStatus to our cross-platform enum.
 * iOS 18+ adds CNAuthorizationStatusLimited for partial access.
 * 2026 Fix: Use runtime check for iOS 18+ to safely handle LIMITED status.
 */
private fun checkAuthorizationStatus(): ContactsAuthorizationStatus {
    val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
    return when (status) {
        CNAuthorizationStatusAuthorized -> ContactsAuthorizationStatus.AUTHORIZED
        CNAuthorizationStatusDenied -> ContactsAuthorizationStatus.DENIED
        CNAuthorizationStatusRestricted -> ContactsAuthorizationStatus.DENIED // Parental controls or MDM
        CNAuthorizationStatusNotDetermined -> ContactsAuthorizationStatus.NOT_DETERMINED
        else -> {
            // iOS 18+ returns CNAuthorizationStatusLimited for partial access
            if (isIOS18OrLater() && status == CN_AUTHORIZATION_STATUS_LIMITED) {
                ContactsAuthorizationStatus.LIMITED
            } else {
                ContactsAuthorizationStatus.NOT_DETERMINED
            }
        }
    }
}
