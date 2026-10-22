"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { LevelStatus } from "@/lib/content-types";

export async function createLevel(params: { number: number; worldId: number; isBoss: boolean }) {
  const supabase = await createClient();
  const { error } = await supabase.from("levels").insert({
    number: params.number,
    world_id: params.worldId,
    is_boss: params.isBoss,
    star_thresholds: params.isBoss ? [10, 13, 15] : [7, 9, 10],
  });
  revalidatePath("/levels");
  return { error: error?.message ?? null };
}

export async function setLevelQuestions(levelNumber: number, questionIds: string[]) {
  const supabase = await createClient();

  const { error: deleteError } = await supabase
    .from("level_questions")
    .delete()
    .eq("level_number", levelNumber);
  if (deleteError) return { error: deleteError.message };

  if (questionIds.length > 0) {
    const rows = questionIds.map((questionId, index) => ({
      level_number: levelNumber,
      question_id: questionId,
      position: index + 1,
    }));
    const { error: insertError } = await supabase.from("level_questions").insert(rows);
    if (insertError) return { error: insertError.message };
  }

  revalidatePath(`/levels/${levelNumber}`);
  revalidatePath("/levels");
  return { error: null };
}

export async function setLevelStatus(levelNumber: number, status: LevelStatus) {
  const supabase = await createClient();
  const { error } = await supabase.from("levels").update({ status }).eq("number", levelNumber);
  revalidatePath(`/levels/${levelNumber}`);
  revalidatePath("/levels");
  return { error: error?.message ?? null };
}

export async function updateStarThresholds(levelNumber: number, thresholds: [number, number, number]) {
  const supabase = await createClient();
  const { error } = await supabase
    .from("levels")
    .update({ star_thresholds: thresholds })
    .eq("number", levelNumber);
  revalidatePath(`/levels/${levelNumber}`);
  return { error: error?.message ?? null };
}
