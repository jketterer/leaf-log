import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var renderKey = 0

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .id(renderKey)
            .onChange(of: scenePhase) { phase in
                if phase == .active {
                    renderKey += 1
                }
            }
    }
}



