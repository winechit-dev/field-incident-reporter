import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { Suspense } from "react";
import { ApiError, ApiUnavailableError } from "@/api/client";
import { getIncident, listUsers } from "@/api/incidents";
import { IncidentDetail } from "@/components/incident-detail";
import { Button } from "@/components/ui/button";
import { getOperator } from "@/lib/session";

export default function IncidentPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <main className="flex flex-col gap-5">
      <div>
        <Button variant="ghost" nativeButton={false} render={<Link href="/incidents" />}>
          ← All incidents
        </Button>
      </div>
      {/* The cookie, the route parameter and the API are all per-request, so this streams in. */}
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading incident…</p>}>
        <Content params={params} />
      </Suspense>
    </main>
  );
}

/** Loads the data. Failures become values here so the JSX stays outside `try`. */
async function load(operatorId: string, id: string) {
  try {
    const [incident, users] = await Promise.all([getIncident(operatorId, id), listUsers()]);
    return { status: "ok" as const, incident, users };
  } catch (error) {
    if (error instanceof ApiError && error.code === "UNKNOWN_USER") return { status: "unknown-user" as const };
    if (error instanceof ApiError && error.status === 404) return { status: "not-found" as const };
    if (error instanceof ApiUnavailableError) return { status: "unavailable" as const, message: error.message };
    throw error;
  }
}

async function Content({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const operator = await getOperator();
  if (!operator) redirect("/sign-in");

  const result = await load(operator.id, id);
  if (result.status === "unknown-user") redirect("/sign-in");
  if (result.status === "not-found") notFound();
  if (result.status === "unavailable") {
    return <p role="alert" className="text-sm text-destructive">{result.message}. Start the backend and reload.</p>;
  }
  return <IncidentDetail incident={result.incident} users={result.users} />;
}
