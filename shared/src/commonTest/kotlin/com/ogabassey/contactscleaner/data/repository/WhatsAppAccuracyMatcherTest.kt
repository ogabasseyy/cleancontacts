package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import kotlin.test.Test
import kotlin.test.assertEquals

class WhatsAppAccuracyMatcherTest {
    @Test
    fun extractsUniqueCheckablePhoneNumbersInStableOrder() {
        val contacts = listOf(
            contact(id = 1, rawNumbers = "+234 801 111 1111, 0802-222-2222"),
            contact(id = 2, rawNumbers = "0802 222 2222, 12345"),
            contact(id = 3, rawNumbers = "+1 (555) 123-4567")
        )

        assertEquals(
            listOf("2348011111111", "08022222222", "15551234567"),
            WhatsAppAccuracyMatcher.extractUniquePhoneNumbers(contacts)
        )
    }

    @Test
    fun appliesCheckedResultsAndLeavesUncheckedContactsUnchanged() {
        val contacts = listOf(
            contact(id = 1, rawNumbers = "+234 801 111 1111", isWhatsApp = false),
            contact(id = 2, rawNumbers = "+234 802 222 2222", isWhatsApp = true),
            contact(id = 3, rawNumbers = "+234 803 333 3333", isWhatsApp = true)
        )
        val results = mapOf(
            "2348011111111" to true,
            "2348022222222" to false
        )

        val updated = WhatsAppAccuracyMatcher.applyResults(contacts, results)

        assertEquals(true, updated[0].isWhatsApp)
        assertEquals(false, updated[1].isWhatsApp)
        assertEquals(true, updated[2].isWhatsApp)
    }

    private fun contact(
        id: Long,
        rawNumbers: String,
        isWhatsApp: Boolean = false
    ) = LocalContact(
        id = id,
        displayName = "Contact $id",
        normalizedNumber = null,
        rawNumbers = rawNumbers,
        rawEmails = "",
        isWhatsApp = isWhatsApp,
        isTelegram = false,
        accountType = "Local",
        accountName = null,
        isJunk = false,
        junkType = null,
        duplicateType = null,
        isFormatIssue = false,
        detectedRegion = null,
        isSensitive = false,
        sensitiveDescription = null,
        matchingKey = null,
        platformUid = null,
        lastSynced = 0L
    )
}
