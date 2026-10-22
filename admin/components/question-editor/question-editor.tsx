"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useFieldArray, useForm } from "react-hook-form";
import { toast } from "sonner";
import { Plus, Trash2, ArrowUp, ArrowDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  LanguageCode,
  QuestionStatus,
  QuestionType,
  QUESTION_TYPES,
  SUPPORTED_LANGUAGES,
  TRUE_FALSE_LABELS,
} from "@/lib/content-types";
import { NEW_TESTAMENT_BOOKS, OLD_TESTAMENT_BOOKS } from "@/lib/bible-books";
import { validateQuestionContent, ValidationIssue } from "@/lib/validation";
import {
  createQuestion,
  duplicateQuestion,
  QuestionFormPayload,
  setQuestionStatus,
  updateQuestion,
} from "@/app/(admin)/questions/actions";
import { QuestionStatusBadge } from "@/components/status-badge";
import { PhonePreview } from "./phone-preview";
import { ImageUpload } from "./image-upload";
import { ReviewPanel, CheckState, CommentItem } from "./review-panel";

interface OptionRow {
  en: string;
  fr: string;
  es: string;
}

interface FormValues {
  type: QuestionType;
  difficulty: number;
  book: string;
  chapterFrom: string;
  chapterTo: string;
  notes: string;
  text: Record<LanguageCode, string>;
  optionRows: OptionRow[];
  correctIndex: number;
  reference: Record<LanguageCode, string>;
}

export interface QuestionEditorData {
  id: string;
  type: QuestionType;
  difficulty: number;
  book: string | null;
  chapterFrom: number | null;
  chapterTo: number | null;
  notes: string | null;
  text: Partial<Record<LanguageCode, string>>;
  options: Partial<Record<LanguageCode, string[]>>;
  correctIndex: number;
  reference: Partial<Record<LanguageCode, string>>;
  imageKey: string | null;
  status: QuestionStatus;
  checks: { bible: CheckState; en: CheckState; fr: CheckState; es: CheckState };
  comments: CommentItem[];
}

interface QuestionEditorProps {
  question: QuestionEditorData | null;
  cdnBaseUrl: string;
  prefill?: { type?: QuestionType; difficulty?: number; book?: string };
}

function emptyOptionRows(count: number): OptionRow[] {
  return Array.from({ length: count }, () => ({ en: "", fr: "", es: "" }));
}

function toOptionRows(options: Partial<Record<LanguageCode, string[]>>): OptionRow[] {
  const maxLen = Math.max(options.en?.length ?? 0, options.fr?.length ?? 0, options.es?.length ?? 0, 2);
  return Array.from({ length: maxLen }, (_, i) => ({
    en: options.en?.[i] ?? "",
    fr: options.fr?.[i] ?? "",
    es: options.es?.[i] ?? "",
  }));
}

function draftKey(id: string) {
  return `bibliq-question-draft-${id}`;
}

// SelectItem can't take an empty-string value, so the "no book selected"
// option uses this sentinel and gets mapped back to "" on change.
const NO_BOOK = "__none__";

export function QuestionEditor({ question, cdnBaseUrl, prefill }: QuestionEditorProps) {
  const router = useRouter();
  const [imageKey, setImageKey] = useState<string | null>(question?.imageKey ?? null);
  const [localImagePreview, setLocalImagePreview] = useState<string | null>(
    question?.imageKey ? `${cdnBaseUrl.replace(/\/$/, "")}/${question.imageKey}` : null
  );
  const [saving, setSaving] = useState(false);
  const [issues, setIssues] = useState<ValidationIssue[]>([]);
  const [hasRestoredDraft, setHasRestoredDraft] = useState(false);

  const defaultValues: FormValues = useMemo(
    () => ({
      type: question?.type ?? prefill?.type ?? "MCQ",
      difficulty: question?.difficulty ?? prefill?.difficulty ?? 1,
      book: question?.book ?? prefill?.book ?? "",
      chapterFrom: question?.chapterFrom != null ? String(question.chapterFrom) : "",
      chapterTo: question?.chapterTo != null ? String(question.chapterTo) : "",
      notes: question?.notes ?? "",
      text: { en: question?.text.en ?? "", fr: question?.text.fr ?? "", es: question?.text.es ?? "" },
      optionRows: question ? toOptionRows(question.options) : emptyOptionRows(4),
      correctIndex: question?.correctIndex ?? 0,
      reference: {
        en: question?.reference.en ?? "",
        fr: question?.reference.fr ?? "",
        es: question?.reference.es ?? "",
      },
    }),
    [question, prefill]
  );

  const { register, control, handleSubmit, watch, setValue, getValues, reset } = useForm<FormValues>({
    defaultValues,
  });

  const { fields, append, remove, swap } = useFieldArray({ control, name: "optionRows" });

  const type = watch("type");
  const book = watch("book");
  const optionRows = watch("optionRows");
  const correctIndex = watch("correctIndex");
  const text = watch("text");
  const reference = watch("reference");

  // Draft key is stable per question id (or "new" for an unsaved one); a
  // prefilled "new" via Save-and-new gets its own draft slot once it exists.
  const key = draftKey(question?.id ?? "new");

  useEffect(() => {
    const saved = typeof window !== "undefined" ? window.localStorage.getItem(key) : null;
    if (saved && !hasRestoredDraft) {
      setHasRestoredDraft(true);
      toast("Unsaved local draft found", {
        action: {
          label: "Restore",
          onClick: () => {
            try {
              reset(JSON.parse(saved));
            } catch {
              /* ignore corrupt draft */
            }
          },
        },
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  useEffect(() => {
    const interval = setInterval(() => {
      if (typeof window === "undefined") return;
      window.localStorage.setItem(key, JSON.stringify(getValues()));
    }, 5_000);
    return () => clearInterval(interval);
  }, [key, getValues]);

  function clearDraft() {
    if (typeof window !== "undefined") window.localStorage.removeItem(key);
  }

  // TRUE_FALSE options are canonical and not freely editable.
  useEffect(() => {
    if (type === "TRUE_FALSE") {
      setValue("optionRows", [
        { en: TRUE_FALSE_LABELS.en[0], fr: TRUE_FALSE_LABELS.fr[0], es: TRUE_FALSE_LABELS.es[0] },
        { en: TRUE_FALSE_LABELS.en[1], fr: TRUE_FALSE_LABELS.fr[1], es: TRUE_FALSE_LABELS.es[1] },
      ]);
      if (correctIndex > 1) setValue("correctIndex", 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type]);

  function buildPayload(values: FormValues): QuestionFormPayload {
    return {
      type: values.type,
      difficulty: Number(values.difficulty),
      book: values.book.trim() || null,
      chapterFrom: values.chapterFrom ? Number(values.chapterFrom) : null,
      chapterTo: values.chapterTo ? Number(values.chapterTo) : null,
      notes: values.notes.trim() || null,
      text: values.text,
      options: {
        en: values.optionRows.map((r) => r.en),
        fr: values.optionRows.map((r) => r.fr),
        es: values.optionRows.map((r) => r.es),
      },
      correctIndex: Number(values.correctIndex),
      reference: values.reference,
      imageKey,
    };
  }

  async function doSave(values: FormValues): Promise<string | null> {
    const payload = buildPayload(values);
    if (question) {
      const { error } = await updateQuestion(question.id, payload);
      if (error) {
        toast.error(error);
        return null;
      }
      return question.id;
    }
    const { error, id } = await createQuestion(payload);
    if (error || !id) {
      toast.error(error ?? "Could not create question");
      return null;
    }
    return id;
  }

  const onSaveDraft = handleSubmit(async (values) => {
    setSaving(true);
    const id = await doSave(values);
    setSaving(false);
    if (!id) return;
    clearDraft();
    toast.success("Saved");
    if (!question) router.push(`/questions/${id}`);
    else router.refresh();
  });

  const onSendToReview = handleSubmit(async (values) => {
    const payload = buildPayload(values);
    const found = validateQuestionContent({
      type: payload.type,
      difficulty: payload.difficulty,
      text: payload.text,
      options: payload.options,
      correctIndex: payload.correctIndex,
      imageKey: payload.imageKey,
      reference: payload.reference,
    });
    setIssues(found);
    if (found.length > 0) {
      toast.error(`Fix ${found.length} issue(s) before sending to review`);
      return;
    }
    setSaving(true);
    const id = await doSave(values);
    // Don't force an already-approved/published question back to in_review
    // just because someone clicked this button again -- only draft/in_review
    // questions actually need the status set here.
    if (id && (!question || question.status === "draft" || question.status === "in_review")) {
      await setQuestionStatus(id, "in_review");
    }
    setSaving(false);
    if (!id) return;
    clearDraft();
    toast.success("Sent to review");
    if (!question) router.push(`/questions/${id}`);
    else router.refresh();
  });

  const onSaveAndNew = handleSubmit(async (values) => {
    setSaving(true);
    const id = await doSave(values);
    setSaving(false);
    if (!id) return;
    clearDraft();
    toast.success("Saved. Starting a new question.");
    const qs = new URLSearchParams({
      type: values.type,
      difficulty: String(values.difficulty),
      book: values.book,
    });
    router.push(`/questions/new?${qs.toString()}`);
  });

  async function onDuplicate() {
    if (!question) return;
    const { error, id } = await duplicateQuestion(question.id);
    if (error || !id) {
      toast.error(error ?? "Could not duplicate");
      return;
    }
    toast.success("Duplicated");
    router.push(`/questions/${id}`);
  }

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      const mod = e.ctrlKey || e.metaKey;
      if (mod && e.key.toLowerCase() === "s") {
        e.preventDefault();
        onSaveDraft();
      } else if (mod && e.key === "Enter") {
        e.preventDefault();
        onSendToReview();
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function moveOption(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= fields.length) return;
    swap(index, target);
    const current = getValues("correctIndex");
    if (current === index) setValue("correctIndex", target);
    else if (current === target) setValue("correctIndex", index);
  }

  function removeOption(index: number) {
    if (fields.length <= 2) {
      toast.error("A question needs at least 2 options");
      return;
    }
    remove(index);
    const current = getValues("correctIndex");
    if (current === index) setValue("correctIndex", 0);
    else if (current > index) setValue("correctIndex", current - 1);
  }

  const isTrueFalse = type === "TRUE_FALSE";
  const isImage = type === "IMAGE";

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_420px]">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-heading text-2xl font-bold text-bibliq-ink">
              {question ? question.id : "New question"}
            </h1>
            {question && <QuestionStatusBadge status={question.status} />}
          </div>
          <div className="flex gap-2">
            {question && (
              <Button variant="outline" onClick={onDuplicate}>
                Duplicate
              </Button>
            )}
            <Button variant="outline" onClick={onSaveAndNew} disabled={saving}>
              Save and new
            </Button>
            <Button variant="outline" onClick={onSaveDraft} disabled={saving}>
              Save draft
            </Button>
            <Button className="bg-bibliq-royal hover:bg-bibliq-royal-dark" onClick={onSendToReview} disabled={saving}>
              Send to review
            </Button>
          </div>
        </div>

        {issues.length > 0 && (
          <Alert variant="destructive">
            <AlertTitle>{issues.length} issue(s) to fix</AlertTitle>
            <AlertDescription>
              <ul className="list-inside list-disc">
                {issues.map((issue, i) => (
                  <li key={i}>{issue.message}</li>
                ))}
              </ul>
            </AlertDescription>
          </Alert>
        )}

        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="space-y-2">
            <Label>Type</Label>
            <Select value={type} onValueChange={(v) => setValue("type", v as QuestionType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {QUESTION_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Difficulty</Label>
            <Select
              value={String(watch("difficulty"))}
              onValueChange={(v) => setValue("difficulty", Number(v))}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {[1, 2, 3, 4, 5].map((d) => (
                  <SelectItem key={d} value={String(d)}>
                    {d}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Book</Label>
            <Select
              value={book || NO_BOOK}
              onValueChange={(v) => setValue("book", v && v !== NO_BOOK ? v : "")}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select a book" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_BOOK}>— None —</SelectItem>
                <SelectGroup>
                  <SelectLabel>Old Testament</SelectLabel>
                  {OLD_TESTAMENT_BOOKS.map((b) => (
                    <SelectItem key={b} value={b}>
                      {b}
                    </SelectItem>
                  ))}
                </SelectGroup>
                <SelectGroup>
                  <SelectLabel>New Testament</SelectLabel>
                  {NEW_TESTAMENT_BOOKS.map((b) => (
                    <SelectItem key={b} value={b}>
                      {b}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Chapters</Label>
            <div className="flex items-center gap-1">
              <Input type="number" placeholder="from" {...register("chapterFrom")} />
              <span className="text-muted-foreground">–</span>
              <Input type="number" placeholder="to" {...register("chapterTo")} />
            </div>
          </div>
        </div>

        <Tabs defaultValue="en">
          <TabsList>
            {SUPPORTED_LANGUAGES.map((lang) => (
              <TabsTrigger key={lang} value={lang} className="uppercase">
                {lang}
              </TabsTrigger>
            ))}
          </TabsList>
          {SUPPORTED_LANGUAGES.map((lang) => (
            <TabsContent key={lang} value={lang} className="space-y-4">
              <div className="space-y-2">
                <Label>Question text ({lang.toUpperCase()})</Label>
                <Textarea rows={3} {...register(`text.${lang}` as const)} />
              </div>
              <div className="space-y-2">
                <Label>Reference ({lang.toUpperCase()})</Label>
                <Input placeholder="Genesis 6:14" {...register(`reference.${lang}` as const)} />
              </div>
            </TabsContent>
          ))}
        </Tabs>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label>Options &amp; correct answer</Label>
            {!isTrueFalse && (
              <Button type="button" variant="outline" size="sm" onClick={() => append({ en: "", fr: "", es: "" })}>
                <Plus className="h-4 w-4" /> Add option
              </Button>
            )}
          </div>
          <RadioGroup
            value={String(correctIndex)}
            onValueChange={(v) => setValue("correctIndex", Number(v))}
            className="space-y-3"
          >
            {fields.map((field, index) => (
              <div key={field.id} className="flex items-start gap-3 rounded-lg border p-3">
                <RadioGroupItem value={String(index)} className="mt-2" />
                <div className="grid flex-1 gap-2 sm:grid-cols-3">
                  {SUPPORTED_LANGUAGES.map((lang) => (
                    <Input
                      key={lang}
                      placeholder={lang.toUpperCase()}
                      disabled={isTrueFalse}
                      {...register(`optionRows.${index}.${lang}` as const)}
                    />
                  ))}
                </div>
                {!isTrueFalse && (
                  <div className="flex flex-col gap-1">
                    <Button type="button" variant="ghost" size="icon-sm" onClick={() => moveOption(index, -1)}>
                      <ArrowUp className="h-3.5 w-3.5" />
                    </Button>
                    <Button type="button" variant="ghost" size="icon-sm" onClick={() => moveOption(index, 1)}>
                      <ArrowDown className="h-3.5 w-3.5" />
                    </Button>
                    <Button type="button" variant="ghost" size="icon-sm" onClick={() => removeOption(index)}>
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </RadioGroup>
        </div>

        {isImage && (
          <div className="space-y-2">
            <Label>Image</Label>
            <ImageUpload
              questionId={question?.id ?? null}
              previewUrl={localImagePreview}
              onUploaded={(key, localUrl) => {
                setImageKey(key);
                setLocalImagePreview(localUrl);
              }}
              onRemoved={() => {
                setImageKey(null);
                setLocalImagePreview(null);
              }}
            />
          </div>
        )}

        <div className="space-y-2">
          <Label>Notes (internal, not shown to players)</Label>
          <Textarea rows={2} {...register("notes")} />
        </div>

        {question && (
          <div className="rounded-lg border p-4">
            <h2 className="mb-3 font-heading text-lg font-semibold">Review</h2>
            <ReviewPanel
              questionId={question.id}
              bible={question.checks.bible}
              en={question.checks.en}
              fr={question.checks.fr}
              es={question.checks.es}
              comments={question.comments}
            />
          </div>
        )}
      </div>

      <div className="lg:sticky lg:top-6 lg:self-start">
        <PhonePreview
          type={type}
          text={text}
          options={{
            en: optionRows.map((r) => r.en),
            fr: optionRows.map((r) => r.fr),
            es: optionRows.map((r) => r.es),
          }}
          correctIndex={correctIndex}
          reference={reference}
          imageUrl={localImagePreview}
        />
      </div>
    </div>
  );
}
