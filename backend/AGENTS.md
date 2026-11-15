# Backend lane

Mock API: json-server serves reads; `src/app.js` enforces every rule. Commands are in `package.json` and `README.md`.

## Roles
- `backend-implementer` owns `backend/` except `openapi.yaml`. It turns contract deltas into behaviour and tests.
- `backend-reviewer` reads the diff against the contract and the shared rules, read-only.
- `contract-architect` owns `openapi.yaml`.

Models, inputs, outputs and handoffs: `docs/agents/agent-matrix.md`.

## Skills
| Working on | Skill |
| --- | --- |
| A route, a rule, an error code, seed data, an API test | `backend-api` (preloaded into both agents) |
| Reviewing a diff, or self-checking before `REVIEW` | `backend-review` |

## Boundaries
- Edit only `backend/`. Ask for contract changes in the handoff.
- Keep each rule in `src/app.js` next to the endpoint it governs.
- `seed.json` is the demo data: one Incident per Status, two Technicians, two Operators. Extend it when a feature needs a new case, and keep every row valid under the contract.

## Workflow
1. Read the handoff and the contract delta.
2. Write the failing test in `test/api.test.js`: one per status code of the changed operation.
3. Implement until green; return errors as `{ error: { code, message } }` using the codes the contract lists.
4. Update `openapi.yaml` only through a handoff request if the behaviour revealed a gap.
5. Run the gates in `docs/agents/quality-gates.md`.

## Practices
- Tests start a real server on an ephemeral port with a temp copy of `seed.json`; they never touch `db.json`.
- Server-owned fields (`createdAt`, `updatedAt`, `reportedBy`, `activityLog`, `resolvedAt`) are never read from client input.
- Validate everything at the edge; unknown fields give `422`.
- A new file-handling path validates ids against the id pattern before touching the disk.

## Done when
`npm test` is green with a test per status code, the spec and behaviour agree, and the handoff lane status reads `DONE`.
