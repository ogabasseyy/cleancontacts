package com.ogabassey.contactscleaner.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatStandardizationTargetTest {
    @Test
    fun `uses provider normalized number for local row when provider marks it international`() {
        val target = FormatStandardizationTarget.resolve(
            rawNumber = "08079182146",
            providerNormalizedNumber = "+2348079182146",
            detectMissingPlus = { null }
        )

        assertEquals("+2348079182146", target)
    }

    @Test
    fun `uses detector target when provider normalized number is unavailable`() {
        val target = FormatStandardizationTarget.resolve(
            rawNumber = "2348079182146",
            providerNormalizedNumber = null,
            detectMissingPlus = { "+2348079182146" }
        )

        assertEquals("+2348079182146", target)
    }

    @Test
    fun `skips row that is already international`() {
        val target = FormatStandardizationTarget.resolve(
            rawNumber = "+2348079182146",
            providerNormalizedNumber = "+2348079182146",
            detectMissingPlus = { error("detector should not be called") }
        )

        assertNull(target)
    }

    @Test
    fun `skips blocked special prefixes`() {
        val hashTarget = FormatStandardizationTarget.resolve(
            rawNumber = "#12345",
            providerNormalizedNumber = "+12345",
            detectMissingPlus = { error("detector should not be called") }
        )
        val starTarget = FormatStandardizationTarget.resolve(
            rawNumber = "*12345",
            providerNormalizedNumber = "+12345",
            detectMissingPlus = { error("detector should not be called") }
        )

        assertNull(hashTarget)
        assertNull(starTarget)
    }
}
