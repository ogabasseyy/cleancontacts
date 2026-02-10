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
}
