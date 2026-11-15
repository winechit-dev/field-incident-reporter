# Design system

One product, two platforms. The dashboard and the phone app share an identity (the brand blue, the neutrals, the Status and Severity hues, the date format and the wording) and each keeps its platform's conventions: shadcn/ui and Tailwind on the web, Material 3 on the phone.

Where the values live:

| Platform | Tokens | Components |
| --- | --- | --- |
| Admin | `admin/src/app/globals.css` | `admin/src/components/ui/` (primitives), `admin/src/components/` (app components) |
| Mobile | `mobile/feature/incidents/.../ui/Theme.kt` (scheme), `ui/Labels.kt` (Status and Severity tones) | `mobile/feature/incidents/.../ui/Components.kt`, `ui/PhotoStrip.kt` |

A value changed on one platform is changed on the other in the same task.

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

| Use | Admin | Mobile |
| --- | --- | --- |
| Typeface | Geist | the platform's default (Roboto, San Francisco) |
| Page or screen title | `text-2xl font-semibold` | top app bar title; `headlineSmall` for an Incident's title |
| Card or section title | `text-base font-medium` | `titleMedium` |
| Body | `text-sm` | `bodyLarge` for content, `bodyMedium` for supporting text |
| Field and section label | `text-xs font-medium text-muted-foreground` | `labelLarge` in `onSurfaceVariant` (`SectionLabel`) |
| Badge | `text-xs font-medium` | `labelMedium` |

## Spacing and shape

| | Admin | Mobile |
| --- | --- | --- |
| Steps | `gap-2` in a control group, `gap-3` between controls, `gap-4` in a form or card, `gap-5` between page sections | 4, 8, 12, 16 dp; 32 dp around an empty state |
| Page padding | `px-4 sm:px-6 py-6`, content capped at `max-w-7xl` | 16 dp, content capped at 640 dp (`ContentWidth`) |
| Card | `rounded-xl`, hairline border, no shadow | Material's medium shape, hairline border, no elevation (`AppCard`) |
| Control | `rounded-lg`, 32 px high | Material defaults; every touch target at least 48 dp |
| Badge | fully rounded | fully rounded (`Pill`) |

No gradients and no decorative shadows on either platform. Depth comes from the card sitting on a slightly darker page.

## Components

| Need | Admin | Mobile |
| --- | --- | --- |
| Status, Severity | `StatusBadge`, `SeverityBadge` | `StatusPill`, `SeverityPill`; `IncidentPills` for the set |
| Main and other actions | `Button`: `default` for the one main action, `outline` for the others, `secondary` for Save, `ghost` for navigation | `Button` for the main action, `OutlinedButton` for the others, `TextButton` in dialogs |
| Text and choices | `Input`, `Textarea`, `NativeSelect`, wrapped in `Field` | `OutlinedTextField`, `ChoiceChips` |
| Container | `Card` | `AppCard` |
| Nothing to show | `EmptyState` | `EmptyState` |
| Could not load | `ApiUnavailable` (with Try again) | `EmptyState` with a Try again button |
| Something was refused | `role="alert"` text beside the control | `Notice`, `FailureBanner` |
| Loading | `Skeleton` shapes that mirror the loaded layout | `CircularProgressIndicator` |
| Time | `LocalTime` | `Instant.display()` |
| Icons | `lucide-react` | `material-icons-core` |

## Patterns

- **One main action per view.** It is the only filled button. On the dashboard's triage panel it is the usual next step for the Status (`primaryAction`); on the phone it sits in a bottom bar so it is reachable without scrolling.
- **Destructive actions** are outlined in the error colour, never filled, and ask first (Delete) or offer Undo afterwards.
- **Losing work asks first.** The phone's form asks before discarding anything entered; an untouched form just closes.
- **Every data view has four states:** loading, empty (saying why), loaded, and failed (saying what happened and what to do).
- **Time** is shown in the viewer's zone as `10 Nov 2026, 14:45` on both platforms.
- **Motion** is short and functional: a 150 ms cross-fade between phone screens, list items that slide when one is removed, colour transitions on hover. Skeletons stop pulsing under `prefers-reduced-motion`.
- **Copy** uses the glossary terms, sentence case, and a verb on every button.

## Accessibility

- Text contrast is at least 4.5:1 in both themes, including every badge.
- Meaning carried by colour is also carried by text or an icon.
- Web: every control has a visible label, focus is always visible, the first Tab stop is "Skip to content", a table row is one link, errors are tied to their field with `aria-describedby`.
- Mobile: icons that act have a `contentDescription`, decorative ones have none, headings are marked for screen readers, chips announce themselves as a single-choice group, errors that appear away from the tapped button are announced.
