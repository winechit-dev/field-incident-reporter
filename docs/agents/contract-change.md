# Contract change

`backend/openapi.yaml` is the contract. The mobile DTOs (`mobile/core/data/.../remote/Dtos.kt`) and the admin types (`admin/src/api/schema.d.ts`, generated) follow it, never the reverse.

## Who
Only `contract-architect` edits `openapi.yaml`. Lane agents request changes in the handoff file under "Contract needs".

## Steps
1. Read the handoff's "Contract needs" and the current spec.
2. Edit `openapi.yaml`: operations, schemas, every status code, and the error `code` values.
3. Keep it additive: new optional fields, new endpoints, new enum values clients can ignore. A breaking change needs an ADR and a new path version (`/v2/...`); the old path stays until every lane has migrated.
4. Parse the file (`ruby -ryaml -e 'YAML.load_file("backend/openapi.yaml")'`) and lint it when the linter exists.
5. Add or update the glossary term if the change introduces a domain word.
6. Fill the handoff's "Contract delta": operation, change, reason.

## Done when
The spec parses, each changed operation lists all its status codes with a schema, the handoff has the delta, and a `backend-implementer` task exists for the behaviour.

## How lanes consume it
- Backend implements the delta and proves each status code in `backend/test/`.
- Admin regenerates types (`npm run gen:api` in `admin/`) and commits the result.
- Mobile updates DTOs and mappers by hand, so the reviewer checks field-by-field against the spec. Unknown JSON fields are ignored, so a new field never breaks an old app build. A new enum value does: the mappers use `valueOf`, so the handoff for one must include a mobile task that maps it.

## Handoff file
Copy `docs/handoffs/_template.md`. Agents communicate through this file and the repo, not through chat.
