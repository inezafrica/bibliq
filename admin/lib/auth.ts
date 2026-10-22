import "server-only";
import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { AdminRole } from "@/lib/content-types";

export interface CurrentAdmin {
  userId: string;
  email: string | null;
  name: string;
  role: AdminRole;
}

/**
 * Reads the signed-in admin's profile. Middleware already redirects anyone
 * who isn't signed in or isn't an admin_users row, so the redirects here are
 * a defensive second layer, not the primary gate.
 */
export async function requireAdmin(): Promise<CurrentAdmin> {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) redirect("/login");

  const { data: adminRow } = await supabase
    .from("admin_users")
    .select("name, role")
    .eq("user_id", user.id)
    .maybeSingle();
  if (!adminRow) redirect("/no-access");

  return {
    userId: user.id,
    email: user.email ?? null,
    name: adminRow.name as string,
    role: adminRow.role as AdminRole,
  };
}

/** For admin-only pages (/publish, /import, /settings): bounce non-matching roles back to the dashboard. */
export async function requireRole(roles: AdminRole[]): Promise<CurrentAdmin> {
  const admin = await requireAdmin();
  if (!roles.includes(admin.role)) redirect("/dashboard");
  return admin;
}
