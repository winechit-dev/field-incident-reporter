# 006 assignee-name

**Goal:** on the mobile Incident detail screen, a Technician sees the name of the Operator assigned to the Incident, online or offline, and falls back to today's wording when the name is not known.
**Lanes:** mobile

Contract: no change. Backend: no work. Admin: no work. See "Contract delta" for why.

## Acceptance criteria
Each criterion names the lane that proves it and, for mobile, the layer. Seed data: `u-op-1` is Mya Sandar, `u-op-2` is Htet Naing (both `OPERATOR`); `u-tech-1` (Aung Kyaw) reported `...0001` (Unassigned, `OPEN`) and `...0002` (assignee `u-op-1`, `IN_PROGRESS`).

"Named wording" below means `<name> is handling this incident.`; "fallback wording" means today's text, `An operator is handling this incident.`, unchanged.

1. Given the server's `GET /users` returns `u-op-1` / "Mya Sandar" / `OPERATOR`, when a sync completes, then the user list is stored in the local `kv` table and the domain user lookup returns Mya Sandar for `u-op-1`. *(mobile, `commonTest` on sync/repository behaviour, `core/data`)*
2. Given the user list was stored by an earlier sync, when the remote is offline and a new `UserRepository` reads it (simulating an app restart with no network), then Mya Sandar is still returned for `u-op-1`. *(mobile, `commonTest`, `core/data`)*
3. Given the stored list has Mya Sandar for `u-op-1`, when a sync runs while `GET /users` fails (network) or the server is unreachable, then the stored list is unchanged, and the `SyncOutcome` is what it would have been without the user refresh (a users-only failure never turns `COMPLETED` into `RETRY`, never touches the outbox, never changes any Sync State). *(mobile, `commonTest`, `core/data`)*
4. Given the stored list has `u-op-1` only, when a later sync's `GET /users` returns `u-op-1` and `u-op-2`, then the stored list is replaced by the new one (both are known); a user the server no longer returns is no longer known. *(mobile, `commonTest`, `core/data`)*
5. Given an Incident with `assignee: u-op-1` and a known Mya Sandar, when the detail view model loads it, then its state carries the name "Mya Sandar" for the Handling section. *(mobile, view-model test, `feature/incidents`)*
6. Given an Incident with an assignee, when no user list has ever been stored, or the assignee id is not in the stored list, or the id belongs to a user whose role is not `OPERATOR`, then the view-model state carries no name and the screen shows the fallback wording, never the raw id and never a blank line. *(mobile, view-model test for the state; emulator run for the wording)*
7. Given the detail screen is open on an Incident assigned to `u-op-1`, when an Operator reassigns it to `u-op-2` on the server and the next sync pulls it, then the view-model state names Htet Naing without reopening the screen. The same holds for the list arriving after the Incident: if the name becomes known after the screen opened, the state updates. *(mobile, view-model test with fake repositories emitting new values; the pull of `assignee` itself is existing `SyncEngine` behaviour but has no test today (`SyncTest.kt` never mentions `assignee`), so add a `commonTest` that a reassigned or cleared `assignee` lands in the local row after sync; it may pass first time as a regression guard)*
8. Given an Incident with `assignee: null`, when the detail screen shows it, then there is no "Handling" section, as today; an Operator un-assigning it on the server removes the section after the next sync. *(mobile, view-model test that the state has no assignee; emulator run)*
9. Given the app on an Android emulator against the seeded backend, signed in as Aung Kyaw: when `...0002` is opened online, then the Handling section reads `Mya Sandar is handling this incident.`; after turning the emulator's network off (airplane mode) and force-stopping and relaunching the app, it still reads the same; `...0001` shows no Handling section. *(mobile, emulator run)*
10. Given the existing behaviour, then: the user picker still lists only Technicians, still fetches live from `GET /users`, and still shows "Can't reach the server..." when offline (it does not fall back to the stored list); push order, retries and Conflict handling (server wins, Failed with the reason) are unchanged; signing out keeps or clears the stored list without affecting the picker. *(mobile, existing `commonTest` and view-model tests stay green unchanged; `scripts/integration/run-integration.sh` passes)*
11. Given the backend, then `GET /users` without `X-User-Id` returns `200` with every seeded user, as today. *(backend, existing test; no new backend work)*

## Contract needs
None. Everything the app needs exists in `backend/openapi.yaml` 1.1.0:
- `GET /users` (`security: []`, so no `X-User-Id`) returns `User[]` with required `id`, `name`, `role`.
- `Incident.assignee` is a required, nullable user id on every Incident in `GET /incidents` and `GET /incidents/{id}`; the server only accepts an `OPERATOR` id there (`PATCH /incidents/{id}`).
- The mobile `UserDto` already maps `id`, `name`, `role` field by field, and `KtorIncidentRemote.users()` already calls `GET /users`.

## Contract delta
| Operation | Change | Reason |
| --- | --- | --- |
| none | none | `GET /users` is public and returns each user's `name`; `Incident.assignee` carries the id to look up. Joining the two is client-side work. Embedding an `assigneeName` in `Incident` was considered and rejected: it duplicates data the client can already get, and the cached list is needed anyway for the offline case. |

`GLOSSARY.md`: unchanged. Assignee, Operator and Incident already cover the feature; the cached user list is an implementation detail, not a domain term. No ADR: the choice (cache in the existing `kv` table, refresh on sync) is cheap to reverse.

## Tasks per lane

**android-implementer** (branch `006-mobile`). Load `mobile-data-sync`, `mobile-compose-ui` and `mobile-testing`.

Constraints:
- No Room schema change. Store the user list as one JSON value in the existing `kv` table under a new key (for example `known_users`), the way `current_user` is stored as `StoredUser`. No version bump, no migration. `FieldReporterDatabase` stays at version 1 and its exported schema is unchanged.
- Layering per `mobile/AGENTS.md`: `:feature:incidents` sees only `:core:domain` types (`User`, `UserRepository`). No DTO, `LocalStore` or Room type leaks into the feature module.
- UI copy: named wording `<name> is handling this incident.`, in the same sentence case as the rest of the app's copy ("Report incident", "No incidents yet"). The fallback keeps today's exact text.

Work, inside out, red first:
1. Domain (`core/domain/.../Repositories.kt`): add to `UserRepository` a read of the known users that works offline, as a `Flow` so the screen follows changes. Suggested shape: `fun knownOperator(id: String): Flow<User?>` (returns a user only if it is in the stored list with role `OPERATOR`), or `val knownUsers: Flow<List<User>>` with the filtering in the view model. Keep `availableUsers()` as is: it stays network-only for the picker (criterion 10).
2. Data:
   - `LocalStore`: add a read (`Flow`) and a write of the stored user list; implement in `RoomLocalStore` over `KvDao` and in `FakeLocalStore` (`core/data/src/commonTest/.../Fakes.kt`). An undecodable stored value reads as "no list" (fallback wording), not a crash.
   - Refresh the stored list on every sync, after push and pull, best effort: on success replace the whole list (criterion 4); on any failure keep the old list and leave `SyncOutcome` as push/pull decided it (criterion 3). Put it in `SyncEngine.sync()` or `IncidentRepositoryImpl.sync()`; either way it runs under the existing sync, so no new scheduling. Catch the failure narrowly: `RemoteException`, plus a mapping failure if `GET /users` ever returns a role the app does not know (`Role.valueOf` throws `IllegalArgumentException`); that must not abort the sync.
   - Also store the list when `availableUsers()` succeeds, so a Technician who has just picked themselves already has names before the first sync completes.
   - `FakeRemote.users()` today ignores `online` and does not record a call. Make it go through `gate("users")` and let tests script its result, so criteria 2-4 can run offline and with a changed list. Check the existing tests still pass with that change.
3. Feature:
   - `IncidentDetailViewModel` takes a `UserRepository` as well (wire it in `App.kt`). Combine `incidents.incident(id)` with the known-Operator lookup for `incident.assignee`, so `DetailState.Loaded` carries the Incident and an optional Operator name. Update `FakeUserRepository` (`feature/incidents/src/commonTest/.../FakeRepositories.kt`) with a scriptable known-users flow.
   - `IncidentDetailScreen.kt` line ~120: inside the existing `if (incident.assignee != null)` Handling section, show the named wording when the name is known, otherwise the fallback wording. Nothing else on the screen changes, including the "An operator is already handling this incident, so it can't be changed." line at the bottom.
4. Tests (each must fail first): criteria 1-4 and the pull half of 7 in `core/data/src/commonTest` with `Harness`/`FakeRemote`; criteria 5-8 in a new `feature/incidents/src/commonTest/.../IncidentDetailViewModelTest.kt` with fake repositories.
5. Emulator run for criteria 6, 8 and 9 with the `run` skill on an Android emulator only: AVD `Pixel_9` (or `Pixel_10a`). A physical phone is attached over adb; never install on or drive it. Target the emulator by serial (`adb -s emulator-5554 ...`, or `-e`), and confirm with `adb devices` which serial is the emulator before installing. The app reaches the API at `http://10.0.2.2:3000`. For criterion 6 on screen, sign in with an empty `kv` (fresh install, `adb -s <emulator> shell pm clear dev.fieldreporter...`) and the backend stopped after the Incidents are already on the device, or describe the state shown by the view-model test if that set-up is impractical. Describe what you saw in "Evidence".
6. Gates: all mobile gates in `docs/agents/quality-gates.md`, including iOS simulator tests and link (the shared UI changes), and `scripts/integration/run-integration.sh` because sync logic changes. `scripts/check-forbidden-terms.sh` exits 0.
7. Document: update `mobile/README.md` if it describes what the detail screen or the `kv` table holds.

**backend**: no task. `GET /users` and `assignee` exist and are tested; criterion 11 is an existing test.

**admin**: no task. The dashboard already shows assignee names from `GET /users`; nothing it sends or reads changes.

Merge order: only `006-mobile`.

## Lane status
| Lane | Status (`TODO` / `IN PROGRESS` / `REVIEW` / `DONE`) | Branch | Notes |
| --- | --- | --- | --- |
| contract | DONE | main (uncommitted) | No change to `backend/openapi.yaml`; it parses. |
| backend | n/a | | No work. |
| mobile | DONE | | Gates green on the Android host and the iOS simulator. Reviewed twice: one blocker found and fixed, none remain. Emulator check done for criteria 8 and 9; criterion 6's wording not run on screen. |
| admin | n/a | | No work. |

## Evidence
Contract: `ruby -ryaml -e 'YAML.load_file("backend/openapi.yaml")'` exits 0; file unchanged.

### Mobile

Tests added, by criterion (all `commonTest` unless noted; each run on JVM and iOS simulator):
- 1: `KnownUsersTest.syncStoresTheUserListAndTheLookupFindsTheOperator`
- 2: `KnownUsersTest.theStoredListIsStillReadOfflineByAFreshRepository`
- 3: `KnownUsersTest.aFailedUserFetchKeepsTheListAndDoesNotChangeTheOutcomeOrTheOutbox` (network and unknown-role failures, with a queued edit and the call order), `aUsersBodyThatDoesNotDecodeStillCompletesTheSyncAndKeepsTheList`, `cancellingTheUsersFetchStillCancelsTheSync`, `anOfflineSyncKeepsTheListAndStillAsksForRetry`; at the remote boundary `UsersRemoteTest.aUsersBodyThatDoesNotDecodeBecomesANetworkFailure`
- 4: `KnownUsersTest.aLaterSyncReplacesTheWholeList`
- 5: `IncidentDetailViewModelTest.theStateNamesTheAssignedOperator`
- 6: `noStoredListMeansNoNameNeverTheRawId`, `anIdMissingFromTheStoredListMeansNoName`, `anIdThatBelongsToANonOperatorMeansNoName`; wording in `HandlingTextTest` (incl. `aBlankNameKeepsTodaysWording`); an unknown role keeps the other Operators: `UsersRemoteTest.aUserWithARoleThisBuildDoesNotKnowIsSkippedAndTheRestAreKept`, `theStoredListSkipsAnUnknownRoleAndKeepsEveryOperator`
- 7: `aReassignmentIsNamedWithoutReopeningTheScreen`, `aListThatArrivesAfterTheScreenOpenedFillsTheNameIn`; pull half: `KnownUsersTest.aReassignedOrClearedAssigneeLandsInTheLocalRowAfterSync`
- 8: `anUnassignedIncidentHasNoAssigneeAndNoName`, `unassigningOnTheServerRemovesTheAssigneeAfterTheNextSync`
- 10: `thePickerStillFailsOfflineAndDoesNotUseTheStoredList`, `fetchingTheListForThePickerAlsoStoresIt`; all pre-existing tests unchanged and green; integration script passes.
- Extra: `onlyAnOperatorIsReturnedAsTheAssigneeAtDataLevel` is `KnownUsersTest.onlyAnOperatorIsReturnedAsTheAssignee`.

Red first (`:core:data:testAndroidHostTest`, with the interface and fakes in place but no refresh in `SyncEngine`): `32 tests completed, 5 failed`:
```
theStoredListIsStillReadOfflineByAFreshRepository | expected:<User(id=u-op-1, name=Mya Sandar, role=OPERATOR)> but was:<null>
anOfflineSyncKeepsTheListAndStillAsksForRetry | expected:<[User(id=u-op-1 ...)]> but was:<[]>
aLaterSyncReplacesTheWholeList | expected:<User(id=u-op-2, name=Htet Naing ...)> but was:<null>
fetchingTheListForThePickerAlsoStoresIt | expected:<User(id=u-op-1 ...)> but was:<null>
syncStoresTheUserListAndTheLookupFindsTheOperator | expected:<[User(id=tech-1 ...), User(id=u-op-1 ...)]> but was:<[]>
```
Passed on the first run, as regression guards, not forced red: `aFailedUserFetchKeepsTheList...` (criterion 3; nothing stored yet to lose, the refresh did not exist), `aReassignedOrClearedAssigneeLandsInTheLocalRow...` (criterion 7 pull half, existing `mergeServer`/`pull`, as the handoff predicted), `thePickerStillFailsOffline...`, `onlyAnOperatorIsReturned...`.

Red, feature (`:feature:incidents:testAndroidHostTest`, view model constructed with `UserRepository` but never looking the name up): `30 tests completed, 3 failed`:
```
theStateNamesTheAssignedOperator | expected:<Mya Sandar> but was:<null>
aReassignmentIsNamedWithoutReopeningTheScreen | expected:<Mya Sandar> but was:<null>
aListThatArrivesAfterTheScreenOpenedFillsTheNameIn | expected:<Mya Sandar> but was:<null>
```
The "no name" tests (criteria 6 and 8) pass against the stub by construction. Note: in these tests `advanceUntilIdle()` does not run work resumed for a `backgroundScope` collector, so they use a `settle()` helper (`advanceTimeBy(1)` then `runCurrent()`).

Gates (from `mobile/`, `--console=plain -q`), final:
- `./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test`: exit 0.
- `./gradlew :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test`: exit 0.
- `./gradlew :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64`: exit 0.
- `./gradlew :androidApp:lintDebug`: exit 0; report has 0 errors and 7 warnings (the baseline 7).
- `scripts/integration/run-integration.sh`: "Integration check passed."
- `scripts/check-forbidden-terms.sh`: "forbidden-terms: clean", exit 0.
- `git status mobile/core/data/schemas`: no change; database still version 1.

Emulator (Android emulator `emulator-5554`, AVD Pixel_9; the physical phone was never targeted: `ANDROID_SERIAL=emulator-5554` for install, `-s emulator-5554` on every adb call). Backend: `PORT=3000 DB_FILE=<temp copy of seed.json>`.
- Fresh install (`pm clear`), picked Aung Kyaw, list showed both Incidents.
- Criterion 9, online: `Pothole on service road` (`...0002`) detail shows Handling: `Mya Sandar is handling this incident.`
- Airplane mode on, Wi-Fi and data off, backend stopped, app force-stopped and relaunched: the same Incident still reads `Mya Sandar is handling this incident.`
- `Hydraulic lift leaking oil` (`...0001`, Unassigned): no Handling section (criterion 8 on screen).
- Not done: criterion 6 wording on screen. Reaching it needs an empty `kv` with Incidents already on the device, and `pm clear` removes both; the state is covered by the three view-model tests and `HandlingTextTest`. Rotation was not checked (no navigation change).
- Cleanup: airplane mode turned off, emulator killed (`adb devices` shows only the physical phone), backend stopped.

Reviewer notes: the user list refresh runs only when the sync reaches COMPLETED (after push and pull), is best effort, and the list is kept across sign-out. After review: `KtorIncidentRemote.users()` maps decode failures to `RemoteException.Network`; `SyncEngine.refreshKnownUsers()` rethrows `CancellationException` and otherwise ignores any failure of the fetch (not of the store write); users with an unknown role are skipped one by one. `mobile/README.md` now describes the `known_users` kv value.

### Review fixes

Changes (all inside `mobile/`):
- Blocker. `KtorIncidentRemote.users()` now reads its body through a `decoding { }` helper that maps `ContentConvertException` (Ktor's `JsonConvertException`), `NoTransformationFoundException` (non-JSON content type) and `SerializationException` to `RemoteException.Network("Unreadable response")`, so HTTP still becomes `RemoteException` in one file. `SyncEngine.refreshKnownUsers()` guards only `remote.users()`: `catch (CancellationException) { throw }` first, then `catch (Exception)` keeps the old list; this backstop is what the requested `KnownUsersTest` case (a fake throwing a `JsonConvertException` directly, bypassing the Ktor boundary) needs to pass. `store.setKnownUsers` is outside the `try`, so a store failure is not swallowed. The `IllegalArgumentException` catch is gone: no unknown role throws any more (nit 1).
- To test the boundary, the client set-up moved into `HttpClientConfig<*>.fieldReporterConfig()` in `DataModule.kt` (`createHttpClient()` calls it, behaviour unchanged), and `core/data` `commonTest` gained `libs.ktor.client.mock` (already in the catalog; the catalog is unchanged).
- Nit 1. `List<UserDto>.toDomain()` maps user by user through `UserDto.toDomainOrNull()` (`Role.entries` lookup) and skips unknown roles; `KtorIncidentRemote.users()` uses it, so the picker gets the same behaviour. The stored-list read is now the top-level `internal fun decodeKnownUsers(value: String?)` in `RoomLocalStore.kt`, also per user; an undecodable value still reads as no list.
- Nit 2. `handlingText` uses `isNullOrBlank()`, so `""` and whitespace give the fallback wording.
- Nit 3. `aFailedUserFetchKeepsTheList...` now runs each failure in its own `Harness` holding a real queued `Update` (an edit after the first sync), and asserts the queued edit was pushed and removed, nothing re-queued, the row is `SYNCED` with the edited title on device and server, the stored list is unchanged, and `remote.calls == ["update <id>", "list", "users"]` (refresh after push and pull).

New tests: `KnownUsersTest.aUsersBodyThatDoesNotDecodeStillCompletesTheSyncAndKeepsTheList`, `KnownUsersTest.cancellingTheUsersFetchStillCancelsTheSync`, new `UsersRemoteTest` (`aUsersBodyThatDoesNotDecodeBecomesANetworkFailure`: missing `name`, `"role": null`, non-array, not JSON, `text/html`; `aUserWithARoleThisBuildDoesNotKnowIsSkippedAndTheRestAreKept`; `theStoredListSkipsAnUnknownRoleAndKeepsEveryOperator`), `HandlingTextTest.aBlankNameKeepsTodaysWording`; rewritten `KnownUsersTest.aFailedUserFetchKeepsTheListAndDoesNotChangeTheOutcomeOrTheOutbox`.

Red (`:core:data:testAndroidHostTest :feature:incidents:testAndroidHostTest`, tests in place, production code with behaviour-preserving seams only): core/data `37 tests completed, 4 failed, 6 skipped`; feature `31 tests completed, 1 failed`:
```
KnownUsersTest aUsersBodyThatDoesNotDecodeStillCompletesTheSyncAndKeepsTheList | io.ktor.serialization.JsonConvertException: Illegal input: Field 'name' is required
UsersRemoteTest aUsersBodyThatDoesNotDecodeBecomesANetworkFailure | [{"id":"u-op-1","role":"OPERATOR"}]. Expected an exception of class dev.fieldreporter.core.data.remote.RemoteException$Network to be thrown, but was io.ktor.serialization.JsonConvertException: Illegal input: Field 'name' is required ...
UsersRemoteTest aUserWithARoleThisBuildDoesNotKnowIsSkippedAndTheRestAreKept | java.lang.IllegalArgumentException: No enum constant dev.fieldreporter.core.domain.Role.AUDITOR
UsersRemoteTest theStoredListSkipsAnUnknownRoleAndKeepsEveryOperator | expected:<[User(id=u-op-1, name=Mya Sandar, role=OPERATOR), User(id=u-op-2, name=Htet Naing, role=OPERATOR)]> but was:<[]>
HandlingTextTest aBlankNameKeepsTodaysWording | for name "" expected:<[An operator] is handling this in...> but was:<[] is handling this in...>
```
Passed first time, as guards: the rewritten `aFailedUserFetch...` (its failures were already caught; it now pins the queue and the call order) and `cancellingTheUsersFetchStillCancelsTheSync` (cancellation was never caught; it pins that the new backstop keeps it so).

Final gates (from `mobile/`, `--console=plain -q`):
- `./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test`: exit 0. core/data JVM 37 tests, 0 failed, 6 skipped (the `*IntegrationTest`s); iOS simulator 31 tests, 0 failed; core/domain 3 tests on iOS, 0 failed. Only output: the existing `expect`/`actual` Beta warnings.
- `./gradlew :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test`: exit 0. feature 31 tests on JVM and 31 on iOS, 0 failed; shared 9, 0 failed. Only output: the existing `BackHandler` deprecation warning at `App.kt:57`.
- `./gradlew :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64`: exit 0.
- `./gradlew :androidApp:lintDebug`: exit 0, `0 errors, 7 warnings` (the baseline 7).
- `scripts/integration/run-integration.sh`: "Integration check passed." (exit 0).
- `scripts/check-forbidden-terms.sh`: "forbidden-terms: clean", exit 0.
- `git status mobile/core/data/schemas`: no change; database still version 1.
- No emulator or device run for these fixes (none needed; the physical phone was not touched).

### Reviews (main session)

**First review (android-reviewer): one blocker.** A `GET /users` answer of `200` with a body that did not decode escaped the best-effort refresh, so `sync()` threw instead of returning `COMPLETED`; the list screen would crash on Android and the scheduler's scope on iOS. Five nits. The first implementation ran on the implementer's default model although it changed `SyncEngine`; the fix was run on the stronger model, as `/implement` prescribes for sync-engine changes.

**Second review: blocker closed, no blockers.** Checked against Ktor 3.6.1's bytecode: every decode failure `users()` can raise now becomes `RemoteException.Network`, and `UsersRemoteTest` exercises the real client on a mock engine. Its own gate run was green on both platforms, with the integration script passing.

Nits left as they are:
- `SyncEngine.refreshKnownUsers()` keeps a broad `catch (Exception)` after rethrowing `CancellationException`. Cancellation is preserved, but the catch can now only hide a bug in our own code, silently. Narrowing it to `RemoteException` is the suggested follow-up.
- A blank stored name reaches the view-model state as blank; the screen falls back correctly.


### Integration (integration-verifier)

Criteria 1-8, 10 and 11 pass from its own run: every Mobile gate on the Android host and the iOS simulator, `scripts/integration/run-integration.sh`, backend 21/21, and `GET /users` without `X-User-Id` against its own backend. The names agree across lanes: `u-op-1` is Mya Sandar in `GET /users`, which is also what the dashboard shows.

Criterion 9 was **not** verified independently. When the verifier ran, port 3000 was held by a backend it had not started and an emulator it had not started was running, so it did not install or run the app. The only emulator run behind criterion 9 is the implementer's, made before the review fixes; those fixes did not change the screen or the stored-list write path, but the run was not repeated after them.

Never run on a screen: the fallback wording (criterion 6) and the absence of the Handling section after an Operator un-assigns (criterion 8); both are proved at the view-model and wording-function level only.

## Open questions
- Fallback wording: settled by the main session before implementation. Both lines use the app's existing sentence case; the glossary fixes the terms, not their capitals. Named: `<name> is handling this incident.` Fallback: `An operator is handling this incident.`
- Sign-out. The stored user list is public data, not tied to the Current User, so keeping it across sign-out is harmless. The implementer may keep it; criterion 10 only requires that the picker is unaffected.
- From review, left as is: the user-list refresh runs inside the sync `Mutex`, so a slow `GET /users` (up to the 30 s request timeout) holds the lock and delays the next sync. Moving it out would need its own guard against two refreshes at once.
- From review, left as is: `RoomLocalStore.setKnownUsers` and the `known_users` round trip have no test against a real database; the project has no Room test set-up. The decode half is covered by `UsersRemoteTest.theStoredListSkipsAnUnknownRoleAndKeepsEveryOperator` through `decodeKnownUsers`.
- Pre-existing, not fixed here: `KtorIncidentRemote.listMine` (and the other calls) also decode with `.body<...>()` outside `send { }`, so a 200 with an undecodable body during pull throws a `JsonConvertException`, which `SyncEngine.pull` does not catch; `sync()` then throws, with the same crash path as the blocker. The `decoding { }` helper in `KtorIncidentRemote` is the likely fix, as its own task with its own tests.
