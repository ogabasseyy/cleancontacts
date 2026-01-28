package com.ogabassey.contactscleaner.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

/**
 * iOS implementation for text analysis using native Foundation APIs.
 */
actual class TextAnalyzer actual constructor() {

    private val commonSymbols = listOf("\u2705", "\u274C", "\u203C", "\u2049", "\u2139", "\u24C2")

    @OptIn(ExperimentalForeignApi::class)
    actual fun isEmojiOnly(text: String): Boolean {
        if (text.isBlank()) return false

        val nsString = text as NSString
        var hasTrueEmoji = false
        var onlyEmojiOrSpace = true

        nsString.enumerateSubstringsInRange(
            range = NSMakeRange(0u, nsString.length),
            options = NSStringEnumerationByComposedCharacterSequences
        ) { substring, _, _, _ ->
            val s = substring ?: return@enumerateSubstringsInRange
            if (s.trim().isEmpty()) return@enumerateSubstringsInRange
            
            if (onlyEmojiOrSpace) {
                // If it's a fancy font, it's NOT an "emoji name"
                if (isEmojiCharacter(s) && !isFancyFontCharacter(s)) {
                    hasTrueEmoji = true
                } else {
                    onlyEmojiOrSpace = false
                }
            }
        }

        return hasTrueEmoji && onlyEmojiOrSpace
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun hasFancyFonts(text: String): Boolean {
        if (text.isBlank()) return false
        
        val nsString = text as NSString
        var foundFancy = false
        
        nsString.enumerateSubstringsInRange(
            range = NSMakeRange(0u, nsString.length),
            options = NSStringEnumerationByComposedCharacterSequences
        ) { substring, _, _, _ ->
            val s = substring ?: return@enumerateSubstringsInRange
            if (isFancyFontCharacter(s)) {
                foundFancy = true
            }
        }
        
        return foundFancy
    }

    private fun isFancyFontCharacter(s: String): Boolean {
        if (s.length < 2) return false
        val high = s[0].code
        val low = s[1].code
        
        // Mathematical Alphanumeric Symbols (U+1D400 to U+1D7FF)
        // High surrogate: 0xD835
        // Low surrogate: 0xDC00 to 0xDFFF
        if (high == 0xD835 && low in 0xDC00..0xDFFF) return true
        
        // Enclosed Alphanumerics (U+2460 to U+24FF) - Often single char, but handled here
        val code = s[0].code
        if (code in 0x2460..0x24FF) return true
        
        return false
    }

    private fun isEmojiCharacter(s: String): Boolean {
        if (s.isEmpty()) return false
        
        // 2026 High-Performance Check:
        // Most modern emojis (including complex ones) start with a surrogate pair
        if (s.length >= 2 && s[0].isHighSurrogate()) return true
        
        // Standard symbols range (Dingbats, Miscellaneous Symbols, etc.)
        val code = s[0].code
        if (code in 0x231A..0x27BF) return true
        if (code in 0xFE00..0xFE0F) return true // Variation selectors
        
        return commonSymbols.contains(s)
    }
}
