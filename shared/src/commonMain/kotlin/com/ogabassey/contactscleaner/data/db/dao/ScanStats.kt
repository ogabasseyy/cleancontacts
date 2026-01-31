package com.ogabassey.contactscleaner.data.db.dao

data class ScanStats(
    val total: Int,
    val whatsAppCount: Int,
    val telegramCount: Int,
    val junkCount: Int,
    val duplicateCount: Int,
    val noNameCount: Int,
    val noNumberCount: Int,
    val emailDuplicateCount: Int,
    val numberDuplicateCount: Int,
    val nameDuplicateCount: Int,
    val accountCount: Int,
    val similarNameCount: Int,
    val invalidCharCount: Int,
    val longNumberCount: Int,
    val shortNumberCount: Int,
    val repetitiveNumberCount: Int,
    val symbolNameCount: Int,
    val numericalNameCount: Int,
    val emojiNameCount: Int,
    val fancyFontCount: Int,
    val formatIssueCount: Int,
    val sensitiveCount: Int,
    val crossAccountDuplicateCount: Int
)
