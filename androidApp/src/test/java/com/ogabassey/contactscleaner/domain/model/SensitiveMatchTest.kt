package com.ogabassey.contactscleaner.domain.model

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class SensitiveMatchTest {

    @Test
    fun testSensitiveMatchToStringRedaction() {
        val sensitiveValue = "123-45-6789"
        val match = SensitiveMatch(
            originalValue = sensitiveValue,
            type = SensitiveType.USA_SSN,
            confidence = 1.0f,
            description = "SSN"
        )

        val stringRepresentation = match.toString()

        // This test confirms that PII is completely redacted in the toString() output
        // which prevents accidental leakage in logs.
        assertFalse("PII should be redacted in toString()", stringRepresentation.contains(sensitiveValue))
        assertTrue("Should contain redacted string", stringRepresentation.contains("***REDACTED***"))
    }
}
