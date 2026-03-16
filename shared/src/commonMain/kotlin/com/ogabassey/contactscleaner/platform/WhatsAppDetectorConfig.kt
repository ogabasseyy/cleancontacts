package com.ogabassey.contactscleaner.platform

/**
 * WhatsApp Detector API configuration for KMP.
 *
 * 2026 Best Practice: Use expect/actual for platform-specific config resolution.
 * The API key and base URL are the same across platforms, but the override
 * mechanism (env vars vs. Info.plist) is platform-specific.
 */
expect object WhatsAppDetectorConfig {
    /**
     * API key for the WhatsApp Detector service.
     * Used in X-API-Key header for HTTP and WebSocket requests.
     */
    val apiKey: String

    /**
     * Base URL for the WhatsApp Detector service.
     * e.g., "https://api.contactscleaner.tech"
     */
    val baseUrl: String

    /**
     * Whether the WhatsApp detector is configured for this build.
     * Used to keep the app launchable when the integration is intentionally absent.
     */
    val isConfigured: Boolean
}
