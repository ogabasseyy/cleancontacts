package com.ogabassey.contactscleaner.domain.repository

import com.ogabassey.contactscleaner.data.api.BatchCheckResponse
import com.ogabassey.contactscleaner.data.api.NumberCheckResult
import com.ogabassey.contactscleaner.data.api.PairingEvent
import com.ogabassey.contactscleaner.data.api.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WhatsApp detection via linked device.
 *
 * This uses a VPS-hosted Baileys service that allows users to link their
 * WhatsApp account to enable accurate WhatsApp detection for their contacts.
 *
 * Multi-Session Support: Each user gets their own isolated WhatsApp session
 * identified by userId. This allows multiple users to link their WhatsApp
 * accounts independently without conflicts.
 */
interface WhatsAppDetectorRepository {

    /**
     * Check if the WhatsApp detection service is available.
     */
    suspend fun isServiceAvailable(): Boolean

    /**
     * Get the session status for a specific user.
     * Returns whether a WhatsApp account is linked for this user.
     *
     * @param userId Unique identifier for the user's session
     */
    suspend fun getSessionStatus(userId: String): SessionStatus

    /**
     * Request a pairing code to link WhatsApp for a specific user.
     * The user will receive a notification on their WhatsApp to enter this code.
     *
     * @param userId Unique identifier for the user's session
     * @param phoneNumber User's phone number in international format
     * @return The 8-digit pairing code, or null if failed
     */
    suspend fun requestPairingCode(userId: String, phoneNumber: String): String?

    /**
     * Disconnect the linked WhatsApp session for a specific user.
     *
     * @param userId Unique identifier for the user's session
     */
    suspend fun disconnect(userId: String): Boolean

    /**
     * Check if a list of phone numbers are on WhatsApp using a user's session.
     *
     * @param userId Unique identifier for the user's session
     * @param numbers List of phone numbers to check
     * @return Map of phone number to hasWhatsApp status
     */
    suspend fun checkNumbers(userId: String, numbers: List<String>): Map<String, Boolean>

    /**
     * Batch check numbers with progress updates using a user's session.
     * Use this for checking entire contact lists.
     *
     * @param userId Unique identifier for the user's session
     * @param numbers List of phone numbers
     * @return Flow emitting progress updates and final results
     */
    fun checkNumbersBatch(userId: String, numbers: List<String>): Flow<WhatsAppCheckProgress>

    /**
     * Connect via WebSocket for real-time pairing events.
     * Provides instant notifications instead of polling.
     *
     * @param userId Unique identifier for the user's session
     * @param phoneNumber User's phone number in international format
     * @return Flow emitting pairing events (code, connected, error)
     */
    fun connectForPairing(userId: String, phoneNumber: String): Flow<PairingEvent>
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
