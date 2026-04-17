package com.ogabassey.contactscleaner.util

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {
    @Test
    fun testSplitAndFilterNotBlank() {
        assertEquals(emptyList(), "".splitAndFilterNotBlank())
        assertEquals(emptyList(), "   ".splitAndFilterNotBlank())
        assertEquals(listOf("a"), "a".splitAndFilterNotBlank())
        assertEquals(listOf(" a ", " b "), " a , b ".splitAndFilterNotBlank())
        assertEquals(listOf(" c"), " , , c,  ".splitAndFilterNotBlank())
        assertEquals(listOf("123", "456"), "123,456".splitAndFilterNotBlank())
    }

    @Test
    fun testFirstNonBlankSegment() {
        assertEquals(null, "".firstNonBlankSegment())
        assertEquals(null, "   ".firstNonBlankSegment())
        assertEquals("a", "a".firstNonBlankSegment())
        assertEquals(" a ", " a , b ".firstNonBlankSegment())
        assertEquals(" c", " , , c,  ".firstNonBlankSegment())
        assertEquals("123", "123,456".firstNonBlankSegment())
    }
}
