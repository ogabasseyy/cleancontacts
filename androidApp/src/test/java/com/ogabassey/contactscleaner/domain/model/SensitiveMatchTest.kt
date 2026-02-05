package com.ogabassey.contactscleaner.domain.model

import org.junit.Test
import org.junit.Assert.assertTrue

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
        println("SensitiveMatch.toString(): $stringRepresentation")

        // This test confirms that PII is redacted in the toString() output
        // which prevents accidental leakage in logs.
        assertTrue("PII should be redacted in toString()", !stringRepresentation.contains(sensitiveValue))
        assertTrue("Should contain masked value", stringRepresentation.contains("***"))
    }
}
