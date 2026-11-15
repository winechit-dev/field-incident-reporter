# Mobile (Kotlin Multiplatform)

Android and iOS clients for Technicians. The UI is Compose Multiplatform and shared by both
platforms (see `docs/adr/0001-shared-compose-multiplatform-ui.md` at the repo root).

| Module | Role |
| --- | --- |
| `:core:domain` | Pure Kotlin: models, rules (`OPEN`-only edits, 5 photo cap), repository and scheduler interfaces |
| `:core:data` | Room (source of truth and outbox), Ktor client, the sync engine, repository implementations, Koin module |
| `:feature:incidents` | Compose Multiplatform screens and view models |
| `:shared` | Builds the single iOS framework and holds the iOS platform code: photos, location, sync scheduling |
| `:androidApp` | The Android application |
| `iosApp` | The iOS host app |

## Offline-first sync

Every change is saved to Room first and queued in an outbox. `SyncEngine` pushes the outbox in order,
then pulls server changes. A network failure leaves the queue intact for a retry with backoff. A `409`
means an Operator already moved the Incident out of Open: the server version wins and the Incident is
marked Failed (see `docs/adr/0002-offline-first-sync-server-wins.md`).

After each completed sync the engine also stores `GET /users` as one JSON value in the `kv` table
(key `known_users`, no schema change). The Incident detail screen reads it to show the assigned
Operator's name offline; without a stored name (or with a blank one) it keeps the wording "An
operator is handling this incident." The refresh is best effort: if `GET /users` fails or its body
does not decode, the old list stays and the sync outcome is unchanged. A user whose role this build
does not know is skipped, the others are kept. The user picker never reads it and still needs the
network.

## Screens and design

Three screens (the user picker, the Incident list and the detail) and the report or edit form, which is a full-height sheet that slides up over the list or the detail (`form/IncidentFormSheet.kt`). They share one theme (`ui/Theme.kt`) that follows the system's light or dark setting, and a small set of components (`ui/Components.kt`). The colours, the Status and Severity tones and the date format are the dashboard's; see `docs/design/design-system.md` at the repo root.

The list has a chip per Status with its count; tapping one narrows the list. The avatar in its top bar opens a sheet with who is signed in and the way to switch.

A photo opens full screen on a tap. A submit that is refused scrolls to the first problem, and a saved report is confirmed on the list. The keyboard resizes the screen (`adjustResize` with `imePadding`).

The back stack is saved, so rotating the phone or returning after the system reclaimed the app shows the same screen, with a half-written report still in its form. Closing the form with something entered asks first, whether by the close button, the back gesture or a drag down. Times are shown in the phone's zone.

`:feature:incidents` depends on `material-icons-core` for icons and `kotlinx-datetime` for local time.

Titles use a bundled display typeface, Plus Jakarta Sans (Bold and ExtraBold, in `feature/incidents/src/commonMain/composeResources/font/`; SIL Open Font License, `licenses/PlusJakartaSans-OFL.txt`). Body text, labels and fields use the platform's typeface. The whole scale is `appTypography()` in `ui/Theme.kt`.

## Run and test

```
./gradlew :androidApp:assembleDebug
./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test
./gradlew :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test
```

The Android emulator reaches the mock API at `http://10.0.2.2:3000`; the iOS simulator uses `http://localhost:3000`.
For the iOS app, open `iosApp` in Xcode.
