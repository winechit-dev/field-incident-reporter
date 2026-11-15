# 011 display-typography

**Goal:** give the dashboard and the phone app one distinctive display typeface for prominent text, and one documented type scale, without touching body text, labels or fields.
**Lanes:** mobile / admin

Before and after screenshots: `docs/screenshots/typography/`. The scale: `docs/design/design-system.md`, "Type".

Run by the main session, which did the implementation itself, with no reviewer pass. See "Open questions".

## What was weak, and what changed

| # | Weakness | Change |
| --- | --- | --- |
| T1 | The phone had no typeface of its own: every title was the platform default at a heavier weight. | Display, headline and `titleLarge` use Plus Jakarta Sans (ExtraBold, and Bold for `titleLarge`), bundled as two font files. Screen titles, sheet titles, an Incident's title, empty-state headings and the account name change through the theme; no call site changed. |
| T2 | The dashboard used Geist for titles and body alike; `--font-heading` existed but pointed at the body face. | `--font-heading` is Plus Jakarta Sans, loaded through `next/font`. Three utilities (`type-display`, `type-headline`, `type-title`) carry it. |
| T3 | Dashboard titles repeated `text-2xl font-semibold`, two of five without `tracking-tight`; empty-state headings and card titles were plain medium-weight body text. | Every page title is `type-headline`; card titles, empty-state headings and the product name are `type-title`; the overview counts are `type-display` with tabular figures. |
| T4 | The type scale was described per use, not per role. | The design system lists all fifteen roles (display, headline, title, body, label; large, medium, small) with face, weight, size and where each is used, on both platforms. |

Unchanged on purpose: body text, tables, field labels, inputs, buttons, badges and chips, on both platforms; and the phone's `titleMedium` and `titleSmall`, which stay on the platform typeface because they carry content (an Incident's title in a list) and section labels.

## Acceptance criteria

1. Given any screen of the phone app, then its title, a sheet's title, an Incident's title and an empty state's heading render in the display face, and body text, labels and fields render in the platform typeface. *(mobile, emulator run)*
2. Given any dashboard page, then its `h1`, card titles and overview counts compute to `Plus Jakarta Sans` and the table, filters and body text to Geist. *(admin, headless Chrome check of computed styles)*
3. Given light and dark, a 1280 px and a 390 px viewport, then nothing is clipped or wraps badly. *(admin, screenshots; mobile, emulator in light and dark)*
4. Given the existing behaviour, then every existing test in both lanes still passes. *(mobile, admin)*

No criterion maps to a new automated test: the change is typeface, weight and tracking only, and the repo has no screenshot tests.

## Contract needs
None.

## Contract delta
None.

## Lane status
| Lane | Status | Branch | Notes |
| --- | --- | --- | --- |
| mobile | REVIEW | main (working tree) | `ui/Theme.kt`, `build.gradle.kts` (`compose.resources`), two font files, the licence |
| admin | REVIEW | main (working tree) | `layout.tsx`, `globals.css`, five page titles, `overview.tsx`, `states.tsx`, `ui/card.tsx` |

## Evidence

**Mobile.** `./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64 :androidApp:lintDebug`: BUILD SUCCESSFUL; lint 0 errors, 4 warnings (the baseline). The APK contains both font files under `assets/composeResources/…/font/`. Run on the Android emulator (1080 × 2424): list, detail, the edit sheet and the account sheet in light, the list in dark. Titles render in the display face; card titles, chips, fields and buttons are unchanged.

**Admin.** `npm run typecheck`, `npm run lint`, `npm test` (53 passed), `npm run build`: green. `npm run gen:api`: no diff. Checked in headless Chrome against the running backend, on a production build: list (light, dark, 390 px), detail, an empty search, sign-in. Computed styles: `h1` Plus Jakarta Sans 800 at 24 px; overview counts 800 at 32 px; card titles 700 at 16 px.

**Not checked.** The iOS app was built and its tests run, but it was not launched on a simulator, so the font's rendering on iOS is unverified. A first production build after the change served the previous CSS from Turbopack's cache until `.next` was cleared; a dev server that was already running may need a restart to show the final weights.

`scripts/check-forbidden-terms.sh` and `scripts/check-agents.sh` exit 0.

## Open questions
- No reviewer pass has run. `/review mobile` and `/review admin` are owed before this reads DONE.
- The iOS rendering check above.
