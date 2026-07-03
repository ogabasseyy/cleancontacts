package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.platform.Logger

import androidx.paging.PagingData
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.dao.WhatsAppCacheDao
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.source.ContactsProviderSource
import com.ogabassey.contactscleaner.domain.model.CleanupDetails
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.model.CrossAccountContact
import com.ogabassey.contactscleaner.domain.model.AccountInstance
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.util.firstNonBlankSegment
import com.ogabassey.contactscleaner.util.formatWithCommas
import com.ogabassey.contactscleaner.util.splitAndFilterNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
    private val backupRepository: com.ogabassey.contactscleaner.domain.repository.BackupRepository,
    private val whatsAppCacheDao: WhatsAppCacheDao? = null
) : ContactRepository {

    private companion object {
        private const val WHATSAPP_CACHE_VALIDITY_MILLIS = 24L * 60L * 60L * 1000L
    }

    private suspend fun recordBackupSafely(
        contacts: List<Contact>,
        actionType: String,
        description: String
    ) {
        if (contacts.isEmpty()) return

        try {
            backupRepository.performBackup(
                contacts = contacts,
                actionType = actionType,
                description = description
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w("ContactRepository", "Backup failed after successful $actionType operation: ${e.message}")
        }
    }

    /**
     * 2026 Best Practice: Extract shared contact processing logic.
     * Processes a single contact through all detectors and builds a LocalContact entity.
     * Used by both scanContacts() and refreshContacts() to eliminate code duplication.
     *
     * @param contact The contact to process
     * @param ignoredIds Set of contact IDs that should skip sensitive/junk detection
     * @return LocalContact entity ready for database insertion
     */
    private fun processContactToEntity(
        contact: Contact,
        ignoredIds: Set<Long>
    ): LocalContact {
        val numbers = contact.numbers
        val primaryNumber = numbers.firstOrNull() ?: ""
        val isIgnored = ignoredIds.contains(contact.id)

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
            // ⚡ Bolt Optimization: Replace multiple `startsWith` with primitive Char
            // comparison to avoid method-call overhead and object allocations.
            val primaryFirstChar = if (primaryNumber.isNotEmpty()) primaryNumber[0] else null

            // 1. Check if Provider already flagged it
            val normNum = contact.normalizedNumber
            val normFirstChar = if (!normNum.isNullOrEmpty()) normNum[0] else null
            val hasBlockedPrefix = primaryFirstChar == '+' || primaryFirstChar == '*' || primaryFirstChar == '#'
            val providerShowsIntl = normFirstChar == '+'
            val providerDiffersFromRaw = normNum != primaryNumber

            if (primaryFirstChar != null &&
                !hasBlockedPrefix &&
                providerShowsIntl &&
                providerDiffersFromRaw
            ) {
                isFormatIssue = true
            }

            // 2. If not flagged yet, run Advanced "Missing Plus" Check
            if (!isFormatIssue && !hasBlockedPrefix) {
                val issue = formatDetector.analyze(primaryNumber)
                if (issue != null) {
                    isFormatIssue = true
                    detectedNormalized = issue.normalizedNumber
                }
            }
        }

        if (junkType == null && !isSensitive && !isFormatIssue) {
            contact.numbers.forEach { number ->
                if (isFormatIssue) return@forEach

                val firstChar = number.firstOrNull()
                val hasBlockedPrefix = firstChar == '+' || firstChar == '*' || firstChar == '#'
                if (!number.isBlank() && !hasBlockedPrefix) {
                    formatDetector.analyze(number)?.let { issue ->
                        isFormatIssue = true
                        detectedNormalized = issue.normalizedNumber
                    }
                }
            }
        }

        return LocalContact(
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

    private suspend fun getValidCachedWhatsAppResults(): Map<String, Boolean> {
        val dao = whatsAppCacheDao ?: return emptyMap()
        val meta = dao.getMeta() ?: return emptyMap()
        if (meta.syncInProgress || meta.lastFullSync <= 0L) return emptyMap()

        val cacheAgeMillis = System.currentTimeMillis() - meta.lastFullSync
        if (cacheAgeMillis >= WHATSAPP_CACHE_VALIDITY_MILLIS) return emptyMap()

        return dao.getAllEntries().associate { entry ->
            entry.normalizedNumber to entry.hasWhatsApp
        }
    }

    private fun applyCachedWhatsAppResult(
        contact: LocalContact,
        cachedWhatsAppResults: Map<String, Boolean>
    ): LocalContact {
        return WhatsAppAccuracyMatcher.applyResults(contact, cachedWhatsAppResults)
    }

    override fun getContactsFlow(type: ContactType): Flow<List<Contact>> {
        return flow {
            val contacts = getContactsSnapshotByType(type)
            emit(contacts)
        }
    }

    override suspend fun scanContacts(): Flow<ScanStatus> = flow {
        Logger.d("ContactRepository", "Starting Streamed SQL Scan (Optimum Performance)...")

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
            Logger.e("ContactRepository", "Permission denied when fetching contacts", e)
            emit(ScanStatus.Error("Permission denied. Please grant contacts permission."))
            return@flow
        } catch (e: IllegalStateException) {
            // ContentProvider unavailable
            Logger.e("ContactRepository", "ContentProvider unavailable", e)
            emit(ScanStatus.Error("Contacts provider unavailable. Please try again."))
            return@flow
        } catch (e: Exception) {
            // Log unexpected errors but continue with fallback
            Logger.e("ContactRepository", "Failed to get contact count, using fallback", e)
            totalToProcess = 1000 // Fallback for progress calculation only
        }

        if (totalToProcess == 0) {
            contactDao.replaceAllContacts(emptyList())
            updateScanResultSummary()
            emit(ScanStatus.Success(ScanResult()))
            return@flow
        }

        // 3. Stream Process
        val ignoredIdsStrings = ignoredContactDao.getAllIds()
        val ignoredIds = HashSet<Long>(ignoredIdsStrings.size)
        for (i in ignoredIdsStrings.indices) {
            val parsed = ignoredIdsStrings[i].toLongOrNull()
            if (parsed != null) ignoredIds.add(parsed)
        }
        val cachedWhatsAppResults = getValidCachedWhatsAppResults()
        var processedCount = 0

        contactsProviderSource.getContactsStreaming(batchSize = 2500)
            .collect { batchContacts ->
                // ⚡ Bolt Optimization: Replace multiple passes (.map + .addAll + .filter)
                // with a single manual loop to eliminate intermediate ArrayList allocations
                // and minimize garbage collection overhead during large contact scans.
                for (i in batchContacts.indices) {
                    val contact = batchContacts[i]
                    val entity = applyCachedWhatsAppResult(
                        contact = processContactToEntity(contact, ignoredIds),
                        cachedWhatsAppResults = cachedWhatsAppResults
                    )

                    val isValid = entity.id > 0 &&
                        (entity.displayName?.length ?: 0) <= 1000 && // Prevent excessively long names
                        entity.rawNumbers.length <= 10000 && // Reasonable limit for multiple numbers
                        entity.rawEmails.length <= 10000

                    if (isValid) {
                        allEntities.add(entity)
                    } else {
                        Logger.w("ContactRepository", "Filtered invalid contact: id=${entity.id}")
                    }
                }

                processedCount += batchContacts.size

                val syncProgress = 0.05f + (processedCount.toFloat() / totalToProcess.toFloat()) * 0.70f
                emit(ScanStatus.Progress(syncProgress.coerceAtMost(0.75f), "Processing contacts (${processedCount.formatWithCommas()})..."))
            }

        // 4. In-Memory Duplicate Detection (⚡ Bolt Optimization: Combine with initial insert)
        emit(ScanStatus.Progress(0.76f, "Analyzing duplicates..."))

        val finalEntities = ContactDuplicateMetadataResolver.apply(allEntities, duplicateDetector)

        // 5. Atomic Replace: Delete old + Insert new in single transaction
        emit(ScanStatus.Progress(0.85f, "Saving contacts to database..."))
        contactDao.replaceAllContacts(finalEntities)


        emit(ScanStatus.Progress(0.95f, "Finalizing report..."))

        // 5. Build Result — use actual processed count, not the fallback estimate
        usageRepository.updateRawScannedCount(finalEntities.size)
        updateScanResultSummary()
        val finalResult = scanResultProvider.scanResult ?: ScanResult()

        emit(ScanStatus.Progress(1.0f))
        emit(ScanStatus.Success(finalResult))
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteContacts(contacts: List<Contact>): Result<Unit> {
        return try {
            val ids = contacts.map { it.id }
            Logger.i("ContactRepository", "deleteContacts requested count=${ids.size} ids=${idsForLog(ids)}")
            if (deleteContactsFromProviderAndSync(ids)) {
                Logger.i("ContactRepository", "deleteContacts succeeded count=${ids.size} ids=${idsForLog(ids)}")
                recordBackupSafely(
                    contacts = contacts,
                    actionType = "DELETE",
                    description = "Deleted ${contacts.size} contact${if (contacts.size > 1) "s" else ""}"
                )
                Result.success(Unit)
            } else {
                Logger.e("ContactRepository", "deleteContacts failed count=${ids.size} ids=${idsForLog(ids)}")
                Result.failure(Exception("Failed to delete contacts from device"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ContactRepository", "deleteContacts threw", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteContactsByIds(contactIds: List<Long>): Boolean {
        return deleteContactsFromProviderAndSync(contactIds)
    }

    private data class DeleteSyncResult(
        val success: Boolean,
        val deletedIds: List<Long>,
        val requiresDuplicateRebuild: Boolean
    )

    private suspend fun deleteContactsFromProviderAndSync(contactIds: List<Long>): Boolean {
        return deleteContactsFromProviderAndLocalCache(
            contactIds = contactIds,
            refreshAfterDelete = true
        ).success
    }

    private suspend fun deleteContactsFromProviderAndLocalCache(
        contactIds: List<Long>,
        refreshAfterDelete: Boolean
    ): DeleteSyncResult {
        if (contactIds.isEmpty()) {
            return DeleteSyncResult(success = true, deletedIds = emptyList(), requiresDuplicateRebuild = false)
        }

        val requiresDuplicateRebuild = contactDao.getContactsByIds(contactIds).any { it.duplicateType != null }
        val providerSuccess = contactsProviderSource.deleteContacts(contactIds)
        if (!providerSuccess) {
            Logger.e(
                "ContactRepository",
                "Provider delete failed for ${contactIds.size} contacts; skipping local cache delete ids=${idsForLog(contactIds)}"
            )
            // Provider delete can fail after deleting a subset (for example, mixed writable/read-only contacts).
            // Re-scan cache so UI reflects actual device state instead of stale local rows.
            val rebuilt = rebuildLocalCacheFromProvider()
            if (!rebuilt) {
                Logger.e("ContactRepository", "Local cache rebuild failed after provider delete failure")
            }
            return DeleteSyncResult(success = false, deletedIds = emptyList(), requiresDuplicateRebuild = false)
        }

        return try {
            contactDao.deleteContacts(contactIds)
            val refreshSucceeded = if (refreshAfterDelete) {
                if (requiresDuplicateRebuild) {
                    rebuildDuplicateMetadataFromLocalCache()
                } else {
                    updateScanResultSummary()
                    true
                }
            } else {
                true
            }

            DeleteSyncResult(
                success = refreshSucceeded,
                deletedIds = if (refreshSucceeded) contactIds else emptyList(),
                requiresDuplicateRebuild = requiresDuplicateRebuild
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ContactRepository", "Failed to cascade delete to local cache", e)
            rebuildLocalCacheFromProvider()
            DeleteSyncResult(success = false, deletedIds = emptyList(), requiresDuplicateRebuild = false)
        }
    }

    private fun idsForLog(ids: List<Long>, maxIds: Int = 10): String {
        val suffix = if (ids.size > maxIds) ",..." else ""
        return ids.take(maxIds).joinToString(prefix = "[", postfix = suffix + "]")
    }

    override suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = flow {
        val contacts = getContactsSnapshotByType(type)
        if (contacts.isEmpty()) {
            emit(CleanupStatus.Success("No contacts to delete"))
            return@flow
        }

        // 2026 Best Practice: Track processed count accurately for progress
        var successCount = 0
        var processedCount = 0
        val deletedContacts = mutableListOf<Contact>()
        var requiresDuplicateRebuild = false
        val batchSize = DeleteBatchPlanner.batchSize(contacts.size)

        Logger.i(
            "ContactRepository",
            "deleteContactsByType started type=$type total=${contacts.size} batchSize=$batchSize"
        )

        contacts.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            val ids = batch.map { it.id }
            val batchResult = deleteContactsFromProviderAndLocalCache(
                contactIds = ids,
                refreshAfterDelete = false
            )
            if (batchResult.success) {
                successCount += batchResult.deletedIds.size
                deletedContacts.addAll(batch)
                requiresDuplicateRebuild = requiresDuplicateRebuild || batchResult.requiresDuplicateRebuild
            }
            processedCount += batch.size
            Logger.d(
                "ContactRepository",
                "deleteContactsByType batch=${batchIndex + 1} processed=$processedCount/${contacts.size} successCount=$successCount"
            )
            val progress = processedCount.toFloat() / contacts.size.toFloat()
            emit(CleanupStatus.Progress(progress.coerceAtMost(1f), "Deleted $successCount of ${contacts.size}"))
        }

        val refreshed = if (successCount > 0) {
            if (requiresDuplicateRebuild) {
                rebuildDuplicateMetadataFromLocalCache()
            } else {
                updateScanResultSummary()
                true
            }
        } else {
            updateScanResultSummary()
            true
        }

        if (!refreshed) {
            Logger.e("ContactRepository", "deleteContactsByType refresh failed type=$type successCount=$successCount total=${contacts.size}")
            emit(CleanupStatus.Error("Deleted $successCount contacts but failed to refresh local cache"))
            return@flow
        }

        if (deletedContacts.isNotEmpty()) {
            recordBackupSafely(
                contacts = deletedContacts,
                actionType = "DELETE",
                description = "Deleted ${deletedContacts.size} contacts from $type"
            )
        }

        when {
            successCount == contacts.size -> emit(CleanupStatus.Success("Successfully deleted $successCount contacts"))
            successCount > 0 -> emit(CleanupStatus.Partial("Deleted $successCount of ${contacts.size} contacts"))
            else -> emit(CleanupStatus.Error("Failed to delete contacts"))
        }
    }

    private fun LocalContact.toDomain() = Contact(
        id = id,
        name = displayName,
        numbers = rawNumbers.splitAndFilterNotBlank(','),
        emails = rawEmails.splitAndFilterNotBlank(','),
        normalizedNumber = normalizedNumber,
        isWhatsApp = isWhatsApp,
        isTelegram = isTelegram,
        isJunk = isJunk,
        junkType = junkType?.let { runCatching { com.ogabassey.contactscleaner.domain.model.JunkType.valueOf(it) }.getOrNull() },
        duplicateType = duplicateType?.let { runCatching { com.ogabassey.contactscleaner.domain.model.DuplicateType.valueOf(it) }.getOrNull() },
        matchingKey = matchingKey,
        accountType = accountType,
        accountName = accountName,
        isSensitive = isSensitive,
        sensitiveDescription = sensitiveDescription,
        formatIssue = null // Default for now
    )

    override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean {
        val success = performProviderMerge(contactIds, customName)
        if (success) {
            return rebuildLocalCacheFromProvider()
        }
        return success
    }
    // 2026 Best Practice: Implement saveContacts by delegating to platform source
    override suspend fun saveContacts(contacts: List<Contact>): Boolean {
        val success = contactsProviderSource.restoreContacts(contacts)
        if (success) {
            return rebuildLocalCacheFromProvider()
        }
        return success
    }

    override suspend fun getDuplicateGroups(type: ContactType): List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary> {
        return when(type) {
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberGroups()
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailGroups()
            ContactType.DUP_NAME -> contactDao.getDuplicateNameGroups()
            ContactType.DUP_SIMILAR_NAME -> contactDao.getSimilarNameGroups()
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
            ContactType.DUP_SIMILAR_NAME -> contactDao.getContactsBySimilarNameKey(key)
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
        var skippedSyncedCount = 0
        var failedMergeableCount = 0
        groups.forEachIndexed { index, group ->
            val contacts = getContactsInGroup(group.groupKey, type)
            val mergeableContacts = DuplicateMergeCandidateFilter.mergeableContacts(contacts)
            if (contacts.size > 1 && mergeableContacts.size < 2) {
                skippedSyncedCount++
                Logger.i(
                    "ContactRepository",
                    "Skipping synced duplicate group during merge type=$type index=${index + 1}/${groups.size} contacts=${contacts.size} mergeable=${mergeableContacts.size}"
                )
            } else if (mergeableContacts.size > 1) {
                val ids = mergeableContacts.map { it.id }
                if (performProviderMerge(ids)) {
                    recordBackupSafely(
                        contacts = mergeableContacts,
                        actionType = "MERGE",
                        description = "Merged ${mergeableContacts.size} duplicates (${group.groupKey})"
                    )
                    successCount++
                } else {
                    failedMergeableCount++
                    Logger.w(
                        "ContactRepository",
                        "Provider-backed duplicate merge failed type=$type index=${index + 1}/${groups.size} mergeable=${mergeableContacts.size}"
                    )
                }
            }
            val progress = (index + 1).toFloat() / groups.size.toFloat()
            emit(CleanupStatus.Progress(progress, "Merging group ${index + 1} of ${groups.size}"))
        }

        if (successCount > 0) {
            val cacheRebuilt = rebuildLocalCacheFromProvider()
            if (!cacheRebuilt) {
                emit(CleanupStatus.Error("Merged $successCount groups but failed to refresh local cache"))
                return@flow
            }
        } else {
            updateScanResultSummary()
        }

        emit(
            DuplicateMergeCompletion.status(
                totalGroups = groups.size,
                mergedGroups = successCount,
                skippedSyncedGroups = skippedSyncedCount,
                failedMergeableGroups = failedMergeableCount
            )
        )
    }

    override suspend fun standardizeFormat(ids: List<Long>): Boolean {
        if (ids.isEmpty()) return true
        val result = standardizeFormatBatch(ids)
        if (result.updatedIds.isNotEmpty()) {
            return rebuildLocalCacheFromProvider() && result.updatedIds.size == result.attemptedCount
        }
        return result.attemptedCount == 0
    }

    override suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
        val ids = contactDao.getFormatIssueIds()
        if (ids.isEmpty()) {
            emit(CleanupStatus.Success("No formatting issues found"))
            return@flow
        }

        var successCount = 0
        var processedCount = 0
        val total = ids.size
        val updatedContactIds = linkedSetOf<Long>()

        Logger.i("ContactRepository", "standardizeAllFormatIssues started total=$total")

        // Pre-fetch contact names for streaming display
        val contactEntities = contactDao.getFormatIssueContactsByIds(ids)
        val contactNames = contactEntities.associate { it.id to (it.displayName ?: "Unknown") }

        // Track recent items for streaming log
        val recentItems = mutableListOf<String>()

        // 2026 Optimization: Smaller batches (50) for 10x faster visual feedback
        // Previously used 500 which caused "stuck at 0%" perception
        ids.chunked(50).forEachIndexed { batchIndex, batch ->
            try {
                val batchResult = standardizeFormatBatch(batch)
                if (batchResult.updatedIds.isNotEmpty()) {
                    successCount += batchResult.updatedIds.size
                    updatedContactIds.addAll(batchResult.updatedIds)

                    // Add batch items to recent list for streaming display
                    batchResult.updatedIds.forEach { id ->
                        val name = contactNames[id]
                        if (name != null) {
                            recentItems.add(0, "Updated: $name")
                            if (recentItems.size > 10) recentItems.removeAt(recentItems.lastIndex)
                        }
                    }
                }

                processedCount += batch.size
                val progress = processedCount.toFloat() / total.toFloat()
                val currentItem = batch.lastOrNull()?.let { contactNames[it] }

                Logger.d(
                    "ContactRepository",
                    "standardizeAllFormatIssues batch=${batchIndex + 1} processed=$processedCount/$total updated=${batchResult.updatedIds.size} successCount=$successCount attempted=${batchResult.attemptedCount}"
                )

                emit(CleanupStatus.Progress(
                    progress = progress.coerceAtMost(1f),
                    message = "Standardizing: ${currentItem ?: "..."} [$processedCount of $total]",
                    details = CleanupDetails(
                        processed = processedCount,
                        total = total,
                        currentItem = currentItem,
                        recentItems = recentItems.toList()
                    )
                ))
            } catch (e: CancellationException) {
                Logger.w(
                    "ContactRepository",
                    "standardizeAllFormatIssues cancelled batch=${batchIndex + 1} processed=$processedCount/$total successCount=$successCount reason=${e.message ?: "none"}"
                )
                throw e
            }
        }

        if (updatedContactIds.isNotEmpty()) {
            // ⚡ Bolt Optimization: Replace chained mapping sequence with single-pass indexed loop.
            val updatedContacts = ArrayList<Contact>(updatedContactIds.size)
            for (i in contactEntities.indices) {
                val entity = contactEntities[i]
                if (updatedContactIds.contains(entity.id)) {
                    updatedContacts.add(entity.toDomain())
                }
            }
            if (updatedContacts.isNotEmpty()) {
                recordBackupSafely(
                    contacts = updatedContacts,
                    actionType = "FORMAT",
                    description = "Standardized ${updatedContacts.size} numbers"
                )
            }
        }

        if (successCount > 0) {
            val cacheRebuilt = rebuildLocalCacheFromProvider()
            if (!cacheRebuilt) {
                Logger.e(
                    "ContactRepository",
                    "standardizeAllFormatIssues cache rebuild failed successCount=$successCount total=$total"
                )
                emit(CleanupStatus.Error("Standardized $successCount contacts but failed to refresh local cache"))
                return@flow
            }
        } else {
            updateScanResultSummary()
        }

        val remainingCount = contactDao.countFormatIssues()
        val finalStatus = FormatStandardizationCompletion.status(
            total = total,
            remainingCount = remainingCount
        )

        when (finalStatus) {
            is CleanupStatus.Success -> Logger.i(
                "ContactRepository",
                "standardizeAllFormatIssues completed successCount=$successCount total=$total remaining=$remainingCount message=${finalStatus.message}"
            )
            is CleanupStatus.Error -> Logger.e(
                "ContactRepository",
                "standardizeAllFormatIssues failed successCount=$successCount total=$total remaining=$remainingCount"
            )
            is CleanupStatus.Partial -> Logger.w(
                "ContactRepository",
                "standardizeAllFormatIssues partial successCount=$successCount total=$total remaining=$remainingCount message=${finalStatus.message}"
            )
            is CleanupStatus.Progress -> Unit
        }

        emit(finalStatus)
    }

    override suspend fun getContactsAllSnapshot(): List<Contact> {
        return contactDao.getAllContacts().map { it.toDomain() }
    }

    override suspend fun getContactsSnapshotByIds(ids: List<Long>): List<Contact> {
        if (ids.isEmpty()) return emptyList()
        return contactDao.getContactsByIds(ids).map { it.toDomain() }
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
            ContactType.JUNK_FANCY_FONT -> contactDao.getFancyFontNameContactsSnapshot()
            // V3
            ContactType.ACCOUNT -> contactDao.getAllContacts() // Default fallback for accounts
            else -> contactDao.getAllContacts()
        }

        // ⚡ Bolt Optimization: Replace multiple passes (.map + .sortedBy) with single pass and in-place sort
        val domainContacts = ArrayList<Contact>(entities.size)
        for (i in entities.indices) {
            domainContacts.add(entities[i].toDomain())
        }
        domainContacts.sortBy { it.name ?: "" }
        return domainContacts
    }

    override suspend fun updateScanResultSummary() {
        Logger.d("ContactRepository", "Updating ScanResult Summary from DB (optimized single query)...")

        // 2026 Best Practice: Use consolidated getScanStats() query instead of 23 separate queries
        val stats = contactDao.getScanStats()

        if (stats.total == 0) {
            scanResultProvider.scanResult = null
            return
        }

        val result = ScanResult(
            total = stats.total,
            rawCount = usageRepository.rawScannedCount.first(),
            whatsAppCount = stats.whatsAppCount,
            telegramCount = stats.telegramCount,
            nonWhatsAppCount = stats.total - stats.whatsAppCount,
            junkCount = stats.junkCount,
            duplicateCount = stats.duplicateCount,
            noNameCount = stats.noNameCount,
            noNumberCount = stats.noNumberCount,
            emailDuplicateCount = stats.duplicateEmailCount,
            numberDuplicateCount = stats.duplicateNumberCount,
            nameDuplicateCount = stats.duplicateNameCount,
            accountCount = stats.accountCount,
            similarNameCount = stats.similarNameCount,
            invalidCharCount = stats.invalidCharCount,
            longNumberCount = stats.longNumberCount,
            shortNumberCount = stats.shortNumberCount,
            repetitiveNumberCount = stats.repetitiveNumberCount,
            symbolNameCount = stats.symbolNameCount,
            numericalNameCount = stats.numericalNameCount,
            emojiNameCount = stats.emojiNameCount,
            fancyFontCount = stats.fancyFontCount,
            formatIssueCount = stats.formatIssueCount,
            sensitiveCount = stats.sensitiveCount,
            crossAccountDuplicateCount = stats.crossAccountCount
        )
        scanResultProvider.scanResult = result
    }

    private suspend fun performProviderMerge(contactIds: List<Long>, customName: String? = null): Boolean {
        return contactsProviderSource.mergeContacts(contactIds, customName)
    }

    private suspend fun rebuildLocalCacheFromProvider(): Boolean {
        try {
            var sawSuccess = false
            var sawError = false

            scanContacts().collect { status ->
                when (status) {
                    is ScanStatus.Success -> sawSuccess = true
                    is ScanStatus.Error -> {
                        sawError = true
                        Logger.e("ContactRepository", "Failed to refresh local cache after provider write: ${status.message}")
                    }
                    else -> Unit
                }
            }

            return sawSuccess && !sawError
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ContactRepository", "Failed to refresh local cache after provider write", e)
            updateScanResultSummary()
            return false
        }
    }

    private suspend fun rebuildDuplicateMetadataFromLocalCache(): Boolean {
        return try {
            val rebuiltContacts = ContactDuplicateMetadataResolver.apply(
                contactDao.getAllContacts(),
                duplicateDetector
            )
            contactDao.replaceAllContacts(rebuiltContacts)
            updateScanResultSummary()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ContactRepository", "Failed to rebuild duplicate metadata after delete", e)
            rebuildLocalCacheFromProvider()
        }
    }

    override suspend fun recalculateWhatsAppCounts() {
        reapplyCachedWhatsAppFlags()
        updateScanResultSummary()
    }

    private suspend fun reapplyCachedWhatsAppFlags(): Int {
        val cachedWhatsAppResults = getValidCachedWhatsAppResults()
        if (cachedWhatsAppResults.isEmpty()) return 0

        var updatedCount = 0
        val batchSize = 500
        val totalContacts = contactDao.countTotal()
        var offset = 0

        while (offset < totalContacts) {
            val batch = contactDao.getContactsBatch(batchSize, offset)
            if (batch.isEmpty()) break

            val updatedBatch = batch.map { contact ->
                applyCachedWhatsAppResult(contact, cachedWhatsAppResults)
            }.filterIndexed { index, updated -> updated.isWhatsApp != batch[index].isWhatsApp }

            if (updatedBatch.isNotEmpty()) {
                contactDao.insertContacts(updatedBatch)
                updatedCount += updatedBatch.size
            }

            offset += batchSize
        }

        return updatedCount
    }

    override suspend fun getUniquePhoneNumbersForWhatsAppAccuracy(): List<String> {
        val numbers = LinkedHashSet<String>()
        val batchSize = 1000
        val totalContacts = contactDao.countTotal()
        var offset = 0

        while (offset < totalContacts) {
            val batch = contactDao.getContactsBatch(batchSize, offset)
            if (batch.isEmpty()) break
            numbers.addAll(WhatsAppAccuracyMatcher.extractUniquePhoneNumbers(batch))
            offset += batchSize
        }

        return numbers.toList()
    }

    override suspend fun applyWhatsAppAccuracyResults(results: Map<String, Boolean>): Int {
        val normalizedResults = WhatsAppAccuracyMatcher.normalizeResults(results)
        if (normalizedResults.isEmpty()) return 0

        var updatedCount = 0
        val batchSize = 500
        val totalContacts = contactDao.countTotal()
        var offset = 0

        while (offset < totalContacts) {
            val batch = contactDao.getContactsBatch(batchSize, offset)
            if (batch.isEmpty()) break

            val updatedBatch = WhatsAppAccuracyMatcher
                .applyResults(batch, normalizedResults)
                .filterIndexed { index, updated -> updated.isWhatsApp != batch[index].isWhatsApp }

            if (updatedBatch.isNotEmpty()) {
                contactDao.insertContacts(updatedBatch)
                updatedCount += updatedBatch.size
            }

            offset += batchSize
        }

        updateScanResultSummary()
        return updatedCount
    }

    override suspend fun clearWhatsAppFlags() {
        // Restore Android's native account-based WhatsApp flags after cloud accuracy is disconnected.
        rebuildLocalCacheFromProvider()
    }

    override suspend fun restoreContacts(contacts: List<Contact>): Boolean {
        val success = contactsProviderSource.restoreContacts(contacts)
        if (success) {
            return rebuildLocalCacheFromProvider()
        }
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
        return rebuildLocalCacheFromProvider()
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

        // ⚡ Bolt Optimization: Replace multiple passes (.groupBy.filter.mapNotNull.sortedBy)
        // with a single-pass loop into a LinkedHashMap to eliminate intermediate allocations.
        val groups = LinkedHashMap<String, MutableList<com.ogabassey.contactscleaner.data.db.entity.LocalContact>>()
        for (instance in allInstances) {
            val key = instance.matchingKey
            if (!key.isNullOrBlank()) {
                groups.getOrPut(key) { ArrayList() }.add(instance)
            }
        }

        val result = ArrayList<CrossAccountContact>(groups.size)
        for ((key, instances) in groups) {
            if (instances.isNotEmpty()) {
                val first = instances.first()

                val accounts = ArrayList<AccountInstance>(instances.size)
                for (instance in instances) {
                    accounts.add(
                        AccountInstance(
                            contactId = instance.id,
                            accountType = instance.accountType,
                            accountName = instance.accountName,
                            displayLabel = getAccountDisplayLabel(instance.accountType, instance.accountName)
                        )
                    )
                }

                result.add(
                    CrossAccountContact(
                        name = first.displayName,
                        matchingKey = key,
                        primaryNumber = first.rawNumbers.firstNonBlankSegment(','),
                        primaryEmail = first.rawEmails.firstNonBlankSegment(','),
                        accounts = accounts
                    )
                )
            }
        }

        result.sortBy { it.name ?: "" }
        return result
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
        val idsToDelete = ArrayList<Long>(instances.size)
        val contactsToDelete = ArrayList<Contact>(instances.size)

        for (instance in instances) {
            if (instance.accountType != keepAccountType || instance.accountName != keepAccountName) {
                idsToDelete.add(instance.id)
                contactsToDelete.add(instance.toDomain())
            }
        }

        if (idsToDelete.isEmpty()) return false

        // Delete from device
        val success = deleteContactsByIds(idsToDelete)
        if (success) {
            recordBackupSafely(
                contacts = contactsToDelete,
                actionType = "CONSOLIDATE",
                description = "Consolidated contact to ${getAccountDisplayLabel(keepAccountType)} ($keepAccountName)"
            )
            updateScanResultSummary()
        }
        return success
    }

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

        when {
            successCount == matchingKeys.size -> emit(CleanupStatus.Success("Consolidated $successCount contacts successfully"))
            successCount > 0 -> emit(CleanupStatus.Partial("Consolidated $successCount of ${matchingKeys.size} contacts"))
            else -> emit(CleanupStatus.Error("Failed to consolidate contacts"))
        }
    }

    private data class FormatStandardizationResult(
        val updatedIds: List<Long>,
        val attemptedCount: Int
    )

    private suspend fun standardizeFormatBatch(ids: List<Long>): FormatStandardizationResult {
        if (ids.isEmpty()) return FormatStandardizationResult(updatedIds = emptyList(), attemptedCount = 0)

        val contacts = contactDao.getFormatIssueContactsByIds(ids)
        if (contacts.isEmpty()) return FormatStandardizationResult(updatedIds = emptyList(), attemptedCount = 0)

        val updatedIds = contactsProviderSource.normalizeContactNumbers(contacts.map { it.id }) { rawNumber, providerNormalizedNumber ->
            FormatStandardizationTarget.resolve(
                rawNumber = rawNumber,
                providerNormalizedNumber = providerNormalizedNumber
            ) { candidate ->
                formatDetector.analyze(candidate)?.normalizedNumber
            }
        }

        return FormatStandardizationResult(
            updatedIds = updatedIds.toList(),
            attemptedCount = contacts.size
        )
    }

    override suspend fun refreshContacts(contacts: List<Contact>): Boolean {
        if (contacts.isEmpty()) return true

        try {
            val ids = contacts.map { it.id }
            val whatsAppIds = contactsProviderSource.getWhatsAppContactIds()
            val telegramIds = contactsProviderSource.getTelegramContactIds()

            // 1. Fetch fresh data from provider
            val freshContacts = contactsProviderSource.getContactsSnapshot(ids, whatsAppIds, telegramIds)

            // 2. Process contacts using extracted helper (2026 Best Practice: DRY)
            val ignoredIdsStrings = ignoredContactDao.getAllIds()
            val ignoredIds = HashSet<Long>(ignoredIdsStrings.size)
            for (i in ignoredIdsStrings.indices) {
                val parsed = ignoredIdsStrings[i].toLongOrNull()
                if (parsed != null) ignoredIds.add(parsed)
            }
            val cachedWhatsAppResults = getValidCachedWhatsAppResults()

            // 3. Update DB
            // First check if any contacts were NOT returned (deleted externally)
            val returnedIds = HashSet<Long>(freshContacts.size)
            val validatedEntities = ArrayList<LocalContact>(freshContacts.size)
            for (i in freshContacts.indices) {
                val contact = applyCachedWhatsAppResult(
                    contact = processContactToEntity(freshContacts[i], ignoredIds),
                    cachedWhatsAppResults = cachedWhatsAppResults
                )
                returnedIds.add(contact.id)

                val isValid = contact.id > 0 &&
                    (contact.displayName?.length ?: 0) <= 1000 &&
                    contact.rawNumbers.length <= 10000 &&
                    contact.rawEmails.length <= 10000
                if (isValid) {
                    validatedEntities.add(contact)
                } else {
                    Logger.w("ContactRepository", "Filtered invalid refreshed contact: id=${contact.id}")
                }
            }

            val idsSet = HashSet<Long>(ids)
            val existingContacts = contactDao.getAllContacts()
            val retainedContacts = ArrayList<LocalContact>(existingContacts.size)
            for (i in existingContacts.indices) {
                val contact = existingContacts[i]
                if (contact.id !in returnedIds && contact.id !in idsSet) {
                    retainedContacts.add(contact)
                }
            }
            val rebuiltContacts = ContactDuplicateMetadataResolver.apply(retainedContacts + validatedEntities, duplicateDetector)
            contactDao.replaceAllContacts(rebuiltContacts)

            // 4. Update Summary
            updateScanResultSummary()

            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ContactRepository", "Failed to refresh contacts", e)
            return false
        }
    }

    private fun getAccountDisplayLabel(accountType: String?, accountName: String? = null): String {
        return when {
            accountType == null || accountType.isEmpty() -> "Local"
            accountType.contains("google", ignoreCase = true) -> {
                // Show actual email address for Gmail accounts to distinguish between multiple accounts
                accountName?.takeIf { it.contains("@") } ?: "Gmail"
            }
            accountType.contains("icloud", ignoreCase = true) -> "iCloud"
            accountType.contains("exchange", ignoreCase = true) -> "Exchange"
            accountType.contains("yahoo", ignoreCase = true) -> "Yahoo"
            accountType.contains("outlook", ignoreCase = true) -> "Outlook"
            else -> accountType.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
}
