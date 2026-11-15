import { Suspense } from "react";
import { signIn } from "./actions";
import { listUsers } from "@/api/incidents";
import { ApiUnavailableError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

export default function SignInPage() {
  return (
    <main className="mx-auto flex max-w-md flex-col gap-6 pt-12">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold">Who are you?</h1>
        <p className="text-sm text-muted-foreground">Choose your name to triage incidents. There is no password.</p>
      </div>
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading operators…</p>}>
        <Operators />
      </Suspense>
    </main>
  );
}

async function Operators() {
  let operators;
  try {
    operators = (await listUsers()).filter((u) => u.role === "OPERATOR");
  } catch (error) {
    if (error instanceof ApiUnavailableError) {
      return <p role="alert" className="text-sm text-destructive">{error.message}. Start the backend and reload.</p>;
    }
    throw error;
  }
  return (
    <div className="flex flex-col gap-3">
      {operators.map((operator) => (
        <Card key={operator.id}>
          <CardContent>
            <form action={signIn} className="flex items-center justify-between">
              <input type="hidden" name="operatorId" value={operator.id} />
              <span className="font-medium">{operator.name}</span>
              <Button type="submit">Continue</Button>
            </form>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
