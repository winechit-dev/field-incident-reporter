import { LocalTime } from "@/components/local-time";
import { SeverityBadge, StatusBadge } from "@/components/status-badges";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { CATEGORY_LABEL, type Incident, type User } from "@/lib/domain";

export function IncidentTable({ incidents, users }: { incidents: Incident[]; users: User[] }) {
  const nameOf = (id: string | null) => (id ? (users.find((u) => u.id === id)?.name ?? id) : null);

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Title</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Severity</TableHead>
          <TableHead>Category</TableHead>
          <TableHead>Reported by</TableHead>
          <TableHead>Assignee</TableHead>
          <TableHead>Reported</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {incidents.map((incident) => (
          <TableRow key={incident.id}>
            <TableCell className="max-w-72 truncate font-medium">{incident.title}</TableCell>
            <TableCell><StatusBadge status={incident.status} /></TableCell>
            <TableCell><SeverityBadge severity={incident.severity} /></TableCell>
            <TableCell>{CATEGORY_LABEL[incident.category]}</TableCell>
            <TableCell>{nameOf(incident.reportedBy)}</TableCell>
            <TableCell className={incident.assignee ? "" : "text-muted-foreground"}>
              {nameOf(incident.assignee) ?? "Unassigned"}
            </TableCell>
            <TableCell><LocalTime iso={incident.createdAt} /></TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
