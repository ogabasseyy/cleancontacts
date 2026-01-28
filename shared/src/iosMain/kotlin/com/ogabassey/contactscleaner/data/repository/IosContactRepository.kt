package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.data.db.dao.IgnoredContactDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private val backupRepository: BackupRepository
) : ContactRepository {

    override suspend fun scanContacts(): Flow<ScanStatus> = flow {
        emit(ScanStatus.Progress(0.05f, "Loading contacts from device..."))

        // 1. Clear local DB
        contactDao.deleteAll()
        emit(ScanStatus.Progress(0.10f, "Initializing database..."))

        // 2. Fetch all contacts from iOS
        val contacts = contactsSource.getAllContacts()
        val total = contacts.size

        if (total == 0) {
            emit(ScanStatus.Success(ScanResult()))
            return@flow
        }

        emit(ScanStatus.Progress(0.20f, "Analyzing ${total.formatWithCommas()} contacts..."))
        usageRepository.updateRawScannedCount(total)

        // 3. Get ignored contacts
        val ignoredIds = ignoredContactDao.getAllIds().toSet()

        // 4. Process each contact
        var junkCount = 0
        var formatIssueCount = 0
        var sensitiveCount = 0

        val localContacts = contacts.mapIndexed { index, contact ->
            val progress = 0.20f + (index.toFloat() / total.toFloat()) * 0.50f
            if (index % 50 == 0) {
                emit(ScanStatus.Progress(progress, "Processing contacts (${(index + 1).formatWithCommas()}/${total.formatWithCommas()})..."))
            }

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
                
                if (isSensitive) {
                    sensitiveCount++
                }
            }

            // Junk detection
            val junkType = if (!isIgnored && !isSensitive) {
                junkDetector.getJunkType(contact.name, contact.normalizedNumber ?: primaryNumber)?.also {
                    junkCount++
                }
            } else null

            // Format issue detection
            var isFormatIssue = false
            var detectedNormalized = contact.normalizedNumber

            if (junkType == null && !isSensitive && primaryNumber.isNotBlank()) {
                if (!primaryNumber.startsWith("+") && !primaryNumber.startsWith("*") && !primaryNumber.startsWith("#")) {
                    formatDetector.analyze(primaryNumber)?.let { issue ->
                        isFormatIssue = true
                        detectedNormalized = issue.normalizedNumber
                        formatIssueCount++
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
                isJunk = junkType != null && !isSensitive,
                junkType = junkType?.name,
                duplicateType = null,
                isFormatIssue = isFormatIssue,
                detectedRegion = if (isFormatIssue && detectedNormalized != null) formatDetector.getRegionCode(detectedNormalized) else null,
                isSensitive = isSensitive,
                sensitiveDescription = sensitiveDesc,
                matchingKey = detectedNormalized ?: contact.emails.firstOrNull() ?: contact.name,
                lastSynced = Clock.System.now().toEpochMilliseconds()
            )
        }

        // 5. Insert into local DB
        emit(ScanStatus.Progress(0.70f, "Storing contacts..."))
        contactDao.insertContacts(localContacts)
        emit(ScanStatus.Progress(0.75f, "Contacts stored."))

        // 6. Detect duplicates using Advanced Detector (Parity with Android)
        emit(ScanStatus.Progress(0.80f, "Identifying duplicate numbers..."))
        val allContactsDomain = localContacts.map { it.toContact() }
        val duplicates = duplicateDetector.detectDuplicates(allContactsDomain)
        
        emit(ScanStatus.Progress(0.85f, "Identifying similar names..."))
        val similarNames = duplicateDetector.detectSimilarNameDuplicates(allContactsDomain)
        
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
        
        // 7. Fetch all counts from database for complete ScanResult
        val whatsAppCount = contactDao.countWhatsApp()
        val telegramCount = contactDao.countTelegram()
        val duplicateCount = contactDao.countDuplicates()
        val numberDuplicateCount = contactDao.countDuplicateNumbers()
        val emailDuplicateCount = contactDao.countDuplicateEmails()
        val nameDuplicateCount = contactDao.countDuplicateNames()
        val similarNameCount = contactDao.countSimilarNames()
        val noNameCount = contactDao.countNoName()
        val noNumberCount = contactDao.countNoNumber()
        val invalidCharCount = contactDao.countInvalidChar()
        val longNumberCount = contactDao.countLongNumber()
        val shortNumberCount = contactDao.countShortNumber()
        val repetitiveNumberCount = contactDao.countRepetitiveNumber()
        val symbolNameCount = contactDao.countSymbolName()
        val numericalNameCount = contactDao.countNumericalName()
        val emojiNameCount = contactDao.countEmojiName()
        val accountCount = contactDao.countAccounts()
        val actualJunkCount = contactDao.countJunk()
        val actualFormatIssueCount = contactDao.countFormatIssues()
        val actualSensitiveCount = contactDao.countSensitive()

        // 8. Update scan result provider with all counts
        val result = ScanResult(
            total = total,
            rawCount = total,
            whatsAppCount = whatsAppCount,
            telegramCount = telegramCount,
            junkCount = actualJunkCount,
            duplicateCount = duplicateCount,
            formatIssueCount = actualFormatIssueCount,
            sensitiveCount = actualSensitiveCount,
            noNameCount = noNameCount,
            noNumberCount = noNumberCount,
            emailDuplicateCount = emailDuplicateCount,
            numberDuplicateCount = numberDuplicateCount,
            nameDuplicateCount = nameDuplicateCount,
            accountCount = accountCount,
            similarNameCount = similarNameCount,
            invalidCharCount = invalidCharCount,
            longNumberCount = longNumberCount,
            shortNumberCount = shortNumberCount,
            repetitiveNumberCount = repetitiveNumberCount,
            symbolNameCount = symbolNameCount,
            numericalNameCount = numericalNameCount,
            emojiNameCount = emojiNameCount,
            fancyFontCount = contactDao.countFancyFontName(),
            nonWhatsAppCount = total - whatsAppCount
        )
        scanResultProvider.scanResult = result

        emit(ScanStatus.Success(result))
    }

    override suspend fun deleteContacts(contactIds: List<Long>): Boolean {
        val success = contactsSource.deleteContacts(contactIds)
        if (success) {
            contactDao.deleteContacts(contactIds)
        }
        return success
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
        val success = deleteContacts(ids)
        updateScanResultSummary()

        if (success) {
            emit(CleanupStatus.Success("Deleted ${contacts.size} contacts"))
        } else {
            emit(CleanupStatus.Error("Failed to delete contacts"))
        }
    }

    override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean {
        return contactsSource.mergeContacts(contactIds, customName)
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

            val updated = contactsSource.updateContactNumber(entity.id, normalizedNumber)
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
            else -> emptyList()
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
        return true
    }

    override suspend fun unignoreContact(id: String): Boolean {
        ignoredContactDao.delete(id)
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
        val junkCount = contactDao.countJunk()
        val duplicateCount = contactDao.countDuplicates()
        val formatIssueCount = contactDao.countFormatIssues()
        val sensitiveCount = contactDao.countSensitive()
        val total = contactDao.countTotal()
        val rawCount = usageRepository.rawScannedCount.first()
        
        scanResultProvider.scanResult = ScanResult(
            total = total,
            rawCount = rawCount,
            whatsAppCount = contactDao.countWhatsApp(),
            telegramCount = contactDao.countTelegram(),
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
            sensitiveCount = contactDao.countSensitive()
        )
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
            isSensitive = isSensitive,
            sensitiveDescription = sensitiveDescription,
            formatIssue = if (isFormatIssue && normalizedNumber != null) {
                FormatIssue(normalizedNumber, 0, detectedRegion ?: "", "")
            } else null
        )
    }
}
