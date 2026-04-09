package com.ogabassey.contactscleaner.util

/**
 * Platform-agnostic number formatting for KMP.
 * Adds commas as thousands separators.
 */
fun Int.formatWithCommas(): String = addThousandsSeparators(toString())

fun Long.formatWithCommas(): String = addThousandsSeparators(toString())

private fun addThousandsSeparators(s: String): String {
    val isNegative = s.isNotEmpty() && s[0] == '-'
    val limit = if (isNegative) 1 else 0

    if (s.length - limit <= 3) return s

    val result = StringBuilder(s.length + (s.length - limit - 1) / 3)
    var count = 0

    for (i in s.length - 1 downTo limit) {
        if (count == 3) {
            result.append(',')
            count = 0
        }
        result.append(s[i])
        count++
    }

    if (isNegative) {
        result.append('-')
    }

    return result.reverse().toString()
}

/**
 * Extracts only ASCII digit characters (0-9) from a string.
 * This differs from `isDigit()` which matches all Unicode digits.
 * This is a highly optimized replacement for `String.filter { it.isDigit() }`
 * that avoids allocating an intermediate List of Characters.
 */
fun String.extractDigits(): String {
    if (isEmpty()) return ""
    val sb = StringBuilder(length)
    for (i in indices) {
        val c = this[i]
        if (c in '0'..'9') sb.append(c)
    }
    return sb.toString()
}

/**
 * Extracts ASCII digits (0-9) and optionally the plus sign ('+') from a string.
 * This differs from `isDigit()` which matches all Unicode digits.
 * Optimized replacement for `String.filter { it.isDigit() || it == '+' }`.
 */
fun String.extractDigitsAndPlus(): String {
    if (isEmpty()) return ""
    val sb = StringBuilder(length)
    for (i in indices) {
        val c = this[i]
        if (c == '+' || c in '0'..'9') sb.append(c)
    }
    return sb.toString()
}
