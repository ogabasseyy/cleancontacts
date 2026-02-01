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
            // Check if continuation is still active before resuming (prevents crash if cancelled)
            if (continuation.isActive) {
                continuation.resume(granted)
            }
        }
    }

    /**
     * 2026 Best Practice: Check permission before write operations.
     * Returns false if permission not granted, preventing silent failures.
     */
    private suspend fun ensureWritePermission(): Boolean {
        val hasPermission = requestContactsPermission()
        if (!hasPermission) {
            println("Contacts write permission not granted")
        }
        return hasPermission
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
            platform_uid = identifier,
            isSensitive = false,
            sensitiveDescription = null,
            formatIssue = null
        )
    }

    private fun buildDisplayName(givenName: String, familyName: String): String {
        val rawName = when {
            givenName.isNotBlank() && familyName.isNotBlank() -> "$givenName $familyName"
            givenName.isNotBlank() -> givenName
            familyName.isNotBlank() -> familyName
            else -> ""
        }.trim()

        // 2026 Fix: Sanitize names starting with slashes (e.g., from certain sync sources or SIM imports)
        // Use trimStart('/') to handle multiple leading slashes (e.g., "///John" -> "John")
        return rawName.trimStart('/').trim()
    }

    /**
     * Delete contacts by their identifiers.
     */
    suspend fun deleteContacts(uids: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (uids.isEmpty()) return@withContext true

        // 2026 Best Practice: Check permission before write operations
        if (!ensureWritePermission()) return@withContext false

        try {
            val saveRequest = CNSaveRequest()
            
            // 2026 Optimization: Fetch ONLY the contacts we want to delete using a predicate
            val predicate = CNContact.predicateForContactsWithIdentifiers(uids)
            val keysToFetch = listOf(CNContactIdentifierKey)
            
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val contactsToDelete = contactStore.unifiedContactsMatchingPredicate(
                    predicate,
                    keysToFetch = keysToFetch,
                    error = errorPtr.ptr
                )
                
                if (errorPtr.value != null) {
                    println("Error fetching contacts for deletion: ${errorPtr.value?.localizedDescription}")
                    return@withContext false
                }
                
                contactsToDelete?.forEach { obj ->
                    val contact = obj as? CNContact
                    val mutableContact = contact?.mutableCopy() as? CNMutableContact
                    mutableContact?.let { saveRequest.deleteContact(it) }
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
        // 2026 Best Practice: Check permission before write operations
        if (!ensureWritePermission()) return@withContext false

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
                    val phoneNumber = CNPhoneNumber.phoneNumberWithStringValue(stringValue = number)
                    CNLabeledValue.labeledValueWithLabel(
                        label = CNLabelPhoneNumberMobile,
                        value = phoneNumber
                    )
                }
                newContact.setPhoneNumbers(phoneNumbers)

                // Set emails
                val emailAddresses = contact.emails.mapNotNull { email ->
                    CNLabeledValue.labeledValueWithLabel(
                        label = CNLabelHome,
                        value = NSString.create(string = email)
                    )
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
     *
     * @param platformUids List of iOS CNContact identifiers to merge
     * @param customName Optional custom name for the merged contact
     */
    suspend fun mergeContacts(platformUids: List<String>, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (platformUids.size < 2) return@withContext false

        // 2026 Best Practice: Check permission before write operations
        if (!ensureWritePermission()) return@withContext false

        try {
            // Fetch contacts by their platform UIDs
            val contactsToMerge = mutableListOf<Contact>()
            // 2026 Fix: Include all keys needed by cnContactToContact to avoid runtime crashes
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

            for (uid in platformUids) {
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val cnContact = contactStore.unifiedContactWithIdentifier(uid, keysToFetch, errorPtr.ptr)
                    val nsError = errorPtr.value
                    when {
                        nsError != null -> {
                            // 2026 Best Practice: Log errors for invalid UIDs instead of silent failure
                            println("⚠️ Failed to fetch contact UID '$uid': ${nsError.localizedDescription}")
                        }
                        cnContact != null -> {
                            contactsToMerge.add(cnContactToContact(cnContact, "Local", "Device"))
                        }
                        else -> {
                            // 2026 Best Practice: Log when UID doesn't resolve to a contact
                            println("⚠️ Contact not found for UID '$uid'")
                        }
                    }
                }
            }

            if (contactsToMerge.size < 2) return@withContext false

            // Create merged contact
            // 2026 Best Practice: Use firstOrNull() for defensive coding even after size check
            val primaryContact = contactsToMerge.firstOrNull() ?: return@withContext false
            val allNumbers = contactsToMerge.flatMap { it.numbers }.distinct()
            val allEmails = contactsToMerge.flatMap { it.emails }.distinct()

            val mergedContact = Contact(
                id = 0L,
                name = customName ?: primaryContact.name,
                numbers = allNumbers,
                emails = allEmails,
                normalizedNumber = allNumbers.firstOrNull()
            )

            // 2026 Fix: Atomic merge - create new contact FIRST, then delete old ones
            // This prevents data loss if creation fails
            val restored = restoreContacts(listOf(mergedContact))
            if (!restored) {
                println("Failed to create merged contact - aborting merge to prevent data loss")
                return@withContext false
            }

            // Only delete old contacts after new one is successfully created
            val deleted = deleteContacts(platformUids)
            if (!deleted) {
                println("Warning: Merged contact created but failed to delete old contacts")
                // Still return true since the merge data is preserved
            }
            true
        } catch (e: Exception) {
            println("Error merging contacts: ${e.message}")
            false
        }
    }

    /**
     * Update a contact's phone number.
     *
     * @param platformUid iOS CNContact identifier
     * @param newNumber New phone number to set
     */
    suspend fun updateContactNumber(platformUid: String, newNumber: String): Boolean = withContext(Dispatchers.IO) {
        // 2026 Best Practice: Check permission before write operations
        if (!ensureWritePermission()) return@withContext false

        try {
            val keysToFetch = listOf(
                CNContactIdentifierKey,
                CNContactPhoneNumbersKey
            )

            // 2026 Fix: Extract result from memScoped to ensure proper propagation
            val success = memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val cnContact = contactStore.unifiedContactWithIdentifier(platformUid, keysToFetch, errorPtr.ptr)

                if (cnContact == null || errorPtr.value != null) {
                    println("Contact not found for UID: $platformUid")
                    return@memScoped false
                }

                val mutableContact = cnContact.mutableCopy() as? CNMutableContact
                    ?: return@memScoped false

                // Update the first phone number
                val newPhoneNumbers = mutableListOf<Any?>()
                val newLabeledValue = CNLabeledValue.labeledValueWithLabel(
                    label = CNLabelPhoneNumberMobile,
                    value = CNPhoneNumber.phoneNumberWithStringValue(stringValue = newNumber)
                )
                newPhoneNumbers.add(newLabeledValue)

                // Keep other numbers
                @Suppress("UNCHECKED_CAST")
                val existingNumbers = cnContact.phoneNumbers as? List<CNLabeledValue> ?: emptyList()
                if (existingNumbers.size > 1) {
                    newPhoneNumbers.addAll(existingNumbers.drop(1))
                }

                mutableContact.setPhoneNumbers(newPhoneNumbers)

                val saveRequest = CNSaveRequest()
                saveRequest.updateContact(mutableContact)

                val saveErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                contactStore.executeSaveRequest(saveRequest, error = saveErrorPtr.ptr)

                saveErrorPtr.value == null
            }
            success
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

    /**
     * Get specific contacts by their identifiers.
     * Efficient O(1) fetch for refresh operations.
     */
    suspend fun getContactsByUids(uids: List<String>): List<Contact> = withContext(Dispatchers.IO) {
        if (uids.isEmpty()) return@withContext emptyList()

        // 2026 Best Practice: Check permission before read operations
        val hasPermission = requestContactsPermission()
        if (!hasPermission) {
            println("Contacts permission not granted for fetching by UIDs")
            return@withContext emptyList()
        }

        val contacts = mutableListOf<Contact>()
        try {
            val predicate = CNContact.predicateForContactsWithIdentifiers(uids)
            // Include all keys needed by cnContactToContact to avoid runtime crashes
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

            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val fetchedContacts = contactStore.unifiedContactsMatchingPredicate(
                    predicate,
                    keysToFetch = keysToFetch,
                    error = errorPtr.ptr
                )

                if (errorPtr.value != null) {
                    println("Error fetching contacts by UIDs: ${errorPtr.value?.localizedDescription}")
                    return@memScoped
                }

                fetchedContacts?.forEach { obj ->
                    val cnContact = obj as? CNContact ?: return@forEach
                    // For refresh, we assume Local/Device context since we lost the original container info
                    // But usually, these IDs persist. If we really need account info, we'd need to re-fetch containers 
                    // check container of this contact. For now, "Local" is a safe fallback for display logic.
                    // To be perfect, we could re-query container for each contact, but that's expensive (N queries).
                    val contact = cnContactToContact(cnContact, "Local", "Device")
                    contacts.add(contact)
                }
            }
        } catch (e: Exception) {
            println("Error fetching contacts by UIDs: ${e.message}")
        }
        contacts
    }
}
