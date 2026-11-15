# Admin lane

Next.js dashboard where Operators triage, filter, assign and resolve Incidents. Layout and behaviour: `README.md`.

## Roles
- `admin-implementer` owns `admin/`.
- `admin-reviewer` reads the diff against the contract and the handoff, read-only.

Models, inputs, outputs and handoffs: `docs/agents/agent-matrix.md`.

## Skills
| Working on | Skill |
| --- | --- |
| A route, a server action, an API call, a filter, a form, a rule, a test | `admin-architecture` (preloaded into both agents) |
| A component, layout, copy, dark mode, accessibility | `admin-ui` |
| Reviewing a diff, or self-checking before `REVIEW` | `admin-review` |

## Stack
Next.js App Router, TypeScript (`strict`), Tailwind, shadcn/ui, Vitest. The API client's types are generated from `../backend/openapi.yaml` into `src/api/schema.d.ts` with `openapi-typescript`; that file is generated, never hand-edited.

## Workflow
1. Read the handoff; run `npm run gen:api` so types match the contract.
2. Write the failing test: pure logic (filter-to-query building, allowed Status actions, activity-log formatting) in Vitest.
3. Implement. Keep pure logic in plain functions under `src/lib/`; components stay thin.
4. Run the admin gates in `docs/agents/quality-gates.md`.
5. For any UI change, open the page in a browser against the running backend (`claude-in-chrome` skill) and describe what you saw.

## Practices
- One typed API module (`src/api/`) wraps every call and adds `X-User-Id` from the Operator picker.
- Branch on the error `code` from the contract.
- Filters (Status, Severity, Category, assignee, date range, search) live in the URL so a view can be shared.
- Operators change Status, assignee and Resolution Note only; the UI offers nothing a Technician-owned field would need.
- Show times in the browser's local zone; the API sends UTC.
- The backend allows CORS for any origin during development; the dashboard talks to `http://localhost:3000`.

## Done when
Gates are green, `npm run gen:api` leaves no diff, and the dashboard shows an Incident a Technician reported.

<!-- BEGIN:nextjs-agent-rules -->

## This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->
