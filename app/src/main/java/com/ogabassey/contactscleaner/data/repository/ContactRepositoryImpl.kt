package com.ogabassey.contactscleaner.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val contactsProviderSource: ContactsProviderSource,
    private val junkDetector: JunkDetector,
    private val duplicateDetector: DuplicateDetector,
    private val formatDetector: com.ogabassey.contactscleaner.data.detector.FormatDetector,
    private val scanResultProvider: com.ogabassey.contactscleaner.data.util.ScanResultProvider
) : ContactRepository {
    override fun getContactsPaged(type: ContactType): Flow<PagingData<Contact>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 30,
                enablePlaceholders = true,
                maxSize = 200
            ),
            pagingSourceFactory = {
                when (type) {
                    ContactType.ALL -> contactDao.getAllContactsPaged()
                    ContactType.WHATSAPP -> contactDao.getWhatsAppContactsPaged()
                    ContactType.TELEGRAM -> contactDao.getTelegramContactsPaged()
                    ContactType.NON_WHATSAPP -> contactDao.getNonWhatsAppContactsPaged()
                    ContactType.JUNK -> contactDao.getJunkContactsPaged()
                    ContactType.DUPLICATE -> contactDao.getAllContactsPaged() // Fallback
                    // Granular
                    ContactType.JUNK_NO_NAME -> contactDao.getNoNameContactsPaged()
                    ContactType.JUNK_NO_NUMBER -> contactDao.getNoNumberContactsPaged()
                    ContactType.JUNK_SUSPICIOUS -> contactDao.getJunkContactsPaged()
                    ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailContactsPaged()
                    ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberContactsPaged()
                    ContactType.DUP_NAME -> contactDao.getDuplicateNameContactsPaged()
                    // V3 Mappings
                    ContactType.ACCOUNT -> contactDao.getAccountContactsPaged()
                    ContactType.DUP_SIMILAR_NAME -> contactDao.getSimilarNameContactsPaged()
                    ContactType.JUNK_INVALID_CHAR -> contactDao.getInvalidCharContactsPaged()
                    ContactType.JUNK_LONG_NUMBER -> contactDao.getLongNumberContactsPaged()
                    ContactType.JUNK_SHORT_NUMBER -> contactDao.getShortNumberContactsPaged()
                    ContactType.JUNK_REPETITIVE -> contactDao.getRepetitiveNumberContactsPaged()
                    ContactType.JUNK_SYMBOL -> contactDao.getSymbolNameContactsPaged()
                    ContactType.FORMAT_ISSUE -> contactDao.getFormatIssueContactsPaged()
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun scanContacts(): Flow<ScanStatus> = flow {
        android.util.Log.d("ContactRepository", "Starting Streamed SQL Scan (Optimum Performance)...")
        
        // 1. Reset Local DB
        contactDao.deleteAll()
        emit(ScanStatus.Progress(0.01f, "Initializing database..."))
        
        // 2. Fetch Total Count for Progress
        var totalToProcess = 0
        try {
             val allIds = contactsProviderSource.getVerifiedContactIds()
             emit(ScanStatus.Progress(0.05f, "Fetching contact list..."))
             totalToProcess = allIds.size
        } catch (e: Exception) {
            totalToProcess = 1000
        }
        
        if (totalToProcess == 0) {
            emit(ScanStatus.Success(ScanResult()))
            return@flow
        }
        
        // 3. Stream Process
        var processedCount = 0
        
        contactsProviderSource.getContactsStreaming(batchSize = 2500)
            .collect { batchContacts ->
                val entities = batchContacts.map { contact ->
                    // Run Junk Detection
                    val junkType = junkDetector.getJunkType(contact.name, contact.normalizedNumber ?: contact.numbers.firstOrNull())
                    
                    // Format Issue Detection (Enhanced)
                    val rawNum = contact.numbers.firstOrNull() ?: ""
                    var isFormatIssue = false
                    var detectedNormalized: String? = contact.normalizedNumber
                    
                    // Format detection: Check ALL non-junk contacts (including WhatsApp)
                    if (junkType == null && rawNum.isNotBlank()) {
                        // Log all 234 numbers being processed
                        if (rawNum.replace(Regex("[^0-9]"), "").startsWith("234")) {
                            android.util.Log.w("ContactRepo", "PROCESSING 234 NUMBER: '$rawNum' name=${contact.name}")
                        }
                        
                        // 1. Check if Provider already flagged it (Old Logic)
                        if (!rawNum.startsWith("+") && 
                             !rawNum.startsWith("*") &&
                             !rawNum.startsWith("#") &&
                             contact.normalizedNumber != null && 
                             contact.normalizedNumber.startsWith("+") &&
                             contact.normalizedNumber != rawNum) {
                            isFormatIssue = true
                            android.util.Log.w("ContactRepo", "PROVIDER FLAGGED: $rawNum -> ${contact.normalizedNumber}")
                        }
                        
                        // 2. If not flagged yet, run Advanced "Missing Plus" Check
                        if (!isFormatIssue && !rawNum.startsWith("+")) {
                             val issue = formatDetector.analyze(rawNum)
                             if (issue != null) {
                                 isFormatIssue = true
                                 detectedNormalized = issue.normalizedNumber
                                 android.util.Log.w("ContactRepo", "DETECTOR FLAGGED: $rawNum -> ${issue.normalizedNumber}")
                             } else if (rawNum.replace(Regex("[^0-9]"), "").startsWith("234")) {
                                 android.util.Log.w("ContactRepo", "DETECTOR RETURNED NULL for 234 number: '$rawNum' len=${rawNum.replace(Regex("[^0-9]"), "").length}")
                             }
                        }
                    } else if (rawNum.replace(Regex("[^0-9]"), "").startsWith("234")) {
                        android.util.Log.w("ContactRepo", "SKIPPED 234: $rawNum junk=$junkType")
                    }

                    LocalContact(
                        id = contact.id,
                        displayName = contact.name,
                        normalizedNumber = detectedNormalized,
                        rawNumbers = contact.numbers.joinToString(","),
                        rawEmails = contact.emails.joinToString(","),
                        isWhatsApp = contact.isWhatsApp,
                        isTelegram = contact.isTelegram,
                        accountType = contact.accountType,
                        accountName = contact.accountName,
                        isJunk = junkType != null,
                        junkType = junkType?.name,
                        duplicateType = null,
                        isFormatIssue = isFormatIssue,
                        detectedRegion = if (isFormatIssue && detectedNormalized != null) formatDetector.getRegionCode(detectedNormalized) else null,
                        lastSynced = System.currentTimeMillis()
                    )
                }
                
                contactDao.insertContacts(entities)
                processedCount += batchContacts.size
                
                // Progress: 5% (initial) + up to 75% for syncing = max 80%
                val syncProgress = 0.05f + (processedCount.toFloat() / totalToProcess.toFloat()) * 0.75f
                emit(ScanStatus.Progress(syncProgress.coerceAtMost(0.8f), "Syncing contacts ($processedCount)..."))
            }

        // 4. Post-Process: Run SQL Analysis
        emit(ScanStatus.Progress(0.85f, "Analyzing duplicates..."))
        android.util.Log.d("ContactRepository", "Running SQL Duplicate Analysis...")
        contactDao.markDuplicateNumbers()
        contactDao.markDuplicateEmails()
        contactDao.markDuplicateNames()
        emit(ScanStatus.Progress(0.95f, "Finalizing report..."))

        // 5. Build Result
        val rawCount = contactsProviderSource.getRawContactCount() // Fetch Source Truth
        
        val result = ScanResult(
            total = contactDao.countTotal(),
            rawCount = rawCount, // SHOW USER THE 58k
            whatsAppCount = contactDao.countWhatsApp(),
            telegramCount = contactDao.countTelegram(),
            nonWhatsAppCount = contactDao.countTotal() - contactDao.countWhatsApp(),
            junkCount = contactDao.countJunk(),
            duplicateCount = contactDao.countDuplicates(),
            // Granular
            noNameCount = contactDao.countNoName(),
            noNumberCount = contactDao.countNoNumber(),
            invalidCharCount = contactDao.countInvalidChar(),
            longNumberCount = contactDao.countLongNumber(),
            shortNumberCount = contactDao.countShortNumber(),
            repetitiveNumberCount = contactDao.countRepetitiveNumber(),
            symbolNameCount = contactDao.countSymbolName(),
            formatIssueCount = contactDao.countFormatIssues(),
            // Duplicates
            numberDuplicateCount = contactDao.countDuplicateNumbers(),
            emailDuplicateCount = contactDao.countDuplicateEmails(),
            nameDuplicateCount = contactDao.countDuplicateNames(),
            // V3
            accountCount = contactDao.countAccounts(),
            similarNameCount = 0 
        )
        
        emit(ScanStatus.Progress(1.0f))
        emit(ScanStatus.Success(result))
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    override suspend fun deleteContacts(contactIds: List<Long>): Boolean {
        val success = contactsProviderSource.deleteContacts(contactIds)
        if (success) {
            contactDao.deleteContacts(contactIds)
        }
        return success
    }

    override suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = flow {
        val contacts = getContactsSnapshotByType(type)
        if (contacts.isEmpty()) {
            emit(CleanupStatus.Success("No contacts to delete"))
            return@flow
        }
        
        var successCount = 0
        contacts.chunked(50).forEachIndexed { index, batch ->
            val ids = batch.map { it.id }
            if (deleteContacts(ids)) {
                successCount += batch.size
            }
            val progress = ((index + 1) * 50).toFloat() / contacts.size.toFloat()
            emit(CleanupStatus.Progress(progress.coerceAtMost(1f), "Deleted $successCount of ${contacts.size}"))
        }
        emit(CleanupStatus.Success("Successfully deleted $successCount contacts"))
    }

    private fun LocalContact.toDomain() = Contact(
        id = id,
        name = displayName,
        numbers = rawNumbers.split(","),
        emails = rawEmails.takeIf { it.isNotEmpty() }?.split(",") ?: emptyList(),
        normalizedNumber = normalizedNumber,
        isWhatsApp = isWhatsApp,
        isTelegram = isTelegram,
        isJunk = isJunk,
        junkType = junkType?.let { runCatching { com.ogabassey.contactscleaner.domain.model.JunkType.valueOf(it) }.getOrNull() },
        duplicateType = duplicateType?.let { runCatching { com.ogabassey.contactscleaner.domain.model.DuplicateType.valueOf(it) }.getOrNull() }
    )

    override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean {
        return contactsProviderSource.mergeContacts(contactIds, customName)
    }
    override suspend fun saveContacts(contacts: List<Contact>): Boolean = false

    override suspend fun getDuplicateGroups(type: ContactType): List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary> {
        return when(type) {
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberGroups()
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailGroups()
            ContactType.DUP_NAME -> contactDao.getDuplicateNameGroups()
            else -> emptyList()
        }
    }
    
    override suspend fun getAccountGroups(): List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary> {
        return contactDao.getAccountGroups()
    }

    override suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact> {
        val entities = when(type) {
            ContactType.DUP_NUMBER -> contactDao.getContactsByNumberKey(key)
            ContactType.DUP_EMAIL -> contactDao.getContactsByEmailKey(key)
            ContactType.DUP_NAME -> contactDao.getContactsByNameKey(key)
            else -> emptyList()
        }
        return entities.map { it.toDomain() }
    }

    override suspend fun mergeDuplicateGroups(type: ContactType): Flow<CleanupStatus> = flow {
        val groups = getDuplicateGroups(type)
        if (groups.isEmpty()) {
            emit(CleanupStatus.Success("No duplicates found"))
            return@flow
        }
        
        var successCount = 0
        groups.forEachIndexed { index, group ->
            val contacts = getContactsInGroup(group.groupKey, type)
            if (contacts.size > 1) {
                val ids = contacts.map { it.id }
                if (mergeContacts(ids)) {
                    successCount++
                }
            }
            val progress = (index + 1).toFloat() / groups.size.toFloat()
            emit(CleanupStatus.Progress(progress, "Merging group ${index + 1} of ${groups.size}"))
        }
        emit(CleanupStatus.Success("Merged $successCount groups successfully"))
    }
        
    override suspend fun standardizeFormat(ids: List<Long>): Boolean {
        if (ids.isEmpty()) return true
        val contacts = contactDao.getFormatIssueContactsByIds(ids)
        if (contacts.isEmpty()) return true
        
        var allSuccess = true
        contacts.forEach { contact ->
             val newNumber = contact.normalizedNumber
             if (newNumber != null) {
                 val success = contactsProviderSource.updateContactNumber(contact.id, newNumber)
                 if (success) {
                     contactDao.insertContacts(listOf(contact.copy(isFormatIssue = false, rawNumbers = newNumber)))
                 } else {
                     allSuccess = false
                 }
             }
        }
        return allSuccess
    }

    override suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
        val ids = contactDao.getFormatIssueIds()
        if (ids.isEmpty()) {
            emit(CleanupStatus.Success("No formatting issues found"))
            return@flow
        }
        
        var successCount = 0
        ids.chunked(50).forEachIndexed { index, batch ->
            if (standardizeFormat(batch)) {
                successCount += batch.size
            }
            val progress = ((index + 1) * 50).toFloat() / ids.size.toFloat()
            emit(CleanupStatus.Progress(progress.coerceAtMost(1f), "Standardized $successCount of ${ids.size}"))
        }
        emit(CleanupStatus.Success("Standardized $successCount contacts successfully"))
    }

    override suspend fun getContactsAllSnapshot(): List<Contact> {
        return contactDao.getAllContacts().map { it.toDomain() }
    }

    override suspend fun getContactsSnapshotByType(type: ContactType): List<Contact> {
        val entities = when (type) {
            ContactType.ALL -> contactDao.getAllContacts()
            ContactType.WHATSAPP -> contactDao.getWhatsAppContactsSnapshot()
            ContactType.NON_WHATSAPP -> contactDao.getNonWhatsAppContactsSnapshot()
            ContactType.JUNK -> contactDao.getJunkContactsSnapshot()
            ContactType.DUPLICATE -> contactDao.getDuplicateContactsSnapshot()
            ContactType.FORMAT_ISSUE -> contactDao.getFormatIssueContactsSnapshot()
            else -> contactDao.getAllContacts()
        }
        return entities.map { it.toDomain() }
    }

    override suspend fun updateScanResultSummary() {
        android.util.Log.d("ContactRepository", "Updating ScanResult Summary from DB...")
        val total = contactDao.countTotal()
        if (total == 0) {
            scanResultProvider.scanResult = null
            return
        }

        val result = ScanResult(
            total = total,
            rawCount = 0, 
            whatsAppCount = contactDao.countWhatsApp(),
            telegramCount = contactDao.countTelegram(),
            nonWhatsAppCount = total - contactDao.countWhatsApp(),
            junkCount = contactDao.countJunk(),
            duplicateCount = contactDao.countDuplicates(),
            noNameCount = contactDao.countNoName(),
            noNumberCount = contactDao.countNoNumber(),
            emailDuplicateCount = contactDao.countDuplicateEmails(),
            numberDuplicateCount = contactDao.countDuplicateNumbers(),
            nameDuplicateCount = contactDao.countDuplicateNames(),
            accountCount = contactDao.countAccounts(),
            similarNameCount = 0,
            invalidCharCount = contactDao.countInvalidChar(),
            longNumberCount = contactDao.countLongNumber(),
            shortNumberCount = contactDao.countShortNumber(),
            repetitiveNumberCount = contactDao.countRepetitiveNumber(),
            symbolNameCount = contactDao.countSymbolName(),
            formatIssueCount = contactDao.countFormatIssues()
        )
        scanResultProvider.scanResult = result
    }

    override suspend fun restoreContacts(contacts: List<Contact>): Boolean {
        val success = contactsProviderSource.restoreContacts(contacts)
        if (success) {
            // Re-sync local DB
            // Optimistically insert, or just clear and let next scan handle it?
            // "Undo" implies immediate visual feedback. 
            // Since our DB is a cache, we must insert them back so they appear in lists.
            // However, we don't know the NEW IDs generated by Android Provider until we re-scan.
            // So: 1. Clear Local DB? Or 2. Insert with placeholders?
            // Best approach: Just invalidate cache/trigger rescan, OR
            // To be fast: We can't easily insert into Room because we need the real IDs.
            // Strategy: Return true, and let ViewModel trigger a refresh/scan or user sees empty list until refresh.
            // Ideally: `contactDao.deleteAll()` then `scanContacts()`?
            // Let's rely on the caller to refresh, but here we just ensure Source restoration works.
        }
        return success
    }
}



