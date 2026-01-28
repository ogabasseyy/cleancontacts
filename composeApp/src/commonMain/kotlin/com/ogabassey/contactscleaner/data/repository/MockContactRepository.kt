package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.domain.model.AccountGroupSummary
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.domain.model.DuplicateType
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.repository.ContactRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Mock ContactRepository for development and testing.
 *
 * 2026 KMP Best Practice: Mock implementations for preview and testing.
 */
class MockContactRepository : ContactRepository {

    private val mockContacts = generateMockContacts()
    private val _ignoredContacts = MutableStateFlow<List<IgnoredContact>>(emptyList())

    override suspend fun scanContacts(): Flow<ScanStatus> = flow {
        emit(ScanStatus.Progress(0.1f, "Loading contacts..."))
        delay(500)
        emit(ScanStatus.Progress(0.3f, "Analyzing duplicates..."))
        delay(500)
        emit(ScanStatus.Progress(0.5f, "Detecting junk..."))
        delay(500)
        emit(ScanStatus.Progress(0.7f, "Checking format issues..."))
        delay(500)
        emit(ScanStatus.Progress(0.9f, "Finalizing..."))
        delay(300)
        emit(ScanStatus.Success(com.ogabassey.contactscleaner.domain.model.ScanResult(total = 500, junkCount = 15, duplicateCount = 10)))
    }

    override suspend fun deleteContacts(contactIds: List<Long>): Boolean {
        delay(500)
        return true
    }

    override suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus> = flow {
        emit(CleanupStatus.Progress(0f, "Starting cleanup..."))
        delay(300)
        emit(CleanupStatus.Progress(0.5f, "Deleting contacts..."))
        delay(500)
        emit(CleanupStatus.Success("Deleted 10 contacts"))
    }

    override suspend fun mergeContacts(contactIds: List<Long>, customName: String?): Boolean {
        delay(500)
        return true
    }

    override suspend fun saveContacts(contacts: List<Contact>): Boolean {
        delay(300)
        return true
    }

    override suspend fun getDuplicateGroups(type: ContactType): List<DuplicateGroupSummary> {
        return listOf(
            DuplicateGroupSummary("+1234567890", 3, "John, John Smith, J. Smith"),
            DuplicateGroupSummary("+0987654321", 2, "Jane, Jane Doe"),
            DuplicateGroupSummary("john@example.com", 2, "John Doe, Johnny D")
        )
    }

    override suspend fun getAccountGroups(): List<AccountGroupSummary> {
        return listOf(
            AccountGroupSummary("Google", "com.google", 150),
            AccountGroupSummary("Phone", "local", 75),
            AccountGroupSummary("WhatsApp", "com.whatsapp", 200)
        )
    }

    override suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact> {
        return mockContacts.take(5)
    }

    override suspend fun mergeDuplicateGroups(type: ContactType): Flow<CleanupStatus> = flow {
        emit(CleanupStatus.Progress(0f, "Starting merge..."))
        delay(500)
        emit(CleanupStatus.Progress(0.5f, "Merging duplicates..."))
        delay(500)
        emit(CleanupStatus.Success("Merged 5 groups"))
    }

    override suspend fun standardizeFormat(ids: List<Long>): Boolean {
        delay(300)
        return true
    }

    override suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus> = flow {
        emit(CleanupStatus.Progress(0f, "Starting format standardization..."))
        delay(500)
        emit(CleanupStatus.Progress(0.5f, "Updating numbers..."))
        delay(500)
        emit(CleanupStatus.Success("Standardized 15 contacts"))
    }

    override suspend fun getContactsAllSnapshot(): List<Contact> {
        return mockContacts
    }

    override suspend fun getContactsSnapshotByType(type: ContactType): List<Contact> {
        return when (type) {
            ContactType.JUNK -> mockContacts.filter { it.isJunk }
            ContactType.DUPLICATE -> mockContacts.filter { it.duplicateType != null }
            ContactType.FORMAT_ISSUE -> mockContacts.filter { it.formatIssue != null }
            ContactType.SENSITIVE -> mockContacts.filter { it.isSensitive }
            ContactType.WHATSAPP -> mockContacts.filter { it.accountType == "com.whatsapp" }
            ContactType.TELEGRAM -> mockContacts.filter { it.accountType == "org.telegram.messenger" }
            else -> mockContacts
        }
    }

    override suspend fun restoreContacts(contacts: List<Contact>): Boolean {
        delay(500)
        return true
    }

    override suspend fun ignoreContact(id: String, displayName: String, reason: String): Boolean {
        val ignored = IgnoredContact(
            id = id,
            displayName = displayName,
            reason = reason,
            timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
        _ignoredContacts.value = _ignoredContacts.value + ignored
        return true
    }

    override suspend fun unignoreContact(id: String): Boolean {
        _ignoredContacts.value = _ignoredContacts.value.filter { it.id != id }
        return true
    }

    override fun getIgnoredContacts(): Flow<List<IgnoredContact>> {
        return _ignoredContacts.asStateFlow()
    }

    override fun getContactsFlow(type: ContactType): Flow<List<Contact>> = flow {
        emit(getContactsSnapshotByType(type))
    }

    override fun getAccountCount(): Flow<Int> = flow {
        emit(150 + 75 + 200)
    }

    override suspend fun updateScanResultSummary() {
        // No-op for mock
    }

    private fun generateMockContacts(): List<Contact> {
        return listOf(
            Contact(
                id = 1,
                name = "John Doe",
                numbers = listOf("+1234567890"),
                emails = listOf("john@example.com"),
                normalizedNumber = "+1234567890",
                isJunk = false,
                duplicateType = DuplicateType.NUMBER_MATCH
            ),
            Contact(
                id = 2,
                name = "John Smith",
                numbers = listOf("+1234567890"),
                emails = listOf("john.s@example.com"),
                normalizedNumber = "+1234567890",
                isJunk = false,
                duplicateType = DuplicateType.NUMBER_MATCH
            ),
            Contact(
                id = 3,
                name = "",
                numbers = listOf("+9876543210"),
                emails = emptyList(),
                normalizedNumber = "+9876543210",
                isJunk = true
            ),
            Contact(
                id = 4,
                name = "Unknown",
                numbers = emptyList(),
                emails = emptyList(),
                normalizedNumber = null,
                isJunk = true
            ),
            Contact(
                id = 5,
                name = "Jane Doe",
                numbers = listOf("2345678901"), // Missing + prefix
                emails = listOf("jane@example.com"),
                normalizedNumber = "+12345678901",
                formatIssue = com.ogabassey.contactscleaner.domain.model.FormatIssue("+12345678901", 234, "NG", "Nigeria")
            ),
            Contact(
                id = 6,
                name = "Bob Wilson",
                numbers = listOf("+1122334455"),
                emails = listOf("bob@example.com"),
                normalizedNumber = "+1122334455",
                accountType = "com.whatsapp"
            ),
            Contact(
                id = 7,
                name = "Alice Brown",
                numbers = listOf("+5566778899"),
                emails = listOf("alice@example.com"),
                normalizedNumber = "+5566778899",
                accountType = "org.telegram.messenger"
            ),
            Contact(
                id = 8,
                name = "SSN Contact",
                numbers = listOf("+1112223333"),
                emails = emptyList(),
                normalizedNumber = "+1112223333",
                sensitiveDescription = "SSN: 123-45-6789",
                isSensitive = true
            )
        )
    }
}
