# UI/UX audit

An audit of the three lanes, done by reading every screen's code and running each app: the dashboard in Chrome at 1440, 768 and 420 px in both colour schemes, and the phone app on a Pixel 9 emulator in light, dark, portrait and landscape. Each finding is a usability problem that was observed, with what was done about it. The work is tracked in `docs/handoffs/007-ui-ux-polish.md`; the shared tokens and components are in `design-system.md`; before and after screenshots are in `docs/screenshots/ui-ux/`.

A second pass, on the visual design rather than usability, followed this audit. It is tracked in `docs/handoffs/008-visual-redesign.md`, with its own before and after screenshots in `docs/screenshots/redesign/`.

## Across the product

| # | Problem | Fix |
| --- | --- | --- |
| X1 | No shared identity. The dashboard was stock grey shadcn; the phone was stock Material purple. Nothing said they were one product. | One brand blue, one set of neutrals, one set of Status and Severity hues, in both. |
| X2 | Neither app had a working dark theme. The dashboard's dark tokens hung off a `.dark` class nothing ever set. The phone ignored the system setting, so in dark mode its status-bar icons turned white on a white screen. | Both follow the system setting. |
| X3 | The same moment was shown differently: `10 Nov 2026, 15:15` on the dashboard, `2026-11-10 08:15 UTC` on the phone, although the standards say clients show local time. | The phone shows local time in the dashboard's format. |
| X4 | Badge colours differed between platforms, and the phone's Medium (yellow on pale yellow) was close to unreadable. | The phone uses the dashboard's tinted pairs, with a dark-theme pair for each. |

## Admin dashboard

| # | Problem | Fix |
| --- | --- | --- |
| A1 | Every page showed a "1 Issue" error in development: data was fetched during prerendering. | The data-loading components wait for the request (`connection()`). |
| A2 | The filter bar was nine equal controls in one wrapping row. Search was no wider than a dropdown, sorting sat among the filters, and the second date was labelled "to". | A card in two rows: what narrows the list on top with a wide search, the date range, sorting and buttons below. Both dates are labelled. |
| A3 | Choosing a filter took two steps: pick, then Apply. | A dropdown applies as soon as it changes. Apply stays for search, dates and no-JavaScript use. |
| A4 | "Clear" did not say what it would clear. | "Clear 2 filters". |
| A5 | Only the title text of a row was clickable. | The whole row is the link's target; it is still one link for keyboard and screen readers. |
| A6 | At tablet width the table scrolled sideways. | Category and Reported by drop out below `lg`; they remain on the detail page. |
| A7 | Loading was a line of text, and the page jumped when data arrived. | Skeletons in the shape of the loaded page. |
| A8 | One empty message for three different situations. | "No incidents yet", "No incidents match these filters" (with Clear filters), and "Nothing on this page" (with a link to page one). |
| A9 | With the API down, a red sentence said "reload". | A panel with what happened, what to do, and Try again. |
| A10 | Pagination showed "Page 1 of 1" and two disabled buttons under a four-row table. | The count always shows; the page controls only when there is more than one page. |
| A11 | The triage panel had three equally weighted buttons, and nothing said what each would do. "Start work" silently assigned the Incident to the Operator. | The usual next step for the Status is the one filled button, and a line under the buttons says what it does. Save changes sits apart. |
| A12 | The resolution note's only instruction was a placeholder, which vanishes on typing. A failed field was red but not tied to its message. | A persistent hint under the field; the error is linked with `aria-describedby`. |
| A13 | On the detail page the triage panel scrolled away while reading a long Incident. | It stays in view on wide screens. |
| A14 | Activity was a stack of paragraphs. Details were `div`s. Section titles were not headings. | A timeline list, a definition list, and real headings. |
| A15 | Every photo's alt text was "Incident photo". | "Photo 2 of 3: <title>", and links that open a new tab say so. |
| A16 | On sign-in the target was a small Continue button inside a wide card. No message when there were no Operators. | The whole card is the button. An empty state. |
| A17 | No skip link, one browser-tab title for every page, no way home from the header, no fallback for an unexpected error. | Skip to content, a title per page, the product name links to the list, an `error.tsx` with Try again. |
| A18 | The native select's class string was copied into two files. | `NativeSelect` and `Field` components. |

Considered and left alone:

- **A sidebar.** The dashboard has one section. A sidebar with one item is decoration; the header link is enough until a second section exists.
- **Charts.** With no aggregate endpoint in the contract, a chart would be built from one page of 20 rows and would mislead.
- **Triage first on narrow screens.** Moving the panel above the description would put the action before the information needed to take it.
- **The filter URL.** A submitted form writes empty parameters (`?q=&status=OPEN&…`). Links the page builds itself are clean; tidying the form's own URL needs client code for little gain.

## Mobile app

| # | Problem | Fix |
| --- | --- | --- |
| M1 | Rotating the phone on the detail or form screen dropped the Technician back on the list. A half-written report vanished from view, and the next form opened could show the old one's text. | The back stack is saved and restored. This was the most serious finding. |
| M2 | Back, or Cancel, on a half-written report discarded it without asking. | It asks first. An untouched form just closes. |
| M3 | Submit report was at the end of a long scroll and cut off by the navigation bar. Edit and Delete were likewise below the content. | Both live in a bottom bar that is always visible. |
| M4 | Navigation was text: "Back", "Cancel", "Switch". The floating button had no icon. | A back arrow, a close cross, and icons with spoken labels. |
| M5 | The list's title was the Technician's name, not what the screen is. | "Incidents", with "Reported by <name>" beneath. |
| M6 | "Switch" signed out on a single tap, next to nothing explaining it. | It asks first and says unsynced changes stay on the device. |
| M7 | Before the first load the list was blank, then showed a bare sentence when empty. Pull-to-refresh did not work on the empty screen. | A progress indicator, a proper empty state, and pull-to-refresh works when empty. |
| M8 | Category and Severity chips scrolled sideways with no hint; the last option could be off-screen. The four badges on the detail screen could clip. | Both wrap. The selected chip also has a tick. |
| M9 | The 2000-character limit appeared only as an error after submitting. | A live counter. |
| M10 | Removing a photo was a red "✕" drawn straight onto the photo, invisible on a red or dark image, with no spoken label. | A cross on a dark disc, a 48 dp target, "Remove photo 1 of 3". |
| M11 | "An operator is already handling this incident" was the last line of the detail screen. | It sits under the title, where the missing buttons would be looked for. |
| M12 | A missing Incident was a dead end with one sentence. The user picker had no empty state. | A message with a way back; an empty state with Try again. |
| M13 | On a tablet or in landscape, cards and fields stretched the full width. | Content is capped at 640 dp and centred. |
| M14 | Cards used `clickable` on a rectangle, so the ripple ignored the rounded corners; screens changed with a hard cut. | Material's clickable card; a short cross-fade between screens. |
| M15 | Keyboards opened in lower case with no Next key. | Sentence capitalisation and Next/Done actions. |

Considered and left alone:

- **A navigation library.** Three screens; the hand-rolled stack is now saveable, which was the real gap.
- **Moving copy to string resources.** Already recorded as its own task in the mobile skill.
- **The launch window.** The Android window theme is still light, so a dark-mode launch shows one light frame before Compose draws. Fixing it needs a `values-night` theme resource; it is cosmetic and separate.

## Backend API

The API is small and already consistent: one error shape, `UPPER_SNAKE` codes, idempotent writes, a status code per rule. Nothing was changed in the contract or the behaviour.

| # | Finding | Action |
| --- | --- | --- |
| B1 | The README said every request needs `X-User-Id`. `GET /users` and `/uploads/*` do not, by design. It also never mentioned the error shape. | README corrected. |
| B2 | Three errors the server can return are not in the contract: `400 BAD_JSON`, `405 METHOD_NOT_ALLOWED`, and multer's own codes for a bad upload (`LIMIT_FILE_SIZE`, `LIMIT_UNEXPECTED_FILE`). | Not changed. Recorded as an open question for `contract-architect` in the handoff: listing them is additive. |
| B3 | An unknown path such as `GET /nope` answers `405 METHOD_NOT_ALLOWED` rather than `404`. | Not changed, for the same reason: it is a behaviour change with tests behind it. |
| B4 | `Error.code` is a free string in the contract, so the generated clients cannot check a `code` they branch on. | Not changed. An enum of codes would be additive and is suggested in the same open question. |
