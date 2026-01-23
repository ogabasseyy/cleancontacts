package com.ogabassey.contactscleaner.data.parser

import android.net.Uri
import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.ImportResult
import java.io.BufferedReader
import java.io.InputStream
import javax.inject.Inject

class ContactImportParser @Inject constructor() {

    fun parseFile(inputStream: InputStream, filename: String): ImportResult {
        val contacts = mutableListOf<Contact>()
        var lineNumber = 0

        BufferedReader(inputStream.reader()).use { reader ->
            reader.forEachLine { line ->
                lineNumber++
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
        }

        return ImportResult(
            validContacts = contacts.filter { !it.isJunk },
            junkContacts = emptyList(), // Will be detected separately
            duplicates = emptyList() // Will be detected separately
        )
    }

    private fun parseCSVLine(line: String, id: Long): Contact? {
        val parts = line.split(',').map { it.trim().removeSurrounding("\"") }
        
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
        return if (line.matches(Regex(".*\\d+.*"))) {
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
