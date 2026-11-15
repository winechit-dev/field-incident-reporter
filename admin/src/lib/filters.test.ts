import { describe, expect, it } from "vitest";
import {
  DEFAULT_FILTERS, PAGE_SIZE, UNASSIGNED_OPTION, activeFilterCount, hasActiveFilters, pageCount, parseFilters, toApiQuery, toSearchParams,
} from "./filters";

describe("parseFilters", () => {
  it("returns the defaults for an empty URL", () => {
    expect(parseFilters({})).toEqual({
      status: undefined, severity: undefined, category: undefined, assignee: undefined,
      q: undefined, from: undefined, to: undefined, ...DEFAULT_FILTERS,
    });
  });

  it("reads valid values", () => {
    const f = parseFilters({
      status: "IN_PROGRESS", severity: "HIGH", category: "SAFETY", assignee: "u-op-1",
      q: "  leak ", from: "2026-11-01", to: "2026-11-15", sort: "updatedAt", order: "asc", page: "3",
    });
    expect(f).toMatchObject({
      status: "IN_PROGRESS", severity: "HIGH", category: "SAFETY", assignee: "u-op-1",
      q: "leak", from: "2026-11-01", to: "2026-11-15", sort: "updatedAt", order: "asc", page: 3,
    });
  });

  it("ignores invalid values instead of failing", () => {
    const f = parseFilters({
      status: "DONE", severity: "EXTREME", category: "x", assignee: "a b; drop", from: "2026-02-31",
      to: "yesterday", sort: "title", order: "sideways", page: "-4",
    });
    expect(f).toEqual({ ...parseFilters({}) });
  });

  it("uses the first value when a parameter repeats", () => {
    expect(parseFilters({ status: ["OPEN", "RESOLVED"] }).status).toBe("OPEN");
  });

  it("clamps the page and caps the search length", () => {
    expect(parseFilters({ page: "99999999" }).page).toBe(10_000);
    expect(parseFilters({ page: "abc" }).page).toBe(1);
    expect(parseFilters({ q: "x".repeat(500) }).q).toHaveLength(100);
  });

  it("accepts a real leap day and rejects a false one", () => {
    expect(parseFilters({ from: "2028-02-29" }).from).toBe("2028-02-29");
    expect(parseFilters({ from: "2027-02-29" }).from).toBeUndefined();
  });
});

describe("toApiQuery", () => {
  it("maps filters to the contract's query parameters", () => {
    const query = toApiQuery(parseFilters({ status: "OPEN", q: "oil", from: "2026-11-01", to: "2026-11-02", page: "2" }));
    expect(query).toEqual({
      status: "OPEN", severity: undefined, category: undefined, assignee: undefined, q: "oil",
      createdAt_gte: "2026-11-01T00:00:00.000Z", createdAt_lte: "2026-11-02T23:59:59.999Z",
      _sort: "createdAt", _order: "desc", _page: 2, _limit: PAGE_SIZE,
    });
  });

  it("covers the whole last day of the range", () => {
    expect(toApiQuery(parseFilters({ to: "2026-11-15" })).createdAt_lte).toBe("2026-11-15T23:59:59.999Z");
  });
});

describe("toSearchParams", () => {
  it("leaves defaults out", () => {
    expect(toSearchParams(parseFilters({})).toString()).toBe("");
    expect(toSearchParams(parseFilters({ page: "1", sort: "createdAt", order: "desc" })).toString()).toBe("");
  });

  it("round-trips through the URL", () => {
    const original = parseFilters({ status: "RESOLVED", severity: "LOW", q: "pipe", from: "2026-11-01", page: "4", order: "asc" });
    const params = Object.fromEntries(toSearchParams(original));
    expect(parseFilters(params)).toEqual(original);
  });
});

describe("pageCount", () => {
  it("is at least one and rounds up", () => {
    expect(pageCount(0)).toBe(1);
    expect(pageCount(PAGE_SIZE)).toBe(1);
    expect(pageCount(PAGE_SIZE + 1)).toBe(2);
  });
});

describe("Unassigned filter", () => {
  it("is read from assigned=false in the URL (criterion 9)", () => {
    expect(parseFilters({ assigned: "false" }).unassigned).toBe(true);
    expect(toApiQuery(parseFilters({ assigned: "false" })).assigned).toBe(false);
  });

  it("is written back as assigned=false and survives a round trip (criterion 9)", () => {
    const f = parseFilters({ assigned: "false", severity: "HIGH", page: "2" });
    expect(toSearchParams(f).get("assigned")).toBe("false");
    expect(parseFilters(Object.fromEntries(toSearchParams(f)))).toEqual(f);
  });

  it("is read from the assignee dropdown's Unassigned option, which no user id can equal (criterion 9)", () => {
    expect(/^[A-Za-z0-9-]{1,64}$/.test(UNASSIGNED_OPTION)).toBe(false);
    const f = parseFilters({ assignee: UNASSIGNED_OPTION });
    expect(f.unassigned).toBe(true);
    expect(f.assignee).toBeUndefined();
    expect(toSearchParams(f).toString()).toBe("assigned=false");
  });

  it("is mutually exclusive with an assignee (criterion 10)", () => {
    const f = parseFilters({ assigned: "false", assignee: "u-op-1" });
    expect(f.unassigned).toBe(true);
    expect(f.assignee).toBeUndefined();
    const q = toApiQuery(f);
    expect(q.assigned).toBe(false);
    expect(q.assignee).toBeUndefined();
    expect(toSearchParams(f).has("assignee")).toBe(false);
  });

  it("never sends both even if built by hand (criterion 10)", () => {
    const q = toApiQuery({ ...DEFAULT_FILTERS, unassigned: true, assignee: "u-op-1" });
    expect(q.assigned === false && q.assignee === undefined).toBe(true);
  });

  it("sends no assigned parameter when off (criterion 10)", () => {
    expect(toApiQuery(parseFilters({ assignee: "u-op-1" }))).toMatchObject({ assigned: undefined, assignee: "u-op-1" });
  });

  it("ignores invalid values (criterion 11)", () => {
    for (const bad of ["true", "maybe", "", "1", "FALSE"]) {
      const f = parseFilters({ assigned: bad });
      expect(f.unassigned).toBeUndefined();
      expect(toApiQuery(f).assigned).toBeUndefined();
    }
    expect(parseFilters({ assigned: ["false", "true"] }).unassigned).toBe(true);
  });

  it("counts as an active filter (criterion 11)", () => {
    expect(hasActiveFilters(parseFilters({ assigned: "false" }))).toBe(true);
    expect(hasActiveFilters(parseFilters({ assigned: "maybe" }))).toBe(false);
  });
});

describe("activeFilterCount", () => {
  it("is zero for the default view, whatever the sorting and page", () => {
    expect(activeFilterCount(DEFAULT_FILTERS)).toBe(0);
    expect(activeFilterCount({ ...DEFAULT_FILTERS, sort: "updatedAt", order: "asc", page: 3 })).toBe(0);
  });

  it("counts each narrowing filter once, and a date range as one", () => {
    expect(activeFilterCount({ ...DEFAULT_FILTERS, status: "OPEN", q: "lift" })).toBe(2);
    expect(activeFilterCount({ ...DEFAULT_FILTERS, from: "2026-01-01", to: "2026-01-31" })).toBe(1);
    expect(activeFilterCount({ ...DEFAULT_FILTERS, unassigned: true, severity: "HIGH", category: "SAFETY" })).toBe(3);
  });
});
