package com.ogabassey.contactscleaner.data.detector

import com.ogabassey.contactscleaner.domain.model.Contact
import com.ogabassey.contactscleaner.domain.model.JunkContact
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class JunkDetector @Inject constructor() {

    private val model: GenerativeModel? by lazy {
        try {
            Generation.getClient()
        } catch (e: Exception) {
            android.util.Log.e("JunkDetector", "Failed to initialize GenAI model", e)
            null
        }
    }

    fun detectJunk(contacts: List<Contact>): List<JunkContact> {
        val junkContacts = mutableListOf<JunkContact>()

        contacts.forEach { contact ->
            val type = getJunkType(contact.name, contact.numbers.firstOrNull())
            if (type != null) {
                junkContacts.add(
                    JunkContact(
                        id = contact.id,
                        name = contact.name,
                        number = contact.numbers.firstOrNull(),
                        type = type
                    )
                )
            }
        }

        return junkContacts
    }

    /**
     * AI-powered smart scan (2026 Best Practice: On-device Gemini Nano)
     */
    suspend fun smartScan(contacts: List<Contact>): List<JunkContact> {
        if (contacts.isEmpty()) return emptyList()
        return detectJunk(contacts) 
    }

    fun getJunkType(name: String?, number: String?): com.ogabassey.contactscleaner.domain.model.JunkType? {
        // 1. Missing Info
        if (name.isNullOrBlank()) return com.ogabassey.contactscleaner.domain.model.JunkType.NO_NAME
        if (number.isNullOrBlank()) return com.ogabassey.contactscleaner.domain.model.JunkType.NO_NUMBER
        
        // 2. Number Analysis
        if (number != null) {
            val cleanedNumber = number.replace(Regex("[^0-9]"), "")
            
            // Invalid Chars (anything not digist, +, -, space, brackets)
            if (Regex("[^0-9+\\s()\\-]").containsMatchIn(number)) {
                return com.ogabassey.contactscleaner.domain.model.JunkType.INVALID_CHAR
            }
            
            // Short Number (< 5 digits)
            if (cleanedNumber.length < 5) {
                return com.ogabassey.contactscleaner.domain.model.JunkType.SHORT_NUMBER
            }
            
            // Long Number (> 15 digits)
            if (cleanedNumber.length > 15) {
                return com.ogabassey.contactscleaner.domain.model.JunkType.LONG_NUMBER
            }
            
            // Repetitive Digits (e.g. 111111)
            if (Regex("(\\d)\\1{5,}").containsMatchIn(cleanedNumber)) {
                return com.ogabassey.contactscleaner.domain.model.JunkType.REPETITIVE_DIGITS
            }
        }

        // 3. Name Analysis
        if (!name.isNullOrBlank()) {
             // Symbol Only Names (e.g. "...", "!!!", emoji)
             // \p{So} = Symbol, Other (emoji, etc)
             // \p{Punct} = Punctuation
            val emojiSymbolOnlyRegex = Regex("^[\\p{So}\\p{Punct}\\s\\d]+$")
            if (emojiSymbolOnlyRegex.matches(name)) {
                return com.ogabassey.contactscleaner.domain.model.JunkType.SYMBOL_NAME
            }
        }

        return null
    }

    // Deprecated string-based method for compatibility if needed, but we should move to Enum
    fun getJunkReason(name: String?, number: String?): String? {
        return getJunkType(name, number)?.name
    }

    private fun getJunkType(contact: Contact): com.ogabassey.contactscleaner.domain.model.JunkType? {
        return getJunkType(contact.name, contact.numbers.firstOrNull())
    }
}
