package com.ogabassey.contactscleaner.data.db.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocalContactRedactionTest {

    @Test
    fun `LocalContact toString should redact sensitive data`() {
        val contact = LocalContact(
            id = 123,
            displayName = "John Doe",
            normalizedNumber = "+1234567890",
            rawNumbers = "+1234567890,08012345678",
            rawEmails = "john@example.com,doe@example.com",
            isWhatsApp = true,
            isTelegram = false,
            accountType = "com.google",
            accountName = "john.doe@gmail.com",
            isJunk = false,
            junkType = null,
            duplicateType = null,
            isFormatIssue = false,
            detectedRegion = "US",
            lastSynced = 1234567890L,
            matchingKey = "key123",
            platformUid = "uid123"
        )

        val stringRep = contact.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain displayName", stringRep.contains("John Doe"))
        assertFalse("Should not contain normalizedNumber", stringRep.contains("+1234567890"))
        assertFalse("Should not contain rawNumbers", stringRep.contains("08012345678"))
        assertFalse("Should not contain rawEmails", stringRep.contains("john@example.com"))
        assertFalse("Should not contain accountName", stringRep.contains("john.doe@gmail.com"))
        assertFalse("Should not contain matchingKey", stringRep.contains("key123"))
        assertFalse("Should not contain platformUid", stringRep.contains("uid123"))

        // Verify redaction marker is present
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))

        // Verify non-sensitive data is present
        assertTrue("Should contain id", stringRep.contains("123"))
        assertTrue("Should contain isWhatsApp", stringRep.contains("isWhatsApp=true"))
        assertTrue("Should contain accountType", stringRep.contains("com.google"))
        assertTrue("Should contain detectedRegion", stringRep.contains("US"))
    }
}
