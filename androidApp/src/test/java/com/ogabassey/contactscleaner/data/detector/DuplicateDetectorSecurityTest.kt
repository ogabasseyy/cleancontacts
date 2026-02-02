package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DuplicateDetectorSecurityTest {

    private lateinit var duplicateDetector: DuplicateDetector

    @Before
    fun setup() {
        val testRegionProvider = object : RegionProvider {
            override fun getRegionIso(): String = "NG"
            override fun getDisplayCountry(regionCode: String): String = "Nigeria"
        }
        duplicateDetector = DuplicateDetector(PhoneNumberHandler(), testRegionProvider)
    }

    @Test
    fun verifyDoSProtection_ignoresExcessivelyLongNames() {
        // Create names that exceed MAX_NAME_LENGTH (1000)
        val longName = "A".repeat(1001)
        val longNameSimilar = "A".repeat(1000) + "B"

        val contacts = listOf(
            Contact(id = 1, name = longName, numbers = listOf("+1"), normalizedNumber = null),
            Contact(id = 2, name = longNameSimilar, numbers = listOf("+2"), normalizedNumber = null),
            Contact(id = 3, name = "John Doe", numbers = listOf("+3"), normalizedNumber = null),
            Contact(id = 4, name = "John Do", numbers = listOf("+4"), normalizedNumber = null)
        )

        val result = duplicateDetector.detectSimilarNameDuplicates(contacts)

        // Find the group for John Doe / John Do
        // Since "John Do" sorts before "John Doe", it will be the key.
        val johnGroup = result.find { it.matchingKey == "John Doe" || it.matchingKey == "John Do" }
        assertNotNull("Should find the normal duplicate group", johnGroup)
        assertEquals("Should detect normal duplicates", 2, johnGroup?.contacts?.size ?: 0)

        // Verify long names are NOT in the result
        val longGroup = result.find { it.matchingKey == longName || it.matchingKey == longNameSimilar }
        assertTrue("Should ignore long names to prevent DoS", longGroup == null)

        assertEquals("Total groups should be 1", 1, result.size)
    }
}
