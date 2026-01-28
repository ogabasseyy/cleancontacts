package com.ogabassey.contactscleaner.platform

/**
 * Platform abstraction for text analysis.
 *
 * 2026 KMP Best Practice: Use native platform APIs for complex text processing
 * like emoji detection, which varies significantly between Unicode implementations.
 */
expect class TextAnalyzer() {
    /**
     * Detects if a name is composed entirely of emojis or spaces.
     */
    fun isEmojiOnly(text: String): Boolean

    /**
     * Detects if a name contains stylized/fancy fonts (mathematical alphanumeric symbols).
     */
    fun hasFancyFonts(text: String): Boolean
}
