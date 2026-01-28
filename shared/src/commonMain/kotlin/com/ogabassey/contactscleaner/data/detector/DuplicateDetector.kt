package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.DuplicateGroup
import com.ogabassey.contactscleaner.domain.model.DuplicateType
import com.ogabassey.contactscleaner.platform.PhoneNumberHandler
import com.ogabassey.contactscleaner.platform.RegionProvider
import kotlin.math.abs

/**
 * Detects duplicate contacts based on phone numbers, emails, and names.
 *
 * 2026 KMP Best Practice: Pure Kotlin with platform abstractions for phone number handling.
 */
class DuplicateDetector(
    private val phoneNumberHandler: PhoneNumberHandler,
    private val regionProvider: RegionProvider
) {

    fun detectDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val allDuplicates = mutableListOf<DuplicateGroup>()

        // 1. Number Duplicates (Strict + E164 Normalization)
        allDuplicates.addAll(detectNumberDuplicates(contacts))

        // 2. Email Duplicates (Strict)
        allDuplicates.addAll(detectEmailDuplicates(contacts))

        // 3. Name Duplicates (Exact)
        allDuplicates.addAll(detectNameDuplicates(contacts))

        return allDuplicates
    }

    private fun detectNumberDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val defaultRegion = regionProvider.getRegionIso()
        val groups = mutableMapOf<String, MutableList<Contact>>()

        contacts.forEach { contact ->
            contact.numbers.forEach { number ->
                val normalized = phoneNumberHandler.normalizeToE164(number, defaultRegion)
                groups.getOrPut(normalized) { mutableListOf() }.add(contact)
            }
        }

        return groups.mapNotNull { (key, group) ->
            val distinctContacts = group.distinctBy { it.id }
            if (distinctContacts.size > 1) {
                DuplicateGroup(
                    matchingKey = key,
                    duplicateType = DuplicateType.NUMBER_MATCH,
                    contacts = distinctContacts.sortedBy { it.name }
                )
            } else null
        }
    }

    private fun detectEmailDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val groups = mutableMapOf<String, MutableList<Contact>>()
        contacts.forEach { contact ->
            contact.emails.forEach { email ->
                val normalized = email.trim().lowercase()
                if (normalized.isNotBlank()) {
                    groups.getOrPut(normalized) { mutableListOf() }.add(contact)
                }
            }
        }

        return groups.mapNotNull { (key, group) ->
            val distinctContacts = group.distinctBy { it.id }
            if (distinctContacts.size > 1) {
                DuplicateGroup(
                    matchingKey = key,
                    duplicateType = DuplicateType.EMAIL_MATCH,
                    contacts = distinctContacts.sortedBy { it.name }
                )
            } else null
        }
    }

    private fun detectNameDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        return contacts
            .groupBy { it.name?.trim()?.lowercase() ?: "" }
            .filter { it.key.isNotEmpty() && it.value.size > 1 }
            .map { (name, duplicates) ->
                DuplicateGroup(
                    matchingKey = name,
                    duplicateType = DuplicateType.NAME_MATCH,
                    contacts = duplicates
                )
            }
    }

    fun detectSimilarNameDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val processedIds = mutableSetOf<Long>()
        val sortedContacts = contacts.filter { !it.name.isNullOrEmpty() }.sortedBy { it.name }

        // Limit comparison scope for performance
        for (i in sortedContacts.indices) {
            val contactA = sortedContacts[i]
            if (contactA.id in processedIds) continue

            val currentGroup = mutableListOf(contactA)
            val nameA = contactA.name!!

            // Sliding window: Look ahead up to 50 items
            val maxLookAhead = (i + 50).coerceAtMost(sortedContacts.size - 1)

            for (j in i + 1..maxLookAhead) {
                val contactB = sortedContacts[j]
                if (contactB.id in processedIds) continue

                val nameB = contactB.name!!

                // If first character differs, we've passed similar names
                if (!nameB.startsWith(nameA.take(1), ignoreCase = true)) break

                // Length filter
                if (abs(nameA.length - nameB.length) > 3) continue

                if (isSimilar(nameA, nameB)) {
                    currentGroup.add(contactB)
                    processedIds.add(contactB.id)
                }
            }

            if (currentGroup.size > 1) {
                groups.add(
                    DuplicateGroup(
                        matchingKey = nameA,
                        duplicateType = DuplicateType.SIMILAR_NAME_MATCH,
                        contacts = currentGroup
                    )
                )
                processedIds.add(contactA.id)
            }
        }
        return groups
    }

    private fun isSimilar(s1: String, s2: String): Boolean {
        val d1 = s1.trim().lowercase()
        val d2 = s2.trim().lowercase()
        if (d1 == d2) return false // Exact match is not "Similar"

        val dist = levenshteinDistance(d1, d2)
        val maxLength = maxOf(d1.length, d2.length)
        val similarity = if (maxLength == 0) 0.0 else (1.0 - dist.toDouble() / maxLength)
        return similarity > 0.82 // 82% threshold
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        var prev = IntArray(s2.length + 1) { it }
        var curr = IntArray(s2.length + 1)

        for (i in 1..s1.length) {
            curr[0] = i
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,        // Insert
                    prev[j] + 1,            // Delete
                    prev[j - 1] + cost      // Replace
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[s2.length]
    }

    fun normalizePhoneNumber(number: String, defaultRegion: String? = null): String {
        val region = defaultRegion ?: regionProvider.getRegionIso()
        return phoneNumberHandler.normalizeToE164(number, region)
    }
}
