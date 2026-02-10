package com.ogabassey.contactscleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilsTest {

    @Test
    fun escapeCsvValue_shouldEscapeFormulaTriggers() {
        // = and @ are formula triggers and should be escaped regardless of field type
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1", isPhoneNumber = false))
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1", isPhoneNumber = true))

        // Contains comma, so it also gets quoted
        assertEquals("\"'@SUM(1,1)\"", ExportUtils.escapeCsvValue("@SUM(1,1)", isPhoneNumber = false))
    }

    @Test
    fun escapeCsvValue_shouldPreservePhoneNumbers_WhenDeclared() {
        // When isPhoneNumber = true, + and - are allowed
        assertEquals("+1234567890", ExportUtils.escapeCsvValue("+1234567890", isPhoneNumber = true))
        assertEquals("-1+1", ExportUtils.escapeCsvValue("-1+1", isPhoneNumber = true))

        // Even weird "phone numbers" are allowed if the caller explicitly says it's a phone number
        // The responsibility is on the caller (contactsToCsv) to only use this for phone columns.
        assertEquals("+Cmd|Calc", ExportUtils.escapeCsvValue("+Cmd|Calc", isPhoneNumber = true))
    }

    @Test
    fun escapeCsvValue_shouldEscapePlusAndMinus_WhenNotPhoneNumber() {
        // When isPhoneNumber = false (default), + and - are treated as unsafe (potential formulas)

        // Malicious payloads
        assertEquals("'+cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("+cmd|' /C calc'!A0", isPhoneNumber = false))
        assertEquals("'-cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("-cmd|' /C calc'!A0", isPhoneNumber = false))

        // Even "innocent" looking strings starting with + in a name field get escaped to be safe
        assertEquals("'+1234567890", ExportUtils.escapeCsvValue("+1234567890", isPhoneNumber = false))
        assertEquals("'-123", ExportUtils.escapeCsvValue("-123", isPhoneNumber = false))

        // Mixed alphanumeric
        assertEquals("'+123A", ExportUtils.escapeCsvValue("+123A", isPhoneNumber = false))
    }

    @Test
    fun escapeCsvValue_shouldHandleQuotesAndFormulas() {
        // Input: =SUM(1,2) -> prepend ' then CSV-quote due to comma
        assertEquals("\"'=SUM(1,2)\"", ExportUtils.escapeCsvValue("=SUM(1,2)", isPhoneNumber = false))
    }

    @Test
    fun escapeCsvValue_shouldLeaveSafeValuesAlone() {
        assertEquals("John Doe", ExportUtils.escapeCsvValue("John Doe", isPhoneNumber = false))
        assertEquals("1234567890", ExportUtils.escapeCsvValue("1234567890", isPhoneNumber = false))
    }

    @Test
    fun escapeCsvValue_shouldEscapeQuotesStandard() {
        // Standard CSV escaping: " -> "" and wrap in "
        assertEquals("\"John \"\"The Duke\"\" Doe\"", ExportUtils.escapeCsvValue("John \"The Duke\" Doe"))
        assertEquals("\"Doe, John\"", ExportUtils.escapeCsvValue("Doe, John"))
    }
}
