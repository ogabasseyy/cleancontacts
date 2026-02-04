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
        // Create a massive line (e.g., 5000 chars) that exceeds the 2000 char limit
        val massiveLine = "0".repeat(5000)
        val validLine = "+1234567890"
        val content = "$massiveLine\n$validLine"

        val result = parser.parseFile(content, "massive.txt")

        // Should only parse the valid line
        assertEquals(1, result.validContacts.size)
        assertEquals("+1234567890", result.validContacts[0].numbers[0])
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
}
