# 005 unassigned-filter

**Goal:** an Operator can filter the dashboard's Incident list to show only Unassigned Incidents (`assignee` is `null`), combined with every other filter and with paging.
**Lanes:** contract / backend / admin

Mobile has no work. The app lists only the Technician's own Incidents (`GET /incidents?reportedBy=<id>&_sort=createdAt&_order=desc` in `KtorIncidentRemote.kt`) and never sends `assigned`. The `Incident` schema, its fields and every enum are unchanged, so `Dtos.kt` and the mappers need nothing. The new `422` cannot reach the app, because it only fires when `assigned` is sent.

## Acceptance criteria
Each criterion names the lane that proves it. Seed data has two Unassigned Incidents (one `OPEN`/`HIGH`, one `OPEN`/`LOW`) and two assigned ones (`u-op-1`, `u-op-2`).

1. Given the seed, when an Operator requests `GET /incidents?assigned=false`, then `200` and every row has `assignee: null`, and no Unassigned Incident is missing. *(backend)*
2. Given the seed, when an Operator requests `GET /incidents?assigned=true`, then `200` and every row has a non-null `assignee`, and no assigned Incident is missing. *(backend)*
3. Given the seed, when an Operator requests `GET /incidents?assigned=false&severity=HIGH`, then only Unassigned `HIGH` Incidents come back; the same holds for `status`, `category`, `reportedBy`, `q` and the `createdAt_gte`/`createdAt_lte` range. *(backend)*
4. Given two Unassigned Incidents, when an Operator requests `GET /incidents?assigned=false&_page=1&_limit=1`, then one row comes back and `X-Total-Count` is `2` (matching rows only, not all Incidents); `_page=2` returns the other one; `_sort`/`_order` order the filtered rows. *(backend)*
5. Given the seed, when an Operator requests `GET /incidents?assigned=false&assignee=u-op-1`, then `200` with `[]` (and `X-Total-Count: 0` when paged); `assigned=true&assignee=u-op-1` returns the same rows as `assignee=u-op-1`. *(backend)*
6. Given any user, when they request `GET /incidents?assigned=maybe`, `assigned=` (empty), `assigned=1` or `assigned=false&assigned=true`, then `422` with `error.code` `VALIDATION_ERROR`. *(backend)*
7. Given no `X-User-Id`, when `GET /incidents?assigned=false` is requested, then `401` (identity is checked before the query). *(backend)*
8. Given a request without `assigned`, when any existing filter is used, then the result is unchanged from today, including `GET /incidents?reportedBy=<tech>` as the mobile app sends it. *(backend, regression)*
9. Given the dashboard list, when an Operator picks "Unassigned" in the assignee filter, then the URL records it, the list shows only Unassigned Incidents, the count and page links reflect only those, and reloading or sharing the URL shows the same list. *(admin, unit test on `parseFilters`/`toApiQuery`/`toSearchParams` + manual run against the mock API)*
10. Given the assignee filter, then "Unassigned" and a specific Operator are mutually exclusive: choosing one clears the other, and the API query never carries both `assigned=false` and `assignee`. *(admin, unit test)*
11. Given a hand-edited URL with an invalid value for the Unassigned filter, when the page loads, then the value is ignored (no filter, no error page), as for every other filter; an "Unassigned" filter counts as active for `hasActiveFilters`. *(admin, unit test)*
12. Given the mobile app, when it syncs as a Technician, then its list request and results are unchanged. *(mobile: no change; proved by backend criterion 8, no mobile task)*

## Contract needs
- Admin: a way to ask `GET /incidents` for Incidents whose `assignee` is `null`, with `X-Total-Count` that counts only those rows. Today `assignee` is equality on an id only, and json-server 0.17 equality cannot match `null`.

## Contract delta
| Operation | Change | Reason |
| --- | --- | --- |
| `GET /incidents` | New optional query parameter `assigned` (boolean). `false`: only Incidents with `assignee: null`. `true`: only Incidents with an assignee. Absent: no filter on assignee. | A separate boolean, not a sentinel value on `assignee` (such as `assignee=none` or `assignee=null`): a sentinel shares the id space, so a user id could collide with it, the generated admin type stays a plain string that hides the special case, and it changes the meaning of an existing parameter. A boolean is additive, typed in the generated client, and `true` comes at no extra cost. |
| `GET /incidents` | Combination rules stated in the description: all filters AND together. `assigned=false&assignee=<id>` is a valid query that matches nothing (`200 []`), not an error; `assigned=true&assignee=<id>` equals `assignee=<id>`. Sorting, paging and `X-Total-Count` apply after every filter, `assigned` included. | AND is how every other filter already behaves, so clients need no special case; an empty result is what the query literally asks for. Stating the `X-Total-Count` rule makes it testable: a filter applied after json-server pages would return short pages with a wrong total. |
| `GET /incidents` | New response `422` with `error.code` `VALIDATION_ERROR` when `assigned` is anything other than exactly `true` or `false`, or is repeated. `X-Total-Count` description tightened. `info.version` 1.0.0 to 1.1.0. | Without it an invalid value would be passed to json-server as an equality filter on a non-existent field and silently return `[]`, which looks like "no Incidents". Reuses the existing validation code; no new code. Additive: only requests that send `assigned` can get it, so no existing client sees a new status. |

Unchanged: the `Incident` schema, every other operation, every enum. `GLOSSARY.md`: the Assignee entry now defines Unassigned, the UI label for `assignee: null` (already used in the detail page's assignee dropdown).

## Tasks per lane
Merge order: contract (this change) → backend → admin. Admin can start in parallel with backend from the spec and seed; it needs the backend only for its manual run and integration check.

**backend-implementer** (branch `005-backend`)
- Red first in `test/api.test.js`: one test per criterion 1-8, including the `422` and the `401`.
- Make `GET /incidents` honour `assigned` as specified. json-server 0.17 cannot express it, so the backend adds behaviour in `src/app.js`. Whatever the approach, `assigned` must never reach json-server as an equality filter, and `X-Total-Count`, `_page`, `_limit`, `_sort` and `_order` must work on the filtered set. Filtering inside `router.render` runs after json-server has paged and counted, so it would break criterion 4.
- Keep seed data as is unless a criterion needs another case. If you add one, keep every row valid under the contract.
- Update `backend/README.md` if it lists the supported filters.

**admin-implementer** (branch `005-admin`)
- `npm run gen:api` and commit the regenerated `src/api/schema.d.ts` (it gains `assigned?: boolean` and the `422`).
- Add an "Unassigned" option to the assignee dropdown. Choose a URL form that cannot collide with an Operator id. The current `assignee` URL check accepts any `[A-Za-z0-9-]{1,64}`, so a sentinel such as `assignee=none` would be read as an id. A separate URL key mirroring the API (for example `assigned=false`) avoids that. Map it in `parseFilters`, `toApiQuery`, `toSearchParams` and `hasActiveFilters` (`src/lib/filters.ts`) per criteria 9-11.
- Show a `422` from the list like any other API error (it should never happen, because `toApiQuery` only sends valid values).
- Document step: remove "Filtering for unassigned Incidents" from "Not built yet" in `admin/README.md` and add Unassigned to the filter list. Also remove "filtering for unassigned incidents" from the "Not built" row in the root `README.md` (line ~345), once the backend and admin lanes are both `DONE`.

**mobile**: no task (see the top of this file).

## Lane status
| Lane | Status (`TODO` / `IN PROGRESS` / `REVIEW` / `DONE`) | Branch | Notes |
| --- | --- | --- | --- |
| contract | DONE | main (uncommitted) | `backend/openapi.yaml` 1.1.0 parses; `openapi-typescript` generates `assigned?: boolean`. |
| backend | DONE | | Criteria 1-8 each have a passing test in `backend/test/api.test.js`. Reviewed: no blockers. |
| mobile | n/a | | No change needed; see the top of this file. |
| admin | DONE | | Criteria 9-11 each have passing Vitest tests in `admin/src/lib/filters.test.ts`. Reviewed: no blockers. Rendered pages checked with `curl`; keyboard, dark mode and narrow-window checks not done (no browser available). |

## Evidence
Contract: `ruby -ryaml -e 'YAML.load_file("backend/openapi.yaml")'` exits 0. `GET /incidents` lists `200`, `401`, `422`. `openapi-typescript` (admin's `gen:api` tool) generates the spec without errors, output written to a scratch path, `admin/` untouched.

### Backend (backend-implementer)
**Approach.** `src/app.js` adds `app.get('/incidents')` before the json-server router. With no `assigned` it calls `next()` and the request is unchanged. With `assigned` it validates the value (`true` or `false`, once), deletes the key from `req.query`, filters the rows to Assigned or Unassigned, and runs them through a request-scoped json-server router. json-server therefore filters, counts (`X-Total-Count`), pages and sorts only the matching rows. `assigned` never reaches json-server. `router.render` moved into `renderIncidents`, which the scoped router also uses, so photo URLs are still rewritten.

**Tests added** (`backend/test/api.test.js`, one per criterion; the oracle is the unfiltered `GET /incidents` so the tests pass in any order):
- criterion 1: `assigned=false` returns every Unassigned row and nothing else.
- criterion 2: `assigned=true` returns every assigned row and nothing else.
- criterion 3: `assigned=false` ANDed with `severity`, `status`, `category`, `reportedBy`, `q`, and both `createdAt` bounds.
- criterion 4: paging over `assigned=false` visits every Unassigned row once; `X-Total-Count` equals the Unassigned count; `_sort`/`_order` apply to the filtered set.
- criterion 5: `assigned=false&assignee=u-op-1` gives `200 []` and `X-Total-Count: 0`; `assigned=true&assignee=u-op-1` equals `assignee=u-op-1`.
- criterion 6: `422 VALIDATION_ERROR` for `maybe`, empty, `1`, `TRUE`, and a repeated parameter, for a Technician and an Operator.
- criterion 7: no `X-User-Id` gives `401 UNAUTHENTICATED` (also for `assigned=maybe`).
- criterion 8: the mobile list shape (`reportedBy` plus `_sort`) and `severity=HIGH` without `assigned` match the unfiltered list.

**Red** (before `src/app.js` changed; `npm test` exit 1):
```
✖ assigned=false returns every Unassigned Incident and nothing else (criterion 1)
✖ assigned=true returns every assigned Incident and nothing else (criterion 2)
✖ assigned=false combines with every other filter by AND (criterion 3)
✖ assigned is applied before paging, counting and sorting (criterion 4)
✖ assigned=false with assignee matches nothing; assigned=true equals assignee (criterion 5)
✖ assigned must be exactly true or false, given once (criterion 6)
ℹ tests 21  ℹ pass 15  ℹ fail 6
```
Criterion 6 failed with `assigned=maybe` returning `200 !== 422`. Criteria 7 and 8 were already green before the change: 7 because the identity middleware runs first, and 8 is a regression guard. Both stay as guards.

**Green** (`npm test`, final):
```
✔ assigned=false returns every Unassigned Incident and nothing else (criterion 1)
✔ assigned=true returns every assigned Incident and nothing else (criterion 2)
✔ assigned=false combines with every other filter by AND (criterion 3)
✔ assigned is applied before paging, counting and sorting (criterion 4)
✔ assigned=false with assignee matches nothing; assigned=true equals assignee (criterion 5)
✔ assigned must be exactly true or false, given once (criterion 6)
✔ identity is checked before the assigned query (criterion 7)
✔ requests without assigned are unchanged, including the mobile list (criterion 8)
ℹ tests 21  ℹ pass 21  ℹ fail 0   (exit 0)
```

**Gates**
- `npm test`: green, 21 of 21, exit 0.
- `openapi.yaml` parses (`ruby -ryaml`): exit 0. The `GET /incidents` codes 200, 401 and 422 are each produced by a test.
- `seed.json`: loads; 4 Incidents covering `OPEN`, `IN_PROGRESS` and `RESOLVED`; 2 Unassigned; file unchanged.
- `scripts/check-forbidden-terms.sh`: `forbidden-terms: clean`, exit 0.
- `scripts/check-agents.sh`: not run; no agent, skill, command or workflow doc changed.
- Smoke run (`npm run reset && npm start`, then `curl`, server stopped afterwards):
```
== assigned=false (Operator)   HTTP/1.1 200 OK    rows 001:null 004:null
== assigned=true               rows 002:u-op-1 003:u-op-2
== assigned=false _page=2&_limit=1   HTTP/1.1 200 OK   X-Total-Count: 2
== assigned=maybe             HTTP/1.1 422   {"error":{"code":"VALIDATION_ERROR",...}}
== no X-User-Id                HTTP/1.1 401   {"error":{"code":"UNAUTHENTICATED",...}}
== mobile list reportedBy=u-tech-1&_sort=createdAt&_order=desc   200
db.json equals seed.json; server stopped
```
`backend/README.md` lists no filters, so it is unchanged.

**Self-review (`backend-review`)**
1. Contract: pass. 200, 401 and 422 are listed and produced; the body goes through `toIncident`; `VALIDATION_ERROR` is an existing code; the change is additive and `info.version` is 1.1.0.
2. Authorization: pass for this change, nothing to check on writes. Pre-existing and out of scope: any known user can read any Incident, through this route as before.
3. Validation: pass. This is a read route; the `422` happens before any read.
4. Idempotency and state: nothing to check; no writes.
5. Files and inputs: nothing to check; no file path or upload involved.
6. Tests: pass. One test per documented code of `GET /incidents`. The `X-Total-Count` assertions use the computed Unassigned count, not a literal `2`, because earlier tests in the same server add Unassigned Incidents.

**Nit (pre-existing, not changed):** the json-server router's own error handler answers unexpected errors with the stack trace, not `500 INTERNAL`. The new scoped router inherits that. No current request path reaches it.

### Contract needs (backend)
None. Criteria 1-8 are met as written.

### Open questions (backend)
- The handoff's Context says json-server "silently returns `[]`" for an unknown `assigned`. That is not what 0.17.4 does: it drops any query key no row has, so unfixed `GET /incidents?assigned=false` returned every Incident. The fix does not depend on this, but the handoff text is wrong.
- Criterion 4 says "`X-Total-Count` is `2`". That holds only on a fresh seed. The test computes the Unassigned count from the live list, which is correct for a shared server.

### Admin (admin-implementer)
**Approach.** `npm run gen:api` regenerated `src/api/schema.d.ts` (`assigned?: boolean`, the `422`). `Filters` gains `unassigned?: true`. URL key is `assigned=false`, as the API names it. Because the filter bar is a plain GET form and one select cannot write a second key without JavaScript, the Assignee select submits the reserved value `@unassigned` (`UNASSIGNED_OPTION`; `@` is outside the id pattern, so no user id can equal it). `parseFilters` reads either form, `toSearchParams` always writes `assigned=false`, so page links and shared links are canonical. When both an assignee and Unassigned arrive, Unassigned wins and the assignee is dropped, and `toApiQuery` also drops `assignee` when `unassigned` is set. A `422` goes through the existing list error handling; no new code.

**Tests added** (`admin/src/lib/filters.test.ts`, describe "Unassigned filter"): `assigned=false` parse and API query (9); round trip and canonical URL (9); dropdown reserved value cannot be a user id and maps to `assigned=false` (9); exclusive with assignee in parse, query and URL (10); `toApiQuery` never sends both for a hand-built filter (10); no `assigned` when off (10); invalid values `true`, `maybe`, empty, `1`, `FALSE` ignored (11); counts in `hasActiveFilters` (11).

**Red** (before `filters.ts` changed, `npm test`):
```
 FAIL  Unassigned filter > is read from assigned=false in the URL (criterion 9)
 FAIL  Unassigned filter > is written back as assigned=false and survives a round trip (criterion 9)
 FAIL  Unassigned filter > is read from the assignee dropdown's Unassigned option ... (criterion 9)
 FAIL  Unassigned filter > is mutually exclusive with an assignee (criterion 10)
 FAIL  Unassigned filter > never sends both even if built by hand (criterion 10)
 FAIL  Unassigned filter > sends no assigned parameter when off (criterion 10)
 FAIL  Unassigned filter > ignores invalid values (criterion 11)
 FAIL  Unassigned filter > counts as an active filter (criterion 11)
 Test Files  1 failed | 1 passed (2)   Tests  8 failed | 27 passed (35)
```

**Gates (final)**
- `npm run typecheck`: green (exit 0).
- `npm run lint`: green, no output.
- `npm test`: `Test Files 2 passed (2)  Tests 35 passed (35)`.
- `npm run build`: green.
- `npm run gen:api`: reruns with the same `schema.d.ts` (the only diff is the contract change itself).
- `scripts/check-forbidden-terms.sh`: exit 0.
- `scripts/integration/run-integration.sh`: not run (the filters changed, so the main session should run it).

**Owed: browser check** (no browser tools). Against a backend with the seed: pick Unassigned in Assignee and Apply; URL becomes `?assignee=%40unassigned`, list shows the two Unassigned Incidents, count and page links use `assigned=false`; reload and a pasted `?assigned=false` show the same list; `?assigned=maybe` shows the unfiltered list, no error page; Clear appears; choosing an Operator afterwards drops Unassigned. Also keyboard use of the select, dark mode, narrow window.

### Contract needs (admin)
None.

### Not done (admin)
The root `README.md` "Not built" edit was not made, as instructed; the main session owns it.

### Reviews and page check (main session)

**backend-reviewer:** no blockers. Its own run: `npm test` 21/21, smoke run on a spare port, 40 concurrent alternating `assigned=true`/`false` requests each got only their own rows. Nits: a new json-server router is built per filtered request (`backend/src/app.js:216-218`); no test for `assigned[a]=b`, which already returns `422`.

**admin-reviewer:** no blockers. Its own run: typecheck, lint, `npm test` 35/35, build, and `gen:api` rewrote `schema.d.ts` byte for byte. Nits: one view has two URLs (`?assignee=%40unassigned` after a form submit, `?assigned=false` in every link the page builds); the repeated-value assertion sits in the "ignores invalid values" test; any `ApiError` other than `UNKNOWN_USER` reaches Next's generic error page (existing); no `*.itest.ts` for what the dashboard sends for Unassigned.

**Page check.** No browser was available, so the server-rendered list was fetched with `curl` as Operator `u-op-1`, against the working-tree backend on a temp copy of the seed.

| URL | Rows | Assignee select | Count | Clear shown |
| --- | --- | --- | --- | --- |
| `/incidents` | 4 | Anyone | 1–4 of 4 | no |
| `?assignee=%40unassigned` (what the form submits) | 2, both Unassigned | Unassigned | 1–2 of 2 | yes |
| `?assigned=false` | the same 2 | Unassigned | 1–2 of 2 | yes |
| `?assigned=false&severity=HIGH` | 1 | Unassigned | 1–1 of 1 | yes |
| `?assignee=u-op-1` | 1 | that Operator | 1–1 of 1 | yes |
| `?assigned=false&assignee=u-op-1` | the 2 Unassigned | Unassigned | 1–2 of 2 | yes |
| `?assigned=maybe`, `=true`, `=`, `?assignee=%40Unassigned` | 4 | Anyone | 1–4 of 4 | no |

After an Operator assigned one of the two, `?assigned=false` listed 1. With 23 Unassigned Incidents, page 1 showed 20 of 23 with a next link of `/incidents?assigned=false&page=2`, and page 2 showed the other 3; the same next link was built from `?assignee=%40unassigned`.

Not checked: keyboard use of the select, dark mode, a narrow window. `?assigned=false&page=2` with only 2 matches shows "21–2 of 2" beside the empty message; that range text for an out-of-range page predates this change.

`scripts/integration/run-integration.sh`: passed all three phases with these changes in the tree.

### Integration (integration-verifier)

All 12 criteria pass, from its own backend (port 3217, temp seed) and dashboard (port 3317): criteria 1-8 by `curl` against the API, 9-11 by the admin unit tests plus the rendered pages, 12 by reading the mobile list request and running it. `scripts/integration/run-integration.sh` and `scripts/check-forbidden-terms.sh` exit 0; backend 21/21, admin 35/35.

Not verified by anyone: picking Unassigned and pressing Apply in a real browser, keyboard use of the select, dark mode, a narrow window. The mobile build and tests were not re-run, since no mobile file changed.

## Open questions
- Root `README.md` has no lane owner in `parallel-work.md`. This handoff gives the "Not built" edit to admin-implementer at its document step. Reassign it if someone else owns the root README.
- Should the mobile app ever show assignment state to Technicians? Out of scope here; the contract already returns `assignee` on every Incident.
