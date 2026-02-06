package com.ogabassey.contactscleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilsTest {

    @Test
    fun escapeCsvValue_shouldEscapeFormulaTriggers() {
        // These should be escaped with a single quote to prevent CSV injection
        assertEquals("'=1+1", ExportUtils.escapeCsvValue("=1+1"))
        assertEquals("'+1+1", ExportUtils.escapeCsvValue("+1+1"))
        assertEquals("'-1+1", ExportUtils.escapeCsvValue("-1+1"))
        // Contains comma, so it also gets quoted
        assertEquals("\"'@SUM(1,1)\"", ExportUtils.escapeCsvValue("@SUM(1,1)"))
    }

    @Test
    fun escapeCsvValue_shouldHandleQuotesAndFormulas() {
        // If it has quotes AND starts with a formula trigger
        // Current logic wraps in quotes if special chars exist.
        // New logic should prepend ' then allow standard CSV escaping if needed?
        // Usually, if we prepend ', the cell is treated as text.
        // However, if the content ALSO has commas, it needs to be quoted for CSV structure.
        // Example: =SUM(1,2) -> contains comma.
        // Steps:
        // 1. Prepend ' -> '=SUM(1,2)
        // 2. Check for special chars (comma) -> Yes
        // 3. Quote it -> "'=SUM(1,2)"

        // Let's verify what happens with a complex case
        // Input: =SUM(1,2)
        // Expected: "'=SUM(1,2)" (Note the outer quotes are for CSV format, inner ' is for Excel safety)

        assertEquals("\"'=SUM(1,2)\"", ExportUtils.escapeCsvValue("=SUM(1,2)"))
    }

    @Test
    fun escapeCsvValue_shouldLeaveSafeValuesAlone() {
        assertEquals("John Doe", ExportUtils.escapeCsvValue("John Doe"))
        assertEquals("1234567890", ExportUtils.escapeCsvValue("1234567890"))
    }

    @Test
    fun escapeCsvValue_shouldEscapeQuotesStandard() {
        // Standard CSV escaping: " -> "" and wrap in "
        assertEquals("\"John \"\"The Duke\"\" Doe\"", ExportUtils.escapeCsvValue("John \"The Duke\" Doe"))
        assertEquals("\"Doe, John\"", ExportUtils.escapeCsvValue("Doe, John"))
    }
}
