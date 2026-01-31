package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.data.db.entity.IgnoredContact
import com.ogabassey.contactscleaner.domain.model.AccountGroupSummary
import com.ogabassey.contactscleaner.domain.model.CleanupStatus
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import com.ogabassey.contactscleaner.domain.model.CrossAccountContact
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for contact operations.
 *
 * 2026 KMP Best Practice: Platform-agnostic repository interface.
 */
interface ContactRepository {
    /**
     * Scan all contacts and analyze for issues.
     */
    suspend fun scanContacts(): Flow<ScanStatus>

    /**
     * Delete contacts by their objects.
     */
    suspend fun deleteContacts(contacts: List<Contact>): Result<Unit>

    /**
     * Delete contacts by their IDs (Legacy/Android).
     */
    suspend fun deleteContactsByIds(contactIds: List<Long>): Boolean

    /**
     * Delete all contacts of a specific type.
     */
    suspend fun deleteContactsByType(type: ContactType): Flow<CleanupStatus>

    /**
     * Merge duplicate contacts.
     */
    suspend fun mergeContacts(contactIds: List<Long>, customName: String? = null): Boolean

    /**
     * Save contacts to the device.
     */
    suspend fun saveContacts(contacts: List<Contact>): Boolean

    /**
     * Get duplicate groups summary.
     */
    suspend fun getDuplicateGroups(type: ContactType): List<DuplicateGroupSummary>

    /**
     * Get account groups summary.
     */
    suspend fun getAccountGroups(): List<AccountGroupSummary>

    /**
     * Get contacts in a specific group.
     */
    suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact>

    /**
     * Merge all duplicate groups of a type.
     */
    suspend fun mergeDuplicateGroups(type: ContactType): Flow<CleanupStatus>

    /**
     * Standardize phone number format for specific contacts.
     */
    suspend fun standardizeFormat(ids: List<Long>): Boolean

    /**
     * Standardize all format issues.
     */
    suspend fun standardizeAllFormatIssues(): Flow<CleanupStatus>

    /**
     * Get all contacts as a snapshot.
     */
    suspend fun getContactsAllSnapshot(): List<Contact>

    /**
     * Get contacts of a specific type as a snapshot.
     */
    suspend fun getContactsSnapshotByType(type: ContactType): List<Contact>

    /**
     * Restore deleted contacts.
     */
    suspend fun restoreContacts(contacts: List<Contact>): Boolean

    /**
     * Add a contact to the ignore list.
     */
    suspend fun ignoreContact(id: String, displayName: String, reason: String): Boolean

    /**
     * Remove a contact from the ignore list.
     */
    suspend fun unignoreContact(id: String): Boolean

    /**
     * Get all ignored contacts.
     */
    fun getIgnoredContacts(): Flow<List<IgnoredContact>>

    /**
     * Get contacts as a Flow (for real-time updates).
     */
    fun getContactsFlow(type: ContactType): Flow<List<Contact>>

    /**
     * Get account count as a Flow.
     */
    fun getAccountCount(): Flow<Int>

    /**
     * Update the scan result summary in the provider.
     */
    suspend fun updateScanResultSummary()

    /**
     * Recalculate WhatsApp/Non-WhatsApp counts using cached WhatsApp numbers.
     * Called after WhatsApp sync completes to update contact flags.
     */
    suspend fun recalculateWhatsAppCounts()

    // --- Cross-Account Duplicates ---

    /**
     * Get all contacts that exist in multiple accounts, grouped by matching key.
     */
    suspend fun getCrossAccountContacts(): List<CrossAccountContact>

    /**
     * Get all instances of a contact across accounts by matching key.
     */
    suspend fun getContactInstancesByMatchingKey(matchingKey: String): List<Contact>

    /**
     * Consolidate a contact to a single account by deleting it from all other accounts.
     * @param matchingKey The matching key of the contact to consolidate
     * @param keepAccountType The account type to keep
     * @param keepAccountName The account name to keep
     * @return True if successful
     */
    suspend fun consolidateContactToAccount(
        matchingKey: String,
        keepAccountType: String?,
        keepAccountName: String?
    ): Boolean

    /**
     * Consolidate multiple contacts to a single account.
     */
    suspend fun consolidateContactsToAccount(
        matchingKeys: List<String>,
        keepAccountType: String?,
        keepAccountName: String?
    ): Flow<CleanupStatus>
}
