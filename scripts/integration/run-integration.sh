#!/usr/bin/env bash
# Runs the integration scenarios across the real parts: the app's sync engine, the API, and the
# dashboard's code. It starts a throwaway API on its own port and data, runs three phases, and cleans up:
#   1. dashboard: what "Start work" and "Resolve" send (shared fixtures), accepted by the API
#   2. app: conflicts on edit and delete, and the journeys (report, Operator resolution, offline create);
#      it writes what it created to a handoff folder
#   3. dashboard: sees exactly what the phone created, through its own list, filters and detail code
set -u
cd "$(dirname "$0")/../.."

PORT="${INTEGRATION_PORT:-3100}"
WORK="$(mktemp -d)"
API_PID=""
cleanup() { [ -n "$API_PID" ] && { kill "$API_PID" 2>/dev/null; wait "$API_PID" 2>/dev/null; }; rm -rf "$WORK"; }
trap cleanup EXIT

cp backend/seed.json "$WORK/db.json"
PORT="$PORT" DB_FILE="$WORK/db.json" node backend/src/server.js > "$WORK/api.log" 2>&1 &
API_PID=$!

for _ in $(seq 1 30); do
  curl -sf "http://localhost:$PORT/users" > /dev/null && break
  sleep 0.5
done
if ! curl -sf "http://localhost:$PORT/users" > /dev/null; then
  echo "The API did not start:"; cat "$WORK/api.log"; exit 1
fi

export FIR_API_URL="http://localhost:$PORT"
export FIR_START_WORK_PATCH="$(cat scripts/integration/fixtures/start-work-patch.json)"
export FIR_RESOLVE_PATCH="$(cat scripts/integration/fixtures/resolve-patch.json)"
export FIR_STATE_DIR="$WORK/handoff"
mkdir -p "$FIR_STATE_DIR"
status=0

echo "== 1. Dashboard: Start work and Resolve against the real API =="
(cd admin && npx vitest run --config vitest.integration.config.mts src/integration/start-work.itest.ts src/integration/resolve.itest.ts --silent=false) || status=1

echo "== 2. App: conflicts and journeys against the real API =="
# --rerun: the API and environment are not Gradle inputs, so an unchanged build would otherwise skip the tests.
(cd mobile && ./gradlew :core:data:testAndroidHostTest --tests '*IntegrationTest' --rerun --console=plain -q) || status=1

echo "== 3. Dashboard: sees what the phone created =="
(cd admin && npx vitest run --config vitest.integration.config.mts src/integration/phone-reports.itest.ts) || status=1

if [ "$status" -eq 0 ]; then echo "Integration check passed."; else echo "Integration check FAILED."; fi
exit "$status"
