package com.ogabassey.contactscleaner.util

data class FormatIssueDisplay(
    val sourceNumber: String,
    val normalizedNumber: String
)

fun formatIssueDisplay(
    numbers: List<String>,
    normalizedNumber: String?
): FormatIssueDisplay? {
    val normalized = normalizedNumber?.takeIf { it.isNotBlank() } ?: return null
    val source = numbers.firstOrNull { it.isFormatIssueSourceFor(normalized) } ?: return null

    return FormatIssueDisplay(
        sourceNumber = source,
        normalizedNumber = normalized
    )
}

private fun String.isFormatIssueSourceFor(normalizedNumber: String): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.startsWith("+") || trimmed.startsWith("*") || trimmed.startsWith("#")) return false

    val sourceDigits = trimmed.extractDigits()
    val normalizedDigits = normalizedNumber.extractDigits()
    if (sourceDigits.isEmpty() || normalizedDigits.isEmpty()) return false

    if (sourceDigits == normalizedDigits) return true

    val localDigits = sourceDigits.trimStart('0')
    return localDigits.length >= 7 && normalizedDigits.endsWith(localDigits)
}
