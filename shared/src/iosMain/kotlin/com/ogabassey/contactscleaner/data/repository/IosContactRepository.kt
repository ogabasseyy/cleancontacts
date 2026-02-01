package com.ogabassey.contactscleaner.data.repository

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
                        println("📱 Using cached WhatsApp numbers: ${snapshot.numbers.size} (business: ${snapshot.businessCount})")
                    }
                    is CacheSnapshot.SyncInProgress -> {
                        println("⏳ WhatsApp cache sync in progress, using empty set for now")
                    }
                    is CacheSnapshot.Invalid -> {
                        // Cache not valid - check if session is connected
                        val status = whatsAppRepository.getSessionStatus(deviceId)
                        if (status.connected) {
                            // Session connected but cache empty/stale - user should trigger sync
                            println("⚠️ WhatsApp cache empty/stale. Please sync WhatsApp contacts.")
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 2026 Best Practice: Always rethrow CancellationException for proper flow cancellation
                throw e
            } catch (e: Exception) {
                println("⚠️ Could not load WhatsApp cache: ${e.message}")
            }
        }
        emit(ScanStatus.Progress(0.20f, "Processing contacts..."))

        // 4. Get ignored contacts
        val ignoredIds = ignoredContactDao.getAllIds().toSet()

        // 5. Process each contact - 2026 Best Practice: Run CPU-intensive work on background thread
        val localContacts = withContext(Dispatchers.Default) {
            contacts.mapIndexed { index, contact ->
                val primaryNumber = contact.numbers.firstOrNull() ?: ""
                val isIgnored = ignoredIds.contains(contact.id.toString())

                // Sensitive detection (iOS Parity Fix: Strip + prefix before analysis)
                // iOS contact sync often adds + prefix to numbers. We strip it for detection
                // so that 11-digit NINs like "+12345678901" become "12345678901" and match the pattern.
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
                                // Pass RAW number to detector.
                                // The detector needs to know if it starts with '+' to distinguish
                                // real international numbers from IDs that happen to start with 1, 44, etc.
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

                // Format issue detection
                var isFormatIssue = false
                var detectedNormalized = contact.normalizedNumber

                if (junkType == null && !isSensitive && primaryNumber.isNotBlank()) {
                    if (!primaryNumber.startsWith("+") && !primaryNumber.startsWith("*") && !primaryNumber.startsWith("#")) {
                        formatDetector.analyze(primaryNumber)?.let { issue ->
                            isFormatIssue = true
                            detectedNormalized = issue.normalizedNumber
                        }
                    }
                }

                // Check if contact is on WhatsApp by comparing normalized numbers
                val isOnWhatsApp = if (whatsAppPhoneNumbers.isNotEmpty()) {
                    // Check if any of the contact's numbers match WhatsApp numbers
                    contact.numbers.any { num ->
                        val normalized = num.filter { it.isDigit() }
                        whatsAppPhoneNumbers.contains(normalized)
                    }
                } else {
                    // Fallback to iOS sync detection if WhatsApp not linked
                    contact.isWhatsApp
                }

                LocalContact(
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
        }

        // 5. Atomic replace: Delete old + Insert new in single transaction
        // 2026 Best Practice: Prevents data loss if operation fails
        emit(ScanStatus.Progress(0.70f, "Saving contacts to database..."))

        // 2026 Best Practice: Validate data before insert
        val validatedContacts = localContacts.filter { contact ->
            val isValid = contact.id > 0 &&
                (contact.displayName?.length ?: 0) <= 1000 && // Prevent excessively long names
                contact.rawNumbers.length <= 10000 && // Reasonable limit for multiple numbers
                contact.rawEmails.length <= 10000
            if (!isValid) {
                println("⚠️ Filtered invalid contact: id=${contact.id}")
            }
            isValid
        }

        contactDao.replaceAllContacts(validatedContacts)
        emit(ScanStatus.Progress(0.75f, "Contacts saved."))

        // 6. Detect duplicates using Advanced Detector (Parity with Android)
        // 2026 Best Practice: Run CPU-intensive duplicate detection on background thread
        emit(ScanStatus.Progress(0.80f, "Identifying duplicate numbers..."))
        val allContactsDomain = validatedContacts.map { it.toContact() }
        val duplicates = withContext(Dispatchers.Default) {
            duplicateDetector.detectDuplicates(allContactsDomain)
        }

        emit(ScanStatus.Progress(0.85f, "Identifying similar names..."))
        val similarNames = withContext(Dispatchers.Default) {
            duplicateDetector.detectSimilarNameDuplicates(allContactsDomain)
        }
        
        // Map to updates
        val duplicateMap = mutableMapOf<Long, Pair<com.ogabassey.contactscleaner.domain.model.DuplicateType, String>>()
        
        fun addToMap(group: com.ogabassey.contactscleaner.domain.model.DuplicateGroup) {
            group.contacts.forEach { contact ->
                val current = duplicateMap[contact.id]
                val newPriority = when(group.duplicateType) {
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NUMBER_MATCH -> 4
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.EMAIL_MATCH -> 3
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NAME_MATCH -> 2
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.SIMILAR_NAME_MATCH -> 1
                    else -> 0
                }
                val currentPriority = when(current?.first) {
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NUMBER_MATCH -> 4
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.EMAIL_MATCH -> 3
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.NAME_MATCH -> 2
                    com.ogabassey.contactscleaner.domain.model.DuplicateType.SIMILAR_NAME_MATCH -> 1
                    else -> 0
                }
                if (newPriority >= currentPriority) {
                    duplicateMap[contact.id] = Pair(group.duplicateType, group.matchingKey)
                }
            }
        }
        
        duplicates.forEach { addToMap(it) }
        similarNames.forEach { addToMap(it) }
        
        if (duplicateMap.isNotEmpty()) {
            val affectedContacts = localContacts.mapNotNull { local ->
                val info = duplicateMap[local.id]
                if (info != null) {
                    local.copy(
                        duplicateType = info.first.name,
                        matchingKey = info.second
                    )
                } else null
            }
            if (affectedContacts.isNotEmpty()) {
                contactDao.insertContacts(affectedContacts)
            }
        }

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
            println("Deleting ${contacts.size} contacts")

            // Separate contacts with and without platform_uid
            val (withUid, withoutUid) = contacts.partition { it.platform_uid != null }
            println("With UID: ${withUid.size}, Without UID: ${withoutUid.size}")

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
            println("🗑️ [DELETE] UIDs to delete from device: $uids")
            if (uids.isNotEmpty()) {
                // 2026 Best Practice: Check device deletion result to ensure consistency
                val deviceDeleted = contactsSource.deleteContacts(uids)
                println("🗑️ [DELETE] Device deletion result: $deviceDeleted")
                if (!deviceDeleted) {
                    println("🗑️ [DELETE] Device deletion FAILED!")
                    return Result.failure(IllegalStateException("Failed to delete contacts from device"))
                }
            }

            // Delete from DB - all contacts (those without uid are DB-only entries)
            contactDao.deleteContacts(contacts.map { it.id })

            // Log warning if some contacts lacked platform_uid
            if (withoutUid.isNotEmpty()) {
                println("Warning: ${withoutUid.size} contacts lacked platform_uid (DB-only deletion)")
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
                    println("Warning: Device delete returned false")
                }
            }
        } catch (e: CancellationException) {
            // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
            throw e
        } catch (e: Exception) {
            println("Warning: Device delete failed: ${e.message}")
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
            println("Error: Failed to cascade delete to local cache: ${e.message}")
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
        // Fetch contacts from DB to get platform_uids
        val contacts = contactDao.getContactsByIds(contactIds)
        val platformUids = contacts.mapNotNull { it.platformUid }

        if (platformUids.size < 2) {
            println("Not enough contacts with platform_uid for merge: ${platformUids.size}")
            return false
        }

        val success = contactsSource.mergeContacts(platformUids, customName)
        if (success) {
            // Remove merged contacts from local DB (keep first one conceptually replaced by new)
            contactDao.deleteContacts(contactIds)
            // Update scan result summary to reflect merged contacts
            updateScanResultSummary()
        }
        return success
    }

    override suspend fun saveContacts(contacts: List<Contact>): Boolean {
        return contactsSource.restoreContacts(contacts)
    }

    override suspend fun getDuplicateGroups(type: ContactType): List<DuplicateGroupSummary> {
        return when (type) {
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberGroups()
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailGroups()
            ContactType.DUP_NAME -> contactDao.getDuplicateNameGroups()
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
                
                val success = mergeContacts(contactsInGroup.map { it.id })
                if (success) merged++
            }
        }
        
        // Refresh summary
        updateScanResultSummary()

        emit(CleanupStatus.Success("Merged $merged duplicate groups"))
    }

    override suspend fun standardizeFormat(ids: List<Long>): Boolean {
        var success = true
        val successfulIds = mutableListOf<Long>()
        val contacts = contactDao.getFormatIssueContactsByIds(ids)

        for (entity in contacts) {
            val normalizedNumber = entity.normalizedNumber ?: continue
            val platformUid = entity.platformUid ?: continue  // Skip if no platform_uid

            val updated = contactsSource.updateContactNumber(platformUid, normalizedNumber)
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

        emit(CleanupStatus.Progress(0.2f, "Standardizing ${formatIssues.size} contacts..."))

        // Record for history
        val contactsSnapshot = formatIssues.map { it.toContact() }
        backupRepository.performBackup(
            contacts = contactsSnapshot,
            actionType = "FORMAT",
            description = "Standardized ${formatIssues.size} numbers"
        )
        
        val ids = formatIssues.map { it.id }
        val success = standardizeFormat(ids)

        // Refresh the scan result summary to update counts
        updateScanResultSummary()

        if (success) {
            emit(CleanupStatus.Success("Standardized ${formatIssues.size} phone numbers"))
        } else {
            emit(CleanupStatus.Error("Some contacts could not be updated"))
        }
    }

    override suspend fun getContactsAllSnapshot(): List<Contact> {
        return contactDao.getAllContacts().map { it.toContact() }
    }

    override suspend fun getContactsSnapshotByType(type: ContactType): List<Contact> {
        return when (type) {
            ContactType.ALL -> contactDao.getAllContacts().map { it.toContact() }
            ContactType.JUNK -> contactDao.getJunkContactsSnapshot().map { it.toContact() }
            ContactType.DUPLICATE -> contactDao.getDuplicateContactsSnapshot().map { it.toContact() }
            ContactType.DUP_NUMBER -> contactDao.getDuplicateNumberContactsSnapshot().map { it.toContact() }
            ContactType.DUP_EMAIL -> contactDao.getDuplicateEmailContactsSnapshot().map { it.toContact() }
            ContactType.DUP_NAME -> contactDao.getDuplicateNameContactsSnapshot().map { it.toContact() }
            ContactType.DUP_SIMILAR_NAME -> contactDao.getSimilarNameContactsSnapshot().map { it.toContact() }
            ContactType.DUP_CROSS_ACCOUNT -> contactDao.getCrossAccountContactsSnapshot().map { it.toContact() }
            ContactType.FORMAT_ISSUE -> contactDao.getFormatIssueContactsSnapshot().map { it.toContact() }
            ContactType.SENSITIVE -> contactDao.getSensitiveContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_NO_NAME -> contactDao.getNoNameContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_NO_NUMBER -> contactDao.getNoNumberContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_INVALID_CHAR -> contactDao.getInvalidCharContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_LONG_NUMBER -> contactDao.getLongNumberContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_SHORT_NUMBER -> contactDao.getShortNumberContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_REPETITIVE -> contactDao.getRepetitiveNumberContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_SYMBOL -> contactDao.getSymbolNameContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_NUMERICAL_NAME -> contactDao.getNumericalNameContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_EMOJI_NAME -> contactDao.getEmojiNameContactsSnapshot().map { it.toContact() }
            ContactType.JUNK_FANCY_FONT -> contactDao.getFancyFontNameContactsSnapshot().map { it.toContact() }
            ContactType.WHATSAPP -> contactDao.getWhatsAppContactsSnapshot().map { it.toContact() }
            ContactType.TELEGRAM -> contactDao.getTelegramContactsSnapshot().map { it.toContact() }
            ContactType.NON_WHATSAPP -> contactDao.getNonWhatsAppContactsSnapshot().map { it.toContact() }
            ContactType.ACCOUNT -> contactDao.getAllContacts().map { it.toContact() }
            ContactType.JUNK_SUSPICIOUS -> contactDao.getJunkContactsSnapshot().map { it.toContact() }
        }
    }

    override suspend fun restoreContacts(contacts: List<Contact>): Boolean {
        return contactsSource.restoreContacts(contacts)
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
        // Update scan result to reflect unignored contact
        updateScanResultSummary()
        return true
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

    /**
     * Recalculate WhatsApp/Non-WhatsApp counts using cached WhatsApp numbers.
     * Called after WhatsApp sync completes to update contact flags in the database.
     *
     * 2026 Best Practice: Process contacts in batches to prevent OOM on large datasets (50k+).
     */
    override suspend fun recalculateWhatsAppCounts() {
        if (whatsAppRepository == null) {
            println("⚠️ WhatsApp repository not available for recalculation")
            return
        }

        try {
            // Get cached WhatsApp numbers
            val cachedNumbers = whatsAppRepository.getCachedNumbers()
            if (cachedNumbers.isEmpty()) {
                println("⚠️ WhatsApp cache is empty, cannot recalculate")
                return
            }

            println("📱 Recalculating WhatsApp flags using ${cachedNumbers.size} cached numbers...")

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
                val updatedContacts = batch.mapNotNull { contact ->
                    val numbers = contact.rawNumbers.split(",").filter { it.isNotBlank() }
                    val isOnWhatsApp = numbers.any { num ->
                        val normalized = num.filter { it.isDigit() }
                        cachedNumbers.contains(normalized)
                    }

                    // Only update if flag changed
                    if (contact.isWhatsApp != isOnWhatsApp) {
                        contact.copy(isWhatsApp = isOnWhatsApp)
                    } else {
                        null
                    }
                }

                // Update batch
                if (updatedContacts.isNotEmpty()) {
                    contactDao.insertContacts(updatedContacts)
                    totalUpdatedCount += updatedContacts.size
                }

                offset += batchSize
            }

            println("✅ Updated WhatsApp flag for $totalUpdatedCount contacts (processed in batches of $batchSize)")

            // Refresh scan result summary
            updateScanResultSummary()
        } catch (e: CancellationException) {
            // 2026 Best Practice: Always rethrow CancellationException for cooperative cancellation
            throw e
        } catch (e: Exception) {
            println("❌ Failed to recalculate WhatsApp counts: ${e.message}")
        }
    }

    // --- Cross-Account Duplicates ---

    override suspend fun getCrossAccountContacts(): List<CrossAccountContact> {
        val allInstances = contactDao.getCrossAccountContactsSnapshot()

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

        val idsToDelete = instances
            .filter { it.accountType != keepAccountType || it.accountName != keepAccountName }
            .map { it.id }

        if (idsToDelete.isEmpty()) return false

        // Record for backup
        val contactsToDelete = instances
            .filter { it.id in idsToDelete }
            .map { it.toContact() }

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

    private fun LocalContact.toContact(): Contact {
        return Contact(
            id = id,
            name = displayName,
            numbers = rawNumbers.split(",").filter { it.isNotBlank() },
            emails = rawEmails.split(",").filter { it.isNotBlank() },
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
