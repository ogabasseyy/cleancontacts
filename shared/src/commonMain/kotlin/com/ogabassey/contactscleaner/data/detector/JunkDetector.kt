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

    /** Valid phone number chars: digits, whitespace, +, -, parentheses */
    private fun isValidNumberChar(c: Char): Boolean =
        c in '0'..'9' || c.isWhitespace() || c == '+' || c == '-' || c == '(' || c == ')'

    /** Valid numerical name chars - same as phone number chars */
    private fun isNumericalNameChar(c: Char): Boolean = isValidNumberChar(c)

    fun detectJunk(contacts: List<Contact>): List<JunkContact> {
        val junkContacts = mutableListOf<JunkContact>()

        for (contact in contacts) {
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
        // 2026 Optimization: Single pass loop to validate chars, count digits, and check repetition
        // This avoids allocating a 'cleanedNumber' string and iterating multiple times.
        var digitCount = 0
        var lastDigit = '\u0000'
        var currentConsecutive = 0
        var maxConsecutive = 0

        for (c in number) {
            // Check for invalid chars first
            if (!isValidNumberChar(c)) {
                return JunkType.INVALID_CHAR
            }

            if (c in '0'..'9') {
                digitCount++
                if (currentConsecutive == 0) {
                    lastDigit = c
                    currentConsecutive = 1
                } else if (c == lastDigit) {
                    currentConsecutive++
                } else {
                    if (currentConsecutive > maxConsecutive) maxConsecutive = currentConsecutive
                    lastDigit = c
                    currentConsecutive = 1
                }
            }
            // Non-digit valid chars (e.g. '-', ' ') are ignored for repetition check
            // effectively treating "1-1" as "11" for repetition purposes
        }

        // Final update for maxConsecutive
        if (currentConsecutive > maxConsecutive) maxConsecutive = currentConsecutive

        // Short Number (< 6 digits)
        if (digitCount < 6) {
            return JunkType.SHORT_NUMBER
        }

        // Long Number (> 15 digits)
        if (digitCount > 15) {
            return JunkType.LONG_NUMBER
        }

        // Repetitive Digits (e.g. 111111)
        if (maxConsecutive >= REPETITIVE_THRESHOLD) {
            return JunkType.REPETITIVE_DIGITS
        }

        // 3. Name Analysis
        // 2026 Fix: name is guaranteed non-null here after line 58 check
        // 2026 Optimization: Single pass loop to compute flags, replacing multiple full traversals
        var hasDigit = false
        var allNumerical = true
        var hasLetterOrDigit = false
        var hasFancyFont = false

        for (c in name) {
            if (c.isDigit()) hasDigit = true

            // Check numerical validity if still potentially all numerical
            if (allNumerical && !isNumericalNameChar(c)) {
                allNumerical = false
            }

            if (!hasLetterOrDigit && c.isLetterOrDigit()) {
                hasLetterOrDigit = true
            }

            // Check for fancy fonts (Mathematical Alphanumeric Symbols, Enclosed, Fullwidth)
            // U+D835 is the high surrogate for Mathematical Alphanumeric Symbols
            if (!hasFancyFont) {
                if (c == '\uD835' ||
                    (c >= '\u2460' && c <= '\u24FF') || // Enclosed Alphanumerics
                    (c >= '\uFF21' && c <= '\uFF3A') || // Fullwidth Latin A-Z
                    (c >= '\uFF41' && c <= '\uFF5A')) { // Fullwidth Latin a-z
                    hasFancyFont = true
                }
            }
        }

        // A. Numerical Name (e.g. "123", "0801...")
        // Require at least one digit - otherwise "----" would be NUMERICAL_NAME instead of SYMBOL_NAME
        if (hasDigit && allNumerical) {
            return JunkType.NUMERICAL_NAME
        }

        // B. Fancy Font Names
        if (hasFancyFont) {
            return JunkType.FANCY_FONT_NAME
        }

        // C. Emoji Only Names
        // 2026 KMP Best Practice: Use platform-specific TextAnalyzer for robust emoji detection.
        if (textAnalyzer.isEmojiOnly(name)) {
            return JunkType.EMOJI_NAME
        }

        // D. Symbol Only Names (e.g. "...", "!!!")
        if (!hasLetterOrDigit) {
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
