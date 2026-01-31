package com.ogabassey.contactscleaner.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Contacts.CNContactStore
import platform.ContactsUI.CNContactViewController
import platform.Foundation.NSError
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationItem
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.navigationItem

/**
 * 2026 Best Practice: Singleton handler to prevent garbage collection.
 * Stores active handlers to ensure they remain alive during modal presentation.
 */
private object DismissHandlerRegistry {
    private val activeHandlers = mutableListOf<DismissHandler>()

    fun register(handler: DismissHandler) {
        activeHandlers.add(handler)
    }

    fun unregister(handler: DismissHandler) {
        activeHandlers.remove(handler)
    }
}

/**
 * Helper class to handle modal dismissal from UIBarButtonItem action.
 * 2026 Best Practice: Inherits from NSObject to work with Objective-C selectors.
 */
private class DismissHandler(
    private val navigationController: UINavigationController,
    private val onDismiss: () -> Unit
) : platform.darwin.NSObject() {

    @Suppress("unused") // Called via Objective-C selector
    @ObjCAction
    fun dismissModal() {
        navigationController.dismissViewControllerAnimated(true) {
            onDismiss()
        }
        // Cleanup after dismissal
        DismissHandlerRegistry.unregister(this)
    }
}

class IosContactLauncher(
    private val onDismiss: () -> Unit = {}
) : ContactLauncher {
    private val contactStore = CNContactStore()

    /**
     * Get the root view controller for presenting modals.
     * Uses keyWindow which works on all iOS versions.
     */
    private fun getRootViewController(): UIViewController? {
        // 2026 Note: keyWindow is deprecated but universally available
        // Scene-based APIs have complex Kotlin/Native interop issues
        return UIApplication.sharedApplication.keyWindow?.rootViewController
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun openContact(id: String) {
        val keysToFetch = listOf(CNContactViewController.descriptorForRequiredKeys())

        try {
            // 2026 Fix: Capture NSError for proper error handling
            val contact = memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val result = contactStore.unifiedContactWithIdentifier(id, keysToFetch, errorPtr.ptr)
                val nsError = errorPtr.value
                if (nsError != null) {
                    println("⚠️ Error fetching contact '$id': ${nsError.localizedDescription}")
                    return
                }
                result
            } ?: return

            val contactViewController = CNContactViewController.viewControllerForContact(contact)
            contactViewController.allowsEditing = true
            contactViewController.allowsActions = true

            val rootViewController = getRootViewController()
            if (rootViewController == null) {
                println("⚠️ Cannot present contact viewer: rootViewController is null")
                return
            }

            val navigationController = UINavigationController(rootViewController = contactViewController)

            // 2026 Best Practice: Create handler BEFORE button and register to prevent GC
            val dismissHandler = DismissHandler(navigationController, onDismiss)
            DismissHandlerRegistry.register(dismissHandler)

            // Add Done button with proper target/action
            val doneButton = UIBarButtonItem(
                barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemDone,
                target = dismissHandler,
                action = NSSelectorFromString("dismissModal")
            )

            // 2026 Best Practice: Access navigationItem via extension property
            // Kotlin/Native exposes UIViewController.navigationItem as extension
            contactViewController.navigationItem.rightBarButtonItem = doneButton

            // Present modally
            rootViewController.presentViewController(
                navigationController,
                animated = true,
                completion = null
            )

        } catch (e: Exception) {
            println("Error opening iOS contact: ${e.message}")
        }
    }
}

@Composable
actual fun rememberContactLauncher(onReturn: () -> Unit): ContactLauncher {
    return remember(onReturn) { IosContactLauncher(onDismiss = onReturn) }
}
