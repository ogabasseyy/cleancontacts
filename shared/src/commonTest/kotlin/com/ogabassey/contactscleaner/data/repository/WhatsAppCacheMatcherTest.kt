package com.ogabassey.contactscleaner.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsAppCacheMatcherTest {
    @Test
    fun marksContactAsWhatsAppWhenAnyRawNumberMatchesCachedDigits() {
        val cachedNumbers = setOf("2348012345678", "15551234567")

        assertTrue(
            WhatsAppCacheMatcher.hasCachedWhatsAppNumber(
                rawNumbers = "+1 (555) 123-4567, 0803 000 0000",
                cachedNumbers = cachedNumbers
            )
        )
    }

    @Test
    fun doesNotMarkContactAsWhatsAppWhenNoRawNumbersMatchCachedDigits() {
        val cachedNumbers = setOf("2348012345678")

        assertFalse(
            WhatsAppCacheMatcher.hasCachedWhatsAppNumber(
                rawNumbers = "+1 (555) 123-4567, 0803 000 0000",
                cachedNumbers = cachedNumbers
            )
        )
    }
}
