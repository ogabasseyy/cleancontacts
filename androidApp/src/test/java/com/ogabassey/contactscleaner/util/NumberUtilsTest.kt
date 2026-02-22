package com.ogabassey.contactscleaner.util

import org.junit.Test
import org.junit.Assert.assertEquals

class NumberUtilsTest {

    @Test
    fun `formatWithCommas formats small positive numbers correctly`() {
        assertEquals("0", 0.formatWithCommas())
        assertEquals("123", 123.formatWithCommas())
        assertEquals("999", 999.formatWithCommas())
    }

    @Test
    fun `formatWithCommas formats large positive numbers correctly`() {
        assertEquals("1,234", 1234.formatWithCommas())
        assertEquals("1,000,000", 1000000.formatWithCommas())
        assertEquals("123,456,789", 123456789.formatWithCommas())
    }

    @Test
    fun `formatWithCommas formats negative numbers correctly`() {
        // Previous implementation had a bug where -123456 became -,123,456
        // New implementation fixes this.
        assertEquals("-1,234", (-1234).formatWithCommas())
        assertEquals("-123,456", (-123456).formatWithCommas())
        assertEquals("-1,234,567", (-1234567).formatWithCommas())
    }

    @Test
    fun `formatWithCommas formats Long correctly`() {
        assertEquals("1,234,567,890", 1234567890L.formatWithCommas())
        // Long.MIN_VALUE is -9223372036854775808
        // -9,223,372,036,854,775,808
        assertEquals("-9,223,372,036,854,775,808", Long.MIN_VALUE.formatWithCommas())
    }
}
