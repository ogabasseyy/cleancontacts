package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.domain.model.Contact

internal object DuplicateMergeCandidateFilter {
    fun mergeableContacts(contacts: List<Contact>): List<Contact> {
        return contacts.filter(::isMergeable)
    }

    fun isMergeable(contact: Contact): Boolean {
        val accountType = contact.accountType?.trim()?.lowercase()
        if (accountType.isNullOrBlank()) return true

        return !accountType.contains("whatsapp") &&
            !accountType.contains("telegram")
    }
}
