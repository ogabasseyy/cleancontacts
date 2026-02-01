package com.ogabassey.contactscleaner.platform

/**
 * Platform abstraction for opening URLs in the system browser.
 *
 * 2026 KMP Best Practice: Use expect/actual for platform-specific URL handling.
 */
expect object UrlOpener {
    /**
     * Opens a URL in the default system browser.
     * @param url The URL to open (must be a valid http/https URL)
     */
    fun openUrl(url: String)
}

/**
 * App legal URLs for Terms of Use and Privacy Policy.
 *
 * Apple App Store Review Guideline 3.1.2 requires:
 * - Functional links to Terms of Use (EULA)
 * - Functional links to Privacy Policy
 * - These must be accessible from within the app
 */
object LegalUrls {
    const val PRIVACY_POLICY = "https://contactscleaner.tech/privacy"
    const val TERMS_OF_USE = "https://contactscleaner.tech/terms"

    // Apple's standard EULA (can be used if you don't have custom terms)
    const val APPLE_STANDARD_EULA = "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"
}
