---
name: admin-architecture
description: Where code goes in admin/ and how data moves through the Next.js dashboard. Use when adding a route, a server action, an API call, a filter, a form, a rule about what an Operator may do, session handling, or a test in the admin lane.
---

# Admin architecture

Next.js App Router with Cache Components on. This Next.js version differs from older ones: before using a framework API, read its guide in `admin/node_modules/next/dist/docs/`.

## Placing new code

| It is | It goes in | Rule |
| --- | --- | --- |
| A decision with no I/O (what a filter means, what a click may change, how a value is labelled) | `src/lib/*.ts` | a pure function with a Vitest file beside it |
| A call to the API | `src/api/incidents.ts` | one typed function per operation, through `unwrap` |
| A page | `src/app/**/page.tsx` | a server component; per-request work inside `<Suspense>` |
| A mutation | `src/app/**/actions.ts` | a `"use server"` action that returns a state value |
| Something interactive | `src/components/*.tsx` with `"use client"` | as small as the interaction needs |
| A primitive | `src/components/ui/` | shadcn/ui, added with its CLI, not hand-written |

Components stay thin: if a component holds an `if` about the domain, move the decision to `src/lib/` and test it there.

## Data flow

- **Types come from the contract.** `npm run gen:api` writes `src/api/schema.d.ts`; it is never edited. `src/lib/domain.ts` re-exports the schema types and holds the runtime enum lists, with `Missing*` checks that fail the type check when the contract gains a value. After a contract change: regenerate, then fix what the compiler reports.
- **Reads happen on the server.** A page awaits `searchParams` and the cookie, calls `src/api/`, and renders. There is no client-side fetching or cache; `createApi` uses `cache: "no-store"`.
- **Anything per-request sits behind `<Suspense>`** (cookies, `searchParams`, API calls), as Cache Components requires. The static shell renders first.
- **Filters live in the URL.** `parseFilters` reads them and drops anything invalid; `toApiQuery` turns them into the API's query. A new filter is added to `Filters`, both functions, the filter bar, and `filters.test.ts`. It is a plain GET form, so it works without JavaScript and a view can be shared as a link.
- **Writes are server actions** used with `useActionState`. After a successful write the action calls `refresh()` so the page re-reads the server; UI state is never updated by hand.

## Errors

`unwrap` produces two errors: `ApiError(status, code, message)` when the API refused, `ApiUnavailableError` when it could not be reached.

- Branch on `error.code`. `UNKNOWN_USER` means the chosen Operator no longer exists: redirect to `/sign-in`.
- Turn failures into values inside a small `try`, then `redirect()` or `refresh()` outside it; both work by throwing and must not be caught.
- A failed action returns `{ status: "error", message, field, values }` so the form keeps what was typed and marks the field.
- `ApiUnavailableError` renders a message that says nothing was saved and what to do.
- A missing Incident calls `notFound()`.

## Forms and rules

- Inputs are uncontrolled with `defaultValue`; the form submits `FormData`. `key={incident.updatedAt}` remounts the fields when the server version changes.
- Everything from `FormData` or the URL is untrusted text: narrow it with a list lookup (`oneOf`) or a pattern before use, on the server.
- A rule is checked in `src/lib/` first for a fast, specific message, and the API remains the authority: handle its refusal as well.
- `buildPatch` sends only fields that differ, so the Activity Log grows only on a real change. A new Operator action is a new `Intent` there, with tests.

## Session and access

There is no login. The sign-in action looks the chosen id up through the API, then sets two `httpOnly`, `sameSite=lax` cookies; `src/proxy.ts` redirects anyone without one. `getOperator()` is the only reader, and every page and action calls it and handles `null`. The Operator's id is sent as `X-User-Id` from the server only; it never appears in client code. The dashboard offers Operator fields only: Status, assignee, Resolution Note.

## Proving it

| The criterion is about | Proof |
| --- | --- |
| A decision | a Vitest test beside the function in `src/lib/` (`npm test`) |
| What the dashboard sends to the API | an `*.itest.ts` in `src/integration/`, run by `scripts/integration/run-integration.sh` against a real API, using the shared fixtures in `scripts/integration/fixtures/` |
| What the page shows | open it in a browser against the running backend (`claude-in-chrome` skill) and describe it in the handoff |

There are no component or end-to-end tests; logic is kept out of components so it can be tested as plain functions. Write the test first and paste it failing, then passing.

Run the Admin gates in `docs/agents/quality-gates.md`. When `next build` or `typecheck` fails on a framework API, the answer is in `node_modules/next/dist/docs/`, not in memory.

## Done when

Each new decision is a tested function in `src/lib/`, every API call goes through `src/api/`, `npm run gen:api` leaves no diff, and the Admin gates are green.
