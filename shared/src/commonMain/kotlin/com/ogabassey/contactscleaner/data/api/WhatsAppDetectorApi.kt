package com.ogabassey.contactscleaner.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * WhatsApp Detector API Client
 *
 * Connects to the VPS-hosted WhatsApp detection service using Baileys.
 * 2026 Best Practice: Uses Ktor for KMP HTTP client.
 */
class WhatsAppDetectorApi(
    private val baseUrl: String = "http://82.29.190.219:3456"
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

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
     * Get the current WhatsApp session status.
     */
    suspend fun getSessionStatus(): SessionStatus {
        return try {
            client.get("$baseUrl/session/status").body()
        } catch (e: Exception) {
            SessionStatus(connected = false, error = e.message)
        }
    }

    /**
     * Request a pairing code to link WhatsApp.
     * The user will receive a notification on WhatsApp to enter this code.
     *
     * @param phoneNumber Phone number in international format (e.g., "+1234567890")
     * @return PairingResponse with the 8-digit code or error
     */
    suspend fun requestPairingCode(phoneNumber: String): PairingResponse {
        return try {
            client.post("$baseUrl/session/pair") {
                contentType(ContentType.Application.Json)
                setBody(PairingRequest(phoneNumber = phoneNumber))
            }.body()
        } catch (e: Exception) {
            PairingResponse(success = false, error = e.message)
        }
    }

    /**
     * Disconnect the current WhatsApp session.
     */
    suspend fun disconnect(): DisconnectResponse {
        return try {
            client.post("$baseUrl/session/disconnect").body()
        } catch (e: Exception) {
            DisconnectResponse(success = false, error = e.message)
        }
    }

    /**
     * Check if phone numbers are registered on WhatsApp.
     *
     * @param numbers List of phone numbers to check
     * @return CheckNumbersResponse with results for each number
     */
    suspend fun checkNumbers(numbers: List<String>): CheckNumbersResponse {
        return try {
            client.post("$baseUrl/check") {
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
     * @param numbers List of phone numbers to check
     * @param batchSize Number of contacts per batch (default 50)
     * @param delayMs Delay between batches in milliseconds (default 1000)
     */
    suspend fun checkNumbersBatch(
        numbers: List<String>,
        batchSize: Int = 50,
        delayMs: Int = 1000
    ): BatchCheckResponse {
        return try {
            client.post("$baseUrl/check/batch") {
                contentType(ContentType.Application.Json)
                setBody(BatchCheckRequest(
                    numbers = numbers,
                    batchSize = batchSize,
                    delayMs = delayMs
                ))
            }.body()
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

    fun close() {
        client.close()
    }
}

// Request/Response DTOs

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long
)

@Serializable
data class SessionStatus(
    val connected: Boolean,
    val phoneNumber: String? = null,
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
