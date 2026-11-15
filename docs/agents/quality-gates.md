# Quality gates

A gate is **green** or **red**; there is no amber. Paste the final output of each gate into the handoff under "Evidence".

## All lanes
- `scripts/check-forbidden-terms.sh` exits 0.
- Each handoff acceptance criterion maps to a named, passing test.
- The lane README still matches the commands and setup.

## Backend (`backend/`)
- `npm test` is green.
- Each changed operation has a test per documented status code.
- `openapi.yaml` parses and matches the behaviour: same fields, same codes.
- `seed.json` still loads and covers every Status.
- A smoke run: `npm run reset && npm start`, then `curl` the changed endpoints.

## Mobile (`mobile/`)
- `./gradlew :core:domain:allTests :core:data:testAndroidHostTest :core:data:iosSimulatorArm64Test` is green.
- `./gradlew :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64` is green.
- Sync or domain logic changes come with a test in `commonTest` that failed first.
- DTOs match the contract field by field.
- A UI change is run on the emulator, using the `run` skill, and the result is described in the handoff.

## Admin (`admin/`)
- `npm run typecheck`, `npm run lint`, `npm test`, `npm run build` are green.
- `npm run gen:api` leaves no diff against the committed `schema.d.ts`.
- A UI change is checked in a browser against the running backend, and the result is described in the handoff.

## Integration (`integration-verifier`)
- Backend running; one acceptance scenario per criterion passes end to end across lanes.
- Mobile conflict scenario: edit offline, Operator changes Status, sync shows the server version and a failure notice.
- The admin dashboard shows what the mobile app reported.
