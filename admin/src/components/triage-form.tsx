"use client";

import { useActionState } from "react";
import { triage, type TriageState } from "@/app/incidents/[id]/actions";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { STATUSES, STATUS_LABEL, type Incident, type Status, type User } from "@/lib/domain";
import { actionsFor, MAX_NOTE_LENGTH } from "@/lib/triage";

const selectClass =
  "h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-destructive dark:bg-input/30";

const QUICK_LABEL = { start: "Start work", resolve: "Resolve", reopen: "Reopen" } as const;

/**
 * Triage controls for one Incident. Quick actions and Save submit the same form, told apart by
 * `intent`. The fields are remounted whenever the Incident changes on the server, so they always
 * show what is stored.
 */
export function TriageForm({ incident, operators }: { incident: Incident; operators: User[] }) {
  const [state, action, pending] = useActionState<TriageState, FormData>(triage, { status: "idle" });
  const failed = state.status === "error" ? state : null;

  return (
    <form action={action} className="flex flex-col gap-4">
      <input type="hidden" name="incidentId" value={incident.id} />
      <Fields key={incident.updatedAt} incident={incident} operators={operators} failed={failed} />

      {failed && <p role="alert" className="text-sm text-destructive">{failed.message}</p>}
      {state.status === "success" && <p role="status" className="text-sm text-green-700 dark:text-green-400">Saved.</p>}

      <div className="flex flex-wrap gap-2">
        {actionsFor(incident.status).map((intent) => (
          <Button key={intent} type="submit" name="intent" value={intent} variant={intent === "resolve" ? "default" : "outline"} disabled={pending}>
            {QUICK_LABEL[intent]}
          </Button>
        ))}
        <Button type="submit" name="intent" value="save" variant="secondary" disabled={pending}>
          {pending ? "Saving…" : "Save changes"}
        </Button>
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
  return (
    <>
      <label className="flex flex-col gap-1 text-xs font-medium text-muted-foreground">
        Status
        <select name="status" defaultValue={v?.status ?? incident.status} aria-invalid={failed?.field === "status"} className={selectClass}>
          {STATUSES.map((s: Status) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-xs font-medium text-muted-foreground">
        Assignee
        <select name="assignee" defaultValue={v?.assignee ?? incident.assignee ?? ""} aria-invalid={failed?.field === "assignee"} className={selectClass}>
          <option value="">Unassigned</option>
          {operators.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-xs font-medium text-muted-foreground">
        Resolution note
        <Textarea
          name="note"
          rows={4}
          maxLength={MAX_NOTE_LENGTH}
          defaultValue={v?.note ?? incident.resolutionNote ?? ""}
          aria-invalid={failed?.field === "note"}
          placeholder="How was it resolved? Required to resolve."
        />
      </label>
    </>
  );
}
