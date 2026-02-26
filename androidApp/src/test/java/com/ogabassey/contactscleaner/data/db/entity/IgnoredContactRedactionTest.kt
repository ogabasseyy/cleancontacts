package com.ogabassey.contactscleaner.data.db.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IgnoredContactRedactionTest {

    @Test
    fun `IgnoredContact toString should redact sensitive data`() {
        val contact = IgnoredContact(
            id = "+1234567890",
            displayName = "John Doe",
            reason = "Manual Ignore",
            timestamp = 1234567890L
        )

        val stringRep = contact.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain displayName", stringRep.contains("John Doe"))
        assertFalse("Should not contain id (phone number)", stringRep.contains("+1234567890"))

        // Verify redaction marker is present
        // We expect it to be redacted.
        // This test should fail initially.
    }
}
