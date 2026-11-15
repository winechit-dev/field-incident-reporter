# Mobile workflow

How the agents, skills and models fit together for a task that touches `mobile/`. The steps themselves are in `lifecycle.md`; the gates in `quality-gates.md`; the lane rules in `mobile/AGENTS.md`. This file adds who runs each step, on which model, with which skill.

## Agents

| Agent | Model | Why this model | Preloaded skills | Loads on demand |
| --- | --- | --- | --- | --- |
| `contract-architect` | `opus` | Few, short runs whose mistakes are paid for by three lanes: acceptance criteria, contract shape, compatibility. | none | none |
| `android-implementer` | `sonnet` | Most of the tokens: test-first code, long Gradle loops. Fast and cheap enough to iterate; the skills carry the project knowledge. | `mobile-architecture`, `mobile-testing` | `mobile-data-sync`, `mobile-compose-ui`, `mobile-debugging`, `mobile-review` |
| `android-reviewer` | `opus` | Reads much, writes little, so the stronger model costs little, and a different model from the author catches what the author's blind spots hide. | `mobile-review`, `mobile-architecture` | `mobile-data-sync`, `mobile-compose-ui` |
| `integration-verifier` | `sonnet` | Runs scripts and an emulator and compares results with criteria; needs reliable tool use, not deep design. | none | `run`, `mobile-debugging` |

`haiku` is not assigned: every one of these roles either writes Kotlin that must compile on two platforms or passes a verdict others rely on. The backend and admin agents are outside this lane and keep inheriting the session model.

**Escalation.** The implementer runs on `opus` for one task when the handoff changes `SyncEngine`, the outbox format or the Room schema, or when a previous run handed back under "Open questions" (`mobile-debugging`, "When to stop"). `/implement` passes the override; the agent file stays on `sonnet`.

## Inputs, outputs, handoffs

| Agent | Starts from | Produces | Hands to |
| --- | --- | --- | --- |
| `contract-architect` | a feature idea, or "Contract needs" in a handoff | the handoff file with Given/When/Then criteria per lane; the `openapi.yaml` delta; glossary and ADR changes | `backend-implementer`, then `android-implementer` and `admin-implementer` in parallel |
| `android-implementer` | the handoff file and the merged contract | failing-then-passing tests, code inside `mobile/`, gate output under "Evidence", an emulator description for UI changes, lane status `REVIEW` | `android-reviewer`; contract gaps go back to `contract-architect` through "Contract needs" |
| `android-reviewer` | the handoff, the diff, its own gate run | a verdict per `mobile-review` section and per gate; findings as `blocker` or `nit` with `file:line` and the failing scenario | `android-implementer` while blockers remain; otherwise the lane is set `DONE` |
| `integration-verifier` | handoffs reading `DONE` in every lane | one verdict per acceptance criterion from `scripts/integration/run-integration.sh` and a device run | the failing lane's implementer, or the feature is done |

Agents talk through the handoff file. No agent edits another's lane, and no agent reviews its own work.

## Stages

| Stage (`lifecycle.md`) | Owner | Skill | Passes when |
| --- | --- | --- | --- |
| 1 Plan, 2 Contract | `contract-architect` | none | the spec parses; every criterion names its lane; a new enum value has a mobile task |
| 3 Implement | `android-implementer` | `mobile-testing` first, then `mobile-data-sync` or `mobile-compose-ui`, with `mobile-architecture` for placement | each criterion has a test seen red, then green |
| 4 Verify | `android-implementer` | `mobile-testing`; `mobile-debugging` if a gate stays red after one fix | every Mobile gate is green and pasted into the handoff |
| 5 Review | `android-reviewer` | `mobile-review` | all seven sections and every gate have a verdict; zero blockers |
| 6 Document, 7 Handoff | `android-implementer` | none | `mobile/README.md` matches the code; lane status `DONE` |
| Integration | `integration-verifier` | `run` | every criterion passes across lanes |

## Parallel mobile work

Two implementers may run at once only on different modules, in separate worktrees, with the interface between them written in the handoff first (`parallel-work.md`). The usual split is `:core:data` (with `mobile-data-sync`) and `:feature:incidents` (with `mobile-compose-ui`), meeting at a `:core:domain` interface. The version catalog and the Room schema have one writer at a time.
