# Agent matrix

Who runs each step, on which model, with which skill, across the three lanes. The steps are in `lifecycle.md`, the gates in `quality-gates.md`, the lane rules in each lane's `AGENTS.md`. The agent files in `.claude/agents/` are the source of truth for `model` and `skills`; this file records why.

## Agents

| Agent | Writes | Model | Why this model | Preloaded skills | Loads on demand |
| --- | --- | --- | --- | --- | --- |
| `contract-architect` | `backend/openapi.yaml`, `GLOSSARY.md`, `docs/adr/`, handoff files | `opus` | Few, short runs whose mistakes are paid for by three lanes. | none | none |
| `backend-implementer` | `backend/` except the contract | `haiku` | The most routine lane: one rules file, one test file, a test per status code that catches a wrong answer at once. | `backend-api` | `backend-review` |
| `backend-reviewer` | nothing | `sonnet` | Small diffs, but the server is where authorization holds; one tier above the author. | `backend-review`, `backend-api` | none |
| `android-implementer` | `mobile/` | `sonnet` | Most of the tokens: test-first Kotlin for two platforms, long Gradle loops. | `mobile-architecture`, `mobile-testing` | `mobile-data-sync`, `mobile-compose-ui`, `mobile-debugging`, `mobile-review` |
| `android-reviewer` | nothing | `opus` | Offline sync fails in ways only careful reading finds; reads much, writes little. | `mobile-review`, `mobile-architecture` | `mobile-data-sync`, `mobile-compose-ui` |
| `admin-implementer` | `admin/` | `sonnet` | Strict TypeScript and a Next.js version whose docs must be read, not recalled. | `admin-architecture` | `admin-ui`, `admin-review` |
| `admin-reviewer` | nothing | `opus` | Server/client boundaries, caching and access checks are easy to get subtly wrong; reads much, writes little. | `admin-review`, `admin-architecture` | `admin-ui` |
| `integration-verifier` | nothing | `sonnet` | Runs scripts and an emulator and compares results with criteria. | none | `run`, `mobile-debugging` |

Every reviewer runs one tier above its lane's implementer, so the check is independent of the author's blind spots. No architect, tester, UI or security agent exists per lane: `contract-architect` already designs across lanes, tests are written by the implementer red-first, and UI and security are sections of each review skill. A separate agent for any of them would re-read the same diff.

**Escalation.** `/implement` passes a stronger model for one task, leaving the agent file unchanged:

| Lane | Raise to | When the handoff |
| --- | --- | --- |
| backend | `sonnet` | changes who may do what, changes file handling, or was handed back under "Open questions" |
| mobile | `opus` | changes `SyncEngine`, the outbox format or the Room schema, or was handed back under "Open questions" |

## Inputs, outputs, handoffs

| Agent | Starts from | Produces | Hands to |
| --- | --- | --- | --- |
| `contract-architect` | a feature idea, or "Contract needs" in a handoff | the handoff with Given/When/Then criteria per lane; the contract delta; glossary and ADR changes | `backend-implementer`; then mobile and admin in parallel |
| `<lane>-implementer` | the handoff and the merged contract | tests seen red then green, code inside its lane, gate output under "Evidence", a device or browser description for UI changes, lane status `REVIEW` | its lane's reviewer; contract gaps go to `contract-architect` through "Contract needs" |
| `<lane>-reviewer` | the handoff, the diff, its own gate run | a verdict per review-skill section and per gate; findings as `blocker` or `nit` with `file:line` and the failing scenario | the implementer, who fixes blockers and, once none remain, sets the lane `DONE` (reviewers change no files) |
| `integration-verifier` | handoffs reading `DONE` in every lane | one verdict per acceptance criterion | the failing lane's implementer, or the feature is done |

Agents talk through the handoff file. No agent edits another's lane, and no agent reviews its own work. Subagents cannot start other subagents: the main session runs each step through the commands and carries nothing between agents except the handoff file.

## Stages

| Stage (`lifecycle.md`) | Owner | Backend skill | Mobile skill | Admin skill | Passes when |
| --- | --- | --- | --- | --- | --- |
| 1 Plan, 2 Contract | `contract-architect` | | | | the spec parses; every criterion names its lane; error codes are named; a new enum value has a mobile task |
| 3 Implement | implementer | `backend-api` | `mobile-testing`, then `mobile-data-sync` or `mobile-compose-ui` | `admin-architecture`, `admin-ui` | each criterion has a test seen red, then green |
| 4 Verify | implementer | `backend-api` | `mobile-testing`; `mobile-debugging` if a gate stays red | `admin-architecture` | every lane gate is green and pasted into the handoff |
| 5 Review | reviewer | `backend-review` | `mobile-review` | `admin-review` | every section and gate has a verdict; zero blockers |
| 6 Document, 7 Handoff | implementer | | | | the lane README matches the code; lane status `DONE` |
| Integration | `integration-verifier` | | | | `scripts/integration/run-integration.sh` passes and every criterion has a verdict |

## Across lanes

The shared rules already live in one place each; this is the map.

| Concern | Where it is decided | How each lane follows |
| --- | --- | --- |
| Request, response and error shapes | `backend/openapi.yaml`, changed only by `contract-architect` (`contract-change.md`) | backend proves each status code in tests; admin regenerates `schema.d.ts` and the compiler reports the rest; mobile updates DTOs by hand and the reviewer checks field by field |
| Names | `GLOSSARY.md` | same terms in code, tests and UI copy |
| Error handling | `standards.md`: clients branch on `error.code` | mobile maps status to `RemoteException` in one place; admin maps to `ApiError` in `unwrap` |
| Enum values | the contract | backend constant lists; admin `domain.ts` (type check fails if one is missing); mobile mapper (needs an explicit task) |
| What an Operator's action sends | `scripts/integration/fixtures/` | admin's integration tests assert it; mobile's conflict tests replay it |
| Order of work | `parallel-work.md`: contract → backend → (mobile ∥ admin) → integration | one writer per directory; parallel agents in separate worktrees |
| A need in another lane | the handoff file, under "Contract needs" or "Open questions" | never an edit outside the lane |

## Parallel work inside mobile

Two mobile implementers may run at once only on different modules, in separate worktrees, with the interface between them written in the handoff first. The usual split is `:core:data` (with `mobile-data-sync`) and `:feature:incidents` (with `mobile-compose-ui`), meeting at a `:core:domain` interface. The version catalog and the Room schema have one writer at a time.
