# Task lifecycle

One task = one feature slice for one lane. Each step ends on a completion criterion; do not start the next step before it holds.

## 1. Plan
Owner: `contract-architect` (cross-lane) or the lane implementer (single-lane).
Write the task into `docs/handoffs/<id>-<slug>.md` from `_template.md`: goal, affected lanes, acceptance criteria as Given/When/Then.
**Done when** every acceptance criterion names the observable result and the lane that proves it.

## 2. Contract
Owner: `contract-architect`. Skip when no request, response or error changes.
Follow `contract-change.md`.
**Done when** `openapi.yaml` parses, the change is additive (or an ADR records the break), and the handoff lists the new operations.

## 3. Implement, red first
Owner: the lane implementer.
Write one failing test per acceptance criterion (red), then the smallest change that turns it green. Keep each commit to one criterion.
**Done when** every criterion has a test that failed before the change and passes after it.

## 4. Verify
Owner: the lane implementer.
Run every gate for the lane from `quality-gates.md`.
**Done when** every gate is green, and the output is pasted into the handoff under "Evidence".

## 5. Review
Owner: the lane reviewer, in a fresh context with read-only tools.
Review the diff against the handoff criteria, `standards.md` and the lane `AGENTS.md`. Findings are `blocker` or `nit`.
**Done when** zero blockers remain. The implementer fixes blockers; the reviewer re-checks them.

## 6. Document
Owner: the lane implementer.
Update the lane README for changed commands or setup, `GLOSSARY.md` for new terms, and an ADR only when the choice is hard to reverse, surprising and a real trade-off.
**Done when** the lane README matches the code and the handoff status for this lane reads `DONE`.

## 7. Handoff
Owner: the lane implementer.
Set the lane status in the handoff file, list what the next lane can now rely on, and name any open question.
**Done when** `integration-verifier` can start from the handoff file alone.
