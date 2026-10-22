import { requireAdmin } from "@/lib/auth";
import { AdminSidebar } from "@/components/admin-sidebar";

// Every admin page reads the signed-in user's session cookie, so none of
// this subtree can be statically prerendered -- opt the whole route group
// out rather than repeating this on every page.
//
// With cacheComponents enabled:
//   - `dynamic`/`revalidate` are removed; use `ensureStatic = false` to
//     preserve normal per-request rendering and prevent static prerendering
//     (which would otherwise call Date.now() inside @supabase/ssr during
//     getUser()).
//   - `instant = false` allows the route to block on navigation rather than
//     requiring an instant (prefetched) UI.
export const ensureStatic = false;
export const instant = false;

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const admin = await requireAdmin();

  return (
    <div className="flex min-h-screen">
      <AdminSidebar name={admin.name} role={admin.role} />
      <main className="flex-1 overflow-y-auto bg-muted/30">
        <div className="mx-auto max-w-7xl p-6 lg:p-8">{children}</div>
      </main>
    </div>
  );
}
