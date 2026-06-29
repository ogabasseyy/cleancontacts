package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidWhatsAppFlagResolverTest {

    @Test
    fun linkedCacheUpdatesFlagsFromSyncedWhatsAppNumbers() {
        val contacts = listOf(
            localContact(id = 1, rawNumbers = "+1 (555) 010-0001", isWhatsApp = false),
            localContact(id = 2, rawNumbers = "+1 (555) 010-0002", isWhatsApp = true),
            localContact(id = 3, rawNumbers = "+1 (555) 010-0003", isWhatsApp = true)
        )

        val updates = AndroidWhatsAppFlagResolver.contactsNeedingLinkedCacheUpdate(
            contacts = contacts,
            cachedNumbers = setOf("15550100001")
        )

        assertEquals(
            listOf(
                contacts[0].copy(isWhatsApp = true),
                contacts[1].copy(isWhatsApp = false),
                contacts[2].copy(isWhatsApp = false)
            ),
            updates
        )
    }

    @Test
    fun nativeRestoreUpdatesFlagsFromAndroidContactIds() {
        val contacts = listOf(
            localContact(id = 1, rawNumbers = "+1 (555) 010-0001", isWhatsApp = false),
            localContact(id = 2, rawNumbers = "+1 (555) 010-0002", isWhatsApp = true),
            localContact(id = 3, rawNumbers = "+1 (555) 010-0003", isWhatsApp = true)
        )

        val updates = AndroidWhatsAppFlagResolver.contactsNeedingNativeRestore(
            contacts = contacts,
            nativeWhatsAppIds = setOf(1L)
        )

        assertEquals(
            listOf(
                contacts[0].copy(isWhatsApp = true),
                contacts[1].copy(isWhatsApp = false),
                contacts[2].copy(isWhatsApp = false)
            ),
            updates
        )
    }

    private fun localContact(
        id: Long,
        rawNumbers: String,
        isWhatsApp: Boolean
    ) = LocalContact(
        id = id,
        displayName = "Contact $id",
        normalizedNumber = null,
        rawNumbers = rawNumbers,
        rawEmails = "",
        isWhatsApp = isWhatsApp,
        isTelegram = false,
        accountType = null,
        accountName = null,
        isJunk = false,
        junkType = null,
        duplicateType = null,
        isFormatIssue = false,
        detectedRegion = null,
        lastSynced = 1L
    )
}
