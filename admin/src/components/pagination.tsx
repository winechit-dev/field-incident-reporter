import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PAGE_SIZE, pageCount, toSearchParams, type Filters } from "@/lib/filters";

function hrefFor(filters: Filters, page: number) {
  const query = toSearchParams({ ...filters, page }).toString();
  return query ? `/incidents?${query}` : "/incidents";
}

/** The range shown and, only when there is more than one page, the way between pages. */
export function Pagination({ filters, total }: { filters: Filters; total: number }) {
  if (total === 0) return null;
  const pages = pageCount(total);
  const page = Math.min(filters.page, pages);
  const first = (page - 1) * PAGE_SIZE + 1;
  const last = Math.min(page * PAGE_SIZE, total);

  return (
    <nav aria-label="Pagination" className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
      <span>
        {first}–{last} of {total} {total === 1 ? "incident" : "incidents"}
      </span>
      {pages > 1 && (
        <div className="flex items-center gap-2">
          <span>Page {page} of {pages}</span>
          {page > 1 ? (
            <Button variant="outline" nativeButton={false} render={<Link href={hrefFor(filters, page - 1)} />}>
              <ChevronLeft aria-hidden="true" /> Previous
            </Button>
          ) : (
            <Button variant="outline" disabled><ChevronLeft aria-hidden="true" /> Previous</Button>
          )}
          {page < pages ? (
            <Button variant="outline" nativeButton={false} render={<Link href={hrefFor(filters, page + 1)} />}>
              Next <ChevronRight aria-hidden="true" />
            </Button>
          ) : (
            <Button variant="outline" disabled>Next <ChevronRight aria-hidden="true" /></Button>
          )}
        </div>
      )}
    </nav>
  );
}
