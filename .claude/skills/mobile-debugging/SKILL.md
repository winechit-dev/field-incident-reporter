---
name: mobile-debugging
description: Diagnosis loop for the mobile app. Use when a Gradle build, KSP or Room step, iOS link or test fails for a reason that is not obvious, when the app crashes or shows wrong data on the emulator or simulator, when sync gets stuck, or when a screen is slow or janky.
---

# Mobile debugging

Reproduce, narrow, prove, fix. A fix without a reproduction that went red and then green is a guess.

## 1. Reproduce

Get the smallest command that fails every time and write it in the handoff.

- Build or test failure: the single Gradle task, with `--console=plain --stacktrace`. Read the first error, not the last; later ones are usually fallout.
- Behaviour on a device: the exact steps, the user chosen, and whether the device was online.

**Done when** one command or one list of steps fails on demand.

## 2. Narrow

| Symptom | Look first at |
| --- | --- |
| Unresolved reference in `commonMain` | An Android- or JVM-only API in common code. Move it behind an interface (see `mobile-architecture`). |
| Compiles on Android, fails on `iosSimulatorArm64` | A dependency with no iOS artifact, or `java.*` in `commonMain` or `commonTest`. |
| Room or KSP error | The exported schema versus the entities; a missing `ksp<Target>` registration; an `@Entity` changed without a version bump. |
| iOS framework link error | A type exported from `:shared` that is `internal`, or a new module not reachable from `:shared`. |
| Gradle fails only on the second run | A build script reading state the configuration cache cannot store. |
| Test passes alone, fails in the suite | Shared state between tests: a singleton, `Dispatchers.setMain` without reset, a reused `Harness`. |
| App cannot reach the API | The base URL: `10.0.2.2:3000` on the Android emulator, `localhost:3000` on the iOS simulator; the backend running (`npm start` in `backend/`). |
| Incident stuck on "Waiting to sync" | The outbox head. `SyncEngine.push` stops at the first op that fails with `Network` or `Unauthorized`; nothing behind it moves. |
| Incident shows "Sync failed" | Expected after a `409` or another `4xx`. Check the `RemoteException` mapping in `KtorIncidentRemote.send` against the response. |
| Screen resets on rotation or after backgrounding | State held in `remember` that needed a view model or `rememberSaveable`. |
| Crash on photo or location | A permission or launcher path in `AndroidPlatformCapabilities` / `IosPlatformCapabilities`. |

Device evidence:

- Android log: `adb logcat -d -t 300 '*:W'`, or filtered to the app: `adb logcat --pid=$(adb shell pidof dev.fieldreporter)`.
- Local data: `adb shell run-as dev.fieldreporter ls databases/` then pull `field_reporter.db` and read the `outbox` and `incidents` tables with `sqlite3`.
- Background sync: `adb shell dumpsys jobscheduler | grep dev.fieldreporter` shows whether WorkManager has the job queued and why it is waiting.
- Offline: `adb shell svc wifi disable && adb shell svc data disable`, and re-enable after.
- iOS simulator log: `xcrun simctl spawn booted log stream --predicate 'process == "iosApp"'`.

**Done when** you can name the one line, rule or configuration value responsible, and changing it alone flips the reproduction.

## 3. Prove and fix

Write the reproduction as a test in the cheapest layer that can express it (see `mobile-testing`), watch it fail, then fix. A build-configuration fault has no unit test; its proof is the failing command passing twice in a row.

**Done when** the new test is green, the original reproduction passes, and every Mobile gate is green.

## Performance

Measure before changing anything, on a release-like build where possible; debug Compose builds are much slower than release and mislead.

| Complaint | Measure with | Usual cause here |
| --- | --- | --- |
| List scroll janks | Layout Inspector recomposition counts; `adb shell dumpsys gfxinfo dev.fieldreporter` | Missing item `key`, a state class holding a mutated list, photos decoded at full size |
| Slow start | `adb shell am start -W dev.fieldreporter/.MainActivity` (cold, three runs) | Work in `Application.onCreate`; Koin `single`s are lazy, keep them so |
| Typing lags in the form | Recomposition counts on the form | The whole form recomposing per keystroke; split the composable or pass narrower state |
| Memory grows with photos | Android Studio memory profiler, or `adb shell dumpsys meminfo dev.fieldreporter` | Uncompressed bitmaps; photos must pass through `PhotoCompressor` before they are stored |
| Sync is slow | Timestamps around `SyncEngine.push` and `pull` | One request per Incident on pull; photo uploads in series |

Report the number before and after. A change with no measured gain is reverted.

## When to stop

If two narrowing attempts in a row end on the same unexplained failure, stop, record the reproduction and what was ruled out under "Open questions" in the handoff, and hand back. The orchestrator can re-run the task on a stronger model with that record.
