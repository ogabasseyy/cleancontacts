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

        // Flatten to (NormalizedNumber, Contact) pairs
        val entries = contacts.flatMap { contact ->
            contact.numbers.map { number ->
                val normalized = phoneNumberHandler.normalizeToE164(number, defaultRegion)
                normalized to contact
            }
        }

        return collectDuplicates(entries, DuplicateType.NUMBER_MATCH)
    }

    private fun detectEmailDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        // Flatten to (NormalizedEmail, Contact) pairs
        val entries = contacts.flatMap { contact ->
            contact.emails.mapNotNull { email ->
                val normalized = email.trim().lowercase()
                if (normalized.isNotBlank()) normalized to contact else null
            }
        }

        return collectDuplicates(entries, DuplicateType.EMAIL_MATCH)
    }

    private fun detectNameDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        // Map to (NormalizedName, Contact) pairs
        val entries = contacts.mapNotNull { contact ->
            val name = contact.name?.trim()?.lowercase()
            if (!name.isNullOrEmpty()) name to contact else null
        }

        return collectDuplicates(entries, DuplicateType.NAME_MATCH)
    }

    /**
     * Generic sort-and-sweep logic to find duplicates without massive Map allocations.
     * Sorts O(N log N) and sweeps O(N).
     */
    private fun <K : Comparable<K>> collectDuplicates(
        entries: List<Pair<K, Contact>>,
        type: DuplicateType
    ): List<DuplicateGroup> {
        if (entries.isEmpty()) return emptyList()

        // Sort by key
        val sortedEntries = entries.sortedBy { it.first }
        val groups = mutableListOf<DuplicateGroup>()

        var i = 0
        while (i < sortedEntries.size) {
            val currentKey = sortedEntries[i].first
            var j = i + 1
            // Find end of current group
            while (j < sortedEntries.size && sortedEntries[j].first == currentKey) {
                j++
            }

            // Potential duplicate group found
            if (j - i > 1) {
                val groupContacts = ArrayList<Contact>(j - i)
                for (k in i until j) {
                    groupContacts.add(sortedEntries[k].second)
                }

                // Ensure distinct contacts (handle cases where same contact has same value twice)
                val distinctContacts = groupContacts.distinctBy { it.id }

                if (distinctContacts.size > 1) {
                    groups.add(
                        DuplicateGroup(
                            matchingKey = currentKey.toString(),
                            duplicateType = type,
                            contacts = distinctContacts.sortedBy { it.name }
                        )
                    )
                }
            }
            i = j
        }
        return groups
    }

    fun detectSimilarNameDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val processedIds = mutableSetOf<Long>()
        val sortedContacts = contacts.filter { !it.name.isNullOrEmpty() }.sortedBy { it.name }

        // Reuse buffers for Levenshtein distance to avoid frequent allocations
        val buffer1 = IntArray(MAX_NAME_LENGTH + 1)
        val buffer2 = IntArray(MAX_NAME_LENGTH + 1)

        // Limit comparison scope for performance
        for (i in sortedContacts.indices) {
            val contactA = sortedContacts[i]
            if (contactA.id in processedIds) continue

            val currentGroup = mutableListOf(contactA)
            // 2026 Best Practice: Avoid !! - use safe access with fallback
            val nameA = contactA.name ?: continue

            // 2026 Security: Skip excessively long names to prevent algorithmic DoS
            if (nameA.length > MAX_NAME_LENGTH) continue

            // Sliding window: Look ahead up to 50 items
            val maxLookAhead = (i + 50).coerceAtMost(sortedContacts.size - 1)

            for (j in i + 1..maxLookAhead) {
                val contactB = sortedContacts[j]
                if (contactB.id in processedIds) continue

                val nameB = contactB.name ?: continue

                if (nameB.length > MAX_NAME_LENGTH) continue

                // If first character differs, we've passed similar names
                if (!nameB.startsWith(nameA.take(1), ignoreCase = true)) break

                // Length filter
                if (abs(nameA.length - nameB.length) > 3) continue

                if (isSimilar(nameA, nameB, buffer1, buffer2)) {
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

    private fun isSimilar(s1: String, s2: String, buffer1: IntArray? = null, buffer2: IntArray? = null): Boolean {
        val d1 = s1.trim().lowercase()
        val d2 = s2.trim().lowercase()
        if (d1 == d2) return false // Exact match is not "Similar"

        val dist = levenshteinDistance(d1, d2, buffer1, buffer2)
        val maxLength = maxOf(d1.length, d2.length)
        val similarity = if (maxLength == 0) 0.0 else (1.0 - dist.toDouble() / maxLength)
        return similarity > 0.82 // 82% threshold
    }

    /**
     * Checks if the provided buffer pair is valid for reuse.
     */
    private fun canReuseBuffers(buffer1: IntArray?, buffer2: IntArray?, requiredLength: Int): Boolean {
        return buffer1 != null && buffer2 != null &&
            buffer1.size > requiredLength && buffer2.size > requiredLength
    }

    private fun levenshteinDistance(
        s1: String,
        s2: String,
        buffer1: IntArray? = null,
        buffer2: IntArray? = null
    ): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        var prev: IntArray
        var curr: IntArray

        // Use buffers if provided and large enough
        if (canReuseBuffers(buffer1, buffer2, s2.length)) {
            prev = buffer1!!
            curr = buffer2!!
            // Initialize prev row
            for (k in 0..s2.length) {
                prev[k] = k
            }
        } else {
            prev = IntArray(s2.length + 1) { it }
            curr = IntArray(s2.length + 1)
        }

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

    companion object {
        private const val MAX_NAME_LENGTH = 1000
    }

    fun normalizePhoneNumber(number: String, defaultRegion: String? = null): String {
        val region = defaultRegion ?: regionProvider.getRegionIso()
        return phoneNumberHandler.normalizeToE164(number, region)
    }
}
