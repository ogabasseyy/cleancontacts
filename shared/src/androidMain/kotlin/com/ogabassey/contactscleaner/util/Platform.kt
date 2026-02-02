package com.ogabassey.contactscleaner.util

import java.util.UUID

actual fun getPlatformTimeMillis(): Long = System.currentTimeMillis()
actual fun getPlatformUUID(): String = UUID.randomUUID().toString()
