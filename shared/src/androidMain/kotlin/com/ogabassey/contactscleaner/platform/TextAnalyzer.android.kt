package com.ogabassey.contactscleaner.platform

/**
 * Android implementation for text analysis.
 */
actual class TextAnalyzer actual constructor() {

    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        var hasNonWhitespace = false
        val len = text.length
        var i = 0
        while (i < len) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            if (Character.isWhitespace(codePoint)) {
                i += charCount
                continue
            }
            hasNonWhitespace = true

            // 2026 Optimization: Manual code point iteration avoids regex overhead.

            // 1. Exclude forbidden characters (Letter, Digit)
            // Mathematical Alphanumeric Symbols (U+1D400..U+1D7FF) are Lu/Ll/Nd,
            // so Character.isLetterOrDigit(codePoint) returns true for them, correctly excluding them.
            if (Character.isLetterOrDigit(codePoint)) return false

            // 2. Exclude Enclosed Alphanumerics (e.g. ① U+2460) which are not emojis but symbols/numbers
            // These were explicitly excluded in the original implementation's FANCY_FONT_REGEX.
            if (codePoint in 0x2460..0x24FF) return false

            // 3. Check allowed emoji characters
            // EMOJI_REGEX was "^[\\p{So}\\u200D\\uFE0F\\uFE0E]+$"
            // So: Symbol, Other (So), ZWJ, VS16, VS15
            val type = Character.getType(codePoint)
            val isAllowed = type == Character.OTHER_SYMBOL.toInt() ||
                    codePoint == 0x200D || // ZWJ
                    codePoint == 0xFE0F || // VS16
                    codePoint == 0xFE0E    // VS15

            if (!isAllowed) return false

            i += charCount
        }

        return hasNonWhitespace
    }

    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false

        // 2026 Optimization: Iterate code points to detect ranges without Regex allocation.
        // Ranges:
        // 1. Mathematical Alphanumeric Symbols: U+1D400 to U+1D7FF
        // 2. Enclosed Alphanumerics: U+2460 to U+24FF

        val len = text.length
        var i = 0
        while (i < len) {
            val codePoint = text.codePointAt(i)
            if (codePoint in 0x1D400..0x1D7FF || codePoint in 0x2460..0x24FF) {
                return true
            }
            i += Character.charCount(codePoint)
        }
        return false
    }
}
