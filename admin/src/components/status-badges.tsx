import { Badge } from "@/components/ui/badge";
import { SEVERITY_LABEL, STATUS_LABEL, type Severity, type Status } from "@/lib/domain";
import { cn } from "@/lib/utils";

const STATUS_STYLE: Record<Status, string> = {
  OPEN: "bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300",
  IN_PROGRESS: "bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-300",
  RESOLVED: "bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300",
};

const SEVERITY_STYLE: Record<Severity, string> = {
  LOW: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
  MEDIUM: "bg-yellow-100 text-yellow-900 dark:bg-yellow-950 dark:text-yellow-300",
  HIGH: "bg-orange-100 text-orange-900 dark:bg-orange-950 dark:text-orange-300",
  CRITICAL: "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300",
};

export function StatusBadge({ status }: { status: Status }) {
  return <Badge className={cn(STATUS_STYLE[status])}>{STATUS_LABEL[status]}</Badge>;
}

export function SeverityBadge({ severity }: { severity: Severity }) {
  return <Badge className={cn(SEVERITY_STYLE[severity])}>{SEVERITY_LABEL[severity]}</Badge>;
}
