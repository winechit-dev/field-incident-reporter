---
description: Run one lane's implementer on a handoff file
argument-hint: <backend|mobile|admin> <handoff file>
---

Run the matching implementer on the handoff in $ARGUMENTS: `backend` → `backend-implementer`, `mobile` → `android-implementer`, `admin` → `admin-implementer`.

- **Isolation.** Use a git worktree (`isolation: worktree`) when another lane is in flight.
- **Model.** Leave the agent's own model, except: for a mobile handoff that changes `SyncEngine`, the outbox format or the Room schema, pass `model: opus`; for a backend handoff that changes who may do what or file handling, pass `model: sonnet`; for any handoff the implementer handed back under "Open questions", pass the next model up.
- **Browser check.** After an admin UI change, open the changed page yourself with the `claude-in-chrome` skill against the running backend and add what you saw under "Evidence"; the implementer has no browser.

Report the gate results and the lane status it set.
