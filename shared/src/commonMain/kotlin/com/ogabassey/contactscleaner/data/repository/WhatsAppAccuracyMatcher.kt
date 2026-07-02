package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.util.extractDigits

object WhatsAppAccuracyMatcher {
    fun extractUniquePhoneNumbers(contacts: List<LocalContact>): List<String> {
        val numbers = LinkedHashSet<String>()
        for (contact in contacts) {
            collectNumbers(contact.rawNumbers, numbers)
        }
        return numbers.toList()
    }

    fun applyResults(
        contacts: List<LocalContact>,
        results: Map<String, Boolean>
    ): List<LocalContact> {
        if (results.isEmpty()) return contacts

        return contacts.map { contact ->
            val checkedMatches = extractNumbers(contact.rawNumbers).mapNotNull { number ->
                results[number]
            }

            if (checkedMatches.isEmpty()) {
                contact
            } else {
                contact.copy(isWhatsApp = checkedMatches.any { it })
            }
        }
    }

    fun normalizeResults(results: Map<String, Boolean>): Map<String, Boolean> {
        if (results.isEmpty()) return emptyMap()

        val normalized = LinkedHashMap<String, Boolean>()
        for ((number, hasWhatsApp) in results) {
            val digits = number.extractDigits()
            if (digits.length in 8..15) {
                normalized[digits] = hasWhatsApp
            }
        }
        return normalized
    }

    private fun collectNumbers(rawNumbers: String, target: MutableSet<String>) {
        for (number in extractNumbers(rawNumbers)) {
            target.add(number)
        }
    }

    private fun extractNumbers(rawNumbers: String): List<String> {
        if (rawNumbers.isBlank()) return emptyList()

        val numbers = ArrayList<String>()
        val parts = rawNumbers.split(',')
        for (part in parts) {
            val digits = part.extractDigits()
            if (digits.length in 8..15) {
                numbers.add(digits)
            }
        }
        return numbers
    }
}
