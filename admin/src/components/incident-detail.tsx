import Image from "next/image";
import { MapPin } from "lucide-react";
import { LocalTime } from "@/components/local-time";
import { SeverityBadge, StatusBadge } from "@/components/status-badges";
import { TriageForm } from "@/components/triage-form";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { CATEGORY_LABEL, STATUS_LABEL, type Incident, type User } from "@/lib/domain";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader><CardTitle role="heading" aria-level={2}>{title}</CardTitle></CardHeader>
      <CardContent className="flex flex-col gap-2 text-sm">{children}</CardContent>
    </Card>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right">{children}</dd>
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
  const photoCount = incident.photos.length;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold text-balance">{incident.title}</h1>
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
          <StatusBadge status={incident.status} />
          <SeverityBadge severity={incident.severity} />
          <span>{CATEGORY_LABEL[incident.category]}</span>
          <span aria-hidden="true">·</span>
          <span>Reported by {nameOf(incident.reportedBy)}, <LocalTime iso={incident.createdAt} /></span>
        </div>
      </div>

      <div className="grid items-start gap-5 lg:grid-cols-3">
        <div className="flex flex-col gap-5 lg:col-span-2">
          <Section title="What happened">
            {incident.description ? <p className="whitespace-pre-wrap">{incident.description}</p> : <p className="text-muted-foreground">No description.</p>}
          </Section>

          <Section title="Location">
            {location.address && <p>{location.address}</p>}
            {hasCoordinates ? (
              <p className="flex flex-wrap items-center gap-x-2 text-muted-foreground">
                <span className="font-mono text-xs">{location.latitude}, {location.longitude}</span>
                <a
                  className="inline-flex items-center gap-1 rounded-sm text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
                  href={mapLink(location.latitude!, location.longitude!)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <MapPin aria-hidden="true" className="size-3.5" />
                  Open map<span className="sr-only"> (opens in a new tab)</span>
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

          <Section title={`Photos (${photoCount})`}>
            {photoCount === 0 ? (
              <p className="text-muted-foreground">No photos yet.</p>
            ) : (
              <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                {incident.photos.map((photo, index) => (
                  <li key={photo.id}>
                    <a
                      href={photo.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block overflow-hidden rounded-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    >
                      {/* Served by the API, not by this app, so Next's optimiser is skipped. */}
                      <Image
                        src={photo.url}
                        alt={`Photo ${index + 1} of ${photoCount}: ${incident.title} (opens full size in a new tab)`}
                        width={400}
                        height={300}
                        unoptimized
                        className="aspect-[4/3] w-full bg-muted object-cover transition-opacity hover:opacity-90"
                      />
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </Section>

          <Section title="Activity">
            {/* Newest first. The line down the left ties the entries into one history. */}
            <ol className="flex flex-col gap-3 border-l pl-4">
              {log.map((entry) => (
                <li key={`${entry.at}-${entry.status}`} className="relative flex flex-col">
                  <span aria-hidden="true" className="absolute top-1.5 -left-[21px] size-2 rounded-full bg-primary ring-4 ring-card" />
                  <span>Moved to {STATUS_LABEL[entry.status]} by {nameOf(entry.actorId)}</span>
                  <span className="text-xs text-muted-foreground"><LocalTime iso={entry.at} /></span>
                </li>
              ))}
              <li className="relative flex flex-col">
                <span aria-hidden="true" className="absolute top-1.5 -left-[21px] size-2 rounded-full bg-muted-foreground ring-4 ring-card" />
                <span>Reported by {nameOf(incident.reportedBy)}</span>
                <span className="text-xs text-muted-foreground"><LocalTime iso={incident.createdAt} /></span>
              </li>
            </ol>
          </Section>
        </div>

        {/* Stays in view on a wide screen, so triage is always one click away while reading. */}
        <div className="flex flex-col gap-5 lg:sticky lg:top-20">
          <Section title="Triage">
            <TriageForm incident={incident} operators={users.filter((u) => u.role === "OPERATOR")} />
          </Section>

          <Section title="Details">
            <dl className="flex flex-col gap-2">
              <Row label="Assignee">{nameOf(incident.assignee) ?? "Unassigned"}</Row>
              <Row label="Reported by">{nameOf(incident.reportedBy)}</Row>
              <Row label="Reported"><LocalTime iso={incident.createdAt} /></Row>
              <Row label="Last updated"><LocalTime iso={incident.updatedAt} /></Row>
              {incident.resolvedAt && <Row label="Resolved"><LocalTime iso={incident.resolvedAt} /></Row>}
            </dl>
          </Section>
        </div>
      </div>
    </div>
  );
}
