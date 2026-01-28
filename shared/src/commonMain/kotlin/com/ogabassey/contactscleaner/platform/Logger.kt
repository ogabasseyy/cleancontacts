package com.ogabassey.contactscleaner.platform

/**
 * Platform abstraction for logging.
 *
 * 2026 KMP Best Practice: Abstract platform logging APIs.
 * - Android: Uses android.util.Log
 * - iOS: Uses NSLog or os_log
 */
expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String)
    fun i(tag: String, message: String)
}
