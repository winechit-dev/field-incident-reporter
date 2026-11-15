# 001 admin-scaffold

**Goal:** an Operator can open the dashboard, choose who they are, and see the Incidents Technicians reported, filtered through the URL.
**Lanes:** admin

## Acceptance criteria
1. Given the backend is running, when an Operator picks their name on the sign-in page, then they land on the Incident list and the choice is remembered. *(admin)*
2. Given Incidents exist, when the list loads, then each row shows title, Status, Severity, Category, reporter and reported time. *(admin)*
3. Given filter fields (Status, Severity, Category, assignee, date range, search), when an Operator sets them, then the URL carries them and the API is queried with matching parameters; invalid values in the URL are ignored. *(admin, unit test)*
4. Given `openapi.yaml`, when `npm run gen:api` runs, then `src/api/schema.d.ts` has no diff. *(admin)*
5. Given a fresh clone, when `typecheck`, `lint`, `test` and `build` run, then all are green. *(admin)*

## Contract needs
None. Filters and `X-Total-Count` paging already exist in the contract.

## Contract delta
None.

## Lane status
| Lane | Status | Branch | Notes |
| --- | --- | --- | --- |
| admin | DONE | main | Detail page and Operator actions (Status, assignee, Resolution Note) are the next task. |

## Evidence
Run on 2026-11-15 against the mock API seeded from `backend/seed.json`.

- `npm run typecheck`, `npm run lint`, `npm test` (11 tests), `npm run build`: all green.
- `npm run gen:api` leaves no diff against the committed `src/api/schema.d.ts`.
- Criterion 1: an unauthenticated request to `/incidents` redirects (307) to `/sign-in`; the sign-in form lists only Operators; submitting it returns 303 to `/incidents` with HttpOnly cookies; the page then reads "Signed in as Mya Sandar".
- Criterion 2: the list shows all four seeded Incidents with title, Status, Severity, Category, reporter, assignee and reported time (checked in headless Chrome; times shown in the browser's zone).
- Criterion 3: against the running API, `status=OPEN` gives 2 rows, `status=RESOLVED&severity=CRITICAL` gives 1, `category=ROAD_HAZARD` gives 1, `assignee=u-op-2` gives 1, `q=pallet` gives 1, `from=2026-11-12&to=2026-11-13` gives 2, `status=OPEN&severity=CRITICAL` shows the empty state, and `status=DONE&severity=x&from=2026-02-31&page=-3&sort=title` is ignored and shows all four. The form keeps the chosen values.

## Open questions
- The contract cannot filter for "unassigned" Incidents (`assignee` is an equality filter). Ask `contract-architect` when the triage view needs it.
