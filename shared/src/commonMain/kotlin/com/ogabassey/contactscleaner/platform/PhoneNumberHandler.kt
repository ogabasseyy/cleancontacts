package com.ogabassey.contactscleaner.platform

/**
 * Platform abstraction for phone number operations.
 *
 * 2026 KMP Best Practice: Use expect/actual for platform-specific libraries.
 * - Android: Uses Google's libphonenumber
 * - iOS: Will use a compatible iOS phone number library or manual parsing
 */
expect class PhoneNumberHandler() {
    /**
     * Normalizes a phone number to E.164 format.
     * @param number The raw phone number string
     * @param defaultRegion The ISO 3166-1 alpha-2 country code (e.g., "US", "NG")
     * @return The normalized E.164 number or a digit-filtered fallback
     */
    fun normalizeToE164(number: String, defaultRegion: String): String

    /**
     * Checks if a phone number is valid for a given region.
     */
    fun isValidNumber(number: String, region: String): Boolean

    /**
     * Analyzes a number to detect if it needs format standardization (missing + prefix).
     * @return A FormatAnalysis result or null if no issue detected
     */
    fun analyzeFormatIssue(rawNumber: String, defaultRegion: String): FormatAnalysis?

    /**
     * Gets the country/region name for a given E.164 number.
     */
    fun getCountryName(e164Number: String): String

    /**
     * Gets the ISO region code for a number.
     */
    fun getRegionCode(number: String, defaultRegion: String): String
}

/**
 * Result of format analysis for a phone number.
 */
data class FormatAnalysis(
    val normalizedNumber: String,
    val countryCode: Int,
    val regionCode: String,
    val displayCountry: String
)
