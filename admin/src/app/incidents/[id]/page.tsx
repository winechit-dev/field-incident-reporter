import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { connection } from "next/server";
import { Suspense } from "react";
import { ApiError, ApiUnavailableError } from "@/api/client";
import { getIncident, listUsers } from "@/api/incidents";
import { IncidentDetail } from "@/components/incident-detail";
import { ArrowLeft } from "lucide-react";
import { IncidentSkeleton } from "@/components/skeletons";
import { ApiUnavailable } from "@/components/states";
import { Button } from "@/components/ui/button";
import { getOperator } from "@/lib/session";

export const metadata: Metadata = { title: "Incident" };

export default function IncidentPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <main id="main" className="flex flex-col gap-4">
      <div>
        <Button variant="ghost" className="-ml-2.5 text-muted-foreground" nativeButton={false} render={<Link href="/incidents" />}>
          <ArrowLeft aria-hidden="true" /> All incidents
        </Button>
      </div>
      {/* The cookie, the route parameter and the API are all per-request, so this streams in. */}
      <Suspense fallback={<IncidentSkeleton />}>
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
  await connection(); // always live data, never prerendered
  const { id } = await params;
  const operator = await getOperator();
  if (!operator) redirect("/sign-in");

  const result = await load(operator.id, id);
  if (result.status === "unknown-user") redirect("/sign-in");
  if (result.status === "not-found") notFound();
  if (result.status === "unavailable") return <ApiUnavailable message={result.message} />;
  return <IncidentDetail incident={result.incident} users={result.users} />;
}
