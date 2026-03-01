package com.ogabassey.contactscleaner.util

import android.os.Build

actual object DeviceInfo {
    actual val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    actual val osVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    actual val platformName: String = "Android"
}
