package com.ogabassey.contactscleaner.util

import com.ogabassey.contactscleaner.domain.model.Contact

/**
 * Shared export utilities for CSV and vCard generation.
 * 2026 Best Practice: Centralize export logic to avoid duplication across ViewModels.
 */
object ExportUtils {

    // 2026 Best Practice: Use CharArray with indexOfAny for efficient special character detection
    private val CSV_SPECIAL_CHARS = charArrayOf(',', '"', '\n', '\r')

    // 2026 Security: Safe characters for phone number field to prevent CSV Injection
    // Allowing digits, +, -, (, ), space, and ; (since we use it as separator)
    private val SAFE_PHONE_CHARS = Regex("^[0-9+\\-(); ]*$")

    /**
     * RFC 4180 compliant CSV escaping.
     * Wraps field in quotes if it contains special characters, and escapes internal quotes.
     * 2026 Security Fix: Prevents CSV Injection (Formula Injection) by prepending single quote
     * to values starting with =, +, -, or @.
     *
     * @param value The raw string value.
     * @param isPhoneNumber If true, allows +, - ONLY if the content is safe (digits/format chars only).
     */
    fun escapeCsvValue(value: String, isPhoneNumber: Boolean = false): String {
        var finalValue = value

        // Security: Prevent CSV Injection (Formula Injection)
        // Check if value starts with dangerous characters (=, @, +, -)
        if (value.startsWith("=") || value.startsWith("@") || value.startsWith("+") || value.startsWith("-")) {
            // If it's a phone number field, check if it contains ONLY safe characters
            val isSafePhone = isPhoneNumber && SAFE_PHONE_CHARS.matches(value)

            // If it's not a safe phone number, escape it
            if (!isSafePhone) {
                finalValue = "'$value"
            }
        }

        return if (finalValue.indexOfAny(CSV_SPECIAL_CHARS) >= 0) {
            "\"${finalValue.replace("\"", "\"\"")}\""
        } else {
            finalValue
        }
    }

    /**
     * RFC 6350 compliant vCard escaping.
     * Escapes backslash, semicolon, comma, and newlines per vCard 3.0/4.0 spec.
     */
    fun escapeVCardValue(value: String): String {
        return value
            .replace("\\", "\\\\")  // Escape backslash first
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
    }

    /**
     * Export a list of Contact objects to CSV format.
     * 2026 Best Practice: Consistent CSV format across all export locations.
     */
    fun contactsToCsv(contacts: List<Contact>): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Phone Numbers,Emails,Account Type,Account Name,Is WhatsApp,Is Telegram,Junk Type,Duplicate Type")

        for (contact in contacts) {
            val name = escapeCsvValue(contact.name ?: "")
            // Pass isPhoneNumber = true for numbers column
            val numbers = escapeCsvValue(contact.numbers.joinToString(";"), isPhoneNumber = true)
            val emails = escapeCsvValue(contact.emails.joinToString(";"))
            val accountType = escapeCsvValue(contact.accountType ?: "")
            val accountName = escapeCsvValue(contact.accountName ?: "")
            val junkType = contact.junkType?.name ?: ""
            val duplicateType = contact.duplicateType?.name ?: ""

            sb.appendLine("$name,$numbers,$emails,$accountType,$accountName,${contact.isWhatsApp},${contact.isTelegram},$junkType,$duplicateType")
        }

        return sb.toString()
    }

    /**
     * Export a list of Contact objects to vCard format.
     * 2026 Best Practice: RFC 6350 compliant vCard 3.0 output.
     */
    fun contactsToVCard(contacts: List<Contact>): String {
        val sb = StringBuilder()

        for (contact in contacts) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")

            val displayName = contact.name ?: contact.numbers.firstOrNull() ?: "Unknown"
            sb.appendLine("FN:${escapeVCardValue(displayName)}")

            contact.name?.let { name ->
                sb.appendLine("N:;${escapeVCardValue(name)};;;")
            }

            contact.numbers.forEach { number ->
                sb.appendLine("TEL;TYPE=CELL:${escapeVCardValue(number)}")
            }

            contact.emails.forEach { email ->
                sb.appendLine("EMAIL:${escapeVCardValue(email)}")
            }

            if (contact.isWhatsApp) {
                sb.appendLine("X-WHATSAPP:TRUE")
            }

            if (contact.isTelegram) {
                sb.appendLine("X-TELEGRAM:TRUE")
            }

            contact.accountType?.let { type ->
                sb.appendLine("X-ACCOUNT-TYPE:${escapeVCardValue(type)}")
            }

            contact.accountName?.let { name ->
                sb.appendLine("X-ACCOUNT-NAME:${escapeVCardValue(name)}")
            }

            sb.appendLine("END:VCARD")
            sb.appendLine()
        }

        return sb.toString()
    }
}
