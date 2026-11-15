import { STATUSES, type Incident, type Status } from "./domain";

export const MAX_NOTE_LENGTH = 2000;

/** What an Operator can do with one click, besides saving the form as it stands. */
export type Intent = "save" | "start" | "resolve" | "reopen";
const INTENTS: readonly Intent[] = ["save", "start", "resolve", "reopen"];

/** The Incident fields an Operator may change. A missing key means "leave it alone". */
export interface OperatorPatch {
  status?: Status;
  assignee?: string | null;
  resolutionNote?: string | null;
}

export type Current = Pick<Incident, "status" | "assignee" | "resolutionNote">;

/** Raw form values. Everything is optional because a quick action sends only its intent. */
export interface TriageInput {
  intent: Intent;
  status?: string;
  /** An Operator's id, or "" for Unassigned. */
  assignee?: string;
  note?: string;
}

export type PatchResult =
  | { ok: true; patch: OperatorPatch }
  | { ok: false; error: string; field?: "status" | "assignee" | "note" };

const fail = (error: string, field?: "status" | "assignee" | "note"): PatchResult => ({ ok: false, error, field });

/** The quick actions worth offering for an Incident in this Status. */
export function actionsFor(status: Status): Exclude<Intent, "save">[] {
  switch (status) {
    case "OPEN":
      return ["start", "resolve"];
    case "IN_PROGRESS":
      return ["resolve"];
    case "RESOLVED":
      return ["reopen"];
  }
}

/**
 * Turns a form submission into the smallest valid change: only fields that differ from the
 * Incident as it stands are included, so the Activity Log and timestamps change only when
 * something really did.
 */
export function buildPatch(current: Current, input: TriageInput, me: string, operatorIds: readonly string[]): PatchResult {
  if (!INTENTS.includes(input.intent)) return fail("Unknown action.");

  const currentNote = current.resolutionNote ?? null;
  let status: Status = current.status;
  let assignee: string | null = current.assignee;
  let note: string | null = currentNote;

  if (input.note !== undefined) {
    const trimmed = input.note.trim();
    if (trimmed.length > MAX_NOTE_LENGTH) return fail(`The note can be at most ${MAX_NOTE_LENGTH} characters.`, "note");
    note = trimmed || null;
  }

  switch (input.intent) {
    case "start":
      status = "IN_PROGRESS";
      assignee = current.assignee ?? me;
      break;
    case "resolve":
      if (!note) return fail("Add a resolution note before resolving.", "note");
      status = "RESOLVED";
      break;
    case "reopen":
      status = "IN_PROGRESS";
      break;
    case "save": {
      if (input.status !== undefined) {
        const chosen = STATUSES.find((s) => s === input.status);
        if (!chosen) return fail("Choose a valid status.", "status");
        status = chosen;
      }
      if (input.assignee !== undefined) {
        if (input.assignee === "") assignee = null;
        else if (operatorIds.includes(input.assignee)) assignee = input.assignee;
        else return fail("Only an operator can be assigned.", "assignee");
      }
      break;
    }
  }

  const patch: OperatorPatch = {};
  if (status !== current.status) patch.status = status;
  if (assignee !== current.assignee) patch.assignee = assignee;
  if (note !== currentNote) patch.resolutionNote = note;
  return Object.keys(patch).length ? { ok: true, patch } : fail("No changes to save.");
}
