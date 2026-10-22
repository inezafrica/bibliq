import Link from "next/link";
import { createClient } from "@/lib/supabase/server";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { QuestionsFilters } from "./questions-filters";
import { QuestionsTable, QuestionRow } from "./questions-table";

const FETCH_LIMIT = 1000;

interface QuestionsPageProps {
  searchParams: Promise<{
    q?: string;
    status?: string;
    type?: string;
    book?: string;
    hasImage?: string;
    used?: string;
    failedChecks?: string;
  }>;
}

export default async function QuestionsPage({ searchParams }: QuestionsPageProps) {
  const params = await searchParams;
  const supabase = await createClient();

  let query = supabase
    .from("questions")
    .select(
      "id, type, difficulty, status, book, text, image_key, check_bible, check_en, check_fr, check_es, created_at"
    )
    .order("created_at", { ascending: false })
    .limit(FETCH_LIMIT);

  if (params.status) query = query.eq("status", params.status);
  if (params.type) query = query.eq("type", params.type);
  if (params.book) query = query.eq("book", params.book);
  if (params.hasImage === "true") query = query.not("image_key", "is", null);
  if (params.hasImage === "false") query = query.is("image_key", null);

  const [{ data: rows }, { data: levelQuestionRows }, { data: worldsRows }] = await Promise.all([
    query,
    supabase.from("level_questions").select("question_id"),
    supabase.from("worlds").select("id, books").order("sort"),
  ]);

  const usedIds = new Set((levelQuestionRows ?? []).map((r) => r.question_id as string));

  let filtered: QuestionRow[] = (rows ?? []).map((r) => ({
    id: r.id as string,
    type: r.type as string,
    difficulty: r.difficulty as number,
    status: r.status as QuestionRow["status"],
    book: r.book as string | null,
    text: r.text as Record<string, string> | null,
    image_key: r.image_key as string | null,
    check_bible: r.check_bible as boolean,
    check_en: r.check_en as boolean,
    check_fr: r.check_fr as boolean,
    check_es: r.check_es as boolean,
    used: usedIds.has(r.id as string),
  }));

  if (params.used === "true") filtered = filtered.filter((r) => r.used);
  if (params.used === "false") filtered = filtered.filter((r) => !r.used);
  if (params.failedChecks === "true") {
    filtered = filtered.filter((r) => !(r.check_bible && r.check_en && r.check_fr && r.check_es));
  }
  if (params.q) {
    const needle = params.q.toLowerCase();
    filtered = filtered.filter((r) =>
      [r.text?.en, r.text?.fr, r.text?.es].some((t) => t?.toLowerCase().includes(needle))
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Questions</h1>
          <p className="text-sm text-muted-foreground">{filtered.length} question(s)</p>
        </div>
        <Link
          href="/questions/new"
          className={cn(buttonVariants(), "bg-bibliq-royal hover:bg-bibliq-royal-dark")}
        >
          New question
        </Link>
      </div>

      <QuestionsFilters worlds={worldsRows ?? []} />
      <QuestionsTable questions={filtered} />
    </div>
  );
}
