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
 * Extracts digit characters from a string and normalizes them to ASCII.
 * This preserves the old `isDigit()` behavior for Unicode numerals while
 * avoiding the intermediate collection allocations of `filter`.
 */
fun String.extractDigits(): String {
    if (isEmpty()) return ""
    val sb = StringBuilder(length)
    for (i in indices) {
        val c = this[i]
        if (c.isDigit()) sb.append(c.digitToInt().digitToChar())
    }
    return sb.toString()
}

/**
 * Extracts digit characters plus ASCII '+' and normalizes digits to ASCII.
 * This preserves the old `isDigit()` behavior for Unicode numerals while
 * avoiding the intermediate collection allocations of `filter`.
 */
fun String.extractDigitsAndPlus(): String {
    if (isEmpty()) return ""
    val sb = StringBuilder(length)
    for (i in indices) {
        val c = this[i]
        when {
            c == '+' -> sb.append(c)
            c.isDigit() -> sb.append(c.digitToInt().digitToChar())
        }
    }
    return sb.toString()
}
