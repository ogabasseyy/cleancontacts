package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.LocalContact

object AndroidWhatsAppFlagResolver {
    fun contactsNeedingLinkedCacheUpdate(
        contacts: List<LocalContact>,
        cachedNumbers: Set<String>
    ): List<LocalContact> {
        if (contacts.isEmpty()) return emptyList()

        val updatedContacts = ArrayList<LocalContact>()
        for (i in contacts.indices) {
            val contact = contacts[i]
            val isOnWhatsApp = WhatsAppCacheMatcher.hasCachedWhatsAppNumber(
                rawNumbers = contact.rawNumbers,
                cachedNumbers = cachedNumbers
            )
            if (contact.isWhatsApp != isOnWhatsApp) {
                updatedContacts.add(contact.copy(isWhatsApp = isOnWhatsApp))
            }
        }
        return updatedContacts
    }

    fun contactsNeedingNativeRestore(
        contacts: List<LocalContact>,
        nativeWhatsAppIds: Set<Long>
    ): List<LocalContact> {
        if (contacts.isEmpty()) return emptyList()

        val updatedContacts = ArrayList<LocalContact>()
        for (i in contacts.indices) {
            val contact = contacts[i]
            val isNativeWhatsApp = contact.id in nativeWhatsAppIds
            if (contact.isWhatsApp != isNativeWhatsApp) {
                updatedContacts.add(contact.copy(isWhatsApp = isNativeWhatsApp))
            }
        }
        return updatedContacts
    }
}
