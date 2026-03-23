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

    companion object {
        private const val MAX_INPUT_LENGTH = 100

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
    }

    fun analyze(value: String, defaultRegion: String? = "NG"): SensitiveMatch? {
        if (value.isBlank()) return null

        val cleanValue = value.trim()

        // 2026 Security Fix: Prevent ReDoS by limiting input length
        // Check AFTER trimming for consistent behavior with whitespace-padded inputs
        if (cleanValue.length > MAX_INPUT_LENGTH) {
            return null
        }

        // --- ⚡ Bolt Optimization: Single Pass Character Analysis ---
        // Instead of running multiple heavy regexes and phone validation on every input,
        // we first scan the string once to count digits, letters, and key symbols.
        // This allows us to skip impossible matches (e.g. no digits = not a phone number).
        var digitCount = 0
        var letterCount = 0
        var hasHyphen = false
        var otherCount = 0 // Count non-digit, non-letter, non-hyphen, non-space

        for (i in 0 until cleanValue.length) {
            val c = cleanValue[i]
            if (c.isDigit()) digitCount++
            else if (c.isLetter()) letterCount++
            else if (c == '-') hasHyphen = true
            else if (!c.isWhitespace()) otherCount++
        }

        // Optimization: If fewer than 6 digits, it cannot be a valid phone number
        // (min 7 usually) or any supported sensitive ID (UK NINO is min 6 digits).
        if (digitCount < 6) return null

        // Optimization: If the input contains formatting symbols like (, ), or .,
        // it is a formatted phone number — not a sensitive ID. No supported PII type
        // (SSN, NINO, Passport, China ID, Credit Card, NIN) uses these characters.
        // This skips the expensive LibPhonenumber calls for inputs like "(555) 123-4567".
        if (otherCount > 0) return null

        // 2026 Fix: Do NOT blindly whitelist just because it starts with '+'.
        // iOS often adds '+' to numbers, including invalid ones (like NINs).
        // Let phoneNumberHandler.isValidNumber decide if the structure is actually valid.

        // LibPhonenumber Validation (Region Specific)
        val region = defaultRegion ?: "NG"
        if (phoneNumberHandler.isValidNumber(cleanValue, region)) {
            return null
        }
        
        // 4. LibPhonenumber Validation (Global Fallback)
        // Try forcing international format check
        // ⚡ Bolt Optimization: Cache the boolean to avoid redundant method calls
        val startsWithPlus = cleanValue.isNotEmpty() && cleanValue[0] == '+'
        val potentialIntl = if (startsWithPlus) cleanValue else "+$cleanValue"
        if (phoneNumberHandler.isValidNumber(potentialIntl, "ZZ")) {
            return null
        }

        // 2026 Guard Clause: Malformed Phone Numbers vs. IDs
        // If the value explicitly starts with '+', it is intended to be a phone number.
        // It failed validation (above), so it is a MALFORMED and INVALID phone number.
        // It is NOT a China Resident ID, SSN, or Passport (none of which start with '+').
        // We return null to avoid regex substring matches on long junk phone strings.
        if (startsWithPlus) {
            return null
        }

        // 1. Check for USA SSN
        if (digitCount >= 9 && hasHyphen && US_SSN_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.USA_SSN, 1.0f, "USA Social Security Number")
        }

        // 2. Check for UK National Insurance Number
        if (letterCount >= 2 && UK_NINO_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.UK_NINO, 1.0f, "UK National Insurance Number")
        }

        // 3. Indian Passport
        if (digitCount >= 7 && letterCount >= 1 && INDIA_PASSPORT_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.UNKNOWN_PII, 0.9f, "Potential Passport Number (India Format)")
        }

        // 4. China ID
        if (digitCount >= 17 && CHINA_ID_REGEX.containsMatchIn(cleanValue)) {
            return SensitiveMatch(cleanValue, SensitiveType.UNKNOWN_PII, 0.9f, "Potential Resident ID (China Format)")
        }

        // 5. Check for Credit Card
        if (digitCount >= 13) {
            // ⚡ Bolt Optimization: Fast-path to avoid StringBuilder instantiation.
            // We already know from the single-pass character scan above if the string has a hyphen.
            // We can also quickly check if the string contains a space before allocating.
            val hasSpace = cleanValue.contains(' ')
            val cleanedForCC = if (hasSpace || hasHyphen) {
                val sb = StringBuilder(cleanValue.length)
                for (i in 0 until cleanValue.length) {
                    val c = cleanValue[i]
                    if (c != '-' && c != ' ') sb.append(c)
                }
                sb.toString()
            } else {
                cleanValue
            }

            if (CREDIT_CARD_REGEX.containsMatchIn(cleanedForCC)) {
                return SensitiveMatch(cleanValue, SensitiveType.CREDIT_CARD, 0.8f, "Possible Credit Card Number")
            }
        }

        // 6. Check for Nigeria NIN / BVN (The Tricky One)
        // 2026 Fix: Redundant phone validation removed - already checked at line 62
        // Bolt Optimization: NIN must be exactly 11 digits and strictly numeric
        if (digitCount == 11 && letterCount == 0 && otherCount == 0) {
            // ⚡ Bolt Optimization: Fast-path to avoid StringBuilder instantiation.
            // Check if string contains spaces before attempting to remove them.
            val cleanedForNIN = if (cleanValue.contains(' ')) {
                val sbNin = StringBuilder(cleanValue.length)
                for (i in 0 until cleanValue.length) {
                    val c = cleanValue[i]
                    if (c != ' ') sbNin.append(c)
                }
                sbNin.toString()
            } else {
                cleanValue
            }

            if (NIGERIA_11_DIGIT_REGEX.matches(cleanedForNIN)) {
                // It's 11 digits but NOT a valid phone number - high probability of NIN/BVN
                return SensitiveMatch(
                    cleanValue,
                    SensitiveType.NIGERIA_NIN_BVN,
                    0.9f,
                    "Potential Nigeria NIN/BVN (11-digit non-phone number)"
                )
            }
        }

        return null
    }
}
