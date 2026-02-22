package com.ogabassey.contactscleaner.util

/**
 * Platform-agnostic number formatting for KMP.
 * Adds commas as thousands separators.
 * Optimized for performance: O(N) using StringBuilder instead of multiple string allocations.
 */
fun Int.formatWithCommas(): String {
    val s = toString()
    val isNegative = s.isNotEmpty() && s[0] == '-'
    val limit = if (isNegative) 1 else 0

    // Early return if no commas needed (e.g. 123, -123)
    if (s.length - limit <= 3) return s

    val result = StringBuilder(s.length + (s.length - limit - 1) / 3)
    var count = 0

    // Iterate from end to start (excluding sign)
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

fun Long.formatWithCommas(): String {
    val s = toString()
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
