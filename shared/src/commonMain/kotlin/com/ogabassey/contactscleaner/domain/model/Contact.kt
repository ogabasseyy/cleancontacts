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
    val platform_uid: String? = null,
    val matchingKey: String? = null,
    val isSensitive: Boolean = false,
    val sensitiveDescription: String? = null,
    val formatIssue: FormatIssue? = null
)


@Serializable
enum class JunkType {
    NO_NAME, NO_NUMBER, SUSPICIOUS_NUMBER, REPETITIVE_DIGITS, SYMBOL_NAME, INVALID_CHAR, LONG_NUMBER, SHORT_NUMBER, NUMERICAL_NAME, EMOJI_NAME, FANCY_FONT_NAME
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
    val numericalNameCount: Int = 0,
    val emojiNameCount: Int = 0,
    val fancyFontCount: Int = 0,
    val formatIssueCount: Int = 0,
    val sensitiveCount: Int = 0,
    val crossAccountDuplicateCount: Int = 0
)

enum class ContactType {
    ALL, WHATSAPP, TELEGRAM, NON_WHATSAPP, JUNK, DUPLICATE, ACCOUNT,
    // Granular Junk
    JUNK_NO_NAME, JUNK_NO_NUMBER, JUNK_SUSPICIOUS,
    JUNK_INVALID_CHAR, JUNK_LONG_NUMBER, JUNK_SHORT_NUMBER, JUNK_REPETITIVE, JUNK_SYMBOL,
    JUNK_NUMERICAL_NAME, JUNK_EMOJI_NAME, JUNK_FANCY_FONT,
    // Granular Duplicates
    DUP_EMAIL, DUP_NUMBER, DUP_NAME, DUP_SIMILAR_NAME, DUP_CROSS_ACCOUNT,
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

/**
 * Represents a contact that exists in multiple accounts.
 * Used for the cross-account duplicates feature.
 */
data class CrossAccountContact(
    val name: String?,
    val matchingKey: String,
    val primaryNumber: String?,
    val primaryEmail: String?,
    val accounts: List<AccountInstance>
)

/**
 * Represents a single instance of a contact in a specific account.
 */
data class AccountInstance(
    val contactId: Long,
    val accountType: String?,
    val accountName: String?,
    val displayLabel: String
)
