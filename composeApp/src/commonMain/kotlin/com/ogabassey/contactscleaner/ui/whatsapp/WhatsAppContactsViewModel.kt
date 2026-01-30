package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.api.WhatsAppContact
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for WhatsApp contacts screen.
 */
@Immutable
sealed interface WhatsAppContactsState {
    data object Loading : WhatsAppContactsState
    data object LoadingMore : WhatsAppContactsState
    data object NotConnected : WhatsAppContactsState
    @Immutable data class Loaded(
        val contacts: List<WhatsAppContact>,
        val businessCount: Int,
        val personalCount: Int,
        val totalCount: Int,
        val hasMore: Boolean
    ) : WhatsAppContactsState
    @Immutable data class Error(val message: String) : WhatsAppContactsState
}

/**
 * Tab filter for contacts display.
 */
enum class ContactsTab {
    ALL, BUSINESS, PERSONAL
}

/**
 * ViewModel for WhatsApp contacts screen.
 * Fetches contacts from the linked WhatsApp session and enables export.
 */
class WhatsAppContactsViewModel(
    private val whatsAppRepository: WhatsAppDetectorRepository,
    private val settings: Settings
) : ViewModel() {

    private val _state = MutableStateFlow<WhatsAppContactsState>(WhatsAppContactsState.Loading)
    val state: StateFlow<WhatsAppContactsState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(ContactsTab.ALL)
    val selectedTab: StateFlow<ContactsTab> = _selectedTab.asStateFlow()

    private val _exportData = MutableStateFlow<String?>(null)
    val exportData: StateFlow<String?> = _exportData.asStateFlow()

    private val deviceId: String
        get() = settings.getStringOrNull(KEY_DEVICE_ID) ?: ""

    init {
        loadContacts()
    }

    fun loadContacts() {
        if (deviceId.isEmpty()) {
            _state.update { WhatsAppContactsState.NotConnected }
            return
        }

        viewModelScope.launch {
            _state.update { WhatsAppContactsState.Loading }
            try {
                val status = whatsAppRepository.getSessionStatus(deviceId)
                if (!status.connected) {
                    _state.update { WhatsAppContactsState.NotConnected }
                    return@launch
                }

                val response = whatsAppRepository.getContacts(deviceId)
                if (response.success) {
                    _state.update {
                        WhatsAppContactsState.Loaded(
                            contacts = response.contacts,
                            businessCount = response.businessCount,
                            personalCount = response.personalCount,
                            totalCount = response.total,
                            hasMore = response.contacts.size < response.total
                        )
                    }
                } else {
                    _state.update { WhatsAppContactsState.Error(response.error ?: "Failed to load contacts") }
                }
            } catch (e: Exception) {
                _state.update { WhatsAppContactsState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    fun selectTab(tab: ContactsTab) {
        _selectedTab.update { tab }
    }

    fun getFilteredContacts(): List<WhatsAppContact> {
        val currentState = _state.value
        if (currentState !is WhatsAppContactsState.Loaded) return emptyList()

        return when (_selectedTab.value) {
            ContactsTab.ALL -> currentState.contacts
            ContactsTab.BUSINESS -> currentState.contacts.filter { it.isBusiness }
            ContactsTab.PERSONAL -> currentState.contacts.filter { !it.isBusiness }
        }
    }

    /**
     * Export contacts to CSV format.
     */
    fun exportToCsv(): String {
        val contacts = getFilteredContacts()
        val sb = StringBuilder()
        sb.appendLine("Phone Number,Name,Push Name,Is Business,Category,Email,Website,Address")

        for (contact in contacts) {
            val name = contact.name?.replace(",", ";") ?: ""
            val pushName = contact.pushName?.replace(",", ";") ?: ""
            val category = contact.businessProfile?.category?.replace(",", ";") ?: ""
            val email = contact.businessProfile?.email?.replace(",", ";") ?: ""
            val website = contact.businessProfile?.website?.joinToString(";") ?: ""
            val address = contact.businessProfile?.address?.replace(",", ";")?.replace("\n", " ") ?: ""

            sb.appendLine("${contact.phoneNumber},$name,$pushName,${contact.isBusiness},$category,$email,$website,$address")
        }

        val csv = sb.toString()
        _exportData.update { csv }
        return csv
    }

    /**
     * Export contacts to vCard format.
     */
    fun exportToVCard(): String {
        val contacts = getFilteredContacts()
        val sb = StringBuilder()

        for (contact in contacts) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")

            val displayName = contact.name ?: contact.pushName ?: contact.phoneNumber
            sb.appendLine("FN:$displayName")

            if (contact.name != null) {
                sb.appendLine("N:;${contact.name};;;")
            }

            sb.appendLine("TEL;TYPE=CELL:${contact.phoneNumber}")

            if (contact.isBusiness) {
                sb.appendLine("X-WHATSAPP-BUSINESS:TRUE")
                contact.businessProfile?.let { profile ->
                    profile.category?.let { sb.appendLine("ORG:$it") }
                    profile.email?.let { sb.appendLine("EMAIL:$it") }
                    profile.website?.forEach { sb.appendLine("URL:$it") }
                    profile.address?.let { sb.appendLine("ADR:;;${it.replace("\n", ";")};;;;") }
                    profile.description?.let { sb.appendLine("NOTE:$it") }
                }
            }

            sb.appendLine("END:VCARD")
            sb.appendLine()
        }

        val vcard = sb.toString()
        _exportData.update { vcard }
        return vcard
    }

    fun clearExportData() {
        _exportData.update { null }
    }

    companion object {
        private const val KEY_DEVICE_ID = "whatsapp_device_id"
    }
}
