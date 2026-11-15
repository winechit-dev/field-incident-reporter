---
name: mobile-testing
description: Which test proves a mobile change, where it lives, and which Gradle task runs it. Use before writing code for a mobile acceptance criterion, when a mobile test is red or flaky, when adding a fake, and when running the mobile gates.
---

# Mobile testing

Red first: the test exists and fails for the right reason before the change. Paste the failing output into the handoff, then the passing output.

## Choosing the test

Take the first row that can express the criterion. Lower rows cost more and run less often.

| The criterion is about | Test lives in | Built with | Runs in |
| --- | --- | --- | --- |
| A rule on a model | `core/domain/src/commonTest` | plain `kotlin.test` | `:core:domain:allTests` |
| Repository, outbox or sync behaviour | `core/data/src/commonTest` | `Harness`, `FakeLocalStore`, `FakeRemote` in `Fakes.kt` | `:core:data:testAndroidHostTest`, `:core:data:iosSimulatorArm64Test` |
| What a screen's state or events do | `feature/incidents/src/commonTest` | `FakeIncidentRepository`, `FakeUserRepository` | `:feature:incidents:testAndroidHostTest`, `:feature:incidents:iosSimulatorArm64Test` |
| iOS platform code, schedulers | `shared/src/commonTest`, `shared/src/iosTest` | plain `kotlin.test` | `:shared:iosSimulatorArm64Test` |
| The real Ktor client against the real API | `core/data/src/androidHostTest`, `*IntegrationTest` | `IntegrationHarness` | `scripts/integration/run-integration.sh` only |
| What the screen looks like | no automated test | the `run` skill on an emulator | by hand, described in the handoff |

`commonTest` runs on both the JVM and the iOS simulator, so it uses `kotlin.test` and `kotlinx-coroutines-test` only. JUnit and `java.*` are available in `androidHostTest` alone.

## Sync and repository tests

`Harness()` wires the real `SyncEngine` and `IncidentRepositoryImpl` to in-memory fakes with a fixed clock and predictable ids.

- Offline is `h.remote.online = false`. An Operator acting on the dashboard is `h.remote.operatorSetsStatus(id, Status.IN_PROGRESS)`.
- Assert on what the Technician would observe: `h.store.incident(id)?.syncState`, `h.store.pendingOps()`, `h.repo.incidents.first()`, and on what reached the server: `h.remote.calls`, `h.remote.server`.
- Cover the three paths a sync change can take: offline then online, online straight through, and Conflict.
- Test names are sentences about behaviour: `networkFailureKeepsQueueIntactAndRequestsRetry`.
- When the server gains a rule, `FakeRemote` gains it too, so the test fails the way production would.

## View-model tests

- `Dispatchers.setMain(StandardTestDispatcher(testScheduler))` in set-up, `Dispatchers.resetMain()` in `@AfterTest`.
- Collect state into a list with `vm.state.onEach { seen += it }.launchIn(backgroundScope)`; `stateIn(WhileSubscribed)` produces nothing without a collector.
- Drive virtual time with `advanceTimeBy` and `advanceUntilIdle`. Timers are constructor parameters (`undoWindowMs`), so the test sets them.
- Assert the state and the events a Technician would notice, including the error text.

## Integration tests

`*IntegrationTest` classes skip themselves unless `FIR_API_URL` is set, so a plain `testAndroidHostTest` reports them as skipped, never passed. Run them through `scripts/integration/run-integration.sh`, which starts its own API. Run it after any change to sync logic, DTOs or the contract.

## Flaky or slow tests

A test that depends on wall-clock time, real dispatchers or test order is a bug in the test. Inject the clock, use the test scheduler, give each test its own `Harness`. Re-running until green is not a fix; find the shared state.

## The gates

Run the Mobile gates in `docs/agents/quality-gates.md` exactly as written, from `mobile/`. Add `--console=plain -q` to keep the output short. A red gate is reported with the failing test name and its assertion message; the HTML report is at `<module>/build/reports/tests/`.

## Done when

Every mobile acceptance criterion names one test from the table, each of those tests was seen red before the change, and every Mobile gate is green on the JVM and the iOS simulator.
