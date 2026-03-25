package com.ogabassey.contactscleaner.util

import com.ogabassey.contactscleaner.domain.model.Contact

/**
 * Shared export utilities for CSV and vCard generation.
 * 2026 Best Practice: Centralize export logic to avoid duplication across ViewModels.
 */
object ExportUtils {

    // 2026 Best Practice: Use CharArray with indexOfAny for efficient special character detection
    private val CSV_SPECIAL_CHARS = charArrayOf(',', '"', '\n', '\r')

    // Characters allowed in phone fields without CSV injection escaping
    private const val SAFE_PHONE_CHARS = "0123456789 +-.(),;*#"

    /**
     * RFC 4180 compliant CSV escaping with CSV Injection prevention.
     * @param isPhoneField true for phone number fields where + and - are legitimate prefixes.
     */
    fun escapeCsvValue(value: String, isPhoneField: Boolean = false): String {
        return quoteCsv(escapeInjection(value, isPhoneField))
    }

    /**
     * Escapes multi-value CSV fields. Each element is injection-escaped individually,
     * then joined and structurally quoted once.
     */
    fun escapeMultiValueCsv(values: List<String>, separator: String = ";", isPhoneField: Boolean = false): String {
        return quoteCsv(values.joinToString(separator) { escapeInjection(it, isPhoneField) })
    }

    private fun escapeInjection(value: String, isPhoneField: Boolean = false): String {
        // ⚡ Bolt Optimization: Use primitive Char checks to avoid multiple string allocations
        // and method overhead during high-frequency list exports.
        if (value.isEmpty()) return value
        val firstChar = value[0]

        var needsEscape = firstChar == '=' || firstChar == '@' ||
            firstChar == '\t' || firstChar == '\r'

        if (!needsEscape && (firstChar == '+' || firstChar == '-')) {
            if (isPhoneField) {
                // Allow +/- in phone fields only if all characters are safe phone chars.
                // Blocks formula payloads like "+cmd|' /C calc'!A0"
                needsEscape = value.any { it !in SAFE_PHONE_CHARS }
            } else {
                needsEscape = true
            }
        }
        return if (needsEscape) "'$value" else value
    }

    private fun quoteCsv(value: String): String {
        return if (value.indexOfAny(CSV_SPECIAL_CHARS) >= 0) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * RFC 6350 compliant vCard escaping.
     * Escapes backslash, semicolon, comma, and newlines per vCard 3.0/4.0 spec.
     */
    fun escapeVCardValue(value: String): String {
        // ⚡ Bolt Optimization: Replace multiple chained replace() calls with a single-pass loop
        // to eliminate intermediate String allocations and multiple iterations.
        var sb: StringBuilder? = null
        var i = 0
        var lastAppendPos = 0
        while (i < value.length) {
            val c = value[i]
            var replacement: String? = null
            var skipNext = false
            when (c) {
                '\\' -> replacement = "\\\\"
                ';' -> replacement = "\\;"
                ',' -> replacement = "\\,"
                '\r' -> {
                    replacement = "\\n"
                    if (i + 1 < value.length && value[i + 1] == '\n') {
                        skipNext = true
                    }
                }
                '\n' -> replacement = "\\n"
            }

            if (replacement != null) {
                if (sb == null) {
                    sb = StringBuilder(value.length + 16)
                }
                sb.append(value, lastAppendPos, i)
                sb.append(replacement)

                if (skipNext) {
                    i++ // Skip the next '\n'
                }
                lastAppendPos = i + 1
            }
            i++
        }

        if (sb != null) {
            if (lastAppendPos < value.length) {
                sb.append(value, lastAppendPos, value.length)
            }
            return sb.toString()
        }

        return value
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
            val numbers = escapeMultiValueCsv(contact.numbers, isPhoneField = true)
            val emails = escapeMultiValueCsv(contact.emails)
            val accountType = escapeCsvValue(contact.accountType ?: "")
            val accountName = escapeCsvValue(contact.accountName ?: "")
            val junkType = escapeCsvValue(contact.junkType?.name ?: "")
            val duplicateType = escapeCsvValue(contact.duplicateType?.name ?: "")

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
