package com.ogabassey.contactscleaner.util

/**
 * ⚡ Bolt Optimization: Single-pass string splitting that filters out blank entries.
 * Avoids the intermediate List allocations of `split(",").filter { it.isNotBlank() }`
 * and avoids substring allocation for blank segments.
 */
fun String.splitAndFilterNotBlank(delimiter: Char = ','): List<String> {
    if (isEmpty()) return emptyList()

    val result = ArrayList<String>()
    var startIdx = 0
    val len = length

    for (i in 0..len) {
        if (i == len || this[i] == delimiter) {
            var isBlank = true
            for (j in startIdx until i) {
                if (!this[j].isWhitespace()) {
                    isBlank = false
                    break
                }
            }

            if (!isBlank) {
                result.add(substring(startIdx, i))
            }
            startIdx = i + 1
        }
    }

    return result
}
