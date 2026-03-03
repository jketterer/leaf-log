import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
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
    }
}