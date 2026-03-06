import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let refreshTrigger: Int

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        redrawAllSubviews(uiViewController.view)
    }

    private func redrawAllSubviews(_ view: UIView) {
        view.setNeedsDisplay()
        view.layer.setNeedsDisplay()
        view.subviews.forEach { redrawAllSubviews($0) }
    }
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var refreshTrigger = 0

    var body: some View {
        ComposeView(refreshTrigger: refreshTrigger)
            .ignoresSafeArea()
            .onChange(of: scenePhase) { phase in
                if phase == .active {
                    refreshTrigger += 1
                }
            }
    }
}



