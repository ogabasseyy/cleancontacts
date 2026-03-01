package com.ogabassey.contactscleaner.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DuplicateGroupRedactionTest {

    @Test
    fun `DuplicateGroup toString should redact matchingKey`() {
        val group = DuplicateGroup(
            matchingKey = "+1234567890",
            duplicateType = DuplicateType.NUMBER_MATCH,
            contacts = emptyList()
        )

        val stringRep = group.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain matchingKey (phone number)", stringRep.contains("+1234567890"))

        // Verify redaction marker
        // This test should fail initially.
    }
}
