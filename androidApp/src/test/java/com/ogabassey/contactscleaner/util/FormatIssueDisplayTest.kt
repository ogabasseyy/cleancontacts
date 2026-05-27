package com.ogabassey.contactscleaner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatIssueDisplayTest {
    @Test
    fun selectsMissingPlusCopyWhenFirstNumberIsAlreadyNormalized() {
        val display = formatIssueDisplay(
            numbers = listOf("+2348182052701", "2348182052701"),
            normalizedNumber = "+2348182052701"
        )

        assertEquals("2348182052701", display?.sourceNumber)
        assertEquals("+2348182052701", display?.normalizedNumber)
    }

    @Test
    fun keepsFormattedRawNumberWhenDigitsMatchNormalizedNumber() {
        val display = formatIssueDisplay(
            numbers = listOf("+2348182052701", "234 818 205 2701"),
            normalizedNumber = "+2348182052701"
        )

        assertEquals("234 818 205 2701", display?.sourceNumber)
        assertEquals("+2348182052701", display?.normalizedNumber)
    }

    @Test
    fun returnsNullWhenOnlyNumberAlreadyMatchesNormalizedNumber() {
        val display = formatIssueDisplay(
            numbers = listOf("+2348182052701"),
            normalizedNumber = "+2348182052701"
        )

        assertNull(display)
    }
}
