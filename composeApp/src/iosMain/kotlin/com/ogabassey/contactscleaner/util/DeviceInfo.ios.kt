package com.ogabassey.contactscleaner.util

import platform.UIKit.UIDevice
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalForeignApi::class)
actual object DeviceInfo {
    actual val deviceModel: String = memScoped {
        val systemInfo = alloc<utsname>()
        uname(systemInfo.ptr)
        systemInfo.machine.toKString()
    }
    actual val osVersion: String = "iOS ${UIDevice.currentDevice.systemVersion}"
    actual val platformName: String = "iOS"
}
