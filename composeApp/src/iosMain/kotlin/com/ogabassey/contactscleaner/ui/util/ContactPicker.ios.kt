package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.cinterop.BetaInteropApi
import platform.Contacts.CNContact
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber
import platform.ContactsUI.CNContactPickerDelegateProtocol
import platform.ContactsUI.CNContactPickerViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject

/**
 * Registry to prevent delegate garbage collection during picker presentation.
 *
 * 2026 Best Practice: Thread-safe using AtomicReference with immutable list.
 * Uses CAS (compare-and-swap) loop for lock-free thread safety.
 */
@OptIn(ExperimentalAtomicApi::class)
private object ContactPickerDelegateRegistry {
    private val activeDelegates = AtomicReference(listOf<ContactPickerDelegate>())

    fun register(delegate: ContactPickerDelegate) {
        while (true) {
            val current = activeDelegates.load()
            if (activeDelegates.compareAndSet(current, current + delegate)) break
        }
    }

    fun unregister(delegate: ContactPickerDelegate) {
        while (true) {
            val current = activeDelegates.load()
            if (activeDelegates.compareAndSet(current, current - delegate)) break
        }
    }
}

/**
 * iOS Contact Picker - Apple Guideline 5.1.1 Compliance
 *
 * Uses CNContactPickerViewController which DOES NOT require authorization.
 * The system shows all contacts, user picks one, and only that contact
 * is shared with the app.
 */
class IosContactPicker(
    private val onContactPicked: (PickedContact) -> Unit,
    private val onCancelled: () -> Unit
) : ContactPicker {

    override fun launch() {
        val picker = CNContactPickerViewController()

        // Create and register delegate to prevent GC
        val delegate = ContactPickerDelegate(
            onContactPicked = { contact ->
                onContactPicked(contact)
            },
            onCancelled = onCancelled,
            onCleanup = { delegate ->
                ContactPickerDelegateRegistry.unregister(delegate)
            }
        )
        ContactPickerDelegateRegistry.register(delegate)
        picker.delegate = delegate

        // Get root view controller
        val rootVC = getRootViewController()
        if (rootVC != null) {
            rootVC.presentViewController(picker, animated = true, completion = null)
        } else {
            // 2026 Fix: Handle null rootViewController by notifying user
            ContactPickerDelegateRegistry.unregister(delegate)
            onCancelled()
        }
    }

    private fun getRootViewController(): UIViewController? {
        return UIApplication.sharedApplication.keyWindow?.rootViewController
    }
}

/**
 * Delegate to handle CNContactPickerViewController callbacks.
 */
@OptIn(BetaInteropApi::class)
private class ContactPickerDelegate(
    private val onContactPicked: (PickedContact) -> Unit,
    private val onCancelled: () -> Unit,
    private val onCleanup: (ContactPickerDelegate) -> Unit
) : NSObject(), CNContactPickerDelegateProtocol {

    override fun contactPicker(picker: CNContactPickerViewController, didSelectContact: CNContact) {
        val contact = didSelectContact

        // Extract name
        val givenName = contact.givenName
        val familyName = contact.familyName
        val fullName = "$givenName $familyName".trim().ifEmpty { null }

        // Extract first phone number
        @Suppress("UNCHECKED_CAST")
        val phoneNumbers = contact.phoneNumbers as? List<CNLabeledValue>
        val firstPhone = phoneNumbers?.firstOrNull()?.value as? CNPhoneNumber
        val phoneNumber = firstPhone?.stringValue

        // Extract first email
        @Suppress("UNCHECKED_CAST")
        val emails = contact.emailAddresses as? List<CNLabeledValue>
        val firstEmail = emails?.firstOrNull()?.value?.toString()

        val picked = PickedContact(
            id = contact.identifier,
            name = fullName,
            phoneNumber = phoneNumber,
            email = firstEmail
        )

        onContactPicked(picked)
        onCleanup(this)
    }

    override fun contactPickerDidCancel(picker: CNContactPickerViewController) {
        onCancelled()
        onCleanup(this)
    }
}

@Composable
actual fun rememberContactPicker(
    onContactPicked: (PickedContact) -> Unit,
    onCancelled: () -> Unit
): ContactPicker {
    // 2026 Best Practice: Use rememberUpdatedState for lambda stability.
    // This prevents recreation of IosContactPicker when callers pass inline lambdas,
    // while ensuring we always invoke the latest callback values.
    val currentOnContactPicked = rememberUpdatedState(onContactPicked)
    val currentOnCancelled = rememberUpdatedState(onCancelled)

    return remember {
        IosContactPicker(
            onContactPicked = { contact -> currentOnContactPicked.value(contact) },
            onCancelled = { currentOnCancelled.value() }
        )
    }
}
