package com.ogabassey.contactscleaner.util

import android.os.Build

actual object DeviceInfo {
    actual val deviceModel: String = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
        Build.MODEL
    } else {
        "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    actual val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    actual val platformName: String = "Android"
}
