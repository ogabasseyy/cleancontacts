package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.JunkType
import com.ogabassey.contactscleaner.platform.TextAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Security tests for JunkDetector DoS prevention.
 * Validates that massive inputs are handled quickly without regex processing.
 */
class JunkDetectorSecurityTest {

    private lateinit var junkDetector: JunkDetector

    @Before
    fun setup() {
        junkDetector = JunkDetector(TextAnalyzer())
    }

    @Test
    fun `detect massive name input preventing DoS`() {
        val massiveName = "a".repeat(5000)

        val contacts = listOf(
            Contact(id = 1, name = massiveName, numbers = listOf("1234567890"), normalizedNumber = null)
        )

        val start = System.currentTimeMillis()
        val result = junkDetector.detectJunk(contacts)
        val duration = System.currentTimeMillis() - start

        assertEquals("Should identify massive name as junk", 1, result.size)
        assertEquals(JunkType.LONG_NAME, result[0].type)

        // DoS protection: execution should complete well under 100ms
        assertTrue("Execution took ${duration}ms, expected < 100ms", duration < 100)
    }

    @Test
    fun `detect massive number input preventing DoS`() {
        val massiveNumber = "1".repeat(5000)

        val contacts = listOf(
            Contact(id = 1, name = "Test", numbers = listOf(massiveNumber), normalizedNumber = null)
        )

        val start = System.currentTimeMillis()
        val result = junkDetector.detectJunk(contacts)
        val duration = System.currentTimeMillis() - start

        assertEquals(1, result.size)
        assertEquals(JunkType.LONG_NUMBER, result[0].type)

        // DoS protection: execution should complete well under 100ms
        assertTrue("Execution took ${duration}ms, expected < 100ms", duration < 100)
    }

    @Test
    fun `names at boundary length are processed normally`() {
        // 1000 chars is the MAX_INPUT_LENGTH boundary
        val boundaryName = "a".repeat(1000)

        val contacts = listOf(
            Contact(id = 1, name = boundaryName, numbers = listOf("1234567890"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        // At exactly 1000 chars, should still be processed normally (not > 1000)
        // Normal alphabetic name passes all junk checks
        assertEquals("Boundary length name should pass normal checks", 0, result.size)
    }

    @Test
    fun `names just over boundary are flagged as LONG_NAME`() {
        val overBoundaryName = "a".repeat(1001)

        val contacts = listOf(
            Contact(id = 1, name = overBoundaryName, numbers = listOf("1234567890"), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(1, result.size)
        assertEquals(JunkType.LONG_NAME, result[0].type)
    }
}
