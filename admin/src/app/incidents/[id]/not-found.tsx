import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function IncidentNotFound() {
  return (
    <main id="main" className="flex flex-col items-start gap-3 pt-8">
      <h1 className="type-headline">Incident not found</h1>
      <p className="text-sm text-muted-foreground">It may have been deleted by the technician who reported it.</p>
      <Button nativeButton={false} render={<Link href="/incidents" />}>Back to incidents</Button>
    </main>
  );
}
