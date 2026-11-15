import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { beforeAll, describe, expect, it } from "vitest";
import { parseFilters } from "@/lib/filters";

/**
 * What the dashboard sees of what the phone did. The app's integration test (run first, by
 * scripts/integration/run-integration.sh) reports incidents, resolves one and syncs one that was
 * created offline, and writes what it made to FIR_STATE_DIR. This checks the dashboard's own
 * filters, list and detail code over the same real API return exactly that, photo bytes included.
 */
const apiUrl = process.env.FIR_API_URL;
const stateDir = process.env.FIR_STATE_DIR;
const OPERATOR = "u-op-1";

/** A missing file means the app phase failed or did not run, which must fail here rather than skip. */
function handoff(name: string) {
  return JSON.parse(readFileSync(join(stateDir!, `${name}.json`), "utf8"));
}

const sha256 = (bytes: ArrayBuffer) => createHash("sha256").update(Buffer.from(bytes)).digest("hex");

describe.skipIf(!apiUrl || !stateDir)("the dashboard sees what the phone did", () => {
  let api: typeof import("@/api/incidents");

  beforeAll(async () => {
    process.env.API_BASE_URL = apiUrl;
    api = await import("@/api/incidents");
  });

  async function listed(query: Record<string, string>) {
    return (await api.listIncidents(OPERATOR, parseFilters(query))).incidents;
  }

  it("1. shows a reported incident, with its location and the photo's exact bytes", async () => {
    const s = handoff("scenario1");

    const rows = await listed({ q: s.title });
    const row = rows.find((i) => i.id === s.id);
    expect(row, "the incident is in the dashboard's list").toBeDefined();
    expect(row).toMatchObject({
      title: s.title, status: "OPEN", severity: s.severity, category: s.category, reportedBy: "u-tech-1",
      location: { latitude: s.latitude, longitude: s.longitude, address: s.address },
    });

    // The dashboard's own filters find it too.
    const filtered = await listed({ category: s.category, severity: s.severity, status: "OPEN", q: s.title });
    expect(filtered.map((i) => i.id)).toContain(s.id);

    const detail = await api.getIncident(OPERATOR, s.id);
    expect(detail.photos).toHaveLength(1);
    const download = await fetch(detail.photos[0].url);
    expect(download.status).toBe(200);
    expect(download.headers.get("content-type")).toMatch(/image\/jpeg/);
    expect(sha256(await download.arrayBuffer())).toBe(s.photoSha256);
  });

  it("2. shows the resolution an Operator made, as the phone received it", async () => {
    const s = handoff("scenario2");

    const detail = await api.getIncident(OPERATOR, s.id);
    expect(detail.status).toBe("RESOLVED");
    expect(detail.resolutionNote).toBe(s.note);
    expect(detail.resolvedAt).not.toBeNull();
    const last = detail.activityLog.at(-1);
    expect(last).toMatchObject({ status: "RESOLVED", actorId: OPERATOR });

    const resolved = await listed({ status: "RESOLVED", q: s.title });
    expect(resolved.map((i) => i.id)).toContain(s.id);
  });

  it("5. lists an incident that was created offline exactly once, with its photo", async () => {
    const s = handoff("scenario5");

    const rows = await listed({ q: s.title });
    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({ id: s.id, title: s.title, status: "OPEN", reportedBy: "u-tech-1" });
    expect(rows[0].photos).toHaveLength(1);

    const download = await fetch(rows[0].photos[0].url);
    expect(sha256(await download.arrayBuffer())).toBe(s.photoSha256);
  });
});
