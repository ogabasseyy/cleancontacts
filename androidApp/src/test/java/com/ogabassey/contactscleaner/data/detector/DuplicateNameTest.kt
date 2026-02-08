package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.DuplicateType
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DuplicateNameTest {

    private lateinit var duplicateDetector: DuplicateDetector

    @Before
    fun setup() {
        val dummyRegionProvider = object : RegionProvider {
            override fun getRegionIso() = "US"
            override fun getDisplayCountry(regionCode: String) = "United States"
        }
        val phoneNumberHandler = PhoneNumberHandler()
        duplicateDetector = DuplicateDetector(phoneNumberHandler, dummyRegionProvider)
    }

    @Test
    fun detectSimilarNamesCorrectly() {
        // "Jonathan" vs "Jonathon" (dist 1, len 8 -> 0.875 > 0.82)
        val contacts = listOf(
            Contact(id = 1, name = "Jonathan", numbers = emptyList(), normalizedNumber = null),
            Contact(id = 2, name = "Jonathon", numbers = emptyList(), normalizedNumber = null)
        )

        val result = duplicateDetector.detectSimilarNameDuplicates(contacts)
        assertEquals(1, result.size)
        assertEquals(DuplicateType.SIMILAR_NAME_MATCH, result[0].duplicateType)
        assertEquals(2, result[0].contacts.size)
    }

    @Test
    fun detectMixedCaseSimilarNames() {
        // "Jonathan" vs "jonathon"
        val contacts = listOf(
            Contact(id = 1, name = "Jonathan", numbers = emptyList(), normalizedNumber = null),
            Contact(id = 2, name = "jonathon", numbers = emptyList(), normalizedNumber = null)
        )

        val result = duplicateDetector.detectSimilarNameDuplicates(contacts)
        assertEquals(1, result.size)
        assertEquals("Jonathan", result[0].matchingKey) // Should pick the first one's name usually
    }

    @Test
    fun ignoreDissimilarNames() {
         val contacts = listOf(
            Contact(id = 1, name = "Alice", numbers = emptyList(), normalizedNumber = null),
            Contact(id = 2, name = "Bob", numbers = emptyList(), normalizedNumber = null)
        )
        val result = duplicateDetector.detectSimilarNameDuplicates(contacts)
        assertEquals(0, result.size)
    }

    @Test
    fun ignoreExactMatches() {
        // Exact matches are handled by detectNameDuplicates, not similar
        // "Alice" vs "Alice" -> dist 0 -> isSimilar returns false?
        // Logic: if (d1 == d2) return false
        val contacts = listOf(
            Contact(id = 1, name = "Alice", numbers = emptyList(), normalizedNumber = null),
            Contact(id = 2, name = "Alice", numbers = emptyList(), normalizedNumber = null)
        )
        val result = duplicateDetector.detectSimilarNameDuplicates(contacts)
        assertEquals(0, result.size)
    }
}
