# 008 visual-redesign

**Goal:** make the dashboard and the phone app look like one modern, considered product, building on the design system from 007, without changing the contract, the business rules or what any request sends.
**Lanes:** mobile / admin

007 fixed usability and stayed visually conservative. This task is the visual pass. Before and after screenshots: `docs/screenshots/redesign/` (the "before" is the state 007 left). The updated tokens and components: `docs/design/design-system.md`.

Run by the main session, which did the implementation itself, with no reviewer pass. See "Open questions".

## What was weak, and what changed

| # | Weakness | Change |
| --- | --- | --- |
| V1 | Status and Severity were two same-shaped tinted pills side by side, so they read as the same kind of thing. | Status is a tinted pill with a dot. Severity is one to four bars and the word. On both platforms. |
| V2 | Type hierarchy came from size alone; titles were regular weight. | Semibold headlines and titles, slightly tightened headlines (mobile `AppTypography`; `tracking-tight` on the web). |
| V3 | Mobile shapes were Material's defaults: 12 dp cards, 4 dp fields. | 20 dp cards, 12 dp fields, fully rounded chips and buttons, 28 dp sheets (`AppShapes`). |
| V4 | A phone list card was a title, two pills and a line of meta. | Status and date on top, title, a two-line description preview, a photo thumbnail when there is one, then Severity and Category. |
| V5 | The phone list had no summary and no way to narrow it. | A chip per Status with its count, and All; tapping one filters the list. An empty filter says so and offers Show all. |
| V6 | Switching technician was an unlabelled exit icon and a dialog. | The Technician's avatar opens a bottom sheet with their name and the switch action. |
| V7 | The phone detail screen was a flat run of label and value text; photos were small thumbnails. | Photos lead, large. Location, Reported and Handling are icon rows in one card. Activity is a dotted list. The "can't be changed" note is a quiet panel. |
| V8 | The form's photo actions were two small text buttons; the selected Severity chip looked like any other. | Two large tiles with icons. The selected Severity chip takes its Severity's colour. A taller submit button. |
| V9 | Screens changed with a plain cross-fade. | A short slide in the direction of travel, with the fade. |
| V10 | The dashboard list opened straight onto filters, with no picture of the queue. | Four tiles (Open, In progress, Unassigned, Resolved): a count each, and a one-click filter. |
| V11 | The dashboard table had seven equal columns. | Five: the title carries Category and reporter as a second line; the assignee has an avatar; the date is right-aligned. Assignee and date drop out on small screens. |
| V12 | The header named the Operator in plain text; sign-in cards were bare. | An avatar beside the name; role under the name on sign-in; a translucent header. |

## Acceptance criteria
Each criterion names the lane that proves it.

1. Given Incidents of several Statuses, when a Status is chosen on the phone list, then only that Status is listed, and choosing All lists everything. *(mobile, `IncidentListViewModelTest` › `aStatusFilterShowsOnlyThatStatusAndClearingItShowsEverything`)*
2. Given a filter, then the counts still cover every Status and the total, and an Incident waiting out its undo window is not counted. *(mobile, `IncidentListViewModelTest` › `countsCoverEveryStatusAndDoNotChangeWithTheFilter`, `anIncidentWaitingOutItsUndoWindowIsNotCounted`)*
3. Given each Severity, then its level rises from 1 to 4; given a name, then its initials are the first letters of up to two names. *(mobile, `LabelsTest`)*
4. Given the dashboard's default view, then the tiles are Open, In progress, Unassigned and Resolved, each links to the list narrowed to it, and none is active. *(admin, `overview.test.ts`)*
5. Given a view narrowed to exactly one tile's filter, then that tile is active and its link clears the filter; given any further filter, then no tile is active; the chosen sorting is kept. *(admin, `overview.test.ts`)*
6. Given a tile, then its count request asks for one row of everything the tile covers, whatever the page's own filters. *(admin, `overview.test.ts` › `toCountQuery`)*
7. Given the dashboard in a browser and the app on an emulator against the seeded backend, then every screen renders in light and dark with nothing clipped or scrolling sideways, and the tile and chip counts match the seed (2 Open, 1 In progress, 2 Unassigned, 1 Resolved on the dashboard; 2, 1, 1, 0 for Aung Kyaw). *(admin, browser check; mobile, emulator run)*
8. Given the existing behaviour, then every existing test in all lanes still passes and `scripts/integration/run-integration.sh` passes. *(all lanes)*

## Contract needs
None. The dashboard's counts use `GET /incidents` with `_limit=1` and read `X-Total-Count`, both already in the contract.

## Contract delta
| Operation | Change | Reason |
| --- | --- | --- |
| none | none | Visual and client-side only. |

## Lane status
| Lane | Status (`TODO` / `IN PROGRESS` / `REVIEW` / `DONE`) | Branch | Notes |
| --- | --- | --- | --- |
| backend | n/a | | No work. |
| mobile | REVIEW | main | No new dependency. Two icons are drawn in `ui/Icons.kt` rather than adding the extended icon set. |
| admin | REVIEW | main | No new dependency. The list page makes four extra single-row requests for the tile counts. |

## Evidence

### Admin
Red first: `overview.test.ts` failed with `Cannot find module './overview'`, then passed.

```
npm run typecheck   ✓ Types generated successfully (tsc: no output)
npm run lint        no findings
npm test            Test Files 3 passed (3) · Tests 47 passed (47)
npm run gen:api     no diff against src/api/schema.d.ts
npm run build       ✓ Compiled successfully
```

Browser check (headless Chrome, signed in as Mya Sandar, seeded backend): sign-in, list, list with the Open tile active, list with no match, detail (Open and Resolved), a refused Resolve and not-found were rendered and looked at; list, detail and sign-in also in dark. The tiles read 2, 1, 2, 1. `scrollWidth` equalled `clientWidth` at 1440, 1280, 768 and 420 px. At 420 px the table first overflowed its container; Assignee now drops below `md` and Reported below `sm`, and it fits. Not checked: a screen reader, Safari and Firefox.

### Mobile
Red first: the new tests failed to compile (`Unresolved reference 'onFilter'`, `'counts'`, `'total'`), then passed.

```
./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test \
  :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test \
  :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64
BUILD SUCCESSFUL

core:domain        3 Android host, 3 iOS simulator, 0 failed
core:data         37 Android host, 31 iOS simulator, 0 failed
feature:incidents 45 Android host, 45 iOS simulator, 0 failed (40 before this task's 5)
shared             9 iOS simulator, 0 failed
lintDebug          0 errors, 7 warnings (the baseline; none new)
```

Emulator run (AVD `Pixel_9`, `emulator-5554` only), signed in as Aung Kyaw: the picker, list, detail (Open, and In progress with the Handling row), new form top and bottom, the account sheet, and the Resolved filter's empty state were looked at in light; the list and detail in dark. The chips read All 2, Open 1, In progress 1, Resolved 0. Both drawn icons (camera, image) render correctly. Not checked on a device: a card or detail screen with real photos (the seed has none), the transitions by eye beyond a normal walk-through, TalkBack, and the iOS UI (the iOS tests and framework link pass).

### All lanes
Backend `npm test`: 21 pass. `scripts/integration/run-integration.sh`: "Integration check passed." `scripts/check-forbidden-terms.sh` and `scripts/check-agents.sh`: exit 0.

## Open questions
- **Review.** No lane reviewer has read this diff, nor 007's. Run `/review` for mobile and admin.
- **Typeface.** The phone still uses the platform font while the dashboard uses Geist. Bundling a font in the app is a separate decision (app size, iOS and Android both).
- **Photos.** The photo-led card and detail layouts were only seen without photos. Worth one look on a device with a real report.
- **Root README phone screenshots** still show the pre-007 look; the three dashboard ones are current.
