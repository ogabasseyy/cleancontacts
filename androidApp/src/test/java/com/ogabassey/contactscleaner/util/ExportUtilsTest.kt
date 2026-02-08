package com.ogabassey.contactscleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilsTest {

    @Test
    fun escapeCsvValue_shouldEscapeFormulaTriggers() {
        // = and @ are formula triggers and should be escaped
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1"))
        // Contains comma, so it also gets quoted
        assertEquals("\"'@SUM(1,1)\"", ExportUtils.escapeCsvValue("@SUM(1,1)"))
    }

    @Test
    fun escapeCsvValue_shouldNotEscapePlusAndMinus() {
        // + and - must NOT be escaped because they appear in phone numbers
        assertEquals("+1234567890", ExportUtils.escapeCsvValue("+1234567890"))
        assertEquals("-1+1", ExportUtils.escapeCsvValue("-1+1"))
    }

    @Test
    fun escapeCsvValue_shouldHandleQuotesAndFormulas() {
        // Input: =SUM(1,2) -> prepend ' then CSV-quote due to comma
        assertEquals("\"'=SUM(1,2)\"", ExportUtils.escapeCsvValue("=SUM(1,2)"))
    }

    @Test
    fun escapeCsvValue_shouldLeaveSafeValuesAlone() {
        assertEquals("John Doe", ExportUtils.escapeCsvValue("John Doe"))
        assertEquals("1234567890", ExportUtils.escapeCsvValue("1234567890"))
    }

    @Test
    fun escapeCsvValue_shouldEscapeQuotesStandard() {
        // Standard CSV escaping: " -> "" and wrap in "
        assertEquals("\"John \"\"The Duke\"\" Doe\"", ExportUtils.escapeCsvValue("John \"The Duke\" Doe"))
        assertEquals("\"Doe, John\"", ExportUtils.escapeCsvValue("Doe, John"))
    }
}
