import { readFileSync } from "node:fs";
import { beforeAll, describe, expect, it } from "vitest";
import { buildPatch } from "@/lib/triage";

/**
 * The dashboard's "Start work" against a real API.
 *
 * `start-work-patch.json` is shared with the app's integration test: it is what an Operator's
 * "Start work" sends, and what the app's conflict test uses to take an incident out from under a
 * Technician. If the dashboard ever sends something else, this test fails until the fixture is
 * updated, which keeps the two sides describing the same action.
 */
const apiUrl = process.env.FIR_API_URL;
const fixture = JSON.parse(
  readFileSync(new URL("../../../scripts/integration/fixtures/start-work-patch.json", import.meta.url), "utf8"),
);

const TECHNICIAN = "u-tech-1";
const OPERATOR = "u-op-1";
const OPERATORS = ["u-op-1", "u-op-2"];

async function call(method: string, path: string, user: string, body?: unknown) {
  const response = await fetch(`${apiUrl}${path}`, {
    method,
    headers: { "X-User-Id": user, ...(body ? { "Content-Type": "application/json" } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await response.text();
  return { status: response.status, json: text ? JSON.parse(text) : null };
}

async function technicianReports(title: string) {
  const id = crypto.randomUUID();
  const created = await call("POST", "/incidents", TECHNICIAN, {
    id, title, description: "", category: "EQUIPMENT", severity: "HIGH",
  });
  expect(created.status).toBe(201);
  return id;
}

describe.skipIf(!apiUrl)("dashboard 'Start work' against the real API", () => {
  let api: typeof import("@/api/incidents");

  beforeAll(async () => {
    process.env.API_BASE_URL = apiUrl; // read once, when the client module loads
    api = await import("@/api/incidents");
  });

  it("sends the shared patch, which the API accepts and records", async () => {
    const id = await technicianReports("Hydraulic lift");
    const incident = await api.getIncident(OPERATOR, id);

    const result = buildPatch(incident, { intent: "start" }, OPERATOR, OPERATORS);
    expect(result).toEqual({ ok: true, patch: fixture });

    const updated = await api.updateIncident(OPERATOR, id, fixture);
    expect(updated.status).toBe("IN_PROGRESS");
    expect(updated.assignee).toBe(OPERATOR);
    expect(updated.activityLog.map((entry) => entry.status)).toEqual(["IN_PROGRESS"]);
  });

  it("leaves a Technician's later edit and delete refused with NOT_OPEN, and the incident untouched", async () => {
    const id = await technicianReports("Cracked floor");
    await api.updateIncident(OPERATOR, id, fixture);

    const edit = await call("PATCH", `/incidents/${id}`, TECHNICIAN, { title: "Edited too late" });
    expect(edit.status).toBe(409);
    expect(edit.json.error.code).toBe("NOT_OPEN");

    const remove = await call("DELETE", `/incidents/${id}`, TECHNICIAN);
    expect(remove.status).toBe(409);
    expect(remove.json.error.code).toBe("NOT_OPEN");

    const after = await api.getIncident(OPERATOR, id);
    expect(after.title).toBe("Cracked floor");
    expect(after.status).toBe("IN_PROGRESS");
  });
});
