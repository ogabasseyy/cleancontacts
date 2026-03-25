package com.ogabassey.contactscleaner.data.repository

fun sanitizeExportFileName(fileName: String): String {
    if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
        throw IllegalArgumentException("Path traversal attempt detected: $fileName")
    }

    val sb = StringBuilder(fileName.length)
    var hasValidStartChar = false

    for (i in 0 until fileName.length) {
        val c = fileName[i]
        val isValidChar =
            (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || c == '-' || c == '_' || c == '.'
        val mappedChar = if (isValidChar) c else '_'

        if (!hasValidStartChar && (mappedChar == '.' || mappedChar == '_')) {
            continue
        }

        hasValidStartChar = true
        sb.append(mappedChar)
    }

    val sanitized = sb.toString()
    return if (sanitized.isEmpty()) "export.csv" else sanitized
}
