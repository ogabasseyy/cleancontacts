package com.ogabassey.contactscleaner.data.db.dao

/**
 * 2026 Best Practice: Consolidated scan statistics data class.
 * Replaces 23 separate COUNT queries with a single optimized query.
 * Used by getScanStats() in ContactDao.
 */
data class ScanStats(
    val total: Int = 0,
    val whatsAppCount: Int = 0,
    val telegramCount: Int = 0,
    val junkCount: Int = 0,
    val duplicateCount: Int = 0,
    val noNameCount: Int = 0,
    val noNumberCount: Int = 0,
    val invalidCharCount: Int = 0,
    val longNumberCount: Int = 0,
    val shortNumberCount: Int = 0,
    val repetitiveNumberCount: Int = 0,
    val symbolNameCount: Int = 0,
    val numericalNameCount: Int = 0,
    val emojiNameCount: Int = 0,
    val fancyFontCount: Int = 0,
    val accountCount: Int = 0,
    val duplicateNumberCount: Int = 0,
    val duplicateEmailCount: Int = 0,
    val duplicateNameCount: Int = 0,
    val formatIssueCount: Int = 0,
    val sensitiveCount: Int = 0,
    val similarNameCount: Int = 0,
    val crossAccountCount: Int = 0
)
