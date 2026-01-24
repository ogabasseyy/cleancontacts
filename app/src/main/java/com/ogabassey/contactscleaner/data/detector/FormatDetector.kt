package com.ogabassey.contactscleaner.data.detector

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class FormatIssue(
    val normalizedNumber: String,
    val countryCode: Int,
    val regionCode: String,
    val displayCountry: String
)

private val NON_DIGIT_REGEX = Regex("[^0-9]")

@Singleton
class FormatDetector @Inject constructor() {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    /**
     * Checks if a number technically works as an international number but is missing the '+' prefix.
     * @param rawNumber The number from the contact (e.g. "23480...", "080...", "+234...")
     * @param defaultRegion The region to assume if ambiguous (e.g. "NG", "US"). 
     *                      Usually inferred from SIM or Locale. Defaulting to users Locale or "US" as fallback.
     */
    fun analyze(rawNumber: String, defaultRegion: String = Locale.getDefault().country): FormatIssue? {
        if (rawNumber.isBlank()) return null
        
        // 1. If it already starts with +, it's already international format
        if (rawNumber.startsWith("+")) return null
        
        // Clean the number (remove spaces, dashes, etc.)
        val cleanedNumber = rawNumber.replace(NON_DIGIT_REGEX, "")
        
        // 2. SPECIAL CASE: Nigerian numbers starting with 234
        // Nigerian format: 234 + 10 digit local number = 13 digits total
        // e.g., "2348012345678" should become "+2348012345678"
        if (cleanedNumber.startsWith("234") && cleanedNumber.length == 13) {
            val normalized = "+$cleanedNumber"
            // Validate with libphonenumber to confirm it's valid
            try {
                val proto = phoneUtil.parse(normalized, "ZZ")
                if (phoneUtil.isValidNumber(proto)) {
                    return FormatIssue(
                        normalizedNumber = normalized,
                        countryCode = 234,
                        regionCode = "NG",
                        displayCountry = "Nigeria"
                    )
                }
            } catch (e: Exception) {
                // Even if libphonenumber fails, trust our pattern
                return FormatIssue(
                    normalizedNumber = normalized,
                    countryCode = 234,
                    regionCode = "NG",
                    displayCountry = "Nigeria"
                )
            }
        }
        
        // 3. For other numbers, use libphonenumber but ONLY if the suggestion matches original
        // This prevents wrong suggestions like 22871089329 → +2347081643445
        val plusNumber = "+$cleanedNumber"
        try {
            val proto = phoneUtil.parse(plusNumber, "ZZ")
            
            if (phoneUtil.isValidNumber(proto)) {
                val formatted = phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)
                
                // CRITICAL: Only accept if E164 format matches "+originalNumber"
                // This prevents libphonenumber from "correcting" to a different number
                if (formatted == plusNumber) {
                    val regionCode = phoneUtil.getRegionCodeForNumber(proto) ?: "Unknown"
                    val countryName = if (regionCode != "Unknown") {
                        Locale.Builder().setRegion(regionCode).build().displayCountry
                    } else {
                        ""
                    }
                    
                    return FormatIssue(
                        normalizedNumber = formatted,
                        countryCode = proto.countryCode,
                        regionCode = regionCode,
                        displayCountry = countryName.ifBlank { "Region +${proto.countryCode}" }
                    )
                }
            }
        } catch (e: Exception) {
            // Ignored
        }

        return null
    }

    fun getCountryName(e164Number: String): String {
        try {
            // Parse with "+" if missing, though E164 should have it.
            val numStr = if (e164Number.startsWith("+")) e164Number else "+$e164Number"
            val proto = phoneUtil.parse(numStr, "ZZ")
            val region = phoneUtil.getRegionCodeForNumber(proto)
            return if (region != null && region != "Unknown") {
                val country = Locale.Builder().setRegion(region).build().displayCountry
                if (country.isNotBlank()) "$country (+${proto.countryCode})" else "Region +${proto.countryCode}"
            } else {
                "Unknown Region"
            }
        } catch (e: Exception) {
            return "Unknown Region"
        }
    }

    /**
     * Extracts the ISO region code (e.g. "US", "NG") from a number.
     * Useful for sorting/grouping.
     */
    fun getRegionCode(number: String, defaultRegion: String = Locale.getDefault().country): String {
        return try {
            val numStr = if (number.startsWith("+")) number else "+$number"
            // Try E164 Style first
            var proto = phoneUtil.parse(numStr, "ZZ")
            // If failed, try local
            if (!phoneUtil.isValidNumber(proto)) {
                 proto = phoneUtil.parse(number, defaultRegion)
            }
            phoneUtil.getRegionCodeForNumber(proto) ?: "ZZ"
        } catch (e: Exception) {
            "ZZ"
        }
    }
}
