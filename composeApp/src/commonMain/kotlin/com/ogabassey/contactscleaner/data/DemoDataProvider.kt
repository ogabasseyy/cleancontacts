package com.ogabassey.contactscleaner.data

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.DuplicateType
import com.ogabassey.contactscleaner.domain.model.FormatIssue
import com.ogabassey.contactscleaner.domain.model.JunkType
import com.ogabassey.contactscleaner.domain.model.ScanResult

/**
 * Demo Data Provider - Apple Guideline 5.1.1 Compliance
 *
 * Provides sample contacts for demo mode so users can see how the app
 * works without granting contacts permission.
 */
object DemoDataProvider {

    /**
     * Sample contacts that demonstrate various issues the app can detect.
     */
    val demoContacts: List<Contact> = listOf(
        // Normal contacts
        Contact(
            id = -1,
            name = "John Smith",
            numbers = listOf("+1 (555) 123-4567"),
            emails = listOf("john.smith@email.com"),
            normalizedNumber = "+15551234567",
            accountType = "com.google",
            accountName = "demo@gmail.com"
        ),
        Contact(
            id = -2,
            name = "Sarah Johnson",
            numbers = listOf("+1 (555) 987-6543"),
            emails = listOf("sarah.j@work.com"),
            normalizedNumber = "+15559876543",
            accountType = "com.google",
            accountName = "demo@gmail.com"
        ),
        Contact(
            id = -3,
            name = "Mike Williams",
            numbers = listOf("+44 7700 900123"),
            emails = emptyList(),
            normalizedNumber = "+447700900123",
            accountType = "com.apple",
            accountName = "iCloud"
        ),

        // Duplicate contacts (same number)
        Contact(
            id = -4,
            name = "John Smith",
            numbers = listOf("555-123-4567"),
            emails = emptyList(),
            normalizedNumber = "+15551234567",
            duplicateType = DuplicateType.NUMBER_MATCH,
            matchingKey = "+15551234567",
            accountType = "com.apple",
            accountName = "iCloud"
        ),

        // Junk: No name
        Contact(
            id = -5,
            name = null,
            numbers = listOf("+1 555 111 2222"),
            emails = emptyList(),
            normalizedNumber = "+15551112222",
            isJunk = true,
            junkType = JunkType.NO_NAME
        ),

        // Junk: No number
        Contact(
            id = -6,
            name = "Random Note",
            numbers = emptyList(),
            emails = emptyList(),
            normalizedNumber = null,
            isJunk = true,
            junkType = JunkType.NO_NUMBER
        ),

        // Junk: Emoji name
        Contact(
            id = -7,
            name = "🎉 Party Contact 🎊",
            numbers = listOf("+1 555 333 4444"),
            emails = emptyList(),
            normalizedNumber = "+15553334444",
            isJunk = true,
            junkType = JunkType.EMOJI_NAME
        ),

        // Junk: Repetitive digits
        Contact(
            id = -8,
            name = "Test Entry",
            numbers = listOf("111-111-1111"),
            emails = emptyList(),
            normalizedNumber = "+11111111111",
            isJunk = true,
            junkType = JunkType.REPETITIVE_DIGITS
        ),

        // Format issue: Missing country code
        Contact(
            id = -9,
            name = "Local Friend",
            numbers = listOf("555-444-3333"),
            emails = emptyList(),
            normalizedNumber = "5554443333",
            formatIssue = FormatIssue(
                normalizedNumber = "+15554443333",
                countryCode = 1,
                regionCode = "US",
                displayCountry = "United States"
            )
        ),

        // Sensitive: Bank contact
        Contact(
            id = -10,
            name = "Chase Bank Support",
            numbers = listOf("+1 800 935 9935"),
            emails = emptyList(),
            normalizedNumber = "+18009359935",
            isSensitive = true,
            sensitiveDescription = "Financial institution"
        ),

        // Duplicate: Email match
        Contact(
            id = -11,
            name = "S. Johnson",
            numbers = listOf("+1 555 222 3333"),
            emails = listOf("sarah.j@work.com"),
            normalizedNumber = "+15552223333",
            duplicateType = DuplicateType.EMAIL_MATCH,
            matchingKey = "sarah.j@work.com"
        ),

        // WhatsApp contact
        Contact(
            id = -12,
            name = "WhatsApp Friend",
            numbers = listOf("+1 555 888 9999"),
            emails = emptyList(),
            normalizedNumber = "+15558889999",
            isWhatsApp = true,
            accountType = "com.whatsapp",
            accountName = "WhatsApp"
        )
    )

    /**
     * Pre-calculated scan result for demo contacts.
     */
    val demoScanResult: ScanResult = ScanResult(
        total = demoContacts.size,
        rawCount = demoContacts.size,
        whatsAppCount = demoContacts.count { it.isWhatsApp },
        telegramCount = 0,
        nonWhatsAppCount = demoContacts.count { !it.isWhatsApp },
        junkCount = demoContacts.count { it.isJunk },
        duplicateCount = demoContacts.count { it.duplicateType != null },
        noNameCount = demoContacts.count { it.junkType == JunkType.NO_NAME },
        noNumberCount = demoContacts.count { it.junkType == JunkType.NO_NUMBER },
        emailDuplicateCount = demoContacts.count { it.duplicateType == DuplicateType.EMAIL_MATCH },
        numberDuplicateCount = demoContacts.count { it.duplicateType == DuplicateType.NUMBER_MATCH },
        nameDuplicateCount = 0,
        accountCount = 3, // google, apple, whatsapp
        similarNameCount = 0,
        invalidCharCount = 0,
        longNumberCount = 0,
        shortNumberCount = 0,
        repetitiveNumberCount = demoContacts.count { it.junkType == JunkType.REPETITIVE_DIGITS },
        symbolNameCount = 0,
        numericalNameCount = 0,
        emojiNameCount = demoContacts.count { it.junkType == JunkType.EMOJI_NAME },
        fancyFontCount = 0,
        formatIssueCount = demoContacts.count { it.formatIssue != null },
        sensitiveCount = demoContacts.count { it.isSensitive },
        crossAccountDuplicateCount = 1 // John Smith exists in both Google and iCloud
    )

    /**
     * Whether demo mode is currently active.
     * Used to show demo badge in UI.
     */
    var isDemoModeActive: Boolean = false
        private set

    fun enableDemoMode() {
        isDemoModeActive = true
    }

    fun disableDemoMode() {
        isDemoModeActive = false
    }
}
