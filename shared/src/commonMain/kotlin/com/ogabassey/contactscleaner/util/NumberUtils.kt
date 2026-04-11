package com.ogabassey.contactscleaner.util

/**
 * Platform-agnostic number formatting for KMP.
 * Adds commas as thousands separators.
 */
fun Int.formatWithCommas(): String = addThousandsSeparators(toString())

fun Long.formatWithCommas(): String = addThousandsSeparators(toString())

// ⚡ Bolt Optimization: Use a single-pass CharArray implementation instead of
// StringBuilder with .reverse() to eliminate redundant string traversals
// and intermediate allocations in high-frequency dashboard rendering.
private fun addThousandsSeparators(s: String): String {
    val isNegative = s.isNotEmpty() && s[0] == '-'
    val limit = if (isNegative) 1 else 0

    val numDigits = s.length - limit
    if (numDigits <= 3) return s

    val numCommas = (numDigits - 1) / 3
    val resultLength = s.length + numCommas
    val result = CharArray(resultLength)

    var destIdx = resultLength - 1
    var count = 0

    for (i in s.length - 1 downTo limit) {
        if (count == 3) {
            result[destIdx--] = ','
            count = 0
        }
        result[destIdx--] = s[i]
        count++
    }

    if (isNegative) {
        result[destIdx] = '-'
    }

    return result.concatToString()
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
