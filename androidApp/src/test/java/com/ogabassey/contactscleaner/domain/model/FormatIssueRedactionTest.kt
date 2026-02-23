package com.ogabassey.contactscleaner.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FormatIssueRedactionTest {

    @Test
    fun `FormatIssue toString should redact sensitive data`() {
        val issue = FormatIssue(
            normalizedNumber = "+1234567890",
            countryCode = 1,
            regionCode = "US",
            displayCountry = "United States"
        )

        val stringRep = issue.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain normalizedNumber", stringRep.contains("+1234567890"))

        // Verify redaction marker is present
        // Note: Since FormatIssue is a data class without toString override yet, this test is expected to FAIL initially.
        // We look for "***REDACTED***" or similar.
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))

        // Verify non-sensitive data is present
        assertTrue("Should contain regionCode", stringRep.contains("US"))
        assertTrue("Should contain displayCountry", stringRep.contains("United States"))
    }

    @Test
    fun `Contact with FormatIssue should not leak number in toString`() {
        val issue = FormatIssue(
            normalizedNumber = "+1234567890",
            countryCode = 1,
            regionCode = "US",
            displayCountry = "United States"
        )

        val contact = Contact(
            id = 123,
            name = "John Doe",
            numbers = listOf("+1234567890"),
            emails = listOf(),
            normalizedNumber = "+1234567890",
            formatIssue = issue
        )

        val stringRep = contact.toString()

        // Contact.toString() redacts its own fields, but FormatIssue.toString() (inside Contact) might leak it.
        assertFalse("Contact.toString should not leak FormatIssue number", stringRep.contains("+1234567890"))
    }
}
