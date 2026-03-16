package com.ogabassey.contactscleaner.platform

/**
 * Android WhatsApp Detector configuration.
 *
 * The API key is injected via generated Secrets object from local.properties or environment variables.
 * This prevents hardcoding secrets in the source code.
 */
actual object WhatsAppDetectorConfig {
    actual val apiKey: String = Secrets.WHATSAPP_DETECTOR_API_KEY

    actual val baseUrl: String = Secrets.WHATSAPP_DETECTOR_BASE_URL

    actual val isConfigured: Boolean = apiKey.isNotBlank() && baseUrl.isNotBlank()
}
