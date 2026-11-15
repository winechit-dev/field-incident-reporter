# Shared Compose Multiplatform UI, built Android-first

The assignment asks for an "Android Native" app, but we want an iOS client too. We use Kotlin Multiplatform with Compose Multiplatform, so Android and iOS share the UI, view models, domain and data layers. Platform-specific capabilities (camera and photo picker, location, image compression, background sync) sit behind small interfaces with native Android and iOS implementations. Android is built and verified first, and iOS follows through the shared UI.

## Considered options

- **Pure Android, single module:** simplest and the clearest "Android Native" signal, but gives no iOS client.
- **KMP with native UI (Jetpack Compose and SwiftUI):** keeps each UI native, but doubles the UI work for full iOS parity.
- **Compose Multiplatform shared UI (chosen):** one UI codebase gives iOS parity cheaply, while Android still runs real Compose, CameraX, WorkManager and Room.

## Consequences

- The Android app is still a native Android app, but reviewers should read the README to see that this was deliberate.
- Business rules (the `OPEN`-only edit rule, sync state logic) live once, in shared code.
- iOS cannot be verified without a Mac and Xcode, so it is built after Android.
- Moving away from this later means rewriting the UI layer, so the decision is costly to reverse.
