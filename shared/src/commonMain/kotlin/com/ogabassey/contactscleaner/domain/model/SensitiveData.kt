package com.ogabassey.contactscleaner.domain.model

import kotlinx.serialization.Serializable

/**
 * Types of sensitive data that can be detected.
 */
@Serializable
enum class SensitiveType {
    NIGERIA_NIN_BVN,
    USA_SSN,
    UK_NINO,
    CREDIT_CARD,
    UNKNOWN_PII
}

/**
 * Represents a detected sensitive data match.
 */
@Serializable
data class SensitiveMatch(
    val originalValue: String,
    val type: SensitiveType,
    val confidence: Float, // 0.0 to 1.0
    val description: String
) {
    override fun toString(): String {
        // 2026 Security Fix: Redact sensitive PII in logs
        val redacted = if (originalValue.length > 4) {
            "***" + originalValue.takeLast(4)
        } else {
            "***"
        }
        return "SensitiveMatch(originalValue=$redacted, type=$type, confidence=$confidence, description=$description)"
    }
}
