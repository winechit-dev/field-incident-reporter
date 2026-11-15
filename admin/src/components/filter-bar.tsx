import Form from "next/form";
import Link from "next/link";
import { Search } from "lucide-react";
import { AutoSubmit } from "@/components/auto-submit";
import { Field } from "@/components/field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { NativeSelect } from "@/components/ui/native-select";
import {
  CATEGORIES, CATEGORY_LABEL, SEVERITIES, SEVERITY_LABEL, STATUSES, STATUS_LABEL, type User,
} from "@/lib/domain";
import { UNASSIGNED_OPTION, activeFilterCount, type Filters } from "@/lib/filters";

/**
 * A GET form: the filters live in the URL, so a view can be shared and the page works without
 * client-side state. Submitting always returns to page one. The top row narrows the list; the
 * bottom row holds the date range, the sorting and the buttons.
 */
export function FilterBar({ filters, operators }: { filters: Filters; operators: User[] }) {
  const active = activeFilterCount(filters);
  return (
    <Form action="/incidents" role="search" aria-label="Filter incidents" className="flex flex-col gap-4 rounded-xl border bg-card p-4">
      <AutoSubmit>
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Search" className="min-w-56 flex-1">
            <span className="relative">
              <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input name="q" type="search" defaultValue={filters.q} placeholder="Title or description" className="bg-card pl-8" />
            </span>
          </Field>
          <Field label="Status" className="w-36">
            <NativeSelect name="status" defaultValue={filters.status ?? ""}>
              <option value="">Any status</option>
              {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
            </NativeSelect>
          </Field>
          <Field label="Severity" className="w-36">
            <NativeSelect name="severity" defaultValue={filters.severity ?? ""}>
              <option value="">Any severity</option>
              {SEVERITIES.map((s) => <option key={s} value={s}>{SEVERITY_LABEL[s]}</option>)}
            </NativeSelect>
          </Field>
          <Field label="Category" className="w-36">
            <NativeSelect name="category" defaultValue={filters.category ?? ""}>
              <option value="">Any category</option>
              {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_LABEL[c]}</option>)}
            </NativeSelect>
          </Field>
          <Field label="Assignee" className="w-40">
            <NativeSelect name="assignee" defaultValue={filters.unassigned ? UNASSIGNED_OPTION : (filters.assignee ?? "")}>
              <option value="">Anyone</option>
              <option value={UNASSIGNED_OPTION}>Unassigned</option>
              {operators.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </NativeSelect>
          </Field>
        </div>

        <div className="flex flex-wrap items-end gap-3 border-t pt-4">
          <Field label="Reported from">
            <Input name="from" type="date" defaultValue={filters.from} max={filters.to} className="bg-card" />
          </Field>
          <Field label="Reported to">
            <Input name="to" type="date" defaultValue={filters.to} min={filters.from} className="bg-card" />
          </Field>
          <Field label="Sort by" className="w-36">
            <NativeSelect name="sort" defaultValue={filters.sort}>
              <option value="createdAt">Reported</option>
              <option value="updatedAt">Last updated</option>
            </NativeSelect>
          </Field>
          <Field label="Order" className="w-36">
            <NativeSelect name="order" defaultValue={filters.order}>
              <option value="desc">Newest first</option>
              <option value="asc">Oldest first</option>
            </NativeSelect>
          </Field>
          <div className="ml-auto flex items-center gap-2">
            {active > 0 && (
              <Button variant="ghost" nativeButton={false} render={<Link href="/incidents" />}>
                Clear {active === 1 ? "1 filter" : `${active} filters`}
              </Button>
            )}
            <Button type="submit">Apply</Button>
          </div>
        </div>
      </AutoSubmit>
    </Form>
  );
}
