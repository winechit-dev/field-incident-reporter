"use client";

import { useActionState } from "react";
import { CircleCheck } from "lucide-react";
import { triage, type TriageState } from "@/app/incidents/[id]/actions";
import { Field } from "@/components/field";
import { Button } from "@/components/ui/button";
import { NativeSelect } from "@/components/ui/native-select";
import { Textarea } from "@/components/ui/textarea";
import { STATUSES, STATUS_LABEL, type Incident, type Status, type User } from "@/lib/domain";
import { actionsFor, MAX_NOTE_LENGTH, primaryAction } from "@/lib/triage";

const QUICK_LABEL = { start: "Start work", resolve: "Resolve", reopen: "Reopen" } as const;
const ERROR_ID = "triage-error";
const NOTE_HINT_ID = "triage-note-hint";

/** What the quick actions will do, said before the click rather than discovered after it. */
function quickHint(incident: Incident): string | null {
  switch (incident.status) {
    case "OPEN":
      return incident.assignee
        ? "Start work moves this incident to In progress."
        : "Start work moves this incident to In progress and assigns it to you.";
    case "IN_PROGRESS":
      return "Resolve needs a resolution note.";
    case "RESOLVED":
      return "Reopen moves this incident back to In progress.";
  }
}

/**
 * Triage controls for one Incident. Quick actions and Save submit the same form, told apart by
 * `intent`. The usual next step is the one filled button; Save changes applies the fields as
 * they stand. The fields are remounted whenever the Incident changes on the server, so they
 * always show what is stored.
 */
export function TriageForm({ incident, operators }: { incident: Incident; operators: User[] }) {
  const [state, action, pending] = useActionState<TriageState, FormData>(triage, { status: "idle" });
  const failed = state.status === "error" ? state : null;
  const primary = primaryAction(incident.status);
  const hint = quickHint(incident);

  return (
    <form action={action} className="flex flex-col gap-4" aria-busy={pending}>
      <input type="hidden" name="incidentId" value={incident.id} />
      <Fields key={incident.updatedAt} incident={incident} operators={operators} failed={failed} />

      {failed && <p id={ERROR_ID} role="alert" className="text-sm text-destructive">{failed.message}</p>}
      {state.status === "success" && !pending && (
        <p role="status" className="flex items-center gap-1.5 text-sm text-success">
          <CircleCheck aria-hidden="true" className="size-4" /> Saved.
        </p>
      )}

      <div className="flex flex-col gap-2 border-t pt-4">
        <div className="flex flex-wrap gap-2">
          {actionsFor(incident.status).map((intent) => (
            <Button key={intent} type="submit" name="intent" value={intent} variant={intent === primary ? "default" : "outline"} disabled={pending}>
              {QUICK_LABEL[intent]}
            </Button>
          ))}
          <Button type="submit" name="intent" value="save" variant="secondary" disabled={pending} className="ml-auto">
            {pending ? "Saving…" : "Save changes"}
          </Button>
        </div>
        {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
      </div>
    </form>
  );
}

function Fields({
  incident,
  operators,
  failed,
}: {
  incident: Incident;
  operators: User[];
  failed: Extract<TriageState, { status: "error" }> | null;
}) {
  const v = failed?.values;
  const describedBy = (field: "status" | "assignee" | "note") => (failed?.field === field ? ERROR_ID : undefined);
  return (
    <>
      <Field label="Assignee">
        <NativeSelect name="assignee" defaultValue={v?.assignee ?? incident.assignee ?? ""} aria-invalid={failed?.field === "assignee"} aria-describedby={describedBy("assignee")}>
          <option value="">Unassigned</option>
          {operators.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
        </NativeSelect>
      </Field>
      <Field label="Status">
        <NativeSelect name="status" defaultValue={v?.status ?? incident.status} aria-invalid={failed?.field === "status"} aria-describedby={describedBy("status")}>
          {STATUSES.map((s: Status) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </NativeSelect>
      </Field>
      <Field label="Resolution note" hint="How it was resolved. Required to resolve." hintId={NOTE_HINT_ID}>
        <Textarea
          name="note"
          rows={4}
          maxLength={MAX_NOTE_LENGTH}
          defaultValue={v?.note ?? incident.resolutionNote ?? ""}
          aria-invalid={failed?.field === "note"}
          aria-describedby={[NOTE_HINT_ID, describedBy("note")].filter(Boolean).join(" ")}
          className="bg-card text-foreground"
        />
      </Field>
    </>
  );
}
