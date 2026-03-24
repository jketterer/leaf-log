import SwiftUI
import ComposeApp
import ActivityKit
import LeafLogShared

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        return .portrait
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinInitializerKt.doInitKoin()
        if #available(iOS 16.2, *) {
            LiveActivityServiceHolder.shared.instance = LiveActivityManager()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                if #available(iOS 16.2, *) {
                    LiveActivityManager.cleanupStaleActivities()
                }
            }
        }
    }
}