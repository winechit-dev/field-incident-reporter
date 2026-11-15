# Admin dashboard

Next.js (App Router) dashboard where Operators triage Incidents reported from the field. Lane rules and workflow: `AGENTS.md`.

```
npm install
cp .env.example .env.local   # optional: API_BASE_URL defaults to http://localhost:3000
npm run dev                  # http://localhost:3001 (the API uses 3000)
```

Start the mock API first (`backend/`, `npm start`), then the dashboard. Pick an Operator on the sign-in page; there is no password.

| Script | Purpose |
| --- | --- |
| `npm run dev`, `build`, `start` | run the app |
| `npm run typecheck` | `next typegen`, then `tsc` |
| `npm run lint` | ESLint |
| `npm test` | Vitest (filter and triage logic) |
| `npm run gen:api` | regenerate `src/api/schema.d.ts` from `../backend/openapi.yaml`; commit the result |

## Layout

- `src/api/`: the typed client. `schema.d.ts` is generated, never hand-edited.
- `src/lib/`: pure logic (`filters.ts` turns the URL into API queries and back) and the contract's enums with labels.
- `src/app/`: routes. Anything per-request (cookies, `searchParams`, API calls) sits inside `<Suspense>`, as Cache Components requires.
- `src/proxy.ts`: sends anyone without a chosen Operator to the sign-in page.
- `src/components/ui/`: shadcn/ui primitives, plus `NativeSelect` and `Skeleton`.
- `src/components/`: the app's own pieces. `Field` labels a control; `states.tsx` holds the empty and API-unavailable panels; `skeletons.tsx` the loading shapes.
- `src/app/globals.css`: the design tokens, shared with the mobile app (`../docs/design/design-system.md`). Dark mode follows the system setting.

## Behaviour

- Filters (search, Status, Severity, Category, assignee or Unassigned, reported date range, sort, page) live in the URL as a plain GET form. Changing a dropdown applies it at once; search and dates wait for Enter or Apply, which also works without JavaScript. Invalid values in the URL are ignored. Unassigned is `assigned=false` in the URL (the dropdown submits a reserved value that is read as the same thing, and links the page builds use `assigned=false`); it excludes a specific assignee.
- Dates are whole days in UTC. Times display in the browser's local zone.
- The API's `X-Total-Count` header drives paging (20 per page). The page controls appear only when there is more than one page.
- Every data view has a loading skeleton, an empty state that says why it is empty, and an API-unavailable panel with Try again. An unexpected failure lands in `src/app/error.tsx`.

## Triage

The detail page (`/incidents/[id]`) shows everything about an Incident and has a triage panel: Start work, Resolve, Reopen, or edit Status, assignee and Resolution Note and save. `src/lib/triage.ts` turns a submission into the smallest valid change, so the Activity Log only grows when the Status really changes. Resolving needs a Resolution Note (an admin-side rule; the contract does not require it). The usual next step for the Status (`primaryAction`) is the one filled button, and a line under the buttons says what it will do.

## Not built yet

Bulk actions.
