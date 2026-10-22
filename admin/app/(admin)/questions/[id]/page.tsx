import { notFound } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import { QuestionEditor, QuestionEditorData } from "@/components/question-editor/question-editor";

interface QuestionPageProps {
  params: Promise<{ id: string }>;
}

export default async function QuestionPage({ params }: QuestionPageProps) {
  const { id } = await params;
  const supabase = await createClient();

  const [{ data: row }, { data: commentRows }] = await Promise.all([
    supabase
      .from("questions")
      .select(
        `id, type, difficulty, book, chapter_from, chapter_to, notes, text, options, correct_index,
         reference, image_key, status,
         check_bible, check_bible_by, check_bible_at,
         check_en, check_en_by, check_en_at,
         check_fr, check_fr_by, check_fr_at,
         check_es, check_es_by, check_es_at`
      )
      .eq("id", id)
      .maybeSingle(),
    supabase
      .from("comments")
      .select("id, body, resolved, created_at, admin_users(name)")
      .eq("question_id", id)
      .order("created_at", { ascending: true }),
  ]);

  if (!row) notFound();

  // Resolve the "who" display names for whichever checks are currently set.
  const reviewerIds = [row.check_bible_by, row.check_en_by, row.check_fr_by, row.check_es_by].filter(
    (v): v is string => Boolean(v)
  );
  const { data: reviewerRows } =
    reviewerIds.length > 0
      ? await supabase.from("admin_users").select("user_id, name").in("user_id", reviewerIds)
      : { data: [] as { user_id: string; name: string }[] };
  const nameById = new Map((reviewerRows ?? []).map((r) => [r.user_id as string, r.name as string]));

  const question: QuestionEditorData = {
    id: row.id as string,
    type: row.type as QuestionEditorData["type"],
    difficulty: row.difficulty as number,
    book: row.book as string | null,
    chapterFrom: row.chapter_from as number | null,
    chapterTo: row.chapter_to as number | null,
    notes: row.notes as string | null,
    text: (row.text as QuestionEditorData["text"]) ?? {},
    options: (row.options as QuestionEditorData["options"]) ?? {},
    correctIndex: row.correct_index as number,
    reference: (row.reference as QuestionEditorData["reference"]) ?? {},
    imageKey: row.image_key as string | null,
    status: row.status as QuestionEditorData["status"],
    checks: {
      bible: {
        value: row.check_bible as boolean,
        by: nameById.get(row.check_bible_by as string) ?? null,
        at: row.check_bible_at as string | null,
      },
      en: {
        value: row.check_en as boolean,
        by: nameById.get(row.check_en_by as string) ?? null,
        at: row.check_en_at as string | null,
      },
      fr: {
        value: row.check_fr as boolean,
        by: nameById.get(row.check_fr_by as string) ?? null,
        at: row.check_fr_at as string | null,
      },
      es: {
        value: row.check_es as boolean,
        by: nameById.get(row.check_es_by as string) ?? null,
        at: row.check_es_at as string | null,
      },
    },
    comments: (commentRows ?? []).map((c) => ({
      id: c.id as string,
      body: c.body as string,
      resolved: c.resolved as boolean,
      createdAt: c.created_at as string,
      author: (c.admin_users as unknown as { name?: string } | null)?.name ?? null,
    })),
  };

  return <QuestionEditor question={question} cdnBaseUrl={process.env.NEXT_PUBLIC_CDN_BASE_URL ?? ""} />;
}
