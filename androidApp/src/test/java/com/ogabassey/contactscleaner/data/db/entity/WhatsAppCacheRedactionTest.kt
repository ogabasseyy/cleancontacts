package com.ogabassey.contactscleaner.data.db.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WhatsAppCacheRedactionTest {

    @Test
    fun `WhatsAppCacheEntry toString should redact normalizedNumber`() {
        val entry = WhatsAppCacheEntry(
            normalizedNumber = "2349169449282",
            isBusiness = true,
            lastSynced = 1234567890L
        )

        val stringRep = entry.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain normalizedNumber", stringRep.contains("2349169449282"))

        // Verify redaction marker is present
        // This test should fail initially.
    }
}
