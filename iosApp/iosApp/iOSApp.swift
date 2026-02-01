import SwiftUI

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
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
