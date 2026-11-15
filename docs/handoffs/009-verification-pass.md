# 009 verification-pass

**Goal:** find and fix what is still wrong after 007 and 008 by using the product rather than restyling it: run the report flow end to end with a real photo, measure contrast, and exercise the states earlier passes listed as unchecked.
**Lanes:** mobile / admin

Two passes had already changed how everything looks, so this one made no change that did not come from an observed defect. Before and after screenshots: `docs/screenshots/verification/`. Run by the main session, with no reviewer pass.

## How it was audited

| Method | What it covered |
| --- | --- |
| The report flow on an Android emulator, end to end: report with a gallery photo, see it on the list, open it, edit it, delete it with and without Undo | every phone screen with real data, the keyboard, validation, and sync to the server |
| The same Incident on the dashboard | that the photo and the edit arrive; the photo grid |
| A throwaway API with 45 Incidents behind a second copy of the dashboard | pagination across three pages, a page past the last, long titles |
| That API stopped | the API-unavailable panel on the list, detail and sign-in pages |
| WCAG contrast computed for 49 colour pairs across both platforms and themes | every text pair, every badge, and control edges |
| Frames captured during a cold start in dark mode | the launch |

## Findings

| # | Found by | Problem | Fix |
| --- | --- | --- | --- |
| P1 | report flow | Tapping Submit report with no photo did nothing visible: the error was below the fold. Nothing on the form said a photo was needed. | A refused submit scrolls the first problem into view. The Photos heading says "at least one". |
| P2 | report flow | Nothing confirmed a saved report; the form just closed, and the new card arrived half-hidden under the filter chips. | The list scrolls to the top and shows "Report saved". |
| P3 | report flow | With the keyboard open the whole screen panned: the top bar left the screen, the submit bar hid behind the keys, and the focused field sat clipped at the keyboard's edge. | The keyboard resizes the screen. The top bar stays, the field is in view and the submit bar rides above the keys. |
| P4 | report flow | The snackbar's Undo was Material's default purple: `inversePrimary` and six other colour roles were never set. | Every role is set in both schemes. |
| P5 | report flow | An Incident with one photo showed it at a fixed width with a blank gap beside it, and a photo could not be opened: a Technician had no way to check what they had attached. | One photo fills the width. A tap opens any photo full screen, uncropped; several can be swiped. |
| P6 | launch frames | The launcher and the splash showed the stock green Android template icon. | The product's mark on the brand blue, with a monochrome layer for themed icons. |
| P7 | launch frames | 007 had recorded a light frame when launching in dark mode. | The window background follows the system setting and matches the app's. The captured frames go wallpaper, splash, dark app. |
| D1 | contrast | The edges of inputs and dropdowns on the dashboard were 1.5:1 in the light theme and 1.9:1 in the dark, under the 3:1 minimum for a control's boundary. | 3.3:1 and 3.7:1. |
| D2 | 45 Incidents | A page past the last showed an empty list above "41-45 of 45 incidents · Page 3 of 3". | It gives the total and the page count; the empty state already offers the way back. |
| D3 | browser | Applying a filter left every empty field in the URL (`?q=&status=OPEN&severity=&…`), so shared links were long and noisy. | The page redirects to the shortest form (`?status=OPEN`). |

Checked and found sound: sync of a report, its photo and a later edit to the server; delete with Undo and without; pagination and its controls; long-title truncation; the API-unavailable panel; every text and badge colour pair (lowest 5.2:1); the two drawn icons; the photo thumbnail on a list card.

## Acceptance criteria
Each criterion names the lane that proves it.

1. Given a report form that needs a photo, when Submit is refused, then the state names the first problem in screen order and counts the refusal; fixing the problem clears it. *(mobile, `IncidentFormViewModelTest` › `aRejectedSubmitSaysWhichProblemComesFirstSoTheScreenCanShowIt`)*
2. Given a new report on a device that can take photos, then the form state says a photo is required; an edit never does. *(mobile, `IncidentFormViewModelTest` › `theFormSaysUpFrontWhetherAPhotoIsRequired`)*
3. Given a new report was saved, then the list is told so. *(mobile, `IncidentListViewModelTest` › `aNewReportIsConfirmedOnTheList`)*
4. Given a page and a total, then the range is the rows on that page, and null when there are none or the page is past the last. *(admin, `filters.test.ts` › `pageRange`)*
5. Given a URL, then its canonical form drops empty values, defaults, invalid and unknown parameters and rewrites the dropdown's Unassigned value; a canonical URL is left alone; the canonical form of a canonical URL is itself. *(admin, `filters.test.ts` › `canonicalHref`)*
6. Given the fixes, when the same flows are run again on the emulator and in the browser, then each problem above is gone. *(mobile, emulator run; admin, browser check)*
7. Given the existing behaviour, then every existing test in all lanes still passes and `scripts/integration/run-integration.sh` passes. *(all lanes)*

## Contract needs
None.

## Contract delta
| Operation | Change | Reason |
| --- | --- | --- |
| none | none | Client-side only. |

## Lane status
| Lane | Status (`TODO` / `IN PROGRESS` / `REVIEW` / `DONE`) | Branch | Notes |
| --- | --- | --- | --- |
| backend | n/a | | No work. |
| mobile | REVIEW | main | No new dependency. The activity is now `adjustResize` and has its own window theme. |
| admin | REVIEW | main | No new dependency. |

## Evidence

### Admin
Red first: six new tests failed (`pageRange`, `canonicalHref` not exported), then passed.

```
npm run typecheck   ✓ Types generated successfully (tsc: no output)
npm run lint        no findings
npm test            Test Files 3 passed (3) · Tests 53 passed (53)
npm run gen:api     no diff against src/api/schema.d.ts
npm run build       ✓ Compiled successfully
```

Browser (headless Chrome): changing the Status dropdown landed on `/incidents?status=OPEN`, not the long form. The control edges are visibly darker in both themes; contrast recomputed at 3.31:1 (light, on the card) and 3.70:1 (dark, on the card). D2's fix is covered by the `pageRange` tests and was not re-rendered against the 45-row API, which had been stopped by then.

### Mobile
Red first: 17 compile errors in the new tests (`firstProblem`, `rejectedSubmits`, `photoRequired`, `onFormSaved`), then passed.

```
./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test \
  :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test \
  :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64
BUILD SUCCESSFUL

core:domain        3 Android host, 3 iOS simulator, 0 failed
core:data         37 Android host, 31 iOS simulator, 0 failed
feature:incidents 48 Android host, 48 iOS simulator, 0 failed (45 before this task's 3)
shared             9 iOS simulator, 0 failed
lintDebug          0 errors, 4 warnings
```

Lint went from 7 warnings to 4: the new launcher icon removed the two monochrome-icon warnings and the obsolete `drawable-v24` folder. The four left are all newer-dependency notices. `docs/agents/quality-gates.md` now gives 4 as the baseline.

One thing to know when building: after the icon file moved folders, `:androidApp:processDebugResources` failed with "resource drawable/ic_launcher_foreground not found" until `:androidApp:mergeDebugResources --rerun-tasks` was run once.

Emulator (AVD `Pixel_9`, `emulator-5554` only), signed in as Thiri Win, each fix re-run after the change:
- P1: Submit with a title and no photo scrolled to "Add at least one photo" under a heading reading "Photos (0/5) · at least one".
- P3: with the keyboard open on the address field, the top bar stayed, the field was fully visible and Submit report sat above the keys.
- P2: after adding a gallery photo and submitting, the list showed "Report saved" with the new card on top, its thumbnail, and "Syncing…".
- P5: the detail screen showed the photo at full width; a tap opened it uncropped on black with a close button.
- P4: the delete snackbar's Undo was the theme's blue.
- P6, P7: the launcher shows the new icon; five frames captured during a dark cold start averaged the wallpaper, the splash, then `#0E1116`.
- The two test reports were deleted through the app afterwards; the server was back to its four seeded Incidents.
- Not checked on a device: the camera path (the gallery path was used), the photo viewer with several photos, TalkBack, and the iOS UI. The iOS tests and framework link pass.

### All lanes
Backend `npm test`: 21 pass. `scripts/integration/run-integration.sh`: "Integration check passed." `scripts/check-forbidden-terms.sh`: "forbidden-terms: clean". `scripts/check-agents.sh`: exit 0.

## Open questions
- **Review.** None of 007, 008 or 009 has had a lane reviewer. Run `/review` for mobile and admin.
- **iOS.** The launcher icon, window theme and keyboard mode are Android-side. The iOS app keeps its own icon; `imePadding` and the rest are shared and compile for iOS, but were not looked at on the simulator.
- **Legacy launcher icons.** The PNG fallbacks in `mipmap-*dpi` still show the template robot. They are used only below Android 8; the app's minimum is 7.
- **Root README phone screenshots** still show the pre-007 look.
