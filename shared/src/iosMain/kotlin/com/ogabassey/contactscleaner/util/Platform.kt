package com.ogabassey.contactscleaner.util

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun getPlatformTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
// 2026 Fix: Normalize to lowercase to match Android's UUID.randomUUID().toString() format
actual fun getPlatformUUID(): String = NSUUID().UUIDString().lowercase()
