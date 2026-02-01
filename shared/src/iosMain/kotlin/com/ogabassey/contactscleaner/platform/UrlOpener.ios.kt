package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation of URL opener.
 *
 * 2026 KMP Best Practice: Uses modern UIApplication.openURL API on main thread.
 */
actual object UrlOpener {

    actual fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            Logger.e("UrlOpener", "Invalid URL: $url")
            return
        }
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(
                nsUrl,
                emptyMap<Any?, Any?>(),
                null
            )
        }
    }
}
