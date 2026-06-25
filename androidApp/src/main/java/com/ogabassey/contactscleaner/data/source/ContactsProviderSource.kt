package com.ogabassey.contactscleaner.data.source

import com.ogabassey.contactscleaner.platform.Logger

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.os.RemoteException
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.ogabassey.contactscleaner.domain.model.Contact
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ContactsProviderSource(
    private val context: Context,
    private val contentResolver: ContentResolver
) {

    private companion object {
        const val MAX_PROVIDER_BATCH = 200
    }

    private data class RestoredContactResult(
        val contact: Contact,
        val contactId: Long?,
        val rawContactId: Long?
    )

    // 2026 Best Practice: Defensive permission checks in data layer
    // These are safety nets - UI layer should request permissions before calling these methods
    private fun hasReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadWritePermissions(operation: String): Boolean {
        val canRead = hasReadPermission()
        val canWrite = hasWritePermission()
        if (!canRead || !canWrite) {
            Logger.w(
                "ContactsProviderSource",
                "READ/WRITE_CONTACTS permission not granted for $operation canRead=$canRead canWrite=$canWrite"
            )
            return false
        }
        return true
    }

    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        // 2026 Best Practice: Defensive permission check
        if (!hasReadPermission()) {
            Logger.w("ContactsProviderSource", "READ_CONTACTS permission not granted")
            return@withContext emptyList()
        }
        val whatsAppIds = getWhatsAppContactIds()
        // 2026 Best Practice: Removed unused getContactAccountTypes() call
        // Account types are now fetched atomically in getContactsStreaming() via RawContactsEntity

        val contactsMap = mutableMapOf<Long, Contact>()

        // 1. Fetch Main Contacts (Source of Truth)
        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            // 2026 Best Practice: Validate column indices before use
            if (idIdx < 0) return@use

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                // 2026 Best Practice: Consistent null handling for cursor strings
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null

                contactsMap[id] = Contact(
                    id = id,
                    name = name,
                    numbers = mutableListOf(),
                    emails = mutableListOf(),
                    normalizedNumber = null
                )
            }
        }

        // 2. Fetch Phones
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

            // 2026 Best Practice: Validate required column indices
            if (idIdx < 0) return@use

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                // 2026 Best Practice: Consistent null handling with index validation
                val number = if (numIdx >= 0) cursor.getString(numIdx) else null
                val normalized = if (normIdx >= 0) cursor.getString(normIdx) else null

                contactsMap[id]?.let { contact ->
                    // Store RAW number in numbers list (for format detection)
                    if (!number.isNullOrBlank()) {
                        (contact.numbers as MutableList).add(number)
                    }
                    // Set normalizedNumber from Provider if available
                    if (!normalized.isNullOrBlank() && contact.normalizedNumber == null) {
                        contactsMap[id] = contact.copy(normalizedNumber = normalized)
                    }
                }
            }
        }

        // 3. Fetch Emails
        contentResolver.query(
             ContactsContract.CommonDataKinds.Email.CONTENT_URI,
             arrayOf(
                 ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                 ContactsContract.CommonDataKinds.Email.ADDRESS
             ),
             null,
             null,
             null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

            // 2026 Best Practice: Validate required column indices
            if (idIdx < 0) return@use

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                // 2026 Best Practice: Consistent null handling with index validation
                val address = if (addrIdx >= 0) cursor.getString(addrIdx) else null
                if (!address.isNullOrBlank()) {
                    contactsMap[id]?.let { contact ->
                        (contact.emails as MutableList).add(address)
                    }
                }
            }
        }

        contactsMap.values.map { contact ->
             if (contact.normalizedNumber == null && contact.numbers.isNotEmpty()) {
                 contact.copy(normalizedNumber = contact.numbers.first())
             } else {
                 contact
             }
        }
    }

    suspend fun getWhatsAppContactIds(): Set<Long> = withContext(Dispatchers.IO) {
        val whatsAppIds = mutableSetOf<Long>()

        // 1. Broad Account check (captures GBWhatsApp, FMWhatsApp, Business, etc.)
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.CONTACT_ID),
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} LIKE ?",
            arrayOf("%whatsapp%"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (id > 0) whatsAppIds.add(id)
            }
        }

        // 2. Broad MIME type check
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID),
            "${ContactsContract.Data.MIMETYPE} LIKE ?",
            arrayOf("%whatsapp%"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (id > 0) whatsAppIds.add(id)
            }
        }

        whatsAppIds
    }

    suspend fun getTelegramContactIds(): Set<Long> = withContext(Dispatchers.IO) {
        val telegramIds = mutableSetOf<Long>()

        // Account check (Broad)
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.CONTACT_ID),
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} LIKE ?",
            arrayOf("%telegram%"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (id > 0) telegramIds.add(id)
            }
        }

        // MIME check (Broad)
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID),
            "${ContactsContract.Data.MIMETYPE} LIKE ?",
            arrayOf("%telegram%"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (id > 0) telegramIds.add(id)
            }
        }

        telegramIds
    }

    private suspend fun getContactAccountTypes(): Map<Long, Pair<String, String>> = withContext(Dispatchers.IO) {
        val accountTypes = mutableMapOf<Long, Pair<String, String>>()

        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME
            ),
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} IS NOT NULL",
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val type = cursor.getString(typeIdx)
                val name = cursor.getString(nameIdx)
                if (!type.isNullOrBlank()) {
                    accountTypes[id] = Pair(type, name ?: "")
                }
            }
        }
        accountTypes
    }

    suspend fun getVerifiedContactIds(): List<Long> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<Long>()
        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            null,
            null,
            "${ContactsContract.Contacts._ID} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(idIdx))
            }
        }
        ids
    }

    suspend fun getContactsSnapshot(
        batchIds: List<Long>,
        whatsAppIds: Set<Long>,
        telegramIds: Set<Long>
    ): List<Contact> = withContext(Dispatchers.IO) {
        if (batchIds.isEmpty()) return@withContext emptyList()

        // 2026 Best Practice: Defensive permission check
        if (!hasReadPermission()) {
            Logger.w("ContactsProviderSource", "READ_CONTACTS permission not granted for snapshot")
            return@withContext emptyList()
        }

        // 2026 Best Practice: Removed unused getContactAccountTypes() call
        // Account types are fetched atomically in getContactsStreaming() via RawContactsEntity
        // Only keep contacts that actually exist in the provider result set.
        val contactsMap = mutableMapOf<Long, Contact>()

        // 2026 Best Practice: Use parameterized queries to prevent SQL injection
        val placeholders = batchIds.joinToString(",") { "?" }
        val selectionArgs = batchIds.map { it.toString() }.toTypedArray()

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            "${ContactsContract.Contacts._ID} IN ($placeholders)",
            selectionArgs,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                contactsMap[id] = Contact(
                    id = id,
                    name = name,
                    numbers = mutableListOf(),
                    emails = mutableListOf(),
                    normalizedNumber = null
                )
            }
        }

        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($placeholders)",
            selectionArgs,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val number = cursor.getString(numIdx)  // RAW number
                val normalized = cursor.getString(normIdx)
                contactsMap[id]?.let { contact ->
                    // Store RAW number for format detection
                    (contact.numbers as MutableList).add(number ?: "")
                    // Set normalizedNumber if not already set
                    if (normalized != null && contact.normalizedNumber == null) {
                        contactsMap[id] = contact.copy(normalizedNumber = normalized)
                    }
                }
            }
        }

        contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} IN ($placeholders)",
            selectionArgs,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val address = cursor.getString(addrIdx)
                if (!address.isNullOrBlank()) {
                    contactsMap[id]?.let { (it.emails as MutableList).add(address) }
                }
            }
        }

        contactsMap.values.map { contact ->
             if (contact.normalizedNumber == null && contact.numbers.isNotEmpty()) {
                 contact.copy(normalizedNumber = contact.numbers.first())
             } else {
                 contact
             }
        }
    }

    // Optimized Single-Pass Cursor Streaming (2026 Best Practice)
    // Fixed: Added error handling and flowOn for proper IO dispatching
    // 2026 Best Practice: Uses RawContactsEntity for atomic contact+account reads (Bug 5.5 fix)
    fun getContactsStreaming(batchSize: Int = 1000): kotlinx.coroutines.flow.Flow<List<Contact>> = kotlinx.coroutines.flow.flow {
        // 2026 Best Practice: Defensive permission check
        if (!hasReadPermission()) {
            Logger.w("ContactsProviderSource", "READ_CONTACTS permission not granted for streaming")
            return@flow // Empty flow
        }

        try {
        // 2026 Best Practice: Use RawContactsEntity for atomic reads of contact data + account info
        // This eliminates the race condition where account info could become stale between queries
        val uri = ContactsContract.RawContactsEntity.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.RawContactsEntity.CONTACT_ID,
            ContactsContract.RawContactsEntity.DATA1, // Generic data column (name, phone, email based on mimetype)
            ContactsContract.RawContactsEntity.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            // 2026 Best Practice: Account info in same query - atomic read
            ContactsContract.RawContacts.ACCOUNT_TYPE,
            ContactsContract.RawContacts.ACCOUNT_NAME
        )
        // Sort by CONTACT_ID to group rows for the same contact together - CRITICAL for O(1) processing
        val sortOrder = "${ContactsContract.RawContactsEntity.CONTACT_ID} ASC"
        val selection = "${ContactsContract.RawContactsEntity.MIMETYPE} LIKE ? OR ${ContactsContract.RawContactsEntity.MIMETYPE} LIKE ? OR ${ContactsContract.RawContactsEntity.MIMETYPE} IN (?, ?, ?)"
        val selectionArgs = arrayOf(
            "%whatsapp%",
            "%telegram%",
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        )

        val batch = ArrayList<Contact>(batchSize)
        val whatsAppIds = getWhatsAppContactIds()
        val telegramIds = getTelegramContactIds()
        // 2026 Best Practice: No separate getContactAccountTypes() call needed - extracted atomically below

        var currentId = -1L
        var currentName: String? = null
        val currentNumbers = mutableListOf<String>()
        val currentEmails = mutableListOf<String>()
        var currentNormalizedNumber: String? = null
        // 2026 Best Practice: Account info extracted atomically from same cursor
        var currentAccountType: String? = null
        var currentAccountName: String? = null

        contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.RawContactsEntity.CONTACT_ID)
            val data1Idx = cursor.getColumnIndex(ContactsContract.RawContactsEntity.DATA1)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.RawContactsEntity.MIMETYPE)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            // 2026 Best Practice: Account indices for atomic read
            val accountTypeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val accountNameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

            // 2026 Best Practice: Cooperative cancellation counter for large cursor iterations
            var iterationCount = 0

            while (cursor.moveToNext()) {
                // 2026 Best Practice: Check for cancellation every 100 rows
                // This ensures the flow can be cancelled even during long cursor iterations
                if (++iterationCount % 100 == 0) {
                    currentCoroutineContext().ensureActive()
                }

                val id = cursor.getLong(idIdx)

                if (id != currentId) {
                    // Emit previous contact
                    if (currentId != -1L) {
                        batch.add(
                            Contact(
                                id = currentId,
                                name = currentName,
                                numbers = ArrayList(currentNumbers),
                                emails = ArrayList(currentEmails),
                                normalizedNumber = currentNormalizedNumber ?: currentNumbers.firstOrNull(),
                                isWhatsApp = currentId in whatsAppIds,
                                isTelegram = currentId in telegramIds,
                                isJunk = false,
                                junkType = null,
                                duplicateType = null,
                                // 2026 Best Practice: Account info from atomic read
                                accountType = currentAccountType,
                                accountName = currentAccountName,
                                isSensitive = false,
                                sensitiveDescription = null,
                                formatIssue = null
                            )
                        )
                        if (batch.size >= batchSize) {
                            emit(ArrayList(batch))
                            batch.clear()
                        }
                    }
                    // Reset for new contact
                    currentId = id
                    currentName = if (data1Idx >= 0) cursor.getString(data1Idx) else null
                    currentNumbers.clear()
                    currentEmails.clear()
                    currentNormalizedNumber = null
                    // 2026 Best Practice: Reset account info for new contact
                    currentAccountType = null
                    currentAccountName = null
                }

                // 2026 Best Practice: Extract account info from first row of each contact (atomic)
                if (currentAccountType == null && accountTypeIdx >= 0) {
                    val accType = cursor.getString(accountTypeIdx)
                    if (!accType.isNullOrBlank()) {
                        currentAccountType = accType
                        currentAccountName = if (accountNameIdx >= 0) cursor.getString(accountNameIdx) else null
                    }
                }

                val mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null
                when (mimeType) {
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        val name = if (data1Idx >= 0) cursor.getString(data1Idx) else null
                        if (!name.isNullOrBlank()) currentName = name
                    }
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val num = if (numIdx >= 0) cursor.getString(numIdx) else null  // RAW number
                        val norm = if (normIdx >= 0) cursor.getString(normIdx) else null  // Normalized (with +)
                        // Always add RAW number for format detection
                        if (!num.isNullOrBlank()) {
                            currentNumbers.add(num)
                        }
                        // Store normalized separately for other uses
                        if (!norm.isNullOrBlank() && currentNormalizedNumber == null) {
                            currentNormalizedNumber = norm
                        }
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        val email = if (emailIdx >= 0) cursor.getString(emailIdx) else null
                        if (!email.isNullOrBlank()) currentEmails.add(email)
                    }
                }
            }

            // Emit last one
            if (currentId != -1L) {
                batch.add(
                    Contact(
                        id = currentId,
                        name = currentName,
                        numbers = ArrayList(currentNumbers),
                        emails = ArrayList(currentEmails),
                        normalizedNumber = currentNormalizedNumber ?: currentNumbers.firstOrNull(),
                        isWhatsApp = currentId in whatsAppIds,
                        isTelegram = currentId in telegramIds,
                        isJunk = false,
                        junkType = null,
                        duplicateType = null,
                        // 2026 Best Practice: Account info from atomic read
                        accountType = currentAccountType,
                        accountName = currentAccountName,
                        isSensitive = false,
                        sensitiveDescription = null,
                        formatIssue = null
                    )
                )
            }
            if (batch.isNotEmpty()) {
                emit(batch)
            }
        }
        } catch (e: SecurityException) {
            Logger.e("ContactsProviderSource", "Permission denied: ${e.message}")
            // Don't emit - caller should handle empty flow
        } catch (e: Exception) {
            Logger.e("ContactsProviderSource", "Error streaming contacts: ${e.message}")
            throw e // Re-throw for upstream handling
        }
    }.flowOn(Dispatchers.IO) // 2026 Best Practice: Ensure IO dispatcher for ContentProvider

    suspend fun getRawContactCount(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                null,
                null,
                null,
                null
            )?.use { count = it.count }
        } catch (e: Exception) {
            Logger.e("ContactsProviderSource", "Error counting raw contacts", e)
        }
        count
    }

    suspend fun deleteContacts(contactIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        if (contactIds.isEmpty()) return@withContext true

        // 2026 Best Practice: This write path also queries the provider for raw IDs.
        if (!hasReadWritePermissions("delete")) {
            return@withContext false
        }

        Logger.i("ContactsProviderSource", "deleteContacts requested count=${contactIds.size}")

        // Resolve aggregate Contacts IDs to RawContacts rows first. On Samsung/Android 15,
        // deleting Contacts.CONTENT_URI with a selection can report success from the provider
        // call but leave the aggregate contact intact. RawContacts deletion is the reliable
        // provider path, then we verify that each aggregate contact disappeared.
        contactIds.chunked(MAX_PROVIDER_BATCH).forEachIndexed { batchIndex, batch ->
            try {
                currentCoroutineContext().ensureActive()
                val rawContactIdsByContactId = lookupRawContactIdsByContactId(batch)
                val rawContactIds = rawContactIdsByContactId.values.flatten()
                val missingContactIds = batch.filterNot { rawContactIdsByContactId.containsKey(it) }

                if (missingContactIds.isNotEmpty()) {
                    Logger.w(
                        "ContactsProviderSource",
                        "deleteContacts batch=${batchIndex + 1} missing raw contacts for ${missingContactIds.size}/${batch.size} contacts ids=${idsForLog(missingContactIds)}"
                    )
                }

                rawContactIds.chunked(MAX_PROVIDER_BATCH).forEachIndexed { rawBatchIndex, rawBatch ->
                    val placeholders = rawBatch.joinToString(",") { "?" }
                    val selection = "${ContactsContract.RawContacts._ID} IN ($placeholders)"
                    val selectionArgs = rawBatch.map { it.toString() }.toTypedArray()
                    val deletedRows = contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        selection,
                        selectionArgs
                    )
                    Logger.i(
                        "ContactsProviderSource",
                        "deleteContacts batch=${batchIndex + 1} rawBatch=${rawBatchIndex + 1} rawRows=${rawBatch.size}/${rawContactIds.size} affectedRows=$deletedRows contacts=${batch.size}"
                    )
                }

                val remainingContactIds = lookupExistingContactIds(batch)
                if (remainingContactIds.isNotEmpty()) {
                    Logger.e(
                        "ContactsProviderSource",
                        "deleteContacts batch=${batchIndex + 1} left ${remainingContactIds.size}/${batch.size} contacts in provider ids=${idsForLog(remainingContactIds.toList())}"
                    )
                    return@withContext false
                }
            } catch (e: CancellationException) {
                Logger.w(
                    "ContactsProviderSource",
                    "deleteContacts cancelled batch=${batchIndex + 1} requested=${contactIds.size} reason=${e.message ?: "none"}"
                )
                throw e
            } catch (e: SecurityException) {
                Logger.e("ContactsProviderSource", "Permission error deleting contacts batch=${batchIndex + 1}", e)
                return@withContext false
            } catch (e: Exception) {
                Logger.e("ContactsProviderSource", "Unexpected error deleting contacts batch=${batchIndex + 1}", e)
                return@withContext false
            }
        }

        Logger.i("ContactsProviderSource", "deleteContacts verified count=${contactIds.size}")
        true
    }

    private fun lookupRawContactIdsByContactId(contactIds: List<Long>): Map<Long, List<Long>> {
        if (contactIds.isEmpty()) return emptyMap()

        val placeholders = contactIds.joinToString(",") { "?" }
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} IN ($placeholders)"
        val selectionArgs = contactIds.map { it.toString() }.toTypedArray()
        val rawContactIdsByContactId = linkedMapOf<Long, MutableList<Long>>()

        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.CONTACT_ID
            ),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val rawIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            if (rawIdIdx < 0 || contactIdIdx < 0) return@use

            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(contactIdIdx)
                val rawContactId = cursor.getLong(rawIdIdx)
                rawContactIdsByContactId.getOrPut(contactId) { mutableListOf() }.add(rawContactId)
            }
        }

        return rawContactIdsByContactId
    }

    private fun lookupExistingContactIds(contactIds: List<Long>): Set<Long> {
        if (contactIds.isEmpty()) return emptySet()

        val placeholders = contactIds.joinToString(",") { "?" }
        val selection = "${ContactsContract.Contacts._ID} IN ($placeholders)"
        val selectionArgs = contactIds.map { it.toString() }.toTypedArray()
        val existingContactIds = linkedSetOf<Long>()

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            if (contactIdIdx < 0) return@use

            while (cursor.moveToNext()) {
                existingContactIds.add(cursor.getLong(contactIdIdx))
            }
        }

        return existingContactIds
    }

    private fun idsForLog(ids: List<Long>, maxIds: Int = 10): String {
        val suffix = if (ids.size > maxIds) ",..." else ""
        return ids.take(maxIds).joinToString(prefix = "[", postfix = suffix + "]")
    }

    suspend fun mergeContacts(contactIds: List<Long>, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (contactIds.size < 2) return@withContext false

        // 2026 Best Practice: Defensive permission check for write operation
        if (!hasWritePermission()) {
            Logger.w("ContactsProviderSource", "WRITE_CONTACTS permission not granted for merge")
            return@withContext false
        }

        val contactsToMerge = getContactsSnapshot(
            batchIds = contactIds,
            whatsAppIds = emptySet(),
            telegramIds = emptySet()
        )

        if (contactIds.size > MAX_PROVIDER_BATCH) {
            Logger.e(
                "ContactsProviderSource",
                "Refusing Android merge for ${contactIds.size} contacts because deleting originals would exceed the single-batch rollback limit"
            )
            return@withContext false
        }

        val mergedContact = AndroidMergeUtils.buildMergedContact(contactsToMerge, customName)
        if (mergedContact == null) {
            Logger.w("ContactsProviderSource", "Unable to build merged Android contact payload")
            return@withContext false
        }

        val restored = restoreContactsInternal(listOf(mergedContact))?.firstOrNull()
        if (restored == null) {
            Logger.e("ContactsProviderSource", "Failed to create merged contact on Android")
            return@withContext false
        }

        val deleted = deleteContacts(contactIds)
        if (!deleted) {
            Logger.e("ContactsProviderSource", "Merged contact created but failed to delete originals")
            val rolledBack = rollbackMergedContact(restored)
            if (!rolledBack) {
                Logger.e("ContactsProviderSource", "Rollback failed after merge delete error for ${mergedContact.name ?: "unnamed contact"}")
            }
            return@withContext false
        }

        true
    }

    suspend fun updateMultipleContactNumbers(updates: Map<Long, String>): Boolean = withContext(Dispatchers.IO) {
        if (updates.isEmpty()) return@withContext true

        // 2026 Best Practice: This write path also queries Data rows before updating.
        if (!hasReadWritePermissions("update")) {
            return@withContext false
        }

        val contactIds = updates.keys.toList()
        val allOps = ArrayList<android.content.ContentProviderOperation>()

        // 1. Process in chunks of 200 (Large enough for throughput, small enough for IPC limits)
        contactIds.chunked(200).forEach { batchIds ->
            // 2026 Best Practice: Use parameterized queries to prevent SQL injection
            val placeholders = batchIds.joinToString(",") { "?" }
            val selection = "${ContactsContract.Data.CONTACT_ID} IN ($placeholders) AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = batchIds.map { it.toString() }.toTypedArray() + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE

            val ops = ArrayList<android.content.ContentProviderOperation>()

            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID, ContactsContract.Data.CONTACT_ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val dataIdIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val contactIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)

                while (cursor.moveToNext()) {
                    val dataId = cursor.getLong(dataIdIdx)
                    val contactId = cursor.getLong(contactIdIdx)
                    val newNumber = updates[contactId]

                    if (newNumber != null) {
                        ops.add(
                            android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(dataId.toString()))
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumber)
                                .build()
                        )
                    }
                }
            }
            allOps.addAll(ops)
        }

        if (allOps.isEmpty()) {
            Logger.e("ContactsProviderSource", "No provider update operations were created for ${updates.size} requested contacts")
            return@withContext false
        }

        // 2. Apply in chunky batches of 400
        // 2026 Best Practice: Catch specific exceptions from applyBatch
        var success = true
        allOps.chunked(400).forEach { batch ->
            try {
                val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(batch))
                val updatedRows = results.sumOf { result -> result.count ?: 0 }
                if (updatedRows != batch.size) {
                    Logger.e(
                        "ContactsProviderSource",
                        "Bulk update affected $updatedRows of ${batch.size} requested data rows"
                    )
                    success = false
                }
            } catch (e: RemoteException) {
                Logger.e("ContactsProviderSource", "Remote error in bulk update", e)
                success = false
            } catch (e: OperationApplicationException) {
                Logger.e("ContactsProviderSource", "Operation error in bulk update", e)
                success = false
            }
        }
        success
    }

    suspend fun normalizeContactNumbers(
        contactIds: List<Long>,
        normalizer: (rawNumber: String, providerNormalizedNumber: String?) -> String?
    ): Set<Long> = withContext(Dispatchers.IO) {
        val distinctContactIds = contactIds.distinct()
        if (distinctContactIds.isEmpty()) return@withContext emptySet()

        if (!hasReadWritePermissions("normalization")) {
            return@withContext emptySet()
        }

        val updatedContactIds = linkedSetOf<Long>()
        var processedCount = 0
        var rowsRead = 0
        var plannedUpdates = 0
        var affectedRows = 0

        Logger.i("ContactsProviderSource", "normalizeContactNumbers started requested=${distinctContactIds.size}")

        try {
            distinctContactIds.chunked(MAX_PROVIDER_BATCH).forEachIndexed { batchIndex, batchIds ->
                currentCoroutineContext().ensureActive()
                processedCount += batchIds.size

                val placeholders = batchIds.joinToString(",") { "?" }
                val selection = "${ContactsContract.Data.CONTACT_ID} IN ($placeholders) AND ${ContactsContract.Data.MIMETYPE} = ?"
                val selectionArgs = batchIds.map { it.toString() }.toTypedArray() + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                val rows = ArrayList<PhoneNumberUpdatePlanner.PhoneRow>(batchIds.size)

                contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Data._ID,
                        ContactsContract.Data.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
                    ),
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    val dataIdIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                    val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val normalizedIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                    if (dataIdIdx < 0 || contactIdIdx < 0 || numberIdx < 0) {
                        Logger.e(
                            "ContactsProviderSource",
                            "normalizeContactNumbers missing required columns dataId=$dataIdIdx contactId=$contactIdIdx number=$numberIdx"
                        )
                        return@use
                    }

                    while (cursor.moveToNext()) {
                        rows.add(
                            PhoneNumberUpdatePlanner.PhoneRow(
                                dataId = cursor.getLong(dataIdIdx),
                                contactId = cursor.getLong(contactIdIdx),
                                rawNumber = cursor.getString(numberIdx).orEmpty(),
                                providerNormalizedNumber = if (normalizedIdx >= 0) cursor.getString(normalizedIdx) else null
                            )
                        )
                    }
                }

                rowsRead += rows.size
                val updates = PhoneNumberUpdatePlanner.planUpdates(rows, normalizer)
                plannedUpdates += updates.size

                Logger.d(
                    "ContactsProviderSource",
                    "normalizeContactNumbers batch=${batchIndex + 1} requested=${batchIds.size} rows=${rows.size} planned=${updates.size}"
                )

                updates.chunked(400).forEach { updateChunk ->
                    currentCoroutineContext().ensureActive()

                    val operations = ArrayList<android.content.ContentProviderOperation>(updateChunk.size)
                    updateChunk.forEach { update ->
                        operations.add(
                            android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(update.dataId.toString()))
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, update.targetNumber)
                                .build()
                        )
                    }

                    try {
                        val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                        var chunkAffectedRows = 0
                        results.forEachIndexed { index, result ->
                            val count = result.count ?: 0
                            chunkAffectedRows += count
                            if (count > 0) {
                                updatedContactIds.add(updateChunk[index].contactId)
                            }
                        }
                        affectedRows += chunkAffectedRows

                        if (chunkAffectedRows != updateChunk.size) {
                            Logger.w(
                                "ContactsProviderSource",
                                "Normalization partially updated $chunkAffectedRows of ${updateChunk.size} phone rows"
                            )
                        }
                    } catch (e: RemoteException) {
                        Logger.e("ContactsProviderSource", "Remote error normalizing ${updateChunk.size} phone rows", e)
                    } catch (e: OperationApplicationException) {
                        Logger.e("ContactsProviderSource", "Operation error normalizing ${updateChunk.size} phone rows", e)
                    }
                }
            }
        } catch (e: CancellationException) {
            Logger.w(
                "ContactsProviderSource",
                "normalizeContactNumbers cancelled processed=$processedCount/${distinctContactIds.size} rows=$rowsRead planned=$plannedUpdates updated=${updatedContactIds.size} reason=${e.message ?: "none"}"
            )
            throw e
        }

        Logger.i(
            "ContactsProviderSource",
            "normalizeContactNumbers finished requested=${distinctContactIds.size} processed=$processedCount rows=$rowsRead planned=$plannedUpdates affectedRows=$affectedRows updated=${updatedContactIds.size}"
        )

        updatedContactIds
    }

    suspend fun updateContactNumber(contactId: Long, newNumber: String): Boolean = updateMultipleContactNumbers(mapOf(contactId to newNumber))

    suspend fun restoreContacts(contacts: List<Contact>): Boolean = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) return@withContext true
        restoreContactsInternal(contacts) != null
    }

    private fun restoreContactsInternal(contacts: List<Contact>): List<RestoredContactResult>? {
        if (contacts.isEmpty()) return emptyList()

        if (!hasWritePermission()) {
            Logger.w("ContactsProviderSource", "WRITE_CONTACTS permission not granted for restore")
            return null
        }

        val results = mutableListOf<RestoredContactResult>()
        try {
            contacts.forEach { contact ->
                val operations = buildRestoreOperations(contact)
                val providerResults = contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                val rawContactId = providerResults.firstOrNull()?.uri?.lastPathSegment?.toLongOrNull()
                if (rawContactId == null) {
                    Logger.e("ContactsProviderSource", "Restore completed without a raw contact ID for ${contact.name ?: "unnamed contact"}")
                    rollbackRestoredContacts(results)
                    return null
                }
                val contactId = try {
                    lookupContactIdForRawContact(rawContactId)
                } catch (e: SecurityException) {
                    Logger.w("ContactsProviderSource", "READ_CONTACTS permission not granted for restored contact lookup; falling back to raw contact ID")
                    null
                }
                results.add(
                    RestoredContactResult(
                        contact = contact,
                        contactId = contactId,
                        rawContactId = rawContactId
                    )
                )
            }
        } catch (e: RemoteException) {
            Logger.e("ContactsProviderSource", "Remote error restoring contacts", e)
            rollbackRestoredContacts(results)
            return null
        } catch (e: OperationApplicationException) {
            Logger.e("ContactsProviderSource", "Operation error restoring contacts", e)
            rollbackRestoredContacts(results)
            return null
        }
        return results
    }

    private fun buildRestoreOperations(contact: Contact): ArrayList<android.content.ContentProviderOperation> {
        val operations = ArrayList<android.content.ContentProviderOperation>()

        val rawContactInsertIndex = operations.size
        operations.add(
            android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, contact.accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, contact.accountName)
                .build()
        )

        if (!contact.name.isNullOrBlank()) {
            operations.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                    .build()
            )
        }

        contact.numbers.forEach { number ->
            operations.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )
        }

        contact.emails.forEach { email ->
            operations.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                    .build()
            )
        }

        return operations
    }

    private fun lookupContactIdForRawContact(rawContactId: Long): Long? {
        return contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.CONTACT_ID),
            "${ContactsContract.RawContacts._ID} = ?",
            arrayOf(rawContactId.toString()),
            null
        )?.use { cursor ->
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            if (contactIdIdx < 0 || !cursor.moveToFirst()) {
                null
            } else {
                cursor.getLong(contactIdIdx).takeIf { it > 0 }
            }
        }
    }

    private suspend fun rollbackMergedContact(restoredContact: RestoredContactResult): Boolean {
        restoredContact.contactId?.let { createdContactId ->
            val deleted = deleteContacts(listOf(createdContactId))
            if (deleted) {
                Logger.i("ContactsProviderSource", "Rolled back merged contact via contact delete after original delete failure")
                return true
            }
            Logger.e("ContactsProviderSource", "Rollback via contact delete failed for merged contact ${restoredContact.contact.name ?: "unnamed contact"}")
        }

        restoredContact.rawContactId?.let { rawContactId ->
            val deleted = deleteRawContacts(listOf(rawContactId))
            if (deleted) {
                Logger.i("ContactsProviderSource", "Rolled back merged contact via raw contact delete after original delete failure")
                return true
            }
            Logger.e("ContactsProviderSource", "Rollback via raw contact delete failed for merged contact ${restoredContact.contact.name ?: "unnamed contact"}")
        }

        Logger.e("ContactsProviderSource", "Unable to rollback merged contact ${restoredContact.contact.name ?: "unnamed contact"} because no created identifier was available")
        return false
    }

    private fun rollbackRestoredContacts(restoredContacts: List<RestoredContactResult>) {
        restoredContacts.asReversed().forEach { restoredContact ->
            val rawContactId = restoredContact.rawContactId
            if (rawContactId == null) {
                Logger.e(
                    "ContactsProviderSource",
                    "Unable to rollback restored contact ${restoredContact.contact.name ?: "unnamed contact"} because no raw contact ID was available"
                )
                return@forEach
            }

            val rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                .appendPath(rawContactId.toString())
                .build()
            try {
                val deletedRows = contentResolver.delete(rawContactUri, null, null)
                if (deletedRows > 0) {
                    Logger.i(
                        "ContactsProviderSource",
                        "Rolled back restored contact ${restoredContact.contact.name ?: "unnamed contact"} via raw contact delete"
                    )
                } else {
                    Logger.e(
                        "ContactsProviderSource",
                        "Rollback via raw contact delete failed for restored contact ${restoredContact.contact.name ?: "unnamed contact"}"
                    )
                }
            } catch (e: Exception) {
                Logger.e(
                    "ContactsProviderSource",
                    "Rollback via raw contact delete threw for restored contact ${restoredContact.contact.name ?: "unnamed contact"}",
                    e
                )
            }
        }
    }

    private suspend fun deleteRawContacts(rawContactIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        if (rawContactIds.isEmpty()) return@withContext true

        if (!hasWritePermission()) {
            Logger.w("ContactsProviderSource", "WRITE_CONTACTS permission not granted for raw contact delete")
            return@withContext false
        }

        rawContactIds.chunked(MAX_PROVIDER_BATCH).forEach { batch ->
            val batchOperations = ArrayList<android.content.ContentProviderOperation>()
            batch.forEach { id ->
                batchOperations.add(
                    android.content.ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection("${ContactsContract.RawContacts._ID} = ?", arrayOf(id.toString()))
                        .build()
                )
            }
            try {
                val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, batchOperations)
                val deletedRows = results.sumOf { result -> result.count ?: 0 }
                if (deletedRows != batch.size) {
                    Logger.e(
                        "ContactsProviderSource",
                        "Rollback raw-contact delete affected $deletedRows of ${batch.size} requested rows"
                    )
                    return@withContext false
                }
            } catch (e: RemoteException) {
                Logger.e("ContactsProviderSource", "Remote error deleting raw contact batch", e)
                return@withContext false
            } catch (e: OperationApplicationException) {
                Logger.e("ContactsProviderSource", "Operation error deleting raw contact batch", e)
                return@withContext false
            }
        }

        true
    }
}
