package com.ogabassey.contactscleaner.data.api

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.repository.Snapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiResponseRedactionTest {

    @Test
    fun `FeedbackResponse toString redacts error details`() {
        val response = FeedbackResponse(
            success = false,
            error = "john@example.com failed to send feedback"
        )

        val stringRep = response.toString()

        assertFalse("Should not contain feedback error details", stringRep.contains("john@example.com"))
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))
        assertTrue("Should retain success flag", stringRep.contains("success=false"))
    }

    @Test
    fun `WhatsApp response toString methods redact sensitive payloads`() {
        val pairing = PairingResponse(
            success = true,
            code = "12345678",
            message = "Pair +2348012345678",
            error = "wa user +2348012345678"
        )
        val disconnect = DisconnectResponse(
            success = false,
            message = "Disconnected +2348012345678",
            error = "session for +2348012345678 failed"
        )
        val numberResult = NumberCheckResult(
            number = "+2348012345678",
            hasWhatsApp = true,
            jid = "2348012345678@s.whatsapp.net"
        )
        val checkNumbers = CheckNumbersResponse(
            success = true,
            results = listOf(numberResult),
            error = "checked +2348012345678"
        )
        val batchCheck = BatchCheckResponse(
            success = true,
            total = 1,
            checked = 1,
            whatsappCount = 1,
            results = listOf(numberResult),
            error = "batch +2348012345678"
        )

        val combined = listOf(pairing, disconnect, checkNumbers, batchCheck).joinToString("\n")

        assertFalse("Should not contain pairing code", combined.contains("12345678"))
        assertFalse("Should not contain phone number", combined.contains("+2348012345678"))
        assertFalse("Should not contain WhatsApp JID", combined.contains("2348012345678@s.whatsapp.net"))
        assertTrue("Should contain REDACTED marker", combined.contains("***REDACTED***"))
        assertTrue("Should retain aggregate count", combined.contains("whatsappCount=1"))
        assertTrue("Should retain result size", combined.contains("size=1"))
    }

    @Test
    fun `Snapshot toString redacts contact list`() {
        val snapshot = Snapshot(
            id = 10,
            contacts = listOf(
                Contact(
                    id = 123,
                    name = "Jane Contact",
                    numbers = listOf("+2348012345678"),
                    emails = listOf("jane@example.com"),
                    normalizedNumber = "+2348012345678"
                )
            ),
            actionType = "delete",
            description = "before cleanup",
            timestamp = 123456789L
        )

        val stringRep = snapshot.toString()

        assertFalse("Should not contain contact name", stringRep.contains("Jane Contact"))
        assertFalse("Should not contain contact phone", stringRep.contains("+2348012345678"))
        assertFalse("Should not contain contact email", stringRep.contains("jane@example.com"))
        assertFalse("Should not contain description", stringRep.contains("before cleanup"))
        assertTrue("Should contain REDACTED marker", stringRep.contains("***REDACTED***"))
        assertTrue("Should retain snapshot metadata", stringRep.contains("actionType=delete"))
        assertTrue("Should retain contact count", stringRep.contains("size=1"))
    }
}
