package com.ogabassey.contactscleaner.platform

import org.junit.Assert.*
import org.junit.Test

class TextAnalyzerTest {

    private val textAnalyzer = TextAnalyzer()

    @Test
    fun testSingleEmoji() {
        assertTrue(textAnalyzer.isEmojiOnly("😀"))
        assertTrue(textAnalyzer.isEmojiOnly("👍"))
        assertTrue(textAnalyzer.isEmojiOnly("🔥"))
        assertTrue(textAnalyzer.isEmojiOnly("🎉"))
        assertTrue(textAnalyzer.isEmojiOnly("😂"))
        assertTrue(textAnalyzer.isEmojiOnly("❤️"))
        assertTrue(textAnalyzer.isEmojiOnly("💯"))
        assertTrue(textAnalyzer.isEmojiOnly("👽"))
        assertTrue(textAnalyzer.isEmojiOnly("🤖"))
        assertTrue(textAnalyzer.isEmojiOnly("🧟"))
    }

    @Test
    fun testMultipleEmojis() {
        assertTrue(textAnalyzer.isEmojiOnly("😀👍🔥"))
        assertTrue(textAnalyzer.isEmojiOnly("🎉😂❤️💯"))
        assertTrue(textAnalyzer.isEmojiOnly("👽🤖🧟"))
    }

    @Test
    fun testEmojiWithWhitespace() {
        assertTrue(textAnalyzer.isEmojiOnly("😀 👍 🔥"))
        assertTrue(textAnalyzer.isEmojiOnly("  🎉  😂  "))
    }

    @Test
    fun testMixedText() {
        assertFalse(textAnalyzer.isEmojiOnly("Hello 😀"))
        assertFalse(textAnalyzer.isEmojiOnly("😀 World"))
        assertFalse(textAnalyzer.isEmojiOnly("He😀llo"))
    }

    @Test
    fun testNumbers() {
        assertFalse(textAnalyzer.isEmojiOnly("123"))
        assertFalse(textAnalyzer.isEmojiOnly("😀123"))
    }

    @Test
    fun testKeycapEmoji() {
        // Keycap emojis like "1️⃣" contain "1", which is a digit.
        // Current implementation excludes digits.
        // So this should return false.
        assertFalse(textAnalyzer.isEmojiOnly("1️⃣"))
        // But "️⃣" itself (COMBINING ENCLOSING KEYCAP) is a NON-SPACING MARK (Mn).
        // Wait, current regex is \p{So}. Mn is NOT So. So regex fails.
        // My manual loop check for OTHER_SYMBOL also fails for Mn.
        // So behavior is consistent: Keycaps are NOT "Emoji Only" currently.
    }

    @Test
    fun testMathematicalBold() {
        // Mathematical Bold Capital A: U+1D400 (D835 DC00)
        val mathBoldA = "\uD835\uDC00"
        assertTrue(textAnalyzer.hasFancyFonts(mathBoldA))
        // Should NOT be considered emoji
        assertFalse(textAnalyzer.isEmojiOnly(mathBoldA))
    }

    @Test
    fun testEnclosedAlphanumeric() {
        // Circled Digit One: U+2460
        val circledOne = "\u2460"
        assertTrue(textAnalyzer.hasFancyFonts(circledOne))
        // Should NOT be considered emoji
        assertFalse(textAnalyzer.isEmojiOnly(circledOne))
    }

    @Test
    fun testNormalText() {
        assertFalse(textAnalyzer.hasFancyFonts("Hello World"))
        assertFalse(textAnalyzer.hasFancyFonts("12345"))
    }

    @Test
    fun testComplexEmojis() {
        // Family: Man, Woman, Girl, Boy
        // U+1F468 U+200D U+1F469 U+200D U+1F467 U+200D U+1F466
        val family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"
        assertTrue(textAnalyzer.isEmojiOnly(family))

        // Flag: Nigeria (Regional Indicator Symbol Letter N + G)
        // U+1F1F3 U+1F1EC
        val flagNG = "\uD83C\uDDF3\uD83C\uDDEC"
        assertTrue(textAnalyzer.isEmojiOnly(flagNG))
    }
}
