# Quality gates

A gate is **green** or **red**; there is no amber. Paste the final output of each gate into the handoff under "Evidence".

## All lanes
- `scripts/check-forbidden-terms.sh` exits 0.
- Each handoff acceptance criterion maps to a named, passing test.
- The lane README still matches the commands and setup.

## Workflow files (`.claude/`, `AGENTS.md`, `docs/agents/`)
- `scripts/check-agents.sh` exits 0 after any change to an agent, a skill, a command or a doc that names one.

## Backend (`backend/`)
- `npm test` is green.
- Each changed operation has a test per documented status code.
- `openapi.yaml` parses and matches the behaviour: same fields, same codes.
- `seed.json` still loads and covers every Status.
- A smoke run: `npm run reset && npm start`, then `curl` the changed endpoints.

## Mobile (`mobile/`)
- `./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test` is green.
- `./gradlew :feature:incidents:testAndroidHostTest :feature:incidents:iosSimulatorArm64Test :shared:iosSimulatorArm64Test` is green.
- `./gradlew :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64` is green.
- `./gradlew :androidApp:lintDebug` reports 0 errors and no warning the change introduced (baseline: 7, about dependency versions, an obsolete SDK check and the launcher icon).
- Sync or domain logic changes come with a test in `commonTest` that failed first.
- DTOs match the contract field by field.
- A UI change is run on the emulator, using the `run` skill, and the result is described in the handoff.

## Admin (`admin/`)
- `npm run typecheck`, `npm run lint`, `npm test`, `npm run build` are green.
- `npm run gen:api` leaves no diff against the committed `schema.d.ts`.
- A UI change is checked in a browser against the running backend, and the result is described in the handoff. The main session runs this check; the lane agents have no browser.

## Integration (`integration-verifier`)
- Backend running; one acceptance scenario per criterion passes end to end across lanes.
- `scripts/integration/run-integration.sh` passes: the app's sync engine, the API and the dashboard's code agree on the five cross-app scenarios (report to triage, an Operator's change reaching the phone, conflicts on edit and on delete, an offline create). It starts its own API, so nothing else needs to be running. Run it after any change to sync logic, the DTOs, the contract, the filters, or what "Start work" or "Resolve" sends.
- Mobile conflict scenario on a device: edit offline, Operator changes Status, sync shows the server version and a failure notice. (The script covers the logic; what the screen shows is still checked by hand.)
- The admin dashboard shows what the mobile app reported.
