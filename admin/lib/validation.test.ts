import { describe, expect, it } from "vitest";
import {
  findDuplicateQuestionAssignments,
  validateLevel,
  validateQuestionContent,
} from "./validation";
import { QuestionContent, QuestionSummary } from "./content-types";

function mcq(overrides: Partial<QuestionContent> = {}): QuestionContent {
  return {
    type: "MCQ",
    difficulty: 1,
    text: { en: "Who built the ark?", fr: "Qui a construit l'arche ?", es: "¿Quién construyó el arca?" },
    options: {
      en: ["Noah", "Abraham", "Moses", "David"],
      fr: ["Noé", "Abraham", "Moïse", "David"],
      es: ["Noé", "Abraham", "Moisés", "David"],
    },
    correctIndex: 0,
    imageKey: null,
    reference: { en: "Genesis 6:14", fr: "Genèse 6:14", es: "Génesis 6:14" },
    ...overrides,
  };
}

describe("validateQuestionContent", () => {
  it("accepts a well-formed MCQ question", () => {
    expect(validateQuestionContent(mcq())).toEqual([]);
  });

  it("flags missing Spanish text", () => {
    const q = mcq({ text: { en: "Who built the ark?", fr: "Qui a construit l'arche ?", es: "" } });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "text.es")).toBe(true);
  });

  it("flags options out of order / wrong option count", () => {
    const q = mcq({ options: { ...mcq().options, fr: ["Noé", "Abraham", "Moïse"] } });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "options.fr")).toBe(true);
  });

  it("flags duplicate options within one language", () => {
    const q = mcq({ options: { ...mcq().options, en: ["Noah", "Noah", "Moses", "David"] } });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "options.en" && i.message.includes("Duplicate"))).toBe(true);
  });

  it("flags correctIndex out of range", () => {
    const q = mcq({ correctIndex: 9 });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "correctIndex")).toBe(true);
  });

  it("requires TRUE_FALSE options to be the exact canonical labels", () => {
    const q = mcq({
      type: "TRUE_FALSE",
      options: { en: ["Yes", "No"], fr: ["Vrai", "Faux"], es: ["Verdadero", "Falso"] },
    });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "options.en")).toBe(true);
  });

  it("accepts canonical TRUE_FALSE labels in every language", () => {
    const q = mcq({
      type: "TRUE_FALSE",
      options: { en: ["True", "False"], fr: ["Vrai", "Faux"], es: ["Verdadero", "Falso"] },
      correctIndex: 1,
    });
    expect(validateQuestionContent(q)).toEqual([]);
  });

  it("requires an image key for IMAGE questions", () => {
    const q = mcq({ type: "IMAGE", imageKey: null });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "imageKey")).toBe(true);
  });

  it("flags a missing S3 file for an IMAGE question", () => {
    const q = mcq({ type: "IMAGE", imageKey: "images/q_0001_v1.webp" });
    const issues = validateQuestionContent(q, { imageExistsInS3: false });
    expect(issues.some((i) => i.path === "imageKey" && i.message.includes("not found"))).toBe(true);
  });

  it("rejects a non-IMAGE question that has an image key", () => {
    const q = mcq({ type: "MCQ", imageKey: "images/q_0001_v1.webp" });
    const issues = validateQuestionContent(q);
    expect(issues.some((i) => i.path === "imageKey")).toBe(true);
  });
});

function summary(overrides: Partial<QuestionSummary> = {}): QuestionSummary {
  return { id: "q_0001", type: "MCQ", status: "approved", correctIndex: 1, book: "Genesis", ...overrides };
}

describe("validateLevel", () => {
  const baseLevel = {
    number: 1,
    world: 1,
    isBoss: false,
    starThresholds: [7, 9, 10],
    questionIds: Array.from({ length: 10 }, (_, i) => `q_${String(i + 1).padStart(4, "0")}`),
  };

  function approvedQuestions(count: number): QuestionSummary[] {
    return Array.from({ length: count }, (_, i) =>
      summary({ id: `q_${String(i + 1).padStart(4, "0")}`, correctIndex: i % 4 })
    );
  }

  it("passes a well-formed 10-question level", () => {
    const result = validateLevel(baseLevel, approvedQuestions(10));
    expect(result.errors).toEqual([]);
  });

  it("requires exactly 10 questions for a non-boss level", () => {
    const level = { ...baseLevel, questionIds: baseLevel.questionIds.slice(0, 8) };
    const result = validateLevel(level, approvedQuestions(8));
    expect(result.errors.some((e) => e.message.includes("exactly 10"))).toBe(true);
  });

  it("requires exactly 15 questions for a boss level", () => {
    const level = { ...baseLevel, isBoss: true };
    const result = validateLevel(level, approvedQuestions(10));
    expect(result.errors.some((e) => e.message.includes("exactly 15"))).toBe(true);
  });

  it("rejects a level containing a non-approved question", () => {
    const questions = approvedQuestions(10);
    questions[0] = { ...questions[0], status: "draft" };
    const result = validateLevel(baseLevel, questions);
    expect(result.errors.some((e) => e.message.includes("Not approved"))).toBe(true);
  });

  it("rejects descending star thresholds", () => {
    const level = { ...baseLevel, starThresholds: [9, 7, 10] };
    const result = validateLevel(level, approvedQuestions(10));
    expect(result.errors.some((e) => e.path === "starThresholds")).toBe(true);
  });

  it("rejects a 3-star threshold above the question count", () => {
    const level = { ...baseLevel, starThresholds: [7, 9, 11] };
    const result = validateLevel(level, approvedQuestions(10));
    expect(result.errors.some((e) => e.path === "starThresholds")).toBe(true);
  });

  it("warns when the correct answer sits in the same position too often", () => {
    const questions = approvedQuestions(10).map((q) => ({ ...q, correctIndex: 0 }));
    const result = validateLevel(baseLevel, questions);
    expect(result.warnings.some((w) => w.message.includes("position 1"))).toBe(true);
  });
});

describe("findDuplicateQuestionAssignments", () => {
  it("flags a question used in two levels", () => {
    const issues = findDuplicateQuestionAssignments([
      { levelNumber: 1, questionId: "q_0001" },
      { levelNumber: 2, questionId: "q_0001" },
      { levelNumber: 2, questionId: "q_0002" },
    ]);
    expect(issues).toHaveLength(1);
    expect(issues[0].message).toContain("levels 1, 2");
  });

  it("passes when every question is used at most once", () => {
    const issues = findDuplicateQuestionAssignments([
      { levelNumber: 1, questionId: "q_0001" },
      { levelNumber: 2, questionId: "q_0002" },
    ]);
    expect(issues).toEqual([]);
  });
});
