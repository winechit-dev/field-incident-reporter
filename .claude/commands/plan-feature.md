---
description: Turn a feature idea into a handoff file and contract delta
argument-hint: <feature description>
---

Use the `contract-architect` agent to plan this feature: $ARGUMENTS

It creates `docs/handoffs/<id>-<slug>.md`, applies any contract change to `backend/openapi.yaml`, and reports which lanes have work. Then list the lane tasks in merge order: backend first, mobile and admin in parallel.
