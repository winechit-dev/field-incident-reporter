import {
  CATEGORIES,
  SEVERITIES,
  STATUSES,
  type Category,
  type Severity,
  type Status,
} from "./domain";

export const PAGE_SIZE = 20;
const MAX_PAGE = 10_000;
const MAX_QUERY_LENGTH = 100;

export const SORT_KEYS = ["createdAt", "updatedAt"] as const;
export type SortKey = (typeof SORT_KEYS)[number];
export type SortOrder = "asc" | "desc";

/** What an Operator has asked to see. Everything is optional except paging and sorting. */
export interface Filters {
  status?: Status;
  severity?: Severity;
  category?: Category;
  /** An Operator's user id. */
  assignee?: string;
  /** Only Incidents with no assignee. Exclusive with `assignee`; `true` or absent, never `false`. */
  unassigned?: true;
  /** Free-text search. */
  q?: string;
  /** First day to include, `YYYY-MM-DD`, UTC. */
  from?: string;
  /** Last day to include, `YYYY-MM-DD`, UTC. */
  to?: string;
  sort: SortKey;
  order: SortOrder;
  page: number;
}

export const DEFAULT_FILTERS: Filters = { sort: "createdAt", order: "desc", page: 1 };

/**
 * The value the assignee dropdown submits for "Unassigned". It cannot be a user id (ids are
 * `[A-Za-z0-9-]`), so it never collides with one. The canonical URL form is `assigned=false`.
 */
export const UNASSIGNED_OPTION = "@unassigned";

type RawParams = Record<string, string | string[] | undefined>;

const first = (value: string | string[] | undefined) => (Array.isArray(value) ? value[0] : value);
const oneOf = <T extends string>(list: readonly T[], value: string | undefined): T | undefined =>
  list.find((item) => item === value);

/** A real calendar day, not just something shaped like one (`2026-02-31` is rejected). */
function validDay(value: string | undefined): string | undefined {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return undefined;
  const [y, m, d] = value.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  const real = date.getUTCFullYear() === y && date.getUTCMonth() === m - 1 && date.getUTCDate() === d;
  return real ? value : undefined;
}

/** Read filters from the URL. Anything invalid is ignored, so a hand-edited URL never breaks the page. */
export function parseFilters(params: RawParams): Filters {
  const q = first(params.q)?.trim().slice(0, MAX_QUERY_LENGTH);
  const rawAssignee = first(params.assignee);
  const unassigned = first(params.assigned) === "false" || rawAssignee === UNASSIGNED_OPTION;
  const assignee = unassigned ? undefined : rawAssignee;
  const page = Number.parseInt(first(params.page) ?? "", 10);
  return {
    status: oneOf(STATUSES, first(params.status)),
    severity: oneOf(SEVERITIES, first(params.severity)),
    category: oneOf(CATEGORIES, first(params.category)),
    assignee: assignee && /^[A-Za-z0-9-]{1,64}$/.test(assignee) ? assignee : undefined,
    unassigned: unassigned ? true : undefined,
    q: q || undefined,
    from: validDay(first(params.from)),
    to: validDay(first(params.to)),
    sort: oneOf(SORT_KEYS, first(params.sort)) ?? DEFAULT_FILTERS.sort,
    order: oneOf(["asc", "desc"] as const, first(params.order)) ?? DEFAULT_FILTERS.order,
    page: Number.isInteger(page) && page >= 1 ? Math.min(page, MAX_PAGE) : 1,
  };
}

/** The query for `GET /incidents`, as the contract defines it. */
export function toApiQuery(f: Filters) {
  return {
    status: f.status,
    severity: f.severity,
    category: f.category,
    assignee: f.unassigned ? undefined : f.assignee,
    assigned: f.unassigned ? (false as const) : undefined,
    q: f.q,
    createdAt_gte: f.from ? `${f.from}T00:00:00.000Z` : undefined,
    createdAt_lte: f.to ? `${f.to}T23:59:59.999Z` : undefined,
    _sort: f.sort,
    _order: f.order,
    _page: f.page,
    _limit: PAGE_SIZE,
  };
}

/** The URL form of the filters. Defaults are left out so shared links stay short. */
export function toSearchParams(f: Filters): URLSearchParams {
  const params = new URLSearchParams();
  const set = (key: string, value: string | undefined) => value && params.set(key, value);
  set("status", f.status);
  set("severity", f.severity);
  set("category", f.category);
  if (f.unassigned) params.set("assigned", "false");
  else set("assignee", f.assignee);
  set("q", f.q);
  set("from", f.from);
  set("to", f.to);
  if (f.sort !== DEFAULT_FILTERS.sort) params.set("sort", f.sort);
  if (f.order !== DEFAULT_FILTERS.order) params.set("order", f.order);
  if (f.page > 1) params.set("page", String(f.page));
  return params;
}

export function hasActiveFilters(f: Filters): boolean {
  return Boolean(f.status || f.severity || f.category || f.assignee || f.unassigned || f.q || f.from || f.to);
}

/** How many things narrow the list. Sorting and paging do not; a date range counts once. */
export function activeFilterCount(f: Filters): number {
  return [f.status, f.severity, f.category, f.assignee || f.unassigned, f.q, f.from || f.to].filter(Boolean).length;
}

export function pageCount(total: number): number {
  return Math.max(1, Math.ceil(total / PAGE_SIZE));
}

/** The rows shown on a page, or null when the page holds none: no rows at all, or a page past the last. */
export function pageRange(page: number, total: number): { first: number; last: number } | null {
  if (total === 0 || page > pageCount(total)) return null;
  return { first: (page - 1) * PAGE_SIZE + 1, last: Math.min(page * PAGE_SIZE, total) };
}

/**
 * Where to send a request whose query is not in its shortest form, or null when it already is.
 * A submitted filter form adds every field, empty or default (`?q=&status=OPEN&severity=&…`);
 * redirecting to the canonical form keeps the address bar, and so shared links, short.
 */
export function canonicalHref(params: RawParams): string | null {
  const given = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    for (const item of Array.isArray(value) ? value : [value]) if (item !== undefined) given.append(key, item);
  }
  const canonical = toSearchParams(parseFilters(params)).toString();
  if (given.toString() === canonical) return null;
  return canonical ? `/incidents?${canonical}` : "/incidents";
}
