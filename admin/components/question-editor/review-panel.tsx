"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { addComment, resolveComment, setQuestionCheck } from "@/app/(admin)/questions/actions";

export interface CheckState {
  value: boolean;
  by: string | null;
  at: string | null;
}

export interface CommentItem {
  id: string;
  body: string;
  author: string | null;
  resolved: boolean;
  createdAt: string;
}

interface ReviewPanelProps {
  questionId: string;
  bible: CheckState;
  en: CheckState;
  fr: CheckState;
  es: CheckState;
  comments: CommentItem[];
}

const CHECK_ROWS: { key: "check_bible" | "check_en" | "check_fr" | "check_es"; label: string }[] = [
  { key: "check_bible", label: "Checked against the Bible text" },
  { key: "check_en", label: "English reviewed" },
  { key: "check_fr", label: "French reviewed" },
  { key: "check_es", label: "Spanish reviewed" },
];

export function ReviewPanel({ questionId, bible, en, fr, es, comments }: ReviewPanelProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [commentBody, setCommentBody] = useState("");
  const states: Record<string, CheckState> = { check_bible: bible, check_en: en, check_fr: fr, check_es: es };

  function handleCheckToggle(check: "check_bible" | "check_en" | "check_fr" | "check_es", value: boolean) {
    startTransition(async () => {
      const { error } = await setQuestionCheck(questionId, check, value);
      if (error) toast.error(error);
      router.refresh();
    });
  }

  function handleAddComment() {
    if (!commentBody.trim()) return;
    startTransition(async () => {
      const { error } = await addComment(questionId, commentBody.trim());
      if (error) toast.error(error);
      else setCommentBody("");
      router.refresh();
    });
  }

  function handleResolve(commentId: string, resolved: boolean) {
    startTransition(async () => {
      await resolveComment(commentId, questionId, resolved);
      router.refresh();
    });
  }

  return (
    <div className="space-y-6">
      <div className="space-y-3">
        {CHECK_ROWS.map(({ key, label }) => {
          const state = states[key];
          return (
            <label key={key} className="flex items-start gap-3 rounded-lg border p-3">
              <Checkbox
                checked={state.value}
                disabled={isPending}
                onCheckedChange={(v) => handleCheckToggle(key, !!v)}
              />
              <span className="flex-1">
                <span className="block text-sm font-medium">{label}</span>
                {state.value && state.at && (
                  <span className="text-xs text-muted-foreground">
                    {state.by ?? "Someone"} · {new Date(state.at).toLocaleString()}
                  </span>
                )}
              </span>
            </label>
          );
        })}
      </div>

      <div className="space-y-3">
        <h3 className="text-sm font-semibold">Comments</h3>
        <div className="space-y-2">
          {comments.length === 0 && <p className="text-sm text-muted-foreground">No comments yet.</p>}
          {comments.map((c) => (
            <div key={c.id} className={`rounded-lg border p-3 text-sm ${c.resolved ? "opacity-60" : ""}`}>
              <div className="flex items-center justify-between">
                <span className="font-medium">{c.author ?? "Someone"}</span>
                <button
                  type="button"
                  className="text-xs text-bibliq-royal underline underline-offset-2"
                  onClick={() => handleResolve(c.id, !c.resolved)}
                >
                  {c.resolved ? "Reopen" : "Resolve"}
                </button>
              </div>
              <p className="mt-1 whitespace-pre-wrap">{c.body}</p>
              <p className="mt-1 text-xs text-muted-foreground">{new Date(c.createdAt).toLocaleString()}</p>
            </div>
          ))}
        </div>
        <Textarea
          placeholder="Leave a comment…"
          value={commentBody}
          onChange={(e) => setCommentBody(e.target.value)}
          rows={3}
        />
        <Button size="sm" onClick={handleAddComment} disabled={isPending || !commentBody.trim()}>
          Add comment
        </Button>
      </div>
    </div>
  );
}
