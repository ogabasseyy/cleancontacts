package com.ogabassey.contactscleaner.platform

/**
 * Android implementation for text analysis.
 */
actual class TextAnalyzer actual constructor() {

    private companion object {
        // 2026 Optimization: Pre-compile regex patterns to avoid recompilation per call
        // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
        // High surrogate: 0xD835
        // Low surrogate: 0xDC00 to 0xDFFF (handled via containsMatchIn)
        private val FANCY_FONT_REGEX = Regex("[\\uD835][\\uDC00-\\uDFFF]|[\\u2460-\\u24FF]")

        // 2026 Best Practice: Only include Symbol,Other (\p{So}) for emoji detection.
        // Excluded categories that are NOT emojis:
        //   - \p{Cn} (Unassigned codepoints)
        //   - \p{Sk} (Modifier symbols like ^, `, ¨)
        //   - \p{Sm} (Math symbols like +, =, <, >)
        //   - \p{Sc} (Currency symbols like $, €, £)
        //   - \p{Pd/Pe/Pf/Pi/Po/Ps} (Punctuation categories)
        // ZWJ (\u200D) and variation selectors (\uFE0F, \uFE0E) are included for emoji sequences.
        // Note: Java 21's \p{IsEmoji} requires Android API 36+; we target API 26.
        private val EMOJI_REGEX = Regex("^[\\p{So}\\u200D\\uFE0F\\uFE0E]+$")
    }

    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        // 2026 Optimization: Avoid regex for whitespace removal
        val cleanedText = text.filter { !it.isWhitespace() }
        if (cleanedText.isEmpty()) return false

        // Use Unicode-aware check to detect letters/digits in any script (e.g., "É", "ß", "١")
        val hasAlphanumeric = cleanedText.any { it.isLetterOrDigit() }
        if (hasAlphanumeric) return false

        // If it's a fancy font, it's NOT an "emoji name"
        if (hasFancyFonts(cleanedText)) return false

        return EMOJI_REGEX.matches(cleanedText)
    }

    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false
        return FANCY_FONT_REGEX.containsMatchIn(text)
    }
}
