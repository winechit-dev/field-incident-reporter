"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { listUsers } from "@/api/incidents";
import { OPERATOR_ID_COOKIE, OPERATOR_NAME_COOKIE } from "@/lib/cookie-names";

const THIRTY_DAYS = 60 * 60 * 24 * 30;

export async function signIn(formData: FormData) {
  const id = String(formData.get("operatorId") ?? "");
  // Look the id up rather than trusting the form: only a real Operator can be chosen.
  const operator = (await listUsers()).find((u) => u.id === id && u.role === "OPERATOR");
  if (!operator) redirect("/sign-in");

  const store = await cookies();
  const options = { httpOnly: true, sameSite: "lax", path: "/", maxAge: THIRTY_DAYS } as const;
  store.set(OPERATOR_ID_COOKIE, operator.id, options);
  store.set(OPERATOR_NAME_COOKIE, operator.name, options);
  redirect("/incidents");
}

export async function signOut() {
  const store = await cookies();
  store.delete(OPERATOR_ID_COOKIE);
  store.delete(OPERATOR_NAME_COOKIE);
  redirect("/sign-in");
}
