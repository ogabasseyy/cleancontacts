package com.ogabassey.contactscleaner.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val PREFS_NAME = "permission_prefs"
private const val KEY_CONTACTS_PERMISSION_REQUESTED = "contacts_permission_requested"

/**
 * Android implementation using Accompanist Permissions.
 *
 * 2026 Best Practice: Uses SharedPreferences to track if permission was ever requested,
 * solving the Android platform limitation where shouldShowRationale returns false
 * for both "fresh install" and "permanently denied" states.
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
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Track if we've ever requested permissions (persisted across app restarts)
    var hasEverRequestedPermission by remember {
        mutableStateOf(prefs.getBoolean(KEY_CONTACTS_PERMISSION_REQUESTED, false))
    }

    return remember(
        permissionsState.allPermissionsGranted,
        permissionsState.shouldShowRationale,
        hasEverRequestedPermission,
        context
    ) {
        // Determine authorization status using the 2026 best practice state machine:
        //
        // | shouldShowRationale | isGranted | hasEverRequested | Result           |
        // |---------------------|-----------|------------------|------------------|
        // | any                 | true      | any              | AUTHORIZED       |
        // | true                | false     | any              | NOT_DETERMINED   |
        // | false               | false     | false            | NOT_DETERMINED   |
        // | false               | false     | true             | DENIED           |

        val authStatus = when {
            permissionsState.allPermissionsGranted -> ContactsAuthorizationStatus.AUTHORIZED

            // User denied once but system allows re-prompting - treat as promptable
            permissionsState.shouldShowRationale -> ContactsAuthorizationStatus.NOT_DETERMINED

            // Not granted, no rationale shown...
            // If we've never requested -> fresh install -> NOT_DETERMINED
            // If we have requested before -> permanently denied -> DENIED
            hasEverRequestedPermission -> ContactsAuthorizationStatus.DENIED

            // Fresh install - treat as NOT_DETERMINED
            else -> ContactsAuthorizationStatus.NOT_DETERMINED
        }

        ContactsPermissionState(
            allPermissionsGranted = permissionsState.allPermissionsGranted,
            authorizationStatus = authStatus,
            shouldShowRationale = permissionsState.shouldShowRationale,
            launchRequest = {
                // Mark that we've requested permission before launching
                prefs.edit().putBoolean(KEY_CONTACTS_PERMISSION_REQUESTED, true).apply()
                hasEverRequestedPermission = true
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
