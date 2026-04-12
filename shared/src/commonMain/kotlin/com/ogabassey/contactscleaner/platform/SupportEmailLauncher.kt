package com.ogabassey.contactscleaner.platform

expect object SupportEmailLauncher {
    fun composeEmail(address: String, subject: String, body: String): Boolean
}

object SupportEmail {
    const val ADDRESS = "support@contactscleaner.tech"

    fun subjectFor(category: String): String = "Contacts Cleaner Support: $category"

    fun bodyFor(
        category: String,
        message: String,
        email: String,
        deviceInfo: String
    ): String {
        val safeMessage = message.trim().ifBlank { "(no message provided)" }
        val safeEmail = email.trim().ifBlank { "Not provided" }
        val safeDevice = deviceInfo.trim().ifBlank { "Not provided" }

        return buildString {
            appendLine("Category: $category")
            appendLine("Email: $safeEmail")
            appendLine("Device: $safeDevice")
            appendLine()
            appendLine("Message:")
            appendLine(safeMessage)
        }
    }
}
