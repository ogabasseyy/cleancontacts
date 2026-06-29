package com.ogabassey.contactscleaner.ui.results

data class SocialConnectionStatus(
    val badge: String,
    val detail: String,
    val action: SocialConnectionAction
)

enum class SocialConnectionAction {
    ImproveAccuracy,
    Link,
    Disconnect
}

fun whatsappSocialConnectionStatus(
    isAndroid: Boolean,
    isConnected: Boolean
): SocialConnectionStatus {
    return when {
        isConnected -> SocialConnectionStatus(
            badge = "WhatsApp connected",
            detail = "Exact verification active",
            action = SocialConnectionAction.Disconnect
        )
        isAndroid -> SocialConnectionStatus(
            badge = "Device-based",
            detail = "Good, not exact",
            action = SocialConnectionAction.ImproveAccuracy
        )
        else -> SocialConnectionStatus(
            badge = "Not linked",
            detail = "Connect WhatsApp for exact verification",
            action = SocialConnectionAction.Link
        )
    }
}
