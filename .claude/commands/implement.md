---
description: Run one lane's implementer on a handoff file
argument-hint: <backend|mobile|admin> <handoff file>
---

Run the matching implementer (`backend` → `backend-implementer`, `mobile` → `android-implementer`, `admin` → `admin-implementer`) on the handoff in $ARGUMENTS. Use a git worktree when other lanes are in flight. For a mobile handoff that changes `SyncEngine`, the outbox format or the Room schema, or one the implementer handed back under "Open questions", pass `model: opus` on the Agent call; otherwise leave the agent's own model. Report the gate results and the lane status it set.
