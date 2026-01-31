package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.JunkContact
import com.ogabassey.contactscleaner.domain.model.JunkType
import com.ogabassey.contactscleaner.platform.Logger
import com.ogabassey.contactscleaner.platform.TextAnalyzer

/**
 * Detects junk contacts based on various criteria.
 *
 * 2026 KMP Best Practice: Pure Kotlin algorithms without platform dependencies.
 * ML Kit GenAI features (Android-only) can be added via expect/actual if needed.
 */
class JunkDetector(
    private val textAnalyzer: TextAnalyzer
) {

    private companion object {
        // 2026 Security: Prevent DoS with massive inputs before regex processing
        private const val MAX_INPUT_LENGTH = 1000
        private const val REPETITIVE_THRESHOLD = 6 // 6+ consecutive same digits = repetitive
    }

    // 2026 Optimization: O(N) character checks instead of regex engine overhead

    /** Valid phone number chars: digits, +, -, space, parentheses */
    private fun isValidNumberChar(c: Char): Boolean =
        c in '0'..'9' || c == '+' || c == '-' || c == ' ' || c == '(' || c == ')'

    /** Valid numerical name chars: digits, +, -, space, parentheses */
    private fun isNumericalNameChar(c: Char): Boolean =
        c in '0'..'9' || c == '+' || c == '-' || c == ' ' || c == '(' || c == ')'

    /** Check for 6+ consecutive identical digits (e.g., "111111") */
    private fun hasRepetitiveDigits(digits: String): Boolean {
        if (digits.length < REPETITIVE_THRESHOLD) return false
        var count = 1
        for (i in 1 until digits.length) {
            if (digits[i] == digits[i - 1]) {
                count++
                if (count >= REPETITIVE_THRESHOLD) return true
            } else {
                count = 1
            }
        }
        return false
    }

    fun detectJunk(contacts: List<Contact>): List<JunkContact> {
        val junkContacts = mutableListOf<JunkContact>()

        contacts.forEach { contact ->
            val type = getJunkType(contact.name, contact.numbers.firstOrNull())
            if (type != null) {
                junkContacts.add(
                    JunkContact(
                        id = contact.id,
                        name = contact.name,
                        number = contact.numbers.firstOrNull(),
                        type = type
                    )
                )
            }
        }

        return junkContacts
    }

    /**
     * Smart scan - wrapper for potential AI-enhanced scanning.
     * On Android, this could integrate with ML Kit Gemini Nano.
     * On iOS, this uses standard detection.
     */
    suspend fun smartScan(contacts: List<Contact>): List<JunkContact> {
        if (contacts.isEmpty()) return emptyList()
        return detectJunk(contacts)
    }

    fun getJunkType(name: String?, number: String?): JunkType? {
        // 1. Missing Info
        if (name.isNullOrBlank()) return JunkType.NO_NAME
        if (number.isNullOrBlank()) return JunkType.NO_NUMBER

        // 2026 Security: Prevent DoS with massive inputs before regex processing
        if (number.length > MAX_INPUT_LENGTH) return JunkType.LONG_NUMBER
        if (name.length > MAX_INPUT_LENGTH) return JunkType.LONG_NAME

        // 2. Number Analysis (number is guaranteed non-null here after isNullOrBlank check)
        // Optimization: Use filter for ASCII digit check instead of Regex replace
        val cleanedNumber = number.filter { it in '0'..'9' }

        // Invalid Chars (anything not digits, +, -, space, brackets)
        // 2026 Optimization: O(N) char check instead of regex
        if (number.any { !isValidNumberChar(it) }) {
            return JunkType.INVALID_CHAR
        }

        // Short Number (< 6 digits)
        if (cleanedNumber.length < 6) {
            return JunkType.SHORT_NUMBER
        }

        // Long Number (> 15 digits)
        if (cleanedNumber.length > 15) {
            return JunkType.LONG_NUMBER
        }

        // Repetitive Digits (e.g. 111111)
        // 2026 Optimization: O(N) loop instead of regex backreference
        if (hasRepetitiveDigits(cleanedNumber)) {
            return JunkType.REPETITIVE_DIGITS
        }

        // 3. Name Analysis
        // 2026 Fix: name is guaranteed non-null here after line 58 check
        // A. Numerical Name (e.g. "123", "0801...")
        // Require at least one digit - otherwise "----" would be NUMERICAL_NAME instead of SYMBOL_NAME
        // 2026 Optimization: O(N) char check instead of regex
        if (name.any { it.isDigit() } && name.all { isNumericalNameChar(it) }) {
            return JunkType.NUMERICAL_NAME
        }

        // B. Fancy Font Names
        if (textAnalyzer.hasFancyFonts(name)) {
            return JunkType.FANCY_FONT_NAME
        }

        // C. Emoji Only Names
        // 2026 KMP Best Practice: Use platform-specific TextAnalyzer for robust emoji detection.
        if (textAnalyzer.isEmojiOnly(name)) {
            return JunkType.EMOJI_NAME
        }

        // D. Symbol Only Names (e.g. "...", "!!!")
        // 2026 Optimization: O(N) char check instead of \p{Punct} regex
        if (name.none { it.isLetterOrDigit() }) {
            return JunkType.SYMBOL_NAME
        }

        return null
    }

    // Compatibility method for string-based reason
    fun getJunkReason(name: String?, number: String?): String? {
        return getJunkType(name, number)?.name
    }

    private fun getJunkType(contact: Contact): JunkType? {
        return getJunkType(contact.name, contact.numbers.firstOrNull())
    }
}
