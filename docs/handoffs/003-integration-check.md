# 003 integration-check

**Goal:** confirm the three lanes agree when used together: the API, the Android app and the Operator dashboard, running at once against one dataset.
**Lanes:** backend, mobile (Android), admin

## Scenarios
Each runs against the seeded API, the app on an emulator, and the dashboard in a browser.

1. **Report to triage.** Given a Technician reports an incident with a photo and GPS location, when an Operator opens the dashboard, then the incident is listed and its detail shows the same title, Status, Severity, Category, address, coordinates and photo.
2. **Operator change reaches the phone.** Given that incident, when the Operator starts work on the dashboard and the Technician refreshes the app, then the app shows *In progress*.
3. **Conflict on edit.** Given a Technician edits an Open incident with no network, when an Operator starts work on it from the dashboard and the network returns, then the app's sync is refused with `409`, the server's version wins, the local edit is discarded, and the app shows *Sync failed* with the reason; the server's title is unchanged.
4. **Conflict on delete.** Given a Technician deletes an Open incident with no network, when an Operator starts work on it and the network returns, then the delete is refused, the incident comes back in the app with the server's Status and a failure notice, and it still exists on the server.
5. **Offline create.** Given a Technician reports an incident with a photo while offline, then it shows *Waiting to sync* and is absent from the dashboard; once the network returns it syncs unaided and appears on the dashboard with its photo.

## Contract needs
None.

## Contract delta
None.

## Lane status
| Lane | Status | Branch | Notes |
| --- | --- | --- | --- |
| backend | DONE | main | |
| mobile | DONE | main | Android only. iOS is not part of this check. |
| admin | DONE | main | |

## Evidence
Run on 2026-11-15, by hand, with the seeded API, the debug build on a Pixel 9 emulator, and the dashboard in headless Chrome as an Operator. The phone's network was switched off and on with `svc wifi` / `svc data`. Nothing in the apps was edited to make a scenario pass.

| # | Scenario | Result |
| --- | --- | --- |
| 1 | Report to triage | **Pass.** Reported "Cracked floor near loading dock" (Safety, High) with address "Dock 2", GPS 16.8409, 96.1735 and a gallery photo. The API held every field and one photo. The dashboard listed it, and its detail page showed the same title, Status, Severity, Category, address, coordinates, reporter and time, with the photo loaded. |
| 2 | Operator change reaches the phone | **Pass.** Resolved the seeded "Pothole" on the dashboard with a note (server: `RESOLVED`). After a pull-to-refresh the app showed *Resolved*. |
| 3 | Conflict on edit | **Pass.** With the phone offline, edited "Hydraulic lift leaking oil" to "... - field note" (phone: *Waiting to sync*, server unchanged). The Operator started work on the dashboard. When the network returned, the app synced unaided; the server refused the edit; the app reverted to the server's title, showed *In progress*, a *Sync failed* badge and "Your change wasn't saved: Incident is already being handled." The server's title never changed and its activity log held only the Operator's entry. OK dismissed the notice. |
| 4 | Conflict on delete | **Pass.** With the phone offline, deleted the Open "Cracked floor" incident and let the undo window pass (phone: hidden; server: still HTTP 200). The Operator started work on it. When the network returned, the delete was refused; the incident reappeared on the phone as *In progress* with the same failure notice. On the server it still exists, photo included. |
| 5 | Offline create | **Pass.** With the phone offline, reported "Offline report from the field" with a photo (phone: *Waiting to sync*; API: no match; dashboard: no match). After the network returned, the badge cleared on its own and the API held the incident with its photo. The dashboard listed it and its detail showed the photo. |

Final API state: every incident matches what the scenarios should have produced, and none of the phone's discarded edits reached the server.

**How it was run, and limits**
- Android and the dashboard only. iOS was not part of this check.
- I did not measure how long the offline create took to sync: my polling loop had a quoting bug and ran blind. The outcome above is checked directly instead.
- Several of my blind tap sequences missed (a mistimed picker tap, a wrong button position, and one stray tap that opened Chrome's first-run prompt on the emulator). These were harness mistakes, not app faults; each was redone with a screenshot check.
- This was a manual run. It is not an automated test, so it will not catch a regression by itself.

## Open questions
- None from the scenarios. Every one behaved as the contract and the ADRs describe.
- Scenarios 3 and 4 are now automated (see below). Scenarios 1, 2 and 5 are still manual, and so is anything about what the screen shows (the badge, the notice).

## Automated conflicts
`scripts/integration/run-conflicts.sh` starts a throwaway API and runs two tests against it:
- **Dashboard** (`admin/src/integration/start-work.itest.ts`, `npm run test:integration`): the dashboard's real triage code and typed client. "Start work" must send exactly `scripts/integration/fixtures/start-work-patch.json`, the API must accept and record it, and a Technician's later edit and delete must then be refused with `409 NOT_OPEN`.
- **App** (`mobile/core/data/src/androidHostTest/.../ConflictIntegrationTest.kt`): the app's real `SyncEngine`, repository and Ktor client over real HTTP, with an in-memory store. "Offline" means not calling `sync()`. The Operator step sends the same fixture. It checks scenario 3 (edit discarded, server version shown, `CONFLICT` failure with the server's message, nothing left to retry, server unchanged) and scenario 4 (the incident returns to the list, is marked failed, and still exists on the server), plus a control that an ordinary report syncs.

The two tests share the fixture, so if the dashboard changes what "Start work" sends, its test fails until the fixture is updated, and the app's test follows.

Both skip themselves when no API is configured, so `npm test` and the normal Gradle runs are unaffected (the app test then shows as 3 skipped, not passed).

**Checked that it can fail.** With the backend's Open-only rule disabled, the dashboard test failed (`expected 200 to be 409`) and the app test failed. With the app's conflict handling replaced by a plain rejection, the control still passed and both conflict tests failed (for example "the local edit is discarded: expected '…oil' but was '…oil - field note'"). Both changes were reverted.

**What it does not cover:** the on-screen result (the badge and the notice), real device networking, the iOS app, and the dashboard's UI (it runs the dashboard's code, not a browser).
