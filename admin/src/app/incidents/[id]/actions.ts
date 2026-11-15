"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { ApiError, ApiUnavailableError } from "@/api/client";
import { getIncident, listUsers, updateIncident } from "@/api/incidents";
import { getOperator } from "@/lib/session";
import { buildPatch, type Intent } from "@/lib/triage";

/** What the form needs back. On an error, `values` repopulate the fields so nothing typed is lost. */
export type TriageState =
  | { status: "idle" }
  | { status: "success" }
  | {
      status: "error";
      message: string;
      field?: "status" | "assignee" | "note";
      values: { status?: string; assignee?: string; note?: string };
    };

const text = (value: FormDataEntryValue | null) => (typeof value === "string" ? value : undefined);

export async function triage(_previous: TriageState, formData: FormData): Promise<TriageState> {
  const operator = await getOperator();
  if (!operator) redirect("/sign-in");

  const id = text(formData.get("incidentId")) ?? "";
  const input = {
    intent: (text(formData.get("intent")) ?? "save") as Intent,
    status: text(formData.get("status")),
    assignee: text(formData.get("assignee")),
    note: text(formData.get("note")),
  };
  const values = { status: input.status, assignee: input.assignee, note: input.note };

  // Failures become values here, so redirects and refresh() happen outside the try block.
  const outcome = await (async (): Promise<TriageState | "unknown-user"> => {
    try {
      const [incident, users] = await Promise.all([getIncident(operator.id, id), listUsers()]);
      const operatorIds = users.filter((u) => u.role === "OPERATOR").map((u) => u.id);
      const result = buildPatch(incident, input, operator.id, operatorIds);
      if (!result.ok) return { status: "error", message: result.error, field: result.field, values };
      await updateIncident(operator.id, id, result.patch);
      return { status: "success" };
    } catch (error) {
      if (error instanceof ApiUnavailableError) return { status: "error", message: `${error.message}. Nothing was saved.`, values };
      if (error instanceof ApiError) {
        if (error.code === "UNKNOWN_USER") return "unknown-user";
        return { status: "error", message: error.message, values };
      }
      throw error;
    }
  })();

  if (outcome === "unknown-user") redirect("/sign-in");
  if (outcome.status === "success") refresh();
  return outcome;
}
