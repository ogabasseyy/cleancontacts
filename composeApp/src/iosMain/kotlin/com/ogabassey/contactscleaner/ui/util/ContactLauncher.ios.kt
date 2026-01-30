package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import platform.Contacts.CNContactStore
import platform.ContactsUI.CNContactViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController

class IosContactLauncher : ContactLauncher {
    private val contactStore = CNContactStore()

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    override fun openContact(id: String) {
        val keysToFetch = listOf(CNContactViewController.descriptorForRequiredKeys())
        
        try {
            val contact = contactStore.unifiedContactWithIdentifier(id, keysToFetch, null) ?: return
            
            val contactViewController = CNContactViewController.viewControllerForContact(contact)
            contactViewController.allowsEditing = true
            contactViewController.allowsActions = true

            // Reach into the UIKit hierarchy from Compose
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            val navigationController = UINavigationController(rootViewController = contactViewController)
            
            rootViewController?.presentViewController(navigationController, animated = true, completion = null)
        } catch (e: Exception) {
            println("Error opening iOS contact: ${e.message}")
        }
    }
}

@Composable
actual fun rememberContactLauncher(): ContactLauncher {
    return remember { IosContactLauncher() }
}
