package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DuplicateDetectorTest {

    private lateinit var duplicateDetector: DuplicateDetector

    @Before
    fun setup() {
        // Test provider that always returns "NG"
        val testRegionProvider = object : RegionProvider {
            override fun getRegionIso(): String = "NG"
            override fun getDisplayCountry(regionCode: String): String {
                return when (regionCode) {
                    "NG" -> "Nigeria"
                    "US" -> "United States"
                    else -> "Unknown"
                }
            }
        }
        // Use actual Android PhoneNumberHandler implementation
        duplicateDetector = DuplicateDetector(PhoneNumberHandler(), testRegionProvider)
    }

    @Test
    fun `detect duplicates with same normalized number`() {
        val contacts = listOf(
            Contact(id = 1, name = "John Doe", numbers = listOf("+1234567890"), normalizedNumber = null),
            Contact(id = 2, name = "John D", numbers = listOf("+1234567890"), normalizedNumber = null),
            Contact(id = 3, name = "Jane Doe", numbers = listOf("+9876543210"), normalizedNumber = null)
        )

        val result = duplicateDetector.detectDuplicates(contacts)

        assertEquals(1, result.size)
        assertEquals(2, result[0].contacts.size)
    }

    @Test
    fun `normalize phone number correctly`() {
        val number1 = duplicateDetector.normalizePhoneNumber("08012345678")
        val number2 = duplicateDetector.normalizePhoneNumber("+2348012345678")
        val number3 = duplicateDetector.normalizePhoneNumber("(234) 801-2345-678")

        assertTrue(number1.startsWith("+"))
        assertTrue(number2.startsWith("+"))
        assertEquals("+2348012345678", number2)
    }

    @Test
    fun `no duplicates when all numbers are unique`() {
        val contacts = listOf(
            Contact(id = 1, name = "John", numbers = listOf("+1111111111"), normalizedNumber = null),
            Contact(id = 2, name = "Jane", numbers = listOf("+2222222222"), normalizedNumber = null),
            Contact(id = 3, name = "Bob", numbers = listOf("+3333333333"), normalizedNumber = null)
        )

        val result = duplicateDetector.detectDuplicates(contacts)

        assertEquals(0, result.size)
    }

    @Test
    fun `detect multiple duplicate groups`() {
        val contacts = listOf(
            Contact(id = 1, name = "John1", numbers = listOf("+1111111111"), normalizedNumber = null),
            Contact(id = 2, name = "John2", numbers = listOf("+1111111111"), normalizedNumber = null),
            Contact(id = 3, name = "Jane1", numbers = listOf("+2222222222"), normalizedNumber = null),
            Contact(id = 4, name = "Jane2", numbers = listOf("+2222222222"), normalizedNumber = null)
        )

        val result = duplicateDetector.detectDuplicates(contacts)

        assertEquals(2, result.size)
        assertTrue(result.all { it.contacts.size == 2 })
    }

    @Test
    fun `detect name duplicates correctly`() {
        val contacts = listOf(
            Contact(id = 1, name = "John Doe", numbers = listOf("+1"), normalizedNumber = null),
            Contact(id = 2, name = "John Doe", numbers = listOf("+2"), normalizedNumber = null), // Exact match
            Contact(id = 3, name = "john doe ", numbers = listOf("+3"), normalizedNumber = null), // Case/trim match
            Contact(id = 4, name = "Jane Doe", numbers = listOf("+4"), normalizedNumber = null) // Unique
        )

        val result = duplicateDetector.detectDuplicates(contacts)

        // Should find 1 group with 3 contacts
        val nameGroup = result.find { it.matchingKey == "john doe" }
        assertNotNull(nameGroup)
        assertEquals(3, nameGroup?.contacts?.size)
        assertTrue(nameGroup?.contacts?.any { it.id == 1L } == true)
        assertTrue(nameGroup?.contacts?.any { it.id == 2L } == true)
        assertTrue(nameGroup?.contacts?.any { it.id == 3L } == true)
    }
}
