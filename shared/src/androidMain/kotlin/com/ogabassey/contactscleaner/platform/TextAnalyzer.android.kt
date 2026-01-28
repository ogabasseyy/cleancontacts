package com.ogabassey.contactscleaner.platform

/**
 * Android implementation for text analysis.
 */
actual class TextAnalyzer actual constructor() {
    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false
        
        val cleanedText = text.replace("\\s".toRegex(), "")
        if (cleanedText.isEmpty()) return false
        
        // JVM Regex engine is quite advanced with Unicode properties.
        val emojiRegex = Regex("^[\\p{So}\\p{Cn}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Pd}\\p{Pe}\\p{Pf}\\p{Pi}\\p{Po}\\p{Ps}\\u200D\\uFE0F\\uFE0E]+$")
        
        val hasAlphanumeric = Regex("[a-zA-Z0-9]").containsMatchIn(cleanedText)
        if (hasAlphanumeric) return false

        // If it's a fancy font, it's NOT an "emoji name"
        if (hasFancyFonts(text)) return false
        
        return emojiRegex.matches(cleanedText)
    }

    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false
        
        // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
        // High surrogate: 0xD835
        // Low surrogate: 0xDC00 to 0xDFFF (handled via containsMatchIn)
        val fancyRegex = Regex("[\\uD835][\\uDC00-\\uDFFF]|[\\u2460-\\u24FF]")
        return fancyRegex.containsMatchIn(text)
    }
}
