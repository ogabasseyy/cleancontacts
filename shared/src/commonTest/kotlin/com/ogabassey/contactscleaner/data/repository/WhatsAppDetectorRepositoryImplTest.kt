package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.api.WhatsAppDetectorApi
import com.ogabassey.contactscleaner.data.db.dao.WhatsAppCacheDao
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheEntry
import com.ogabassey.contactscleaner.data.db.entity.WhatsAppCacheMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class WhatsAppDetectorRepositoryImplTest {
    @Test
    fun replaceCacheWithAccuracyResultsStoresPositiveAndNegativeResults() = runBlocking {
        val dao = FakeWhatsAppCacheDao(
            entries = listOf(
                WhatsAppCacheEntry(
                    normalizedNumber = "2348011111111",
                    hasWhatsApp = true,
                    isBusiness = true,
                    lastSynced = 1L
                )
            )
        )
        val repository = WhatsAppDetectorRepositoryImpl(
            api = WhatsAppDetectorApi(baseUrl = "https://api.example.test", apiKey = "test"),
            cacheDao = dao
        )

        repository.replaceCacheWithAccuracyResults(
            mapOf(
                "+234 801 111 1111" to true,
                "+234 802 222 2222" to false,
                "+234 803 333 3333" to true
            )
        )

        assertEquals(
            mapOf(
                "2348011111111" to true,
                "2348022222222" to false,
                "2348033333333" to true
            ),
            dao.entries.associate { it.normalizedNumber to it.hasWhatsApp }
        )
        assertEquals(setOf("2348011111111", "2348033333333"), dao.getAllNumbers().toSet())
        assertEquals(setOf("2348011111111"), dao.getBusinessNumbers().toSet())
        assertEquals(
            WhatsAppCacheMeta(
                key = "sync_status",
                lastFullSync = dao.meta.lastFullSync,
                totalCount = 2,
                businessCount = 1,
                personalCount = 1,
                syncInProgress = false
            ),
            dao.meta
        )
    }

    private class FakeWhatsAppCacheDao(
        entries: List<WhatsAppCacheEntry> = emptyList(),
        var meta: WhatsAppCacheMeta = WhatsAppCacheMeta()
    ) : WhatsAppCacheDao {
        var entries = entries.toMutableList()

        override suspend fun insertAll(entries: List<WhatsAppCacheEntry>) {
            this.entries = entries.toMutableList()
        }

        override suspend fun getAllEntries(): List<WhatsAppCacheEntry> = entries

        override suspend fun getAllNumbers(): List<String> {
            return entries.filter { it.hasWhatsApp }.map { it.normalizedNumber }
        }

        override suspend fun getBusinessNumbers(): List<String> {
            return entries.filter { it.hasWhatsApp && it.isBusiness }.map { it.normalizedNumber }
        }

        override suspend fun hasNumber(number: String): Boolean {
            return entries.any { it.normalizedNumber == number && it.hasWhatsApp }
        }

        override suspend fun getCount(): Int = entries.size

        override suspend fun getBusinessCount(): Int = entries.count { it.hasWhatsApp && it.isBusiness }

        override suspend fun deleteAll() {
            entries.clear()
        }

        override suspend fun deleteAllMeta() {
            meta = WhatsAppCacheMeta()
        }

        override suspend fun updateMeta(meta: WhatsAppCacheMeta) {
            this.meta = meta
        }

        override suspend fun getMeta(): WhatsAppCacheMeta? = meta

        override fun getMetaFlow(): Flow<WhatsAppCacheMeta?> = flowOf(meta)

        override suspend fun setSyncInProgress(inProgress: Boolean) {
            meta = meta.copy(syncInProgress = inProgress)
        }

        override suspend fun getLastSyncTime(): Long? = meta.lastFullSync
    }
}
