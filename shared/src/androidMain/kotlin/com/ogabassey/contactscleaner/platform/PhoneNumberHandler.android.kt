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
                fallbackNormalize(number)
            }
        } catch (e: Exception) {
            fallbackNormalize(number)
        }
    }

    // ⚡ Bolt Optimization: Single pass character loop for fallback normalization.
    // Replaces number.filter { it.isDigit() || it == '+' }
    // Eliminates intermediate String and List allocations.
    private fun fallbackNormalize(number: String): String {
        if (number.isEmpty()) return ""
        val sb = StringBuilder(number.length)
        for (i in number.indices) {
            val c = number[i]
            if (c in '0'..'9' || c == '+') {
                sb.append(c)
            }
        }
        return sb.toString()
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

        // ⚡ Bolt Optimization: Single pass character loop
        // Combines first digit check (O(N) search) and digit filtering (O(N) + allocations) into one pass.
        // This eliminates the intermediate List/String allocations of `filter` and iterates the string once.
        var firstDigit: Char? = null
        val sb = StringBuilder(rawNumber.length)
        for (i in rawNumber.indices) {
            val c = rawNumber[i]
            if (c in '0'..'9') {
                if (firstDigit == null) {
                    firstDigit = c
                    if (c == '0') return null // Local number, not a "missing plus" issue
                }
                sb.append(c)
            }
        }

        if (firstDigit == null) return null // No digits found
        val cleanedNumber = sb.toString()

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
