import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { LevelEditor, PoolQuestion } from "@/components/level-editor/level-editor";

interface LevelPageProps {
  params: Promise<{ number: string }>;
}

export default async function LevelPage({ params }: LevelPageProps) {
  const { number } = await params;
  const levelNumber = Number(number);
  if (!Number.isInteger(levelNumber)) notFound();

  const supabase = await createClient();

  const [{ data: level }, { data: slotRows }, { data: approvedRows }, { data: allAssignments }] = await Promise.all([
    supabase.from("levels").select("number, world_id, is_boss, star_thresholds, status").eq("number", levelNumber).maybeSingle(),
    supabase
      .from("level_questions")
      .select("position, questions(id, type, status, correct_index, book, text)")
      .eq("level_number", levelNumber)
      .order("position"),
    supabase
      .from("questions")
      .select("id, type, status, correct_index, book, text")
      .eq("status", "approved"),
    supabase.from("level_questions").select("level_number, question_id").neq("level_number", levelNumber),
  ]);

  if (!level) notFound();

  function toPoolQuestion(row: {
    id: unknown;
    type: unknown;
    status: unknown;
    correct_index: unknown;
    book: unknown;
    text: unknown;
  }): PoolQuestion {
    const textMap = row.text as Record<string, string> | null;
    return {
      id: row.id as string,
      type: row.type as PoolQuestion["type"],
      status: row.status as PoolQuestion["status"],
      correctIndex: row.correct_index as number,
      book: row.book as string | null,
      text: textMap?.en ?? "(no English text)",
    };
  }

  const initialQuestions: PoolQuestion[] = (slotRows ?? [])
    .map((r) => r.questions as unknown as Parameters<typeof toPoolQuestion>[0] | null)
    .filter((q): q is Parameters<typeof toPoolQuestion>[0] => q !== null)
    .map(toPoolQuestion);

  const usedElsewhere = new Set((allAssignments ?? []).map((a) => a.question_id as string));
  const pool: PoolQuestion[] = (approvedRows ?? [])
    .filter((r) => !usedElsewhere.has(r.id as string))
    .map(toPoolQuestion);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold text-bibliq-ink">
          Level {levelNumber} {level.is_boss ? "(Boss)" : ""}
        </h1>
        <p className="text-sm text-muted-foreground">World {level.world_id as number}</p>
      </div>
      <LevelEditor
        level={{
          number: level.number as number,
          world: level.world_id as number,
          isBoss: level.is_boss as boolean,
          starThresholds: level.star_thresholds as number[],
          questionIds: initialQuestions.map((q) => q.id),
        }}
        levelStatus={level.status as "draft" | "ready" | "published"}
        initialQuestions={initialQuestions}
        pool={pool}
        otherLevelAssignments={(allAssignments ?? []).map((a) => ({
          levelNumber: a.level_number as number,
          questionId: a.question_id as string,
        }))}
      />
    </div>
  );
}
