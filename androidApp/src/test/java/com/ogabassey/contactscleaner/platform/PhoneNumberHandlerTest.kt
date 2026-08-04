package com.ogabassey.contactscleaner.platform

import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberHandlerTest {

    private val phoneNumberHandler = PhoneNumberHandler()

    @Test
    fun analyzeFormatIssue_localNumberStartingWithZero_returnsNull() {
        // Local number: 08012345678 (starts with 0)
        // Optimization should return null immediately
        val result = phoneNumberHandler.analyzeFormatIssue("08012345678", "NG")
        assertNull(result)
    }

    @Test
    fun analyzeFormatIssue_localNumberStartingWithZeroWithParenthesis_returnsNull() {
        // Local number: (080) 1234 5678 (starts with 0)
        // Optimization should return null immediately
        val result = phoneNumberHandler.analyzeFormatIssue("(080) 1234 5678", "NG")
        assertNull(result)
    }

    @Test
    fun analyzeFormatIssue_validMissingPlus_returnsAnalysis() {
        // Valid international number missing plus: 2348012345678 (starts with 2)
        // Should return analysis
        val result = phoneNumberHandler.analyzeFormatIssue("2348012345678", "NG")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
        assertEquals("Nigeria", result?.displayCountry)
    }

    @Test
    fun analyzeFormatIssue_validMissingPlusWithSpaces_returnsAnalysis() {
        // Valid international number missing plus: 234 80 1234 5678 (starts with 2)
        // Should return analysis
        val result = phoneNumberHandler.analyzeFormatIssue("234 80 1234 5678", "NG")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
    }

    @Test
    fun analyzeFormatIssue_alreadyPlus_returnsNull() {
        // Already has plus: +2348012345678
        // Should return null (not an issue)
        val result = phoneNumberHandler.analyzeFormatIssue("+2348012345678", "NG")
        assertNull(result)
    }

    @Test
    fun analyzeFormatIssue_shortNumber_returnsNull() {
        // Too short: 123
        // Should return null
        val result = phoneNumberHandler.analyzeFormatIssue("123", "NG")
        assertNull(result)
    }

    @Test
    fun analyzeFormatIssue_empty_returnsNull() {
        val result = phoneNumberHandler.analyzeFormatIssue("", "NG")
        assertNull(result)
    }

    @Test
    fun analyzeFormatIssue_noDigits_returnsNull() {
        val result = phoneNumberHandler.analyzeFormatIssue("abc", "NG")
        assertNull(result)
    }

    @Test
    fun analyzeFormatIssue_iddPrefix_returnsNull() {
        // IDD prefix: 004477... (starts with 0)
        // Should return null as +0044... is invalid
        val result = phoneNumberHandler.analyzeFormatIssue("00447700900000", "GB")
        assertNull(result)
    }

    @Test
    fun normalizeToE164_unicodeDigitsFallback_normalizesToAsciiDigits() {
        val result = phoneNumberHandler.normalizeToE164("٠٨٠١٢٣٤٥٦٧٨", "ZZ")
        assertEquals("08012345678", result)
    }

    @Test
    fun libphonenumber_isAlphaNumberLongNumericInput_returnsFalseWithoutStackOverflow() {
        val numericInput = "+" + "1".repeat(1_684)

        assertFalse(PhoneNumberUtil.getInstance().isAlphaNumber(numericInput))
    }
}
