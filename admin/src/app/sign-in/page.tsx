import type { Metadata } from "next";
import { Suspense } from "react";
import { connection } from "next/server";
import { ChevronRight } from "lucide-react";
import { signIn } from "./actions";
import { listUsers } from "@/api/incidents";
import { ApiUnavailableError } from "@/api/client";
import { OperatorsSkeleton } from "@/components/skeletons";
import { ApiUnavailable, EmptyState } from "@/components/states";

export const metadata: Metadata = { title: "Choose operator" };

export default function SignInPage() {
  return (
    <main id="main" className="mx-auto flex max-w-md flex-col gap-6 pt-12">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold">Who are you?</h1>
        <p className="text-sm text-muted-foreground">Choose your name to triage incidents. There is no password.</p>
      </div>
      <Suspense fallback={<OperatorsSkeleton />}>
        <Operators />
      </Suspense>
    </main>
  );
}

const initials = (name: string) => name.split(/\s+/).map((part) => part[0]).join("").slice(0, 2).toUpperCase();

async function Operators() {
  await connection(); // the list is live data, never prerendered
  let operators;
  try {
    operators = (await listUsers()).filter((u) => u.role === "OPERATOR");
  } catch (error) {
    if (error instanceof ApiUnavailableError) return <ApiUnavailable message={error.message} />;
    throw error;
  }
  if (operators.length === 0) {
    return <EmptyState title="No operators to choose from." description="Add an Operator to the backend's seed data, then reload." />;
  }
  return (
    <ul className="flex flex-col gap-3">
      {operators.map((operator) => (
        <li key={operator.id}>
          {/* The whole card is the button: one large target instead of a small one inside it. */}
          <form action={signIn}>
            <input type="hidden" name="operatorId" value={operator.id} />
            <button
              type="submit"
              className="flex w-full items-center gap-3 rounded-xl border bg-card p-4 text-left transition-colors outline-none hover:border-primary/50 hover:bg-accent focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              <span aria-hidden="true" className="flex size-9 items-center justify-center rounded-full bg-accent text-sm font-medium text-accent-foreground">
                {initials(operator.name)}
              </span>
              <span className="flex-1 font-medium">
                <span className="sr-only">Continue as </span>
                {operator.name}
              </span>
              <ChevronRight aria-hidden="true" className="size-4 text-muted-foreground" />
            </button>
          </form>
        </li>
      ))}
    </ul>
  );
}
