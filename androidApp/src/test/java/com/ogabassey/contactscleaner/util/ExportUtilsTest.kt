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
    fun escapeCsvValue_shouldEscapePlusAndMinusForDefaultFields() {
        // Default behavior (isPhoneNumber = false) should escape + and -
        // This is critical for Name fields that might start with + or -
        assertEquals("'+1234567890", ExportUtils.escapeCsvValue("+1234567890"))
        assertEquals("'-1+1", ExportUtils.escapeCsvValue("-1+1"))
        assertEquals("'+cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("+cmd|' /C calc'!A0"))
    }

    @Test
    fun escapeCsvValue_shouldAllowSafePhoneNumbers() {
        // When isPhoneNumber = true, valid phone numbers starting with + should be allowed
        assertEquals("+1234567890", ExportUtils.escapeCsvValue("+1234567890", isPhoneNumber = true))
        assertEquals("+1 (555) 123-4567", ExportUtils.escapeCsvValue("+1 (555) 123-4567", isPhoneNumber = true))
        // Semicolon is used as separator for multiple numbers
        assertEquals("+123;+456", ExportUtils.escapeCsvValue("+123;+456", isPhoneNumber = true))
        // Minus sign at start is technically valid in some contexts or typos but safe
        assertEquals("-123", ExportUtils.escapeCsvValue("-123", isPhoneNumber = true))
    }

    @Test
    fun escapeCsvValue_shouldEscapeMaliciousPhoneNumbers() {
        // Even if isPhoneNumber = true, malicious payloads MUST be escaped
        // Payload containing letters
        assertEquals("'+cmd|' /C calc'!A0", ExportUtils.escapeCsvValue("+cmd|' /C calc'!A0", isPhoneNumber = true))
        // Payload containing special chars not allowed in phone numbers
        assertEquals("'+123|456", ExportUtils.escapeCsvValue("+123|456", isPhoneNumber = true))
        // Payload containing = inside
        assertEquals("'+123=456", ExportUtils.escapeCsvValue("+123=456", isPhoneNumber = true))
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
