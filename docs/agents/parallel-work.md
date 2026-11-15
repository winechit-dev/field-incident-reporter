# Parallel work

## Lanes never overlap
Each directory has one writer at a time. Two agents never edit the same file in the same task.

## Merge order
contract → backend → (mobile ∥ admin) → integration verification.
Mobile and admin start as soon as the contract change is merged: they build against `openapi.yaml` and the seed data, and need a running backend only for integration verification.

## Worktrees
Run each parallel agent in its own git worktree (`isolation: worktree`), on a branch named `<id>-<lane>` such as `014-mobile`. Merge a lane when its gates are green and its review has zero blockers.

## Shared files
| File | Rule |
| --- | --- |
| `backend/openapi.yaml` | `contract-architect` only |
| `GLOSSARY.md`, `docs/adr/` | `contract-architect`, on a lane's request |
| `docs/handoffs/<id>-*.md` | each agent edits only its own lane's section |
| `mobile/gradle/libs.versions.toml` | one mobile agent at a time |
| Room schema (`mobile/core/data/schemas/`) | one mobile agent at a time; a migration is its own task |
| `backend/seed.json` | `backend-implementer`; mobile and admin ask via handoff |

## Mobile modules
Split mobile work by module: `:core:domain`, `:core:data`, `:feature:incidents`, `:androidApp`, `iosApp`. Parallel mobile agents take different modules and agree the interface between them in the handoff first.

## Conflicts
If a merge conflicts, the later lane rebases onto the earlier one. Never resolve a conflict by editing another lane's files; raise it in the handoff.
