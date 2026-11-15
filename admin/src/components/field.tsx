import { cn } from "@/lib/utils";

/**
 * A labelled control. The label wraps the control, so clicking it focuses the control and no
 * id is needed. `hint` is extra help; point the control at it with `aria-describedby`.
 */
export function Field({
  label,
  hint,
  hintId,
  className,
  children,
}: {
  label: string;
  hint?: string;
  hintId?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <label className={cn("flex flex-col gap-1 text-xs font-medium text-muted-foreground", className)}>
      {label}
      {children}
      {hint && <span id={hintId} className="font-normal">{hint}</span>}
    </label>
  );
}
