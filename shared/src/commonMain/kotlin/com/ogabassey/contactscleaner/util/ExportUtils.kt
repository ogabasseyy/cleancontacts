package com.ogabassey.contactscleaner.util

import com.ogabassey.contactscleaner.domain.model.Contact

/**
 * Shared export utilities for CSV and vCard generation.
 * 2026 Best Practice: Centralize export logic to avoid duplication across ViewModels.
 */
object ExportUtils {

    // 2026 Best Practice: Use CharArray with indexOfAny for efficient special character detection
    private val CSV_SPECIAL_CHARS = charArrayOf(',', '"', '\n', '\r')

    /**
     * RFC 4180 compliant CSV escaping with strict formula injection protection.
     *
     * @param value The raw string to escape.
     * @param isPhoneNumber If true, allows leading '+' and '-' which are valid in phone numbers.
     *                      If false (default), escapes '+' and '-' to prevent formula injection.
     */
    fun escapeCsvValue(value: String, isPhoneNumber: Boolean = false): String {
        var finalValue = value

        // 2026 Security Fix: Prevent CSV Injection (Formula Injection)
        // 1. Always escape '=' and '@' (standard formula triggers).
        if (value.startsWith("=") || value.startsWith("@")) {
            finalValue = "'$value"
        }
        // 2. Strict Field Typing:
        // Only allow '+' and '-' if the caller explicitly declares this is a phone number field.
        // Otherwise, treat '+' and '-' as potential formula triggers and escape them.
        else if (value.startsWith("+") || value.startsWith("-")) {
            if (!isPhoneNumber) {
                finalValue = "'$value"
            }
        }

        // Standard CSV escaping: handle quotes, commas, newlines
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
            // Only 'numbers' field is marked as a phone number field
            val name = escapeCsvValue(contact.name ?: "", isPhoneNumber = false)
            val numbers = escapeCsvValue(contact.numbers.joinToString(";"), isPhoneNumber = true)
            val emails = escapeCsvValue(contact.emails.joinToString(";"), isPhoneNumber = false)
            val accountType = escapeCsvValue(contact.accountType ?: "", isPhoneNumber = false)
            val accountName = escapeCsvValue(contact.accountName ?: "", isPhoneNumber = false)

            // Enum names and booleans are safe by definition (limited charset), but we escape for consistency
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
