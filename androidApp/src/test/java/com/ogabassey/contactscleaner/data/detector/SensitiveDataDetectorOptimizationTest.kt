package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.SensitiveType
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SensitiveDataDetectorOptimizationTest {

    private lateinit var detector: SensitiveDataDetector

    @Before
    fun setup() {
        // Use the actual Android PhoneNumberHandler implementation
        detector = SensitiveDataDetector(PhoneNumberHandler())
    }

    @Test
    fun `ignores input with plus sign immediately`() {
        // Should return null (not sensitive) because '+' is a phone symbol
        // This validates the optimization path for international numbers
        val result = detector.analyze("+1234567890")
        assertNull(result)
    }

    @Test
    fun `ignores input with parentheses immediately`() {
        // Should return null (not sensitive) because '(' and ')' are phone symbols
        // This validates the optimization path for formatted numbers
        val result = detector.analyze("(555) 123-4567")
        assertNull(result)
    }

    @Test
    fun `ignores input with dot immediately`() {
        // Should return null (not sensitive) because '.' is a phone symbol
        val result = detector.analyze("555.123.4567")
        assertNull(result)
    }

    @Test
    fun `ignores input with hash or star immediately`() {
        // Should return null (not sensitive) because '#' and '*' are phone symbols (USSD)
        val result = detector.analyze("*123#")
        assertNull(result)
    }

    @Test
    fun `still detects SSN correctly`() {
        // Should NOT be ignored because '-' is allowed (hyphen) and no phone symbols are present
        val ssn = "123-45-6789"
        val match = detector.analyze(ssn)

        assertNotNull(match)
        assertEquals(SensitiveType.USA_SSN, match?.type)
    }

    @Test
    fun `still detects NINO correctly`() {
        // Should NOT be ignored because letters are allowed and no phone symbols are present
        val nino = "Pb123456C"
        val match = detector.analyze(nino)

        assertNotNull(match)
        assertEquals(SensitiveType.UK_NINO, match?.type)
    }
}
