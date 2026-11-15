# 010 report-sheet

**Goal:** the Report incident form opens as a full-height bottom sheet that slides up over the list, with the form's fields redesigned, keeping every field, rule and request as it is.
**Lanes:** mobile

The request reached this task cut off after "Scrollable Content" in its layout section, and without its screenshot. The third part was taken to be a fixed footer with the submit button and the offline-sync note, which is what the form already had. Before and after screenshots: `docs/screenshots/report-sheet/`.

Run by the main session, with no reviewer pass. Committed together with 009, because both changed the form and could not be separated cleanly.

## What changed

| Area | Before | After |
| --- | --- | --- |
| Presentation | A separate screen reached by a sideways slide. | A sheet that slides up over the list (or over the detail, for an edit), opens at full height under the status bar with rounded top corners, and slides down to close. No half-open state. |
| Dismissal | Close button and back. | Close button, back gesture, or a drag down from the handle or from the top of the fields. With something entered, each asks before discarding. |
| Layout | Top bar, fields, bottom bar. | Header (close, title; a hairline appears once the fields scroll under it), scrolling fields, footer (submit, sync note). On a short screen, such as a phone on its side, the footer scrolls with the fields. |
| Fields | Floating labels inside outlined boxes; a counter line under each. | The label above the field with the count beside it, a placeholder that suggests what to write, a white field on the sheet. The line under a field appears only for an error. |
| Category | Four chips that wrapped onto two uneven rows. | A two-by-two grid of tiles, each with an icon; the chosen one is outlined and ticked. |
| Severity | Four chips. | One four-part scale, Low to Critical, with the level bars; the chosen part takes that Severity's colour. |
| Location | A field, a line of coordinates, an outlined button. | The field with a pin; a full-width "Use my location" that shows "Finding your location…" while it works; then a ticked line saying the phone's location is attached, with the numbers, and the button becomes "Update my location". |
| Photos | Two wide tiles above a strip of thumbnails. | One strip: Take photo and Gallery tiles the size of a photo, then the photos, each with its remove button. "n of 5" beside the heading; "Add at least one photo" under it. |
| Spacing | 20 to 24 dp between sections plus a counter line under each field. | 16 dp between sections, 6 dp inside one. Reduced after feedback during the task that the first version's gaps were too big; Location is now visible without scrolling on a Pixel 9. |

## Acceptance criteria
Each criterion names the lane that proves it.

1. Given a back stack, then the form on top is the sheet and the screen under it is the base: the list for a new report, the detail for an edit; with no form there is no sheet. *(mobile, `NavigatorTest` › `aFormIsASheetOverTheScreenItWasOpenedFrom`)*
2. Given the Technician asks for the phone's location, then the form shows it is being found until an answer comes, found or not; asking is not yet an unsaved change; asking again clears an earlier location error. *(mobile, `IncidentFormViewModelTest` › `askingForTheLocationShowsItIsBeingFoundUntilAnAnswerComes`, `askingAgainClearsTheEarlierLocationError`)*
3. Given the app on an Android emulator, when Report incident is tapped, then the sheet slides up to full height under the status bar; the form can be completed and submitted, after which the sheet slides away and the list shows the new report and "Report saved". *(mobile, emulator run)*
4. Given the sheet with nothing entered, then back and a drag down close it; with text entered, then back, a drag down and the close button each ask first, Keep editing keeps the text, and Discard closes the sheet. *(mobile, emulator run)*
5. Given the keyboard, dark mode, a rotation and a short screen, then the header stays, the focused field is visible, the submit button is reachable, and the sheet and what was typed survive. *(mobile, emulator run)*
6. Given the existing behaviour, then every existing form, list and navigator test passes unchanged, including the unsaved-changes, validation and restore tests. *(mobile)*

## Contract needs
None.

## Contract delta
| Operation | Change | Reason |
| --- | --- | --- |
| none | none | Presentation only. |

## Lane status
| Lane | Status (`TODO` / `IN PROGRESS` / `REVIEW` / `DONE`) | Branch | Notes |
| --- | --- | --- | --- |
| backend | n/a | | No work. |
| mobile | REVIEW | main | No new dependency. `IncidentFormScreen.kt` is now `IncidentFormSheet.kt`. Two more icons (shield, more) are drawn in `ui/Icons.kt`. |
| admin | n/a | | No work. |

## Evidence

Red first: the new tests failed to compile (`Unresolved reference 'locating'`, `'onLocationRequested'`, `'base'`, `'sheet'`), then passed.

```
./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test \
  :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test \
  :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64
BUILD SUCCESSFUL

core:domain        3 Android host, 3 iOS simulator, 0 failed
core:data         37 Android host, 31 iOS simulator, 0 failed
feature:incidents 51 Android host, 51 iOS simulator, 0 failed (48 before this task's 3)
shared             9 iOS simulator, 0 failed
lintDebug          0 errors, 4 warnings (the baseline)
```

`scripts/integration/run-integration.sh`: "Integration check passed." `scripts/check-forbidden-terms.sh` and `scripts/check-agents.sh`: exit 0.

Emulator (AVD `Pixel_9`, `emulator-5554` only), signed in as Thiri Win:
- Tapping Report incident opened the sheet at full height under the status bar, the list dimmed behind it.
- Back with nothing entered closed it. With "Leak" typed: back showed "Discard this report?"; a drag down from the handle showed it too; Keep editing left the text in place; Close then Discard closed the sheet, and reopening gave an empty form. A drag down with nothing entered closed it, as did a drag down starting on the fields while they were scrolled to the top.
- With the keyboard up on the title, the header stayed and Submit report sat above the keys.
- Submit with nothing entered showed "Give the incident a title" under a red-edged field.
- A title, a gallery photo and Submit: the strip showed Take photo, Gallery and the thumbnail with "1 of 5"; the sheet went, and the list showed the report on top with "Report saved" and "Syncing…".
- The edit sheet opened over the detail screen as "Edit incident" with the stored values and "This phone's location is attached"; Discard returned to the detail screen.
- Safety and Critical selected, then dark mode: the sheet stayed open with both selected, Critical in its red. Rotating to landscape kept the sheet; with the footer fixed only a sliver of the fields showed, so the footer now scrolls with them below 480 dp, and both fields were then visible.
- The test report was deleted through the app; the server was back to its four seeded Incidents.
- Not checked: the "Finding your location…" state by eye (the lookup returned before a frame could be captured), the camera path, a tap outside the sheet (the only uncovered area is the status bar), TalkBack, and the iOS simulator's UI.

## Open questions
- **Review.** None of 007 to 010 has had a lane reviewer.
- **The rest of the request.** If the cut-off section asked for more than a fixed footer, it is not covered.
- **iOS.** `ModalBottomSheet` is shared code and the iOS tests and link pass, but the sheet's gestures and keyboard behaviour were not looked at on the simulator.
