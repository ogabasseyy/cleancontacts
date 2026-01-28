package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ogabassey.contactscleaner.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scan_settings")

/**
 * 2026 AGP 9.0: Migrated from Hilt to Koin.
 */
class ScanSettingsRepository(
    private val context: Context
) {
    private object PreferencesKeys {
        val TOTAL = intPreferencesKey("total")
        val RAW_COUNT = intPreferencesKey("raw_count")
        val WHATSAPP_COUNT = intPreferencesKey("whatsapp_count")
        val TELEGRAM_COUNT = intPreferencesKey("telegram_count")
        val NON_WHATSAPP_COUNT = intPreferencesKey("non_whatsapp_count")
        val JUNK_COUNT = intPreferencesKey("junk_count")
        val DUPLICATE_COUNT = intPreferencesKey("duplicate_count")
        val NO_NAME_COUNT = intPreferencesKey("no_name_count")
        val NO_NUMBER_COUNT = intPreferencesKey("no_number_count")
        val EMAIL_DUP_COUNT = intPreferencesKey("email_dup_count")
        val NUMBER_DUP_COUNT = intPreferencesKey("number_dup_count")
        val NAME_DUP_COUNT = intPreferencesKey("name_dup_count")
        val ACCOUNT_COUNT = intPreferencesKey("account_count")
        val SIMILAR_NAME_COUNT = intPreferencesKey("similar_name_count")
        val INVALID_CHAR_COUNT = intPreferencesKey("invalid_char_count")
        val LONG_NUMBER_COUNT = intPreferencesKey("long_number_count")
        val SHORT_NUMBER_COUNT = intPreferencesKey("short_number_count")
        val REPETITIVE_COUNT = intPreferencesKey("repetitive_count")
        val SYMBOL_COUNT = intPreferencesKey("symbol_count")
        val FORMAT_ISSUE_COUNT = intPreferencesKey("format_issue_count")
        val LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
    }

    val scanResult: Flow<ScanResult?> = context.dataStore.data.map { preferences ->
        val total = preferences[PreferencesKeys.TOTAL] ?: return@map null
        
        ScanResult(
            total = total,
            rawCount = preferences[PreferencesKeys.RAW_COUNT] ?: 0,
            whatsAppCount = preferences[PreferencesKeys.WHATSAPP_COUNT] ?: 0,
            telegramCount = preferences[PreferencesKeys.TELEGRAM_COUNT] ?: 0,
            nonWhatsAppCount = preferences[PreferencesKeys.NON_WHATSAPP_COUNT] ?: 0,
            junkCount = preferences[PreferencesKeys.JUNK_COUNT] ?: 0,
            duplicateCount = preferences[PreferencesKeys.DUPLICATE_COUNT] ?: 0,
            noNameCount = preferences[PreferencesKeys.NO_NAME_COUNT] ?: 0,
            noNumberCount = preferences[PreferencesKeys.NO_NUMBER_COUNT] ?: 0,
            emailDuplicateCount = preferences[PreferencesKeys.EMAIL_DUP_COUNT] ?: 0,
            numberDuplicateCount = preferences[PreferencesKeys.NUMBER_DUP_COUNT] ?: 0,
            nameDuplicateCount = preferences[PreferencesKeys.NAME_DUP_COUNT] ?: 0,
            accountCount = preferences[PreferencesKeys.ACCOUNT_COUNT] ?: 0,
            similarNameCount = preferences[PreferencesKeys.SIMILAR_NAME_COUNT] ?: 0,
            invalidCharCount = preferences[PreferencesKeys.INVALID_CHAR_COUNT] ?: 0,
            longNumberCount = preferences[PreferencesKeys.LONG_NUMBER_COUNT] ?: 0,
            shortNumberCount = preferences[PreferencesKeys.SHORT_NUMBER_COUNT] ?: 0,
            repetitiveNumberCount = preferences[PreferencesKeys.REPETITIVE_COUNT] ?: 0,
            symbolNameCount = preferences[PreferencesKeys.SYMBOL_COUNT] ?: 0,
            formatIssueCount = preferences[PreferencesKeys.FORMAT_ISSUE_COUNT] ?: 0
        )
    }

    val lastScanTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SCAN_TIME] ?: 0L
    }

    suspend fun saveScanResult(result: ScanResult) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL] = result.total
            preferences[PreferencesKeys.RAW_COUNT] = result.rawCount
            preferences[PreferencesKeys.WHATSAPP_COUNT] = result.whatsAppCount
            preferences[PreferencesKeys.TELEGRAM_COUNT] = result.telegramCount
            preferences[PreferencesKeys.NON_WHATSAPP_COUNT] = result.nonWhatsAppCount
            preferences[PreferencesKeys.JUNK_COUNT] = result.junkCount
            preferences[PreferencesKeys.DUPLICATE_COUNT] = result.duplicateCount
            preferences[PreferencesKeys.NO_NAME_COUNT] = result.noNameCount
            preferences[PreferencesKeys.NO_NUMBER_COUNT] = result.noNumberCount
            preferences[PreferencesKeys.EMAIL_DUP_COUNT] = result.emailDuplicateCount
            preferences[PreferencesKeys.NUMBER_DUP_COUNT] = result.numberDuplicateCount
            preferences[PreferencesKeys.NAME_DUP_COUNT] = result.nameDuplicateCount
            preferences[PreferencesKeys.ACCOUNT_COUNT] = result.accountCount
            preferences[PreferencesKeys.SIMILAR_NAME_COUNT] = result.similarNameCount
            preferences[PreferencesKeys.INVALID_CHAR_COUNT] = result.invalidCharCount
            preferences[PreferencesKeys.LONG_NUMBER_COUNT] = result.longNumberCount
            preferences[PreferencesKeys.SHORT_NUMBER_COUNT] = result.shortNumberCount
            preferences[PreferencesKeys.REPETITIVE_COUNT] = result.repetitiveNumberCount
            preferences[PreferencesKeys.SYMBOL_COUNT] = result.symbolNameCount
            preferences[PreferencesKeys.FORMAT_ISSUE_COUNT] = result.formatIssueCount
            preferences[PreferencesKeys.LAST_SCAN_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
