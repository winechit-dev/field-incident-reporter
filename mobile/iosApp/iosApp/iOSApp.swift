import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // iOS requires background tasks to be registered before launch finishes.
        IosAppKt.registerBackgroundTasks()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}