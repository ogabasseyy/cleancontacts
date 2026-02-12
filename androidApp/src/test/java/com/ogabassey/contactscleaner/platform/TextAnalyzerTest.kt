package com.ogabassey.contactscleaner.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAnalyzerTest {

    private val textAnalyzer = TextAnalyzer()

    @Test
    fun `isEmojiOnly should return true for standard emojis`() {
        assertTrue(textAnalyzer.isEmojiOnly("😀"))
        assertTrue(textAnalyzer.isEmojiOnly("😀🚀"))
        assertTrue(textAnalyzer.isEmojiOnly("  😀  ")) // Whitespace ignored
    }

    @Test
    fun `isEmojiOnly should return true for composite emojis`() {
        // ZWJ sequence: Man + ZWJ + Heart + ZWJ + Kiss + Man
        // Using simpler ZWJ: Family (Man, Woman, Girl, Boy)
        // \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66
        assertTrue(textAnalyzer.isEmojiOnly("👨‍👩‍👧‍👦"))
    }

    @Test
    fun `isEmojiOnly should return false for mixed text`() {
        assertFalse(textAnalyzer.isEmojiOnly("Hello 😀"))
        assertFalse(textAnalyzer.isEmojiOnly("123 😀"))
        assertFalse(textAnalyzer.isEmojiOnly("A"))
    }

    @Test
    fun `isEmojiOnly should return false for fancy fonts`() {
        // Mathematical Bold Capital A (U+1D400)
        assertFalse(textAnalyzer.isEmojiOnly("\uD835\uDC00"))
        // Enclosed Alphanumeric (Circled Digit One: ① U+2460)
        assertFalse(textAnalyzer.isEmojiOnly("\u2460"))
    }

    @Test
    fun `isEmojiOnly should return false for blank string`() {
        assertFalse(textAnalyzer.isEmojiOnly(""))
        assertFalse(textAnalyzer.isEmojiOnly("   "))
    }

    @Test
    fun `hasFancyFonts should return true for mathematical symbols`() {
        // Mathematical Bold Capital A (U+1D400)
        assertTrue(textAnalyzer.hasFancyFonts("\uD835\uDC00"))
        // Mathematical Monospace Digit One (U+1D7F7)
        assertTrue(textAnalyzer.hasFancyFonts("\uD835\uDFF7"))
    }

    @Test
    fun `hasFancyFonts should return true for enclosed alphanumerics`() {
        // Circled Digit One (① U+2460)
        assertTrue(textAnalyzer.hasFancyFonts("\u2460"))
    }

    @Test
    fun `hasFancyFonts should return false for normal text`() {
        assertFalse(textAnalyzer.hasFancyFonts("Hello World"))
        assertFalse(textAnalyzer.hasFancyFonts("12345"))
        assertFalse(textAnalyzer.hasFancyFonts("😀"))
    }
}
