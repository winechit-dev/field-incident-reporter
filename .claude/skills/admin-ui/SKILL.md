---
name: admin-ui
description: Visual and interaction rules for the admin dashboard. Use when adding or changing a component, a table, a form's look, a badge, layout or spacing, dark mode, responsive behaviour, UI copy or accessibility in admin/.
---

# Admin UI

Tailwind 4 and shadcn/ui primitives in `src/components/ui/`. One look across the dashboard comes from reusing what exists.

## Building blocks

- Reach for a primitive in `src/components/ui/` first (`Button`, `Badge`, `Card`, `Input`, `NativeSelect`, `Select`, `Skeleton`, `Table`, `Textarea`). Add a missing one with the shadcn CLI so it matches `components.json`.
- A control in a form that submits (the filter bar, the triage form) is a `NativeSelect` inside a `Field`; the popup `Select` cannot travel in a plain form submission.
- Nothing to show is an `EmptyState`; an unreachable API is `ApiUnavailable`; a loading view is a skeleton from `src/components/skeletons.tsx` shaped like the loaded page.
- A person is an `Avatar` beside their name. A headline count is a tile in `Overview`, built from `overviewTiles` in `src/lib/overview.ts`.
- Status and Severity always render through `StatusBadge` (a tinted pill with a dot) and `SeverityBadge` (four bars and the word). Their colours and the labels in `src/lib/domain.ts` are the only place an enum becomes something visible; a new enum value is added to both, and the type check enforces it.
- Timestamps render through `LocalTime`: UTC from the server, the viewer's zone after hydration, with the ISO value in `title`.
- Combine classes with `cn(...)`.

## Tokens

- Colour comes from the theme tokens in `globals.css`: `text-muted-foreground`, `text-destructive`, `text-success`, `border-input`, `bg-background`, `bg-card`. A literal palette colour is allowed only in the badge maps, and always with its `dark:` pair.
- The tokens are shared with the mobile app. `docs/design/design-system.md` lists them; a value changed here is changed in `mobile/.../ui/Theme.kt` or `ui/Labels.kt` in the same task.
- Dark mode follows the system (`prefers-color-scheme`); there is no `.dark` class and no switch. `dark:` utilities work as usual.
- Spacing follows the existing rhythm: `gap-2` inside a control group, `gap-3` between controls, `gap-4` inside a form or card, `gap-5` between page sections, `px-4 sm:px-6 py-6` page padding, content capped at `max-w-7xl`.
- A view has one filled (`default`) button: its main action. The rest are `outline`, `secondary` or `ghost`.
- Type: `text-2xl font-semibold` for the page title, `text-sm` for body and tables, `text-xs font-medium text-muted-foreground` for field labels.

## States

Every view that loads data renders four states: the `<Suspense>` fallback while loading, an empty message that says why it is empty ("No incidents match these filters."), the data, and a failure that says what happened and what to do. A form adds pending (buttons disabled, label changes to "Saving…") and success.

## Accessibility

- Every input has a visible `<label>` that wraps it or points to it.
- A failed field sets `aria-invalid`; the message is in an element with `role="alert"`. A success message uses `role="status"`.
- Interactive things are `<button>` or `<a>`; a table row that navigates contains a real link.
- Focus is always visible (`focus-visible:ring-*`); the tab order follows the reading order.
- Meaning carried by colour is also carried by text, as the badges do.
- Text contrast holds in both themes; check dark mode for anything using a literal colour.

## Responsive

Operators work on a desktop, but the dashboard stays usable at tablet width. Controls wrap with `flex flex-wrap`; a table drops its least important columns below `lg` rather than scrolling sideways; the detail page stacks its columns below `lg`.

## Copy

Glossary terms, sentence case, and a verb on every button ("Start work", "Resolve", "Save changes"). An error says what was not done and what to do next. The dashboard shows only what an Operator can act on.

## Proving it

If a change to `globals.css` does not show in a running `next dev`, delete `.next/dev` and restart it; the CSS cache can go stale.

The changed page is opened in a browser against the running backend, and each state, keyboard-only use of the changed control, dark mode and a narrow window are checked and described in the handoff. Subagents have no browser tools. Say under "Evidence" that the browser check is still owed; the session that ran `/implement` does it with the `claude-in-chrome` skill and records what it saw.

## Done when

The change reuses the existing primitives and tokens, all states render, and the browser check covered keyboard, dark mode and a narrow window, or is recorded as owed.
