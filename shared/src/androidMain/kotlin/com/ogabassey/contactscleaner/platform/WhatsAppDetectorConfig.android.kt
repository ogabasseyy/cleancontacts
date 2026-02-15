package com.ogabassey.contactscleaner.platform

/**
 * Android WhatsApp Detector configuration.
 *
 * The API key can be configured via environment variable (WHATSAPP_DETECTOR_API_KEY)
 * or uses the default value. For CI/CD, set the environment variable.
 */
actual object WhatsAppDetectorConfig {
    actual val apiKey: String = run {
        System.getenv("WHATSAPP_DETECTOR_API_KEY")?.takeIf { it.isNotEmpty() }
            ?: "ROTATED_KEY_REDACTED"
    }

    actual val baseUrl: String = run {
        System.getenv("WHATSAPP_DETECTOR_BASE_URL")?.takeIf { it.isNotEmpty() }
            ?: "https://api.contactscleaner.tech"
    }

    init {
        if (apiKey.isEmpty()) {
            throw IllegalStateException(
                "WhatsApp Detector API key not configured. " +
                "Set WHATSAPP_DETECTOR_API_KEY environment variable or update the default value."
            )
        }
    }
}
