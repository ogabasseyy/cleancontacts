package com.ogabassey.contactscleaner.platform

/**
 * iOS implementation for phone number handling.
 *
 * 2026 KMP Best Practice: Improved manual logic to match Android's libphonenumber intelligence
 * without adding heavy binary dependencies.
 */
actual class PhoneNumberHandler actual constructor() {

    private data class CountryRule(
        val countryCode: String,
        val nationalNumberLengths: List<Int> // Standard lengths (NSN) excluding country code
    )

    private val countryRules = mapOf(
        "NG" to CountryRule("234", listOf(10)), // 08012345678 (11 digits with 0, 10 without)
        "US" to CountryRule("1", listOf(10)),   // 2025550123
        "CA" to CountryRule("1", listOf(10)),
        "GB" to CountryRule("44", listOf(10, 11)), // 7...
        "GH" to CountryRule("233", listOf(9)),
        "KE" to CountryRule("254", listOf(9, 10)),
        "ZA" to CountryRule("27", listOf(9)),
        "IN" to CountryRule("91", listOf(10)),
        
        // African Countries (Strict Lengths to fix collisions)
        "SN" to CountryRule("221", listOf(9)),  // Senegal: 9 digits. (+221 + 9 = 12 total)
        "SL" to CountryRule("232", listOf(8)),  // Sierra Leone: 8 digits. (+232 + 8 = 11 total) 
        "BJ" to CountryRule("229", listOf(8)),  // Benin: 8 digits
        "TG" to CountryRule("228", listOf(8)),  // Togo: 8 digits
        "CI" to CountryRule("225", listOf(10)), // Ivory Coast: 10 digits
        "LR" to CountryRule("231", listOf(7, 8)), // Liberia
        "GM" to CountryRule("220", listOf(7)),  // Gambia
        "MR" to CountryRule("222", listOf(8)),  // Mauritania
        "CM" to CountryRule("237", listOf(9)),  // Cameroon
        "BF" to CountryRule("226", listOf(8)),  // Burkina Faso
        "NE" to CountryRule("227", listOf(8)),  // Niger
        
        // Others
        "CN" to CountryRule("86", listOf(11)),
        "JP" to CountryRule("81", listOf(10, 11)), // 090...
        "DE" to CountryRule("49", listOf(10, 11)),
        "FR" to CountryRule("33", listOf(9)),
        "BR" to CountryRule("55", listOf(10, 11)),
        "AE" to CountryRule("971", listOf(9)), 
        "SA" to CountryRule("966", listOf(9))
    )

    // Fallback for getting just the code string for legacy helpers
    private val countryCodeMap: Map<String, String>
        get() = countryRules.mapValues { it.value.countryCode }

    actual fun normalizeToE164(number: String, defaultRegion: String): String {
        if (number.isBlank()) return ""
        
        // 1. Basic cleaning
        var cleaned = number.filter { it.isDigit() || it == '+' }
        if (cleaned.isBlank()) return ""

        // 2. If already starts with +, return as is
        if (cleaned.startsWith("+")) return cleaned

        // 3. Handle local format (starting with 0)
        if (cleaned.startsWith("0")) {
            val code = countryRules[defaultRegion]?.countryCode ?: "234"
            return "+$code${cleaned.substring(1)}"
        }

        // 4. Fallback: Assume it's already international without +
        return if (cleaned.length >= 10) "+$cleaned" else cleaned
    }

    actual fun isValidNumber(number: String, region: String): Boolean {
        val isExplicitlyInternational = number.trim().startsWith("+")
        val digits = number.filter { it.isDigit() }
        val len = digits.length
        
        if (len < 7) return false
        
        // 1. International Validation (Called if + prefix exists)
        // iOS often adds '+' to everything, so we must separate real vs accidental matches.
        if (isExplicitlyInternational) {
            
            // Check against known country rules
            for ((iso, rule) in countryRules) {
                if (digits.startsWith(rule.countryCode)) {
                    val localPart = digits.substring(rule.countryCode.length)
                    val localLen = localPart.length
                    
                    // A. Strict Length Validation
                    // If the country has defined lengths, we MUST match one of them.
                    // This fixes Senegal (221) collision: 
                    //   NIN 22151801930 -> 11 digits total. 
                    //   Senegal = 221 + 9 digits = 12 total.
                    //   Result: INVALID (Correctly flagged as NIN).
                    if (rule.nationalNumberLengths.isNotEmpty()) {
                         if (localLen !in rule.nationalNumberLengths) {
                             // Length mismatch -> Not a valid number for this country -> Flag as Sensitive?
                             // Wait, if it's invalid for Senegal, it returns FALSE.
                             // Detector sees FALSE -> Flags as Sensitive. CORRECT.
                             continue 
                         }
                    } else {
                        // generic fallback
                        if (localLen < 5) continue
                    }
                    
                    // B. NG-US Collision Heuristic
                    // Problem: US number (+1 + 10 digits = 11 total) looks exactly like NIN (11 digits).
                    // If we are in Nigeria, and we see an 11-digit number starting with 1...
                    // It is 99% likely to be a NIN, not a US number.
                    // We incorrectly validated it as US because +1 matched.
                    
                    if (region == "NG" && rule.countryCode == "1") {
                        // If total length is 11 (1 + 10), and we are in Nigeria...
                        // We must assume it's a NIN to be safe.
                        // (Real US numbers starting with +1 will unfortunately be flagged, 
                        // but user can "Keep Safe". Better than missing PII).
                         if (len == 11) {
                             return false // Force FAIL -> Flag as Sensitive (NIN)
                         }
                    }

                    // Special strict check for Nigeria local part if we matched 234
                    if (iso == "NG" && localLen == 10) {
                        val prefix = if (localPart.startsWith("0")) {
                            localPart.substring(0, 3)
                        } else {
                            "0${localPart.substring(0, 2)}"
                        }
                        return prefix in listOf("070", "071", "080", "081", "090", "091")
                    }
                    
                    return true // Valid international number
                }
            }
        }
        
        // 2. Local Validation (No + prefix)
        // Strict local rules. '123...' is NOT US number here.
        
        // Special Case: Allow "234" (Nigeria) without '+'
        if (region == "NG" && digits.startsWith("234") && len == 13) {
             val localPart = digits.substring(3)
             val prefix = "0${localPart.substring(0, 2)}"
             return prefix in listOf("070", "071", "080", "081", "090", "091")
        }
        
        // Nigeria (NG) local rules (0xx...)
        if (region == "NG" && len == 11) {
            val prefix = digits.substring(0, 3)
            return prefix in listOf("070", "071", "080", "081", "090", "091")
        }

        // USA/Canada local rules
        if ((region == "US" || region == "CA") && (len == 10 || len == 11)) {
            val localPart = if (len == 11 && digits.startsWith("1")) digits.substring(1) else digits
            if (localPart.length == 10) {
                return !localPart.startsWith("0") && !localPart.startsWith("1")
            }
        }
        
        return false
    }

    // Helper for analysis tool
    actual fun analyzeFormatIssue(rawNumber: String, defaultRegion: String): FormatAnalysis? {
         if (rawNumber.isBlank() || rawNumber.startsWith("+")) return null
         val cleanedNumber = rawNumber.filter { it.isDigit() }
         
         // 1. Missing + check
         for ((iso, rule) in countryRules) {
             if (cleanedNumber.startsWith(rule.countryCode)) {
                 val localPart = cleanedNumber.substring(rule.countryCode.length)
                 if (localPart.length in rule.nationalNumberLengths) {
                      return FormatAnalysis(
                        normalizedNumber = "+$cleanedNumber",
                        countryCode = rule.countryCode.toInt(),
                        regionCode = iso,
                        displayCountry = getCountryNameForIso(iso)
                    )
                 }
             }
         }
         // ... (rest of method same) ...
         // 2. Local numbers (starts with 0)
         if (cleanedNumber.startsWith("0") && cleanedNumber.length >= 10) {
            val iso = defaultRegion
            val code = countryRules[iso]?.countryCode ?: "234"
            return FormatAnalysis(
                normalizedNumber = "+$code${cleanedNumber.substring(1)}",
                countryCode = code.toInt(),
                regionCode = iso,
                displayCountry = getCountryNameForIso(iso)
            )
         }
         return null
    }

    actual fun getCountryName(e164Number: String): String {
        val digits = e164Number.filter { it.isDigit() }
        for ((iso, rule) in countryRules) {
            if (digits.startsWith(rule.countryCode)) {
                 return "${getCountryNameForIso(iso)} (+${rule.countryCode})"
            }
        }
        return "Unknown Region"
    }

    actual fun getRegionCode(number: String, defaultRegion: String): String {
        val digits = number.filter { it.isDigit() }
        for ((iso, rule) in countryRules) {
            if (digits.startsWith(rule.countryCode)) return iso
        }
        return defaultRegion
    }

    private fun getCountryNameForIso(iso: String): String {
        return when (iso) {
            "NG" -> "Nigeria"
            "US" -> "USA"
            "GB" -> "United Kingdom"
            "IN" -> "India"
            "CA" -> "Canada"
            "AU" -> "Australia"
            "DE" -> "Germany"
            "FR" -> "France"
            "ES" -> "Spain"
            "IT" -> "Italy"
            "ZA" -> "South Africa"
            "KE" -> "Kenya"
            "GH" -> "Ghana"
            else -> "Region $iso"
        }
    }
}
