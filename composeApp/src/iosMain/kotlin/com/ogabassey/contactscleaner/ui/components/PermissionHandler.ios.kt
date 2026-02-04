package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType

/**
 * iOS implementation using CNContactStore.
 */
@Composable
actual fun rememberContactsPermissionState(): ContactsPermissionState {
    var permissionGranted by remember { mutableStateOf(false) }
    
    // Initial check
    LaunchedEffect(Unit) {
        val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
        permissionGranted = status == CNAuthorizationStatusAuthorized
    }

    return remember(permissionGranted) {
        ContactsPermissionState(
            allPermissionsGranted = permissionGranted,
            shouldShowRationale = false,
            launchRequest = {
                val store = CNContactStore()
                store.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                     permissionGranted = granted
                }
            }
        )
    }
}
