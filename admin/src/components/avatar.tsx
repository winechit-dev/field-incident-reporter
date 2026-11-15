import { initials } from "@/lib/overview";
import { cn } from "@/lib/utils";

/** A person, as their initials on a tinted disc. Decorative: the name is always shown beside it. */
export function Avatar({ name, className }: { name: string; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn("flex size-6 shrink-0 items-center justify-center rounded-full bg-accent text-[0.625rem] font-semibold text-accent-foreground", className)}
    >
      {initials(name)}
    </span>
  );
}
