import SwiftUI
import composeApp

/**
 * iOS Application entry point for Contacts Cleaner.
 *
 * 2026 KMP Best Practice: SwiftUI wrapper for Compose Multiplatform.
 */
class CrashListener {
    static func install() {
        NSSetUncaughtExceptionHandler { exception in
            print("🚨 CRASH CAUGHT 🚨")
            print("Name: \(exception.name)")
            print("Reason: \(exception.reason ?? "Unknown")")
            print("Stack: \(exception.callStackSymbols.joined(separator: "\n"))")
        }
    }
}

@main
struct iOSApp: App {
    init() {
        CrashListener.install()

        // Per RevenueCat docs: configure Purchases early in app lifecycle.
        // App.init() is the earliest point for SwiftUI apps.
        MainViewControllerKt.InitializeRevenueCat()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
