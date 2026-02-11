package com.ogabassey.contactscleaner.platform

import org.junit.Test
import org.junit.Assert.*

class TextAnalyzerTest {
    private val analyzer = TextAnalyzer()

    @Test
    fun isEmojiOnly_basicEmojis() {
        assertTrue(analyzer.isEmojiOnly("😀"))
        assertTrue(analyzer.isEmojiOnly("😀😁😂"))
        assertTrue(analyzer.isEmojiOnly("  😀  ")) // whitespace is allowed/ignored? No, cleanedText must match regex. regex is `^+`. So if cleanedText is empty, returns false?
        // Wait, current code:
        // val cleanedText = text.filter { !it.isWhitespace() }
        // if (cleanedText.isEmpty()) return false
        // return EMOJI_REGEX.matches(cleanedText)

        // So "  " returns false.
        // "  😀  " -> cleaned="😀". matches regex? Yes. So true.
    }

    @Test
    fun isEmojiOnly_complexEmojis() {
        // ZWJ sequences
        assertTrue(analyzer.isEmojiOnly("👨‍👩‍👧‍👦"))
        // Variation Selectors
        assertTrue(analyzer.isEmojiOnly("❤")) // Assuming this is handled correctly
        assertTrue(analyzer.isEmojiOnly("❤️")) // With VS16
    }

    @Test
    fun isEmojiOnly_mixedContent() {
        assertFalse(analyzer.isEmojiOnly("Hello 😀"))
        assertFalse(analyzer.isEmojiOnly("123"))
        assertFalse(analyzer.isEmojiOnly("ABC"))
    }

    @Test
    fun isEmojiOnly_fancyFonts() {
        // These are technically symbols but not emojis we want?
        // Current code explicitly checks `hasFancyFonts`.
        // U+1D400 (𝐀) - Mathematical Bold Capital A
        assertFalse(analyzer.isEmojiOnly("𝐀"))
        // U+2460 (①) - Enclosed Alphanumeric
        assertFalse(analyzer.isEmojiOnly("①"))
    }

    @Test
    fun hasFancyFonts_detection() {
        assertTrue(analyzer.hasFancyFonts("Here is 𝐀 fancy font"))
        assertTrue(analyzer.hasFancyFonts("① item"))
        assertFalse(analyzer.hasFancyFonts("Just normal text"))
        assertFalse(analyzer.hasFancyFonts("😀"))
    }
}
