
package com.ogabassey.contactscleaner.data.repository
import com.ogabassey.contactscleaner.util.extractDigits
import com.ogabassey.contactscleaner.util.firstNonBlankSegment
import com.ogabassey.contactscleaner.util.splitAndFilterNotBlank


import com.ogabassey.contactscleaner.platform.Logger

import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao
import com.ogabassey.contactscleaner.data.db.dao.ScanStats
import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.data.detector.DuplicateDetector
import com.ogabassey.contactscleaner.data.detector.FormatDetector
import com.ogabassey.contactscleaner.data.detector.JunkDetector
import com.ogabassey.contactscleaner.data.detector.SensitiveDataDetector
import com.ogabassey.contactscleaner.data.source.IosContactsSource
import com.ogabassey.contactscleaner.data.util.ScanResultProvider
import com.ogabassey.contactscleaner.domain.model.*
import com.ogabassey.contactscleaner.domain.repository.BackupRepository
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import com.ogabassey.contactscleaner.domain.repository.UsageRepository
import com.ogabassey.contactscleaner.domain.repository.CacheSnapshot
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import com.ogabassey.contactscleaner.util.formatWithCommas


/**
 * iOS ContactRepository implementation using CNContactStore.
 *
 * 2026 KMP Best Practice: Platform-specific repository implementation.
 */
class IosContactRepository(
    private val contactDao: ContactDao,
    private val contactsSource: IosContactsSource,
    private val junkDetector: JunkDetector,
    private val duplicateDetector: DuplicateDetector,
    private val formatDetector: FormatDetector,
    private val sensitiveDetector: SensitiveDataDetector,
    private val ignoredContactDao: IgnoredContactDao,
    private val scanResultProvider: ScanResultProvider,
    private val usageRepository: UsageRepository,
    private val backupRepository: BackupRepository,
    private val whatsAppRepository: WhatsAppDetectorRepository? = null,
    private val settings: Settings? = null
) : ContactRepository {

    companion object {
        private const val KEY_DEVICE_ID = "whatsapp_device_id"

        /** Returns true if the number is a candidate for format normalization. */
        fun isNormalizable(number: String): Boolean {
            // ⚡ Bolt Optimization: Use primitive Char checks instead of startsWith to avoid allocations
            if (number.isBlank()) return false
            val trimmed = number.trim()
            if (trimmed.length < 7) return false
            val firstChar = trimmed[0]
            if (firstChar == '+' || firstChar == '*' || firstChar == '#') return false
            if (firstChar == '0' && trimmed.length > 1 && trimmed[1] == '0') return false
            return true
        }
    }

    /**
     * 2026 Best Practice: Extract shared contact processing logic.
     * Processes a single contact through all detectors and builds a LocalContact entity.
     * Used by both scanContacts() and refreshContacts() to eliminate code duplication.
     *
     * @param contact The contact to process
     * @param ignoredIds Set of contact IDs that should skip sensitive/junk detection
     * @param whatsAppPhoneNumbers Set of WhatsApp phone numbers for matching
     * @return LocalContact entity ready for database insertion
     */
    private fun processContactToEntity(
        contact: Contact,
        ignoredIds: Set<Long>,
        whatsAppPhoneNumbers: Set<String> = emptySet()
    ): LocalContact {
        val primaryNumber = contact.numbers.firstOrNull() ?: ""
        val isIgnored = ignoredIds.contains(contact.id)

        // Sensitive detection
        var isSensitive = false
        var sensitiveDesc: String? = null

        if (!isIgnored) {
            // 1. Scan Name
            contact.name?.let { name ->
                sensitiveDetector.analyze(name)?.let {
                    isSensitive = true
                    sensitiveDesc = it.description
                }
            }

            // 2. Scan All Numbers (if not already found sensitive in name)
            if (!isSensitive) {
                contact.numbers.forEach { num ->
                    if (!isSensitive) {
                        sensitiveDetector.analyze(num)?.let {
                            isSensitive = true
                            sensitiveDesc = it.description
                        }
                    }
                }
            }
        }

        // Junk detection
        val junkType = if (!isIgnored && !isSensitive) {
            junkDetector.getJunkType(contact.name, contact.normalizedNumber ?: primaryNumber)
        } else null

        // Format issue detection — exit on first normalizable issue found
        var isFormatIssue = false
        var detectedNormalized = contact.normalizedNumber

        if (junkType == null && !isSensitive) {
            for (number in contact.numbers) {
                if (isNormalizable(number)) {
                    formatDetector.analyze(number)?.let { issue ->
                        isFormatIssue = true
                        detectedNormalized = issue.normalizedNumber
                    }
                }
                if (isFormatIssue) break
            }
        }

        // Check if contact is on WhatsApp by comparing normalized numbers
        val isOnWhatsApp = if (whatsAppPhoneNumbers.isNotEmpty()) {
            contact.numbers.any { num ->
                val normalized = num.extractDigits()
                whatsAppPhoneNumbers.contains(normalized)
            }
        } else {
            contact.isWhatsApp
        }

        return LocalContact(
            id = contact.id,
            displayName = contact.name,
            normalizedNumber = detectedNormalized,
            rawNumbers = contact.numbers.joinToString(","),
            rawEmails = contact.emails.joinToString(","),
            isWhatsApp = isOnWhatsApp,
            isTelegram = contact.isTelegram,
            accountType = contact.accountType,
            accountName = contact.accountName,
            isJunk = junkType != null && !isSensitive,
            junkType = junkType?.name,
            duplicateType = null,
            isFormatIssue = isFormatIssue,
            detectedRegion = if (isFormatIssue && detectedNormalized != null) formatDetector.getRegionCode(detectedNormalized) else null,
            isSensitive = isSensitive,
            sensitiveDescription = sensitiveDesc,
            matchingKey = detectedNormalized ?: contact.emails.firstOrNull() ?: contact.name,
            platformUid = contact.platform_uid,
            lastSynced = Clock.System.now().toEpochMilliseconds()
        )
    }

    override suspend fun scanContacts(): Flow<ScanStatus> = flow {
        emit(ScanStatus.Progress(0.05f, "Loading contacts from device..."))

        // 2026 Best Practice: Don't delete early - use atomic replace at end
        emit(ScanStatus.Progress(0.10f, "Initializing scan..."))

        // 2. Fetch all contacts from iOS
        val contacts = contactsSource.getAllContacts()
        val total = contacts.size

        if (total == 0) {
            emit(ScanStatus.Success(ScanResult()))
            return@flow
        }

        emit(ScanStatus.Progress(0.15f, "Analyzing ${total.formatWithCommas()} contacts..."))
        usageRepository.updateRawScannedCount(total)

        // 3. Load cached WhatsApp numbers for comparison (2026 Best Practice: Use atomic cache snapshot)
        var whatsAppPhoneNumbers = emptySet<String>()
        val deviceId = settings?.getStringOrNull(KEY_DEVICE_ID)
        if (deviceId != null && whatsAppRepository != null) {
            try {
                emit(ScanStatus.Progress(0.18f, "Loading WhatsApp cache..."))

                // 2026 Best Practice: Use atomic snapshot to prevent race condition
                // between validity check and data retrieval
                when (val snapshot = whatsAppRepository.getValidCacheSnapshot()) {
                    is CacheSnapshot.Valid -> {
                        whatsAppPhoneNumbers = snapshot.numbers
                        Logger.d("Logger", "📱 Using cached WhatsApp numbers: ${snapshot.numbers.size} (business: ${snapshot.businessCount})")
                    }
                    is CacheSnapshot.SyncInProgress -> {
                        Logger.d("Logger", "⏳ WhatsApp cache sync in progress, using empty set for now")
                    }
                    is CacheSnapshot.Invalid -> {
                        // Cache not valid - check if session is connected
                        val status = whatsAppRepository.getSessionStatus(deviceId)
                        if (status.connected) {
                            // Session connected but cache empty/stale - user should trigger sync
                            Logger.e("Logger", "⚠️ WhatsApp cache empty/stale. Please sync WhatsApp contacts.")
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 2026 Best Practice: Always rethrow CancellationException for proper flow cancellation
                throw e
            } catch (e: Exception) {
                Logger.e("Logger", "⚠️ Could not load WhatsApp cache: ${e.message}")
            }
        }
        emit(ScanStatus.Progress(0.20f, "Processing contacts..."))

        // 4. Get ignored contacts
        val ignoredIdsStrings = ignoredContactDao.getAllIds()
        val ignoredIds = HashSet<Long>(ignoredIdsStrings.size)
        for (i in ignoredIdsStrings.indices) {
            val parsed = ignoredIdsStrings[i].toLongOrNull()
            if (parsed != null) ignoredIds.add(parsed)
        }

        // 5. Process each contact - 2026 Best Practice: Use extracted helper for consistency
        val validatedContacts = withContext(Dispatchers.Default) {
            val resultList = ArrayList<LocalContact>(contacts.size)
            for (i in contacts.indices) {
                val entity = processContactToEntity(contacts[i], ignoredIds, whatsAppPhoneNumbers)
                val isValid = entity.id > 0 &&
                    (entity.displayName?.length ?: 0) <= 1000 && // Prevent excessively long names
                    entity.rawNumbers.length <= 10000 && // Reasonable limit for multiple numbers
                    entity.rawEmails.length <= 10000
                if (!isValid) {
                    Logger.e("Logger", "⚠️ Filtered invalid contact: id=${entity.id}")
                } else {
                    resultList.add(entity)
                }
            }
            resultList
        }

        // 5. Atomic replace: Delete old + Insert new in single transaction
        // 2026 Best Practice: Prevents data loss if operation fails
        emit(ScanStatus.Progress(0.70f, "Saving contacts to database..."))

        emit(ScanStatus.Progress(0.80f, "Analyzing duplicates..."))
        val finalContacts = withContext(Dispatchers.Default) {
            ContactDuplicateMetadataResolver.apply(validatedContacts, duplicateDetector)
        }

        contactDao.replaceAllContacts(finalContacts)
        emit(ScanStatus.Progress(0.85f, "Contacts saved."))

        emit(ScanStatus.Progress(0.90f, "Calculating result statistics..."))

        // 7. Fetch all counts from database using consolidated query (2026 Performance Optimization)
        val stats = contactDao.getScanStats()

        // 8. Update scan result provider with all counts
        // 2026 Best Practice: Use stats.total from DB for consistency with other DB-derived counts
        val result = ScanResult(
            total = stats.total,
            rawCount = total,  // Keep raw device count for reference
            whatsAppCount = stats.whatsAppCount,
            telegramCount = stats.telegramCount,
            junkCount = stats.junkCount,
            duplicateCount = stats.duplicateCount,
            formatIssueCount = stats.formatIssueCount,
            sensitiveCount = stats.sensitiveCount,
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
            nonWhatsAppCount = stats.total - stats.whatsAppCount,
            crossAccountDuplicateCount = stats.crossAccountCount
        )
        scanResultProvider.scanResult = result

        emit(ScanStatus.Success(result))
    }

    override suspend fun deleteContacts(contacts: List<Contact>): Result<Unit> {
        return try {
            Logger.d("Logger", "Deleting ${contacts.size} contacts")

            // Separate contacts with and without platform_uid
            val (withUid, withoutUid) = contacts.partition { it.platform_uid != null }
            Logger.d("Logger", "With UID: ${withUid.size}, Without UID: ${withoutUid.size}")

            // Record for history/undo before deletion
            if (contacts.isNotEmpty()) {
                backupRepository.performBackup(
                    contacts = contacts,
                    actionType = "DELETE",
                    description = "Deleted ${contacts.size} contact${if (contacts.size > 1) "s" else ""}"
                )
            }

            // Delete from device (only contacts with platform_uid)
            val uids = withUid.mapNotNull { it.platform_uid }
            Logger.d("IosContactRepository", "🗑️ [DELETE] Deleting ${uids.size} contacts from device")
            if (uids.isNotEmpty()) {
                // 2026 Best Practice: Check device deletion result to ensure consistency
                val deviceDeleted = contactsSource.deleteContacts(uids)
                Logger.d("Logger", "🗑️ [DELETE] Device deletion result: $deviceDeleted")
                if (!deviceDeleted) {
                    Logger.d("Logger", "🗑️ [DELETE] Device deletion FAILED!")
                    return Result.failure(IllegalStateException("Failed to delete contacts from device"))
                }
            }

            // Delete from DB - all contacts (those without uid are DB-only entries)
            contactDao.deleteContacts(contacts.map { it.id })

            // Log warning if some contacts lacked platform_uid
            if (withoutUid.isNotEmpty()) {
                Logger.e("Logger", "Warning: ${withoutUid.size} contacts lacked platform_uid (DB-only deletion)")
            }

            // Update scan result summary to reflect changes
            updateScanResultSummary()

            Result.success(Unit)
        } catch (e: CancellationException) {
            // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContactsByIds(contactIds: List<Long>): Boolean {
        // Fallback for Long-based deletions (Android style)
        val contacts = contactDao.getContactsByIds(contactIds).map { it.toContact() }
        var deviceDeleteSuccess = true

        try {
            val uids = contacts.mapNotNull { it.platform_uid }
            if (uids.isNotEmpty()) {
                // 2026 Best Practice: Check device deletion result
                deviceDeleteSuccess = contactsSource.deleteContacts(uids)
                if (!deviceDeleteSuccess) {
                    Logger.e("Logger", "Warning: Device delete returned false")
                }
            }
        } catch (e: CancellationException) {
            // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
            throw e
        } catch (e: Exception) {
            Logger.e("Logger", "Warning: Device delete failed: ${e.message}")
            deviceDeleteSuccess = false
        }

        // 2026 Best Practice: Always cascade delete to local cache
        // Even if device delete fails, clean local cache to avoid stale data
        try {
            contactDao.deleteContacts(contactIds)
        } catch (e: CancellationException) {
            // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
            throw e
        } catch (e: Exception) {
            Logger.e("Logger", "Error: Failed to cascade delete to local cache: ${e.message}")
            return false
        }

        return deviceDeleteSuccess
    }

    override suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = flow {
        emit(CleanupStatus.Progress(0f, "Finding contacts to delete..."))

        val contacts = getContactsSnapshotByType(type)
        if (contacts.isEmpty()) {
            emit(CleanupStatus.Success("No contacts to delete"))
            return@flow
        }

        emit(CleanupStatus.Progress(0.3f, "Deleting ${contacts.size} contacts..."))

        val ids = contacts.map { it.id }
        
        backupRepository.performBackup(
            contacts = contacts,
            actionType = "DELETE",
            description = "Deleted ${contacts.size} contacts from $type"
        )
        val success = deleteContactsByIds(ids)
        updateScanResultSummary()

        if (success) {
            emit(CleanupStatus.Success("Deleted ${contacts.size} contacts"))
        } else {
            emit(CleanupStatus.Error("Failed to delete contacts"))
        }
    }

    override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean {
        val success = performProviderMerge(contactIds, customName)
        if (success) {
            rebuildLocalCacheFromProvider()
        }
        return success
    }

    override suspend fun saveContacts(contacts: List<Contact>): Boolean {
        val success = contactsSource.restoreContacts(contacts)
        if (success) {
            rebuildLocalCacheFromProvider()
        }
        return success
    }

    override suspend fun getDuplicateGroups(type: ContactType): List<DuplicateGroupSummary> {
        return when (type) {
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberGroups()
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailGroups()
            ContactType.DUP_NAME -> contactDao.getDuplicateNameGroups()
            ContactType.DUP_SIMILAR_NAME -> contactDao.getSimilarNameGroups()
            else -> emptyList()
        }
    }

    override suspend fun getAccountGroups(): List<AccountGroupSummary> {
        return contactDao.getAccountGroups()
    }

    override suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact> {
        return when (type) {
            ContactType.DUP_NUMBER -> contactDao.getContactsByNumberKey(key)
            ContactType.DUP_EMAIL -> contactDao.getContactsByEmailKey(key)
            ContactType.DUP_NAME -> contactDao.getContactsByNameKey(key)
            ContactType.DUP_SIMILAR_NAME -> contactDao.getContactsBySimilarNameKey(key)
            else -> emptyList()
        }.map { it.toContact() }
    }

    override suspend fun mergeDuplicateGroups(type: ContactType): Flow<CleanupStatus> = flow {
        emit(CleanupStatus.Progress(0f, "Finding duplicate groups..."))

        val groups = getDuplicateGroups(type)
        if (groups.isEmpty()) {
            emit(CleanupStatus.Success("No duplicate groups to merge"))
            return@flow
        }

        var merged = 0
        groups.forEachIndexed { index, group ->
            val progress = (index.toFloat() / groups.size.toFloat())
            emit(CleanupStatus.Progress(progress, "Merging group ${index + 1}/${groups.size}..."))

            val contactsInGroup = getContactsInGroup(group.groupKey, type)
            if (contactsInGroup.size >= 2) {
                // Record for history
                backupRepository.performBackup(
                    contacts = contactsInGroup,
                    actionType = "MERGE",
                    description = "Merged ${contactsInGroup.size} duplicates (${group.groupKey})"
                )
                
                val success = performProviderMerge(contactsInGroup.map { it.id })
                if (success) merged++
            }
        }

        if (merged > 0) {
            rebuildLocalCacheFromProvider()
        } else {
            updateScanResultSummary()
        }

        emit(CleanupStatus.Success("Merged $merged duplicate groups"))
    }

    override suspend fun standardizeFormat(ids: List<Long>): Boolean {
        var success = true
        val successfulIds = mutableListOf<Long>()
        val contacts = contactDao.getFormatIssueContactsByIds(ids)

        for (entity in contacts) {
            val platformUid = entity.platformUid ?: continue  // Skip if no platform_uid

            // Normalize ALL numbers on the contact, not just the first
            val updated = contactsSource.normalizeAllNumbers(platformUid) { rawNumber ->
                if (isNormalizable(rawNumber)) formatDetector.analyze(rawNumber)?.normalizedNumber else null
            }
            if (updated) {
                successfulIds.add(entity.id)
            } else {
                success = false
            }
        }

        // Clear format issue flag for successfully updated contacts
        if (successfulIds.isNotEmpty()) {
            contactDao.clearFormatIssueFlags(successfulIds)
        }

        return success
    }

    override suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
        emit(CleanupStatus.Progress(0f, "Finding format issues..."))

        val formatIssues = contactDao.getFormatIssueContactsSnapshot()
        if (formatIssues.isEmpty()) {
            emit(CleanupStatus.Success("No format issues to fix"))
            return@flow
        }

        val total = formatIssues.size
        var successCount = 0

        // Pre-fetch contact names for streaming display
        val contactNames = formatIssues.associate { it.id to (it.displayName ?: "Unknown") }

        // Record for history
        val contactsSnapshot = formatIssues.map { it.toContact() }
        backupRepository.performBackup(
            contacts = contactsSnapshot,
            actionType = "FORMAT",
            description = "Standardized ${formatIssues.size} numbers"
        )

        // Track recent items for streaming log
        val recentItems = mutableListOf<String>()

        // 2026 Optimization: Process in batches of 25 for streaming progress
        // iOS CNContactStore is slower per-contact, so smaller batches give better feedback
        formatIssues.chunked(25).forEachIndexed { batchIndex, batch ->
            val batchIds = batch.map { it.id }

            // Process this batch — normalize ALL numbers on each contact
            val batchSuccessful = mutableListOf<Long>()
            for (entity in batch) {
                val platformUid = entity.platformUid ?: continue

                val updated = contactsSource.normalizeAllNumbers(platformUid) { rawNumber ->
                    if (isNormalizable(rawNumber)) formatDetector.analyze(rawNumber)?.normalizedNumber else null
                }
                if (updated) {
                    batchSuccessful.add(entity.id)
                    successCount++

                    // Add to recent items for streaming display
                    val name = contactNames[entity.id]
                    if (name != null) {
                        recentItems.add(0, "Updated: $name")
                        if (recentItems.size > 10) recentItems.removeLast()
                    }
                }
            }

            // Clear format issue flags for successfully updated contacts
            if (batchSuccessful.isNotEmpty()) {
                contactDao.clearFormatIssueFlags(batchSuccessful)
            }

            val progress = successCount.toFloat() / total.toFloat()
            val currentItem = batch.lastOrNull()?.let { contactNames[it.id] }

            emit(CleanupStatus.Progress(
                progress = progress.coerceAtMost(1f),
                message = "Standardizing: ${currentItem ?: "..."} [$successCount of $total]",
                details = CleanupDetails(
                    processed = successCount,
                    total = total,
                    currentItem = currentItem,
                    recentItems = recentItems.toList()
                )
            ))
        }

        // Refresh the scan result summary to update counts
        updateScanResultSummary()

        if (successCount == total) {
            emit(CleanupStatus.Success("Standardized $successCount phone numbers"))
        } else if (successCount > 0) {
            emit(CleanupStatus.Success("Standardized $successCount of $total phone numbers"))
        } else {
            emit(CleanupStatus.Error("Could not update contacts"))
        }
    }

    override suspend fun getContactsAllSnapshot(): List<Contact> {
        return contactDao.getAllContacts().map { it.toContact() }
    }

    override suspend fun getContactsSnapshotByIds(ids: List<Long>): List<Contact> {
        if (ids.isEmpty()) return emptyList()
        return contactDao.getContactsByIds(ids).map { it.toContact() }
    }

    override suspend fun getContactsSnapshotByType(type: ContactType): List<Contact> {
        val entities = when (type) {
            ContactType.ALL -> contactDao.getAllContacts()
            ContactType.JUNK -> contactDao.getJunkContactsSnapshot()
            ContactType.DUPLICATE -> contactDao.getDuplicateContactsSnapshot()
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberContactsSnapshot()
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailContactsSnapshot()
            ContactType.DUP_NAME -> contactDao.getDuplicateNameContactsSnapshot()
            ContactType.DUP_SIMILAR_NAME -> contactDao.getSimilarNameContactsSnapshot()
            ContactType.DUP_CROSS_ACCOUNT -> contactDao.getCrossAccountContactsSnapshot()
            ContactType.FORMAT_ISSUE -> contactDao.getFormatIssueContactsSnapshot()
            ContactType.SENSITIVE -> contactDao.getSensitiveContactsSnapshot()
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
            ContactType.WHATSAPP -> contactDao.getWhatsAppContactsSnapshot()
            ContactType.TELEGRAM -> contactDao.getTelegramContactsSnapshot()
            ContactType.NON_WHATSAPP -> contactDao.getNonWhatsAppContactsSnapshot()
            ContactType.ACCOUNT -> contactDao.getAllContacts()
            ContactType.JUNK_SUSPICIOUS -> contactDao.getJunkContactsSnapshot()
        }

        // ⚡ Bolt Optimization: Unify mapping operation to improve readability and DRY
        return entities.map { it.toContact() }
    }

    override suspend fun refreshContacts(contacts: List<Contact>): Boolean {
        if (contacts.isEmpty()) return true

        return try {
            val uids = contacts.mapNotNull { it.platform_uid }
            val ids = contacts.map { it.id } // DB IDs
            if (uids.isEmpty()) return false

            // 1. Build existing account metadata map to preserve during refresh
            // 2026 Fix: Preserve account info that may not be available from CNContact directly
            // ⚡ Bolt Optimization: Replace multiple passes (.filter.associate) with a single-pass loop
            val existingAccountInfo = HashMap<String, Pair<String?, String?>>(contacts.size)
            for (i in contacts.indices) {
                val contact = contacts[i]
                val platformUid = contact.platform_uid
                if (platformUid != null) {
                    existingAccountInfo[platformUid] = Pair(contact.accountType, contact.accountName)
                }
            }

            // 2. Fetch fresh data from source with preserved account info
            val freshContacts = contactsSource.getContactsByUids(uids, existingAccountInfo)

            // 3. Load cached WhatsApp numbers if available
            var whatsAppPhoneNumbers = emptySet<String>()
            if (whatsAppRepository != null && settings != null) {
                try {
                    val snapshot = whatsAppRepository.getValidCacheSnapshot()
                    if (snapshot is CacheSnapshot.Valid) {
                        whatsAppPhoneNumbers = snapshot.numbers
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.d("Logger", "Note: Could not load WhatsApp cache for refresh: ${e.message}")
                }
            }

            // 4. Process contacts using extracted helper
            val ignoredIdsStrings = ignoredContactDao.getAllIds()
            val ignoredIds = HashSet<Long>(ignoredIdsStrings.size)
            for (i in ignoredIdsStrings.indices) {
                val parsed = ignoredIdsStrings[i].toLongOrNull()
                if (parsed != null) ignoredIds.add(parsed)
            }
            val refreshedIds = HashSet<Long>(freshContacts.size)
            val validatedContacts = withContext(Dispatchers.Default) {
                val resultList = ArrayList<LocalContact>(freshContacts.size)
                for (i in freshContacts.indices) {
                    val entity = processContactToEntity(freshContacts[i], ignoredIds, whatsAppPhoneNumbers)
                    if (entity.id > 0) {
                        refreshedIds.add(entity.id)
                    }
                    val isValid = entity.id > 0 &&
                        (entity.displayName?.length ?: 0) <= 1000 &&
                        entity.rawNumbers.length <= 10000 &&
                        entity.rawEmails.length <= 10000
                    if (isValid) {
                        resultList.add(entity)
                    }
                }
                resultList
            }

            // 4. Update DB
             // IDs that were requested but returned might be missing (deleted externally)
            // Since we use UIDs to fetch, if one is missing from `freshContacts`, it implies it's gone from device.
            // But `freshContacts` returns new objects.
            
            // Map fetched UIDs
            val fetchedUids = freshContacts.mapNotNull { it.platform_uid }.toSet()
            // Identify which of certain UIDs were NOT found
            val uidsSet = uids.toSet()
            val missingDbIds = HashSet<Long>()
            for (contact in contacts) {
                val uid = contact.platform_uid
                if (uid != null && uid in uidsSet && uid !in fetchedUids) {
                    missingDbIds.add(contact.id)
                }
            }
            
            val existingContacts = contactDao.getAllContacts()
            val retainedContacts = ArrayList<LocalContact>(existingContacts.size)
            for (contact in existingContacts) {
                if (contact.id !in missingDbIds && contact.id !in refreshedIds) {
                    retainedContacts.add(contact)
                }
            }
            val rebuiltContacts = withContext(Dispatchers.Default) {
                ContactDuplicateMetadataResolver.apply(retainedContacts + validatedContacts, duplicateDetector)
            }
            contactDao.replaceAllContacts(rebuiltContacts)

            // 5. Update Summary
            updateScanResultSummary()
            true
        } catch (e: CancellationException) {
            // 2026 Best Practice: Preserve coroutine cancellation semantics
            throw e
        } catch (e: Exception) {
            Logger.e("Logger", "Error refreshing contacts: ${e.message}")
            false
        }
    }

    override suspend fun restoreContacts(contacts: List<Contact>): Boolean {
        val success = contactsSource.restoreContacts(contacts)
        if (success) {
            return rebuildLocalCacheFromProvider()
        }
        return false
    }

    override suspend fun ignoreContact(id: String, displayName: String, reason: String): Boolean {
        val ignored = IgnoredContact(
            id = id,
            displayName = displayName,
            reason = reason,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        ignoredContactDao.insert(ignored)
        // Update scan result to reflect ignored contact
        updateScanResultSummary()
        return true
    }

    override suspend fun unignoreContact(id: String): Boolean {
        ignoredContactDao.delete(id)
        return rebuildLocalCacheFromProvider()
    }

    override fun getIgnoredContacts(): Flow<List<IgnoredContact>> {
        return ignoredContactDao.getAll()
    }

    override fun getContactsFlow(type: ContactType): Flow<List<Contact>> = flow {
        emit(getContactsSnapshotByType(type))
    }

    override fun getAccountCount(): Flow<Int> = flow {
        emit(contactDao.countAccounts())
    }

    override suspend fun updateScanResultSummary() {
        // 2026 Best Practice: Use consolidated getScanStats() query instead of 23+ separate queries
        val stats = contactDao.getScanStats()
        val rawCount = usageRepository.rawScannedCount.first()

        if (stats.total == 0) {
            scanResultProvider.scanResult = null
            return
        }

        scanResultProvider.scanResult = ScanResult(
            total = stats.total,
            rawCount = rawCount,
            whatsAppCount = stats.whatsAppCount,
            telegramCount = stats.telegramCount,
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
            crossAccountDuplicateCount = stats.crossAccountCount,
            nonWhatsAppCount = stats.total - stats.whatsAppCount
        )
    }

    private suspend fun performProviderMerge(contactIds: List<Long>, customName: String? = null): Boolean {
        val contacts = contactDao.getContactsByIds(contactIds)
        val platformUids = contacts.mapNotNull { it.platformUid }

        if (platformUids.size < 2) {
            Logger.d("Logger", "Not enough contacts with platform_uid for merge: ${platformUids.size}")
            return false
        }

        return contactsSource.mergeContacts(platformUids, customName)
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
                        Logger.e("Logger", "Failed to refresh local cache after provider write: ${status.message}")
                    }
                    else -> Unit
                }
            }

            return sawSuccess && !sawError
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Logger", "Failed to refresh local cache after provider write: ${e.message}")
            updateScanResultSummary()
            return false
        }
    }

    /**
     * Recalculate WhatsApp/Non-WhatsApp counts using cached WhatsApp numbers.
     * Called after WhatsApp sync completes to update contact flags in the database.
     *
     * 2026 Best Practice: Process contacts in batches to prevent OOM on large datasets (50k+).
     */
    override suspend fun recalculateWhatsAppCounts() {
        if (whatsAppRepository == null) {
            Logger.e("Logger", "⚠️ WhatsApp repository not available for recalculation")
            return
        }

        try {
            // Get cached WhatsApp numbers
            val cachedNumbers = whatsAppRepository.getCachedNumbers()
            if (cachedNumbers.isEmpty()) {
                Logger.e("Logger", "⚠️ WhatsApp cache is empty, cannot recalculate")
                return
            }

            Logger.d("Logger", "📱 Recalculating WhatsApp flags using ${cachedNumbers.size} cached numbers...")

            // 2026 Best Practice: Process in batches to prevent OOM on 50k+ contacts
            val batchSize = 500
            val totalContacts = contactDao.countTotal()
            var offset = 0
            var totalUpdatedCount = 0

            while (offset < totalContacts) {
                // Fetch batch
                val batch = contactDao.getContactsBatch(batchSize, offset)
                if (batch.isEmpty()) break

                // Process batch
                val updatedContacts = ArrayList<LocalContact>(batch.size)
                for (i in batch.indices) {
                    val contact = batch[i]
                    val isOnWhatsApp = WhatsAppCacheMatcher.hasCachedWhatsAppNumber(
                        rawNumbers = contact.rawNumbers,
                        cachedNumbers = cachedNumbers
                    )

                    // Only update if flag changed
                    if (contact.isWhatsApp != isOnWhatsApp) {
                        updatedContacts.add(contact.copy(isWhatsApp = isOnWhatsApp))
                    }
                }

                // Update batch
                if (updatedContacts.isNotEmpty()) {
                    contactDao.insertContacts(updatedContacts)
                    totalUpdatedCount += updatedContacts.size
                }

                offset += batchSize
            }

            Logger.d("Logger", "✅ Updated WhatsApp flag for $totalUpdatedCount contacts (processed in batches of $batchSize)")

            // Refresh scan result summary
            updateScanResultSummary()
        } catch (e: CancellationException) {
            // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
            throw e
        } catch (e: Exception) {
            Logger.e("Logger", "❌ Failed to recalculate WhatsApp counts: ${e.message}")
        }
    }

    override suspend fun clearWhatsAppFlags() {
        val updatedCount = contactDao.clearWhatsAppFlags()
        Logger.d("Logger", "📱 Cleared WhatsApp flag for $updatedCount contacts after disconnect")
        updateScanResultSummary()
    }

    // --- Cross-Account Duplicates ---

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

    override suspend fun getContactInstancesByMatchingKey(matchingKey: String): List<Contact> {
        return contactDao.getContactInstancesByMatchingKey(matchingKey).map { it.toContact() }
    }

    override suspend fun consolidateContactToAccount(
        matchingKey: String,
        keepAccountType: String?,
        keepAccountName: String?
    ): Boolean {
        // Public API: always refresh summary after consolidation
        return consolidateContactToAccountInternal(matchingKey, keepAccountType, keepAccountName, refreshSummary = true)
    }

    /**
     * Internal implementation with optional summary refresh.
     * 2026 Best Practice: Avoid redundant summary refreshes in batch operations.
     */
    private suspend fun consolidateContactToAccountInternal(
        matchingKey: String,
        keepAccountType: String?,
        keepAccountName: String?,
        refreshSummary: Boolean
    ): Boolean {
        val instances = contactDao.getContactInstancesByMatchingKey(matchingKey)
        if (instances.size < 2) return false

        val idsToDelete = ArrayList<Long>(instances.size)
        val contactsToDelete = ArrayList<Contact>(instances.size)

        for (i in instances.indices) {
            val instance = instances[i]
            if (instance.accountType != keepAccountType || instance.accountName != keepAccountName) {
                idsToDelete.add(instance.id)
                contactsToDelete.add(instance.toContact())
            }
        }

        if (idsToDelete.isEmpty()) return false

        backupRepository.performBackup(
            contacts = contactsToDelete,
            actionType = "CONSOLIDATE",
            description = "Consolidated contact to ${getAccountDisplayLabel(keepAccountType)} ($keepAccountName)"
        )

        val success = deleteContactsByIds(idsToDelete)

        // Only refresh summary if requested (skip in batch operations)
        if (refreshSummary) {
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
            // 2026 Best Practice: Skip per-item refresh, do once at end
            if (consolidateContactToAccountInternal(key, keepAccountType, keepAccountName, refreshSummary = false)) {
                successCount++
            }
            val progress = (index + 1).toFloat() / matchingKeys.size.toFloat()
            emit(CleanupStatus.Progress(progress, "Consolidating ${index + 1} of ${matchingKeys.size}"))
        }

        // Single summary refresh after all consolidations
        updateScanResultSummary()

        emit(CleanupStatus.Success("Consolidated $successCount contacts successfully"))
    }

    private fun getAccountDisplayLabel(accountType: String?, accountName: String? = null): String {
        return when {
            accountType == null || accountType.isEmpty() -> "iOS"
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

    private fun LocalContact.toContact(): Contact {
        return Contact(
            id = id,
            name = displayName,
            numbers = rawNumbers.splitAndFilterNotBlank(','),
            emails = rawEmails.splitAndFilterNotBlank(','),
            normalizedNumber = normalizedNumber,
            isWhatsApp = isWhatsApp,
            isTelegram = isTelegram,
            isJunk = isJunk,
            junkType = junkType?.let { JunkType.valueOf(it) },
            duplicateType = duplicateType?.let { DuplicateType.valueOf(it) },
            accountType = accountType,
            accountName = accountName,
            platform_uid = platformUid,
            matchingKey = matchingKey,
            isSensitive = isSensitive,
            sensitiveDescription = sensitiveDescription,
            formatIssue = if (isFormatIssue && normalizedNumber != null) {
                FormatIssue(normalizedNumber, 0, detectedRegion ?: "", "")
            } else null
        )
    }

    private fun Contact.toLocal(): LocalContact {
        return LocalContact(
            id = id,
            displayName = name,
            normalizedNumber = normalizedNumber,
            rawNumbers = numbers.joinToString(","),
            rawEmails = emails.joinToString(","),
            isWhatsApp = isWhatsApp,
            isTelegram = isTelegram,
            accountType = accountType,
            accountName = accountName,
            isJunk = isJunk,
            junkType = junkType?.name,
            duplicateType = duplicateType?.name,
            isFormatIssue = formatIssue != null,
            detectedRegion = formatIssue?.regionCode,
            isSensitive = isSensitive,
            sensitiveDescription = sensitiveDescription,
            matchingKey = matchingKey,
            platformUid = platform_uid,
            lastSynced = Clock.System.now().toEpochMilliseconds()
        )
    }
}
