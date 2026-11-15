import Link from "next/link";
import { STATUS_DOT } from "@/components/status-badges";
import type { OverviewTile } from "@/lib/overview";
import { cn } from "@/lib/utils";

/**
 * The headline counts above the list. Each tile is also a filter: one click narrows the list
 * to it, a second click clears it. The counts cover every Incident, whatever the list is
 * currently filtered to, so they stay a stable picture of the queue.
 */
export function Overview({ tiles, counts }: { tiles: OverviewTile[]; counts: number[] }) {
  return (
    <nav aria-label="Incidents by status">
      <ul className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {tiles.map((tile, index) => (
          <li key={tile.key}>
            <Link
              href={tile.href}
              aria-current={tile.active ? "true" : undefined}
              className={cn(
                "flex flex-col gap-1 rounded-xl border bg-card p-4 transition-colors outline-none hover:border-primary/50 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50",
                tile.active && "border-primary bg-accent",
              )}
            >
              <span className="flex items-center gap-2 text-sm text-muted-foreground">
                <span
                  aria-hidden="true"
                  className={cn("size-2 rounded-full", tile.narrowing.status ? STATUS_DOT[tile.narrowing.status] : "bg-muted-foreground")}
                />
                {tile.label}
                {tile.active && <span className="sr-only"> (showing now, select to clear)</span>}
              </span>
              <span className="type-display">{counts[index].toLocaleString("en")}</span>
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
