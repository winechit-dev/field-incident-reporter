import type { components } from "@/api/schema";

export type Status = components["schemas"]["Status"];
export type Severity = components["schemas"]["Severity"];
export type Category = components["schemas"]["Category"];
export type Incident = components["schemas"]["Incident"];
export type User = components["schemas"]["User"];

// Runtime lists of the contract's enums. The `Missing*` checks fail the type check if the
// contract gains a value that is not listed here, so these cannot drift silently.
export const STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED"] as const satisfies readonly Status[];
export const SEVERITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const satisfies readonly Severity[];
export const CATEGORIES = ["EQUIPMENT", "ROAD_HAZARD", "SAFETY", "OTHER"] as const satisfies readonly Category[];

type Missing<T, L extends readonly T[]> = Exclude<T, L[number]>;
export const _statusesComplete: Missing<Status, typeof STATUSES> extends never ? true : never = true;
export const _severitiesComplete: Missing<Severity, typeof SEVERITIES> extends never ? true : never = true;
export const _categoriesComplete: Missing<Category, typeof CATEGORIES> extends never ? true : never = true;

export const STATUS_LABEL: Record<Status, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
};

export const SEVERITY_LABEL: Record<Severity, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

export const CATEGORY_LABEL: Record<Category, string> = {
  EQUIPMENT: "Equipment",
  ROAD_HAZARD: "Road hazard",
  SAFETY: "Safety",
  OTHER: "Other",
};
