package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.FormatIssue
import com.ogabassey.contactscleaner.platform.FormatAnalysis
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider

/**
 * Detects format issues in phone numbers (e.g., missing international prefix).
 *
 * 2026 KMP Best Practice: Uses platform abstractions for phone number parsing.
 */
class FormatDetector(
    private val phoneNumberHandler: PhoneNumberHandler,
    private val regionProvider: RegionProvider
) {

    /**
     * Checks if a number technically works as an international number but is missing the '+' prefix.
     * @param rawNumber The number from the contact (e.g. "23480...", "080...", "+234...")
     * @param defaultRegion The region to assume if ambiguous (e.g. "NG", "US").
     */
    fun analyze(rawNumber: String, defaultRegion: String? = null): FormatIssue? {
        val region = defaultRegion ?: regionProvider.getRegionIso()
        val analysis = phoneNumberHandler.analyzeFormatIssue(rawNumber, region) ?: return null

        return FormatIssue(
            normalizedNumber = analysis.normalizedNumber,
            countryCode = analysis.countryCode,
            regionCode = analysis.regionCode,
            displayCountry = analysis.displayCountry
        )
    }

    fun getCountryName(e164Number: String): String {
        return phoneNumberHandler.getCountryName(e164Number)
    }

    /**
     * Extracts the ISO region code (e.g. "US", "NG") from a number.
     */
    fun getRegionCode(number: String, defaultRegion: String? = null): String {
        val region = defaultRegion ?: regionProvider.getRegionIso()
        return phoneNumberHandler.getRegionCode(number, region)
    }
}
