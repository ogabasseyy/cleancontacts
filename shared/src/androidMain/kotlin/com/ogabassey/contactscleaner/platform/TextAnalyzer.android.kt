package com.ogabassey.contactscleaner.platform

/**
 * Android implementation for text analysis.
 *
 * Uses single-pass O(N) codepoint iteration instead of regex for performance
 * in high-frequency contact scanning loops.
 */
actual class TextAnalyzer actual constructor() {

    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        var hasContent = false
        var i = 0
        val length = text.length

        while (i < length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            if (Character.isWhitespace(codePoint)) {
                i += charCount
                continue
            }

            hasContent = true

            // Fail on letters/digits (e.g. 'A', '1', 'ß', '١')
            if (Character.isLetterOrDigit(codePoint)) return false

            // Fail on fancy font symbols (e.g. 𝐀, ①)
            if (isFancyFont(codePoint)) return false

            // Must be a valid emoji component: Symbol Other, ZWJ, or variation selectors
            val type = Character.getType(codePoint).toByte()
            val isSymbolOther = type == Character.OTHER_SYMBOL
            val isSpecial = codePoint == 0x200D || codePoint == 0xFE0F || codePoint == 0xFE0E

            if (!isSymbolOther && !isSpecial) return false

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

    private fun isFancyFont(codePoint: Int): Boolean {
        // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
        if (codePoint in 0x1D400..0x1D7FF) return true
        // Enclosed Alphanumerics (U+2460 to U+24FF)
        if (codePoint in 0x2460..0x24FF) return true
        return false
    }
}
