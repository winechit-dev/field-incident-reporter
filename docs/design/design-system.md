# Design system

One product, two platforms. The dashboard and the phone app share an identity (the brand blue, the neutrals, the Status and Severity hues, the date format and the wording) and each keeps its platform's conventions: shadcn/ui and Tailwind on the web, Material 3 on the phone.

Where the values live:

| Platform | Tokens | Components |
| --- | --- | --- |
| Admin | `admin/src/app/globals.css` | `admin/src/components/ui/` (primitives), `admin/src/components/` (app components) |
| Mobile | `mobile/feature/incidents/.../ui/Theme.kt` (scheme), `ui/Labels.kt` (Status and Severity tones) | `mobile/feature/incidents/.../ui/Components.kt`, `ui/PhotoStrip.kt` |

A value changed on one platform is changed on the other in the same task.

The product's mark is a report on a clipboard, white on the brand blue: the dashboard's header, the phone's sign-in screen and the Android launcher icon (`androidApp/src/main/res/drawable/ic_launcher_*.xml`).

## Colour

Both themes follow the system's light or dark setting. Neither app has its own switch.

| Role | Light | Dark | Admin token | Mobile role |
| --- | --- | --- | --- | --- |
| Brand, main action | `#2A56C6` | `#A9C2FF` | `--primary` | `primary` |
| Text on brand | `#FFFFFF` | `#0B2A6F` | `--primary-foreground` | `onPrimary` |
| Page background | `#F6F7F9` | `#0E1116` | `--background` | `background`, `surface` |
| Card | `#FFFFFF` | `#161A21` | `--card` | `surfaceContainerLow` |
| Text | `#12161D` | `#ECEEF2` | `--foreground` | `onSurface` |
| Secondary text | `#586172` | `#A0A9B8` | `--muted-foreground` | `onSurfaceVariant` |
| Hairline border | `#E1E5EA` | white 12% | `--border` | `outlineVariant` |
| Control edge | `#848E9F` | `#6B7484` | `--input` | `outline` (`#7A8496` / `#7C8696`) |
| Error | `#C62828` | `#FF8A80` | `--destructive` | `error` |
| Success | `#166534` | `#86EFAC` | `--success` | not used |

Status and Severity are the only other colours. Each is a tinted background with text that reads on it, at 4.5:1 or better, and always carries its label: colour never stands alone.

| Value | Light background / text | Dark background / text |
| --- | --- | --- |
| Open | `#DBEAFE` / `#1E40AF` | `#172554` / `#93C5FD` |
| In progress | `#FEF3C7` / `#78350F` | `#451A03` / `#FCD34D` |
| Resolved | `#DCFCE7` / `#166534` | `#052E16` / `#86EFAC` |
| Low | `#E2E8F0` / `#334155` | `#1E293B` / `#CBD5E1` |
| Medium | `#FEF9C3` / `#713F12` | `#422006` / `#FDE047` |
| High | `#FFEDD5` / `#7C2D12` | `#431407` / `#FDBA74` |
| Critical | `#FEE2E2` / `#991B1B` | `#450A0A` / `#FCA5A5` |

On the web these are Tailwind's `*-100` with `*-800/900` and `*-950` with `*-300`, in `status-badges.tsx`. On mobile they are `Status.tone(dark)` and `Severity.tone(dark)`.

## Type

Two faces. **Plus Jakarta Sans** is the display face on both platforms, used only for prominent text: page and screen titles, sheet titles, an Incident's title, card titles on the web, empty-state headings and headline numbers. Everything read at length or typed into (body, tables, labels, fields, buttons, badges) stays on the reading face: Geist on the web, the platform's default (Roboto, San Francisco) on the phone.

Plus Jakarta Sans is under the SIL Open Font License 1.1. The phone bundles two weights, Bold and ExtraBold, in `mobile/feature/incidents/src/commonMain/composeResources/font/`, with the licence in `mobile/licenses/`. The web loads the same two through `next/font`, which self-hosts them at build time. A glyph the face lacks (Burmese, Thai, CJK in an Incident's title) falls back to the reading face.

The scale is defined once per platform: `appTypography()` in `ui/Theme.kt`, and the `type-*` utilities with `--font-heading` in `admin/src/app/globals.css`. Do not set a face, weight or tracking at a call site.

| Role | Face, weight, tracking | Mobile (sp, size/line) | Admin | Used for |
| --- | --- | --- | --- | --- |
| Display L / M / S | display, ExtraBold, −0.025 to −0.02 em | 57/64, 45/52, 36/44 | `type-display` (32/40 px, tabular figures) | headline numbers (overview tiles); none on the phone yet |
| Headline L / M | display, ExtraBold, −0.015 to −0.01 em | 32/40, 28/36 | not used | "Who are you?" on the phone (`headlineMedium`) |
| Headline S | display, ExtraBold, −0.01 em | 24/32 | `type-headline` (24/32 px) | page titles; the list's top bar; an Incident's title |
| Title L | display, Bold | 22/28 | not used | other top bars, sheet titles, empty-state headings, the account name |
| Title M | mobile: reading, SemiBold. Admin: display, Bold | 16/24 | `type-title` (16/22 px) | a card's title; an Incident's title in a list; web empty-state headings and the product name |
| Title S | reading, SemiBold | 14/20 | `text-sm font-medium` | a section label |
| Body L / M / S | reading, Regular | 16/24, 14/20, 12/16 | `text-base` (fields on small screens), `text-sm`, `text-xs` | content, supporting text, hints |
| Label L | reading, SemiBold | 14/20 | `text-sm font-medium` | buttons |
| Label M / S | reading, Medium | 12/16, 11/16 | `text-xs font-medium` | badges, field labels (`text-muted-foreground`), counters |

Phone sizes are in sp and web sizes in rem, so both follow the person's font-size setting.

## Spacing and shape

| | Admin | Mobile |
| --- | --- | --- |
| Steps | `gap-2` in a control group, `gap-3` between controls, `gap-4` in a form or card, `gap-5` between page sections | 4, 8, 12, 16 dp; 32 dp around an empty state |
| Page padding | `px-4 sm:px-6 py-6`, content capped at `max-w-7xl` | 16 dp, content capped at 640 dp (`ContentWidth`) |
| Card | `rounded-xl`, hairline border, no shadow | 20 dp corners, hairline border, no elevation (`AppCard`) |
| Control | `rounded-lg`, 32 px high | 12 dp corners on text fields, fully rounded buttons and chips; the main button is 48 to 52 dp high; every touch target at least 48 dp |
| Sheet and dialog | not used | 28 dp corners |
| Badge | fully rounded | fully rounded (`Pill`) |

No gradients and no decorative shadows on either platform. Depth comes from the card sitting on a slightly darker page.

## Components

| Need | Admin | Mobile |
| --- | --- | --- |
| Status | `StatusBadge`: a tinted pill with a dot | `StatusPill`: the same |
| Severity | `SeverityBadge`: one to four bars and the word | `SeverityLevel`: the same; `IncidentPills` for Status, Severity, Category and Sync State together |
| A person | `Avatar` (initials on a tinted disc) | `Avatar` |
| Headline counts | `Overview`: four tiles that are also one-click filters | the Status chips above the list, each with its count |
| A fact with an icon | definition list in a `Card` | `InfoRow` inside an `AppCard` |
| Main and other actions | `Button`: `default` for the one main action, `outline` for the others, `secondary` for Save, `ghost` for navigation | `Button` for the main action, `OutlinedButton` for the others, `TextButton` in dialogs |
| Text and choices | `Input`, `Textarea`, `NativeSelect`, wrapped in `Field` | `OutlinedTextField`, `ChoiceChips` |
| Container | `Card` | `AppCard` |
| Nothing to show | `EmptyState` | `EmptyState` |
| Could not load | `ApiUnavailable` (with Try again) | `EmptyState` with a Try again button |
| Something was refused | `role="alert"` text beside the control | `Notice`, `FailureBanner` |
| Loading | `Skeleton` shapes that mirror the loaded layout | `CircularProgressIndicator` |
| Time | `LocalTime` | `Instant.display()` |
| Icons | `lucide-react` | `material-icons-core`, plus `AppIcons.Camera` and `AppIcons.Image` drawn in `ui/Icons.kt` |

## Patterns

- **Status and Severity look different on purpose.** Status is the one tinted pill; Severity is four bars and a word. Two same-shaped badges side by side read as the same kind of thing.
- **Counts double as filters.** The dashboard's tiles and the phone's chips each show how many Incidents are in a Status and narrow the list to it in one tap. The counts cover everything, whatever the list is filtered to.
- **One main action per view.** It is the only filled button. On the dashboard's triage panel it is the usual next step for the Status (`primaryAction`); on the phone it sits in a bottom bar so it is reachable without scrolling.
- **Destructive actions** are outlined in the error colour, never filled, and ask first (Delete) or offer Undo afterwards.
- **The report and edit form is a full-height sheet** on the phone. It slides up over the screen it was opened from and stops under the status bar; a header (close, title), the scrolling fields, and a footer with the submit button. It closes by the close button, the back gesture or a drag down, and asks first when something has been entered. On a short screen the footer scrolls with the fields.
- **Choices on the form.** Category is a two-by-two grid of tiles with icons. Severity is one four-part scale, Low to Critical, whose chosen part takes that Severity's colour: it is an order, so it is drawn as one.
- **A field's label sits above it**, always visible, with the character count beside it; the placeholder suggests what to write. The line under a field appears only for an error.
- **Form rhythm:** 16 dp between sections, 6 dp inside one.
- **Account actions** live in a bottom sheet behind the avatar on the phone; opening it is the pause a confirm dialog would give.
- **A refused action shows why.** When a submit is refused, the first problem is scrolled into view; a requirement is stated on the form before it is broken ("Photos · at least one").
- **A saved report is confirmed** with a short message on the list, which scrolls to it.
- **The keyboard resizes the screen** on the phone; it never pushes the top bar away or covers the main action.
- **Photos open full screen** on a tap, uncropped. One photo fills the width; several scroll.
- **Losing work asks first.** The phone's form asks before discarding anything entered; an untouched form just closes.
- **Every data view has four states:** loading, empty (saying why), loaded, and failed (saying what happened and what to do).
- **Time** is shown in the viewer's zone as `10 Nov 2026, 14:45` on both platforms.
- **Motion** is short and functional: a 220 ms fade with a short slide in the direction of travel between phone screens, list items that slide when one is removed, colour transitions on hover. Skeletons stop pulsing under `prefers-reduced-motion`.
- **Copy** uses the glossary terms, sentence case, and a verb on every button.

## Accessibility

- Text contrast is at least 4.5:1 in both themes, including every badge, and the edge of an input or dropdown is at least 3:1 against what it sits on. These are measured, not judged by eye: the lowest text pair is 5.2:1.
- On the phone every Material colour role is set in `ui/Theme.kt`. A role left out falls back to Material's purple baseline.
- Meaning carried by colour is also carried by text or an icon.
- Web: every control has a visible label, focus is always visible, the first Tab stop is "Skip to content", a table row is one link, errors are tied to their field with `aria-describedby`.
- Mobile: icons that act have a `contentDescription`, decorative ones have none, headings are marked for screen readers, chips announce themselves as a single-choice group, errors that appear away from the tapped button are announced.
