package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.JunkType
import com.ogabassey.contactscleaner.platform.TextAnalyzer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JunkDetectorTest {

    private lateinit var junkDetector: JunkDetector

    @Before
    fun setup() {
        // Use the actual Android TextAnalyzer implementation
        junkDetector = JunkDetector(TextAnalyzer())
    }

    @Test
    fun `detect blank contact with no name and no number`() {
        val contacts = listOf(
            Contact(id = 1, name = null, numbers = emptyList(), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(1, result.size)
        assertEquals(JunkType.NO_NAME, result[0].type)
    }

    @Test
    fun `detect invalid characters in phone number`() {
        val contacts = listOf(
            Contact(id = 1, name = "Test", numbers = listOf("08@@@33"), normalizedNumber = null),
            Contact(id = 2, name = "Test2", numbers = listOf("+234!!33"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(2, result.size)
        assertTrue(result.all { it.type == JunkType.INVALID_CHAR })
    }

    @Test
    fun `detect phone number too short`() {
        val contacts = listOf(
            Contact(id = 1, name = "Test", numbers = listOf("12345"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(1, result.size)
        assertEquals(JunkType.SHORT_NUMBER, result[0].type)
    }

    @Test
    fun `detect phone number too long`() {
        val contacts = listOf(
            Contact(id = 1, name = "Test", numbers = listOf("12345678901234567890"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(1, result.size)
        assertEquals(JunkType.LONG_NUMBER, result[0].type)
    }

    @Test
    fun `detect emoji-only names`() {
        val contacts = listOf(
            Contact(id = 1, name = "😀😀😀", numbers = listOf("+1234567890"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(1, result.size)
        // JunkDetector checks for EMOJI_NAME before SYMBOL_NAME
        assertEquals(JunkType.EMOJI_NAME, result[0].type)
    }

    @Test
    fun `valid contact should not be detected as junk`() {
        val contacts = listOf(
            Contact(id = 1, name = "John Doe", numbers = listOf("+1234567890"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(0, result.size)
    }
}
