---
name: mobile-review
description: The review checklist for a mobile/ diff, covering correctness against the handoff and contract, offline-sync safety, layering, security and code quality. Use when reviewing mobile work, and as a self-check before an implementer sets the mobile lane to REVIEW.
---

# Mobile review

Review the diff, not the codebase. Every finding names a file, a line and the scenario that goes wrong. A finding with no failing scenario is a `nit` at most.

Work through all seven sections; each gets an explicit verdict in the report, including "nothing to check" when the diff does not touch it.

## 1. Criteria and tests

- Each mobile acceptance criterion in the handoff maps to a named test, and that test would fail if the change were reverted. Read the test body; a test that asserts nothing about the criterion does not count.
- The test sits in the cheapest layer that can express it (`mobile-testing`).
- Re-run the Mobile gates from `docs/agents/quality-gates.md` yourself. Your run outranks the pasted evidence.

## 2. Contract

Open `backend/openapi.yaml` beside `core/data/.../remote/Dtos.kt`.

- Field names, nullability and types match the schema, field by field.
- New response fields have defaults; new enum values have a mapping (`valueOf` throws on an unknown one).
- Every status code the operation documents reaches a `RemoteException` in `KtorIncidentRemote.send` and a decision in `SyncEngine.push`.
- Server-owned fields are never sent.

## 3. Offline and sync safety

Ask of every changed write path:

- Does the write land in Room and the outbox in one transaction, followed by `requestSync()`?
- If the process dies between the HTTP response and `removeOp`, is the replay harmless?
- If the device is offline for a day, is anything lost or reordered?
- If an Operator moved the Incident out of `OPEN` meanwhile, does the server version win and the Technician see why?
- Can a row written to the outbox by the previous app version still be decoded?
- Is a Room entity change accompanied by a version bump, an exported schema, a migration on both platforms and a migration test?

## 4. Layering

- Module dependencies stay inside the table in `mobile/AGENTS.md`.
- `commonMain` holds no platform import; platform work sits behind `PlatformCapabilities` or `SyncScheduler`.
- Data-layer types stay `internal`; the feature module sees domain types only.
- Koin appears only in `:core:data` and the entry points.

## 5. UI

- State is one immutable class; every variant (loading, empty, loaded, error or blocked) renders.
- State that must survive rotation is in a view model or `rememberSaveable`.
- Lazy lists have stable keys; images and icons have content descriptions.
- Copy uses glossary terms. Status, Severity and Sync State render through `Pill`, `SyncBadge`, `FailureBanner`.
- The handoff describes an emulator run of the changed flow.

## 6. Security

- No secret, token, host name of a real service, or real personal data in code, tests, seeds or logs.
- Logs carry ids at most: never an Incident's title, description, location or a photo path at a level that ships.
- A new permission is in the manifest, requested at the moment of use, and the feature degrades when it is refused.
- Files shared with other apps go through `FileProvider` with the narrowest path in `file_paths.xml`; exported components stay limited to the launcher activity.
- Values interpolated into a URL or query are ids the app generated or received from the server, not free text.
- Known and accepted for the mock API, so not findings unless the diff widens them: cleartext HTTP, `allowBackup`, minification off, a hard-coded emulator base URL. A diff that points the app at a real host makes all four blockers.

## 7. Code quality

- Names are glossary terms; comments say why.
- Specific exceptions are caught; nothing swallows `CancellationException`.
- No `!!` on a value that can be null at runtime, no `GlobalScope`, no `runBlocking` outside tests.
- New dependencies are in the version catalog and in the narrowest source set.
- The diff contains only what the handoff asked for.

## Severity

`blocker`: a criterion without a real test, a contract mismatch, possible data loss or a stuck outbox, a layering break, a red gate, or anything in section 6. Everything else is a `nit`.

## Report

One line per section with its verdict, then findings as `blocker` or `nit` with `file:line` and the failing scenario, then the gate results from your own run.

## Done when

All seven sections and every Mobile gate have an explicit verdict.
