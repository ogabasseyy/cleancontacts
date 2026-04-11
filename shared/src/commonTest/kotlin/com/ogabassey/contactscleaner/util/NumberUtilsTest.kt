package com.ogabassey.contactscleaner.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberUtilsTest {
    @Test
    fun extractDigits_preservesAsciiDigits() {
        assertEquals("234567890", "234-567-890".extractDigits())
    }

    @Test
    fun extractDigits_normalizesUnicodeDigitsToAscii() {
        assertEquals("0123456789", "٠١٢٣٤٥٦٧٨٩".extractDigits())
    }

    @Test
    fun extractDigitsAndPlus_keepsAsciiPlusAndNormalizesUnicodeDigits() {
        assertEquals("+2348123456789", "+٢٣٤ (٨١٢) ٣٤٥-٦٧٨٩".extractDigitsAndPlus())
    }
}
