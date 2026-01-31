package com.ogabassey.contactscleaner.ui.whatsapp

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ogabassey.contactscleaner.data.api.BusinessDetectionProgress
import com.ogabassey.contactscleaner.data.api.WhatsAppContact
import com.ogabassey.contactscleaner.data.db.dao.ContactDao
import com.ogabassey.contactscleaner.domain.repository.WhatsAppDetectorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
        val nonWhatsAppCount: Int = 0,
        val hasMore: Boolean,
        val businessDetectionProgress: BusinessDetectionProgress? = null
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
    private val settings: Settings,
    private val contactDao: ContactDao
) : ViewModel() {

    private val _state = MutableStateFlow<WhatsAppContactsState>(WhatsAppContactsState.Loading)
    val state: StateFlow<WhatsAppContactsState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(ContactsTab.ALL)
    val selectedTab: StateFlow<ContactsTab> = _selectedTab.asStateFlow()

    private val _exportData = MutableStateFlow<String?>(null)
    val exportData: StateFlow<String?> = _exportData.asStateFlow()

    // 2026 Best Practice: Track polling job to prevent multiple concurrent pollers
    private var businessDetectionJob: Job? = null

    /**
     * 2026 Best Practice: Expose filtered contacts as a derived StateFlow.
     * Computed once per state change, not on every recomposition.
     */
    val filteredContacts: StateFlow<List<WhatsAppContact>> = combine(
        _state,
        _selectedTab
    ) { state, tab ->
        if (state !is WhatsAppContactsState.Loaded) {
            emptyList()
        } else {
            when (tab) {
                ContactsTab.ALL -> state.contacts
                ContactsTab.BUSINESS -> state.contacts.filter { it.isBusiness }
                ContactsTab.PERSONAL -> state.contacts.filter { !it.isBusiness }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    // 2026 Best Practice: Calculate non-WhatsApp count from phone contacts
                    // This shows phone contacts that don't have WhatsApp accounts
                    val totalPhoneContacts = contactDao.countTotal()
                    val phoneWhatsAppCount = contactDao.countWhatsApp()
                    val nonWhatsAppCount = totalPhoneContacts - phoneWhatsAppCount

                    _state.update {
                        WhatsAppContactsState.Loaded(
                            contacts = response.contacts,
                            businessCount = response.businessCount,
                            personalCount = response.personalCount,
                            totalCount = response.total,
                            nonWhatsAppCount = nonWhatsAppCount.coerceAtLeast(0),
                            hasMore = response.contacts.size < response.total,
                            businessDetectionProgress = status.businessDetectionProgress
                        )
                    }

                    // 2026 Best Practice: Poll for business detection progress if still in progress
                    if (status.businessDetectionProgress?.inProgress == true) {
                        startBusinessDetectionPolling()
                    }
                } else {
                    _state.update { WhatsAppContactsState.Error(response.error ?: "Failed to load contacts") }
                }
            } catch (e: Exception) {
                _state.update { WhatsAppContactsState.Error(e.message ?: "Unknown error") }
            }
        }
    }

    /**
     * 2026 Best Practice: Poll for business detection progress updates.
     * Refreshes every 5 seconds while detection is in progress.
     * Uses isActive for cooperative cancellation.
     */
    private fun startBusinessDetectionPolling() {
        // 2026 Best Practice: Cancel any existing polling job to prevent multiple concurrent pollers
        businessDetectionJob?.cancel()
        businessDetectionJob = viewModelScope.launch {
            // 2026 Best Practice: Use isActive for cooperative cancellation
            while (isActive) {
                delay(5000) // Poll every 5 seconds

                try {
                    val status = whatsAppRepository.getSessionStatus(deviceId)
                    val progress = status.businessDetectionProgress

                    // Update progress in current state
                    _state.update { currentState ->
                        if (currentState is WhatsAppContactsState.Loaded) {
                            currentState.copy(businessDetectionProgress = progress)
                        } else {
                            currentState
                        }
                    }

                    // Stop polling when detection is complete
                    if (progress?.done == true || progress?.inProgress != true) {
                        // Reload contacts to get final business flags
                        loadContacts()
                        break
                    }
                } catch (e: CancellationException) {
                    // 2026 Best Practice: Always rethrow CancellationException
                    throw e
                } catch (e: Exception) {
                    // Ignore polling errors, continue polling
                }
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
     * 2026 Fix: Properly escape CSV values to handle quotes, commas, and newlines.
     */
    fun exportToCsv(): String {
        val contacts = getFilteredContacts()
        val sb = StringBuilder()
        sb.appendLine("Phone Number,Name,Push Name,Is Business,Category,Email,Website,Address")

        for (contact in contacts) {
            // 2026 Fix: Also escape phoneNumber - it may contain special characters
            val phoneNumber = escapeCsvValue(contact.phoneNumber)
            val name = escapeCsvValue(contact.name ?: "")
            val pushName = escapeCsvValue(contact.pushName ?: "")
            val category = escapeCsvValue(contact.businessProfile?.category ?: "")
            val email = escapeCsvValue(contact.businessProfile?.email ?: "")
            val website = escapeCsvValue(contact.businessProfile?.website?.joinToString(";") ?: "")
            val address = escapeCsvValue(contact.businessProfile?.address ?: "")

            sb.appendLine("$phoneNumber,$name,$pushName,${contact.isBusiness},$category,$email,$website,$address")
        }

        val csv = sb.toString()
        _exportData.update { csv }
        return csv
    }

    /**
     * 2026 Best Practice: Proper RFC 4180 CSV escaping.
     * Wraps field in quotes if it contains special characters, and escapes internal quotes.
     */
    private fun escapeCsvValue(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * 2026 Best Practice: Proper RFC 6350 vCard escaping.
     * Escapes backslash, semicolon, comma, and newlines per vCard 3.0/4.0 spec.
     */
    private fun escapeVCardValue(value: String): String {
        return value
            .replace("\\", "\\\\")  // Escape backslash first
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
    }

    /**
     * Export contacts to vCard format.
     * 2026 Fix: Properly escape vCard values per RFC 6350.
     */
    fun exportToVCard(): String {
        val contacts = getFilteredContacts()
        val sb = StringBuilder()

        for (contact in contacts) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")

            val displayName = contact.name ?: contact.pushName ?: contact.phoneNumber
            sb.appendLine("FN:${escapeVCardValue(displayName)}")

            // 2026 Fix: Use local variable for smart cast across module boundaries
            val contactName = contact.name
            if (contactName != null) {
                sb.appendLine("N:;${escapeVCardValue(contactName)};;;")
            }

            sb.appendLine("TEL;TYPE=CELL:${contact.phoneNumber}")

            if (contact.isBusiness) {
                sb.appendLine("X-WHATSAPP-BUSINESS:TRUE")
                contact.businessProfile?.let { profile ->
                    profile.category?.let { sb.appendLine("ORG:${escapeVCardValue(it)}") }
                    profile.email?.let { sb.appendLine("EMAIL:$it") }
                    profile.website?.forEach { sb.appendLine("URL:$it") }
                    profile.address?.let { sb.appendLine("ADR:;;${escapeVCardValue(it)};;;;") }
                    profile.description?.let { sb.appendLine("NOTE:${escapeVCardValue(it)}") }
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
