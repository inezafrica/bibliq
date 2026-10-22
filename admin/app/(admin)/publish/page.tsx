import { requireRole } from "@/lib/auth";
import { createClient } from "@/lib/supabase/server";
import { PublishForm } from "@/components/publish/publish-form";

export default async function PublishPage() {
  await requireRole(["admin"]);
  const supabase = await createClient();

  const [{ data: levels }, { data: levelQuestions }, { data: packs }, { data: nextDropSetting }] =
    await Promise.all([
      supabase.from("levels").select("number, is_boss").eq("status", "ready").order("number"),
      supabase.from("level_questions").select("level_number"),
      supabase.from("packs").select("id, version, level_from, level_to").order("id", { ascending: false }),
      supabase.from("settings").select("value").eq("key", "next_drop_date").maybeSingle(),
    ]);

  const countByLevel = new Map<number, number>();
  for (const lq of levelQuestions ?? []) {
    const n = lq.level_number as number;
    countByLevel.set(n, (countByLevel.get(n) ?? 0) + 1);
  }

  // Latest version per pack id.
  const latestByPackId = new Map<number, { id: number; version: number; levelFrom: number; levelTo: number }>();
  for (const p of packs ?? []) {
    const id = p.id as number;
    const existing = latestByPackId.get(id);
    if (!existing || (p.version as number) > existing.version) {
      latestByPackId.set(id, { id, version: p.version as number, levelFrom: p.level_from as number, levelTo: p.level_to as number });
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Publish</h1>
        <p className="text-sm text-muted-foreground">Build a pack, review the dry run, then ship it to the CDN.</p>
      </div>

      <PublishForm
        readyLevels={(levels ?? []).map((l) => ({
          number: l.number as number,
          isBoss: l.is_boss as boolean,
          questionCount: countByLevel.get(l.number as number) ?? 0,
        }))}
        existingPacks={Array.from(latestByPackId.values()).sort((a, b) => b.id - a.id)}
        defaultNextDropDate={(nextDropSetting?.value as string | undefined) ?? new Date().toISOString().slice(0, 10)}
      />
    </div>
  );
}
