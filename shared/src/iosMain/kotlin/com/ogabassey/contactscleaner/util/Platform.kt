package com.ogabassey.contactscleaner.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getPlatformTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
