# Task lifecycle

One task = one feature slice for one lane. Each step ends on a completion criterion; do not start the next step before it holds.

Lane status in the handoff: the implementer sets `REVIEW` when its own steps (3, 4, 6 and 7) are complete. After a review with zero blockers, the main session sets `DONE`. Reviewers change no files.

## 1. Plan
Owner: `contract-architect`, through `/plan-feature`. For a task in one lane that changes no request, response, error or domain term, the main session may write the handoff itself. Implementers never plan: they start from a handoff that already exists.
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
**Done when** zero blockers remain. The implementer fixes blockers; the reviewer re-checks them. The main session then records the outcome in the handoff and sets the lane to `DONE`.

## 6. Document
Owner: the lane implementer.
Update the lane README for changed commands or setup, `GLOSSARY.md` for new terms, and an ADR only when the choice is hard to reverse, surprising and a real trade-off.
**Done when** the lane README matches the code.

## 7. Handoff
Owner: the lane implementer.
Set the lane status to `REVIEW` in the handoff file, list what the next lane can now rely on, and name any open question.
**Done when** the lane's reviewer, and later `integration-verifier`, can start from the handoff file alone.
