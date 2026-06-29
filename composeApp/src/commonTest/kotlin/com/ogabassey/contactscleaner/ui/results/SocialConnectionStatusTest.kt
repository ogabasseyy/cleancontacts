package com.ogabassey.contactscleaner.ui.results

import kotlin.test.Test
import kotlin.test.assertEquals

class SocialConnectionStatusTest {
    @Test
    fun androidWithoutLinkedWhatsAppOffersAccuracyUpgrade() {
        val status = whatsappSocialConnectionStatus(
            isAndroid = true,
            isConnected = false
        )

        assertEquals("Device-based", status.badge)
        assertEquals("Good, not exact", status.detail)
        assertEquals(SocialConnectionAction.ImproveAccuracy, status.action)
    }

    @Test
    fun androidWithLinkedWhatsAppOffersDisconnect() {
        val status = whatsappSocialConnectionStatus(
            isAndroid = true,
            isConnected = true
        )

        assertEquals("WhatsApp connected", status.badge)
        assertEquals("Exact verification active", status.detail)
        assertEquals(SocialConnectionAction.Disconnect, status.action)
    }
}
