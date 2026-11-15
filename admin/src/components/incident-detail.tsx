import Image from "next/image";
import { LocalTime } from "@/components/local-time";
import { SeverityBadge, StatusBadge } from "@/components/status-badges";
import { TriageForm } from "@/components/triage-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CATEGORY_LABEL, STATUS_LABEL, type Incident, type User } from "@/lib/domain";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader><CardTitle>{title}</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-2 text-sm">{children}</CardContent>
    </Card>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right">{children}</span>
    </div>
  );
}

function mapLink(latitude: number, longitude: number) {
  return `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=17/${latitude}/${longitude}`;
}

export function IncidentDetail({ incident, users }: { incident: Incident; users: User[] }) {
  const nameOf = (id: string | null) => (id ? (users.find((u) => u.id === id)?.name ?? id) : null);
  const { location } = incident;
  const hasCoordinates = location.latitude != null && location.longitude != null;
  const log = [...incident.activityLog].reverse();

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold">{incident.title}</h1>
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={incident.status} />
          <SeverityBadge severity={incident.severity} />
          <span className="text-sm text-muted-foreground">{CATEGORY_LABEL[incident.category]}</span>
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <div className="flex flex-col gap-5 lg:col-span-2">
          <Section title="What happened">
            {incident.description ? <p className="whitespace-pre-wrap">{incident.description}</p> : <p className="text-muted-foreground">No description.</p>}
          </Section>

          <Section title="Location">
            {location.address && <p>{location.address}</p>}
            {hasCoordinates ? (
              <p className="text-muted-foreground">
                {location.latitude}, {location.longitude} ·{" "}
                <a className="underline" href={mapLink(location.latitude!, location.longitude!)} target="_blank" rel="noopener noreferrer">
                  Open map
                </a>
              </p>
            ) : (
              !location.address && <p className="text-muted-foreground">Location unavailable.</p>
            )}
          </Section>

          {incident.status === "RESOLVED" && incident.resolutionNote && (
            <Section title="Resolution">
              <p className="whitespace-pre-wrap">{incident.resolutionNote}</p>
            </Section>
          )}

          <Section title={`Photos (${incident.photos.length})`}>
            {incident.photos.length === 0 ? (
              <p className="text-muted-foreground">No photos yet.</p>
            ) : (
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                {incident.photos.map((photo) => (
                  <a key={photo.id} href={photo.url} target="_blank" rel="noopener noreferrer">
                    {/* Served by the API, not by this app, so Next's optimiser is skipped. */}
                    <Image src={photo.url} alt="Incident photo" width={400} height={300} unoptimized className="aspect-[4/3] w-full rounded-lg object-cover" />
                  </a>
                ))}
              </div>
            )}
          </Section>

          <Section title="Activity">
            {log.map((entry) => (
              <p key={`${entry.at}-${entry.status}`}>
                <span className="text-muted-foreground"><LocalTime iso={entry.at} /> · </span>
                Moved to {STATUS_LABEL[entry.status]} by {nameOf(entry.actorId)}
              </p>
            ))}
            <p>
              <span className="text-muted-foreground"><LocalTime iso={incident.createdAt} /> · </span>
              Reported by {nameOf(incident.reportedBy)}
            </p>
          </Section>
        </div>

        <div className="flex flex-col gap-5">
          <Section title="Triage">
            <TriageForm incident={incident} operators={users.filter((u) => u.role === "OPERATOR")} />
          </Section>

          <Section title="Details">
            <Row label="Reported by">{nameOf(incident.reportedBy)}</Row>
            <Row label="Assignee">{nameOf(incident.assignee) ?? "Unassigned"}</Row>
            <Row label="Reported"><LocalTime iso={incident.createdAt} /></Row>
            <Row label="Last updated"><LocalTime iso={incident.updatedAt} /></Row>
            {incident.resolvedAt && <Row label="Resolved"><LocalTime iso={incident.resolvedAt} /></Row>}
          </Section>
        </div>
      </div>
    </div>
  );
}
