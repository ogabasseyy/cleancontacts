package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateMergeCandidateFilterTest {
    @Test
    fun `excludes whatsapp and telegram sync account contacts`() {
        val contacts = listOf(
            contact(id = 1, accountType = "com.google"),
            contact(id = 2, accountType = "com.whatsapp"),
            contact(id = 3, accountType = "org.telegram.messenger")
        )

        val mergeable = DuplicateMergeCandidateFilter.mergeableContacts(contacts)

        assertEquals(listOf(1L), mergeable.map { it.id })
    }

    @Test
    fun `keeps local samsung and google contacts as mergeable`() {
        assertTrue(DuplicateMergeCandidateFilter.isMergeable(contact(id = 1, accountType = null)))
        assertTrue(DuplicateMergeCandidateFilter.isMergeable(contact(id = 2, accountType = "")))
        assertTrue(DuplicateMergeCandidateFilter.isMergeable(contact(id = 3, accountType = "vnd.sec.contact.phone")))
        assertTrue(DuplicateMergeCandidateFilter.isMergeable(contact(id = 4, accountType = "com.google")))
    }

    @Test
    fun `detects unmergeable synced account types case insensitively`() {
        assertFalse(DuplicateMergeCandidateFilter.isMergeable(contact(id = 1, accountType = "com.WhatsApp.w4b")))
        assertFalse(DuplicateMergeCandidateFilter.isMergeable(contact(id = 2, accountType = "ORG.Telegram.Messenger")))
    }

    private fun contact(id: Long, accountType: String?): Contact {
        return Contact(
            id = id,
            name = "Test",
            numbers = listOf("+15551234567"),
            emails = emptyList(),
            normalizedNumber = "+15551234567",
            accountType = accountType,
            accountName = null
        )
    }
}
