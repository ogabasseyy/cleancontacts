package com.ogabassey.contactscleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilsTest {

    @Test
    fun escapeCsvValue_shouldEscapeFormulaTriggers() {
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1"))
        assertEquals("\"'@SUM(1,1)\"", ExportUtils.escapeCsvValue("@SUM(1,1)"))
    }

    @Test
    fun escapeCsvValue_shouldEscapePlusAndMinusInTextFields() {
        assertEquals("'+cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("+cmd|' /C calc'!A0"))
        assertEquals("'-cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("-cmd|' /C calc'!A0"))
        assertEquals("'+1234567890", ExportUtils.escapeCsvValue("+1234567890"))
        assertEquals("'-1+1", ExportUtils.escapeCsvValue("-1+1"))
    }

    @Test
    fun escapeCsvValue_shouldPreservePlusAndMinusInPhoneFields() {
        assertEquals("+1234567890", ExportUtils.escapeCsvValue("+1234567890", isPhoneField = true))
        assertEquals("-1+1", ExportUtils.escapeCsvValue("-1+1", isPhoneField = true))
        assertEquals("+44 7911 123456", ExportUtils.escapeCsvValue("+44 7911 123456", isPhoneField = true))
    }

    @Test
    fun escapeCsvValue_shouldAlwaysEscapeEqualsAndAtInPhoneFields() {
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1", isPhoneField = true))
        assertEquals("\"'@SUM(1,1)\"", ExportUtils.escapeCsvValue("@SUM(1,1)", isPhoneField = true))
    }

    @Test
    fun escapeCsvValue_shouldHandleEdgeCases() {
        assertEquals("", ExportUtils.escapeCsvValue(""))
        assertEquals("'+", ExportUtils.escapeCsvValue("+"))
        assertEquals("'-", ExportUtils.escapeCsvValue("-"))
        assertEquals("+", ExportUtils.escapeCsvValue("+", isPhoneField = true))
        assertEquals("-", ExportUtils.escapeCsvValue("-", isPhoneField = true))
    }

    @Test
    fun escapeCsvValue_shouldHandleQuotesAndFormulas() {
        assertEquals("\"'=SUM(1,2)\"", ExportUtils.escapeCsvValue("=SUM(1,2)"))
    }

    @Test
    fun escapeCsvValue_shouldLeaveSafeValuesAlone() {
        assertEquals("John Doe", ExportUtils.escapeCsvValue("John Doe"))
        assertEquals("1234567890", ExportUtils.escapeCsvValue("1234567890"))
    }

    @Test
    fun escapeCsvValue_shouldEscapeQuotesStandard() {
        assertEquals("\"John \"\"The Duke\"\" Doe\"", ExportUtils.escapeCsvValue("John \"The Duke\" Doe"))
        assertEquals("\"Doe, John\"", ExportUtils.escapeCsvValue("Doe, John"))
    }

    // Multi-value field tests

    @Test
    fun escapeMultiValueCsv_shouldEscapeEachValueIndividually() {
        // Second value has injection marker — must be caught. Comma in value triggers CSV quoting.
        val values = listOf("+1234567890", "=SUM(1,2)")
        assertEquals("\"+1234567890;'=SUM(1,2)\"", ExportUtils.escapeMultiValueCsv(values, isPhoneField = true))
    }

    @Test
    fun escapeMultiValueCsv_shouldNotDoubleQuote() {
        // Phone number with comma should be quoted once, not double-quoted
        val values = listOf("+1234,5678")
        assertEquals("\"+1234,5678\"", ExportUtils.escapeMultiValueCsv(values, isPhoneField = true))
    }

    @Test
    fun escapeMultiValueCsv_shouldHandleMultiplePhoneNumbers() {
        val values = listOf("+1234567890", "+44 7911 123456")
        assertEquals("+1234567890;+44 7911 123456", ExportUtils.escapeMultiValueCsv(values, isPhoneField = true))
    }

    @Test
    fun escapeMultiValueCsv_shouldEscapePlusInTextMode() {
        val values = listOf("safe@email.com", "+malicious")
        assertEquals("safe@email.com;'+malicious", ExportUtils.escapeMultiValueCsv(values))
    }

    @Test
    fun escapeMultiValueCsv_shouldDoubleInternalQuotes() {
        val values = listOf("value\"with\"quotes")
        assertEquals("\"value\"\"with\"\"quotes\"", ExportUtils.escapeMultiValueCsv(values))
    }

    @Test
    fun escapeMultiValueCsv_shouldHandleEmptyList() {
        assertEquals("", ExportUtils.escapeMultiValueCsv(emptyList()))
    }

    @Test
    fun escapeCsvValue_shouldEscapeTabAndCrPrefixes() {
        assertEquals("'\t=cmd", ExportUtils.escapeCsvValue("\t=cmd"))
        assertEquals("'\tdata", ExportUtils.escapeCsvValue("\tdata"))
        assertEquals("\"'\r=cmd\"", ExportUtils.escapeCsvValue("\r=cmd"))
        assertEquals("\"'\rdata\"", ExportUtils.escapeCsvValue("\rdata"))
    }
}
