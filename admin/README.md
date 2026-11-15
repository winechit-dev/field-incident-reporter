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
| `npm test` | Vitest (filter logic) |
| `npm run gen:api` | regenerate `src/api/schema.d.ts` from `../backend/openapi.yaml`; commit the result |

## Layout

- `src/api/`: the typed client. `schema.d.ts` is generated, never hand-edited.
- `src/lib/`: pure logic (`filters.ts` turns the URL into API queries and back) and the contract's enums with labels.
- `src/app/`: routes. Anything per-request (cookies, `searchParams`, API calls) sits inside `<Suspense>`, as Cache Components requires.
- `src/proxy.ts`: sends anyone without a chosen Operator to the sign-in page.
- `src/components/ui/`: shadcn/ui primitives.

## Behaviour

- Filters (search, Status, Severity, Category, assignee, reported date range, sort, page) live in the URL as a plain GET form. Invalid values in the URL are ignored.
- Dates are whole days in UTC. Times display in the browser's local zone.
- The API's `X-Total-Count` header drives paging (20 per page).

## Triage

The detail page (`/incidents/[id]`) shows everything about an Incident and has a triage panel: Start work, Resolve, Reopen, or edit Status, assignee and Resolution Note and save. `src/lib/triage.ts` turns a submission into the smallest valid change, so the Activity Log only grows when the Status really changes. Resolving needs a Resolution Note (an admin-side rule; the contract does not require it).

## Not built yet

Filtering for unassigned Incidents (the contract cannot express it), and bulk actions.
