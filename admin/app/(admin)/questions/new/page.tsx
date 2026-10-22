import { QuestionEditor } from "@/components/question-editor/question-editor";
import { QuestionType } from "@/lib/content-types";

interface NewQuestionPageProps {
  searchParams: Promise<{ type?: string; difficulty?: string; book?: string }>;
}

export default async function NewQuestionPage({ searchParams }: NewQuestionPageProps) {
  const params = await searchParams;

  return (
    <QuestionEditor
      question={null}
      cdnBaseUrl={process.env.NEXT_PUBLIC_CDN_BASE_URL ?? ""}
      prefill={{
        type: params.type as QuestionType | undefined,
        difficulty: params.difficulty ? Number(params.difficulty) : undefined,
        book: params.book,
      }}
    />
  );
}
