package com.ogabassey.contactscleaner.util

/**
 * Platform-specific device information for feedback reports.
 */
expect object DeviceInfo {
    val deviceModel: String
    val osVersion: String
    val platformName: String
}
