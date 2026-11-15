---
name: admin-review
description: The review checklist for an admin/ diff, covering contract match, server and client boundaries, error handling, access control, accessibility and tests. Use when reviewing admin work, and as a self-check before an implementer sets the admin lane to REVIEW.
---

# Admin review

Review the diff. Every finding names a file, a line and what an Operator would see go wrong. Each of the six sections gets an explicit verdict, including "nothing to check".

## 1. Criteria and tests

- Each admin acceptance criterion maps to a named test that would fail if the change were reverted. Read the assertions.
- New decisions are pure functions in `src/lib/` with tests; a component holding untested domain logic is a blocker.
- Re-run the Admin gates from `docs/agents/quality-gates.md` yourself, including `npm run gen:api` followed by `git status` on `src/api/schema.d.ts`.

## 2. Contract

- `schema.d.ts` is generated output with no hand edits.
- Every API call goes through `src/api/` and `unwrap`; no `fetch` elsewhere outside `src/integration/`.
- Code branches on `error.code`, never on the message or on status alone.
- A patch sends Operator fields only (`status`, `assignee`, `resolutionNote`), and only those that changed.
- If the diff changes what "Start work" or "Resolve" sends, the shared fixtures in `scripts/integration/fixtures/` changed with it and the integration script was run.

## 3. Server and client

- Cookies, `searchParams` and API calls are read in server components inside `<Suspense>`.
- `"use client"` appears only where there is interaction, and such a file imports nothing server-only (`next/headers`, `src/lib/session.ts`).
- `redirect()`, `refresh()` and `notFound()` are called outside `try`.
- State that should survive a reload or be shareable is in the URL.
- A framework API new to the diff matches its guide in `node_modules/next/dist/docs/`.

## 4. Input and access

- Every value from `FormData`, `searchParams` or a route parameter is narrowed on the server before use.
- Each page and action calls `getOperator()` and handles `null`.
- The Operator id reaches the API from server code only.
- Session cookies stay `httpOnly` and `sameSite=lax`. They are unsigned and not `secure`, which is accepted for the mock API; a diff that points the dashboard at a real host makes that a blocker.
- Nothing renders API text as HTML; no secret or real personal data in code, tests or `.env.example`.

## 5. UI and accessibility

- Loading, empty, data and failure states all render; a form shows pending and success.
- Inputs have labels; errors use `aria-invalid` and `role="alert"`.
- Status, Severity and times go through `StatusBadge`, `SeverityBadge`, `LocalTime`.
- Colours come from theme tokens, with a `dark:` pair for any literal.
- The handoff describes a browser check against the running backend.

## 6. Code quality

- TypeScript stays strict: no `any`, no non-null `!`, and an `as` cast only where the value is validated on the next line.
- Names are glossary terms; comments say why.
- New dependencies are justified in the handoff.
- The diff contains only what the handoff asked for.

## Severity

`blocker`: a criterion without a real test, a contract mismatch, a hand-edited `schema.d.ts`, unvalidated input reaching the API, a page or action reachable without an Operator, a server-only import in client code, a red gate. Everything else is a `nit`.

## Done when

All six sections and every Admin gate have an explicit verdict.
