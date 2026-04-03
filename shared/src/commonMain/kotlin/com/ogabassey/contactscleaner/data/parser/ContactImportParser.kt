package com.ogabassey.contactscleaner.data.parser

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ImportResult

class ContactImportParser {

    companion object {
        /**
         * Maximum allowed line length to prevent DoS/memory exhaustion attacks.
         * Lines exceeding this length are silently skipped during parsing.
         */
        const val MAX_LINE_LENGTH = 2000
    }

    fun parseFile(content: String, filename: String): ImportResult {
        val contacts = mutableListOf<Contact>()
        var lineNumber = 0

        content.lineSequence().forEach { line ->
            lineNumber++

            // 2026 Security: Skip excessively long lines to prevent DoS
            if (line.length > MAX_LINE_LENGTH) return@forEach

            val trimmedLine = line.trim()

            if (trimmedLine.isNotEmpty()) {
                // Detect format: CSV vs plain text
                val parsedContact = if (trimmedLine.contains(',')) {
                    parseCSVLine(trimmedLine, lineNumber.toLong())
                } else {
                    parsePlainTextLine(trimmedLine, lineNumber.toLong())
                }

                parsedContact?.let { contacts.add(it) }
            }
        }

        return ImportResult(
            validContacts = contacts.filter { !it.isJunk },
            junkContacts = emptyList(), // Will be detected separately
            duplicates = emptyList() // Will be detected separately
        )
    }

    private fun parseCSVLine(line: String, id: Long): Contact? {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        // ⚡ Bolt Optimization: Reuse single StringBuilder with clear() to avoid allocations
        var inQuotes = false
        
        var i = 0

        while (i < line.length) {
            val char = line[i]

            if (inQuotes) {
                if (char == '\"') {
                    // Check lookahead for escaped quote ("")
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        current.append('\"')
                        i += 2 // Skip both quotes
                    } else {
                        inQuotes = false
                        i++
                    }
                } else {
                    current.append(char)
                    i++
                }
            } else {
                if (char == '\"') {
                    inQuotes = true
                    i++
                } else if (char == ',') {
                    parts.add(current.toString().trim())
                    current.clear()
                    i++
                } else {
                    current.append(char)
                    i++
                }
            }
        }
        parts.add(current.toString().trim())
        
        return when {
            parts.size >= 2 -> {
                Contact(
                    id = id,
                    name = parts[0].ifBlank { null },
                    numbers = listOf(parts[1]),
                    normalizedNumber = parts[1]
                )
            }
            parts.size == 1 -> {
                Contact(
                    id = id,
                    name = null,
                    numbers = listOf(parts[0]),
                    normalizedNumber = parts[0]
                )
            }
            else -> null
        }
    }

    private fun parsePlainTextLine(line: String, id: Long): Contact? {
        // Assume each line is a phone number
        // Simple digit check: .any {} is more efficient than regex for single-character validation
        return if (line.any { it.isDigit() }) {
            Contact(
                id = id,
                name = null,
                numbers = listOf(line),
                normalizedNumber = line
            )
        } else {
            null
        }
    }
}
