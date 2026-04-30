package com.ogabassey.contactscleaner.data.source

import com.ogabassey.contactscleaner.domain.model.Contact

internal data class MergeTargetAccount(
    val accountType: String?,
    val accountName: String?
)

internal object AndroidMergeUtils {
    fun buildMergedContact(
        contacts: List<Contact>,
        customName: String? = null
    ): Contact? {
        if (contacts.size < 2) return null

        val targetAccount = selectTargetAccount(contacts)
        val primaryContact = contacts.firstOrNull {
            it.accountType == targetAccount.accountType && it.accountName == targetAccount.accountName
        } ?: contacts.firstOrNull()
            ?: return null

        val mergedNumbers = linkedSetOf<String>()
        val mergedEmails = linkedSetOf<String>()

        // ⚡ Bolt: Replaced multi-pass collection processing with indexed loops
        // to eliminate intermediate allocations and reduce GC pressure
        for (i in contacts.indices) {
            val contact = contacts[i]

            val numbers = contact.numbers
            for (j in numbers.indices) {
                val trimmed = numbers[j].trim()
                if (trimmed.isNotBlank()) {
                    mergedNumbers.add(trimmed)
                }
            }

            val emails = contact.emails
            for (j in emails.indices) {
                val trimmed = emails[j].trim()
                if (trimmed.isNotBlank()) {
                    mergedEmails.add(trimmed)
                }
            }
        }

        val mergedName = customName?.trim()?.takeIf { it.isNotBlank() }
            ?: contacts.firstNotNullOfOrNull { it.name?.trim()?.takeIf(String::isNotBlank) }
            ?: primaryContact.name

        if (mergedName.isNullOrBlank() && mergedNumbers.isEmpty() && mergedEmails.isEmpty()) {
            return null
        }

        return Contact(
            id = 0L,
            name = mergedName,
            numbers = mergedNumbers.toList(),
            emails = mergedEmails.toList(),
            normalizedNumber = mergedNumbers.firstOrNull(),
            accountType = targetAccount.accountType,
            accountName = targetAccount.accountName
        )
    }

    fun selectTargetAccount(contacts: List<Contact>): MergeTargetAccount {
        val googleAccount = contacts.firstOrNull { isGoogleAccount(it.accountType) && !it.accountName.isNullOrBlank() }
        if (googleAccount != null) {
            return MergeTargetAccount(googleAccount.accountType, googleAccount.accountName)
        }

        val syncableAccount = contacts.firstOrNull { !it.accountType.isNullOrBlank() }
        if (syncableAccount != null) {
            return MergeTargetAccount(syncableAccount.accountType, syncableAccount.accountName)
        }

        val fallback = contacts.firstOrNull()
        return MergeTargetAccount(fallback?.accountType, fallback?.accountName)
    }

    private fun isGoogleAccount(accountType: String?): Boolean {
        return accountType?.contains("google", ignoreCase = true) == true
    }
}
