package com.ogabassey.contactscleaner.ui.components

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Android implementation using Accompanist Permissions.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberContactsPermissionState(): ContactsPermissionState {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
    )
    val context = androidx.compose.ui.platform.LocalContext.current

    return remember(permissionsState.allPermissionsGranted, permissionsState.shouldShowRationale, permissionsState, context) {
        ContactsPermissionState(
            allPermissionsGranted = permissionsState.allPermissionsGranted,
            shouldShowRationale = permissionsState.shouldShowRationale,
            launchRequest = { 
                permissionsState.launchMultiplePermissionRequest() 
            }
        )
    }
}
