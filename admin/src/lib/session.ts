import { cookies } from "next/headers";
import { OPERATOR_ID_COOKIE, OPERATOR_NAME_COOKIE } from "./cookie-names";

export interface Operator {
  id: string;
  name: string;
}

/**
 * The Operator chosen on the sign-in page. There is no login: this is a convenience that names
 * the Current User, sent to the API as `X-User-Id`.
 */
export async function getOperator(): Promise<Operator | null> {
  const store = await cookies();
  const id = store.get(OPERATOR_ID_COOKIE)?.value;
  const name = store.get(OPERATOR_NAME_COOKIE)?.value;
  return id && name ? { id, name } : null;
}
