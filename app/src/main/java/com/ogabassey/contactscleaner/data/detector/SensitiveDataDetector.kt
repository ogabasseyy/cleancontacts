package com.ogabassey.contactscleaner.data.detector

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

enum class SensitiveType {
    NIGERIA_NIN_BVN,
    USA_SSN,
    UK_NINO,
    CREDIT_CARD,
    UNKNOWN_PII
}

data class SensitiveMatch(
    val originalValue: String,
    val type: SensitiveType,
    val confidence: Float, // 0.0 to 1.0
    val description: String
)

@Singleton
class SensitiveDataDetector @Inject constructor() {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    // --- Global ID Patterns (Strict Regex to minimize False Positives) ---

    // USA SSN: XXX-XX-XXXX (Strict formatting)
    private val US_SSN_PATTERN = Pattern.compile("\\b(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b")

    // UK National Insurance: 2 letters, 6 digits, 1 letter
    private val UK_NINO_PATTERN = Pattern.compile("\\b[A-CEGHJ-PR-TW-Z]{1}[A-CEGHJ-NPR-TW-Z]{1}[0-9]{6}[A-D\\s]?\\b", Pattern.CASE_INSENSITIVE)

    // USA Passport (9 Digits) - Contextual keyword check usually required, but safe to flag 9-digit isolated numbers if strict
    // Note: Often confused with phone numbers. Use caution. Simple 9 digit match is too aggressive.
    // Better strategy: Only flag if labeled "Passport" or if strict alpha-numeric format.
    // For now, we omit pure 9-digit match to avoid false flagging every localized phone number.

    // UK Passport: 9 digits starting with digit? Varies. 
    // Modern UK Passports are 9 digits. 
    // Detection Strategy: 9 digits is risky (overlaps with phone/account numbers). 
    // We will focus on unambiguous Alpha-Numeric Passports (India, China, etc)

    // India Passport: 1 Letter + 7 Digits (e.g., A1234567)
    private val INDIA_PASSPORT_PATTERN = Pattern.compile("\\b[A-Z]{1}[0-9]{7}\\b")

    // China Resident ID (18 digits)
    private val CHINA_ID_PATTERN = Pattern.compile("\\b\\d{17}[0-9Xx]\\b")

    // Credit Card (Luhn-validatable candidates: 13-19 digits)
    // Visa (4...), MasterCard (5...), Amex (3...), Discover (6...)
    private val CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b")

    // Nigeria NIN/BVN (11 Digits)
    private val NIGERIA_11_DIGIT_PATTERN = Pattern.compile("\\b\\d{11}\\b")

    fun analyze(value: String, defaultRegion: String? = "NG"): SensitiveMatch? {
        val cleanValue = value.trim()
        if (cleanValue.isBlank()) return null

        // 1. Check for USA SSN
        if (US_SSN_PATTERN.matcher(cleanValue).find()) {
            return SensitiveMatch(cleanValue, SensitiveType.USA_SSN, 1.0f, "USA Social Security Number")
        }

        // 2. Check for UK National Insurance Number
        if (UK_NINO_PATTERN.matcher(cleanValue).find()) {
            return SensitiveMatch(cleanValue, SensitiveType.UK_NINO, 1.0f, "UK National Insurance Number")
        }

        // 3. Indian Passport
        if (INDIA_PASSPORT_PATTERN.matcher(cleanValue).find()) {
            return SensitiveMatch(cleanValue, SensitiveType.UNKNOWN_PII, 0.9f, "Potential Passport Number (India Format)")
        }

        // 4. China ID
        if (CHINA_ID_PATTERN.matcher(cleanValue).find()) {
            return SensitiveMatch(cleanValue, SensitiveType.UNKNOWN_PII, 0.9f, "Potential Resident ID (China Format)")
        }

        // 5. Check for Credit Card
        val ccMatcher = CREDIT_CARD_PATTERN.matcher(cleanValue.replace("-", "").replace(" ", ""))
        if (ccMatcher.find()) {
            return SensitiveMatch(cleanValue, SensitiveType.CREDIT_CARD, 0.8f, "Possible Credit Card Number")
        }

        // 6. Check for Nigeria NIN / BVN (The Tricky One)
        // If it looks like an 11-digit number, we need to ensure it's NOT a phone number.
        val matcher11 = NIGERIA_11_DIGIT_PATTERN.matcher(cleanValue.replace(" ", ""))
        if (matcher11.matches()) {
            // Case A: Is it a valid phone number?
            // If it IS a valid phone number, we assume it's a contact, not PII.
            if (isValidPhoneNumber(cleanValue, "NG")) {
                return null // It's just a phone number, likely safe to "clean" or "merge".
            }

            // Case B: It's 11 digits but NOT a valid phone number.
            // High probability of being NIN or BVN.
            return SensitiveMatch(
                cleanValue, 
                SensitiveType.NIGERIA_NIN_BVN, 
                0.9f, 
                "Potential Nigeria NIN/BVN (11-digit non-phone number)"
            )
        }

        return null
    }

    /**
     * Helper to verify if a string is a valid phone number for a given region.
     * Used to filter out valid phone numbers from being flagged as PII.
     */
    private fun isValidPhoneNumber(number: String, region: String): Boolean {
        return try {
            val proto = phoneUtil.parse(number, region)
            phoneUtil.isValidNumber(proto)
        } catch (e: Exception) {
            false
        }
    }
}
