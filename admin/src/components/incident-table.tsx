import Link from "next/link";
import { LocalTime } from "@/components/local-time";
import { SeverityBadge, StatusBadge } from "@/components/status-badges";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { CATEGORY_LABEL, type Incident, type User } from "@/lib/domain";

/**
 * The title is the row's link, stretched over the whole row so anywhere on it opens the
 * Incident; keyboard and screen-reader users still meet one link per row. The two least
 * important columns drop out on narrow screens so the table does not scroll sideways.
 */
export function IncidentTable({ incidents, users }: { incidents: Incident[]; users: User[] }) {
  const nameOf = (id: string | null) => (id ? (users.find((u) => u.id === id)?.name ?? id) : null);

  return (
    <div className="overflow-hidden rounded-xl border bg-card">
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/50 hover:bg-muted/50">
            <TableHead className="pl-4">Title</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Severity</TableHead>
            <TableHead className="hidden lg:table-cell">Category</TableHead>
            <TableHead className="hidden lg:table-cell">Reported by</TableHead>
            <TableHead>Assignee</TableHead>
            <TableHead className="pr-4">Reported</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {incidents.map((incident) => (
            <TableRow key={incident.id} className="relative focus-within:bg-muted/50">
              <TableCell className="max-w-72 truncate py-3 pl-4 font-medium">
                <Link
                  href={`/incidents/${incident.id}`}
                  className="outline-none after:absolute after:inset-0 focus-visible:underline focus-visible:after:ring-2 focus-visible:after:ring-ring focus-visible:after:ring-inset"
                >
                  {incident.title}
                </Link>
              </TableCell>
              <TableCell><StatusBadge status={incident.status} /></TableCell>
              <TableCell><SeverityBadge severity={incident.severity} /></TableCell>
              <TableCell className="hidden lg:table-cell">{CATEGORY_LABEL[incident.category]}</TableCell>
              <TableCell className="hidden lg:table-cell">{nameOf(incident.reportedBy)}</TableCell>
              <TableCell className={incident.assignee ? "" : "text-muted-foreground"}>
                {nameOf(incident.assignee) ?? "Unassigned"}
              </TableCell>
              <TableCell className="pr-4 text-muted-foreground"><LocalTime iso={incident.createdAt} /></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
