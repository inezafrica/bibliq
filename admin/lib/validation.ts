import { z } from "zod";
import {
  EXPECTED_OPTION_COUNT,
  LevelContent,
  QuestionContent,
  QuestionSummary,
  SUPPORTED_LANGUAGES,
  TRUE_FALSE_LABELS,
} from "./content-types";

export interface ValidationIssue {
  path: string;
  message: string;
}

// ---------------------------------------------------------------------------
// Question content
// ---------------------------------------------------------------------------

const localizedTextSchema = z.record(z.string(), z.string());
const localizedOptionsSchema = z.record(z.string(), z.array(z.string()));

/**
 * Base shape check (types, presence). Cross-field rules (language coverage,
 * option counts, TRUE_FALSE labels, duplicate options) run in
 * validateQuestionContent below, where we can produce one ValidationIssue
 * per concrete problem instead of zod's single generic message.
 */
export const questionContentSchema = z.object({
  type: z.enum(["MCQ", "TRUE_FALSE", "IMAGE", "WHO_SAID", "WHICH_BOOK", "COMPLETE_VERSE", "ORDER"]),
  difficulty: z.number().int().min(1).max(5),
  text: localizedTextSchema,
  options: localizedOptionsSchema,
  correctIndex: z.number().int().min(0),
  imageKey: z.string().nullable(),
  reference: localizedTextSchema,
});

/**
 * Full validation of one question's content, used by the editor form, the
 * importer, and the publish dry run. Returns an empty array when the
 * question is fit to be approved/published.
 */
export function validateQuestionContent(
  input: QuestionContent,
  options: { imageExistsInS3?: boolean } = {}
): ValidationIssue[] {
  const issues: ValidationIssue[] = [];

  for (const lang of SUPPORTED_LANGUAGES) {
    const text = input.text[lang];
    if (!text || text.trim() === "") {
      issues.push({ path: `text.${lang}`, message: `Missing ${lang.toUpperCase()} question text` });
    }
    const reference = input.reference[lang];
    if (!reference || reference.trim() === "") {
      issues.push({ path: `reference.${lang}`, message: `Missing ${lang.toUpperCase()} verse reference` });
    }
    const opts = input.options[lang];
    if (!opts || opts.length === 0) {
      issues.push({ path: `options.${lang}`, message: `Missing ${lang.toUpperCase()} options` });
      continue;
    }
    opts.forEach((opt, index) => {
      if (!opt || opt.trim() === "") {
        issues.push({ path: `options.${lang}.${index}`, message: `Blank option in ${lang.toUpperCase()}` });
      }
    });
    const nonBlank = opts.map((o) => o.trim().toLowerCase());
    const duplicates = nonBlank.filter((o, i) => o !== "" && nonBlank.indexOf(o) !== i);
    if (duplicates.length > 0) {
      issues.push({ path: `options.${lang}`, message: `Duplicate options in ${lang.toUpperCase()}` });
    }
  }

  // Option counts must match across every language that's present.
  const counts = SUPPORTED_LANGUAGES.map((lang) => input.options[lang]?.length ?? 0).filter(
    (count) => count > 0
  );
  const distinctCounts = new Set(counts);
  if (distinctCounts.size > 1) {
    issues.push({ path: "options", message: "Option count differs between languages" });
  }

  const expectedCount = EXPECTED_OPTION_COUNT[input.type];
  if (expectedCount !== undefined) {
    for (const lang of SUPPORTED_LANGUAGES) {
      const opts = input.options[lang];
      if (opts && opts.length !== expectedCount) {
        issues.push({
          path: `options.${lang}`,
          message: `${input.type} must have exactly ${expectedCount} options, found ${opts.length}`,
        });
      }
    }
  }

  if (input.type === "TRUE_FALSE") {
    for (const lang of SUPPORTED_LANGUAGES) {
      const opts = input.options[lang];
      if (opts && JSON.stringify(opts) !== JSON.stringify(TRUE_FALSE_LABELS[lang])) {
        issues.push({
          path: `options.${lang}`,
          message: `TRUE_FALSE options must be exactly ${TRUE_FALSE_LABELS[lang].join(" / ")}`,
        });
      }
    }
  }

  const referenceOptionCount = input.options.en?.length ?? 0;
  if (referenceOptionCount > 0 && (input.correctIndex < 0 || input.correctIndex >= referenceOptionCount)) {
    issues.push({
      path: "correctIndex",
      message: `correctIndex ${input.correctIndex} is out of range (0-${referenceOptionCount - 1})`,
    });
  }

  if (input.type === "IMAGE") {
    if (!input.imageKey) {
      issues.push({ path: "imageKey", message: "IMAGE questions must have an image" });
    } else if (options.imageExistsInS3 === false) {
      issues.push({ path: "imageKey", message: "Image file was not found in S3" });
    }
  } else if (input.imageKey) {
    issues.push({ path: "imageKey", message: `${input.type} questions must not have an image` });
  }

  return issues;
}

export function isQuestionContentValid(input: QuestionContent): boolean {
  return validateQuestionContent(input).length === 0;
}

// ---------------------------------------------------------------------------
// Levels
// ---------------------------------------------------------------------------

export const levelContentSchema = z.object({
  number: z.number().int().min(1),
  world: z.number().int().min(1),
  isBoss: z.boolean(),
  starThresholds: z.array(z.number().int().min(0)).length(3),
  questionIds: z.array(z.string()),
});

export interface LevelValidationResult {
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
}

const EXPECTED_MIX = { MCQ: 5, TRUE_FALSE: 3, IMAGE: 2 };

/**
 * Validates one level's question set. `questions` must be exactly the
 * questions referenced by `level.questionIds`, in the same order --
 * callers resolve ids to rows before calling this.
 */
export function validateLevel(level: LevelContent, questions: QuestionSummary[]): LevelValidationResult {
  const errors: ValidationIssue[] = [];
  const warnings: ValidationIssue[] = [];

  const expectedCount = level.isBoss ? 15 : 10;
  if (level.questionIds.length !== expectedCount) {
    errors.push({
      path: "questionIds",
      message: `Level ${level.number} must have exactly ${expectedCount} questions, found ${level.questionIds.length}`,
    });
  }

  const missing = level.questionIds.filter((id) => !questions.some((q) => q.id === id));
  if (missing.length > 0) {
    errors.push({ path: "questionIds", message: `Question id(s) not found: ${missing.join(", ")}` });
  }

  const notApproved = questions.filter((q) => q.status !== "approved" && q.status !== "published");
  if (notApproved.length > 0) {
    errors.push({
      path: "questionIds",
      message: `Not approved: ${notApproved.map((q) => q.id).join(", ")}`,
    });
  }

  const [t1, t2, t3] = level.starThresholds;
  if (!(t1 <= t2 && t2 <= t3)) {
    errors.push({ path: "starThresholds", message: "Star thresholds must be ascending" });
  }
  if (t3 > expectedCount) {
    errors.push({
      path: "starThresholds",
      message: `3-star threshold (${t3}) cannot exceed the question count (${expectedCount})`,
    });
  }

  // Warning: a correct answer position repeated more than 4 times.
  const positionCounts = new Map<number, number>();
  for (const q of questions) {
    positionCounts.set(q.correctIndex, (positionCounts.get(q.correctIndex) ?? 0) + 1);
  }
  for (const [position, count] of positionCounts) {
    if (count > 4) {
      warnings.push({
        path: "questionIds",
        message: `Correct answer is at position ${position + 1} in ${count} questions -- mix it up`,
      });
    }
  }

  // Warning: type mix far from the suggested 5 MCQ / 3 TRUE_FALSE / 2 IMAGE template.
  if (!level.isBoss && questions.length === 10) {
    const typeCounts = { MCQ: 0, TRUE_FALSE: 0, IMAGE: 0 } as Record<string, number>;
    for (const q of questions) {
      if (q.type in typeCounts) typeCounts[q.type] += 1;
    }
    const drift = Object.entries(EXPECTED_MIX).reduce(
      (sum, [type, expected]) => sum + Math.abs((typeCounts[type] ?? 0) - expected),
      0
    );
    if (drift > 3) {
      warnings.push({
        path: "questionIds",
        message: "Question type mix is far from the suggested 5 MCQ / 3 True-False / 2 Picture",
      });
    }
  }

  // Warning: a reference's book doesn't match the world's book range -- caller
  // passes book info on each QuestionSummary; we only warn when we know both.
  return { errors, warnings };
}

export interface LevelQuestionAssignment {
  levelNumber: number;
  questionId: string;
}

/** No question may be used in two levels at once. */
export function findDuplicateQuestionAssignments(
  assignments: LevelQuestionAssignment[]
): ValidationIssue[] {
  const byQuestion = new Map<string, number[]>();
  for (const { levelNumber, questionId } of assignments) {
    const levels = byQuestion.get(questionId) ?? [];
    levels.push(levelNumber);
    byQuestion.set(questionId, levels);
  }
  const issues: ValidationIssue[] = [];
  for (const [questionId, levels] of byQuestion) {
    const distinctLevels = Array.from(new Set(levels));
    if (distinctLevels.length > 1) {
      issues.push({
        path: `questionIds.${questionId}`,
        message: `Question ${questionId} is used in levels ${distinctLevels.sort((a, b) => a - b).join(", ")}`,
      });
    }
  }
  return issues;
}
