package com.ogabassey.contactscleaner.data.api

import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Feedback API client that submits user feedback to the Vercel serverless function.
 * The API sends the feedback as an email via Resend.
 *
 * Creates a short-lived HttpClient per call to avoid lifecycle/leak concerns.
 */
object FeedbackApi {
    private const val BASE_URL = "https://contactscleaner.tech"

    suspend fun submitFeedback(request: FeedbackRequest): FeedbackResponse {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
        }
        return try {
            val response = client.post("$BASE_URL/api/feedback") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (response.status.isSuccess()) {
                FeedbackResponse(success = true)
            } else {
                FeedbackResponse(success = false, error = "Server error: ${response.status.value}")
            }
        } catch (e: Exception) {
            FeedbackResponse(success = false, error = e.message ?: "Failed to send feedback")
        } finally {
            client.close()
        }
    }
}

@Serializable
data class FeedbackRequest(
    val category: String,
    val message: String,
    val email: String = "",
    val deviceInfo: String = ""
)

@Serializable
data class FeedbackResponse(
    val success: Boolean,
    val error: String? = null
)
