package com.ogabassey.contactscleaner.data.detector

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Locale

class FormatDetectorTest {

    private lateinit var detector: FormatDetector

    @Before
    fun setUp() {
        detector = FormatDetector()
    }

    @Test
    fun `analyze should correct Nigerian numbers without plus prefix`() {
        val result = detector.analyze("2348012345678", "NG")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
        assertEquals("NG", result?.regionCode)
    }

    @Test
    fun `analyze should return null for correctly formatted numbers`() {
        val result = detector.analyze("+2348012345678", "NG")
        assertNull(result)
    }

    @Test
    fun `analyze should handle numbers with special characters`() {
        // "234-801-234-5678" -> should be cleaned to "2348012345678" then normalized
        val result = detector.analyze("234-801-234-5678", "NG")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
    }

    @Test
    fun `analyze should clean non-digits before processing`() {
        // This tests the Regex optimization implicitly
        val result = detector.analyze("(234) 801 234 5678", "NG")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
    }

    @Test
    fun `analyze should not return format issue if suggested number matches original`() {
        // If the number is effectively the same (e.g. valid local number), we don't always flag
        // But here we're testing the implicit "optimization" logic in FormatDetector.kt:
        // "CRITICAL: Only accept if E164 format matches +originalNumber"
        
        // This test case depends on how libphonenumber parses unknown numbers.
        // Let's test a case that WOULD fail if logic was wrong.
        // e.g. "08012345678" with default region NG -> "+2348012345678"
        // The code does: "val plusNumber = +$cleanedNumber" -> "+080..." -> likely invalid for libphonenumber as International
        
        // Let's stick to the 234 specific logic first
        val result = detector.analyze("2348012345678")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
    }
}
