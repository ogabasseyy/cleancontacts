package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object SupportEmailLauncher {
    actual fun composeEmail(address: String, subject: String, body: String): Boolean {
        val encodedSubject = percentEncode(subject)
        val encodedBody = percentEncode(body)
        val url = NSURL.URLWithString("mailto:$address?subject=$encodedSubject&body=$encodedBody")
            ?: return false

        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(
                url,
                emptyMap<Any?, Any?>(),
                null
            )
        }

        return true
    }

    private fun percentEncode(value: String): String {
        val bytes = value.encodeToByteArray()
        val encoded = StringBuilder(bytes.size * 3)

        bytes.forEach { byte ->
            val code = byte.toInt() and 0xFF
            val isUnreserved =
                (code in 'A'.code..'Z'.code) ||
                    (code in 'a'.code..'z'.code) ||
                    (code in '0'.code..'9'.code) ||
                    code == '-'.code ||
                    code == '_'.code ||
                    code == '.'.code ||
                    code == '~'.code

            if (isUnreserved) {
                encoded.append(code.toChar())
            } else {
                encoded.append('%')
                encoded.append(code.toString(16).uppercase().padStart(2, '0'))
            }
        }

        return encoded.toString()
    }
}
