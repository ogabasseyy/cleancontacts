package com.ogabassey.contactscleaner.ui.util

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidContactLauncher(private val context: android.content.Context) : ContactLauncher {
    override fun openContact(id: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id)
            intent.data = uri
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
actual fun rememberContactLauncher(): ContactLauncher {
    val context = LocalContext.current
    return remember(context) { AndroidContactLauncher(context) }
}
