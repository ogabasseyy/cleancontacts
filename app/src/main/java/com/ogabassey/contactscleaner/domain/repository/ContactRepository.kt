package com.ogabassey.contactscleaner.domain.repository

import androidx.paging.PagingData
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ContactType
import com.ogabassey.contactscleaner.domain.model.ScanResult
import com.ogabassey.contactscleaner.domain.model.ScanStatus
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContactsPaged(type: ContactType): Flow<PagingData<Contact>>
    suspend fun scanContacts(): Flow<ScanStatus>
    suspend fun deleteContacts(contactIds: List<Long>): Boolean
    suspend fun deleteContactsByType(type: ContactType): Flow<com.ogabassey.contactscleaner.domain.model.CleanupStatus>
    suspend fun mergeContacts(contactIds: List<Long>, customName: String? = null): Boolean
    suspend fun saveContacts(contacts: List<Contact>): Boolean
    
    // New Grouped Methods
    suspend fun getDuplicateGroups(type: ContactType): List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary>
    suspend fun getAccountGroups(): List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>
    suspend fun getContactsInGroup(key: String, type: ContactType): List<Contact>
    suspend fun mergeDuplicateGroups(type: ContactType): Flow<com.ogabassey.contactscleaner.domain.model.CleanupStatus>
    suspend fun standardizeFormat(ids: List<Long>): Boolean
    suspend fun standardizeAllFormatIssues(): Flow<com.ogabassey.contactscleaner.domain.model.CleanupStatus>
    
    // Snapshot methods for Export
    suspend fun getContactsAllSnapshot(): List<Contact>
    suspend fun getContactsSnapshotByType(type: ContactType): List<Contact>
    
    // Summary persistence
    suspend fun updateScanResultSummary()
    
    // Undo Support
    suspend fun restoreContacts(contacts: List<Contact>): Boolean
}
