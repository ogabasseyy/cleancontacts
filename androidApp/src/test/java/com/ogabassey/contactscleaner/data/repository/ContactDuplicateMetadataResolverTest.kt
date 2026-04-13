package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.domain.model.DuplicateType
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ContactDuplicateMetadataResolverTest {

    private lateinit var duplicateDetector: DuplicateDetector

    @Before
    fun setup() {
        val regionProvider = object : RegionProvider {
            override fun getRegionIso(): String = "NG"

            override fun getDisplayCountry(regionCode: String): String = regionCode
        }

        duplicateDetector = DuplicateDetector(PhoneNumberHandler(), regionProvider)
    }

    @Test
    fun `apply marks similar name duplicates`() {
        val contacts = listOf(
            testLocalContact(id = 1, name = "Jonathan Doe", numbers = listOf("+2348011111111")),
            testLocalContact(id = 2, name = "Jonathon Doe", numbers = listOf("+2348022222222"))
        )

        val resolved = ContactDuplicateMetadataResolver.apply(contacts, duplicateDetector)

        assertEquals(DuplicateType.SIMILAR_NAME_MATCH.name, resolved[0].duplicateType)
        assertEquals(DuplicateType.SIMILAR_NAME_MATCH.name, resolved[1].duplicateType)
        assertNotNull(resolved[0].matchingKey)
        assertEquals(resolved[0].matchingKey, resolved[1].matchingKey)
    }

    @Test
    fun `apply prefers exact duplicates over similar names`() {
        val contacts = listOf(
            testLocalContact(id = 1, name = "Jonathan Doe", numbers = listOf("+2348011111111")),
            testLocalContact(id = 2, name = "Jonathon Doe", numbers = listOf("+2348011111111"))
        )

        val resolved = ContactDuplicateMetadataResolver.apply(contacts, duplicateDetector)

        assertEquals(DuplicateType.NUMBER_MATCH.name, resolved[0].duplicateType)
        assertEquals(DuplicateType.NUMBER_MATCH.name, resolved[1].duplicateType)
    }

    @Test
    fun `apply clears stale duplicate metadata for unique contacts`() {
        val contacts = listOf(
            testLocalContact(
                id = 1,
                name = "Unique Person",
                numbers = emptyList(),
                emails = listOf("Unique@Example.com"),
                duplicateType = DuplicateType.NUMBER_MATCH.name,
                matchingKey = "+2348011111111"
            )
        )

        val resolved = ContactDuplicateMetadataResolver.apply(contacts, duplicateDetector)

        assertNull(resolved[0].duplicateType)
        assertEquals("unique@example.com", resolved[0].matchingKey)
    }

    private fun testLocalContact(
        id: Long,
        name: String,
        numbers: List<String>,
        emails: List<String> = emptyList(),
        duplicateType: String? = null,
        matchingKey: String? = null
    ) = LocalContact(
        id = id,
        displayName = name,
        normalizedNumber = numbers.firstOrNull(),
        rawNumbers = numbers.joinToString(","),
        rawEmails = emails.joinToString(","),
        isWhatsApp = false,
        isTelegram = false,
        accountType = "com.google",
        accountName = "user@example.com",
        isJunk = false,
        junkType = null,
        duplicateType = duplicateType,
        isFormatIssue = false,
        detectedRegion = null,
        isSensitive = false,
        sensitiveDescription = null,
        matchingKey = matchingKey,
        platformUid = null,
        lastSynced = 0L
    )
}
