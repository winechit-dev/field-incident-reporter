import { Suspense } from "react";
import { redirect } from "next/navigation";
import { ApiError, ApiUnavailableError } from "@/api/client";
import { listIncidents, listUsers } from "@/api/incidents";
import { FilterBar } from "@/components/filter-bar";
import { IncidentTable } from "@/components/incident-table";
import { Pagination } from "@/components/pagination";
import { parseFilters, type Filters } from "@/lib/filters";
import { getOperator } from "@/lib/session";

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default function IncidentsPage({ searchParams }: { searchParams: SearchParams }) {
  return (
    <main className="flex flex-col gap-5">
      <h1 className="text-2xl font-semibold">Incidents</h1>
      {/* Filters, the cookie and the API are all per-request, so the page streams in behind a boundary. */}
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading incidents…</p>}>
        <IncidentsContent searchParams={searchParams} />
      </Suspense>
    </main>
  );
}

/** Loads the page's data. Failures are turned into values here so the JSX below stays outside `try`. */
async function load(operatorId: string, filters: Filters) {
  try {
    const [{ incidents, total }, users] = await Promise.all([listIncidents(operatorId, filters), listUsers()]);
    return { status: "ok" as const, incidents, total, users };
  } catch (error) {
    if (error instanceof ApiError && error.code === "UNKNOWN_USER") return { status: "unknown-user" as const };
    if (error instanceof ApiUnavailableError) return { status: "unavailable" as const, message: error.message };
    throw error;
  }
}

async function IncidentsContent({ searchParams }: { searchParams: SearchParams }) {
  const filters = parseFilters(await searchParams);
  const operator = await getOperator();
  if (!operator) redirect("/sign-in");

  const result = await load(operator.id, filters);
  if (result.status === "unknown-user") redirect("/sign-in"); // the chosen user no longer exists
  if (result.status === "unavailable") {
    return <p role="alert" className="text-sm text-destructive">{result.message}. Start the backend and reload.</p>;
  }

  const { incidents, total, users } = result;
  return (
    <>
      <FilterBar filters={filters} operators={users.filter((u) => u.role === "OPERATOR")} />
      {incidents.length === 0 ? (
        <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
          No incidents match these filters.
        </p>
      ) : (
        <IncidentTable incidents={incidents} users={users} />
      )}
      <Pagination filters={filters} total={total} />
    </>
  );
}
