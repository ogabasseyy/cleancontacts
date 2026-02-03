package com.ogabassey.contactscleaner.data.parser

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ImportResult

class ContactImportParser {

    private companion object {
        // 2026 Security: Limit line length to prevent DoS/memory exhaustion
        const val MAX_LINE_LENGTH = 2000
    }

    fun parseFile(content: String, filename: String): ImportResult {
        val contacts = mutableListOf<Contact>()
        var lineNumber = 0

        content.lineSequence().forEach { line ->
            lineNumber++

            // 2026 Security: Skip excessively long lines
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
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    parts.add(current.toString().trim().removeSurrounding("\""))
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        parts.add(current.toString().trim().removeSurrounding("\""))

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
        // 2026 Optimization: Use manual check instead of Regex(".*\\d+.*") to prevent ReDoS
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
