package com.ogabassey.contactscleaner.data.parser

import com.ogabassey.contactscleaner.domain.model.Contact
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ContactImportParserTest {

    private lateinit var parser: ContactImportParser

    @Before
    fun setup() {
        parser = ContactImportParser()
    }

    @Test
    fun `parse CSV with name and number`() {
        val csv = """
            John Doe,+1234567890
            Jane Smith,+9876543210
        """.trimIndent()

        val result = parser.parseFile(csv, "test.csv")

        assertEquals(2, result.validContacts.size)
        assertEquals("John Doe", result.validContacts[0].name)
        assertEquals("+1234567890", result.validContacts[0].numbers[0])
    }

    @Test
    fun `parse CSV with only numbers`() {
        val csv = """
            +1234567890
            +9876543210
        """.trimIndent()

        val result = parser.parseFile(csv, "test.csv")

        assertEquals(2, result.validContacts.size)
        assertNull(result.validContacts[0].name)
    }

    @Test
    fun `parse plain text file with numbers`() {
        val txt = """
            08012345678
            07066554433
            09011223344
        """.trimIndent()

        val result = parser.parseFile(txt, "test.txt")

        assertEquals(3, result.validContacts.size)
        assertTrue(result.validContacts.all { it.name == null })
    }

    @Test
    fun `skip empty lines`() {
        val csv = """
            John,+111111

            Jane,+222222

        """.trimIndent()

        val result = parser.parseFile(csv, "test.csv")

        assertEquals(2, result.validContacts.size)
    }

    @Test
    fun `parse CSV with quoted fields`() {
        val csv = """
            "John Doe","+1234567890"
            "Jane, Smith","+9876543210"
        """.trimIndent()

        val result = parser.parseFile(csv, "test.csv")

        assertEquals(2, result.validContacts.size)
        assertEquals("John Doe", result.validContacts[0].name)
        assertEquals("Jane, Smith", result.validContacts[1].name)
    }
}
