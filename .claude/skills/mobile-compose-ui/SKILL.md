---
name: mobile-compose-ui
description: Screens, view models, navigation and visual design in mobile/feature/incidents. Use when adding or changing a Compose Multiplatform screen, a view model's state or events, the back stack, theming, UI copy, accessibility, or anything involving recomposition and the Android or iOS lifecycle.
---

# Compose UI

All UI is Compose Multiplatform in `:feature:incidents` `commonMain` and runs unchanged on Android and iOS. Import from `androidx.compose.*` and `org.jetbrains.androidx.lifecycle`; an Android-only artifact here breaks the iOS link.

## Screen shape

Each screen is a folder with two files: `XScreen.kt` and `XViewModel.kt`.

- The view model exposes one immutable `data class` state as `StateFlow`, plus plain functions for each user intent (`onTitle`, `submit`, `requestDelete`).
- The screen collects with `collectAsStateWithLifecycle()` and receives navigation as lambdas (`onBack`, `onOpen`). A screen never sees the back stack or a repository.
- Loading, empty, loaded and blocked are explicit in the state (`loaded: Boolean`, `DetailState.Missing`, `blockedMessage`). Every new screen renders each of them.
- Validation and error text are computed in the view model and carried in state (`titleError`), so a test can assert them without Compose.
- One-off effects (snackbar, undo offer) arrive as a sealed `Event` from a `Channel`, collected in `LaunchedEffect(viewModel)`.
- Reusable pieces go in `ui/` and take `modifier: Modifier = Modifier` as the last defaulted parameter.

## Navigation and lifecycle

Navigation is a hand-rolled back stack of `Screen` values in `feature/incidents`, with `BackHandler` for the system back gesture. Three screens do not justify a navigation library; propose one in the handoff once deep links or tabs appear.

- A new destination is a new `Screen` subtype, a branch in `App.kt`, and a line each in `Navigator.snapshot()` and `Navigator.restore()`. The back stack is saved with `rememberSaveable`, so a destination missing from those two is lost on rotation.
- A screen that holds entered text asks before discarding it: the view model exposes whether there is anything to lose (`FormState.hasUnsavedChanges`) and the screen adds its own `BackHandler`.
- A view model that is per-visit or per-item gets a `key` in `viewModel(key = ...)`. Without one, the next visit reuses the old state.
- Activity recreation (rotation, dark mode switch, process death) rebuilds composition. State that must survive lives in a view model or `rememberSaveable`; a plain `remember` is for derivable or throwaway values. After a navigation change, rotate the emulator on a non-root screen and confirm the screen stays.
- Launchers for camera, gallery and permissions come from `PlatformCapabilities.remember…` and are created unconditionally at the top of the screen, as `rememberLauncherForActivityResult` requires.
- `MainActivity` calls `enableEdgeToEdge()`. Content goes inside `Scaffold` and applies its `padding`; lists add bottom `contentPadding` so the FAB never covers the last row.

## Design system

There is one theme: `FieldReporterTheme` in `ui/Theme.kt`, applied in `App.kt`. It follows the system's light or dark setting.

- Colour, type and shape come from `MaterialTheme.colorScheme`, `.typography` and `.shapes`. Literal colours live in two places only: the two schemes in `ui/Theme.kt`, and the Status and Severity tones in `ui/Labels.kt`, each with a light and a dark pair and a `label()` beside it.
- The values are shared with the dashboard. `docs/design/design-system.md` lists them; a value changed here is changed in `admin/src/app/globals.css` or `status-badges.tsx` in the same task.
- Spacing uses the existing steps: 4, 8, 12, 16 dp, and 32 dp for empty states. Wrap a screen's content in `ContentWidth` so it stays readable on a tablet and in landscape.
- Reuse the pieces in `ui/Components.kt`: `StatusPill`, `SeverityPill` and `IncidentPills` for Status, Severity and Sync State; `AppCard` for a card; `EmptyState` for nothing-here and could-not-load; `Notice` and `FailureBanner` for a refusal; `SectionLabel` for a group heading; `ChoiceChips` for a single choice.
- A screen has one filled `Button`: its main action. When the content scrolls, the main action goes in the `Scaffold`'s `bottomBar` with `navigationBarsPadding()`, so it is always reachable.
- Icons come from `material-icons-core` (`Icons.Filled`, `Icons.AutoMirrored.Filled`). The extended set is not a dependency.
- Times are shown with `Instant.display()`: the viewer's zone, in the dashboard's format.
- UI copy uses glossary terms and sentence case, and says what happens next ("Saved on this device first. It syncs when you're online.").
- Copy is currently Kotlin string literals. Moving it to Compose resources is a single dedicated task; until then keep new copy next to the code that shows it, and keep all enum-to-text mapping in `ui/Labels.kt`.

## Accessibility

- Every `Image` and `Icon` has a `contentDescription`, or `null` when purely decorative next to a text label.
- A screen or section title is marked `semantics { heading() }`. A message that appears away from the control that caused it is a polite live region.
- Touch targets are at least 48 dp; a custom clickable row uses `Modifier.clickable` on the whole row.
- Meaning carried by colour is also carried by text (a `Pill` always has a label).
- Text scales: no fixed heights around text, `maxLines` with `TextOverflow.Ellipsis` for titles.

## Recomposition

- `LazyColumn` items pass a stable `key` (`items(list, key = { it.id })`).
- State classes hold immutable values and `List`s that are replaced, never mutated.
- Derive in the view model, not in composition. Inside a composable, wrap an expensive derivation in `remember(inputs)`.
- Pass method references (`viewModel::onTitle`) or remembered lambdas to children that recompose often.
- Read fast-changing state (scroll offset) as late as possible, in a lambda modifier.

## Proving it

View-model behaviour is proved by a `commonTest` (see `mobile-testing`). What the screen shows is proved by running it: launch on the emulator with the `run` skill, walk the changed flow, including the offline case and a rotation, and write what you saw under "Evidence" in the handoff.

## Done when

The state has a test for each new intent, every state variant renders, the flow was walked on the emulator, and the iOS framework links.
