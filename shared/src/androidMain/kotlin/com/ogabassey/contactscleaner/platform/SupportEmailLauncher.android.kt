package com.ogabassey.contactscleaner.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object SupportEmailLauncher : KoinComponent {
    private val context: Context by inject()

    actual fun composeEmail(address: String, subject: String, body: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(address)}")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) == null) {
                Logger.e("SupportEmailLauncher", "No email app available for support email")
                return false
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Logger.e("SupportEmailLauncher", "Failed to open support email composer", e)
            false
        }
    }
}
