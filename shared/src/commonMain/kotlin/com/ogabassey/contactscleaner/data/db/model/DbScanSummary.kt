package com.ogabassey.contactscleaner.data.db.model

/**
 * Summary of scan results directly from the database.
 * Used to fetch all counts in a single query for performance.
 */
data class DbScanSummary(
    val total: Int,
    val whatsAppCount: Int,
    val telegramCount: Int,
    val junkCount: Int,
    val duplicateCount: Int,
    val noNameCount: Int,
    val noNumberCount: Int,
    val invalidCharCount: Int,
    val longNumberCount: Int,
    val shortNumberCount: Int,
    val repetitiveNumberCount: Int,
    val symbolNameCount: Int,
    val numericalNameCount: Int,
    val emojiNameCount: Int,
    val fancyFontCount: Int,
    val accountCount: Int,
    val duplicateNumberCount: Int,
    val duplicateEmailCount: Int,
    val duplicateNameCount: Int,
    val similarNameCount: Int,
    val formatIssueCount: Int,
    val sensitiveCount: Int,
    val crossAccountDuplicateCount: Int
)
