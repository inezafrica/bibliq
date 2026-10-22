"use server";

import { revalidatePath } from "next/cache";
import { requireRole } from "@/lib/auth";
import { createClient } from "@/lib/supabase/server";
import { Pack } from "@/lib/content-types";
import { validateQuestionContent } from "@/lib/validation";

export interface ImportPreviewItem {
  id: string;
  willImport: boolean;
  reason: string | null;
}

export interface ImportPreviewResult {
  packId: number;
  questions: ImportPreviewItem[];
  levels: ImportPreviewItem[];
}

function parsePack(packJsonText: string): { pack: Pack | null; error: string | null } {
  try {
    const pack = JSON.parse(packJsonText) as Pack;
    if (!Array.isArray(pack.questions) || !Array.isArray(pack.levels)) {
      return { pack: null, error: "File is missing 'questions' or 'levels' arrays" };
    }
    return { pack, error: null };
  } catch {
    return { pack: null, error: "That file isn't valid JSON" };
  }
}

export async function previewImport(
  packJsonText: string
): Promise<{ error: string | null; result: ImportPreviewResult | null }> {
  await requireRole(["admin"]);
  const { pack, error } = parsePack(packJsonText);
  if (!pack) return { error, result: null };

  const supabase = await createClient();
  const [{ data: existingQuestions }, { data: existingLevels }] = await Promise.all([
    supabase.from("questions").select("id"),
    supabase.from("levels").select("number"),
  ]);
  const existingIds = new Set((existingQuestions ?? []).map((q) => q.id as string));
  const existingLevelNumbers = new Set((existingLevels ?? []).map((l) => l.number as number));

  const questions: ImportPreviewItem[] = pack.questions.map((q) => {
    if (existingIds.has(q.id)) return { id: q.id, willImport: false, reason: "Question id already exists" };
    const issues = validateQuestionContent({
      type: q.type,
      difficulty: q.difficulty,
      text: q.text,
      options: q.options,
      correctIndex: q.correctIndex,
      imageKey: q.imageUrl ?? null,
      reference: q.reference,
    });
    if (issues.length > 0) return { id: q.id, willImport: false, reason: issues.map((i) => i.message).join("; ") };
    return { id: q.id, willImport: true, reason: null };
  });

  const levels: ImportPreviewItem[] = pack.levels.map((l) => {
    const label = `Level ${l.number}`;
    if (existingLevelNumbers.has(l.number)) return { id: label, willImport: false, reason: "Level already exists" };
    return { id: label, willImport: true, reason: null };
  });

  return { error: null, result: { packId: pack.packId, questions, levels } };
}

export async function runImport(packJsonText: string, alreadyReviewed: boolean) {
  await requireRole(["admin"]);
  const { pack, error } = parsePack(packJsonText);
  if (!pack) return { error, imported: 0, importedLevels: 0, skipped: 0 };

  const preview = await previewImport(packJsonText);
  if (!preview.result) return { error: preview.error, imported: 0, importedLevels: 0, skipped: 0 };

  const supabase = await createClient();
  const importableQuestionIds = new Set(preview.result.questions.filter((q) => q.willImport).map((q) => q.id));
  const toImportQuestions = pack.questions.filter((q) => importableQuestionIds.has(q.id));

  let imported = 0;
  for (const q of toImportQuestions) {
    const { error: insertError } = await supabase.from("questions").insert({
      id: q.id,
      type: q.type,
      difficulty: q.difficulty,
      text: q.text,
      options: q.options,
      correct_index: q.correctIndex,
      reference: q.reference,
      image_key: q.imageUrl ?? null,
      source: "import",
      status: alreadyReviewed ? "approved" : "in_review",
      check_bible: alreadyReviewed,
      check_en: alreadyReviewed,
      check_fr: alreadyReviewed,
      check_es: alreadyReviewed,
    });
    if (!insertError) imported++;
  }
  // Explicit ids (q_0001...) bypass the sequence -- re-sync so the next
  // manually created question doesn't collide with one just imported.
  await supabase.rpc("sync_question_id_sequence");

  const importableLevelNumbers = new Set(
    preview.result.levels.filter((l) => l.willImport).map((l) => Number(l.id.replace("Level ", "")))
  );
  let importedLevels = 0;
  for (const level of pack.levels.filter((l) => importableLevelNumbers.has(l.number))) {
    const { error: levelError } = await supabase.from("levels").insert({
      number: level.number,
      world_id: level.world,
      is_boss: level.isBoss,
      star_thresholds: level.starThresholds,
      status: "draft",
    });
    if (!levelError) {
      importedLevels++;
      const rows = level.questionIds
        .filter((qid) => importableQuestionIds.has(qid) || !pack.questions.some((q) => q.id === qid))
        .map((qid, index) => ({ level_number: level.number, question_id: qid, position: index + 1 }));
      if (rows.length > 0) await supabase.from("level_questions").insert(rows);
    }
  }

  revalidatePath("/questions");
  revalidatePath("/levels");
  revalidatePath("/dashboard");

  return {
    error: null,
    imported,
    importedLevels,
    skipped: toImportQuestions.length - imported + (preview.result.questions.length - toImportQuestions.length),
  };
}
