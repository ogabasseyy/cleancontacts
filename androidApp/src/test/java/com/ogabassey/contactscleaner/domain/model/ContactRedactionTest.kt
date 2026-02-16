package com.ogabassey.contactscleaner.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContactRedactionTest {

    @Test
    fun `Contact toString should redact sensitive data`() {
        val contact = Contact(
            id = 123,
            name = "John Doe",
            numbers = listOf("+1234567890"),
            emails = listOf("john@example.com"),
            normalizedNumber = "+1234567890",
            accountName = "john.doe@gmail.com",
            matchingKey = "key123",
            platform_uid = "uid123",
            isWhatsApp = true
        )

        val stringRep = contact.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain name", stringRep.contains("John Doe"))
        assertFalse("Should not contain phone number", stringRep.contains("+1234567890"))
        assertFalse("Should not contain email", stringRep.contains("john@example.com"))
        assertFalse("Should not contain account name", stringRep.contains("john.doe@gmail.com"))
        assertFalse("Should not contain matching key", stringRep.contains("key123"))
        assertFalse("Should not contain platform uid", stringRep.contains("uid123"))

        // Verify redaction markers
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))

        // Verify non-sensitive data is present
        assertTrue("Should contain id", stringRep.contains("123"))
        assertTrue("Should contain isWhatsApp", stringRep.contains("isWhatsApp=true"))
    }

    @Test
    fun `JunkContact toString should redact sensitive data`() {
        val junkContact = JunkContact(
            id = 456,
            name = "Junk Name",
            number = "+9876543210",
            type = JunkType.NO_NAME
        )

        val stringRep = junkContact.toString()

        assertFalse("Should not contain name", stringRep.contains("Junk Name"))
        assertFalse("Should not contain number", stringRep.contains("+9876543210"))
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))
        assertTrue("Should contain id", stringRep.contains("456"))
        assertTrue("Should contain type", stringRep.contains("NO_NAME"))
    }

    @Test
    fun `CrossAccountContact toString should redact sensitive data`() {
        val crossAccount = CrossAccountContact(
            name = "Cross Name",
            matchingKey = "matchKey",
            primaryNumber = "+11223344",
            primaryEmail = "cross@example.com",
            accounts = emptyList()
        )

        val stringRep = crossAccount.toString()

        assertFalse("Should not contain name", stringRep.contains("Cross Name"))
        assertFalse("Should not contain matchingKey", stringRep.contains("matchKey"))
        assertFalse("Should not contain primaryNumber", stringRep.contains("+11223344"))
        assertFalse("Should not contain primaryEmail", stringRep.contains("cross@example.com"))
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))
    }

    @Test
    fun `AccountInstance toString should redact sensitive data`() {
        val accountInstance = AccountInstance(
            contactId = 789,
            accountType = "com.google",
            accountName = "myaccount@gmail.com",
            displayLabel = "My Label"
        )

        val stringRep = accountInstance.toString()

        assertFalse("Should not contain accountName", stringRep.contains("myaccount@gmail.com"))
        assertFalse("Should not contain displayLabel", stringRep.contains("My Label"))
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))
        assertTrue("Should contain contactId", stringRep.contains("789"))
        assertTrue("Should contain accountType", stringRep.contains("com.google"))
    }

    @Test
    fun `DuplicateGroupSummary toString should redact sensitive data`() {
        val summary = DuplicateGroupSummary(
            groupKey = "group1",
            count = 5,
            previewNames = "Name1, Name2, Name3"
        )

        val stringRep = summary.toString()

        assertFalse("Should not contain previewNames", stringRep.contains("Name1"))
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))
        assertTrue("Should contain groupKey", stringRep.contains("group1"))
        assertTrue("Should contain count", stringRep.contains("5"))
    }
}
