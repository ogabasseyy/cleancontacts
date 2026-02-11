package com.ogabassey.contactscleaner.platform

/**
 * Android implementation for text analysis.
 */
actual class TextAnalyzer actual constructor() {

    // 2026 Optimization: Removed Regex constants to eliminate compilation overhead and allocation.
    // Replaced with O(N) manual character iteration loops.

    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        var hasContent = false
        var i = 0
        val length = text.length

        while (i < length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // Skip whitespace
            if (Character.isWhitespace(codePoint)) {
                i += charCount
                continue
            }

            hasContent = true

            // 1. Fail if it's a standard letter or digit (e.g. 'A', '1', 'ß')
            if (Character.isLetterOrDigit(codePoint)) {
                return false
            }

            // 2. Fail if it's a fancy font symbol (e.g. 𝐀, ①)
            // Even if it's technically a symbol (So), we don't want it as an "emoji name".
            if (isFancyFont(codePoint)) {
                return false
            }

            // 3. Must be a valid emoji component
            // Valid: Symbol Other (So), Zero Width Joiner, or Variation Selectors
            val type = Character.getType(codePoint).toByte()
            val isSymbolOther = type == Character.OTHER_SYMBOL
            val isSpecial = codePoint == 0x200D || codePoint == 0xFE0F || codePoint == 0xFE0E

            if (!isSymbolOther && !isSpecial) {
                return false
            }

            i += charCount
        }

        return hasContent
    }

    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false
        var i = 0
        val length = text.length
        while (i < length) {
            val codePoint = text.codePointAt(i)
            if (isFancyFont(codePoint)) return true
            i += Character.charCount(codePoint)
        }
        return false
    }

    // 2026 Optimization: Helper to check fancy font ranges directly
    private fun isFancyFont(codePoint: Int): Boolean {
        // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
        if (codePoint in 0x1D400..0x1D7FF) return true
        // Enclosed Alphanumerics (U+2460 to U+24FF)
        if (codePoint in 0x2460..0x24FF) return true
        return false
    }
}
