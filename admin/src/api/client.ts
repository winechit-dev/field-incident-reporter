import createClient from "openapi-fetch";
import type { paths } from "./schema";

const baseUrl = process.env.API_BASE_URL ?? "http://localhost:3000";

/** The API could not be reached, or answered with something other than the contract. */
export class ApiUnavailableError extends Error {}

/** The API refused the request. Branch on `code`, never on `message`. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

/** A typed client for the contract. Every request carries the Current User in `X-User-Id`. */
export function createApi(userId?: string) {
  return createClient<paths>({
    baseUrl,
    headers: userId ? { "X-User-Id": userId } : undefined,
    cache: "no-store",
  });
}

/** Turns a failed response into an [ApiError], and a network failure into [ApiUnavailableError]. */
export async function unwrap<T>(
  call: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<{ data: T; response: Response }> {
  let result;
  try {
    result = await call;
  } catch {
    throw new ApiUnavailableError(`Can't reach the API at ${baseUrl}`);
  }
  if (result.data !== undefined) return { data: result.data, response: result.response };
  const body = result.error as { error?: { code?: string; message?: string } } | undefined;
  throw new ApiError(result.response.status, body?.error?.code ?? "UNKNOWN", body?.error?.message ?? result.response.statusText);
}
