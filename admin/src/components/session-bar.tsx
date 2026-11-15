import { signOut } from "@/app/sign-in/actions";
import { Avatar } from "@/components/avatar";
import { Button } from "@/components/ui/button";
import { getOperator } from "@/lib/session";

export async function SessionBar() {
  const operator = await getOperator();
  if (!operator) return null;
  return (
    <form action={signOut} className="flex items-center gap-3 text-sm">
      <span className="flex items-center gap-2">
        <Avatar name={operator.name} className="size-7 text-xs" />
        <span className="sr-only">Signed in as </span>
        <span className="font-medium max-sm:sr-only">{operator.name}</span>
      </span>
      <Button type="submit" variant="outline">Switch operator</Button>
    </form>
  );
}
