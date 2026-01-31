package com.ogabassey.contactscleaner.ui.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 2026 Best Practice: ContactLauncher with callback for Android.
 * Uses ActivityResultLauncher to detect when user returns from Contacts app.
 */
class AndroidContactLauncher(
    private val context: android.content.Context,
    private val launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) : ContactLauncher {
    override fun openContact(id: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id)
            intent.data = uri
            launcher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: just start activity without result tracking
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id)
                fallbackIntent.data = uri
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
}

@Composable
actual fun rememberContactLauncher(onReturn: () -> Unit): ContactLauncher {
    val context = LocalContext.current

    // 2026 Best Practice: Use ActivityResultLauncher to detect return from Contacts app
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // User returned from contacts app - trigger refresh callback
        onReturn()
    }

    return remember(context, launcher) { AndroidContactLauncher(context, launcher) }
}
