import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import { redirect } from "next/navigation";
import { connection } from "next/server";
import { ApiError, ApiUnavailableError } from "@/api/client";
import { countIncidents, listIncidents, listUsers } from "@/api/incidents";
import { FilterBar } from "@/components/filter-bar";
import { Overview } from "@/components/overview";
import { IncidentTable } from "@/components/incident-table";
import { Pagination } from "@/components/pagination";
import { IncidentsSkeleton } from "@/components/skeletons";
import { ApiUnavailable, EmptyState } from "@/components/states";
import { Button } from "@/components/ui/button";
import { hasActiveFilters, parseFilters, type Filters } from "@/lib/filters";
import { overviewTiles, toCountQuery } from "@/lib/overview";
import { getOperator } from "@/lib/session";

export const metadata: Metadata = { title: "Incidents" };

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default function IncidentsPage({ searchParams }: { searchParams: SearchParams }) {
  return (
    <main id="main" className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">Incidents</h1>
        <p className="text-sm text-muted-foreground">Reports from Technicians in the field. Open one to triage it.</p>
      </div>
      {/* Filters, the cookie and the API are all per-request, so the page streams in behind a boundary. */}
      <Suspense fallback={<IncidentsSkeleton />}>
        <IncidentsContent searchParams={searchParams} />
      </Suspense>
    </main>
  );
}

/** Loads the page's data. Failures are turned into values here so the JSX below stays outside `try`. */
async function load(operatorId: string, filters: Filters) {
  try {
    const tiles = overviewTiles(filters);
    const [{ incidents, total }, users, counts] = await Promise.all([
      listIncidents(operatorId, filters),
      listUsers(),
      Promise.all(tiles.map((tile) => countIncidents(operatorId, toCountQuery(tile)))),
    ]);
    return { status: "ok" as const, incidents, total, users, tiles, counts };
  } catch (error) {
    if (error instanceof ApiError && error.code === "UNKNOWN_USER") return { status: "unknown-user" as const };
    if (error instanceof ApiUnavailableError) return { status: "unavailable" as const, message: error.message };
    throw error;
  }
}

async function IncidentsContent({ searchParams }: { searchParams: SearchParams }) {
  await connection(); // always live data, never prerendered
  const filters = parseFilters(await searchParams);
  const operator = await getOperator();
  if (!operator) redirect("/sign-in");

  const result = await load(operator.id, filters);
  if (result.status === "unknown-user") redirect("/sign-in"); // the chosen user no longer exists
  if (result.status === "unavailable") return <ApiUnavailable message={result.message} />;

  const { incidents, total, users, tiles, counts } = result;
  return (
    <>
      <Overview tiles={tiles} counts={counts} />
      <FilterBar filters={filters} operators={users.filter((u) => u.role === "OPERATOR")} />
      {incidents.length > 0 ? (
        <IncidentTable incidents={incidents} users={users} />
      ) : total > 0 ? (
        // Past the last page, for example after a shared link outlived its Incidents.
        <EmptyState title="Nothing on this page." description="There are fewer incidents than this page number expects.">
          <Button variant="outline" nativeButton={false} render={<Link href="/incidents" />}>Go to the first page</Button>
        </EmptyState>
      ) : hasActiveFilters(filters) ? (
        <EmptyState title="No incidents match these filters." description="Loosen or clear the filters to see more.">
          <Button variant="outline" nativeButton={false} render={<Link href="/incidents" />}>Clear filters</Button>
        </EmptyState>
      ) : (
        <EmptyState title="No incidents yet." description="When a Technician reports one from the field, it appears here." />
      )}
      <Pagination filters={filters} total={total} />
    </>
  );
}
