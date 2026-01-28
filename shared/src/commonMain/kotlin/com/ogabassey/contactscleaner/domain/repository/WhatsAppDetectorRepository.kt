package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.data.api.BatchCheckResponse
import com.ogabassey.contactscleaner.data.api.NumberCheckResult
import com.ogabassey.contactscleaner.data.api.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WhatsApp detection via linked device.
 *
 * This uses a VPS-hosted Baileys service that allows users to link their
 * WhatsApp account to enable accurate WhatsApp detection for their contacts.
 */
interface WhatsAppDetectorRepository {

    /**
     * Check if the WhatsApp detection service is available.
     */
    suspend fun isServiceAvailable(): Boolean

    /**
     * Get the current session status.
     * Returns whether a WhatsApp account is linked.
     */
    suspend fun getSessionStatus(): SessionStatus

    /**
     * Request a pairing code to link WhatsApp.
     * The user will receive a notification on their WhatsApp to enter this code.
     *
     * @param phoneNumber User's phone number in international format
     * @return The 8-digit pairing code, or null if failed
     */
    suspend fun requestPairingCode(phoneNumber: String): String?

    /**
     * Disconnect the linked WhatsApp session.
     */
    suspend fun disconnect(): Boolean

    /**
     * Check if a list of phone numbers are on WhatsApp.
     *
     * @param numbers List of phone numbers to check
     * @return Map of phone number to hasWhatsApp status
     */
    suspend fun checkNumbers(numbers: List<String>): Map<String, Boolean>

    /**
     * Batch check numbers with progress updates.
     * Use this for checking entire contact lists.
     *
     * @param numbers List of phone numbers
     * @return Flow emitting progress updates and final results
     */
    fun checkNumbersBatch(numbers: List<String>): Flow<WhatsAppCheckProgress>
}

/**
 * Progress update for batch WhatsApp checking.
 */
sealed class WhatsAppCheckProgress {
    data class InProgress(
        val checked: Int,
        val total: Int,
        val whatsappCount: Int
    ) : WhatsAppCheckProgress()

    data class Complete(
        val results: Map<String, Boolean>,
        val whatsappCount: Int
    ) : WhatsAppCheckProgress()

    data class Error(val message: String) : WhatsAppCheckProgress()
}
