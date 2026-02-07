package com.ogabassey.contactscleaner.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SensitiveMatchTest {

    @Test
    fun `toString should redact sensitive value`() {
        val sensitiveValue = "123-45-6789"
        val match = SensitiveMatch(
            originalValue = sensitiveValue,
            type = SensitiveType.USA_SSN,
            confidence = 1.0f,
            description = "USA Social Security Number"
        )

        val stringRepresentation = match.toString()

        // Verify the sensitive value is NOT present in the string representation
        assertFalse(
            "toString should not contain the sensitive value",
            stringRepresentation.contains(sensitiveValue)
        )

        // Verify other fields are present
        assertTrue(stringRepresentation.contains("USA_SSN"))
        assertTrue(stringRepresentation.contains("USA Social Security Number"))
        assertTrue(stringRepresentation.contains("1.0"))
    }
}
