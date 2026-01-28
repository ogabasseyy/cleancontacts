package com.ogabassey.contactscleaner.data.repository

import com.ogabassey.contactscleaner.data.api.SessionStatus
import com.ogabassey.contactscleaner.data.api.WhatsAppDetectorApi
import com.ogabassey.contactscleaner.domain.repository.WhatsAppCheckProgress
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of WhatsAppDetectorRepository using the VPS-hosted Baileys service.
 *
 * 2026 Best Practice: Repository pattern with clean API abstraction.
 */
class WhatsAppDetectorRepositoryImpl(
    private val api: WhatsAppDetectorApi = WhatsAppDetectorApi()
) : WhatsAppDetectorRepository {

    override suspend fun isServiceAvailable(): Boolean {
        val health = api.checkHealth()
        return health.status == "ok"
    }

    override suspend fun getSessionStatus(): SessionStatus {
        return api.getSessionStatus()
    }

    override suspend fun requestPairingCode(phoneNumber: String): String? {
        val response = api.requestPairingCode(phoneNumber)
        return if (response.success) response.code else null
    }

    override suspend fun disconnect(): Boolean {
        val response = api.disconnect()
        return response.success
    }

    override suspend fun checkNumbers(numbers: List<String>): Map<String, Boolean> {
        if (numbers.isEmpty()) return emptyMap()

        val response = api.checkNumbers(numbers)
        if (!response.success) return emptyMap()

        return response.results.associate { it.number to it.hasWhatsApp }
    }

    override fun checkNumbersBatch(numbers: List<String>): Flow<WhatsAppCheckProgress> = flow {
        if (numbers.isEmpty()) {
            emit(WhatsAppCheckProgress.Complete(emptyMap(), 0))
            return@flow
        }

        val batchSize = 50
        val delayMs = 1000L
        val allResults = mutableMapOf<String, Boolean>()
        var whatsappCount = 0

        val batches = numbers.chunked(batchSize)
        var checkedCount = 0

        for (batch in batches) {
            val response = api.checkNumbers(batch)

            if (!response.success) {
                emit(WhatsAppCheckProgress.Error(response.error ?: "Failed to check numbers"))
                return@flow
            }

            for (result in response.results) {
                allResults[result.number] = result.hasWhatsApp
                if (result.hasWhatsApp) whatsappCount++
            }

            checkedCount += batch.size
            emit(WhatsAppCheckProgress.InProgress(
                checked = checkedCount,
                total = numbers.size,
                whatsappCount = whatsappCount
            ))

            // Rate limiting between batches
            if (checkedCount < numbers.size) {
                delay(delayMs)
            }
        }

        emit(WhatsAppCheckProgress.Complete(allResults, whatsappCount))
    }
}
