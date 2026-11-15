---
name: backend-review
description: The review checklist for a backend/ diff, covering contract match, authorization, validation, idempotency, file handling and test coverage. Use when reviewing backend work, and as a self-check before an implementer sets the backend lane to REVIEW.
---

# Backend review

Review the diff. Every finding names a file, a line and the request that gets the wrong answer. Each of the six sections gets an explicit verdict, including "nothing to check".

## 1. Contract

Open `backend/openapi.yaml` beside the changed route.

- Every status code the route can return is listed for that operation, and every listed code can be produced.
- Response bodies carry exactly the schema's fields; `toIncident` was applied.
- Each `error.code` returned appears in the contract or in the table in `backend-api`. A new one came through a handoff request.
- The change is additive, or an ADR records the break.

## 2. Authorization

For each changed route, send it in your head as: no header, an unknown user, a Technician who is not the reporter, the reporting Technician, an Operator.

- Each of the five gets the status the contract promises.
- Ownership is checked against `req.user.id`, never a value from the body or query.
- A Technician cannot set an Operator field and the reverse; the field lists were updated, not bypassed.
- Reads go through json-server unchanged. A diff that adds data a Technician must not see needs a rule on the read path too; today any known user can read any Incident.

## 3. Validation

- Unknown body fields are refused with `422`.
- Every new field has a type, a length or range, and a null rule in `validateField`.
- Server-owned fields are never taken from input.
- Validation runs before any write, so a `422` leaves the data untouched.

## 4. Idempotency and state

- Replaying the request returns the first result with no second write.
- The replay check runs before `requireOpen`.
- Deleting something absent is `204`.
- `activityLog` and `resolvedAt` change only on a real Status change.

## 5. Files and inputs

- Ids are matched against `ID_PATTERN` before they reach a path.
- Stored names come from the id and a fixed extension.
- Size and type limits hold; the type check trusts the client's declared MIME type, so a diff that serves uploads as anything but static images is a blocker.
- No secret, real personal data, or request body in logs or seeds.

## 6. Tests

- One test per documented status code of each changed operation; read the assertions.
- A refused write is followed by an assertion that the row is unchanged.
- Tests create their own data and pass in any order.
- Re-run `npm test` yourself, then the smoke run from `docs/agents/quality-gates.md`.

## Severity

`blocker`: a contract mismatch, a request answered with the wrong status, a write a role should not be able to make, data changed by a refused request, a missing status-code test, a red gate. Everything else is a `nit`.

## Done when

All six sections and every Backend gate have an explicit verdict.
