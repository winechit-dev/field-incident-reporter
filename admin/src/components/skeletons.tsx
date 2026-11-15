import { Skeleton } from "@/components/ui/skeleton";

/** The loading shapes mirror the loaded layout, so the page does not jump when data arrives. */
export function IncidentsSkeleton() {
  return (
    <div role="status" aria-label="Loading incidents" className="flex flex-col gap-5">
      <Skeleton className="h-28 rounded-xl" />
      <div className="overflow-hidden rounded-xl border bg-card">
        {Array.from({ length: 6 }, (_, i) => (
          <div key={i} className="flex items-center gap-4 border-b px-4 py-3 last:border-0">
            <Skeleton className="h-4 flex-1" />
            <Skeleton className="h-5 w-20 rounded-full" />
            <Skeleton className="h-5 w-16 rounded-full" />
            <Skeleton className="hidden h-4 w-28 md:block" />
            <Skeleton className="h-4 w-32" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function IncidentSkeleton() {
  return (
    <div role="status" aria-label="Loading incident" className="flex flex-col gap-5">
      <div className="flex flex-col gap-2">
        <Skeleton className="h-8 w-80 max-w-full" />
        <Skeleton className="h-5 w-56" />
      </div>
      <div className="grid gap-5 lg:grid-cols-3">
        <div className="flex flex-col gap-5 lg:col-span-2">
          <Skeleton className="h-28 rounded-xl" />
          <Skeleton className="h-24 rounded-xl" />
          <Skeleton className="h-40 rounded-xl" />
        </div>
        <Skeleton className="h-80 rounded-xl" />
      </div>
    </div>
  );
}

export function OperatorsSkeleton() {
  return (
    <div role="status" aria-label="Loading operators" className="flex flex-col gap-3">
      <Skeleton className="h-16 rounded-xl" />
      <Skeleton className="h-16 rounded-xl" />
    </div>
  );
}
