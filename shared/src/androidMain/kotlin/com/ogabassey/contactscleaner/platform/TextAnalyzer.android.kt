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

            // Keycap sequences: (digit/#/*) + [U+FE0F] + U+20E3
            // Must check before digit rejection since digits are valid keycap bases
            if (isKeycapBase(codePoint)) {
                val skip = consumeKeycapTail(text, i + charCount, length)
                if (skip > 0) {
                    i += charCount + skip
                    continue
                }
            }

            // Fail on letters/digits (e.g. 'A', '1', 'ß', '١')
            if (Character.isLetterOrDigit(codePoint)) return false

            // Fail on fancy font symbols (e.g. 𝐀, ①)
            if (isFancyFont(codePoint)) return false

            // Must be a valid emoji component
            val type = Character.getType(codePoint).toByte()
            val isSymbolOther = type == Character.OTHER_SYMBOL
            val isModifierSymbol = type == Character.MODIFIER_SYMBOL
            val isEnclosingMark = type == Character.ENCLOSING_MARK
            val isFormat = type == Character.FORMAT

            // ZWJ, variation selectors, skin tone modifiers, keycap combiner, tag characters
            val isSpecial = codePoint == 0x200D ||
                            codePoint == 0xFE0F ||
                            codePoint == 0xFE0E ||
                            codePoint in 0x1F3FB..0x1F3FF ||
                            codePoint == 0x20E3 ||
                            codePoint in 0xE0020..0xE007F

            if (!isSymbolOther && !isModifierSymbol && !isEnclosingMark && !isFormat && !isSpecial) return false

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

    /** Keycap base characters: digits 0-9, #, * */
    private fun isKeycapBase(codePoint: Int): Boolean =
        codePoint in 0x0030..0x0039 || codePoint == 0x0023 || codePoint == 0x002A

    /** If text[from..] starts with [FE0F] + 20E3, return chars consumed. 0 = no keycap tail. */
    private fun consumeKeycapTail(text: String, from: Int, length: Int): Int {
        if (from >= length) return 0
        var pos = from
        val next = text.codePointAt(pos)
        if (next == 0xFE0F) pos += Character.charCount(next)
        if (pos < length && text.codePointAt(pos) == 0x20E3) {
            return pos + Character.charCount(0x20E3) - from
        }
        return 0
    }

    private fun isFancyFont(codePoint: Int): Boolean {
        // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
        if (codePoint in 0x1D400..0x1D7FF) return true
        // Enclosed Alphanumerics (U+2460 to U+24FF)
        if (codePoint in 0x2460..0x24FF) return true
        // Fullwidth Latin letters (A-Z: U+FF21–U+FF3A, a-z: U+FF41–U+FF5A)
        if (codePoint in 0xFF21..0xFF3A || codePoint in 0xFF41..0xFF5A) return true
        return false
    }
}
