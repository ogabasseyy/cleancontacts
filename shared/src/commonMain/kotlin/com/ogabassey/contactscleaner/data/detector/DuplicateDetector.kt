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

        // 2026 Optimization: Flatten and sort instead of using groupBy (Map)
        // This avoids creating Map nodes and small ArrayLists for every unique key.
        val flattened = contacts.flatMap { contact ->
            contact.numbers.map { number ->
                phoneNumberHandler.normalizeToE164(number, defaultRegion) to contact
            }
        }

        return collectDuplicates(flattened, DuplicateType.NUMBER_MATCH)
    }

    private fun detectEmailDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        // 2026 Optimization: Flatten and sort instead of using groupBy (Map)
        val flattened = contacts.flatMap { contact ->
            contact.emails.mapNotNull { email ->
                val normalized = email.trim().lowercase()
                if (normalized.isNotBlank()) {
                    normalized to contact
                } else null
            }
        }

        return collectDuplicates(flattened, DuplicateType.EMAIL_MATCH)
    }

    private fun detectNameDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        // 2026 Optimization: Map and sort instead of using groupBy (Map)
        val items = contacts.mapNotNull { contact ->
            val name = contact.name?.trim()?.lowercase()
            if (!name.isNullOrEmpty()) {
                name to contact
            } else null
        }

        return collectDuplicates(items, DuplicateType.NAME_MATCH)
    }

    /**
     * Generic sort-and-sweep helper to detect duplicates.
     * Sorts by key and collects adjacent items with the same key.
     * Avoids O(N) map allocations in favor of O(N log N) sort which is friendlier to memory.
     */
    private fun collectDuplicates(
        items: List<Pair<String, Contact>>,
        duplicateType: DuplicateType
    ): List<DuplicateGroup> {
        if (items.isEmpty()) return emptyList()

        val sorted = items.sortedBy { it.first }
        val groups = mutableListOf<DuplicateGroup>()

        var i = 0
        while (i < sorted.size) {
            val key = sorted[i].first
            var j = i + 1
            // Find end of current group
            while (j < sorted.size && sorted[j].first == key) {
                j++
            }

            // If we found duplicates (more than 1 item in group)
            if (j - i > 1) {
                val groupContacts = sorted.subList(i, j).map { it.second }.distinctBy { it.id }
                if (groupContacts.size > 1) {
                    groups.add(
                        DuplicateGroup(
                            matchingKey = key,
                            duplicateType = duplicateType,
                            contacts = groupContacts.sortedBy { it.name }
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

        // 2026 Optimization: Pre-calculate normalized names to avoid O(N * W) allocations
        // W = Window Size (50). This reduces String allocations by ~50x in the hot loop.
        val processedContacts = contacts
            .filter { !it.name.isNullOrEmpty() }
            .map { ProcessedContact(it, it.name!!.trim().lowercase()) }
            .sortedBy { it.cleanName }

        // Reuse buffers for Levenshtein distance to avoid frequent allocations
        val buffer1 = IntArray(MAX_NAME_LENGTH + 1)
        val buffer2 = IntArray(MAX_NAME_LENGTH + 1)

        // Limit comparison scope for performance
        for (i in processedContacts.indices) {
            val pContactA = processedContacts[i]
            val contactA = pContactA.contact

            if (contactA.id in processedIds) continue

            val currentGroup = mutableListOf(contactA)
            val nameA = contactA.name!! // Safe due to earlier filter
            val cleanNameA = pContactA.cleanName

            // 2026 Security: Skip excessively long names to prevent algorithmic DoS
            if (cleanNameA.length > MAX_NAME_LENGTH) continue

            // Hoist prefix outside inner loop to avoid String allocation per iteration
            val cleanNameAPrefix = cleanNameA.take(1)

            // Sliding window: Look ahead up to 50 items
            val maxLookAhead = (i + 50).coerceAtMost(processedContacts.size - 1)

            for (j in i + 1..maxLookAhead) {
                val pContactB = processedContacts[j]
                val contactB = pContactB.contact

                if (contactB.id in processedIds) continue

                val cleanNameB = pContactB.cleanName

                // 2026 Security: Skip excessively long names to prevent algorithmic DoS
                if (cleanNameB.length > MAX_NAME_LENGTH) continue

                // If first character differs, we've passed similar names
                if (!cleanNameB.startsWith(cleanNameAPrefix)) break

                // Length filter
                if (abs(cleanNameA.length - cleanNameB.length) > 3) continue

                if (isSimilar(cleanNameA, cleanNameB, buffer1, buffer2)) {
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

    private fun isSimilar(d1: String, d2: String, buffer1: IntArray? = null, buffer2: IntArray? = null): Boolean {
        // 2026 Optimization: Inputs are already normalized (trimmed & lowercased)
        if (d1 == d2) return false // Exact match is not "Similar"

        val dist = levenshteinDistance(d1, d2, buffer1, buffer2)
        val maxLength = maxOf(d1.length, d2.length)
        val similarity = if (maxLength == 0) 0.0 else (1.0 - dist.toDouble() / maxLength)
        return similarity > 0.82 // 82% threshold
    }

    /**
     * Checks if the provided buffer pair is valid for reuse.
     * Buffers must be non-null and large enough to hold s2.length + 1 elements.
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

private data class ProcessedContact(
    val contact: Contact,
    val cleanName: String
)
