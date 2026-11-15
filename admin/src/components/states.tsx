import { CloudOff, Inbox } from "lucide-react";
import { RetryButton } from "@/components/retry-button";
import { cn } from "@/lib/utils";

function Panel({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      className={cn("flex flex-col items-center gap-3 rounded-xl border border-dashed bg-card px-6 py-12 text-center", className)}
      {...props}
    />
  );
}

/** Nothing to show. `title` says why; `children` hold the way out, if there is one. */
export function EmptyState({ title, description, children }: { title: string; description?: string; children?: React.ReactNode }) {
  return (
    <Panel>
      <Inbox aria-hidden="true" className="size-8 text-muted-foreground" />
      <div className="flex flex-col gap-1">
        <p className="font-medium">{title}</p>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {children}
    </Panel>
  );
}

/** The API could not be reached. Says what happened, what to do, and offers a retry. */
export function ApiUnavailable({ message }: { message: string }) {
  return (
    <Panel role="alert" className="border-destructive/40">
      <CloudOff aria-hidden="true" className="size-8 text-destructive" />
      <div className="flex flex-col gap-1">
        <p className="font-medium">{message}</p>
        <p className="text-sm text-muted-foreground">Start the backend, then try again.</p>
      </div>
      <RetryButton />
    </Panel>
  );
}
