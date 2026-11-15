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

## Run and test

```
./gradlew :androidApp:assembleDebug
./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test
```

The Android emulator reaches the mock API at `http://10.0.2.2:3000`; the iOS simulator uses `http://localhost:3000`.
For the iOS app, open `iosApp` in Xcode.
