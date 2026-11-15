#!/usr/bin/env bash
# Runs the two conflict scenarios (edit and delete) across the real parts: the app's sync engine,
# the API, and the dashboard's "Start work". It starts a throwaway API on its own port and data,
# runs the dashboard's integration test and then the app's, and cleans up.
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
status=0

echo "== Dashboard: Start work against the real API =="
(cd admin && npm run --silent test:integration) || status=1

echo "== App: sync conflicts against the real API =="
# --rerun: the API and environment are not Gradle inputs, so an unchanged build would otherwise skip the test.
(cd mobile && ./gradlew :core:data:testAndroidHostTest --tests '*ConflictIntegrationTest' --rerun --console=plain -q) || status=1

if [ "$status" -eq 0 ]; then echo "Integration check passed."; else echo "Integration check FAILED."; fi
exit "$status"
