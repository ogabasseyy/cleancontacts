package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation of URL opener.
 *
 * 2026 KMP Best Practice: Uses UIApplication.sharedApplication.openURL.
 */
actual object UrlOpener {

    actual fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: run {
            Logger.e("UrlOpener", "Invalid URL: $url")
            return
        }

        if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(nsUrl)
        } else {
            Logger.e("UrlOpener", "Cannot open URL: $url")
        }
    }
}
