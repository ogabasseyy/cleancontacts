package com.ogabassey.contactscleaner.platform

import org.junit.Test
import org.junit.Assert.*

class TextAnalyzerTest {
    private val analyzer = TextAnalyzer()

    @Test
    fun isEmojiOnly_basicEmojis() {
        assertTrue(analyzer.isEmojiOnly("😀"))
        assertTrue(analyzer.isEmojiOnly("😀😁😂"))
        assertTrue(analyzer.isEmojiOnly("  😀  "))
    }

    @Test
    fun isEmojiOnly_complexEmojis() {
        assertTrue(analyzer.isEmojiOnly("👨‍👩‍👧‍👦"))
        assertTrue(analyzer.isEmojiOnly("❤"))
        assertTrue(analyzer.isEmojiOnly("❤️"))
    }

    @Test
    fun isEmojiOnly_mixedContent() {
        assertFalse(analyzer.isEmojiOnly("Hello 😀"))
        assertFalse(analyzer.isEmojiOnly("123"))
        assertFalse(analyzer.isEmojiOnly("ABC"))
    }

    @Test
    fun isEmojiOnly_blankAndEmpty() {
        assertFalse(analyzer.isEmojiOnly(""))
        assertFalse(analyzer.isEmojiOnly("   "))
    }

    @Test
    fun isEmojiOnly_fancyFontsAreNotEmoji() {
        assertFalse(analyzer.isEmojiOnly("𝐀"))
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
