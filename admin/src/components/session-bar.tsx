import { signOut } from "@/app/sign-in/actions";
import { Button } from "@/components/ui/button";
import { getOperator } from "@/lib/session";

export async function SessionBar() {
  const operator = await getOperator();
  if (!operator) return null;
  return (
    <form action={signOut} className="flex items-center gap-3 text-sm">
      <span className="text-muted-foreground">
        <span className="max-sm:sr-only">Signed in as </span>
        <span className="font-medium text-foreground">{operator.name}</span>
      </span>
      <Button type="submit" variant="outline">Switch operator</Button>
    </form>
  );
}
