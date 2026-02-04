package com.ogabassey.contactscleaner.platform

import android.content.Intent
import android.net.Uri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import android.content.Context

/**
 * Android implementation of URL opener.
 *
 * 2026 KMP Best Practice: Uses Intent.ACTION_VIEW with FLAG_ACTIVITY_NEW_TASK.
 */
actual object UrlOpener : KoinComponent {

    private val context: Context by inject()

    actual fun openUrl(url: String) {
        try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()

            // 2026 Security: Validate scheme to prevent open redirect/malicious intents
            if (scheme != "http" && scheme != "https") {
                Logger.e("UrlOpener", "Blocked unsafe URL scheme: $scheme in $url")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Logger.e("UrlOpener", "Failed to open URL: $url - ${e.message}")
        }
    }
}
