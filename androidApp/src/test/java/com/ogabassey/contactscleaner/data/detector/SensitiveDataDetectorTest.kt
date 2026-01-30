package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.SensitiveType
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SensitiveDataDetectorTest {

    private lateinit var detector: SensitiveDataDetector

    @Before
    fun setup() {
        // Use the actual Android PhoneNumberHandler implementation
        detector = SensitiveDataDetector(PhoneNumberHandler())
    }

    @Test
    fun `detects USA SSN correctly`() {
        val ssn = "123-45-6789"
        val match = detector.analyze(ssn)

        assertNotNull(match)
        assertEquals(SensitiveType.USA_SSN, match?.type)
    }

    @Test
    fun `detects Nigeria NIN or BVN when not a phone number`() {
        // 11 digits, but random start (e.g., 999...) which is invalid for NG Phone (080/090/etc)
        val dummyNin = "99912345678"
        val match = detector.analyze(dummyNin, "NG")

        // Should be flagged because it's 11 digits but starts with 999 (invalid phone prefix)
        assertNotNull("Should detect as sensitive data", match)
        assertEquals(SensitiveType.NIGERIA_NIN_BVN, match?.type)
    }

    @Test
    fun `ignores valid Nigeria phone numbers (False Positive Check)`() {
        // Valid NG Phone: 080 1234 5678
        val validPhone = "08012345678"
        val match = detector.analyze(validPhone, "NG")

        // Should returned null because it IS a phone number, so we want to process it as a contact
        assertNull("Valid phone number should NOT be flagged as sensitive PII", match)
    }

    @Test
    fun `detects UK National Insurance Number`() {
        val nino = "Pb123456C"
        val match = detector.analyze(nino)

        assertNotNull(match)
        assertEquals(SensitiveType.UK_NINO, match?.type)
    }

    @Test
    fun `ignores overly long input to prevent ReDoS`() {
        // Create a string longer than 100 chars that contains a valid SSN at the end
        val prefix = "a".repeat(150)
        val ssn = "123-45-6789"
        val longInput = prefix + ssn

        // This should return null because input length > 100
        val match = detector.analyze(longInput)

        assertNull("Should return null for input > 100 chars to prevent ReDoS", match)
    }
}
