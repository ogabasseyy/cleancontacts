package com.ogabassey.contactscleaner.util

/**
 * Splits a string by a given character and returns only the non-blank segments.
 * Eliminates the intermediate List allocation from calling .split().filter { ... }.
 */
fun String.splitAndFilterNotBlank(delimiter: Char = ','): List<String> {
    if (isEmpty()) return emptyList()
    var result: ArrayList<String>? = null
    var startIndex = 0
    while (true) {
        val delimiterIndex = indexOf(delimiter, startIndex)
        val endIndex = if (delimiterIndex == -1) length else delimiterIndex

        var isBlank = true
        for (i in startIndex until endIndex) {
            if (!this[i].isWhitespace()) {
                isBlank = false
                break
            }
        }

        if (!isBlank) {
            if (result == null) result = ArrayList()
            result.add(substring(startIndex, endIndex))
        }

        if (delimiterIndex == -1) break
        startIndex = delimiterIndex + 1
    }
    return result ?: emptyList()
}

/**
 * Finds the first non-blank segment in a string delimited by a character.
 * Eliminates the intermediate List allocation from calling .split().filter { ... }.firstOrNull().
 */
fun String.firstNonBlankSegment(delimiter: Char = ','): String? {
    if (isEmpty()) return null
    var startIndex = 0
    while (true) {
        val delimiterIndex = indexOf(delimiter, startIndex)
        val endIndex = if (delimiterIndex == -1) length else delimiterIndex

        var isBlank = true
        for (i in startIndex until endIndex) {
            if (!this[i].isWhitespace()) {
                isBlank = false
                break
            }
        }

        if (!isBlank) {
            return substring(startIndex, endIndex)
        }

        if (delimiterIndex == -1) break
        startIndex = delimiterIndex + 1
    }
    return null
}
