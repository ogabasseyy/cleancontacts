package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSLog

/**
 * iOS implementation using NSLog.
 */
actual object Logger {
    actual fun d(tag: String, message: String) {
        NSLog("[$tag] DEBUG: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val errorMsg = if (throwable != null) "$message: ${throwable.message}" else message
        NSLog("[$tag] ERROR: $errorMsg")
    }

    actual fun w(tag: String, message: String) {
        NSLog("[$tag] WARN: $message")
    }

    actual fun i(tag: String, message: String) {
        NSLog("[$tag] INFO: $message")
    }
}
