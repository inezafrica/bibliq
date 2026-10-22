import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

const TARGET_BUFFER_LEVELS = 30;

function daysUntil(dateStr: string): number {
  const target = new Date(`${dateStr}T00:00:00Z`);
  const now = new Date();
  const diffMs = target.getTime() - Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  return Math.round(diffMs / (1000 * 60 * 60 * 24));
}

export default async function DashboardPage() {
  const supabase = await createClient();

  const [
    { count: approvedCount },
    { data: usedQuestionRows },
    { count: readyLevelsCount },
    { data: nextDropSetting },
    { count: missingBible },
    { count: missingEn },
    { count: missingFr },
    { count: missingEs },
    { data: auditRows },
  ] = await Promise.all([
    supabase.from("questions").select("id", { count: "exact", head: true }).eq("status", "approved"),
    supabase.from("level_questions").select("question_id"),
    supabase.from("levels").select("number", { count: "exact", head: true }).eq("status", "ready"),
    supabase.from("settings").select("value").eq("key", "next_drop_date").maybeSingle(),
    supabase
      .from("questions")
      .select("id", { count: "exact", head: true })
      .in("status", ["draft", "in_review"])
      .eq("check_bible", false),
    supabase
      .from("questions")
      .select("id", { count: "exact", head: true })
      .in("status", ["draft", "in_review"])
      .eq("check_en", false),
    supabase
      .from("questions")
      .select("id", { count: "exact", head: true })
      .in("status", ["draft", "in_review"])
      .eq("check_fr", false),
    supabase
      .from("questions")
      .select("id", { count: "exact", head: true })
      .in("status", ["draft", "in_review"])
      .eq("check_es", false),
    supabase
      .from("audit_log")
      .select("id, action, table_name, row_id, created_at, admin_users(name)")
      .order("created_at", { ascending: false })
      .limit(10),
  ]);

  const usedQuestionIds = new Set((usedQuestionRows ?? []).map((r) => r.question_id as string));
  const { data: approvedUnassignedRows } = await supabase
    .from("questions")
    .select("id")
    .eq("status", "approved");
  const unassignedApprovedCount = (approvedUnassignedRows ?? []).filter(
    (q) => !usedQuestionIds.has(q.id as string)
  ).length;

  const nextDropDate = (nextDropSetting?.value as string | undefined) ?? null;
  const daysLeft = nextDropDate ? daysUntil(nextDropDate) : null;
  const bufferOk = (readyLevelsCount ?? 0) >= TARGET_BUFFER_LEVELS;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Dashboard</h1>
        <p className="text-sm text-muted-foreground">Where BibliQ content stands right now.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Next drop</CardDescription>
            <CardTitle className="font-heading text-3xl text-bibliq-royal">
              {daysLeft === null ? "--" : daysLeft >= 0 ? `${daysLeft}d` : `${Math.abs(daysLeft)}d late`}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{nextDropDate ?? "Not set"}</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Ready, unpublished levels</CardDescription>
            <CardTitle className="font-heading text-3xl text-bibliq-ink">{readyLevelsCount ?? 0}</CardTitle>
          </CardHeader>
          <CardContent>
            <Badge variant={bufferOk ? "secondary" : "destructive"}>
              {bufferOk ? "2+ weeks of buffer" : `Target: ${TARGET_BUFFER_LEVELS} levels`}
            </Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Approved, not yet in a level</CardDescription>
            <CardTitle className="font-heading text-3xl text-bibliq-ink">{unassignedApprovedCount}</CardTitle>
          </CardHeader>
          <CardContent>
            <Link href="/levels" className="text-sm text-bibliq-royal underline underline-offset-2">
              Assign to a level
            </Link>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Total approved questions</CardDescription>
            <CardTitle className="font-heading text-3xl text-bibliq-ink">{approvedCount ?? 0}</CardTitle>
          </CardHeader>
          <CardContent>
            <Link href="/questions?status=approved" className="text-sm text-bibliq-royal underline underline-offset-2">
              View all
            </Link>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">Review queue</CardTitle>
          <CardDescription>Draft / in-review questions still missing each check.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {[
              { label: "Bible", count: missingBible ?? 0 },
              { label: "English", count: missingEn ?? 0 },
              { label: "French", count: missingFr ?? 0 },
              { label: "Spanish", count: missingEs ?? 0 },
            ].map((item) => (
              <Link
                key={item.label}
                href={`/questions?status=in_review`}
                className="rounded-lg border p-4 text-center transition-colors hover:bg-muted"
              >
                <p className="font-heading text-2xl font-bold text-bibliq-ink">{item.count}</p>
                <p className="text-xs text-muted-foreground">{item.label} unchecked</p>
              </Link>
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="font-heading text-lg">Recent activity</CardTitle>
          <CardDescription>Last 10 changes across the content pipeline.</CardDescription>
        </CardHeader>
        <CardContent>
          {!auditRows || auditRows.length === 0 ? (
            <p className="text-sm text-muted-foreground">No activity yet.</p>
          ) : (
            <ul className="divide-y">
              {auditRows.map((row) => {
                const actor = (row.admin_users as unknown as { name?: string } | null)?.name ?? "Someone";
                return (
                  <li key={row.id as string} className="flex items-center justify-between py-3 text-sm">
                    <span>
                      <span className="font-medium">{actor}</span>{" "}
                      <span className="text-muted-foreground">
                        {String(row.action).toLowerCase()}d {row.table_name} {row.row_id}
                      </span>
                    </span>
                    <span className="text-xs text-muted-foreground">
                      {new Date(row.created_at as string).toLocaleString()}
                    </span>
                  </li>
                );
              })}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
