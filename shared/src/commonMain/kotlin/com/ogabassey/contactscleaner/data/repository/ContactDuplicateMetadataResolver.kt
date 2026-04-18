package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.DuplicateGroup
import com.ogabassey.contactscleaner.domain.model.DuplicateType
import com.ogabassey.contactscleaner.util.splitAndFilterNotBlank

object ContactDuplicateMetadataResolver {

    fun apply(contacts: List<LocalContact>, duplicateDetector: DuplicateDetector): List<LocalContact> {
        if (contacts.isEmpty()) return contacts

        val domainContacts = contacts.map { it.toDuplicateContact() }
        val duplicateGroups = duplicateDetector.detectDuplicates(domainContacts) +
            duplicateDetector.detectSimilarNameDuplicates(domainContacts)
        val assignments = buildDuplicateAssignments(duplicateGroups)

        return contacts.map { contact ->
            val assignment = assignments[contact.id]
            contact.copy(
                duplicateType = assignment?.first?.name,
                matchingKey = assignment?.second ?: defaultMatchingKey(contact)
            )
        }
    }

    internal fun buildDuplicateAssignments(
        groups: List<DuplicateGroup>
    ): Map<Long, Pair<DuplicateType, String>> {
        val assignments = mutableMapOf<Long, Pair<DuplicateType, String>>()

        groups.forEach { group ->
            group.contacts.forEach { contact ->
                val current = assignments[contact.id]
                if (current == null || priorityOf(group.duplicateType) >= priorityOf(current.first)) {
                    assignments[contact.id] = group.duplicateType to group.matchingKey
                }
            }
        }

        return assignments
    }

    internal fun defaultMatchingKey(contact: LocalContact): String? {
        return contact.normalizedNumber?.takeIf { it.isNotBlank() }
            ?: contact.rawEmails
                .splitAndFilterNotBlank(',')
                .firstOrNull()
                ?.trim()
                ?.lowercase()
            ?: contact.displayName
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
    }

    private fun priorityOf(type: DuplicateType): Int = when (type) {
        DuplicateType.NUMBER_MATCH -> 4
        DuplicateType.EMAIL_MATCH -> 3
        DuplicateType.NAME_MATCH -> 2
        DuplicateType.SIMILAR_NAME_MATCH -> 1
    }

    private fun LocalContact.toDuplicateContact(): Contact {
        return Contact(
            id = id,
            name = displayName,
            numbers = rawNumbers.splitAndFilterNotBlank(','),
            emails = rawEmails.splitAndFilterNotBlank(','),
            normalizedNumber = normalizedNumber,
            isWhatsApp = isWhatsApp,
            isTelegram = isTelegram,
            accountType = accountType,
            accountName = accountName,
            platform_uid = platformUid,
            matchingKey = matchingKey
        )
    }
}
