import { DEFAULT_FILTERS, activeFilterCount, toApiQuery, toSearchParams, type Filters } from "./filters";

type Narrowing = Pick<Filters, "status" | "unassigned">;

/** The headline groups an Operator works from, in the order work flows through them. */
const TILES: readonly { key: string; label: string; narrowing: Narrowing }[] = [
  { key: "open", label: "Open", narrowing: { status: "OPEN" } },
  { key: "in-progress", label: "In progress", narrowing: { status: "IN_PROGRESS" } },
  { key: "unassigned", label: "Unassigned", narrowing: { unassigned: true } },
  { key: "resolved", label: "Resolved", narrowing: { status: "RESOLVED" } },
];

export interface OverviewTile {
  key: string;
  label: string;
  /** What the tile counts and, when followed, narrows the list to. */
  narrowing: Narrowing;
  /** The list narrowed to this tile, or the plain list when it is already the active one. */
  href: string;
  /** True when this tile's filter is the only one applied. */
  active: boolean;
}

/**
 * The tiles above the list. Each is a count and a one-click filter. A tile is active only when
 * its filter is the whole filter, so it never claims a view it does not describe.
 */
export function overviewTiles(current: Filters): OverviewTile[] {
  return TILES.map(({ key, label, narrowing }) => {
    const active =
      activeFilterCount(current) === 1 &&
      current.status === narrowing.status &&
      Boolean(current.unassigned) === Boolean(narrowing.unassigned);
    const target: Filters = active
      ? { ...DEFAULT_FILTERS, sort: current.sort, order: current.order }
      : { ...DEFAULT_FILTERS, ...narrowing, sort: current.sort, order: current.order };
    const query = toSearchParams(target).toString();
    return { key, label, narrowing, active, href: query ? `/incidents?${query}` : "/incidents" };
  });
}

/** The cheapest request that returns a tile's total: one row, and the count in `X-Total-Count`. */
export function toCountQuery(tile: Pick<OverviewTile, "narrowing">) {
  return { ...toApiQuery({ ...DEFAULT_FILTERS, ...tile.narrowing }), _page: 1, _limit: 1 };
}

/** The letters shown in a person's avatar: the first letter of up to two names. */
export function initials(name: string): string {
  const letters = name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase());
  return letters.join("") || "?";
}
