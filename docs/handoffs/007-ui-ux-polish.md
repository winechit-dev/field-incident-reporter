# 007 ui-ux-polish

**Goal:** fix the usability problems found by auditing all three lanes, and give the dashboard and the phone app one design system, without changing the contract, the business rules or what any request sends.
**Lanes:** mobile / admin / backend (README only)

The audit and its findings (X1-X4, A1-A18, M1-M15, B1-B4): `docs/design/ui-ux-audit.md`. The tokens and components: `docs/design/design-system.md`. Before and after screenshots: `docs/screenshots/ui-ux/`.

This task was run by the main session, which did the implementation itself rather than through the lane agents, and has had no reviewer pass. See "Open questions".

## Acceptance criteria
Each criterion names the lane that proves it.

1. Given the default view, with any sorting and page, when the filters are counted, then the count is 0; given narrowing filters, then each counts once and a date range counts once. *(admin, `filters.test.ts` › `activeFilterCount`)*
2. Given an Incident's Status, when the triage panel picks its main button, then it is Start work for Open, Resolve for In progress and none for Resolved, and it is always one of the actions offered. *(admin, `triage.test.ts` › `primaryAction`)*
3. Given the dashboard in a browser against the running backend, then: the list, empty, filtered, detail, error and sign-in views render in light and dark; nothing scrolls sideways at 1440, 768 and 420 px; Tab moves in reading order starting at "Skip to content"; clicking anywhere on a row opens the Incident; changing a dropdown applies the filter. *(admin, browser check)*
4. Given a new report form, when nothing has been entered, then it has no unsaved changes; when a title or a photo is added, then it has; when only whitespace is typed, then it has not. *(mobile, `IncidentFormViewModelTest` › `aNewReportHasUnsavedChangesOnceSomethingIsEntered`)*
5. Given an edit form, then it has unsaved changes only while a field differs from the stored Incident. *(mobile, `IncidentFormViewModelTest` › `anEditHasUnsavedChangesOnlyWhileAFieldDiffersFromTheIncident`)*
6. Given a form that was saved, then it has nothing unsaved, so closing it never asks. *(mobile, `IncidentFormViewModelTest` › `aSavedFormHasNothingUnsaved`)*
7. Given a back stack of list, detail and form, when the navigator is saved and restored, then the same screens are there, the form keeps its view-model key, and the next form opened gets a new one; an unreadable snapshot starts on the list. *(mobile, `NavigatorTest` › the three `aRestored…` / `aSnapshot…` tests)*
8. Given a time from the server, when it is shown, then it is in the viewer's zone as `10 Nov 2026, 14:45`, landing on the local day near midnight. *(mobile, `LabelsTest`)*
9. Given every Status and Severity, then each has its own tone in the light and the dark theme. *(mobile, `LabelsTest` › `everyStatusAndSeverityHasItsOwnToneInBothThemes`)*
10. Given the app on an Android emulator against the seeded backend, then: every screen renders in light and dark; rotating on the detail screen and on a half-filled form keeps the screen and the text; Back on a form with text asks before discarding; Submit report, Edit and Delete are visible without scrolling. *(mobile, emulator run)*
11. Given the existing behaviour, then every existing test in all three lanes still passes unchanged and `scripts/integration/run-integration.sh` passes. *(all lanes)*

## Contract needs
None.

## Contract delta
| Operation | Change | Reason |
| --- | --- | --- |
| none | none | UI and documentation only. Audit findings B2-B4 are suggestions for `contract-architect`, listed under "Open questions". |

## Lane status
| Lane | Status (`TODO` / `IN PROGRESS` / `REVIEW` / `DONE`) | Branch | Notes |
| --- | --- | --- | --- |
| backend | REVIEW | `007-ui-ux-polish` | `README.md` only (finding B1). No code or contract change. |
| mobile | REVIEW | `007-ui-ux-polish` | Built on top of 006 (`1ce6e8a`). Two new dependencies in `:feature:incidents`: `material-icons-core` 1.7.3 and `kotlinx-datetime` 0.8.0. |
| admin | REVIEW | `007-ui-ux-polish` | No new dependency. |

## Evidence

### Admin
Red first: the four new tests failed with `activeFilterCount is not a function` / `primaryAction is not a function` (`4 failed | 35 passed`), then passed.

```
npm run typecheck   ✓ Types generated successfully (tsc: no output)
npm run lint        no findings
npm test            Test Files 2 passed (2) · Tests 39 passed (39)
npm run gen:api     no diff against src/api/schema.d.ts
npm run build       all routes built (Partial Prerender)
```

Browser check, done by the main session with headless Chrome driven over the DevTools protocol, signed in as Mya Sandar against the seeded backend:
- Sign-in, list, list with no match, filtered list, detail (Open and Resolved), a refused Resolve, and not-found were each rendered and looked at. Light and dark were both rendered for sign-in, list and detail.
- `scrollWidth` equalled `clientWidth` at 1440, 768 and 420 px on the list and at 1440 and 768 px on the detail page: no sideways scroll.
- Real Tab key presses went: Skip to content, product name, Switch operator, search, Status, Severity, Category, Assignee, the two dates. A real mouse click at the far right of a row opened that Incident. Changing the Status dropdown navigated to `status=OPEN` and showed the two Open rows.
- Resolve with an empty note showed "Add a resolution note before resolving." under a red-bordered note field.
- The development "1 Issue" overlay (finding A1) is gone on all three routes.
- Not checked: a screen reader, Safari and Firefox, and the API-unavailable panel in a browser (the backend was kept running; the panel's logic is the same branch as before).

One thing to know when checking by hand: after `globals.css` changed, the running `next dev` kept serving the old tokens until `.next/dev` was deleted and the server restarted.

### Mobile
Red first: the new tests failed to compile (`Unresolved reference 'hasUnsavedChanges'`, `'restore'`, `'snapshot'`, `Too many arguments for 'fun Instant.display()'`), then passed.

```
./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test \
  :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test \
  :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64 :androidApp:lintDebug
BUILD SUCCESSFUL

core:domain        3 Android host, 3 iOS simulator, 0 failed
core:data         37 Android host, 31 iOS simulator, 0 failed
feature:incidents 40 Android host, 40 iOS simulator, 0 failed (31 before this task's 9)
shared             9 iOS simulator, 0 failed
lintDebug          0 errors, 7 warnings (the baseline; none new)
```

Emulator run (AVD `Pixel_9`, `emulator-5554` only; the attached phone was not touched), signed in as Aung Kyaw against the seeded backend:
- The picker, list, detail (Open, and In progress with "Mya Sandar is handling this incident."), new form, edit form and form with errors were each walked and looked at in light; the list and detail in dark.
- Times read `10 Nov 2026, 15:15` in the emulator's zone (UTC+7), matching the dashboard.
- Rotating on the detail screen kept the detail screen. Before the fix it returned to the list.
- On a new form with "Leak" typed, rotating to landscape and back kept the form and the text.
- Back on that form showed "Discard this report?"; Discard returned to the list.
- Delete and Edit, and Submit report with its caption, sat in the bottom bar above the navigation bar.
- Not checked on a device: the camera, gallery and location buttons (unchanged behaviour, only wrapped in a `FlowRow`), a photo strip with real photos, the picker's error state, TalkBack, and the iOS simulator's UI (the iOS tests and framework link pass).

### Backend
`npm test`: 21 tests, 21 pass. README only.

### Integration
`scripts/integration/run-integration.sh`: "Integration check passed."

### All lanes
`scripts/check-forbidden-terms.sh`: "forbidden-terms: clean", exit 0. `scripts/check-agents.sh`: "check-agents: 8 agents, 11 skills, 34 docs agree", exit 0.

## Open questions
- **Review.** No lane reviewer has read this diff. Run `/review` for mobile and admin before it is treated as `DONE`.
- **Contract suggestions for `contract-architect`** (audit B2-B4, all additive): list `400 BAD_JSON`, `405 METHOD_NOT_ALLOWED` and the upload codes in the contract; consider `404` for an unknown path; consider an enum for `Error.code`.
- **Root README screenshots.** The three dashboard screenshots were retaken. The five phone screenshots in the root README still show the old look, because they tell a story (a photo, an offline edit) that needs a device session to restage.
- **Android launch frame.** The window theme is still light, so a dark-mode launch shows one light frame (audit, mobile "left alone").
