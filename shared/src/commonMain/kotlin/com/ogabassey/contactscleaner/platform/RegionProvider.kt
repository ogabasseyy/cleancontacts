package com.ogabassey.contactscleaner.platform

/**
 * Platform abstraction for getting the device's default region.
 *
 * 2026 KMP Best Practice: Use interface + platform implementations for DI.
 * - Android: Uses TelephonyManager or Locale
 * - iOS: Uses NSLocale
 */
interface RegionProvider {
    /**
     * Gets the ISO 3166-1 alpha-2 country code for the device's region.
     * @return Country code like "US", "NG", "GB", etc.
     */
    fun getRegionIso(): String

    /**
     * Gets the display name for a region code.
     */
    fun getDisplayCountry(regionCode: String): String
}
