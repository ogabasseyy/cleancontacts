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
    // 2026 Security Fix: Override toString to prevent accidental logging of PII (CWE-532)
    // The default data class toString() would include originalValue (e.g., SSN, Credit Card)
    override fun toString(): String {
        return "SensitiveMatch(type=$type, confidence=$confidence, description='$description', originalValue=***REDACTED***)"
    }
}
