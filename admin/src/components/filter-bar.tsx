import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  CATEGORIES, CATEGORY_LABEL, SEVERITIES, SEVERITY_LABEL, STATUSES, STATUS_LABEL, type User,
} from "@/lib/domain";
import { UNASSIGNED_OPTION, hasActiveFilters, type Filters } from "@/lib/filters";
import Link from "next/link";

const selectClass =
  "h-8 rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 dark:bg-input/30";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1 text-xs font-medium text-muted-foreground">
      {label}
      {children}
    </label>
  );
}

/**
 * A plain GET form: the filters live in the URL, so a view can be shared and the page works
 * without client-side state. Submitting always returns to page one.
 */
export function FilterBar({ filters, operators }: { filters: Filters; operators: User[] }) {
  return (
    <form action="/incidents" method="get" className="flex flex-wrap items-end gap-3">
      <Field label="Search">
        <Input name="q" type="search" defaultValue={filters.q} placeholder="Title, description…" className="w-52" />
      </Field>
      <Field label="Status">
        <select name="status" defaultValue={filters.status ?? ""} className={selectClass}>
          <option value="">Any</option>
          {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
        </select>
      </Field>
      <Field label="Severity">
        <select name="severity" defaultValue={filters.severity ?? ""} className={selectClass}>
          <option value="">Any</option>
          {SEVERITIES.map((s) => <option key={s} value={s}>{SEVERITY_LABEL[s]}</option>)}
        </select>
      </Field>
      <Field label="Category">
        <select name="category" defaultValue={filters.category ?? ""} className={selectClass}>
          <option value="">Any</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_LABEL[c]}</option>)}
        </select>
      </Field>
      <Field label="Assignee">
        <select name="assignee" defaultValue={filters.unassigned ? UNASSIGNED_OPTION : (filters.assignee ?? "")} className={selectClass}>
          <option value="">Anyone</option>
          <option value={UNASSIGNED_OPTION}>Unassigned</option>
          {operators.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
        </select>
      </Field>
      <Field label="Reported from">
        <Input name="from" type="date" defaultValue={filters.from} />
      </Field>
      <Field label="to">
        <Input name="to" type="date" defaultValue={filters.to} />
      </Field>
      <Field label="Sort by">
        <select name="sort" defaultValue={filters.sort} className={selectClass}>
          <option value="createdAt">Reported</option>
          <option value="updatedAt">Last updated</option>
        </select>
      </Field>
      <Field label="Order">
        <select name="order" defaultValue={filters.order} className={selectClass}>
          <option value="desc">Newest first</option>
          <option value="asc">Oldest first</option>
        </select>
      </Field>
      <Button type="submit">Apply</Button>
      {hasActiveFilters(filters) && (
        <Button variant="ghost" nativeButton={false} render={<Link href="/incidents" />}>
          Clear
        </Button>
      )}
    </form>
  );
}
