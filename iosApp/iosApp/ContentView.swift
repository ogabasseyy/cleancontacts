import UIKit
import SwiftUI
import composeApp

/**
 * ContentView that hosts the Compose Multiplatform UI.
 *
 * 2026 KMP Best Practice: Bridge between SwiftUI and Compose.
 */
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
