---
description: Review one lane's work in a fresh context
argument-hint: <backend|mobile|admin> <handoff file>
---

Run the matching reviewer on the handoff in $ARGUMENTS: `backend` → `backend-reviewer`, `mobile` → `android-reviewer`, `admin` → `admin-reviewer`.

Tell it which diff to review: the lane's branch against `main` (`git diff main...<branch> -- <lane dir>`), or the working tree when the work is uncommitted.

Show its verdict per section, then its blockers and nits. If blockers exist, send them to the lane implementer with `/implement`, then review again. When none remain, have the implementer set the lane status to `DONE`; the reviewer changes no files.
