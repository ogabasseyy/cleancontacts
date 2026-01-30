package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.SensitiveMatch
import com.ogabassey.contactscleaner.domain.model.SensitiveType
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler

/**
 * Detects sensitive data (PII) in contact fields.
 *
 * 2026 KMP Best Practice: Pure Kotlin regex patterns for cross-platform detection.
 */
class SensitiveDataDetector(
    private val phoneNumberHandler: PhoneNumberHandler
) {

    // --- Global ID Patterns (Strict Regex to minimize False Positives) ---

    // USA SSN: XXX-XX-XXXX (Strict formatting)
    private val US_SSN_REGEX = Regex("\\b(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b")

    // UK National Insurance: 2 letters, 6 digits, 1 letter
    private val UK_NINO_REGEX = Regex("\\b[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z]\\d{6}[A-D\\s]?\\b", RegexOption.IGNORE_CASE)

    // India Passport: 1 Letter + 7 Digits (e.g., A1234567)
    private val INDIA_PASSPORT_REGEX = Regex("\\b[A-Z]\\d{7}\\b")

    // China Resident ID (18 digits)
    private val CHINA_ID_REGEX = Regex("\\b\\d{17}[0-9Xx]\\b")

    // Credit Card (Luhn-validatable candidates: 13-19 digits)
    private val CREDIT_CARD_REGEX = Regex("\\b(?:4\\d{12}(?:\\d{3})?|5[1-5]\\d{14}|3[47]\\d{13}|6(?:011|5\\d{2})\\d{12})\\b")

    // Nigeria NIN/BVN (11 Digits)
    private val NIGERIA_11_DIGIT_REGEX = Regex("^\\d{11}$")

    private val MAX_INPUT_LENGTH = 100

    fun analyze(value: String, defaultRegion: String? = "NG"): SensitiveMatch? {
        val cleanValue = value.trim()
        if (cleanValue.length > MAX_INPUT_LENGTH) {
            return null
        }

        // State of the Art Fix 2026: whitelist ALL valid phone numbers.
        // If libphonenumber says it's valid, it's not a sensitive ID.
        // This handles international formats (+234) and local formats robustly.
        // State of the Art Fix 2026: Paranoid Whitelisting for Phone Numbers
        
        // 1. Explicit Whitelist: International Format
        // 2026 Fix: Do NOT blindly whitelist just because it starts with '+'.
        // iOS often adds '+' to numbers, including invalid ones (like NINs).
        // Let phoneNumberHandler.isValidNumber decide if the structure is actually valid.
        
        // 2. Explicit Whitelist: Common Country Codes (e.g. 234xxxxxxxx)
        // Removed: handled by isValidNumber logic now.

        // 3. LibPhonenumber Validation (Region Specific)
        val region = defaultRegion ?: "NG"
        if (phoneNumberHandler.isValidNumber(cleanValue, region)) {
            return null
        }
        
        // 4. LibPhonenumber Validation (Global Fallback)
        // Try forcing international format check
        val potentialIntl = if (cleanValue.startsWith("+")) cleanValue else "+$cleanValue"
        if (phoneNumberHandler.isValidNumber(potentialIntl, "ZZ")) {
            return null
        }

        // 2026 Guard Clause: Malformed Phone Numbers vs. IDs
        // If the value explicitly starts with '+', it is intended to be a phone number.
        // It failed validation (above), so it is a MALFORMED and INVALID phone number.
        // It is NOT a China Resident ID, SSN, or Passport (none of which start with '+').
        // We return null to avoid regex substring matches on long junk phone strings.
        if (cleanValue.startsWith("+")) {
            return null
        }

        // 1. Check for USA SSN
        if (US_SSN_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.USA_SSN, 1.0f, "USA Social Security Number")
        }

        // 2. Check for UK National Insurance Number
        if (UK_NINO_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.UK_NINO, 1.0f, "UK National Insurance Number")
        }

        // 3. Indian Passport
        if (INDIA_PASSPORT_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.UNKNOWN_PII, 0.9f, "Potential Passport Number (India Format)")
        }

        // 4. China ID
        if (CHINA_ID_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.UNKNOWN_PII, 0.9f, "Potential Resident ID (China Format)")
        }

        // 5. Check for Credit Card
        val cleanedForCC = cleanValue.replace("-", "").replace(" ", "")
        if (CREDIT_CARD_REGEX.containsMatchIn(cleanedForCC)) {
            return SensitiveMatch(cleanValue, SensitiveType.CREDIT_CARD, 0.8f, "Possible Credit Card Number")
        }

        // 6. Check for Nigeria NIN / BVN (The Tricky One)
        val cleanedForNIN = cleanValue.replace(" ", "")
        if (NIGERIA_11_DIGIT_REGEX.matches(cleanedForNIN)) {
            // If it IS a valid phone number, assume it's a contact, not PII
            if (phoneNumberHandler.isValidNumber(cleanValue, region)) {
                return null
            }

            // It's 11 digits but NOT a valid phone number - high probability of NIN/BVN
            return SensitiveMatch(
                cleanValue,
                SensitiveType.NIGERIA_NIN_BVN,
                0.9f,
                "Potential Nigeria NIN/BVN (11-digit non-phone number)"
            )
        }

        return null
    }
}
