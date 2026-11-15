# Mobile lane

Kotlin Multiplatform app for Technicians: Compose Multiplatform UI on Android and iOS. Modules and sync design: `README.md`. Decisions: `docs/adr/0001-*.md`, `docs/adr/0002-*.md`.

## Roles
- `android-implementer` owns `mobile/`. It works one module at a time.
- `android-reviewer` reads the diff against the contract, the layering rules and the handoff, read-only.

Models, inputs, outputs and who hands what to whom: `docs/agents/agent-matrix.md`.

## Skills
Load the skill for the slice before editing it. `mobile-architecture` and `mobile-testing` are preloaded into the implementer; `mobile-review` and `mobile-architecture` into the reviewer.

| Working on | Skill |
| --- | --- |
| Where a class, dependency or platform API goes; Koin; coroutines; Gradle | `mobile-architecture` |
| DTOs, Room, the outbox, `SyncEngine`, Ktor, error mapping | `mobile-data-sync` |
| Screens, view models, navigation, theme, copy, accessibility | `mobile-compose-ui` |
| Choosing, writing or running a test; the gates | `mobile-testing` |
| A failure with no obvious cause; a slow screen | `mobile-debugging` |
| Reviewing a diff, or self-checking before `REVIEW` | `mobile-review` |

## Module boundaries
| Module | Holds | May depend on |
| --- | --- | --- |
| `:core:domain` | models, rules, repository and scheduler interfaces | coroutines only |
| `:core:data` | Room, Ktor, `SyncEngine`, repositories, DTOs | `:core:domain` |
| `:feature:incidents` | screens, view models, `PlatformCapabilities` | `:core:domain` |
| `:shared` | iOS framework and the iOS platform code (camera, picker, location, sync scheduling) | all of the above |
| `:androidApp`, `iosApp` | entry points, platform implementations, DI start-up | all |

Platform behaviour (camera, location, compression, background sync) goes behind an interface in `commonMain` and is implemented in the app or in a platform source set.

## Workflow
1. Read the handoff. Update DTOs and mappers in `core/data/.../remote/` to match the contract, field by field.
2. Write the failing test: sync and domain rules in `commonTest` using `Harness` and `FakeRemote` (`core/data/src/commonTest`); view-model logic with fake repositories.
3. Implement from the inside out: domain → data → feature → app.
4. Run the mobile gates in `docs/agents/quality-gates.md`.
5. For any UI change, run the app on an emulator with the `run` skill and describe what you saw.

## Practices
- Offline-first: every write goes to Room and the outbox first, then `SyncScheduler.requestSync()`.
- Branch on error `code` and on `RemoteException` type; user-facing text lives in the feature module.
- Photos are compressed JPEGs in local files; Room stores the path until upload succeeds.
- A Room schema change gets a version bump, an exported schema and a migration, as its own task.
- Emulator reaches the API at `http://10.0.2.2:3000`; the iOS simulator at `http://localhost:3000`.

## Done when
Gates are green on Android and the iOS simulator, DTOs match the contract, and each criterion has a test that failed first.
