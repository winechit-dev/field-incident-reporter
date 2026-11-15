import { describe, expect, it } from "vitest";
import { DEFAULT_FILTERS, parseFilters } from "./filters";
import { initials, overviewTiles, toCountQuery } from "./overview";

describe("overviewTiles", () => {
  it("offers Open, In progress, Unassigned and Resolved, in that order", () => {
    expect(overviewTiles(DEFAULT_FILTERS).map((t) => t.label)).toEqual(["Open", "In progress", "Unassigned", "Resolved"]);
  });

  it("links each tile to the list narrowed to it, and none is active on the default view", () => {
    const tiles = overviewTiles(DEFAULT_FILTERS);
    expect(tiles.map((t) => t.href)).toEqual([
      "/incidents?status=OPEN", "/incidents?status=IN_PROGRESS", "/incidents?assigned=false", "/incidents?status=RESOLVED",
    ]);
    expect(tiles.some((t) => t.active)).toBe(false);
  });

  it("marks the tile whose filter is the only one applied, and clicking it again clears it", () => {
    const tiles = overviewTiles(parseFilters({ status: "OPEN" }));
    expect(tiles.filter((t) => t.active).map((t) => t.label)).toEqual(["Open"]);
    expect(tiles[0].href).toBe("/incidents");
  });

  it("is not active when another filter narrows the list further", () => {
    expect(overviewTiles(parseFilters({ status: "OPEN", severity: "HIGH" })).some((t) => t.active)).toBe(false);
    expect(overviewTiles(parseFilters({ status: "OPEN", q: "lift" })).some((t) => t.active)).toBe(false);
  });

  it("keeps the chosen sorting and returns to page one", () => {
    const tiles = overviewTiles(parseFilters({ sort: "updatedAt", order: "asc", page: "3" }));
    expect(tiles[2].href).toBe("/incidents?assigned=false&sort=updatedAt&order=asc");
  });

  it("round-trips: following a tile's link makes exactly that tile active", () => {
    for (const tile of overviewTiles(DEFAULT_FILTERS)) {
      const params = Object.fromEntries(new URL(tile.href, "http://x").searchParams);
      expect(overviewTiles(parseFilters(params)).filter((t) => t.active).map((t) => t.key)).toEqual([tile.key]);
    }
  });
});

describe("toCountQuery", () => {
  it("asks for one row of everything the tile covers, whatever the page's own filters", () => {
    const [open, , unassigned] = overviewTiles(parseFilters({ q: "lift", page: "4" }));
    expect(toCountQuery(open)).toMatchObject({ status: "OPEN", q: undefined, _page: 1, _limit: 1 });
    expect(toCountQuery(unassigned)).toMatchObject({ assigned: false, status: undefined, _page: 1, _limit: 1 });
  });
});

describe("initials", () => {
  it("takes the first letter of up to two names", () => {
    expect(initials("Mya Sandar")).toBe("MS");
    expect(initials("htet")).toBe("H");
    expect(initials("  a  b  c ")).toBe("AB");
    expect(initials("  ")).toBe("?");
  });
});
