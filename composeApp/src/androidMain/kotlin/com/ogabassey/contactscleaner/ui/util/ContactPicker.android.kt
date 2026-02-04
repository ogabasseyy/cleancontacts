package com.ogabassey.contactscleaner.ui.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android Contact Picker - Apple Guideline 5.1.1 Compliance
 *
 * Uses ActivityResultContracts.PickContact which DOES NOT require
 * READ_CONTACTS permission. The system shows all contacts, user picks one,
 * and only that contact's URI is returned. We can then read basic data
 * from the returned URI without full contacts permission.
 */
class AndroidContactPicker(
    private val context: Context,
    private val launcher: androidx.activity.result.ActivityResultLauncher<Void?>
) : ContactPicker {

    override fun launch() {
        launcher.launch(null)
    }
}

/**
 * Reads contact data from a picked contact URI.
 * This works without READ_CONTACTS permission because the user
 * explicitly picked this contact.
 */
private fun readContactFromUri(context: Context, uri: Uri): PickedContact? {
    var name: String? = null
    var phoneNumber: String? = null
    var email: String? = null
    var contactId: String? = null

    try {
        // Read display name
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (idIndex >= 0) contactId = cursor.getString(idIndex)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
            }
        }

        // Read phone number
        if (contactId != null) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (phoneIndex >= 0) phoneNumber = cursor.getString(phoneIndex)
                }
            }

            // Read email
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    if (emailIndex >= 0) email = cursor.getString(emailIndex)
                }
            }
        }

        return contactId?.let { id ->
            PickedContact(
                id = id,
                name = name,
                phoneNumber = phoneNumber,
                email = email
            )
        }

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
actual fun rememberContactPicker(
    onContactPicked: (PickedContact) -> Unit,
    onCancelled: () -> Unit
): ContactPicker {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val contact = readContactFromUri(context, uri)
            if (contact != null) {
                onContactPicked(contact)
            } else {
                onCancelled()
            }
        } else {
            onCancelled()
        }
    }

    return remember(context, launcher) {
        AndroidContactPicker(context, launcher)
    }
}
