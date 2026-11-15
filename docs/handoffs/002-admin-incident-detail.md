# 002 admin-incident-detail

**Goal:** an Operator can open an Incident from the list, see everything about it, and triage it: start work, assign, resolve with a note, or reopen.
**Lanes:** admin

## Acceptance criteria
1. Given the list, when an Operator selects an Incident's title, then a detail page shows its description, Status, Severity, Category, location (address and a map link when coordinates exist), photos, reporter, assignee, Resolution Note, activity log and timestamps. An unknown id shows a not-found page. *(admin)*
2. Given an Open Incident, when the Operator chooses "Start work", then Status becomes In progress, they become the assignee if nobody was, and the activity log gains an entry. *(admin, unit test + API)*
3. Given an Incident, when the Operator chooses "Resolve" without a Resolution Note, then nothing is sent and the form asks for one; with a note, Status becomes Resolved and the note is shown. *(admin, unit test + API)*
4. Given a Resolved Incident, when the Operator chooses "Reopen", then Status becomes In progress. *(admin, unit test + API)*
5. Given any Incident, when the Operator changes only the assignee (to an Operator or to Unassigned), then only `assignee` is sent. *(admin, unit test)*
6. Given the submitted form, then only fields that changed are sent; an unchanged form says "No changes"; an assignee that is not an Operator is refused; a note over 2000 characters is refused. *(admin, unit test)*
7. Given the API refuses the change or is unreachable, then the form shows a message and keeps what the Operator typed. *(admin)*

## Contract needs
None. `PATCH /incidents/{id}` already accepts `status`, `assignee` and `resolutionNote` from an Operator.

## Contract delta
None.

## Lane status
| Lane | Status | Branch | Notes |
| --- | --- | --- | --- |
| admin | DONE | main | Photos load from the API via `next/image` with `unoptimized`. |

## Evidence
Run on 2026-11-15 against the mock API, driving headless Chrome as an Operator.

- `npm run typecheck`, `npm run lint`, `npm test` (27 tests, 16 of them new for the triage rules, written red first), `npm run build`: all green. `npm run gen:api` leaves no diff.
- Criterion 1: the list's titles link to the detail page. For the seeded "Hydraulic lift" Incident (with a photo uploaded as a Technician) the page showed description, Status, Severity, Category, address and a map link, the photo loaded from the API, reporter, assignee, timestamps and the activity log. `/incidents/does-not-exist` shows the not-found page.
- Criterion 2: "Start work" on an Open Incident gave server state `IN_PROGRESS`, assignee `u-op-1`, one activity entry.
- Criterion 3: "Resolve" with an empty note showed "Add a resolution note before resolving." and left the server unchanged (Open, no assignee, empty log). With a note it gave `RESOLVED`, the note stored, `resolvedAt` set, a second activity entry, and the page refreshed itself with "Saved."
- Criterion 4: "Reopen" gave `IN_PROGRESS`, `resolvedAt` cleared, the note kept.
- Criterion 5 and 6: choosing another Operator and saving changed only the assignee (activity entries stayed at 3). Saving an unchanged form showed "No changes to save."
- Criterion 7: with the API stopped mid-edit, "Save changes" showed "Can't reach the API at http://localhost:3000. Nothing was saved." and the typed note was still in the field.

## Open questions
- Operator actions use `X-User-Id` from a cookie that anyone can set, because the contract has no real authentication. Fine for this prototype; a real deployment needs proper auth.
- Requiring a Resolution Note to resolve is an admin-side rule only; the contract does not require it.
- After an Operator starts work, a Technician's pending edit from the mobile app gets `409 NOT_OPEN`. That cross-lane scenario belongs in the integration check.
