package com.ogabassey.contactscleaner.data.source

import android.content.ContentResolver
import android.provider.ContactsContract
import com.ogabassey.contactscleaner.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContactsProviderSource @Inject constructor(
    private val contentResolver: ContentResolver
) {

    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val whatsAppIds = getWhatsAppContactIds()
        val accountTypesMap = getContactAccountTypes()

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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                
                contactsMap[id] = Contact(
                    id = id,
                    name = name,
                    numbers = mutableListOf(),
                    emails = mutableListOf(),
                    normalizedNumber = null,
                    isWhatsApp = id in whatsAppIds,
                    accountType = accountTypesMap[id]?.first,
                    accountName = accountTypesMap[id]?.second
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

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val number = cursor.getString(numIdx)  // RAW number as stored by user
                val normalized = cursor.getString(normIdx)  // Provider's normalized version

                contactsMap[id]?.let { contact ->
                    // Store RAW number in numbers list (for format detection)
                    (contact.numbers as MutableList).add(number ?: "")
                    // Set normalizedNumber from Provider if available
                    if (normalized != null && contact.normalizedNumber == null) {
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
            
            while(cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val address = cursor.getString(addrIdx)
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

        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.CONTACT_ID),
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ? OR ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
            arrayOf("com.whatsapp", "com.whatsapp.w4b"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)

            while (cursor.moveToNext()) {
                whatsAppIds.add(cursor.getLong(idIndex))
            }
        }

        whatsAppIds
    }

    suspend fun getTelegramContactIds(): Set<Long> = withContext(Dispatchers.IO) {
        val telegramIds = mutableSetOf<Long>()

        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.CONTACT_ID),
            "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
            arrayOf("org.telegram.messenger"),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)

            while (cursor.moveToNext()) {
                telegramIds.add(cursor.getLong(idIndex))
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
        
        val accountTypesMap = getContactAccountTypes() 
        val contactsMap = batchIds.associateWith { id ->
            Contact(
                id = id,
                name = null,
                numbers = mutableListOf(),
                emails = mutableListOf(),
                normalizedNumber = null,
                isWhatsApp = id in whatsAppIds,
                isTelegram = id in telegramIds,
                accountType = accountTypesMap[id]?.first,
                accountName = accountTypesMap[id]?.second
            )
        }.toMutableMap()

        val idListStr = batchIds.joinToString(",")

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            "${ContactsContract.Contacts._ID} IN ($idListStr)",
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                contactsMap[id]?.let { contactsMap[id] = it.copy(name = name) }
            }
        }

        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN ($idListStr)",
            null,
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
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} IN ($idListStr)",
            null,
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
    fun getContactsStreaming(batchSize: Int = 1000): kotlinx.coroutines.flow.Flow<List<Contact>> = kotlinx.coroutines.flow.flow {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        )
        // Sort by ID to group rows for the same contact together - CRITICAL for O(1) processing
        val sortOrder = "${ContactsContract.Data.CONTACT_ID} ASC"
        val selection = "${ContactsContract.Data.MIMETYPE} IN (?, ?, ?)"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        )

        val batch = ArrayList<Contact>(batchSize)
        val whatsAppIds = getWhatsAppContactIds()
        val telegramIds = getTelegramContactIds()
        val accountMap = getContactAccountTypes()

        var currentId = -1L
        var currentName: String? = null
        val currentNumbers = mutableListOf<String>()
        val currentEmails = mutableListOf<String>()
        var currentNormalizedNumber: String? = null

        contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY)
            val mimeIdx = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)

                if (id != currentId) {
                    // Emit previous contact
                    if (currentId != -1L) {
                        batch.add(
                            Contact(
                                id = currentId,
                                name = currentName,
                                numbers = ArrayList(currentNumbers), // Copy
                                emails = ArrayList(currentEmails),   // Copy
                                normalizedNumber = currentNormalizedNumber ?: currentNumbers.firstOrNull(),
                                isWhatsApp = currentId in whatsAppIds,
                                isTelegram = currentId in telegramIds,
                                accountType = accountMap[currentId]?.first,
                                accountName = accountMap[currentId]?.second
                            )
                        )
                        if (batch.size >= batchSize) {
                            emit(ArrayList(batch))
                            batch.clear()
                        }
                    }
                    // Reset
                    currentId = id
                    currentName = cursor.getString(nameIdx)
                    currentNumbers.clear()
                    currentEmails.clear()
                    currentNormalizedNumber = null
                }

                val mimeType = cursor.getString(mimeIdx)
                when (mimeType) {
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) currentName = name
                    }
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val num = cursor.getString(numIdx)  // RAW number
                        val norm = cursor.getString(normIdx)  // Normalized (with +)
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
                        val email = cursor.getString(emailIdx)
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
                        accountType = accountMap[currentId]?.first,
                        accountName = accountMap[currentId]?.second
                    )
                )
            }
            if (batch.isNotEmpty()) {
                emit(batch)
            }
        }
    }

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
            android.util.Log.e("ContactsProviderSource", "Error counting raw contacts", e)
        }
        count
    }

    suspend fun deleteContacts(contactIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        if (contactIds.isEmpty()) return@withContext true
        contactIds.chunked(400).forEach { batch ->
            val batchOperations = ArrayList<android.content.ContentProviderOperation>()
            batch.forEach { id ->
                batchOperations.add(
                    android.content.ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(id.toString()))
                        .build()
                )
            }
            try {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, batchOperations)
            } catch (e: Exception) {
                android.util.Log.e("ContactsProviderSource", "Error deleting batch", e)
                return@withContext false
            }
        }
        true
    }


    suspend fun mergeContacts(contactIds: List<Long>, customName: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (contactIds.size < 2) return@withContext false

        // 1. Get a RawContactID for each ContactID
        val rawIds = mutableListOf<Long>()
        // Optimization: Query all at once or in batches if possible, but one-by-one is safer for mapping strictly
        // To do it in one query: SELECT _id, contact_id FROM raw_contacts WHERE contact_id IN (...)
        
        val idListStr = contactIds.joinToString(",")
        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.CONTACT_ID),
            "${ContactsContract.RawContacts.CONTACT_ID} IN ($idListStr)",
            null,
            null
        )?.use { cursor ->
            val rawIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts._ID)
            val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
            val foundContactIds = mutableSetOf<Long>()
            
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(contactIdIdx)
                if (contactId !in foundContactIds) {
                    // Just take the first raw ID found for this contact ID
                    rawIds.add(cursor.getLong(rawIdIdx))
                    foundContactIds.add(contactId)
                }
            }
        }

        if (rawIds.size < 2) return@withContext false

        // 2. Aggregate them all to the first one (Anchor)
        val operations = ArrayList<android.content.ContentProviderOperation>()
        val anchorId = rawIds[0]

        for (i in 1 until rawIds.size) {
            operations.add(
                android.content.ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                    .withValue(ContactsContract.AggregationExceptions.TYPE, ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, anchorId)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, rawIds[i])
                    .build()
            )
        }

        if (customName != null) {
            operations.add(
                android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(anchorId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, customName)
                    .build()
            )
        }

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            true
        } catch (e: Exception) {
            android.util.Log.e("ContactsProviderSource", "Merge failed", e)
            false
        }
    }

    suspend fun updateContactNumber(contactId: Long, newNumber: String): Boolean = withContext(Dispatchers.IO) {
        val ops = ArrayList<android.content.ContentProviderOperation>()
        val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        
        var updated = false
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
             while (cursor.moveToNext()) {
                 val dataId = cursor.getLong(0)
                 ops.add(
                     android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                         .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(dataId.toString()))
                         .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumber)
                         .build()
                 )
             }
        }
        
        if (ops.isNotEmpty()) {
            try {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                updated = true
            } catch (e: Exception) {
                updated = false
            }
        }
        updated
    }
    suspend fun restoreContacts(contacts: List<Contact>): Boolean = withContext(Dispatchers.IO) {
        if (contacts.isEmpty()) return@withContext true
        
        val operations = ArrayList<android.content.ContentProviderOperation>()
        
        contacts.forEach { contact ->
            // 1. Create RawContact
            val rawContactInsertIndex = operations.size
            operations.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Add Name
            if (!contact.name.isNullOrBlank()) {
                operations.add(
                    android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name)
                        .build()
                )
            }

            // 3. Add Phones
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

            // 4. Add Emails
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
        }

        try {
            // Apply in batches of 400 to avoid TransactionTooLargeException
            operations.chunked(400).forEach { batch ->
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ArrayList(batch))
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ContactsProviderSource", "Error restoring contacts", e)
            false
        }
    }
}
