"use client";

import { useMemo, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowDown, ArrowUp, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { QuestionStatusBadge } from "@/components/status-badge";
import { validateLevel, findDuplicateQuestionAssignments } from "@/lib/validation";
import { LevelContent, QuestionSummary } from "@/lib/content-types";
import { setLevelQuestions, setLevelStatus } from "@/app/(admin)/levels/actions";

export interface PoolQuestion extends QuestionSummary {
  text: string;
  book: string | null;
}

interface LevelEditorProps {
  level: LevelContent;
  levelStatus: "draft" | "ready" | "published";
  initialQuestions: PoolQuestion[]; // the level's current slots, in order
  pool: PoolQuestion[]; // approved + unused questions available to add
  otherLevelAssignments: { levelNumber: number; questionId: string }[]; // every other level's question ids, for the duplicate check
}

export function LevelEditor({ level, levelStatus, initialQuestions, pool, otherLevelAssignments }: LevelEditorProps) {
  const router = useRouter();
  const [slots, setSlots] = useState<PoolQuestion[]>(initialQuestions);
  const [search, setSearch] = useState("");
  const [saving, startSaving] = useTransition();

  const expected = level.isBoss ? 15 : 10;
  const slotIds = new Set(slots.map((q) => q.id));
  const availablePool = pool.filter(
    (q) => !slotIds.has(q.id) && (search.trim() === "" || q.text.toLowerCase().includes(search.toLowerCase()))
  );

  const summaries: QuestionSummary[] = slots.map((s) => ({
    id: s.id,
    type: s.type,
    status: s.status,
    correctIndex: s.correctIndex,
    book: s.book,
  }));

  const { errors, warnings } = useMemo(
    () => validateLevel({ ...level, questionIds: slots.map((s) => s.id) }, summaries),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [slots, level]
  );

  const duplicateIssues = useMemo(
    () =>
      findDuplicateQuestionAssignments([
        ...otherLevelAssignments,
        ...slots.map((s) => ({ levelNumber: level.number, questionId: s.id })),
      ]),
    [otherLevelAssignments, slots, level.number]
  );

  function addQuestion(q: PoolQuestion) {
    if (slots.length >= expected) {
      toast.error(`This level already has ${expected} questions`);
      return;
    }
    setSlots((prev) => [...prev, q]);
  }

  function removeSlot(index: number) {
    setSlots((prev) => prev.filter((_, i) => i !== index));
  }

  function moveSlot(index: number, direction: -1 | 1) {
    setSlots((prev) => {
      const target = index + direction;
      if (target < 0 || target >= prev.length) return prev;
      const copy = [...prev];
      [copy[index], copy[target]] = [copy[target], copy[index]];
      return copy;
    });
  }

  function handleSave() {
    startSaving(async () => {
      const { error } = await setLevelQuestions(
        level.number,
        slots.map((s) => s.id)
      );
      if (error) {
        toast.error(error);
        return;
      }
      toast.success("Level saved");
      router.refresh();
    });
  }

  function handleStatusChange(status: "draft" | "ready" | "published") {
    startSaving(async () => {
      const { error } = await setLevelStatus(level.number, status);
      if (error) toast.error(error);
      else {
        toast.success(`Level marked ${status}`);
        router.refresh();
      }
    });
  }

  const allIssues = [...errors, ...duplicateIssues];

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-heading text-lg font-semibold">
            Slots ({slots.length}/{expected})
          </h2>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={saving} onClick={() => handleStatusChange("draft")}>
              Mark draft
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={saving || allIssues.length > 0}
              onClick={() => handleStatusChange("ready")}
            >
              Mark ready
            </Button>
          </div>
        </div>

        {allIssues.length > 0 && (
          <Alert variant="destructive">
            <AlertTitle>{allIssues.length} issue(s)</AlertTitle>
            <AlertDescription>
              <ul className="list-inside list-disc">
                {allIssues.map((issue, i) => (
                  <li key={i}>{issue.message}</li>
                ))}
              </ul>
            </AlertDescription>
          </Alert>
        )}
        {warnings.length > 0 && (
          <Alert>
            <AlertTitle>{warnings.length} warning(s)</AlertTitle>
            <AlertDescription>
              <ul className="list-inside list-disc">
                {warnings.map((w, i) => (
                  <li key={i}>{w.message}</li>
                ))}
              </ul>
            </AlertDescription>
          </Alert>
        )}

        <ol className="space-y-2">
          {slots.map((q, index) => (
            <li key={q.id} className="flex items-center gap-2 rounded-lg border p-2">
              <span className="w-6 text-center text-sm text-muted-foreground">{index + 1}</span>
              <span className="flex-1 truncate text-sm">
                <span className="font-mono text-xs text-muted-foreground">{q.id}</span> {q.text}
              </span>
              <QuestionStatusBadge status={q.status} />
              <div className="flex gap-1">
                <Button variant="ghost" size="icon-sm" onClick={() => moveSlot(index, -1)}>
                  <ArrowUp className="h-3.5 w-3.5" />
                </Button>
                <Button variant="ghost" size="icon-sm" onClick={() => moveSlot(index, 1)}>
                  <ArrowDown className="h-3.5 w-3.5" />
                </Button>
                <Button variant="ghost" size="icon-sm" onClick={() => removeSlot(index)}>
                  <X className="h-3.5 w-3.5" />
                </Button>
              </div>
            </li>
          ))}
          {slots.length === 0 && <p className="text-sm text-muted-foreground">No questions assigned yet.</p>}
        </ol>

        <Button className="bg-bibliq-royal hover:bg-bibliq-royal-dark" onClick={handleSave} disabled={saving}>
          Save slots
        </Button>
      </div>

      <div className="space-y-3">
        <h2 className="font-heading text-lg font-semibold">Approved &amp; unused questions</h2>
        <Input placeholder="Search…" value={search} onChange={(e) => setSearch(e.target.value)} />
        <ul className="max-h-[600px] space-y-2 overflow-y-auto">
          {availablePool.map((q) => (
            <li key={q.id} className="flex items-center gap-2 rounded-lg border p-2">
              <span className="flex-1 truncate text-sm">
                <span className="font-mono text-xs text-muted-foreground">{q.id}</span> {q.text}
              </span>
              <span className="text-xs text-muted-foreground">{q.type}</span>
              <Button size="sm" variant="outline" onClick={() => addQuestion(q)}>
                Add
              </Button>
            </li>
          ))}
          {availablePool.length === 0 && (
            <p className="text-sm text-muted-foreground">No matching approved questions.</p>
          )}
        </ul>
      </div>
    </div>
  );
}
