package com.ogabassey.contactscleaner.platform

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.localizedStringForCountryCode

/**
 * iOS implementation using NSLocale.
 */
class IosRegionProvider : RegionProvider {

    override fun getRegionIso(): String {
        return NSLocale.currentLocale.countryCode ?: "US"
    }

    override fun getDisplayCountry(regionCode: String): String {
        return NSLocale.currentLocale.localizedStringForCountryCode(regionCode) ?: regionCode
    }
}
