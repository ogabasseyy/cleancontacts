package com.ogabassey.contactscleaner.ui.whatsapp

object WhatsAppAccuracyPolicy {
    fun shouldRunCloudAccuracyAfterLink(isAndroid: Boolean): Boolean = isAndroid
}
