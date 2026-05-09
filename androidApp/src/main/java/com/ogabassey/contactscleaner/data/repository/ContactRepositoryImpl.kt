package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.platform.Logger

import androidx.paging.PagingData
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
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
    private val backupRepository: com.ogabassey.contactscleaner.domain.repository.BackupRepository
) : ContactRepository {

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
        ignoredIds: Set<String>
    ): LocalContact {
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
        val ignoredIds = ignoredContactDao.getAllIds().toSet()
        var processedCount = 0

        contactsProviderSource.getContactsStreaming(batchSize = 2500)
            .collect { batchContacts ->
                // 2026 Best Practice: Use extracted helper to process contacts
                val entities = batchContacts.map { contact ->
                    processContactToEntity(contact, ignoredIds)
                }

                allEntities.addAll(entities)
                processedCount += batchContacts.size

                val syncProgress = 0.05f + (processedCount.toFloat() / totalToProcess.toFloat()) * 0.70f
                emit(ScanStatus.Progress(syncProgress.coerceAtMost(0.75f), "Processing contacts (${processedCount.formatWithCommas()})..."))
            }

        // 3.5 Validate Data Before Analysis
        val validatedEntities = allEntities.filter { contact ->
            val isValid = contact.id > 0 &&
                (contact.displayName?.length ?: 0) <= 1000 && // Prevent excessively long names
                contact.rawNumbers.length <= 10000 && // Reasonable limit for multiple numbers
                contact.rawEmails.length <= 10000
            if (!isValid) {
                Logger.w("ContactRepository", "Filtered invalid contact: id=${contact.id}")
            }
            isValid
        }

        // 4. In-Memory Duplicate Detection (⚡ Bolt Optimization: Combine with initial insert)
        emit(ScanStatus.Progress(0.76f, "Analyzing duplicates..."))

        val finalEntities = ContactDuplicateMetadataResolver.apply(validatedEntities, duplicateDetector)

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
            // Record for history/undo before deletion
            if (contacts.isNotEmpty()) {
                backupRepository.performBackup(
                    contacts = contacts,
                    actionType = "DELETE",
                    description = "Deleted ${contacts.size} contact${if (contacts.size > 1) "s" else ""}"
                )
            }

            val ids = contacts.map { it.id }
            if (deleteContactsFromProviderAndSync(ids)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete contacts"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContactsByIds(contactIds: List<Long>): Boolean {
        return deleteContactsFromProviderAndSync(contactIds)
    }

    private suspend fun deleteContactsFromProviderAndSync(contactIds: List<Long>): Boolean {
        if (contactIds.isEmpty()) return true

        val requiresDuplicateRebuild = contactDao.getContactsByIds(contactIds).any { it.duplicateType != null }
        val providerSuccess = contactsProviderSource.deleteContacts(contactIds)
        if (!providerSuccess) {
            Logger.e(
                "ContactRepository",
                "Provider delete failed for ${contactIds.size} contacts; skipping local cache delete"
            )
            return false
        }

        return try {
            contactDao.deleteContacts(contactIds)
            if (requiresDuplicateRebuild) {
                rebuildDuplicateMetadataFromLocalCache()
            } else {
                updateScanResultSummary()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("ContactRepository", "Failed to cascade delete to local cache", e)
            rebuildLocalCacheFromProvider()
        }
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
                if (performProviderMerge(ids)) {
                    successCount++
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

        when {
            successCount == groups.size -> emit(CleanupStatus.Success("Merged $successCount groups successfully"))
            successCount > 0 -> emit(CleanupStatus.Partial("Merged $successCount of ${groups.size} groups"))
            else -> emit(CleanupStatus.Error("Failed to merge duplicate groups"))
        }
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

        // Pre-fetch contact names for streaming display
        val contactEntities = contactDao.getFormatIssueContactsByIds(ids)
        val contactNames = contactEntities.associate { it.id to (it.displayName ?: "Unknown") }

        // Record for history
        val formatIssues = contactEntities.map { it.toDomain() }
        backupRepository.performBackup(
            contacts = formatIssues,
            actionType = "FORMAT",
            description = "Standardized ${formatIssues.size} numbers"
        )

        // Track recent items for streaming log
        val recentItems = mutableListOf<String>()

        // 2026 Optimization: Smaller batches (50) for 10x faster visual feedback
        // Previously used 500 which caused "stuck at 0%" perception
        ids.chunked(50).forEach { batch ->
            val batchResult = standardizeFormatBatch(batch)
            if (batchResult.updatedIds.isNotEmpty()) {
                successCount += batchResult.updatedIds.size

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
        }

        if (successCount > 0) {
            val cacheRebuilt = rebuildLocalCacheFromProvider()
            if (!cacheRebuilt) {
                emit(CleanupStatus.Error("Standardized $successCount contacts but failed to refresh local cache"))
                return@flow
            }
        } else {
            updateScanResultSummary()
        }

        when {
            successCount == total -> emit(CleanupStatus.Success("Standardized $successCount contacts successfully"))
            successCount > 0 -> emit(CleanupStatus.Partial("Standardized $successCount of $total contacts"))
            else -> emit(CleanupStatus.Error("Failed to standardize contacts"))
        }
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
        return entities.map { it.toDomain() }.sortedBy { it.name ?: "" }
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
        // Android uses native WhatsApp detection via account_type, no VPS cache needed.
        // Just refresh the summary which already has correct counts.
        updateScanResultSummary()
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

        // Record for backup
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

        val updatedIds = contactsProviderSource.normalizeContactNumbers(contacts.map { it.id }) { rawNumber ->
            val firstChar = rawNumber.firstOrNull()
            val hasBlockedPrefix = firstChar == '+' || firstChar == '*' || firstChar == '#'
            if (rawNumber.isBlank() || hasBlockedPrefix) {
                null
            } else {
                formatDetector.analyze(rawNumber)?.normalizedNumber
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
            val ignoredIds = ignoredContactDao.getAllIds().toSet()
            val refreshedEntities = freshContacts.map { contact ->
                processContactToEntity(contact, ignoredIds)
            }

            // 3. Update DB
            // First check if any contacts were NOT returned (deleted externally)
            val returnedIds = refreshedEntities.map { it.id }.toSet()
            val idsSet = ids.toSet()
            val deletedIds = ids.filter { it !in returnedIds }
            val existingContacts = contactDao.getAllContacts()
            val retainedContacts = ArrayList<com.ogabassey.contactscleaner.data.db.entity.LocalContact>(existingContacts.size)
            for (i in existingContacts.indices) {
                val contact = existingContacts[i]
                if (contact.id !in returnedIds && contact.id !in idsSet) {
                    retainedContacts.add(contact)
                }
            }
            val validatedEntities = refreshedEntities.filter { contact ->
                val isValid = contact.id > 0 &&
                    (contact.displayName?.length ?: 0) <= 1000 &&
                    contact.rawNumbers.length <= 10000 &&
                    contact.rawEmails.length <= 10000
                if (!isValid) {
                    Logger.w("ContactRepository", "Filtered invalid refreshed contact: id=${contact.id}")
                }
                isValid
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
