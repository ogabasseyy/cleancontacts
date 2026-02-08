package com.ogabassey.contactscleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Security tests for ExportUtils.
 * Verifies CSV Injection protections and standard CSV escaping.
 */
class ExportUtilsTest {

    @Test
    fun `escapeCsvValue escapes special characters`() {
        // Simple string -> no escaping
        assertEquals("normal", ExportUtils.escapeCsvValue("normal"))

        // Comma -> wrap in quotes
        assertEquals("\"comma,separated\"", ExportUtils.escapeCsvValue("comma,separated"))

        // Newline -> wrap in quotes
        assertEquals("\"new\nline\"", ExportUtils.escapeCsvValue("new\nline"))

        // Quote -> wrap in quotes and escape internal quote
        assertEquals("\"quote\"\"inside\"", ExportUtils.escapeCsvValue("quote\"inside"))
    }

    @Test
    fun `escapeCsvValue prevents CSV Injection`() {
        // Starts with =
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1"))
        // Starts with +
        assertEquals("'+123", ExportUtils.escapeCsvValue("+123"))
        // Starts with -
        assertEquals("'-123", ExportUtils.escapeCsvValue("-123"))
        // Starts with @
        assertEquals("'@cmd", ExportUtils.escapeCsvValue("@cmd"))
    }

    @Test
    fun `escapeCsvValue handles CSV Injection with special characters`() {
        // Starts with = and contains comma -> should be quoted AND prepended with '
        // Logic: "=1,2" -> prepends ' -> "'=1,2" -> contains comma -> wrapped in quotes -> "\"'=1,2\""
        assertEquals("\"'=1,2\"", ExportUtils.escapeCsvValue("=1,2"))

        // Starts with + and contains quote
        // +1"2 -> '+1"2 -> contains quote -> "'+1""2"
        assertEquals("\"'+1\"\"2\"", ExportUtils.escapeCsvValue("+1\"2"))
    }
}
