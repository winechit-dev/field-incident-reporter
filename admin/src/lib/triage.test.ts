import { describe, expect, it } from "vitest";
import { actionsFor, buildPatch, MAX_NOTE_LENGTH, type Current } from "./triage";

const me = "u-op-1";
const operators = ["u-op-1", "u-op-2"];
const open: Current = { status: "OPEN", assignee: null, resolutionNote: null };
const inProgress: Current = { status: "IN_PROGRESS", assignee: "u-op-2", resolutionNote: null };
const resolved: Current = { status: "RESOLVED", assignee: "u-op-2", resolutionNote: "Replaced the valve." };

const run = (current: Current, input: Parameters<typeof buildPatch>[1]) => buildPatch(current, input, me, operators);

describe("start work", () => {
  it("moves to In progress and assigns the Operator when nobody is assigned", () => {
    expect(run(open, { intent: "start" })).toEqual({ ok: true, patch: { status: "IN_PROGRESS", assignee: me } });
  });

  it("keeps an existing assignee", () => {
    expect(run({ ...open, assignee: "u-op-2" }, { intent: "start" })).toEqual({
      ok: true, patch: { status: "IN_PROGRESS" },
    });
  });
});

describe("resolve", () => {
  it("asks for a Resolution Note", () => {
    const result = run(inProgress, { intent: "resolve", note: "   " });
    expect(result).toMatchObject({ ok: false, field: "note" });
    expect(result.ok === false && result.error).toMatch(/resolution note/i);
  });

  it("sends Resolved with the trimmed note", () => {
    expect(run(inProgress, { intent: "resolve", note: "  Replaced the valve. " })).toEqual({
      ok: true, patch: { status: "RESOLVED", resolutionNote: "Replaced the valve." },
    });
  });

  it("accepts a note already on the Incident", () => {
    expect(run({ ...inProgress, resolutionNote: "Done earlier" }, { intent: "resolve" })).toEqual({
      ok: true, patch: { status: "RESOLVED" },
    });
  });
});

describe("reopen", () => {
  it("moves a Resolved Incident back to In progress", () => {
    expect(run(resolved, { intent: "reopen" })).toEqual({ ok: true, patch: { status: "IN_PROGRESS" } });
  });
});

describe("save", () => {
  it("sends only the assignee when only that changed", () => {
    expect(run(inProgress, { intent: "save", status: "IN_PROGRESS", assignee: me, note: "" })).toEqual({
      ok: true, patch: { assignee: me },
    });
  });

  it("clears the assignee for Unassigned", () => {
    expect(run(inProgress, { intent: "save", status: "IN_PROGRESS", assignee: "", note: "" })).toEqual({
      ok: true, patch: { assignee: null },
    });
  });

  it("changes Status and note together", () => {
    expect(run(open, { intent: "save", status: "IN_PROGRESS", assignee: "", note: "On site" })).toEqual({
      ok: true, patch: { status: "IN_PROGRESS", resolutionNote: "On site" },
    });
  });

  it("clears the note when it is emptied", () => {
    expect(run(resolved, { intent: "save", status: "RESOLVED", assignee: "u-op-2", note: "" })).toEqual({
      ok: true, patch: { resolutionNote: null },
    });
  });

  it("says so when nothing changed", () => {
    const result = run(inProgress, { intent: "save", status: "IN_PROGRESS", assignee: "u-op-2", note: "" });
    expect(result).toMatchObject({ ok: false, error: "No changes to save." });
  });

  it("refuses an assignee who is not an Operator", () => {
    expect(run(open, { intent: "save", status: "OPEN", assignee: "u-tech-1", note: "" })).toMatchObject({
      ok: false, field: "assignee",
    });
  });

  it("refuses an unknown Status", () => {
    expect(run(open, { intent: "save", status: "DONE", assignee: "", note: "" })).toMatchObject({
      ok: false, field: "status",
    });
  });

  it("refuses a note over the limit", () => {
    const note = "x".repeat(MAX_NOTE_LENGTH + 1);
    expect(run(open, { intent: "save", status: "OPEN", assignee: "", note })).toMatchObject({ ok: false, field: "note" });
  });

  it("refuses an unknown intent", () => {
    expect(run(open, { intent: "delete" as never })).toMatchObject({ ok: false });
  });
});

describe("actionsFor", () => {
  it("offers the next sensible step for each Status", () => {
    expect(actionsFor("OPEN")).toEqual(["start", "resolve"]);
    expect(actionsFor("IN_PROGRESS")).toEqual(["resolve"]);
    expect(actionsFor("RESOLVED")).toEqual(["reopen"]);
  });
});
