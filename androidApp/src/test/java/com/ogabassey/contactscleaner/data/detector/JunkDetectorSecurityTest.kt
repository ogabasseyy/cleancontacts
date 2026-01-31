package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.JunkType
import com.ogabassey.contactscleaner.platform.TextAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JunkDetectorSecurityTest {

    private lateinit var junkDetector: JunkDetector

    @Before
    fun setup() {
        // Use the actual Android TextAnalyzer implementation
        junkDetector = JunkDetector(TextAnalyzer())
    }

    @Test
    fun `detect massive name input preventing DoS`() {
        // Create a massive name (5000 chars) that would normally trigger regex processing
        val massiveName = "a".repeat(5000)

        val contacts = listOf(
            Contact(id = 1, name = massiveName, numbers = listOf("1234567890"), normalizedNumber = null)
        )

        // This should run quickly if input length limits are enforced
        val start = System.currentTimeMillis()
        val result = junkDetector.detectJunk(contacts)
        val end = System.currentTimeMillis()

        // Verify it was detected as junk (fail open/safe)
        assertEquals("Should identify massive name as junk", 1, result.size)
        // We expect it to be flagged as SYMBOL_NAME (or whatever we decide for long names)
        // Currently, without the fix, it might be NO_NAME or valid depending on regex,
        // but with the fix it will be SYMBOL_NAME.
        // For reproduction, we just check it runs. The assertion on type will pass AFTER fix.
        // If we want to assert "before fix behavior", it likely won't be SYMBOL_NAME unless it matches regex.
        // "a" repeat 5000 won't match SYMBOL_NAME_REGEX (punctuation/space) or NUMERICAL_NAME_REGEX (digits/space).
        // So currently it returns NULL (empty list).

        // After fix, it should be LONG_NAME.
        // So for now, I expect this test to FAIL (result.size = 0) until I implement the fix.
        // That confirms the "vulnerability" (or behavior change).
        assertEquals(JunkType.LONG_NAME, result[0].type)

        println("Execution time: ${end - start}ms")
    }

    @Test
    fun `detect massive number input preventing DoS`() {
        // Create a massive number (5000 chars)
        val massiveNumber = "1".repeat(5000)

        val contacts = listOf(
            Contact(id = 1, name = "Test", numbers = listOf(massiveNumber), normalizedNumber = null)
        )

        val result = junkDetector.detectJunk(contacts)

        assertEquals(1, result.size)
        // Currently logic flags it as LONG_NUMBER (> 15) but only AFTER regex check.
        // With fix, it should still be LONG_NUMBER but faster/safer.
        assertEquals(JunkType.LONG_NUMBER, result[0].type)
    }
}
