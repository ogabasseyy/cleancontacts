package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSBundle

/**
 * iOS WhatsApp Detector configuration.
 *
 * The API key can be configured via Info.plist (WHATSAPP_DETECTOR_API_KEY) or falls back
 * to the default value. For different environments, override via Info.plist.
 */
actual object WhatsAppDetectorConfig {
    actual val apiKey: String = run {
        val plistKey = NSBundle.mainBundle.objectForInfoDictionaryKey("WHATSAPP_DETECTOR_API_KEY") as? String
        plistKey?.takeIf { it.isNotEmpty() }
            ?: "e59ec0ca77c64b123d56e683c92e7009c0cbaf0d393dc916b1856ead3b063332"
    }

    actual val baseUrl: String = run {
        val plistUrl = NSBundle.mainBundle.objectForInfoDictionaryKey("WHATSAPP_DETECTOR_BASE_URL") as? String
        plistUrl?.takeIf { it.isNotEmpty() }
            ?: "https://api.contactscleaner.tech"
    }

    init {
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "WhatsApp Detector API key not configured. " +
                "Set WHATSAPP_DETECTOR_API_KEY in Info.plist or update the default value."
            )
        }
    }
}
