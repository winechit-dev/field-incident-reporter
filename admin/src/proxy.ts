import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { OPERATOR_ID_COOKIE } from "@/lib/cookie-names";

/** Send anyone who has not chosen an Operator to the sign-in page. */
export function proxy(request: NextRequest) {
  if (request.cookies.has(OPERATOR_ID_COOKIE)) return NextResponse.next();
  return NextResponse.redirect(new URL("/sign-in", request.url));
}

export const config = {
  matcher: ["/((?!sign-in|_next/static|_next/image|favicon.ico).*)"],
};
