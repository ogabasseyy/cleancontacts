package com.ogabassey.contactscleaner.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImportResultRedactionTest {

    @Test
    fun `ImportResult toString should not leak sensitive data`() {
        // We rely on List<T> using T.toString().
        // Since Contact.toString() and JunkContact.toString() are redacted,
        // we mainly check duplicates (List<DuplicateGroup>).
        // But ImportResult itself is a data class, so it prints "ImportResult(validContacts=..., duplicates=...)".
        // If DuplicateGroup leaks, ImportResult leaks.

        val group = DuplicateGroup(
            matchingKey = "+1234567890",
            duplicateType = DuplicateType.NUMBER_MATCH,
            contacts = emptyList()
        )

        val result = ImportResult(
            validContacts = emptyList(),
            junkContacts = emptyList(),
            duplicates = listOf(group)
        )

        val stringRep = result.toString()

        // Verify sensitive data is NOT present
        assertFalse("Should not contain matchingKey from DuplicateGroup", stringRep.contains("+1234567890"))
    }
}
