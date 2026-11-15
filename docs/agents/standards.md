# Shared standards

## Language
Glossary terms only: Incident, Technician, Operator, Status, Severity, Category, Sync State, Conflict. One meaning per word, in code, tests and UI copy.

## API conventions
- JSON, `camelCase` fields, `UPPER_SNAKE` enum values.
- Timestamps are ISO-8601 UTC strings. Clients show local time.
- Ids are client-generated UUIDs for Incidents and Photos; creating twice with one id is safe.
- Every request carries `X-User-Id` except `GET /users` and `/uploads/*`.
- Errors are `{ "error": { "code": "UPPER_SNAKE", "message": "..." } }`. Clients branch on `code`, never on `message`.
- Status codes: `409 NOT_OPEN` means Conflict, `422` validation, `403` role or ownership, `401` unknown user.
- Writes are idempotent so sync retries are safe: create by id, delete of a missing thing returns `204`.

## Domain rules every lane honours
- Only Operators change Status, assignee and Resolution Note.
- A Technician edits, deletes or changes photos only while the Incident is `OPEN`.
- An Incident has at most five photos.
- On a Conflict the server version wins.

## Architecture
- Mobile: dependencies point inward. `feature → domain`, `data → domain`; `commonMain` has no platform imports; platform behaviour sits behind interfaces (`PlatformCapabilities`, `SyncScheduler`). Room is the source of truth; every write lands locally first.
- Backend: rules live in `src/app.js`; `json-server` only serves reads.
- Admin: the API client is generated from the contract; UI state derives from server data.

## Code
- Small modules with narrow interfaces; test through the interface.
- Names say what a thing is in the domain; comments explain why, not what.
- No secrets, no real personal data in seeds or tests.
- Package and bundle ids use `dev.fieldreporter`.
