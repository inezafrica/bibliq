import { NextResponse, type NextRequest } from "next/server";
import { updateSession } from "@/lib/supabase/middleware";

// Fully public -- no session required. /auth/* handles the email-link
// callback (invite / password recovery), which is how a session gets
// created in the first place, so it can't require one.
const PUBLIC_PATHS = ["/login", "/no-access", "/auth"];

// Requires a session, but deliberately NOT admin_users membership -- a
// freshly invited user has a session (from the email link) before anyone
// has necessarily confirmed their role, and needs to set a password either way.
const SESSION_ONLY_PATHS = ["/update-password"];

export async function middleware(request: NextRequest) {
  const { response, userId, supabase } = await updateSession(request);
  const { pathname } = request.nextUrl;

  const isPublic = PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));
  if (isPublic) {
    return response;
  }

  if (!userId) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", pathname);
    return NextResponse.redirect(loginUrl);
  }

  const isSessionOnly = SESSION_ONLY_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));
  if (isSessionOnly) {
    return response;
  }

  // RLS on admin_users only lets a signed-in user see their own row, so an
  // empty result here means "authenticated, but not an admin" -- not a
  // query failure.
  const { data: adminRow } = await supabase
    .from("admin_users")
    .select("role")
    .eq("user_id", userId)
    .maybeSingle();

  if (!adminRow) {
    return NextResponse.redirect(new URL("/no-access", request.url));
  }

  return response;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)"],
};
