import UIKit
import SwiftUI
import ComposeApp

final class FilamentBridgeFactoryAdapter: FilamentBridgeFactory {
    func createBridge() -> any FilamentBridgeProtocol {
        FilamentBridgeAdapter()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        FilamentBridgeHolder.shared.bridgeFactory = FilamentBridgeFactoryAdapter()
        FilamentBridgeHolder.shared.bridge = nil
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}


