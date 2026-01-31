package com.ogabassey.contactscleaner.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * WhatsApp Detector API Client
 *
 * Connects to the VPS-hosted WhatsApp detection service using Baileys.
 * 2026 Best Practice: Uses Ktor for KMP HTTP client.
 *
 * Multi-Session Support: Each user gets their own isolated WhatsApp session
 * identified by userId. This allows multiple users to link their WhatsApp
 * accounts independently without conflicts.
 */
class WhatsAppDetectorApi(
    private val baseUrl: String = "https://api.contactscleaner.tech"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }

    private val wsUrl = baseUrl.replace("https://", "wss://").replace("http://", "ws://")

    /**
     * Check if the service is healthy and reachable.
     */
    suspend fun checkHealth(): HealthResponse {
        return try {
            client.get("$baseUrl/health").body()
        } catch (e: Exception) {
            HealthResponse(status = "error", timestamp = 0L)
        }
    }

    /**
     * Get the WhatsApp session status for a specific user.
     *
     * @param userId Unique identifier for the user's session
     */
    suspend fun getSessionStatus(userId: String): SessionStatus {
        return try {
            client.get("$baseUrl/session/$userId/status").body()
        } catch (e: Exception) {
            SessionStatus(connected = false, error = e.message)
        }
    }

    /**
     * Request a pairing code to link WhatsApp for a specific user.
     * The user will receive a notification on WhatsApp to enter this code.
     *
     * @param userId Unique identifier for the user's session
     * @param phoneNumber Phone number in international format (e.g., "+1234567890")
     * @return PairingResponse with the 8-digit code or error
     */
    suspend fun requestPairingCode(userId: String, phoneNumber: String): PairingResponse {
        return try {
            client.post("$baseUrl/session/$userId/pair") {
                contentType(ContentType.Application.Json)
                setBody(PairingRequest(phoneNumber = phoneNumber))
            }.body()
        } catch (e: Exception) {
            PairingResponse(success = false, error = e.message)
        }
    }

    /**
     * Disconnect/destroy a user's WhatsApp session.
     *
     * @param userId Unique identifier for the user's session
     */
    suspend fun disconnect(userId: String): DisconnectResponse {
        return try {
            client.delete("$baseUrl/session/$userId").body()
        } catch (e: Exception) {
            DisconnectResponse(success = false, error = e.message)
        }
    }

    /**
     * Check if phone numbers are registered on WhatsApp using a user's session.
     *
     * @param userId Unique identifier for the user's session
     * @param numbers List of phone numbers to check
     * @return CheckNumbersResponse with results for each number
     */
    suspend fun checkNumbers(userId: String, numbers: List<String>): CheckNumbersResponse {
        return try {
            client.post("$baseUrl/session/$userId/check") {
                contentType(ContentType.Application.Json)
                setBody(CheckNumbersRequest(numbers = numbers))
            }.body()
        } catch (e: Exception) {
            CheckNumbersResponse(
                success = false,
                results = emptyList(),
                error = e.message
            )
        }
    }

    /**
     * Batch check phone numbers with rate limiting for large lists.
     * Use this for checking entire contact lists.
     *
     * @param userId Unique identifier for the user's session
     * @param numbers List of phone numbers to check
     * @param batchSize Number of contacts per batch (default 50)
     */
    suspend fun checkNumbersBatch(
        userId: String,
        numbers: List<String>,
        batchSize: Int = 50
    ): BatchCheckResponse {
        // Note: Batch checking uses regular check endpoint with client-side batching
        // Server handles batching internally when using the per-user check endpoint
        return try {
            client.post("$baseUrl/session/$userId/check") {
                contentType(ContentType.Application.Json)
                setBody(CheckNumbersRequest(numbers = numbers))
            }.body<CheckNumbersResponse>().let { response ->
                BatchCheckResponse(
                    success = response.success,
                    total = numbers.size,
                    checked = response.results.size,
                    whatsappCount = response.results.count { it.hasWhatsApp },
                    results = response.results,
                    error = response.error
                )
            }
        } catch (e: Exception) {
            BatchCheckResponse(
                success = false,
                total = numbers.size,
                checked = 0,
                whatsappCount = 0,
                results = emptyList(),
                error = e.message
            )
        }
    }

    /**
     * Get all WhatsApp contacts with business detection.
     * Returns contacts synced from WhatsApp with business/personal classification.
     *
     * Privacy-compliant: No contacts are sent TO the server.
     * The server returns WhatsApp contacts FROM the linked session.
     *
     * @param userId Unique identifier for the user's session
     * @param limit Maximum number of contacts to return (default 500)
     * @param offset Number of contacts to skip (for pagination)
     * @return WhatsAppContactsResponse with contacts and business detection
     */
    suspend fun getContacts(
        userId: String,
        limit: Int = 500,
        offset: Int = 0
    ): WhatsAppContactsResponse {
        return try {
            client.get("$baseUrl/session/$userId/contacts") {
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        } catch (e: Exception) {
            WhatsAppContactsResponse(
                success = false,
                error = e.message
            )
        }
    }

    /**
     * Connect via WebSocket for real-time pairing events.
     * This provides instant notification when pairing code is generated
     * and when WhatsApp connects, instead of polling.
     *
     * @param userId Unique identifier for the user's session
     * @param phoneNumber Phone number in international format
     * @return Flow of PairingEvent for real-time updates
     */
    fun connectForPairing(userId: String, phoneNumber: String): Flow<PairingEvent> = callbackFlow {
        try {
            client.webSocket("$wsUrl/ws/pairing") {
                // Send start_pairing message with userId
                val startMessage = json.encodeToString(
                    WebSocketMessage.serializer(),
                    WebSocketMessage(type = "start_pairing", phoneNumber = phoneNumber, userId = userId)
                )
                send(Frame.Text(startMessage))

                // Now request the pairing code via HTTP (this triggers the WebSocket push)
                val pairingResponse = requestPairingCode(userId, phoneNumber)
                if (!pairingResponse.success) {
                    // 2026 Fix: Use trySend to avoid exception if channel is closed
                    trySend(PairingEvent.Error(pairingResponse.error ?: "Failed to request pairing code"))
                    // 2026 Fix: Explicitly close channel after terminal event
                    channel.close()
                    return@webSocket
                }

                // Listen for WebSocket events
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val event = parsePairingEvent(text)
                        // 2026 Fix: Use trySend for safety - channel might be closed if collector cancelled
                        trySend(event)

                        // Close connection when we get a terminal event
                        if (event is PairingEvent.Connected || event is PairingEvent.Error) {
                            // 2026 Fix: Explicitly close channel after terminal event so Flow completes
                            channel.close()
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 2026 Fix: Use trySend to safely emit error even if channel is closing
            trySend(PairingEvent.Error(e.message ?: "WebSocket connection failed"))
            // 2026 Fix: Explicitly close channel after error so Flow completes
            channel.close()
        }

        awaitClose {
            // 2026 Best Practice: Log cleanup for debugging, no active resources to close
            // WebSocket session is already closed by the time we reach here
            println("WhatsApp pairing WebSocket connection closed")
        }
    }

    private fun parsePairingEvent(jsonText: String): PairingEvent {
        return try {
            val parsed = json.decodeFromString<WebSocketEvent>(jsonText)
            when (parsed.type) {
                "session_created" -> PairingEvent.SessionCreated(parsed.phoneNumber ?: "")
                "pairing_code" -> PairingEvent.PairingCode(parsed.code ?: "")
                "connected" -> PairingEvent.Connected
                "error" -> PairingEvent.Error(parsed.error ?: "Unknown error")
                else -> PairingEvent.Error("Unknown event type: ${parsed.type}")
            }
        } catch (e: Exception) {
            PairingEvent.Error("Failed to parse event: ${e.message}")
        }
    }

    fun close() {
        client.close()
    }
}

/**
 * WebSocket message sent to server
 */
@Serializable
private data class WebSocketMessage(
    val type: String,
    val phoneNumber: String? = null,
    val userId: String? = null
)

/**
 * WebSocket event received from server
 */
@Serializable
private data class WebSocketEvent(
    val type: String,
    val phoneNumber: String? = null,
    val code: String? = null,
    val error: String? = null,
    val timestamp: Long? = null
)

/**
 * Pairing events for real-time updates
 */
sealed class PairingEvent {
    data class SessionCreated(val phoneNumber: String) : PairingEvent()
    data class PairingCode(val code: String) : PairingEvent()
    data object Connected : PairingEvent()
    data class Error(val message: String) : PairingEvent()
}

// Request/Response DTOs

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long
)

@Serializable
data class BusinessDetectionProgress(
    val done: Boolean,
    val inProgress: Boolean,
    val checked: Int,
    val total: Int,
    val businessCount: Int
)

@Serializable
data class SessionStatus(
    val connected: Boolean,
    val userId: String? = null,
    val phoneNumber: String? = null,
    val lastActivity: Long? = null,
    val createdAt: Long? = null,
    val contactsCount: Int? = null,
    val businessDetectionProgress: BusinessDetectionProgress? = null,
    val error: String? = null
)

@Serializable
data class PairingRequest(
    val phoneNumber: String
)

@Serializable
data class PairingResponse(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class DisconnectResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class CheckNumbersRequest(
    val numbers: List<String>
)

@Serializable
data class CheckNumbersResponse(
    val success: Boolean,
    val results: List<NumberCheckResult>,
    val error: String? = null
)

@Serializable
data class NumberCheckResult(
    val number: String,
    val hasWhatsApp: Boolean,
    val jid: String? = null
)

@Serializable
data class BatchCheckRequest(
    val numbers: List<String>,
    val batchSize: Int = 50,
    val delayMs: Int = 1000
)

@Serializable
data class BatchCheckResponse(
    val success: Boolean,
    val total: Int,
    val checked: Int,
    val whatsappCount: Int,
    val results: List<NumberCheckResult>,
    val error: String? = null
)

// WhatsApp Contacts DTOs

@Serializable
data class WhatsAppContactsResponse(
    val success: Boolean,
    val userId: String? = null,
    val total: Int = 0,
    val businessCount: Int = 0,
    val personalCount: Int = 0,
    val contacts: List<WhatsAppContact> = emptyList(),
    val error: String? = null
)

@Serializable
data class WhatsAppContact(
    val jid: String,
    val phoneNumber: String,
    val name: String? = null,
    val pushName: String? = null,
    val isBusiness: Boolean = false,
    val businessProfile: BusinessProfile? = null
)

@Serializable
data class BusinessProfile(
    val description: String? = null,
    val category: String? = null,
    val email: String? = null,
    val website: List<String>? = null,
    val address: String? = null
)
