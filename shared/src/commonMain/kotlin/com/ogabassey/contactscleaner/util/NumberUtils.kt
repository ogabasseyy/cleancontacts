package com.ogabassey.contactscleaner.util

/**
 * Platform-agnostic number formatting for KMP.
 * Adds commas as thousands separators.
 */
fun Int.formatWithCommas(): String {
    return this.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}

fun Long.formatWithCommas(): String {
    return this.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
