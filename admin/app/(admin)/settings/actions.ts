"use server";

import { revalidatePath } from "next/cache";
import { requireRole } from "@/lib/auth";
import { createClient } from "@/lib/supabase/server";
import { createAdminClient } from "@/lib/supabase/admin";
import { AdminRole } from "@/lib/content-types";

export async function inviteUser(email: string, name: string, role: AdminRole) {
  await requireRole(["admin"]);
  const adminClient = createAdminClient();

  const { data, error } = await adminClient.auth.admin.inviteUserByEmail(email);
  if (error || !data.user) {
    return { error: error?.message ?? "Could not send invite" };
  }

  const supabase = await createClient();
  const { error: insertError } = await supabase
    .from("admin_users")
    .insert({ user_id: data.user.id, name, role });
  if (insertError) return { error: insertError.message };

  revalidatePath("/settings");
  return { error: null };
}

export async function updateUserRole(userId: string, role: AdminRole) {
  await requireRole(["admin"]);
  const supabase = await createClient();
  const { error } = await supabase.from("admin_users").update({ role }).eq("user_id", userId);
  revalidatePath("/settings");
  return { error: error?.message ?? null };
}

export async function removeUser(userId: string) {
  const admin = await requireRole(["admin"]);
  if (admin.userId === userId) {
    return { error: "You can't remove your own admin access" };
  }
  const supabase = await createClient();
  const { error } = await supabase.from("admin_users").delete().eq("user_id", userId);
  revalidatePath("/settings");
  return { error: error?.message ?? null };
}

export async function updateSetting(key: "next_drop_date" | "min_app_version", value: string | number) {
  await requireRole(["admin"]);
  const supabase = await createClient();
  const { error } = await supabase.from("settings").update({ value }).eq("key", key);
  revalidatePath("/settings");
  revalidatePath("/dashboard");
  return { error: error?.message ?? null };
}
