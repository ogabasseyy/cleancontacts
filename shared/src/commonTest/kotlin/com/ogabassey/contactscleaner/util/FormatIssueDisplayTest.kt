package com.ogabassey.contactscleaner.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
