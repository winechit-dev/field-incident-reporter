# Field Incident Reporter

Technicians report physical incidents from a phone; Operators triage them on a web dashboard. Three lanes share one API contract.

| Lane | Path | Stack | Owner agents |
| --- | --- | --- | --- |
| Backend | `backend/` | json-server + Express | `backend-implementer`, `backend-reviewer` |
| Mobile | `mobile/` | Kotlin Multiplatform, Compose Multiplatform, Room, Ktor | `android-implementer`, `android-reviewer` |
| Admin | `admin/` | Next.js (App Router), TypeScript | `admin-implementer`, `admin-reviewer` |
| Contract | `backend/openapi.yaml`, `GLOSSARY.md`, `docs/adr/` | OpenAPI 3 | `contract-architect` |

Read your lane's `AGENTS.md` before touching code: `backend/AGENTS.md`, `mobile/AGENTS.md`, `admin/AGENTS.md`. Each lists the skills in `.claude/skills/` for its work. Every agent's model, inputs, outputs and handoffs: `docs/agents/agent-matrix.md`.

## Contract-first

`backend/openapi.yaml` is the single source of truth for every request, response and error. Change the contract before any lane's code, and only `contract-architect` edits it. Steps and rules: `docs/agents/contract-change.md`.

## Lanes

Stay in your lane's directory. A need in another lane becomes a handoff file in `docs/handoffs/`, never an edit. Parallel work, worktrees and merge order: `docs/agents/parallel-work.md`.

## Task lifecycle

Every task runs plan → contract → implement (red first) → verify → review → document → handoff. Steps and completion criteria: `docs/agents/lifecycle.md`. Commands: `/plan-feature`, `/implement`, `/review`, `/verify-integration`.

## Gates

A task is done when its lane's gates are green and its handoff acceptance criteria each map to a passing test. Gates per lane: `docs/agents/quality-gates.md`.

## Language and standards

`GLOSSARY.md` terms are canonical: Incident, Technician, Operator, Status, Severity, Sync State. Use them in code, tests, UI copy and docs. Shared conventions: `docs/agents/standards.md`. Reasons behind big choices: `docs/adr/`.

## Submission rule

The public repo must contain no reference to the assessing company. Run `scripts/check-forbidden-terms.sh` before every handoff; it must exit 0.
