"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { LocalizedOptions, LocalizedText, QuestionStatus, QuestionType } from "@/lib/content-types";

export interface QuestionFormPayload {
  type: QuestionType;
  difficulty: number;
  book: string | null;
  chapterFrom: number | null;
  chapterTo: number | null;
  notes: string | null;
  text: LocalizedText;
  options: LocalizedOptions;
  correctIndex: number;
  reference: LocalizedText;
  imageKey: string | null;
}

function toRow(payload: QuestionFormPayload) {
  return {
    type: payload.type,
    difficulty: payload.difficulty,
    book: payload.book,
    chapter_from: payload.chapterFrom,
    chapter_to: payload.chapterTo,
    notes: payload.notes,
    text: payload.text,
    options: payload.options,
    correct_index: payload.correctIndex,
    reference: payload.reference,
    image_key: payload.imageKey,
  };
}

export async function createQuestion(payload: QuestionFormPayload) {
  const supabase = await createClient();
  const { data, error } = await supabase
    .from("questions")
    .insert({ ...toRow(payload), source: "manual" })
    .select("id")
    .single();
  if (error) return { error: error.message, id: null };
  revalidatePath("/questions");
  return { error: null, id: data.id as string };
}

export async function updateQuestion(id: string, payload: QuestionFormPayload) {
  const supabase = await createClient();
  const { error } = await supabase.from("questions").update(toRow(payload)).eq("id", id);
  revalidatePath("/questions");
  revalidatePath(`/questions/${id}`);
  return { error: error?.message ?? null };
}

export async function setQuestionStatus(id: string, status: QuestionStatus) {
  const supabase = await createClient();
  const { error } = await supabase.from("questions").update({ status }).eq("id", id);
  revalidatePath(`/questions/${id}`);
  revalidatePath("/questions");
  return { error: error?.message ?? null };
}

const CHECK_COLUMNS = ["check_bible", "check_en", "check_fr", "check_es"] as const;
type CheckColumn = (typeof CHECK_COLUMNS)[number];

export async function setQuestionCheck(id: string, check: CheckColumn, value: boolean) {
  const supabase = await createClient();
  const { error } = await supabase
    .from("questions")
    .update({ [check]: value })
    .eq("id", id);
  revalidatePath(`/questions/${id}`);
  return { error: error?.message ?? null };
}

export async function duplicateQuestion(id: string) {
  const supabase = await createClient();
  const { data: original, error: fetchError } = await supabase
    .from("questions")
    .select("type, difficulty, book, chapter_from, chapter_to, notes, text, options, correct_index, reference, image_key")
    .eq("id", id)
    .single();
  if (fetchError || !original) return { error: fetchError?.message ?? "Question not found", id: null };

  const { data, error } = await supabase
    .from("questions")
    .insert({
      type: original.type,
      difficulty: original.difficulty,
      book: original.book,
      chapter_from: original.chapter_from,
      chapter_to: original.chapter_to,
      notes: original.notes,
      text: original.text,
      options: original.options,
      correct_index: original.correct_index,
      reference: original.reference,
      // Deliberately NOT image_key: a duplicated question starts without an
      // image rather than sharing the original's S3 object.
      source: "manual",
    })
    .select("id")
    .single();

  if (error) return { error: error.message, id: null };
  revalidatePath("/questions");
  return { error: null, id: data.id as string };
}

export async function addComment(questionId: string, body: string) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  const { error } = await supabase
    .from("comments")
    .insert({ question_id: questionId, body, author: user?.id ?? null });
  revalidatePath(`/questions/${questionId}`);
  return { error: error?.message ?? null };
}

export async function resolveComment(commentId: string, questionId: string, resolved: boolean) {
  const supabase = await createClient();
  const { error } = await supabase.from("comments").update({ resolved }).eq("id", commentId);
  revalidatePath(`/questions/${questionId}`);
  return { error: error?.message ?? null };
}

export async function bulkUpdateStatus(ids: string[], status: QuestionStatus) {
  if (ids.length === 0) return { error: null };
  const supabase = await createClient();
  const { error } = await supabase.from("questions").update({ status }).in("id", ids);
  revalidatePath("/questions");
  return { error: error?.message ?? null };
}

export async function bulkDeleteDrafts(ids: string[]) {
  if (ids.length === 0) return { error: null };
  const supabase = await createClient();
  // Only drafts: deleting an in-review/approved/published question should go
  // through "retire" instead, never a hard delete.
  const { error } = await supabase.from("questions").delete().in("id", ids).eq("status", "draft");
  revalidatePath("/questions");
  return { error: error?.message ?? null };
}

export async function bulkAssignToLevel(ids: string[], levelNumber: number) {
  if (ids.length === 0) return { error: null };
  const supabase = await createClient();

  const { data: existing, error: fetchError } = await supabase
    .from("level_questions")
    .select("position")
    .eq("level_number", levelNumber)
    .order("position", { ascending: false })
    .limit(1);

  if (fetchError) return { error: fetchError.message };

  let nextPosition = (existing?.[0]?.position as number | undefined ?? 0) + 1;
  const rows = ids.map((questionId) => ({
    level_number: levelNumber,
    question_id: questionId,
    position: nextPosition++,
  }));

  const { error } = await supabase.from("level_questions").insert(rows);
  revalidatePath("/questions");
  revalidatePath("/levels");
  return { error: error?.message ?? null };
}
