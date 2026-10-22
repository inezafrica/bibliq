import { Badge } from "@/components/ui/badge";
import type { LevelStatus, QuestionStatus } from "@/lib/content-types";

const QUESTION_STATUS_STYLE: Record<QuestionStatus, string> = {
  draft: "bg-muted text-muted-foreground",
  in_review: "bg-bibliq-gold/20 text-amber-800",
  approved: "bg-bibliq-olive/15 text-bibliq-olive",
  published: "bg-bibliq-royal/15 text-bibliq-royal",
  retired: "bg-bibliq-coral/15 text-bibliq-coral",
};

const LEVEL_STATUS_STYLE: Record<LevelStatus, string> = {
  draft: "bg-muted text-muted-foreground",
  ready: "bg-bibliq-gold/20 text-amber-800",
  published: "bg-bibliq-royal/15 text-bibliq-royal",
};

export function QuestionStatusBadge({ status }: { status: QuestionStatus }) {
  return (
    <Badge variant="outline" className={QUESTION_STATUS_STYLE[status]}>
      {status.replace("_", " ")}
    </Badge>
  );
}

export function LevelStatusBadge({ status }: { status: LevelStatus }) {
  return (
    <Badge variant="outline" className={LEVEL_STATUS_STYLE[status]}>
      {status}
    </Badge>
  );
}
