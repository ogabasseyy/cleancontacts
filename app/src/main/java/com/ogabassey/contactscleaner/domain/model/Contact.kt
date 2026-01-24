package com.ogabassey.contactscleaner.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: Long,
    val name: String?,
    val numbers: List<String>,
    val emails: List<String> = emptyList(),
    val normalizedNumber: String?,
    val isWhatsApp: Boolean = false,
    val isTelegram: Boolean = false,
    val isJunk: Boolean = false,
    val junkType: JunkType? = null,
    val duplicateType: DuplicateType? = null,
    val accountType: String? = null,
    val accountName: String? = null,
    val isSensitive: Boolean = false,
    val sensitiveDescription: String? = null
)


@Serializable
enum class JunkType {
    NO_NAME, NO_NUMBER, SUSPICIOUS_NUMBER, REPETITIVE_DIGITS, SYMBOL_NAME, INVALID_CHAR, LONG_NUMBER, SHORT_NUMBER
}

@Serializable
enum class DuplicateType {
    NUMBER_MATCH, EMAIL_MATCH, NAME_MATCH, SIMILAR_NAME_MATCH
}

data class JunkContact(
    val id: Long,
    val name: String?,
    val number: String?,
    val type: JunkType
)

data class ScanResult(
    val total: Int = 0,
    val rawCount: Int = 0, // NEW: Tracks RawContacts source count
    val whatsAppCount: Int = 0,
    val telegramCount: Int = 0,
    val nonWhatsAppCount: Int = 0,
    val junkCount: Int = 0,
    val duplicateCount: Int = 0,
    // Granular counts
    val noNameCount: Int = 0,
    val noNumberCount: Int = 0,
    val emailDuplicateCount: Int = 0,
    val numberDuplicateCount: Int = 0,
    val nameDuplicateCount: Int = 0,
    // Expanded Granular (V3)
    val accountCount: Int = 0,
    val similarNameCount: Int = 0,
    val invalidCharCount: Int = 0,
    val longNumberCount: Int = 0,
    val shortNumberCount: Int = 0,
    val repetitiveNumberCount: Int = 0,
    val symbolNameCount: Int = 0,
    val formatIssueCount: Int = 0,
    val sensitiveCount: Int = 0 
)

enum class ContactType {
    ALL, WHATSAPP, TELEGRAM, NON_WHATSAPP, JUNK, DUPLICATE, ACCOUNT,
    // Granular Junk
    JUNK_NO_NAME, JUNK_NO_NUMBER, JUNK_SUSPICIOUS,
    JUNK_INVALID_CHAR, JUNK_LONG_NUMBER, JUNK_SHORT_NUMBER, JUNK_REPETITIVE, JUNK_SYMBOL,
    // Granular Duplicates
    DUP_EMAIL, DUP_NUMBER, DUP_NAME, DUP_SIMILAR_NAME,
    // Format Issues
    FORMAT_ISSUE,
    // Sensitive Data
    SENSITIVE
}

data class DuplicateGroup(
    val matchingKey: String,
    val duplicateType: DuplicateType,
    val contacts: List<Contact>
)

data class DuplicateGroupSummary(
    val groupKey: String,
    val count: Int,
    val previewNames: String // Comma separated names
)

data class AccountGroupSummary(
    val accountType: String?,
    val accountName: String?,
    val count: Int
)

data class ImportResult(
    val validContacts: List<Contact>,
    val junkContacts: List<JunkContact>,
    val duplicates: List<DuplicateGroup>
)
