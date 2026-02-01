package com.ogabassey.contactscleaner.data.repository

import androidx.paging.PagingData
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.model.CrossAccountContact
import com.ogabassey.contactscleaner.domain.model.AccountInstance
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.util.formatWithCommas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

class ContactRepositoryImpl constructor(
    private val contactDao: ContactDao,
    private val contactsProviderSource: ContactsProviderSource,
    private val junkDetector: JunkDetector,
    private val duplicateDetector: DuplicateDetector,
    private val formatDetector: com.ogabassey.contactscleaner.data.detector.FormatDetector,
    private val sensitiveDetector: com.ogabassey.contactscleaner.data.detector.SensitiveDataDetector,
    private val ignoredContactDao: com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao,
    private val scanResultProvider: com.ogabassey.contactscleaner.data.util.ScanResultProvider,
    private val usageRepository: com.ogabassey.contactscleaner.domain.repository.UsageRepository,
    private val backupRepository: com.ogabassey.contactscleaner.domain.repository.BackupRepository
) : ContactRepository {
    override fun getContactsFlow(type: ContactType): Flow<List<Contact>> {
        return flow {
            val contacts = getContactsSnapshotByType(type)
            emit(contacts)
        }
    }

    override suspend fun scanContacts(): Flow<ScanStatus> = flow {
        android.util.Log.d("ContactRepository", "Starting Streamed SQL Scan (Optimum Performance)...")

        // 2026 Best Practice: Accumulate contacts first, then atomic replace at end
        // This prevents data loss if operation fails partway through
        val allEntities = mutableListOf<LocalContact>()
        emit(ScanStatus.Progress(0.01f, "Initializing scan..."))

        // 2. Fetch Total Count for Progress
        // 2026 Best Practice: Handle specific exceptions and surface failures
        var totalToProcess = 0
        try {
             val allIds = contactsProviderSource.getVerifiedContactIds()
             emit(ScanStatus.Progress(0.05f, "Fetching contact list..."))
             totalToProcess = allIds.size
        } catch (e: SecurityException) {
            // Missing contacts permission
            android.util.Log.e("ContactRepository", "Permission denied when fetching contacts", e)
            emit(ScanStatus.Error("Permission denied. Please grant contacts permission."))
            return@flow
        } catch (e: IllegalStateException) {
            // ContentProvider unavailable
            android.util.Log.e("ContactRepository", "ContentProvider unavailable", e)
            emit(ScanStatus.Error("Contacts provider unavailable. Please try again."))
            return@flow
        } catch (e: Exception) {
            // Log unexpected errors but continue with fallback
            android.util.Log.w("ContactRepository", "Failed to get contact count, using fallback", e)
            totalToProcess = 1000 // Fallback for progress calculation only
        }

        if (totalToProcess == 0) {
            emit(ScanStatus.Success(ScanResult()))
            return@flow
        }

        // 3. Stream Process
        val ignoredIds = ignoredContactDao.getAllIds().toSet()
        var processedCount = 0

        contactsProviderSource.getContactsStreaming(batchSize = 2500)
            .collect { batchContacts ->
                val entities = batchContacts.mapNotNull { contact ->
                    // 2026 Best Practice: Check Ignore List first (Skip if explicitly ignored)
                    // Note: In a production app, we'd pre-load a HashSet of ignored IDs for O(1) check
                    // but for this implementation we'll assume it's small or handled by filter logic.
                    // For performance, we'll implement it as a skip here.

                    val numbers = contact.numbers
                    val primaryNumber = numbers.firstOrNull() ?: ""
                    val isIgnored = ignoredIds.contains(contact.id.toString())

                    // Run Sensitive Data Detection (Safety Net)
                    var isSensitive = false
                    var sensitiveDesc: String? = null

                    if (!isIgnored) {
                        // 1. Scan Name
                        val nameMatch = sensitiveDetector.analyze(contact.name ?: "")
                        if (nameMatch != null) {
                            isSensitive = true
                            sensitiveDesc = nameMatch.description
                        }

                        // 2. Scan All Numbers (if not already found sensitive in name)
                        if (!isSensitive) {
                            for (num in contact.numbers) {
                                if (num.isNotBlank()) {
                                    val match = sensitiveDetector.analyze(num)
                                    if (match != null) {
                                        isSensitive = true
                                        sensitiveDesc = match.description
                                        break
                                    }
                                }
                                if (isSensitive) break
                            }
                        }
                    }

                    // Run Junk Detection
                    val junkType = if (!isIgnored) {
                        junkDetector.getJunkType(contact.name, contact.normalizedNumber ?: primaryNumber)
                    } else null

                    // Format Issue Detection (Enhanced)
                    var isFormatIssue = false
                    var detectedNormalized: String? = contact.normalizedNumber

                    // Format detection: Only for non-junk, non-sensitive contacts
                    if (junkType == null && !isSensitive && primaryNumber.isNotBlank()) {
                        // 1. Check if Provider already flagged it (Old Logic)
                        val normNum = contact.normalizedNumber
                        if (!primaryNumber.startsWith("+") &&
                             !primaryNumber.startsWith("*") &&
                             !primaryNumber.startsWith("#") &&
                             normNum != null &&
                             normNum.startsWith("+") &&
                             normNum != primaryNumber) {
                            isFormatIssue = true
                        }

                        // 2. If not flagged yet, run Advanced "Missing Plus" Check
                        if (!isFormatIssue && !primaryNumber.startsWith("+")) {
                             val issue = formatDetector.analyze(primaryNumber)
                             if (issue != null) {
                                 isFormatIssue = true
                                 detectedNormalized = issue.normalizedNumber
                             }
                        }
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
                        isJunk = junkType != null && !isSensitive, // Sensitive takes precedence over Junk
                        junkType = junkType?.name,
                        duplicateType = null,
                        isFormatIssue = isFormatIssue,
                        detectedRegion = if (isFormatIssue && detectedNormalized != null) formatDetector.getRegionCode(detectedNormalized) else null,
                        isSensitive = isSensitive,
                        sensitiveDescription = sensitiveDesc,
                        lastSynced = System.currentTimeMillis()
                    )
                }

                allEntities.addAll(entities)
                processedCount += batchContacts.size

                val syncProgress = 0.05f + (processedCount.toFloat() / totalToProcess.toFloat()) * 0.70f
                emit(ScanStatus.Progress(syncProgress.coerceAtMost(0.75f), "Processing contacts (${processedCount.formatWithCommas()})..."))
            }

        // 3.5 Atomic Replace: Delete old + Insert new in single transaction
        emit(ScanStatus.Progress(0.76f, "Saving contacts to database..."))

        // 2026 Best Practice: Validate data before insert
        val validatedEntities = allEntities.filter { contact ->
            val isValid = contact.id > 0 &&
                (contact.displayName?.length ?: 0) <= 1000 && // Prevent excessively long names
                contact.rawNumbers.length <= 10000 && // Reasonable limit for multiple numbers
                contact.rawEmails.length <= 10000
            if (!isValid) {
                android.util.Log.w("ContactRepository", "Filtered invalid contact: id=${contact.id}")
            }
            isValid
        }

        contactDao.replaceAllContacts(validatedEntities)

        // 4. Post-Process: Run Advanced Kotlin-based Duplicate Detection (Multi-number support)
        emit(ScanStatus.Progress(0.82f, "Analyzing duplicates..."))

        // Fetch all for analysis (using lightweight projection if possible, but full object needed for detector)
        val allContacts = contactDao.getAllContacts().map { it.toDomain() }
        val duplicates = duplicateDetector.detectDuplicates(allContacts)

        // Map duplicates to a map for O(1) lookup: ContactID -> Pair(Type, Key)
        val duplicateMap = mutableMapOf<Long, Pair<com.ogabassey.contactscleaner.domain.model.DuplicateType, String>>()
        duplicates.forEach { group ->
            group.contacts.forEach { contact ->
                // Priority: Ensure we don't overwrite if already processed?
                // Actually, `detectDuplicates` aggregates well.
                // If a contact is in multiple groups, the last one wins in this simple map,
                // or we could prioritize NUMBER > EMAIL > NAME.

                val current = duplicateMap[contact.id]
                // Priority: NUMBER > EMAIL > NAME
                val newPriority = when(group.duplicateType) {
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NUMBER_MATCH -> 3
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.EMAIL_MATCH -> 2
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NAME_MATCH -> 1
                    else -> 0
                }
                val currentPriority = when(current?.first) {
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NUMBER_MATCH -> 3
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.EMAIL_MATCH -> 2
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NAME_MATCH -> 1
                    else -> 0
                }

                if (newPriority >= currentPriority) {
                     duplicateMap[contact.id] = Pair(group.duplicateType, group.matchingKey)
                }
            }
        }

        if (duplicateMap.isNotEmpty()) {
             // Batch update duplicates
             // Optimized: Convert to LocalContact updates.
             // Since we can't easily do partial updates on varying fields for 1000s of rows efficiently via Room
             // without @Update entity, we will fetch, modify, update.
             // OR use a raw query loop.

             // Safer: Fetch affected IDs, apply changes, update.
             val affectedIds = duplicateMap.keys.toList()
             val affectedContacts = contactDao.getContactsByIds(affectedIds)
             val updates = affectedContacts.map { local ->
                 val info = duplicateMap[local.id]
                 if (info != null) {
                     local.copy(
                         duplicateType = info.first.name,
                         matchingKey = info.second
                     )
                 } else local
             }
             contactDao.insertContacts(updates) // @Insert(OnConflict=REPLACE) updates them
        }

        // Remove SQL-based logic calls
        // contactDao.markDuplicateNumbers()
        // contactDao.markDuplicateEmails()
        // contactDao.markDuplicateNames()


        emit(ScanStatus.Progress(0.95f, "Finalizing report..."))

        // 5. Build Result
        usageRepository.updateRawScannedCount(totalToProcess)
        updateScanResultSummary()
        val finalResult = scanResultProvider.scanResult ?: ScanResult()

        emit(ScanStatus.Progress(1.0f))
        emit(ScanStatus.Success(finalResult))
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteContacts(contacts: List<Contact>): Result<Unit> {
        return try {
            // Record for history/undo before deletion
            if (contacts.isNotEmpty()) {
                backupRepository.performBackup(
                    contacts = contacts,
                    actionType = "DELETE",
                    description = "Deleted ${contacts.size} contact${if (contacts.size > 1) "s" else ""}"
                )
            }

            val ids = contacts.map { it.id }
            val providerSuccess = contactsProviderSource.deleteContacts(ids)

            // 2026 Best Practice: Always cascade delete to local cache
            // Even if provider delete fails, clean local cache to avoid stale data
            try {
                contactDao.deleteContacts(ids)
            } catch (e: Exception) {
                android.util.Log.e("ContactRepository", "Failed to cascade delete to local cache", e)
            }

            // Update scan result summary to reflect changes
            updateScanResultSummary()

            if (providerSuccess) Result.success(Unit) else Result.failure(Exception("Failed to delete contacts"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContactsByIds(contactIds: List<Long>): Boolean {
        val providerSuccess = contactsProviderSource.deleteContacts(contactIds)

        // 2026 Best Practice: Always cascade delete to local cache
        // Even if provider delete fails, clean local cache to avoid stale data
        // On next scan, contacts still on device will be re-synced
        try {
            contactDao.deleteContacts(contactIds)
        } catch (e: Exception) {
            android.util.Log.e("ContactRepository", "Failed to cascade delete to local cache", e)
        }

        return providerSuccess
    }

    override suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = flow {
        val contacts = getContactsSnapshotByType(type)
        if (contacts.isEmpty()) {
            emit(CleanupStatus.Success("No contacts to delete"))
            return@flow
        }

        // Record for history
        backupRepository.performBackup(
            contacts = contacts,
            actionType = "DELETE",
            description = "Deleted ${contacts.size} contacts from $type"
        )

        // 2026 Best Practice: Track processed count accurately for progress
        var successCount = 0
        var processedCount = 0
        contacts.chunked(50).forEach { batch ->
            val ids = batch.map { it.id }
            if (deleteContactsByIds(ids)) {
                successCount += batch.size
            }
            processedCount += batch.size
            val progress = processedCount.toFloat() / contacts.size.toFloat()
            emit(CleanupStatus.Progress(progress.coerceAtMost(1f), "Deleted $successCount of ${contacts.size}"))
        }

        // Refresh summary
        updateScanResultSummary()

        emit(CleanupStatus.Success("Successfully deleted $successCount contacts"))
    }

    private fun LocalContact.toDomain() = Contact(
        id = id,
        name = displayName,
        numbers = rawNumbers.split(",").filter { it.isNotBlank() },
        emails = rawEmails.split(",").filter { it.isNotBlank() },
        normalizedNumber = normalizedNumber,
        isWhatsApp = isWhatsApp,
        isTelegram = isTelegram,
        isJunk = isJunk,
        junkType = junkType?.let { runCatching { com.ogabassey.contactscleaner.domain.model.JunkType.valueOf(it) }.getOrNull() },
        duplicateType = duplicateType?.let { runCatching { com.ogabassey.contactscleaner.domain.model.DuplicateType.valueOf(it) }.getOrNull() },
        accountType = accountType,
        accountName = accountName,
        isSensitive = isSensitive,
        sensitiveDescription = sensitiveDescription,
        formatIssue = null // Default for now
    )

    override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean {
        val success = contactsProviderSource.mergeContacts(contactIds, customName)
        if (success) {
            // Update scan result summary to reflect merged contacts
            updateScanResultSummary()
        }
        return success
    }
    // 2026 Best Practice: Implement saveContacts by delegating to platform source
    override suspend fun saveContacts(contacts: List<Contact>): Boolean {
        return contactsProviderSource.restoreContacts(contacts)
    }

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
                // Record for history
                backupRepository.performBackup(
                    contacts = contacts,
                    actionType = "MERGE",
                    description = "Merged ${contacts.size} duplicates (${group.groupKey})"
                )

                val ids = contacts.map { it.id }
                if (mergeContacts(ids)) {
                    successCount++
                }
            }
            val progress = (index + 1).toFloat() / groups.size.toFloat()
            emit(CleanupStatus.Progress(progress, "Merging group ${index + 1} of ${groups.size}"))
        }

        // Refresh summary
        updateScanResultSummary()

        emit(CleanupStatus.Success("Merged $successCount groups successfully"))
    }

    override suspend fun standardizeFormat(ids: List<Long>): Boolean {
        if (ids.isEmpty()) return true
        val contacts = contactDao.getFormatIssueContactsByIds(ids)
        if (contacts.isEmpty()) return true

        val updates = contacts.associate { it.id to (it.normalizedNumber ?: "") }.filterValues { it.isNotBlank() }
        if (updates.isEmpty()) return true

        val success = contactsProviderSource.updateMultipleContactNumbers(updates)
        if (success) {
            val updatedEntities = contacts.map { it.copy(isFormatIssue = false, rawNumbers = it.normalizedNumber ?: it.rawNumbers) }
            contactDao.insertContacts(updatedEntities)
        }
        return success
    }

    override suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
        val ids = contactDao.getFormatIssueIds()
        if (ids.isEmpty()) {
            emit(CleanupStatus.Success("No formatting issues found"))
            return@flow
        }

        var successCount = 0
        val total = ids.size

        // Record for history
        val formatIssues = contactDao.getFormatIssueContactsByIds(ids).map { it.toDomain() }
        backupRepository.performBackup(
            contacts = formatIssues,
            actionType = "FORMAT",
            description = "Standardized ${formatIssues.size} numbers"
        )

        // 2026 Best Practice: Large batches for high-speed processing
        ids.chunked(500).forEachIndexed { index, batch ->
            if (standardizeFormat(batch)) {
                successCount += batch.size
            }
            val progress = successCount.toFloat() / total.toFloat()
            emit(CleanupStatus.Progress(progress.coerceAtMost(1f), "Standardizing... [$successCount of $total]"))
        }

        // Refresh summary
        updateScanResultSummary()

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
            ContactType.SENSITIVE -> contactDao.getSensitiveContactsSnapshot()
            // Granular Duplicates
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberContactsSnapshot()
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailContactsSnapshot()
            ContactType.DUP_NAME -> contactDao.getDuplicateNameContactsSnapshot()
            ContactType.DUP_SIMILAR_NAME -> contactDao.getSimilarNameContactsSnapshot()
            ContactType.DUP_CROSS_ACCOUNT -> contactDao.getCrossAccountContactsSnapshot()
            // Granular Junk
            ContactType.JUNK_NO_NAME -> contactDao.getNoNameContactsSnapshot()
            ContactType.JUNK_NO_NUMBER -> contactDao.getNoNumberContactsSnapshot()
            ContactType.JUNK_INVALID_CHAR -> contactDao.getInvalidCharContactsSnapshot()
            ContactType.JUNK_LONG_NUMBER -> contactDao.getLongNumberContactsSnapshot()
            ContactType.JUNK_SHORT_NUMBER -> contactDao.getShortNumberContactsSnapshot()
            ContactType.JUNK_REPETITIVE -> contactDao.getRepetitiveNumberContactsSnapshot()
            ContactType.JUNK_SYMBOL -> contactDao.getSymbolNameContactsSnapshot()
            ContactType.JUNK_NUMERICAL_NAME -> contactDao.getNumericalNameContactsSnapshot()
            ContactType.JUNK_EMOJI_NAME -> contactDao.getEmojiNameContactsSnapshot()
            // V3
            ContactType.ACCOUNT -> contactDao.getAllContacts() // Default fallback for accounts
            else -> contactDao.getAllContacts()
        }
        return entities.map { it.toDomain() }.sortedBy { it.name ?: "" }
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
            rawCount = usageRepository.rawScannedCount.first(),
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
            similarNameCount = contactDao.countSimilarNames(),
            invalidCharCount = contactDao.countInvalidChar(),
            longNumberCount = contactDao.countLongNumber(),
            shortNumberCount = contactDao.countShortNumber(),
            repetitiveNumberCount = contactDao.countRepetitiveNumber(),
            symbolNameCount = contactDao.countSymbolName(),
            numericalNameCount = contactDao.countNumericalName(),
            emojiNameCount = contactDao.countEmojiName(),
            fancyFontCount = contactDao.countFancyFontName(),
            formatIssueCount = contactDao.countFormatIssues(),
            sensitiveCount = contactDao.countSensitive(),
            crossAccountDuplicateCount = contactDao.countCrossAccountContacts()
        )
        scanResultProvider.scanResult = result
    }

    override suspend fun recalculateWhatsAppCounts() {
        // Android uses native WhatsApp detection via account_type, no VPS cache needed.
        // Just refresh the summary which already has correct counts.
        updateScanResultSummary()
    }

    override suspend fun restoreContacts(contacts: List<Contact>): Boolean {
        val success = contactsProviderSource.restoreContacts(contacts)
        return success
    }

    override suspend fun ignoreContact(id: String, displayName: String, reason: String): Boolean {
        val longId = id.toLongOrNull() ?: return false
        ignoredContactDao.insert(com.ogabassey.contactscleaner.data.db.entity.IgnoredContact(id, displayName, reason, System.currentTimeMillis()))
        contactDao.resetSensitiveFlag(longId)
        updateScanResultSummary()
        return true
    }

    override suspend fun unignoreContact(id: String): Boolean {
        ignoredContactDao.delete(id)
        // Update scan result to reflect unignored contact
        updateScanResultSummary()
        return true
    }

    override fun getIgnoredContacts(): Flow<List<com.ogabassey.contactscleaner.data.db.entity.IgnoredContact>> {
        return ignoredContactDao.getAll()
    }

    override fun getAccountCount(): Flow<Int> = flow {
        emit(contactDao.countAccounts())
    }.flowOn(Dispatchers.IO)

    /**
     * Get all contacts that exist in multiple accounts, grouped by matching key.
     */
    override suspend fun getCrossAccountContacts(): List<CrossAccountContact> {
        val allInstances = contactDao.getCrossAccountContactsSnapshot()

        // Group by matching_key
        return allInstances.groupBy { it.matchingKey ?: "" }
            .filter { it.key.isNotBlank() && it.value.isNotEmpty() }
            .mapNotNull { (key, instances) ->
                val first = instances.firstOrNull() ?: return@mapNotNull null
                CrossAccountContact(
                    name = first.displayName,
                    matchingKey = key,
                    primaryNumber = first.rawNumbers.split(",").filter { it.isNotBlank() }.firstOrNull(),
                    primaryEmail = first.rawEmails.split(",").filter { it.isNotBlank() }.firstOrNull(),
                    accounts = instances.map { instance ->
                        AccountInstance(
                            contactId = instance.id,
                            accountType = instance.accountType,
                            accountName = instance.accountName,
                            displayLabel = getAccountDisplayLabel(instance.accountType)
                        )
                    }
                )
            }
            .sortedBy { it.name ?: "" }
    }

    /**
     * Get all instances of a contact across accounts by matching key.
     */
    override suspend fun getContactInstancesByMatchingKey(matchingKey: String): List<Contact> {
        return contactDao.getContactInstancesByMatchingKey(matchingKey).map { it.toDomain() }
    }

    /**
     * Consolidate a contact to a single account by deleting it from all other accounts.
     * @param matchingKey The matching key of the contact to consolidate
     * @param keepAccountType The account type to keep (e.g., "com.google")
     * @param keepAccountName The account name to keep (e.g., "user@gmail.com")
     * @return True if successful
     */
    override suspend fun consolidateContactToAccount(
        matchingKey: String,
        keepAccountType: String?,
        keepAccountName: String?
    ): Boolean {
        val instances = contactDao.getContactInstancesByMatchingKey(matchingKey)
        if (instances.size < 2) return false

        // Find IDs to delete (all except the one to keep)
        val idsToDelete = instances
            .filter { it.accountType != keepAccountType || it.accountName != keepAccountName }
            .map { it.id }

        if (idsToDelete.isEmpty()) return false

        // Record for backup
        val contactsToDelete = instances
            .filter { it.id in idsToDelete }
            .map { it.toDomain() }

        backupRepository.performBackup(
            contacts = contactsToDelete,
            actionType = "CONSOLIDATE",
            description = "Consolidated contact to ${getAccountDisplayLabel(keepAccountType)} ($keepAccountName)"
        )

        // Delete from device
        val success = deleteContactsByIds(idsToDelete)
        if (success) {
            updateScanResultSummary()
        }
        return success
    }

    /**
     * Consolidate multiple contacts to a single account.
     */
    override suspend fun consolidateContactsToAccount(
        matchingKeys: List<String>,
        keepAccountType: String?,
        keepAccountName: String?
    ): Flow<CleanupStatus> = flow {
        if (matchingKeys.isEmpty()) {
            emit(CleanupStatus.Success("No contacts to consolidate"))
            return@flow
        }

        var successCount = 0
        matchingKeys.forEachIndexed { index, key ->
            if (consolidateContactToAccount(key, keepAccountType, keepAccountName)) {
                successCount++
            }
            val progress = (index + 1).toFloat() / matchingKeys.size.toFloat()
            emit(CleanupStatus.Progress(progress, "Consolidating ${index + 1} of ${matchingKeys.size}"))
        }

        // Refresh summary
        updateScanResultSummary()

        emit(CleanupStatus.Success("Consolidated $successCount contacts successfully"))
    }

    private fun getAccountDisplayLabel(accountType: String?): String {
        return when {
            accountType == null -> "Local"
            accountType.contains("google", ignoreCase = true) -> "Google"
            accountType.contains("icloud", ignoreCase = true) -> "iCloud"
            accountType.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
            accountType.contains("telegram", ignoreCase = true) -> "Telegram"
            accountType.contains("exchange", ignoreCase = true) -> "Exchange"
            accountType.contains("yahoo", ignoreCase = true) -> "Yahoo"
            accountType.contains("outlook", ignoreCase = true) -> "Outlook"
            else -> accountType.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
}
