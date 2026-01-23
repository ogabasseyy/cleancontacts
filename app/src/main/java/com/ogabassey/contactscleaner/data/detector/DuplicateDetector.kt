package com.ogabassey.contactscleaner.data.detector

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.DuplicateGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class DuplicateDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun detectDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val allDuplicates = mutableListOf<DuplicateGroup>()
        
        // 1. Number Duplicates (Strict + E164 Normalization)
        allDuplicates.addAll(detectNumberDuplicates(contacts))
        
        // 2. Email Duplicates (Strict)
        allDuplicates.addAll(detectEmailDuplicates(contacts))
        
        // 3. Name Duplicates (Fuzzy & Exact)
        allDuplicates.addAll(detectNameDuplicates(contacts))

        return allDuplicates
    }

    private fun detectNumberDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        val defaultRegion = getDeviceCountryIso()
        val groups = mutableMapOf<String, MutableList<Contact>>()
        
        contacts.forEach { contact ->
            contact.numbers.forEach { number ->
                val normalized = normalizePhoneNumber(number, defaultRegion)
                groups.getOrPut(normalized) { mutableListOf() }.add(contact)
            }
        }

        return groups.mapNotNull { (key, group) ->
            val distinctContacts = group.distinctBy { it.id }
            if (distinctContacts.size > 1) {
                DuplicateGroup(
                    matchingKey = key,
                    duplicateType = com.ogabassey.contactscleaner.domain.model.DuplicateType.NUMBER_MATCH,
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
                    duplicateType = com.ogabassey.contactscleaner.domain.model.DuplicateType.EMAIL_MATCH,
                    contacts = distinctContacts.sortedBy { it.name }
                )
            } else null
        }
    }

    private fun detectNameDuplicates(contacts: List<Contact>): List<DuplicateGroup> {
        // Strict Name Match
        return contacts
            .groupBy { it.name?.trim()?.lowercase() ?: "" }
            .filter { it.key.isNotEmpty() && it.value.size > 1 }
            .map { (name, duplicates) ->
                DuplicateGroup(
                    matchingKey = name,
                    duplicateType = com.ogabassey.contactscleaner.domain.model.DuplicateType.NAME_MATCH,
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

            // Sliding window: Look ahead up to 50 items. 
            // Since sorted, similar names are guaranteed to be nearby.
            val maxLookAhead = (i + 50).coerceAtMost(sortedContacts.size - 1)
            
            for (j in i + 1..maxLookAhead) {
                val contactB = sortedContacts[j]
                if (contactB.id in processedIds) continue
                
                val nameB = contactB.name!!
                
                // If the first character is already different, we've likely passed similar names
                if (!nameB.startsWith(nameA.take(1), ignoreCase = true)) break

                // Length filter check
                if (kotlin.math.abs(nameA.length - nameB.length) > 3) continue

                if (isSimilar(nameA, nameB)) {
                    currentGroup.add(contactB)
                    processedIds.add(contactB.id)
                }
            }

            if (currentGroup.size > 1) {
                groups.add(
                    DuplicateGroup(
                        matchingKey = nameA, 
                        duplicateType = com.ogabassey.contactscleaner.domain.model.DuplicateType.SIMILAR_NAME_MATCH,
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
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun getDeviceCountryIso(): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso
            val networkCountry = tm?.networkCountryIso
            
            when {
                !simCountry.isNullOrBlank() -> simCountry.uppercase()
                !networkCountry.isNullOrBlank() -> networkCountry.uppercase()
                else -> Locale.getDefault().country
            }
        } catch (e: Exception) {
            Locale.getDefault().country
        }
    }

    fun normalizePhoneNumber(number: String, defaultRegion: String? = null): String {
        val region = defaultRegion ?: getDeviceCountryIso()
        val formatted = try {
             PhoneNumberUtils.formatNumberToE164(number, region)
        } catch (e: Exception) {
            null
        }
        
        return formatted ?: number.filter { it.isDigit() || it == '+' }
    }
}
