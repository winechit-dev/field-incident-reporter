---
name: mobile-data-sync
description: The offline-first data layer in mobile/core/data, covering Room, the outbox, SyncEngine, the Ktor client, DTOs and error mapping. Use when a contract change reaches mobile, when adding an endpoint, a queued operation, a column or a migration, or when changing how sync handles a failure or a Conflict.
---

# Data and sync

Room is the source of truth. Every write lands locally, joins the outbox, and `SyncScheduler.requestSync()` is called. `SyncEngine` pushes the outbox in order, then pulls. Reasons: `docs/adr/0002-offline-first-sync-server-wins.md`.

## Following a contract change

`backend/openapi.yaml` leads; mobile follows by hand, so each step is checked against the spec.

1. `remote/Dtos.kt`: one `@Serializable` DTO per schema, fields named exactly as the spec. A field the spec marks optional, and every field added after the first release, gets a default, so an old build still parses a new response.
2. Enums cross the wire as `String` and are converted in the mapper with `valueOf`, which throws on a value this build does not know. A new enum value in the contract therefore needs a mobile decision in the same handoff: map it to a known fallback in the mapper, with a test that parses the new value.
3. Mappers `toDomain()` / `toDto()` / `toCreateRequest()` sit beside the DTO. Server-owned fields (`createdAt`, `updatedAt`, `reportedBy`, `activityLog`, `resolvedAt`) are read, never sent.
4. `IncidentRemote` gets the operation; `KtorIncidentRemote` implements it through `send { }`, which adds `X-User-Id` and maps failures.
5. `FakeRemote` in `commonTest` gets the same operation with the server's rule (idempotent create, `OPEN`-only change), so tests fail the way the server would.

## Error mapping

`KtorIncidentRemote.send` is the single place HTTP becomes `RemoteException`. `SyncEngine.push` is the single place a `RemoteException` becomes a decision.

| Response | `RemoteException` | Sync decision |
| --- | --- | --- |
| I/O failure, timeout, `5xx`, unreadable body | `Network` | stop, keep the queue, `SyncOutcome.RETRY` |
| `401` | `Unauthorized` | stop, keep the queue, retry |
| `409` | `Conflict` | server version wins; Incident marked `FAILED` with the reason |
| `404` | `NotFound` | drop the queued changes, mark `FAILED` |
| other `4xx` | `Rejected(status)` | drop the queued changes, mark `FAILED` |

A new status code is added to both, with a test per row. The error envelope's `message` is shown to the Technician; behaviour branches on the HTTP status and the exception type only. The feature module sees domain exceptions (`NotEditableException`, `ServerUnavailableException`) and never a `RemoteException`.

## Outbox rules

- A queued change is a `PendingOp` subtype, serialised to JSON in the `outbox` table. Each `@SerialName` is permanent: rows written by an installed build must still decode after an update. A new operation is a new subtype; a new field on an existing one is optional with a default.
- Every operation is idempotent on the server (create by client id, photo upload by photo id, delete of a missing row returns `204`), because a sync can be cut off after the request and before `removeOp`.
- `Create` sends the local row as it is when the op runs, so edits made before the first sync fold into it.
- A local write and its `enqueue` happen in one `store.inTransaction { }`.
- Sync never runs twice at once: `SyncEngine.running` is a `Mutex`.
- Photo files are deleted only after the server confirms the upload or the delete, through the injected `deleteFile`.

## Room schema

Database version is 1; the exported schema is `core/data/schemas/…/1.json`.

A change to any `@Entity` is its own task and includes all four: the version bump, the new exported schema file committed, a `Migration` registered in both `androidDatabaseModule` and `iosDatabaseModule`, and a test that opens a version-N database and reads it after migrating. A destructive fallback would erase unsynced reports, so it is never an option here.

Entities and DAOs are `internal`. Nested values (photos, activity log) are stored as JSON columns through the DTOs; `RoomLocalStore` converts between entity and domain.

## Networking

- One `HttpClient`, built by `createHttpClient()`: JSON with `ignoreUnknownKeys`, a 5 s connect and 30 s request timeout so offline fails fast.
- The engine is chosen by source set: OkHttp in `androidMain`, Darwin in `iosMain`.
- The base URL is passed into `dataModule(baseUrl)` by the app; the data layer holds no host name.
- Photos upload as multipart with the client-generated photo id.

## Done when

Each DTO matches the spec field by field, each status code the operation documents has a row in the mapping and a test, `FakeRemote` mirrors the new server rule, and a failing-first `commonTest` covers the sync behaviour offline, online and on Conflict.
