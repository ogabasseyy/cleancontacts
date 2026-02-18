package com.ogabassey.contactscleaner.data.parser

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security tests for ContactImportParser.
 * Verifies DoS protections and regex-free parsing.
 */
class ContactImportParserSecurityTest {

    private lateinit var parser: ContactImportParser

    @Before
    fun setup() {
        parser = ContactImportParser()
    }

    @Test
    fun `parseFile ignores lines exceeding max length limit`() {
        // Use constant from production code to ensure test stays in sync
        val massiveLine = "0".repeat(ContactImportParser.MAX_LINE_LENGTH * 2 + 1000)
        val validLine = "+1234567890"
        val content = "$massiveLine\n$validLine"

        val result = parser.parseFile(content, "massive.txt")

        // Should only parse the valid line
        assertEquals(1, result.validContacts.size)
        assertEquals("+1234567890", result.validContacts[0].numbers[0])
    }

    @Test
    fun `parseFile boundary test for max line length limit`() {
        // Test exact boundary using the actual constant
        val exactlyAtLimit = "0".repeat(ContactImportParser.MAX_LINE_LENGTH)
        val oneOverLimit = "0".repeat(ContactImportParser.MAX_LINE_LENGTH + 1)
        val content = "$exactlyAtLimit\n$oneOverLimit"

        val result = parser.parseFile(content, "boundary.txt")

        // Only the line at exactly MAX_LINE_LENGTH should be accepted (condition is > not >=)
        assertEquals(1, result.validContacts.size)
        assertEquals(exactlyAtLimit, result.validContacts[0].numbers[0])
    }

    @Test
    fun `parsePlainTextLine works without regex`() {
        // Ensure that removing regex doesn't break functionality
        val content = """
            08012345678
            NoDigitsHere
            +1987654321
        """.trimIndent()

        val result = parser.parseFile(content, "test.txt")

        assertEquals(2, result.validContacts.size)
        assertEquals("08012345678", result.validContacts[0].numbers[0])
        assertEquals("+1987654321", result.validContacts[1].numbers[0])
    }

    @Test
    fun `parseCSVLine handles escaped quotes correctly`() {
        // "A""B",C -> Field 1: A"B, Field 2: C
        // Current implementation incorrectly parses this as AB
        val content = "\"A\"\"B\",C"
        val result = parser.parseFile(content, "escaped.csv")

        assertEquals(1, result.validContacts.size)
        assertEquals("A\"B", result.validContacts[0].name)
        assertEquals("C", result.validContacts[0].numbers[0])
    }
}
