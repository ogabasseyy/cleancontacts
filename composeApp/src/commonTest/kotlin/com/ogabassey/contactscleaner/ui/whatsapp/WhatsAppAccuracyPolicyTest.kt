package com.ogabassey.contactscleaner.ui.whatsapp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsAppAccuracyPolicyTest {
    @Test
    fun androidRunsCloudAccuracyAfterLinking() {
        assertTrue(WhatsAppAccuracyPolicy.shouldRunCloudAccuracyAfterLink(isAndroid = true))
    }

    @Test
    fun iosDoesNotRunExtraCloudAccuracyAfterDirectLinking() {
        assertFalse(WhatsAppAccuracyPolicy.shouldRunCloudAccuracyAfterLink(isAndroid = false))
    }
}
