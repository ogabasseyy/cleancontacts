package com.ogabassey.contactscleaner.domain.model

data class PaywallPackage(
    val id: String,
    val title: String,
    val price: String, // Localized price string (e.g., "$9.99", "€8.99")
    val description: String? = null,
    val identifier: String // monthly, annual, lifetime
)
