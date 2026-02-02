package com.ogabassey.contactscleaner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ogabassey.contactscleaner.data.db.entity.LocalContact
import com.ogabassey.contactscleaner.domain.model.AccountGroupSummary
import com.ogabassey.contactscleaner.domain.model.DuplicateGroupSummary
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for contact operations.
 *
 * 2026 KMP Best Practice: Room 2.7.0+ DAOs work across platforms.
 * All methods use suspend or Flow for cross-platform compatibility.
 * Paging is handled differently per platform (Android uses room-paging,
 * iOS uses manual pagination via snapshot queries).
 */
@Dao
interface ContactDao {

    // --- Consolidated Scan Stats Query (2026 Performance Optimization) ---
    /**
     * 2026 Best Practice: Single query to retrieve all scan statistics.
     * Consolidates 23 separate COUNT queries into one optimized query.
     * Uses CASE WHEN expressions for conditional counting.
     */
    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN is_whatsapp = 1 THEN 1 ELSE 0 END) as whatsAppCount,
            SUM(CASE WHEN is_telegram = 1 THEN 1 ELSE 0 END) as telegramCount,
            SUM(CASE WHEN is_junk = 1 THEN 1 ELSE 0 END) as junkCount,
            SUM(CASE WHEN duplicate_type IS NOT NULL THEN 1 ELSE 0 END) as duplicateCount,
            SUM(CASE WHEN junk_type = 'NO_NAME' THEN 1 ELSE 0 END) as noNameCount,
            SUM(CASE WHEN junk_type = 'NO_NUMBER' THEN 1 ELSE 0 END) as noNumberCount,
            SUM(CASE WHEN junk_type = 'INVALID_CHAR' THEN 1 ELSE 0 END) as invalidCharCount,
            SUM(CASE WHEN junk_type = 'LONG_NUMBER' THEN 1 ELSE 0 END) as longNumberCount,
            SUM(CASE WHEN junk_type = 'SHORT_NUMBER' THEN 1 ELSE 0 END) as shortNumberCount,
            SUM(CASE WHEN junk_type = 'REPETITIVE_DIGITS' THEN 1 ELSE 0 END) as repetitiveNumberCount,
            SUM(CASE WHEN junk_type = 'SYMBOL_NAME' THEN 1 ELSE 0 END) as symbolNameCount,
            SUM(CASE WHEN junk_type = 'NUMERICAL_NAME' THEN 1 ELSE 0 END) as numericalNameCount,
            SUM(CASE WHEN junk_type = 'EMOJI_NAME' THEN 1 ELSE 0 END) as emojiNameCount,
            SUM(CASE WHEN junk_type = 'FANCY_FONT_NAME' THEN 1 ELSE 0 END) as fancyFontCount,
            (SELECT COUNT(DISTINCT account_type) FROM contacts WHERE account_type IS NOT NULL AND account_type != '') as accountCount,
            SUM(CASE WHEN duplicate_type = 'NUMBER_MATCH' THEN 1 ELSE 0 END) as duplicateNumberCount,
            SUM(CASE WHEN duplicate_type = 'EMAIL_MATCH' THEN 1 ELSE 0 END) as duplicateEmailCount,
            SUM(CASE WHEN duplicate_type = 'NAME_MATCH' THEN 1 ELSE 0 END) as duplicateNameCount,
            SUM(CASE WHEN is_format_issue = 1 THEN 1 ELSE 0 END) as formatIssueCount,
            SUM(CASE WHEN is_sensitive = 1 THEN 1 ELSE 0 END) as sensitiveCount,
            SUM(CASE WHEN duplicate_type = 'SIMILAR_NAME_MATCH' THEN 1 ELSE 0 END) as similarNameCount,
            (SELECT COUNT(*) FROM (
                SELECT matching_key FROM contacts
                WHERE matching_key IS NOT NULL AND matching_key != ''
                GROUP BY matching_key
                HAVING COUNT(DISTINCT COALESCE(account_type,'') || ':' || COALESCE(account_name,'')) > 1
            )) as crossAccountCount
        FROM contacts
    """)
    suspend fun getScanStats(): ScanStats

    // --- Count Queries (All Suspend for KMP) ---
    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun countTotal(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_whatsapp = 1")
    suspend fun countWhatsApp(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_telegram = 1")
    suspend fun countTelegram(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_junk = 1")
    suspend fun countJunk(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type IS NOT NULL")
    suspend fun countDuplicates(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'NO_NAME'")
    suspend fun countNoName(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'NO_NUMBER'")
    suspend fun countNoNumber(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'INVALID_CHAR'")
    suspend fun countInvalidChar(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'LONG_NUMBER'")
    suspend fun countLongNumber(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'SHORT_NUMBER'")
    suspend fun countShortNumber(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS'")
    suspend fun countRepetitiveNumber(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'SYMBOL_NAME'")
    suspend fun countSymbolName(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'NUMERICAL_NAME'")
    suspend fun countNumericalName(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'EMOJI_NAME'")
    suspend fun countEmojiName(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE junk_type = 'FANCY_FONT_NAME'")
    suspend fun countFancyFontName(): Int

    @Query("SELECT COUNT(DISTINCT account_type) FROM contacts WHERE account_type IS NOT NULL AND account_type != ''")
    suspend fun countAccounts(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'NUMBER_MATCH'")
    suspend fun countDuplicateNumbers(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'EMAIL_MATCH'")
    suspend fun countDuplicateEmails(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'NAME_MATCH'")
    suspend fun countDuplicateNames(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_format_issue = 1")
    suspend fun countFormatIssues(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE is_sensitive = 1")
    suspend fun countSensitive(): Int

    @Query("SELECT COUNT(*) FROM contacts WHERE duplicate_type = 'SIMILAR_NAME_MATCH'")
    suspend fun countSimilarNames(): Int

    // --- Cross-Account Duplicates Queries ---
    // 2026 Fix: Only detect cross-account duplicates between Google and iOS (local) accounts.
    // Excludes WhatsApp and Telegram since those are synced contacts, not user duplicates.

    /**
     * Count unique contacts that exist in multiple accounts.
     * A contact is considered cross-account if it has the same matching_key
     * but different account_type or account_name combinations.
     * Only counts Google (com.google) and iOS local (null/empty/Local) accounts.
     * 2026 Fix: Added account_type = 'Local' to include iOS local contacts.
     */
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT matching_key FROM contacts
            WHERE matching_key IS NOT NULL AND matching_key != ''
            AND (account_type IS NULL OR account_type = '' OR account_type = 'com.google' OR account_type = 'Local')
            GROUP BY matching_key
            HAVING COUNT(DISTINCT COALESCE(account_type,'') || ':' || COALESCE(account_name,'')) > 1
        )
    """)
    suspend fun countCrossAccountContacts(): Int

    /**
     * Get all contacts that exist in multiple accounts.
     * Returns all LocalContact instances where the matching_key appears
     * in more than one distinct account.
     * Only includes Google (com.google) and iOS local (null/empty/Local) accounts.
     * 2026 Fix: Added account_type = 'Local' to include iOS local contacts.
     */
    @Query("""
        SELECT * FROM contacts
        WHERE (account_type IS NULL OR account_type = '' OR account_type = 'com.google' OR account_type = 'Local')
        AND matching_key IN (
            SELECT matching_key FROM contacts
            WHERE matching_key IS NOT NULL AND matching_key != ''
            AND (account_type IS NULL OR account_type = '' OR account_type = 'com.google' OR account_type = 'Local')
            GROUP BY matching_key
            HAVING COUNT(DISTINCT COALESCE(account_type,'') || ':' || COALESCE(account_name,'')) > 1
        )
        ORDER BY display_name ASC, matching_key ASC
    """)
    suspend fun getCrossAccountContactsSnapshot(): List<LocalContact>

    /**
     * Get all instances of a contact across accounts by matching key.
     * Only includes Google (com.google) and iOS local (null/empty/Local) accounts.
     * 2026 Fix: Added account_type = 'Local' to include iOS local contacts.
     */
    @Query("""
        SELECT * FROM contacts
        WHERE matching_key = :matchingKey
        AND (account_type IS NULL OR account_type = '' OR account_type = 'com.google' OR account_type = 'Local')
        ORDER BY account_type, account_name
    """)
    suspend fun getContactInstancesByMatchingKey(matchingKey: String): List<LocalContact>

    // --- Bulk Updates for Analysis ---
    @Query("UPDATE contacts SET duplicate_type = 'NUMBER_MATCH' WHERE normalized_number IN (SELECT normalized_number FROM contacts WHERE normalized_number IS NOT NULL AND normalized_number != '' GROUP BY normalized_number HAVING COUNT(*) > 1)")
    suspend fun markDuplicateNumbers()

    @Query("UPDATE contacts SET duplicate_type = 'EMAIL_MATCH' WHERE raw_emails IN (SELECT raw_emails FROM contacts WHERE raw_emails IS NOT NULL AND raw_emails != '' GROUP BY raw_emails HAVING COUNT(*) > 1) AND duplicate_type IS NULL")
    suspend fun markDuplicateEmails()

    @Query("UPDATE contacts SET duplicate_type = 'NAME_MATCH' WHERE display_name IN (SELECT display_name FROM contacts WHERE display_name IS NOT NULL AND display_name != '' GROUP BY display_name HAVING COUNT(*) > 1) AND duplicate_type IS NULL")
    suspend fun markDuplicateNames()

    // --- Grouped Queries for Duplicate Groups ---
    @Query("SELECT matching_key as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'NUMBER_MATCH' GROUP BY matching_key HAVING COUNT(*) > 1 ORDER BY count DESC")
    suspend fun getDuplicateNumberGroups(): List<DuplicateGroupSummary>

    @Query("SELECT matching_key as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'EMAIL_MATCH' GROUP BY matching_key HAVING COUNT(*) > 1 ORDER BY count DESC")
    suspend fun getDuplicateEmailGroups(): List<DuplicateGroupSummary>

    @Query("SELECT matching_key as groupKey, COUNT(*) as count, GROUP_CONCAT(display_name) as previewNames FROM contacts WHERE duplicate_type = 'NAME_MATCH' GROUP BY matching_key HAVING COUNT(*) > 1 ORDER BY count DESC")
    suspend fun getDuplicateNameGroups(): List<DuplicateGroupSummary>

    @Query("SELECT account_type as accountType, account_name as accountName, COUNT(*) as count FROM contacts WHERE account_type IS NOT NULL AND account_type != '' GROUP BY account_type, account_name ORDER BY count DESC")
    suspend fun getAccountGroups(): List<AccountGroupSummary>

    // --- Contacts by Key ---
    @Query("SELECT * FROM contacts WHERE matching_key = :key AND duplicate_type = 'NUMBER_MATCH'")
    suspend fun getContactsByNumberKey(key: String): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE matching_key = :key AND duplicate_type = 'EMAIL_MATCH'")
    suspend fun getContactsByEmailKey(key: String): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE matching_key = :key AND duplicate_type = 'NAME_MATCH'")
    suspend fun getContactsByNameKey(key: String): List<LocalContact>

    // --- ID Lists ---
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

    @Query("SELECT id FROM contacts WHERE junk_type = 'NUMERICAL_NAME'")
    suspend fun getNumericalNameContactIds(): List<Long>

    @Query("SELECT id FROM contacts WHERE junk_type = 'EMOJI_NAME'")
    suspend fun getEmojiNameContactIds(): List<Long>
    @Query("SELECT id FROM contacts WHERE junk_type = 'FANCY_FONT_NAME'")
    suspend fun getFancyFontNameContactIds(): List<Long>

    @Query("SELECT id FROM contacts WHERE is_format_issue = 1")
    suspend fun getFormatIssueIds(): List<Long>

    // --- Snapshot Queries ---
    @Query("SELECT * FROM contacts WHERE is_format_issue = 1 ORDER BY detected_region ASC, normalized_number ASC")
    suspend fun getFormatIssueContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE id IN (:ids)")
    suspend fun getContactsByIds(ids: List<Long>): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE id IN (:ids) AND is_format_issue = 1")
    suspend fun getFormatIssueContactsByIds(ids: List<Long>): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 1")
    suspend fun getWhatsAppContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_telegram = 1")
    suspend fun getTelegramContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 0")
    suspend fun getNonWhatsAppContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_junk = 1")
    suspend fun getJunkContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type IS NOT NULL")
    suspend fun getDuplicateContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE is_sensitive = 1")
    suspend fun getSensitiveContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'NO_NAME' ORDER BY display_name ASC")
    suspend fun getNoNameContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'NO_NUMBER' ORDER BY display_name ASC")
    suspend fun getNoNumberContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'INVALID_CHAR' ORDER BY display_name ASC")
    suspend fun getInvalidCharContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'LONG_NUMBER' ORDER BY display_name ASC")
    suspend fun getLongNumberContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'SHORT_NUMBER' ORDER BY display_name ASC")
    suspend fun getShortNumberContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'REPETITIVE_DIGITS' ORDER BY display_name ASC")
    suspend fun getRepetitiveNumberContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'SYMBOL_NAME' ORDER BY display_name ASC")
    suspend fun getSymbolNameContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'NUMERICAL_NAME' ORDER BY display_name ASC")
    suspend fun getNumericalNameContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE junk_type = 'EMOJI_NAME' ORDER BY display_name ASC")
    suspend fun getEmojiNameContactsSnapshot(): List<LocalContact>
    @Query("SELECT * FROM contacts WHERE junk_type = 'FANCY_FONT_NAME' ORDER BY display_name ASC")
    suspend fun getFancyFontNameContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'NUMBER_MATCH' ORDER BY display_name ASC")
    suspend fun getDuplicateNumberContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'EMAIL_MATCH' ORDER BY display_name ASC")
    suspend fun getDuplicateEmailContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'NAME_MATCH' ORDER BY display_name ASC")
    suspend fun getDuplicateNameContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts WHERE duplicate_type = 'SIMILAR_NAME_MATCH' ORDER BY display_name ASC")
    suspend fun getSimilarNameContactsSnapshot(): List<LocalContact>

    @Query("SELECT * FROM contacts ORDER BY display_name ASC")
    suspend fun getAllContacts(): List<LocalContact>

    /**
     * 2026 Best Practice: Paginated contact query to prevent OOM on large datasets.
     * Use this for batch processing of 50k+ contacts.
     */
    @Query("SELECT * FROM contacts ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getContactsBatch(limit: Int, offset: Int): List<LocalContact>

    // Note: Use countTotal() for total contact count - no duplicate method needed

    // --- Flow Queries (KMP Compatible) ---
    @Query("SELECT * FROM contacts ORDER BY display_name ASC")
    fun getAllContactsFlow(): Flow<List<LocalContact>>

    @Query("SELECT * FROM contacts WHERE is_whatsapp = 1 ORDER BY display_name ASC")
    fun getWhatsAppContactsFlow(): Flow<List<LocalContact>>

    @Query("SELECT * FROM contacts WHERE is_junk = 1 ORDER BY display_name ASC")
    fun getJunkContactsFlow(): Flow<List<LocalContact>>

    // --- CRUD Operations ---
    // 2026 Fix: Use @Upsert for semantically correct insert-or-update behavior
    // @Upsert is cleaner than REPLACE (which does DELETE then INSERT)
    @Upsert
    suspend fun insertContacts(contacts: List<LocalContact>)

    @Query("DELETE FROM contacts WHERE id IN (:contactIds)")
    suspend fun deleteContacts(contactIds: List<Long>)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query("UPDATE contacts SET is_sensitive = 0 WHERE id = :id")
    suspend fun resetSensitiveFlag(id: Long)

    @Query("UPDATE contacts SET is_format_issue = 0 WHERE id = :id")
    suspend fun clearFormatIssueFlag(id: Long)

    @Query("UPDATE contacts SET is_format_issue = 0 WHERE id IN (:ids)")
    suspend fun clearFormatIssueFlags(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getCount(): Int

    // --- 2026 Best Practice: Transaction Methods for Atomic Operations ---

    /**
     * Atomically replace all contacts - deletes existing and inserts new in single transaction.
     * Prevents data loss if insert fails after delete.
     */
    @Transaction
    suspend fun replaceAllContacts(contacts: List<LocalContact>) {
        deleteAll()
        insertContacts(contacts)
    }

    /**
     * Atomically mark duplicates after detection.
     * Ensures all duplicate types are marked together.
     */
    @Transaction
    suspend fun markAllDuplicates() {
        markDuplicateNumbers()
        markDuplicateEmails()
        markDuplicateNames()
    }
}
