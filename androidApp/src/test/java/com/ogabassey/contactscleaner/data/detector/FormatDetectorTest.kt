package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FormatDetectorTest {

    private lateinit var detector: FormatDetector

    // Test implementation of RegionProvider
    private val testRegionProvider = object : RegionProvider {
        override fun getRegionIso(): String = "NG"
        override fun getDisplayCountry(regionCode: String): String {
            return when (regionCode) {
                "NG" -> "Nigeria"
                "US" -> "United States"
                else -> "Unknown"
            }
        }
    }

    @Before
    fun setUp() {
        // Use the actual Android implementation of PhoneNumberHandler
        detector = FormatDetector(PhoneNumberHandler(), testRegionProvider)
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
        val result = detector.analyze("2348012345678")
        assertNotNull(result)
        assertEquals("+2348012345678", result?.normalizedNumber)
    }
}
