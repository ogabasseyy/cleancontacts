package com.ogabassey.contactscleaner.data.source

import com.ogabassey.contactscleaner.domain.model.Contact
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import platform.Contacts.*
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.create

/**
 * iOS contacts data source using CNContactStore.
 *
 * 2026 KMP Best Practice: Platform-specific data source for iOS.
 */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class IosContactsSource {
    private val contactStore = CNContactStore()

    /**
     * Request contacts permission from the user using a blocking call.
     * This will trigger the iOS permission dialog if needed.
     */
    suspend fun requestContactsPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        contactStore.requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, error ->
            if (error != null) {
                println("Permission request error: ${error.localizedDescription}")
            }
            continuation.resume(granted)
        }
    }

    /**
     * Fetch all contacts from the iOS Contacts database.
     * Will request permission if needed.
     */
    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()

        // Request permission first (iOS will show dialog if not yet determined)
        val hasPermission = requestContactsPermission()
        if (!hasPermission) {
            println("Contacts permission not granted, returning empty list")
            return@withContext contacts
        }

        try {
            // Maxwell's 2026 Best Practice: Fetch by Container
            // Instead of one big fetch, we iterate containers to know the SOURCE of each contact.
            
            val containers = mutableListOf<CNContainer>()
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                // Fetch all containers (iCloud, Gmail, Local, Exchange, etc.)
                val fetchedContainers = contactStore.containersMatchingPredicate(null, errorPtr.ptr)
                
                fetchedContainers?.forEach {
                    (it as? CNContainer)?.let { container -> containers.add(container) }
                }

                if (errorPtr.value != null) {
                    println("Error fetching containers: ${errorPtr.value?.localizedDescription}")
                }
            }

            // Keys we need for the contacts (keep minimal set that works reliably)
            val keysToFetch = listOf(
                CNContactIdentifierKey,
                CNContactGivenNameKey,
                CNContactFamilyNameKey,
                CNContactMiddleNameKey,
                CNContactPhoneNumbersKey,
                CNContactEmailAddressesKey,
                CNContactSocialProfilesKey,
                CNContactInstantMessageAddressesKey,
                CNContactOrganizationNameKey
            )

            // Iterate each container and fetch its contacts
            for (container in containers) {
                // Determine Account Name & Type from Container (handle nulls)
                val safeAccountName = container.name ?: "Unknown"
                val accountType = when (container.type) {
                    CNContainerTypeExchange -> "Exchange"
                    CNContainerTypeCardDAV -> "CardDAV" // Likely Gmail/Yahoo
                    CNContainerTypeLocal -> "Local"
                    else -> "iCloud" // Default assumption for unclassified remote
                }

                // Refine CardDAV/Exchange names if possible (e.g. from container name)
                val refinedAccountType = when {
                    safeAccountName.lowercase().contains("gmail") -> "com.google"
                    safeAccountName.lowercase().contains("icloud") -> "com.apple.icloud"
                    safeAccountName.lowercase().contains("whatsapp") -> "com.whatsapp"
                    safeAccountName.lowercase().contains("telegram") -> "org.telegram"
                    else -> accountType
                }

                val predicate = CNContact.predicateForContactsInContainerWithIdentifier(container.identifier)
                val request = CNContactFetchRequest(keysToFetch = keysToFetch)
                request.predicate = predicate
                request.unifyResults = true // We still want unified view within the container context

                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    contactStore.enumerateContactsWithFetchRequest(
                        request,
                        error = errorPtr.ptr
                    ) { cnContact, _ ->
                        cnContact?.let {
                            val contact = cnContactToContact(it, refinedAccountType, safeAccountName)
                            contacts.add(contact)
                        }
                    }

                    if (errorPtr.value != null) {
                        println("Error fetching contacts from container ${safeAccountName}: ${errorPtr.value?.localizedDescription}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Error fetching contacts by container: ${e.message}")
        }

        contacts
    }

    /**
     * Convert a CNContact to our domain Contact model.
     */
    /**
     * Convert a CNContact to our domain Contact model.
     */
    private fun cnContactToContact(
        cnContact: CNContact, 
        accountType: String, 
        accountName: String
    ): Contact {
        val identifier = cnContact.identifier
        val givenName = cnContact.givenName
        val familyName = cnContact.familyName
        val displayName = buildDisplayName(givenName, familyName)

        // WhatsApp/Telegram Detection - check multiple sources
        var isWhatsApp = false
        var isTelegram = false

        // 1. Check account type/name from container (passed as parameters)
        val accountLower = accountType.lowercase()
        val accountNameLower = accountName.lowercase()
        if (accountLower.contains("whatsapp") || accountNameLower.contains("whatsapp")) {
            isWhatsApp = true
        }
        if (accountLower.contains("telegram") || accountNameLower.contains("telegram")) {
            isTelegram = true
        }

        // Extract phone numbers AND check labels for WhatsApp/Telegram
        val phoneNumbers = mutableListOf<String>()
        @Suppress("UNCHECKED_CAST")
        val cnPhoneNumbers = cnContact.phoneNumbers as? List<CNLabeledValue> ?: emptyList()
        for (labeledValue in cnPhoneNumbers) {
            val phoneNumber = labeledValue.value as? CNPhoneNumber
            phoneNumber?.let { phoneNumbers.add(it.stringValue) }

            // 2. Check phone number labels for WhatsApp/Telegram
            val label = labeledValue.label?.lowercase() ?: ""
            if (label.contains("whatsapp")) isWhatsApp = true
            if (label.contains("telegram")) isTelegram = true
        }

        // Extract emails - value is NSString, convert to Kotlin String
        val emails = mutableListOf<String>()
        @Suppress("UNCHECKED_CAST")
        val cnEmails = cnContact.emailAddresses as? List<CNLabeledValue> ?: emptyList()
        for (labeledValue in cnEmails) {
            // NSString.toString() converts to Kotlin String
            labeledValue.value?.toString()?.let { emails.add(it) }
        }

        // 3. Check Social Profiles
        @Suppress("UNCHECKED_CAST")
        val socialProfiles = cnContact.socialProfiles as? List<CNLabeledValue> ?: emptyList()
        for (profile in socialProfiles) {
            val value = profile.value as? CNSocialProfile ?: continue
            val service = value.service?.lowercase() ?: ""
            val profileLabel = profile.label?.lowercase() ?: ""
            if (service.contains("whatsapp") || profileLabel.contains("whatsapp")) isWhatsApp = true
            if (service.contains("telegram") || profileLabel.contains("telegram")) isTelegram = true
        }

        // 4. Check Instant Message Addresses
        @Suppress("UNCHECKED_CAST")
        val imAddresses = cnContact.instantMessageAddresses as? List<CNLabeledValue> ?: emptyList()
        for (im in imAddresses) {
            val value = im.value as? CNInstantMessageAddress ?: continue
            val service = value.service?.lowercase() ?: ""
            val imLabel = im.label?.lowercase() ?: ""
            if (service.contains("whatsapp") || imLabel.contains("whatsapp")) isWhatsApp = true
            if (service.contains("telegram") || imLabel.contains("telegram")) isTelegram = true
        }

        // 5. Check organization name (some synced contacts have app name as org)
        val orgName = cnContact.organizationName.lowercase()
        if (orgName.contains("whatsapp")) isWhatsApp = true
        if (orgName.contains("telegram")) isTelegram = true

        // Generate a numeric ID from the identifier string
        val numericId = identifier.hashCode().toLong().let { if (it < 0) -it else it }

        return Contact(
            id = numericId,
            name = displayName,
            numbers = phoneNumbers,
            emails = emails,
            normalizedNumber = phoneNumbers.firstOrNull()?.normalizePhoneNumber(),
            isWhatsApp = isWhatsApp,
            isTelegram = isTelegram,
            isJunk = false,
            junkType = null,
            duplicateType = null,
            accountType = accountType,
            accountName = accountName,
            isSensitive = false,
            sensitiveDescription = null,
            formatIssue = null
        )
    }

    private fun buildDisplayName(givenName: String, familyName: String): String {
        return when {
            givenName.isNotBlank() && familyName.isNotBlank() -> "$givenName $familyName"
            givenName.isNotBlank() -> givenName
            familyName.isNotBlank() -> familyName
            else -> ""
        }
    }

    /**
     * Delete contacts by their identifiers.
     */
    suspend fun deleteContacts(contactIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        // Note: iOS contact deletion requires the full identifier string
        // This is a simplified implementation - in production you'd need to map IDs back to identifiers
        try {
            val saveRequest = CNSaveRequest()

            // We need to fetch contacts first to get their mutable copies
            val keysToFetch = listOf(CNContactIdentifierKey)
            val request = CNContactFetchRequest(keysToFetch = keysToFetch)

            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.enumerateContactsWithFetchRequest(
                    request,
                    error = errorPtr.ptr
                ) { cnContact, _ ->
                    cnContact?.let { contact ->
                        val contactId = contact.identifier.hashCode().toLong().let { if (it < 0) -it else it }
                        if (contactId in contactIds) {
                            val mutableContact = contact.mutableCopy() as? CNMutableContact
                            mutableContact?.let { saveRequest.deleteContact(it) }
                        }
                    }
                }

                val saveErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.executeSaveRequest(saveRequest, error = saveErrorPtr.ptr)
                saveErrorPtr.value?.let { error ->
                    println("Error saving delete request: ${error.localizedDescription}")
                    return@withContext false
                }
            }
            true
        } catch (e: Exception) {
            println("Error deleting contacts: ${e.message}")
            false
        }
    }

    /**
     * Get contact count.
     */
    suspend fun getContactCount(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val keysToFetch = listOf(CNContactIdentifierKey)
        val request = CNContactFetchRequest(keysToFetch = keysToFetch)

        try {
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.enumerateContactsWithFetchRequest(
                    request,
                    error = errorPtr.ptr
                ) { _, _ ->
                    count++
                }
            }
        } catch (e: Exception) {
            println("Error counting contacts: ${e.message}")
        }

        count
    }

    /**
     * Restore contacts (add new contacts).
     */
    suspend fun restoreContacts(contacts: List<Contact>): Boolean = withContext(Dispatchers.IO) {
        try {
            val saveRequest = CNSaveRequest()

            for (contact in contacts) {
                val newContact = CNMutableContact()

                // Set name
                contact.name?.let { name ->
                    val parts = name.split(" ", limit = 2)
                    newContact.setGivenName(parts.getOrNull(0) ?: "")
                    newContact.setFamilyName(parts.getOrNull(1) ?: "")
                }

                // Set phone numbers
                val phoneNumbers = contact.numbers.mapNotNull { number ->
                    val phoneNumber = CNPhoneNumber.phoneNumberWithStringValue(number)
                    CNLabeledValue.labeledValueWithLabel(
                        CNLabelPhoneNumberMobile,
                        phoneNumber
                    )
                }
                newContact.setPhoneNumbers(phoneNumbers)

                // Set emails - need to convert String to NSString for NSCopyingProtocol
                val emailAddresses = contact.emails.mapNotNull { email ->
                    @Suppress("CAST_NEVER_SUCCEEDS")
                    val nsEmail = NSString.create(string = email)
                    CNLabeledValue.labeledValueWithLabel(CNLabelHome, nsEmail)
                }
                newContact.setEmailAddresses(emailAddresses)

                saveRequest.addContact(newContact, toContainerWithIdentifier = null)
            }

            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.executeSaveRequest(saveRequest, error = errorPtr.ptr)
                errorPtr.value?.let { error ->
                    println("Error saving contacts: ${error.localizedDescription}")
                    return@withContext false
                }
            }
            true
        } catch (e: Exception) {
            println("Error restoring contacts: ${e.message}")
            false
        }
    }

    /**
     * Merge contacts (aggregate into one).
     * Note: iOS doesn't have the same aggregation concept as Android.
     * This implementation creates a new contact with merged data and deletes the old ones.
     */
    suspend fun mergeContacts(contactIds: List<Long>, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (contactIds.size < 2) return@withContext false

        try {
            // Fetch all contacts to merge
            val contactsToMerge = mutableListOf<Contact>()
            val keysToFetch = listOf(
                CNContactIdentifierKey,
                CNContactGivenNameKey,
                CNContactFamilyNameKey,
                CNContactPhoneNumbersKey,
                CNContactEmailAddressesKey
            )
            val request = CNContactFetchRequest(keysToFetch = keysToFetch)

            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.enumerateContactsWithFetchRequest(
                    request,
                    error = errorPtr.ptr
                ) { cnContact, _ ->
                    cnContact?.let { contact ->
                        val contactId = contact.identifier.hashCode().toLong().let { if (it < 0) -it else it }
                        if (contactId in contactIds) {
                            contactsToMerge.add(cnContactToContact(contact, "Local", "Device"))
                        }
                    }
                }
            }

            if (contactsToMerge.size < 2) return@withContext false

            // Create merged contact
            val primaryContact = contactsToMerge.first()
            val allNumbers = contactsToMerge.flatMap { it.numbers }.distinct()
            val allEmails = contactsToMerge.flatMap { it.emails }.distinct()

            val mergedContact = Contact(
                id = 0,
                name = customName ?: primaryContact.name,
                numbers = allNumbers,
                emails = allEmails,
                normalizedNumber = allNumbers.firstOrNull()
            )

            // Delete old contacts and add merged one
            val deleted = deleteContacts(contactIds)
            if (!deleted) return@withContext false

            restoreContacts(listOf(mergedContact))
        } catch (e: Exception) {
            println("Error merging contacts: ${e.message}")
            false
        }
    }

    /**
     * Update a contact's phone number.
     */
    suspend fun updateContactNumber(contactId: Long, newNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val keysToFetch = listOf(
                CNContactIdentifierKey,
                CNContactPhoneNumbersKey
            )
            val request = CNContactFetchRequest(keysToFetch = keysToFetch)

            var updated = false
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.enumerateContactsWithFetchRequest(
                    request,
                    error = errorPtr.ptr
                ) { cnContact, stop ->
                    cnContact?.let { contact ->
                        val id = contact.identifier.hashCode().toLong().let { if (it < 0) -it else it }
                        if (id == contactId) {
                            val mutableContact = contact.mutableCopy() as? CNMutableContact
                            mutableContact?.let { mutable ->
                                // Update the first phone number
                                val newPhoneNumbers = mutableListOf<Any?>()
                                val newLabeledValue = CNLabeledValue.labeledValueWithLabel(
                                    CNLabelPhoneNumberMobile,
                                    CNPhoneNumber.phoneNumberWithStringValue(newNumber)
                                )
                                newPhoneNumbers.add(newLabeledValue)

                                // Keep other numbers
                                @Suppress("UNCHECKED_CAST")
                                val existingNumbers = contact.phoneNumbers as? List<CNLabeledValue> ?: emptyList()
                                if (existingNumbers.size > 1) {
                                    newPhoneNumbers.addAll(existingNumbers.drop(1))
                                }

                                mutable.setPhoneNumbers(newPhoneNumbers)

                                val saveRequest = CNSaveRequest()
                                saveRequest.updateContact(mutable)

                                val saveErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                                contactStore.executeSaveRequest(saveRequest, error = saveErrorPtr.ptr)

                                if (saveErrorPtr.value == null) {
                                    updated = true
                                }
                            }
                            // Stop enumeration by setting the pointer value
                            stop?.pointed?.value = true
                        }
                    }
                }
            }

            updated
        } catch (e: Exception) {
            println("Error updating contact: ${e.message}")
            false
        }
    }

    /**
     * Simple phone number normalization.
     */
    private fun String.normalizePhoneNumber(): String {
        return this.filter { it.isDigit() || it == '+' }
    }
}
