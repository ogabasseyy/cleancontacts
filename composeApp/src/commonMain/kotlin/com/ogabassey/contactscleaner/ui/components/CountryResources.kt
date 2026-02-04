package com.ogabassey.contactscleaner.ui.components

import androidx.compose.runtime.Immutable

@Immutable
data class CountryCode(
    val name: String,
    val code: String,
    val flag: String,
    val regionIso: String, // ISO 3166-1 alpha-2 code (e.g., "US", "NG")
    val localDigits: Int   // Expected local number length (after country code)
)

object CountryResources {
    // Hardcoded fallback ensures initialization never fails even if list is modified
    private val DEFAULT_FALLBACK = CountryCode(
        name = "Nigeria",
        code = "+234",
        flag = "\uD83C\uDDF3\uD83C\uDDEC",
        regionIso = "NG",
        localDigits = 10
    )

    // Local digit counts sourced from ITU-T E.164 and telecom standards
    val countries = listOf(
        CountryCode("Algeria", "+213", "🇩🇿", "DZ", 9),
        CountryCode("Argentina", "+54", "🇦🇷", "AR", 10),
        CountryCode("Australia", "+61", "🇦🇺", "AU", 9),
        CountryCode("Austria", "+43", "🇦🇹", "AT", 10),
        CountryCode("Bangladesh", "+880", "🇧🇩", "BD", 10),
        CountryCode("Belgium", "+32", "🇧🇪", "BE", 9),
        CountryCode("Brazil", "+55", "🇧🇷", "BR", 11),
        CountryCode("Cameroon", "+237", "🇨🇲", "CM", 9),
        CountryCode("Canada", "+1", "🇨🇦", "CA", 10),
        CountryCode("China", "+86", "🇨🇳", "CN", 11),
        CountryCode("Colombia", "+57", "🇨🇴", "CO", 10),
        CountryCode("Denmark", "+45", "🇩🇰", "DK", 8),
        CountryCode("Egypt", "+20", "🇪🇬", "EG", 10),
        CountryCode("Ethiopia", "+251", "🇪🇹", "ET", 9),
        CountryCode("Finland", "+358", "🇫🇮", "FI", 9),
        CountryCode("France", "+33", "🇫🇷", "FR", 9),
        CountryCode("Germany", "+49", "🇩🇪", "DE", 11),
        CountryCode("Ghana", "+233", "🇬🇭", "GH", 9),
        CountryCode("Greece", "+30", "🇬🇷", "GR", 10),
        CountryCode("India", "+91", "🇮🇳", "IN", 10),
        CountryCode("Indonesia", "+62", "🇮🇩", "ID", 10),
        CountryCode("Ireland", "+353", "🇮🇪", "IE", 9),
        CountryCode("Israel", "+972", "🇮🇱", "IL", 9),
        CountryCode("Italy", "+39", "🇮🇹", "IT", 10),
        CountryCode("Ivory Coast", "+225", "🇨🇮", "CI", 10),
        CountryCode("Japan", "+81", "🇯🇵", "JP", 10),
        CountryCode("Kenya", "+254", "🇰🇪", "KE", 9),
        CountryCode("Malaysia", "+60", "🇲🇾", "MY", 10),
        CountryCode("Mexico", "+52", "🇲🇽", "MX", 10),
        CountryCode("Morocco", "+212", "🇲🇦", "MA", 9),
        CountryCode("Netherlands", "+31", "🇳🇱", "NL", 9),
        CountryCode("New Zealand", "+64", "🇳🇿", "NZ", 9),
        CountryCode("Nigeria", "+234", "🇳🇬", "NG", 10),
        CountryCode("Norway", "+47", "🇳🇴", "NO", 8),
        CountryCode("Pakistan", "+92", "🇵🇰", "PK", 10),
        CountryCode("Philippines", "+63", "🇵🇭", "PH", 10),
        CountryCode("Poland", "+48", "🇵🇱", "PL", 9),
        CountryCode("Portugal", "+351", "🇵🇹", "PT", 9),
        CountryCode("Russia", "+7", "🇷🇺", "RU", 10),
        CountryCode("Saudi Arabia", "+966", "🇸🇦", "SA", 9),
        CountryCode("Senegal", "+221", "🇸🇳", "SN", 9),
        CountryCode("Singapore", "+65", "🇸🇬", "SG", 8),
        CountryCode("South Africa", "+27", "🇿🇦", "ZA", 9),
        CountryCode("South Korea", "+82", "🇰🇷", "KR", 10),
        CountryCode("Spain", "+34", "🇪🇸", "ES", 9),
        CountryCode("Sweden", "+46", "🇸🇪", "SE", 9),
        CountryCode("Switzerland", "+41", "🇨🇭", "CH", 9),
        CountryCode("Tanzania", "+255", "🇹🇿", "TZ", 9),
        CountryCode("Thailand", "+66", "🇹🇭", "TH", 9),
        CountryCode("Tunisia", "+216", "🇹🇳", "TN", 8),
        CountryCode("Turkey", "+90", "🇹🇷", "TR", 10),
        CountryCode("Uganda", "+256", "🇺🇬", "UG", 9),
        CountryCode("Ukraine", "+380", "🇺🇦", "UA", 9),
        CountryCode("United Arab Emirates", "+971", "🇦🇪", "AE", 9),
        CountryCode("United Kingdom", "+44", "🇬🇧", "GB", 10),
        CountryCode("United States", "+1", "🇺🇸", "US", 10),
        CountryCode("Vietnam", "+84", "🇻🇳", "VN", 9)
    )

    // Defensive lookup: firstOrNull + Elvis operator prevents NoSuchElementException
    private val fallbackCountry = countries.firstOrNull { it.regionIso == "NG" }
        ?: DEFAULT_FALLBACK

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
