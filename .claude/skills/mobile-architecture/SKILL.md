---
name: mobile-architecture
description: Where code goes in mobile/ and how the modules connect. Use when adding a class, module, dependency, platform capability, Koin binding, coroutine scope or Gradle change in the Kotlin Multiplatform app, or when judging whether a change respects the layering.
---

# Mobile architecture

The module table and its allowed dependencies live in `mobile/AGENTS.md`. This skill holds the placement decisions that table leaves open.

## Placing new code

Ask in order; the first yes decides.

1. A rule about an Incident that holds on every screen and on the server (`OPEN`-only edits, the photo cap)? `:core:domain`, as a property or function on the model, tested in `commonTest`.
2. Talks to Room, Ktor or the outbox? `:core:data`, `internal`, behind an interface the domain or the data layer already declares (`IncidentRepository`, `LocalStore`, `IncidentRemote`).
3. Decides what a screen shows or what a tap does? A view model in `:feature:incidents`. User-facing text and colour stay in this module.
4. Needs an Android or iOS API? See "Platform code".

`:feature:incidents` and `:core:data` never import each other. They meet only through `:core:domain` interfaces, wired in the app modules.

## Platform code

Common code declares an interface; the platform implements it; the host passes the implementation in.

| Capability | Interface (common) | Android | iOS |
| --- | --- | --- | --- |
| Camera, gallery, location | `PlatformCapabilities` in `:feature:incidents` | `androidApp/.../platform/` | `shared/src/iosMain/.../platform/` |
| Background sync | `SyncScheduler` in `:core:domain` | `WorkManagerSyncScheduler` | `RetryingSyncScheduler` + `BackgroundSync` |
| Database builder | Koin module | `androidDatabaseModule` | `iosDatabaseModule` |

Prefer an interface over `expect`/`actual`: an interface gets a fake in `commonTest`. `expect`/`actual` is used once, for the Room constructor, because Room generates it.

A capability the platform lacks returns `null` from `PlatformCapabilities` and the UI hides the feature. Both platforms get an implementation in the same task, or the handoff records which one is missing.

## Dependency injection

Koin holds the data layer only. `dataModule(baseUrl)` plus one database module plus a `SyncScheduler` binding, started in `FieldReporterApp` (Android) and `IosApp` (iOS).

- A new data-layer class is a `single` in `dataModule`, bound to its interface.
- View models take repositories as constructor parameters and are built with `viewModel { ... }` at the call site in `App.kt`. This keeps `:feature:incidents` free of Koin and lets tests pass fakes directly.
- Only entry points call `GlobalContext.get()`: `MainActivity`, `SyncWorker`, `IosApp`.

## Coroutines and Flow

- Repository and store functions are `suspend` and main-safe. Room moves work off the main thread through `setQueryCoroutineContext`; Ktor suspends. Callers add no `withContext`.
- Reads are cold `Flow`s from Room. A view model turns them into one `StateFlow` with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- One-off UI events go through a `Channel(BUFFERED)` exposed with `receiveAsFlow()`.
- Work that outlives a screen (sync) belongs to the scheduler, never to `viewModelScope`.
- Time and ids are injected (`clock`, `newId`, `undoWindowMs`) so tests control them.
- Shared mutable state is guarded by a `Mutex` (`SyncEngine.running`) or funnelled through a conflated `Channel` (`RetryingSyncScheduler`). Kotlin/Native runs these on real threads.
- Catch the specific exception the callee documents. A broad `catch (e: Exception)` swallows `CancellationException` and breaks cancellation.

## Gradle

- Every version and coordinate lives in `gradle/libs.versions.toml`; module scripts use `libs.` accessors only. One agent edits the catalog at a time.
- KMP library modules use `com.android.kotlin.multiplatform.library` with the `android { }` block inside `kotlin { }`, and `withHostTest {}` to get JVM tests.
- Each module declares `iosArm64()` and `iosSimulatorArm64()`. A new module does the same and is added to `settings.gradle.kts` and to the gate commands in `docs/agents/quality-gates.md`.
- A dependency goes in the narrowest source set that compiles: `commonMain` only when it publishes every target the module declares.
- Room's KSP processor is registered once per target (`kspAndroid`, `kspIosArm64`, `kspIosSimulatorArm64`).
- The configuration cache is on. A build-script change is done when a second run of the same task reports the cache entry reused.

## Done when

Each new type sits in the module the placement questions name, no module gained a dependency outside its row in `mobile/AGENTS.md`, and both platforms compile.
