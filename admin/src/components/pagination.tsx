import Link from "next/link";
import { Button } from "@/components/ui/button";
import { PAGE_SIZE, pageCount, toSearchParams, type Filters } from "@/lib/filters";

function hrefFor(filters: Filters, page: number) {
  const query = toSearchParams({ ...filters, page }).toString();
  return query ? `/incidents?${query}` : "/incidents";
}

export function Pagination({ filters, total }: { filters: Filters; total: number }) {
  const pages = pageCount(total);
  const first = total === 0 ? 0 : (filters.page - 1) * PAGE_SIZE + 1;
  const last = Math.min(filters.page * PAGE_SIZE, total);

  return (
    <div className="flex items-center justify-between text-sm text-muted-foreground">
      <span>
        {total === 0 ? "No incidents" : `${first}–${last} of ${total} incidents`}
      </span>
      <div className="flex items-center gap-2">
        <span>Page {Math.min(filters.page, pages)} of {pages}</span>
        {filters.page > 1 ? (
          <Button variant="outline" nativeButton={false} render={<Link href={hrefFor(filters, filters.page - 1)} />}>Previous</Button>
        ) : (
          <Button variant="outline" disabled>Previous</Button>
        )}
        {filters.page < pages ? (
          <Button variant="outline" nativeButton={false} render={<Link href={hrefFor(filters, filters.page + 1)} />}>Next</Button>
        ) : (
          <Button variant="outline" disabled>Next</Button>
        )}
      </div>
    </div>
  );
}
