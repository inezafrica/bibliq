import "server-only";
import { createClient as createSupabaseClient } from "@supabase/supabase-js";

/**
 * Service-role client. Bypasses RLS entirely -- use ONLY for actions a
 * logged-in admin is already authorized for at the application layer
 * (e.g. publishing, inviting a user), never to answer an ordinary read.
 * The `server-only` import makes an accidental client-side import a build
 * error instead of a leaked secret.
 */
export function createAdminClient() {
  return createSupabaseClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!,
    { auth: { autoRefreshToken: false, persistSession: false } }
  );
}
