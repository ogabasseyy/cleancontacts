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
    fun escapeCsvValue_shouldNotEscapePurePhoneNumbers() {
        // + and - must NOT be escaped if they are just digits/symbols (phone numbers)
        assertEquals("+1234567890", ExportUtils.escapeCsvValue("+1234567890"))
        // -1+1 contains no letters, so it's treated as safe (though it might be a math formula, it's not RCE)
        assertEquals("-1+1", ExportUtils.escapeCsvValue("-1+1"))
    }

    @Test
    fun escapeCsvValue_shouldEscapeMaliciousPlusAndMinus() {
        // + and - SHOULD be escaped if they contain letters (potential RCE)
        assertEquals("'+cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("+cmd|' /C calc'!A0"))
        assertEquals("'-cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("-cmd|' /C calc'!A0"))
        // Mixed alphanumeric starting with +
        assertEquals("'+123A", ExportUtils.escapeCsvValue("+123A"))
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
