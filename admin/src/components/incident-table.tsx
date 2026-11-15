import Link from "next/link";
import { Avatar } from "@/components/avatar";
import { LocalTime } from "@/components/local-time";
import { SeverityBadge, StatusBadge } from "@/components/status-badges";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { CATEGORY_LABEL, type Incident, type User } from "@/lib/domain";

/**
 * The title is the row's link, stretched over the whole row so anywhere on it opens the
 * Incident; keyboard and screen-reader users still meet one link per row. Category and reporter
 * sit under the title as a second line. Assignee drops out below `md` and Reported below `sm`,
 * so the table never scrolls sideways; both are on the detail page.
 */
export function IncidentTable({ incidents, users }: { incidents: Incident[]; users: User[] }) {
  const nameOf = (id: string | null) => (id ? (users.find((u) => u.id === id)?.name ?? id) : null);
  const head = "h-9 text-xs font-medium text-muted-foreground";

  return (
    <div className="overflow-hidden rounded-xl border bg-card">
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/40 hover:bg-muted/40">
            <TableHead className={`${head} pl-4`}>Incident</TableHead>
            <TableHead className={head}>Status</TableHead>
            <TableHead className={head}>Severity</TableHead>
            <TableHead className={`${head} hidden md:table-cell`}>Assignee</TableHead>
            <TableHead className={`${head} hidden pr-4 text-right sm:table-cell`}>Reported</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {incidents.map((incident) => {
            const assignee = nameOf(incident.assignee);
            return (
              <TableRow key={incident.id} className="relative focus-within:bg-muted/50">
                <TableCell className="max-w-48 py-3 pl-4 sm:max-w-96">
                  <Link
                    href={`/incidents/${incident.id}`}
                    className="block truncate font-medium outline-none after:absolute after:inset-0 focus-visible:underline focus-visible:after:ring-2 focus-visible:after:ring-ring focus-visible:after:ring-inset"
                  >
                    {incident.title}
                  </Link>
                  <span className="block truncate text-xs text-muted-foreground">
                    {CATEGORY_LABEL[incident.category]} · Reported by {nameOf(incident.reportedBy)}
                  </span>
                </TableCell>
                <TableCell><StatusBadge status={incident.status} /></TableCell>
                <TableCell><SeverityBadge severity={incident.severity} /></TableCell>
                <TableCell className="hidden md:table-cell">
                  {assignee ? (
                    <span className="flex items-center gap-2"><Avatar name={assignee} />{assignee}</span>
                  ) : (
                    <span className="text-muted-foreground">Unassigned</span>
                  )}
                </TableCell>
                <TableCell className="hidden pr-4 text-right text-muted-foreground sm:table-cell"><LocalTime iso={incident.createdAt} /></TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
