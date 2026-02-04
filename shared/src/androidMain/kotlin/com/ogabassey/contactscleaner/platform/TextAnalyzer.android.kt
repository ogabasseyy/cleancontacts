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

        // JVM Regex engine is quite advanced with Unicode properties.
        private val EMOJI_REGEX = Regex("^[\\p{So}\\p{Cn}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Pd}\\p{Pe}\\p{Pf}\\p{Pi}\\p{Po}\\p{Ps}\\u200D\\uFE0F\\uFE0E]+$")
    }

    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        // 2026 Optimization: Avoid regex for whitespace removal
        val cleanedText = text.filter { !it.isWhitespace() }
        if (cleanedText.isEmpty()) return false

        // 2026 Optimization: Avoid regex for ASCII alphanumeric check
        val hasAlphanumeric = cleanedText.any { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
        if (hasAlphanumeric) return false

        // If it's a fancy font, it's NOT an "emoji name"
        if (hasFancyFonts(text)) return false

        return EMOJI_REGEX.matches(cleanedText)
    }

    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false
        return FANCY_FONT_REGEX.containsMatchIn(text)
    }
}
