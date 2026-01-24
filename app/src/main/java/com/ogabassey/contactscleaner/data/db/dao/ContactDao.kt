package com.ogabassey.contactscleaner.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.domain.model.AccountGroupSummary

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY display_name ASC")
    fun getAllContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 1 ORDER BY display_name ASC")
    fun getWhatsAppContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 0 ORDER BY display_name ASC")
    fun getNonWhatsAppContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE is_junk = 1 ORDER BY display_name ASC")
    fun getJunkContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'NO_NAME' ORDER BY display_name ASC")
    fun getNoNameContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'NO_NUMBER' ORDER BY display_name ASC")
    fun getNoNumberContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'EMAIL_MATCH' ORDER BY display_name ASC")
    fun getDuplicateEmailContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'NUMBER_MATCH' ORDER BY display_name ASC")
    fun getDuplicateNumberContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'NAME_MATCH' ORDER BY display_name ASC")
    fun getDuplicateNameContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'SIMILAR_NAME_MATCH' ORDER BY display_name ASC")
    fun getSimilarNameContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE is_sensitive = 1 ORDER BY display_name ASC")
    fun getSensitiveContactsPaged(): PagingSource<Int, LocalContact>
    
    // --- Bulk Updates for Analysis Form ---
    @Query("UPDATE contacts SET duplicate_type = 'NUMBER_MATCH' WHERE normalized_number IN (SELECT normalized_number FROM contacts WHERE normalized_number IS NOT NULL AND normalized_number != '' GROUP BY normalized_number HAVING COUNT(*) > 1)")
    fun markDuplicateNumbers()

    @Query("UPDATE contacts SET duplicate_type = 'EMAIL_MATCH' WHERE raw_emails IN (SELECT raw_emails FROM contacts WHERE raw_emails IS NOT NULL AND raw_emails != '' GROUP BY raw_emails HAVING COUNT(*) > 1) AND duplicate_type IS NULL")
    fun markDuplicateEmails()

    @Query("UPDATE contacts SET duplicate_type = 'NAME_MATCH' WHERE display_name IN (SELECT display_name FROM contacts WHERE display_name IS NOT NULL AND display_name != '' GROUP BY display_name HAVING COUNT(*) > 1) AND duplicate_type IS NULL")
    fun markDuplicateNames()
    
    // Scan Result Counts
    @Query("SELECT COUNT(*) FROM contacts")
    fun countTotal(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_whatsapp = 1")
    fun countWhatsApp(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_telegram = 1")
    fun countTelegram(): Int



    @Query("SELECT * FROM contacts WHERE is_telegram = 1 ORDER BY display_name ASC")
    fun getTelegramContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT COUNT(*) FROM contacts WHERE is_junk = 1")
    fun countJunk(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type IS NOT NULL")
    fun countDuplicates(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'NO_NAME'")
    fun countNoName(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'NO_NUMBER'")
    fun countNoNumber(): Int
    
    // Granular Counts
    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'INVALID_CHAR'")
    fun countInvalidChar(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'LONG_NUMBER'")
    fun countLongNumber(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'SHORT_NUMBER'")
    fun countShortNumber(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS'")
    fun countRepetitiveNumber(): Int
    
    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'SYMBOL_NAME'")
    fun countSymbolName(): Int
    
    @Query("SELECT COUNT(DISTINCT account_type) FROM contacts WHERE account_type IS NOT NULL AND account_type != ''")
    fun countAccounts(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'NUMBER_MATCH'")
    fun countDuplicateNumbers(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'EMAIL_MATCH'")
    fun countDuplicateEmails(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'NAME_MATCH'")
    fun countDuplicateNames(): Int

    @Query("SELECT * FROM contacts WHERE junk_type = 'INVALID_CHAR' ORDER BY display_name ASC")
    fun getInvalidCharContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'LONG_NUMBER' ORDER BY display_name ASC")
    fun getLongNumberContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'SHORT_NUMBER' ORDER BY display_name ASC")
    fun getShortNumberContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS' ORDER BY display_name ASC")
    fun getRepetitiveNumberContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'SYMBOL_NAME' ORDER BY display_name ASC")
    fun getSymbolNameContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE account_type IS NOT NULL AND account_type != '' ORDER BY display_name ASC")
    fun getAccountContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT * FROM contacts WHERE is_format_issue = 1 ORDER BY detected_region ASC, normalized_number ASC")
    fun getFormatIssueContactsPaged(): PagingSource<Int, LocalContact>

    @Query("SELECT COUNT(*) FROM contacts WHERE is_sensitive = 1")
    fun countSensitive(): Int


    // --- Grouped Queries for Competitor Style View ---
    @Query("SELECT normalized_number as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'NUMBER_MATCH' GROUP BY normalized_number HAVING COUNT(*) > 1 ORDER BY count DESC")
    suspend fun getDuplicateNumberGroups(): List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary>

    @Query("SELECT raw_emails as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'EMAIL_MATCH' GROUP BY raw_emails HAVING COUNT(*) > 1 ORDER BY count DESC")
    suspend fun getDuplicateEmailGroups(): List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary>

    @Query("SELECT display_name as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'NAME_MATCH' GROUP BY display_name HAVING COUNT(*) > 1 ORDER BY count DESC")
    suspend fun getDuplicateNameGroups(): List<com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary>
    
    @Query("SELECT account_type as accountType, account_name as accountName, COUNT(*) as count FROM contacts WHERE account_type IS NOT NULL AND account_type != '' GROUP BY account_type, account_name ORDER BY count DESC")
    suspend fun getAccountGroups(): List<com.ogabassey.contactscleaner.domain.model.AccountGroupSummary>
    
    @Query("SELECT * FROM contacts WHERE normalized_number = :key AND duplicate_type = 'NUMBER_MATCH'")
    suspend fun getContactsByNumberKey(key: String): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE raw_emails = :key AND duplicate_type = 'EMAIL_MATCH'")
    suspend fun getContactsByEmailKey(key: String): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE display_name = :key AND duplicate_type = 'NAME_MATCH'")
    suspend fun getContactsByNameKey(key: String): List<LocalContact>
    
    @Query("SELECT id FROM contacts WHERE is_whatsapp = 0")
    suspend fun getNonWhatsAppContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE is_junk = 1")
    suspend fun getJunkContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'NO_NAME'")
    suspend fun getNoNameContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'NO_NUMBER'")
    suspend fun getNoNumberContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'INVALID_CHAR'")
    suspend fun getInvalidCharContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'LONG_NUMBER'")
    suspend fun getLongNumberContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'SHORT_NUMBER'")
    suspend fun getShortNumberContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS'")
    suspend fun getRepetitiveNumberContactIds(): List<Long>
    
    @Query("SELECT id FROM contacts WHERE junk_type = 'SYMBOL_NAME'")
    suspend fun getSymbolNameContactIds(): List<Long>

    @Query("SELECT COUNT(*) FROM contacts WHERE is_format_issue = 1")
    fun countFormatIssues(): Int

    @Query("SELECT id FROM contacts WHERE is_format_issue = 1")
    suspend fun getFormatIssueIds(): List<Long>
    @Query("SELECT * FROM contacts WHERE is_format_issue = 1 ORDER BY detected_region ASC, normalized_number ASC")
    suspend fun getFormatIssueContactsSnapshot(): List<LocalContact>



    @Query("SELECT * FROM contacts WHERE id IN (:ids)")
    suspend fun getContactsByIds(ids: List<Long>): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE id IN (:ids) AND is_format_issue = 1")
    suspend fun getFormatIssueContactsByIds(ids: List<Long>): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 1")
    suspend fun getWhatsAppContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 0")
    suspend fun getNonWhatsAppContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_junk = 1")
    suspend fun getJunkContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type IS NOT NULL")
    suspend fun getDuplicateContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_sensitive = 1")
    suspend fun getSensitiveContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts ORDER BY display_name ASC")
    suspend fun getAllContacts(): List<LocalContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<LocalContact>)

    @Query("DELETE FROM contacts WHERE id IN (:contactIds)")
    suspend fun deleteContacts(contactIds: List<Long>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getCount(): Int
}
