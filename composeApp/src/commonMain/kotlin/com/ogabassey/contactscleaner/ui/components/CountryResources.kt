package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Immutable

@Immutable
data class CountryCode(
    val name: String,
    val code: String,
    val flag: String,
    val regionIso: String // ISO 3166-1 alpha-2 code (e.g., "US", "NG")
)

object CountryResources {
    val countries = listOf(
        CountryCode("Algeria", "+213", "🇩🇿", "DZ"),
        CountryCode("Argentina", "+54", "🇦🇷", "AR"),
        CountryCode("Australia", "+61", "🇦🇺", "AU"),
        CountryCode("Austria", "+43", "🇦🇹", "AT"),
        CountryCode("Bangladesh", "+880", "🇧🇩", "BD"),
        CountryCode("Belgium", "+32", "🇧🇪", "BE"),
        CountryCode("Brazil", "+55", "🇧🇷", "BR"),
        CountryCode("Cameroon", "+237", "🇨🇲", "CM"),
        CountryCode("Canada", "+1", "🇨🇦", "CA"),
        CountryCode("China", "+86", "🇨🇳", "CN"),
        CountryCode("Colombia", "+57", "🇨🇴", "CO"),
        CountryCode("Denmark", "+45", "🇩🇰", "DK"),
        CountryCode("Egypt", "+20", "🇪🇬", "EG"),
        CountryCode("Ethiopia", "+251", "🇪🇹", "ET"),
        CountryCode("Finland", "+358", "🇫🇮", "FI"),
        CountryCode("France", "+33", "🇫🇷", "FR"),
        CountryCode("Germany", "+49", "🇩🇪", "DE"),
        CountryCode("Ghana", "+233", "🇬🇭", "GH"),
        CountryCode("Greece", "+30", "🇬🇷", "GR"),
        CountryCode("India", "+91", "🇮🇳", "IN"),
        CountryCode("Indonesia", "+62", "🇮🇩", "ID"),
        CountryCode("Ireland", "+353", "🇮🇪", "IE"),
        CountryCode("Israel", "+972", "🇮🇱", "IL"),
        CountryCode("Italy", "+39", "🇮🇹", "IT"),
        CountryCode("Ivory Coast", "+225", "🇨🇮", "CI"),
        CountryCode("Japan", "+81", "🇯🇵", "JP"),
        CountryCode("Kenya", "+254", "🇰🇪", "KE"),
        CountryCode("Malaysia", "+60", "🇲🇾", "MY"),
        CountryCode("Mexico", "+52", "🇲🇽", "MX"),
        CountryCode("Morocco", "+212", "🇲🇦", "MA"),
        CountryCode("Netherlands", "+31", "🇳🇱", "NL"),
        CountryCode("New Zealand", "+64", "🇳🇿", "NZ"),
        CountryCode("Nigeria", "+234", "🇳🇬", "NG"),
        CountryCode("Norway", "+47", "🇳🇴", "NO"),
        CountryCode("Pakistan", "+92", "🇵🇰", "PK"),
        CountryCode("Philippines", "+63", "🇵🇭", "PH"),
        CountryCode("Poland", "+48", "🇵🇱", "PL"),
        CountryCode("Portugal", "+351", "🇵🇹", "PT"),
        CountryCode("Russia", "+7", "🇷🇺", "RU"),
        CountryCode("Saudi Arabia", "+966", "🇸🇦", "SA"),
        CountryCode("Senegal", "+221", "🇸🇳", "SN"),
        CountryCode("Singapore", "+65", "🇸🇬", "SG"),
        CountryCode("South Africa", "+27", "🇿🇦", "ZA"),
        CountryCode("South Korea", "+82", "🇰🇷", "KR"),
        CountryCode("Spain", "+34", "🇪🇸", "ES"),
        CountryCode("Sweden", "+46", "🇸🇪", "SE"),
        CountryCode("Switzerland", "+41", "🇨🇭", "CH"),
        CountryCode("Tanzania", "+255", "🇹🇿", "TZ"),
        CountryCode("Thailand", "+66", "🇹🇭", "TH"),
        CountryCode("Tunisia", "+216", "🇹🇳", "TN"),
        CountryCode("Turkey", "+90", "🇹🇷", "TR"),
        CountryCode("Uganda", "+256", "🇺🇬", "UG"),
        CountryCode("Ukraine", "+380", "🇺🇦", "UA"),
        CountryCode("United Arab Emirates", "+971", "🇦🇪", "AE"),
        CountryCode("United Kingdom", "+44", "🇬🇧", "GB"),
        CountryCode("United States", "+1", "🇺🇸", "US"),
        CountryCode("Vietnam", "+84", "🇻🇳", "VN")
    )

    private val fallbackCountry = countries.first { it.regionIso == "NG" }

    /**
     * Get the default country based on device region.
     * Falls back to Nigeria if region not found in list.
     */
    fun getDefaultCountry(regionIso: String): CountryCode {
        return countries.find { it.regionIso.equals(regionIso, ignoreCase = true) }
            ?: fallbackCountry
    }

    // Legacy support - kept for backwards compatibility
    val defaultCountry: CountryCode get() = fallbackCountry
}
