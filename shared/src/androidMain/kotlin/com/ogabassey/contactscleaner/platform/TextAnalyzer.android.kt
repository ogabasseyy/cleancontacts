package com.ogabassey.contactscleaner.platform

/**
 * Android implementation for text analysis.
 */
actual class TextAnalyzer actual constructor() {

    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        var hasContent = false
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // Skip whitespace
            if (Character.isWhitespace(codePoint)) {
                i += charCount
                continue
            }

            hasContent = true

            // 1. Check for alphanumeric (fail fast)
            // This covers letters (including some fancy fonts) and digits.
            if (Character.isLetterOrDigit(codePoint)) return false

            // 2. Check for Fancy Fonts
            // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
            // These might be categorized as Letter or Symbol depending on the exact char.
            // If isLetterOrDigit missed them (e.g. if categorized as Symbol), catch them here.
            if (codePoint in 0x1D400..0x1D7FF) return false

            // Enclosed Alphanumerics (U+2460 to U+24FF)
            if (codePoint in 0x2460..0x24FF) return false

            // 3. Check for Emoji-ness
            // Must be OTHER_SYMBOL (\p{So}) OR \u200D OR \uFE0F OR \uFE0E
            // Note: Character.getType returns int, constants are bytes.
            val type = Character.getType(codePoint).toByte()
            if (type == Character.OTHER_SYMBOL ||
                codePoint == 0x200D ||
                codePoint == 0xFE0F ||
                codePoint == 0xFE0E) {
                // OK, it's an emoji part
            } else {
                // Not an emoji
                return false
            }

            i += charCount
        }

        return hasContent
    }

    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false

        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
            if (codePoint in 0x1D400..0x1D7FF) return true

            // Enclosed Alphanumerics (U+2460 to U+24FF)
            if (codePoint in 0x2460..0x24FF) return true

            i += charCount
        }
        return false
    }
}
