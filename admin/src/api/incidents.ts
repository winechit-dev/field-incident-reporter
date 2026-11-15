import { createApi, unwrap } from "./client";
import { toApiQuery, type Filters } from "@/lib/filters";
import type { Incident, User } from "@/lib/domain";

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
