package com.ogabassey.contactscleaner.platform

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Android implementation using Google's libphonenumber.
 */
actual class PhoneNumberHandler actual constructor() {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    actual fun normalizeToE164(number: String, defaultRegion: String): String {
        return try {
            val parsedNumber = phoneUtil.parse(number, defaultRegion)
            if (phoneUtil.isValidNumber(parsedNumber)) {
                phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else {
                number.filter { it.isDigit() || it == '+' }
            }
        } catch (e: Exception) {
            number.filter { it.isDigit() || it == '+' }
        }
    }

    actual fun isValidNumber(number: String, region: String): Boolean {
        return try {
            val proto = phoneUtil.parse(number, region)
            phoneUtil.isValidNumber(proto)
        } catch (e: Exception) {
            false
        }
    }

    actual fun analyzeFormatIssue(rawNumber: String, defaultRegion: String): FormatAnalysis? {
        if (rawNumber.isBlank()) return null

        // If it already starts with +, it's already international format
        if (rawNumber.startsWith("+")) return null

        // Clean the number (remove spaces, dashes, etc.)
        val cleanedNumber = rawNumber.replace(Regex("[^0-9]"), "")

        // Special case: Nigerian numbers starting with 234
        if (cleanedNumber.startsWith("234") && cleanedNumber.length == 13) {
            val normalized = "+$cleanedNumber"
            try {
                val proto = phoneUtil.parse(normalized, "ZZ")
                if (phoneUtil.isValidNumber(proto)) {
                    return FormatAnalysis(
                        normalizedNumber = normalized,
                        countryCode = 234,
                        regionCode = "NG",
                        displayCountry = "Nigeria"
                    )
                }
            } catch (e: Exception) {
                // Even if libphonenumber fails, trust our pattern
                return FormatAnalysis(
                    normalizedNumber = normalized,
                    countryCode = 234,
                    regionCode = "NG",
                    displayCountry = "Nigeria"
                )
            }
        }

        // For other numbers, use libphonenumber
        val plusNumber = "+$cleanedNumber"
        try {
            val proto = phoneUtil.parse(plusNumber, "ZZ")

            if (phoneUtil.isValidNumber(proto)) {
                val formatted = phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)

                // Only accept if E164 format matches "+originalNumber"
                if (formatted == plusNumber) {
                    val regionCode = phoneUtil.getRegionCodeForNumber(proto) ?: "Unknown"
                    val countryName = if (regionCode != "Unknown") {
                        Locale.Builder().setRegion(regionCode).build().displayCountry
                    } else {
                        ""
                    }
                    return FormatAnalysis(
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

    actual fun getCountryName(e164Number: String): String {
        return try {
            val numStr = if (e164Number.startsWith("+")) e164Number else "+$e164Number"
            val proto = phoneUtil.parse(numStr, "ZZ")
            val region = phoneUtil.getRegionCodeForNumber(proto)
            if (region != null && region != "Unknown") {
                val country = Locale.Builder().setRegion(region).build().displayCountry
                if (country.isNotBlank()) "$country (+${proto.countryCode})" else "Region +${proto.countryCode}"
            } else {
                "Unknown Region"
            }
        } catch (e: Exception) {
            "Unknown Region"
        }
    }

    actual fun getRegionCode(number: String, defaultRegion: String): String {
        return try {
            val numStr = if (number.startsWith("+")) number else "+$number"
            var proto = phoneUtil.parse(numStr, "ZZ")
            if (!phoneUtil.isValidNumber(proto)) {
                proto = phoneUtil.parse(number, defaultRegion)
            }
            phoneUtil.getRegionCodeForNumber(proto) ?: "ZZ"
        } catch (e: Exception) {
            "ZZ"
        }
    }
}
