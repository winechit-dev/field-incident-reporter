import { Badge } from "@/components/ui/badge";
import { SEVERITIES, SEVERITY_LABEL, STATUS_LABEL, type Severity, type Status } from "@/lib/domain";
import { cn } from "@/lib/utils";

/*
 * The only place an enum becomes a colour. The mobile app uses the same pairs
 * (docs/design/design-system.md), so a change here is made there too.
 */
const STATUS_STYLE: Record<Status, string> = {
  OPEN: "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300",
  IN_PROGRESS: "bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-300",
  RESOLVED: "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300",
};

/** The solid colour of a Status, for a dot beside its name. */
export const STATUS_DOT: Record<Status, string> = {
  OPEN: "bg-blue-800 dark:bg-blue-300",
  IN_PROGRESS: "bg-amber-900 dark:bg-amber-300",
  RESOLVED: "bg-green-800 dark:bg-green-300",
};

const SEVERITY_BAR: Record<Severity, string> = {
  LOW: "bg-slate-700 dark:bg-slate-300",
  MEDIUM: "bg-yellow-900 dark:bg-yellow-300",
  HIGH: "bg-orange-900 dark:bg-orange-300",
  CRITICAL: "bg-red-800 dark:bg-red-300",
};

const BAR_HEIGHT = ["h-[3px]", "h-[6px]", "h-[9px]", "h-[12px]"] as const;

/** Where an Incident stands: a tinted pill with a dot. */
export function StatusBadge({ status }: { status: Status }) {
  return (
    <Badge className={cn("gap-1.5", STATUS_STYLE[status])}>
      <span aria-hidden="true" className={cn("size-1.5 rounded-full", STATUS_DOT[status])} />
      {STATUS_LABEL[status]}
    </Badge>
  );
}

/**
 * How serious an Incident is: one to four bars and the word. Drawn differently from Status on
 * purpose, so the two are never read as the same kind of thing. The bars repeat the word, so
 * they are hidden from screen readers.
 */
export function SeverityBadge({ severity }: { severity: Severity }) {
  const level = SEVERITIES.indexOf(severity) + 1;
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-medium whitespace-nowrap text-foreground">
      <span aria-hidden="true" className="flex h-3 items-end gap-0.5">
        {BAR_HEIGHT.map((height, index) => (
          <span key={height} className={cn("w-[3px] rounded-[1px]", height, index < level ? SEVERITY_BAR[severity] : "bg-border")} />
        ))}
      </span>
      {SEVERITY_LABEL[severity]}
    </span>
  );
}
