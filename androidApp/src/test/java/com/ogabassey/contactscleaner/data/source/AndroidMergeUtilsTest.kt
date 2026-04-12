package com.ogabassey.contactscleaner.data.source

import com.ogabassey.contactscleaner.domain.model.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidMergeUtilsTest {

    @Test
    fun `selectTargetAccount prefers google account`() {
        val contacts = listOf(
            testContact(id = 1, accountType = null, accountName = null),
            testContact(id = 2, accountType = "com.google", accountName = "lakshmi@gmail.com")
        )

        val target = AndroidMergeUtils.selectTargetAccount(contacts)

        assertEquals("com.google", target.accountType)
        assertEquals("lakshmi@gmail.com", target.accountName)
    }

    @Test
    fun `buildMergedContact preserves account and removes duplicate data`() {
        val contacts = listOf(
            testContact(
                id = 1,
                name = "Lakshmi",
                numbers = listOf("+15551230000", "+15551230000"),
                emails = listOf("lakshmi@gmail.com"),
                accountType = "com.google",
                accountName = "lakshmi@gmail.com"
            ),
            testContact(
                id = 2,
                name = "Lakshmi N",
                numbers = listOf("+15559870000"),
                emails = listOf("lakshmi@gmail.com", "lakshmi@work.com"),
                accountType = null,
                accountName = null
            )
        )

        val merged = AndroidMergeUtils.buildMergedContact(contacts, customName = "Lakshmi Narayan")

        assertNotNull(merged)
        val actual = requireNotNull(merged)
        assertEquals("Lakshmi Narayan", actual.name)
        assertEquals(listOf("+15551230000", "+15559870000"), actual.numbers)
        assertEquals(listOf("lakshmi@gmail.com", "lakshmi@work.com"), actual.emails)
        assertEquals("com.google", actual.accountType)
        assertEquals("lakshmi@gmail.com", actual.accountName)
    }

    private fun testContact(
        id: Long,
        name: String? = "Test Contact",
        numbers: List<String> = listOf("+15551234567"),
        emails: List<String> = emptyList(),
        accountType: String?,
        accountName: String?
    ): Contact {
        return Contact(
            id = id,
            name = name,
            numbers = numbers,
            emails = emails,
            normalizedNumber = numbers.firstOrNull(),
            accountType = accountType,
            accountName = accountName
        )
    }
}
