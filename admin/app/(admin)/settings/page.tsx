import { requireRole } from "@/lib/auth";
import { createClient } from "@/lib/supabase/server";
import { createAdminClient } from "@/lib/supabase/admin";
import { UsersPanel } from "@/components/settings/users-panel";
import { GeneralSettings } from "@/components/settings/general-settings";

export default async function SettingsPage() {
  const admin = await requireRole(["admin"]);
  const supabase = await createClient();

  const [{ data: adminUsers }, { data: nextDropSetting }, { data: minVersionSetting }] = await Promise.all([
    supabase.from("admin_users").select("user_id, name, role").order("name"),
    supabase.from("settings").select("value").eq("key", "next_drop_date").maybeSingle(),
    supabase.from("settings").select("value").eq("key", "min_app_version").maybeSingle(),
  ]);

  // Emails live in auth.users, only reachable with the service-role client.
  const adminClient = createAdminClient();
  const { data: authUsers } = await adminClient.auth.admin.listUsers();
  const emailById = new Map((authUsers?.users ?? []).map((u) => [u.id, u.email ?? null]));

  const users = (adminUsers ?? []).map((u) => ({
    userId: u.user_id as string,
    name: u.name as string,
    role: u.role as "admin" | "editor" | "reviewer",
    email: emailById.get(u.user_id as string) ?? null,
  }));

  return (
    <div className="space-y-10">
      <div>
        <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Settings</h1>
      </div>

      <UsersPanel users={users} currentUserId={admin.userId} />

      <div>
        <h2 className="mb-4 font-heading text-lg font-semibold">General</h2>
        <GeneralSettings
          nextDropDate={(nextDropSetting?.value as string | undefined) ?? ""}
          minAppVersion={Number((minVersionSetting?.value as number | undefined) ?? 1)}
        />
      </div>
    </div>
  );
}
