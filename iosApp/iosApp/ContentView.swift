import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let refreshTrigger: Int

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        guard let superview = uiViewController.view.superview else { return }
        let frame = uiViewController.view.frame
        uiViewController.view.removeFromSuperview()
        superview.addSubview(uiViewController.view)
        uiViewController.view.frame = frame
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



