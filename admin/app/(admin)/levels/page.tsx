import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { LevelStatusBadge } from "@/components/status-badge";
import { NewLevelDialog } from "./new-level-dialog";
import { cn } from "@/lib/utils";

export default async function LevelsPage() {
  const supabase = await createClient();

  const [{ data: worlds }, { data: levels }, { data: levelQuestions }] = await Promise.all([
    supabase.from("worlds").select("id, name, books, color, sort").order("sort"),
    supabase.from("levels").select("number, world_id, is_boss, star_thresholds, status").order("number"),
    supabase.from("level_questions").select("level_number, question_id, questions(type)"),
  ]);

  const questionCountByLevel = new Map<number, number>();
  const typeCountByLevel = new Map<number, Record<string, number>>();
  for (const lq of levelQuestions ?? []) {
    const levelNumber = lq.level_number as number;
    questionCountByLevel.set(levelNumber, (questionCountByLevel.get(levelNumber) ?? 0) + 1);
    const type = (lq.questions as unknown as { type?: string } | null)?.type ?? "?";
    const counts = typeCountByLevel.get(levelNumber) ?? {};
    counts[type] = (counts[type] ?? 0) + 1;
    typeCountByLevel.set(levelNumber, counts);
  }

  const levelsByWorld = new Map<number, typeof levels>();
  for (const level of levels ?? []) {
    const list = levelsByWorld.get(level.world_id as number) ?? [];
    list.push(level);
    levelsByWorld.set(level.world_id as number, list);
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Levels</h1>
          <p className="text-sm text-muted-foreground">{(levels ?? []).length} level(s) across all worlds</p>
        </div>
        <NewLevelDialog worlds={(worlds ?? []).map((w) => ({ id: w.id as number, books: w.books as string }))} />
      </div>

      {(worlds ?? []).map((world) => {
        const worldLevels = levelsByWorld.get(world.id as number) ?? [];
        const name = (world.name as Record<string, string>)?.en ?? `World ${world.id}`;
        return (
          <section key={world.id as number} className="space-y-3">
            <div className="flex items-center gap-2">
              <span
                className="h-3 w-3 rounded-full"
                style={{ backgroundColor: world.color as string }}
              />
              <h2 className="font-heading text-lg font-semibold">
                World {world.id as number} · {name}
              </h2>
              <span className="text-sm text-muted-foreground">{world.books as string}</span>
            </div>

            {worldLevels.length === 0 ? (
              <p className="pl-5 text-sm text-muted-foreground">No levels yet.</p>
            ) : (
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {worldLevels.map((level) => {
                  const expected = level.is_boss ? 15 : 10;
                  const count = questionCountByLevel.get(level.number as number) ?? 0;
                  const mix = typeCountByLevel.get(level.number as number) ?? {};
                  return (
                    <Link
                      key={level.number as number}
                      href={`/levels/${level.number}`}
                      className={cn(
                        "flex flex-col gap-1 rounded-lg border p-3 transition-colors hover:bg-muted",
                        level.is_boss && "border-bibliq-gold"
                      )}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-heading font-bold">
                          Level {level.number as number} {level.is_boss ? "👑" : ""}
                        </span>
                        <LevelStatusBadge status={level.status as "draft" | "ready" | "published"} />
                      </div>
                      <span className="text-xs text-muted-foreground">
                        {count}/{expected} questions ·{" "}
                        {Object.entries(mix)
                          .map(([t, c]) => `${c} ${t}`)
                          .join(", ") || "empty"}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        Stars: {(level.star_thresholds as number[]).join(" / ")}
                      </span>
                    </Link>
                  );
                })}
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}
