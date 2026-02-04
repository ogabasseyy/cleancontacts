package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable
import com.ogabassey.contactscleaner.domain.model.Contact

/**
 * Contact picked from the system picker.
 * Contains only the data returned by the picker, not full contact info.
 */
data class PickedContact(
    val id: String,
    val name: String?,
    val phoneNumber: String?,
    val email: String?
)

/**
 * Contact Picker - Apple Guideline 5.1.1 Compliance
 *
 * Allows users to pick contacts from the system contact picker WITHOUT
 * requiring READ_CONTACTS permission. The system shows all contacts,
 * and only the selected contact's data is shared with the app.
 *
 * iOS: Uses CNContactPickerViewController
 * Android: Uses ActivityResultContracts.PickContact
 */
interface ContactPicker {
    /**
     * Launch the system contact picker.
     * Result will be delivered via the callback passed to rememberContactPicker.
     */
    fun launch()
}

/**
 * Creates a ContactPicker that delivers picked contacts via callback.
 *
 * @param onContactPicked Called when user picks a contact
 * @param onCancelled Called when user cancels the picker
 */
@Composable
expect fun rememberContactPicker(
    onContactPicked: (PickedContact) -> Unit,
    onCancelled: () -> Unit = {}
): ContactPicker
