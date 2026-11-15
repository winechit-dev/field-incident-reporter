import { createApi, unwrap } from "./client";
import { toApiQuery, type Filters } from "@/lib/filters";
import type { Incident, User } from "@/lib/domain";
import type { OperatorPatch } from "@/lib/triage";

/** Public: the sign-in page needs it before anyone is chosen. */
export async function listUsers(): Promise<User[]> {
  const { data } = await unwrap(createApi().GET("/users"));
  return data;
}

export async function listIncidents(userId: string, filters: Filters): Promise<{ incidents: Incident[]; total: number }> {
  const { data, response } = await unwrap(
    createApi(userId).GET("/incidents", { params: { query: toApiQuery(filters) } }),
  );
  const total = Number(response.headers.get("X-Total-Count"));
  return { incidents: data, total: Number.isFinite(total) ? total : data.length };
}

/** @throws ApiError with status 404 when the Incident does not exist. */
export async function getIncident(userId: string, id: string): Promise<Incident> {
  const { data } = await unwrap(createApi(userId).GET("/incidents/{incidentId}", { params: { path: { incidentId: id } } }));
  return data;
}

/** An Operator's change. The server owns the Activity Log and timestamps. */
export async function updateIncident(userId: string, id: string, patch: OperatorPatch): Promise<Incident> {
  const { data } = await unwrap(
    createApi(userId).PATCH("/incidents/{incidentId}", { params: { path: { incidentId: id } }, body: patch }),
  );
  return data;
}
