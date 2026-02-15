
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
    fun isEmojiOnly_skinToneModifiers() {
        assertTrue(analyzer.isEmojiOnly("👋🏿"))
        assertTrue(analyzer.isEmojiOnly("👍🏻"))
        assertTrue(analyzer.isEmojiOnly("🤝🏽"))
    }

    @Test
    fun isEmojiOnly_keycapSequences() {
        assertTrue(analyzer.isEmojiOnly("1️⃣"))
        assertTrue(analyzer.isEmojiOnly("#️⃣"))
    }

    @Test
    fun isEmojiOnly_flagSequences() {
        assertTrue(analyzer.isEmojiOnly("🇺🇸"))
        assertTrue(analyzer.isEmojiOnly("🇳🇬"))
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

    @Test
    fun hasFancyFonts_fullwidthLatin() {
        assertTrue(analyzer.hasFancyFonts("Ａ"))       // U+FF21 fullwidth A
        assertTrue(analyzer.hasFancyFonts("ａ"))       // U+FF41 fullwidth a
        assertTrue(analyzer.hasFancyFonts("Name Ｚ"))  // U+FF3A fullwidth Z
        assertFalse(analyzer.hasFancyFonts("Normal A"))
    }

    @Test
    fun isEmojiOnly_fullwidthLatinIsNotEmoji() {
        assertFalse(analyzer.isEmojiOnly("Ａ"))
        assertFalse(analyzer.isEmojiOnly("ｚ"))
    }
}
