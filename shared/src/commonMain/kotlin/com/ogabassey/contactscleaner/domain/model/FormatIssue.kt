package com.ogabassey.contactscleaner.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a detected format issue in a phone number.
 * Typically indicates a number that should have international prefix (+).
 */
@Serializable
data class FormatIssue(
    val normalizedNumber: String,
    val countryCode: Int,
    val regionCode: String,
    val displayCountry: String
) {
    // 2026 Security Fix: Override toString to prevent accidental logging of PII (CWE-532)
    override fun toString(): String {
        return "FormatIssue(normalizedNumber=***REDACTED***, countryCode=$countryCode, regionCode=$regionCode, displayCountry=$displayCountry)"
    }
}
