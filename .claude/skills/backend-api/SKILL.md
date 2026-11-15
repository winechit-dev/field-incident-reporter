---
name: backend-api
description: How the mock API in backend/ is built and tested. Use when adding or changing an endpoint, a validation or authorization rule, an error code, file handling, seed data or an API test, and when checking that behaviour matches the contract.
---

# Backend API

`backend/openapi.yaml` says what the API does; `src/app.js` makes it true; `test/api.test.js` proves it. Reads (list, get, filter, sort, page, search) are served by json-server. Every write is an Express route in `createApp`, registered before the json-server router.

## Shape of a write route

Each route makes these checks. Two orderings are load-bearing: replay before state, and every check before the write.

1. **Who** — the `X-User-Id` middleware has set `req.user`, or answered `401`.
2. **Unknown fields** — a body key outside `TECHNICIAN_FIELDS` / `OPERATOR_FIELDS` is `422 VALIDATION_ERROR`.
3. **Find** — `find(id)` throws `404 NOT_FOUND`. Deletes look the row up directly and answer `204` when it is gone.
4. **Role and ownership** — `requireTechnicianOwner`, or the role's field list; `403 FORBIDDEN`.
5. **Replay** — if this id was already created or stored by the same user, return the existing row with `200`.
6. **State** — `requireOpen(row)` throws `409 NOT_OPEN`.
7. **Values** — `validateField(name, value)` per field; `422`.
8. **Write** — set server-owned fields, save, answer with `toIncident(req, row)`.

Replay comes before state because a sync retry of an upload that already succeeded must return `200` even after an Operator has picked the Incident up.

Wrap every handler in `wrap(...)` and signal failure by throwing `ApiError(status, code, message)`. The last middleware is the only place a response body for an error is written.

## Rules that must hold

- Server-owned fields (`status` on create, `reportedBy`, `createdAt`, `updatedAt`, `resolvedAt`, `activityLog`, `photos`) are computed here and never read from the body.
- `activityLog` grows and `resolvedAt` changes only when `status` really changes.
- Writes are idempotent: create by client id, photo upload by photo id, delete of something absent is `204`.
- An id that reaches the file system (`photoId`) is checked against `ID_PATTERN` first; the stored file name is built from the id and a fixed extension, never from the upload's name.
- A new field gets a case in `validateField` with its bounds, and is added to exactly one of the two field lists.
- A new enum value is added to the constant list here, to the contract, and reported in the handoff so mobile can map it.
- Time comes from the injected `now`, so tests can fix it.

## Error codes

Clients branch on `error.code`. The codes in use:

| Status | Code | Raised by |
| --- | --- | --- |
| 400 | `BAD_JSON` | body parser |
| 401 | `UNAUTHENTICATED`, `UNKNOWN_USER` | user middleware |
| 403 | `FORBIDDEN` | role or ownership |
| 404 | `NOT_FOUND` | `find` |
| 405 | `METHOD_NOT_ALLOWED` | the catch-all for unhandled writes |
| 409 | `NOT_OPEN`, `ID_TAKEN` | `requireOpen`, create |
| 413 | `LIMIT_FILE_SIZE` | multer |
| 422 | `VALIDATION_ERROR`, `PHOTO_LIMIT` | validation, photo cap |
| 500 | `INTERNAL` | anything unexpected |

A new code is a contract change: ask for it under "Contract needs" before using it. Reuse an existing code when a client would react the same way.

## Data

- `seed.json` is the demo data and the test fixture: two Technicians, two Operators, at least one Incident per Status. Mobile and admin integration tests use its ids (`u-tech-1`, `u-op-1`), so existing ids stay. Extend it when a feature needs a new case; every row stays valid under the contract.
- `db.json` is created from the seed on first start and is git-ignored. `npm run reset` restores it.
- json-server filters on any top-level field, so a new top-level field is filterable at once. Nested data (`location`, `photos`) is not; say so in the contract if a client needs to filter on it.
- Stored photo rows hold a file name; `toIncident` turns it into a URL on the way out. Anything returned from a route passes through it.

## Tests

One file, `test/api.test.js`, on `node:test`. `before` starts a real server on port 0 with a temp copy of `seed.json`, so tests never touch `db.json`.

- Use the helpers: `call(method, url, user, body)`, `create(user, extra)`, `uploadPhoto(incidentId, user, id, type)`, and the seeded ids `TECH`, `OTHER_TECH`, `OP`.
- A changed operation gets a test per status code the contract lists for it, including the replay (`200`) and the wrong-role (`403`) cases.
- Assert the status and the `error.code`, and for a refused write assert the stored row is unchanged.
- Each test creates the Incident it needs, so tests pass in any order.

## Observability

Unexpected errors are logged with `console.error` and answered as `500 INTERNAL` with no detail. Expected refusals (`ApiError`) are not logged. Keep request bodies, user names and photo contents out of logs.

## Done when

`npm test` is green with a test per documented status code of each changed operation, every code returned is one the contract lists, and a smoke run (`npm run reset && npm start`, then `curl` the changed endpoint) answers as the spec says.
