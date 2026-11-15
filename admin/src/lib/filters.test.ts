import { describe, expect, it } from "vitest";
import { DEFAULT_FILTERS, PAGE_SIZE, pageCount, parseFilters, toApiQuery, toSearchParams } from "./filters";

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
