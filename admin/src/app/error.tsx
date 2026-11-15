"use client"; // Error boundaries must be Client Components

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

/** An unexpected failure. Expected ones (API down, Incident gone) are handled where they happen. */
export default function Error({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main id="main" role="alert" className="flex flex-col items-start gap-3 pt-8">
      <h1 className="type-headline">Something went wrong</h1>
      <p className="text-sm text-muted-foreground">The page could not be shown. Nothing was changed.</p>
      <Button onClick={() => retry()}>Try again</Button>
    </main>
  );
}
