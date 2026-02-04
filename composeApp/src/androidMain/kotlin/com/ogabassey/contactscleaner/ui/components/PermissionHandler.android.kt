package com.ogabassey.contactscleaner.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Android implementation using Accompanist Permissions.
 *
 * Note: Android doesn't have iOS 18+ style "limited" access for contacts.
 * Users either grant full access or deny completely.
 * For denied state, we provide openSettings to let users change their mind.
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
    val context = LocalContext.current

    // 2026 Fix: Compute authStatus inside remember to avoid stale closure
    return remember(
        permissionsState.allPermissionsGranted,
        permissionsState.shouldShowRationale,
        permissionsState.revokedPermissions.size,
        context
    ) {
        // Determine authorization status
        // Note: shouldShowRationale = true means user denied once but can still re-prompt
        // We treat it as NOT_DETERMINED so the app can re-request permission
        val authStatus = when {
            permissionsState.allPermissionsGranted -> ContactsAuthorizationStatus.AUTHORIZED
            permissionsState.shouldShowRationale -> {
                // User denied once but system allows re-prompting - treat as promptable
                ContactsAuthorizationStatus.NOT_DETERMINED
            }
            // If not granted, no rationale, but has revoked permissions -> permanently denied
            permissionsState.revokedPermissions.isNotEmpty() -> ContactsAuthorizationStatus.DENIED
            else -> ContactsAuthorizationStatus.NOT_DETERMINED
        }

        ContactsPermissionState(
            allPermissionsGranted = permissionsState.allPermissionsGranted,
            authorizationStatus = authStatus,
            shouldShowRationale = permissionsState.shouldShowRationale,
            launchRequest = {
                permissionsState.launchMultiplePermissionRequest()
            },
            openSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
    }
}
