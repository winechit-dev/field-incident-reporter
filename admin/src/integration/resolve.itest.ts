import { readFileSync } from "node:fs";
import { beforeAll, describe, expect, it } from "vitest";
import { buildPatch } from "@/lib/triage";

/**
 * The dashboard's "Resolve" against a real API. `resolve-patch.json` is shared with the app's
 * integration test, which uses it to resolve an incident out from under a Technician and checks
 * the phone picks the change up. See start-work.itest.ts for the same idea.
 */
const apiUrl = process.env.FIR_API_URL;
const fixture = JSON.parse(
  readFileSync(new URL("../../../scripts/integration/fixtures/resolve-patch.json", import.meta.url), "utf8"),
);

const TECHNICIAN = "u-tech-1";
const OPERATOR = "u-op-1";
const OPERATORS = ["u-op-1", "u-op-2"];

describe.skipIf(!apiUrl)("dashboard 'Resolve' against the real API", () => {
  let api: typeof import("@/api/incidents");

  beforeAll(async () => {
    process.env.API_BASE_URL = apiUrl;
    api = await import("@/api/incidents");
  });

  it("sends the shared patch, which the API accepts and records", async () => {
    const id = crypto.randomUUID();
    const created = await fetch(`${apiUrl}/incidents`, {
      method: "POST",
      headers: { "X-User-Id": TECHNICIAN, "Content-Type": "application/json" },
      body: JSON.stringify({ id, title: "Pothole", description: "", category: "ROAD_HAZARD", severity: "MEDIUM" }),
    });
    expect(created.status).toBe(201);

    const incident = await api.getIncident(OPERATOR, id);
    const result = buildPatch(incident, { intent: "resolve", note: fixture.resolutionNote }, OPERATOR, OPERATORS);
    expect(result).toEqual({ ok: true, patch: fixture });

    const updated = await api.updateIncident(OPERATOR, id, fixture);
    expect(updated.status).toBe("RESOLVED");
    expect(updated.resolutionNote).toBe(fixture.resolutionNote);
    expect(updated.resolvedAt).not.toBeNull();
    expect(updated.activityLog.map((entry) => entry.status)).toEqual(["RESOLVED"]);
  });
});
