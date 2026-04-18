package com.ogabassey.contactscleaner.util

/**
 * Splits a string by a delimiter and filters out blank resulting strings in a single pass.
 * This avoids intermediate List allocations from `split(",").filter { it.isNotBlank() }`.
 */
fun String.splitAndFilterNotBlank(delimiter: Char = ','): List<String> {
    if (isEmpty()) return emptyList()

    val result = ArrayList<String>()
    var startIndex = 0
    var i = 0

    while (i < length) {
        if (this[i] == delimiter) {
            val sub = substring(startIndex, i)
            if (sub.isNotBlank()) {
                result.add(sub)
            }
            startIndex = i + 1
        }
        i++
    }

    if (startIndex < length) {
        val sub = substring(startIndex, length)
        if (sub.isNotBlank()) {
            result.add(sub)
        }
    }

    return result
}
