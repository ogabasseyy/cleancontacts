package com.ogabassey.contactscleaner.platform

import android.util.Log

/**
 * Android implementation using android.util.Log.
 */
actual object Logger {
    actual fun d(tag: String, message: String) {
        logSafely { Log.d(tag, message) } ?: println("D/$tag: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        logSafely {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } ?: run {
            println("E/$tag: $message")
            throwable?.printStackTrace()
        }
    }

    actual fun w(tag: String, message: String) {
        logSafely { Log.w(tag, message) } ?: println("W/$tag: $message")
    }

    actual fun i(tag: String, message: String) {
        logSafely { Log.i(tag, message) } ?: println("I/$tag: $message")
    }

    private fun logSafely(write: () -> Int): Int? {
        return try {
            write()
        } catch (_: RuntimeException) {
            null
        }
    }
}
