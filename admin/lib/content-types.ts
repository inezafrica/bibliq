// Shared content shapes. These mirror exactly what the Android app's
// kotlinx.serialization models expect -- do not rename fields casually,
// a mismatch here breaks every client that downloads a pack.

export const SUPPORTED_LANGUAGES = ["en", "fr", "es"] as const;
export type LanguageCode = (typeof SUPPORTED_LANGUAGES)[number];

export const QUESTION_TYPES = [
  "MCQ",
  "TRUE_FALSE",
  "IMAGE",
  "WHO_SAID",
  "WHICH_BOOK",
  "COMPLETE_VERSE",
  "ORDER",
] as const;
export type QuestionType = (typeof QUESTION_TYPES)[number];

export const QUESTION_STATUSES = [
  "draft",
  "in_review",
  "approved",
  "published",
  "retired",
] as const;
export type QuestionStatus = (typeof QUESTION_STATUSES)[number];

export const LEVEL_STATUSES = ["draft", "ready", "published"] as const;
export type LevelStatus = (typeof LEVEL_STATUSES)[number];

export const ADMIN_ROLES = ["admin", "editor", "reviewer"] as const;
export type AdminRole = (typeof ADMIN_ROLES)[number];

export type LocalizedText = Record<LanguageCode, string>;
export type LocalizedOptions = Record<LanguageCode, string[]>;

/** Canonical True/False labels per language -- TRUE_FALSE options must match these exactly. */
export const TRUE_FALSE_LABELS: LocalizedOptions = {
  en: ["True", "False"],
  fr: ["Vrai", "Faux"],
  es: ["Verdadero", "Falso"],
};

export const EXPECTED_OPTION_COUNT: Partial<Record<QuestionType, number>> = {
  MCQ: 4,
  IMAGE: 4,
  TRUE_FALSE: 2,
};

/** The pack-level question shape -- exactly what ships to the app inside pack_XXX.json. */
export interface PackQuestion {
  id: string;
  type: QuestionType;
  difficulty: number;
  text: LocalizedText;
  options: LocalizedOptions;
  correctIndex: number;
  imageUrl?: string;
  reference: LocalizedText;
}

export interface PackLevel {
  number: number;
  world: number;
  isBoss: boolean;
  starThresholds: [number, number, number];
  questionIds: string[];
}

export interface Pack {
  packId: number;
  levels: PackLevel[];
  questions: PackQuestion[];
}

export interface ManifestPackEntry {
  id: number;
  file: string;
  version: number;
  levels: [number, number];
  sha256: string;
}

export interface Manifest {
  schemaVersion: number;
  minAppVersion: number;
  latestLevel: number;
  nextDropDate: string;
  packs: ManifestPackEntry[];
}

/** The content fields of a question as edited in the admin -- DB-shape superset of PackQuestion. */
export interface QuestionContent {
  type: QuestionType;
  difficulty: number;
  text: Partial<LocalizedText>;
  options: Partial<LocalizedOptions>;
  correctIndex: number;
  imageKey: string | null;
  reference: Partial<LocalizedText>;
}

export interface QuestionSummary {
  id: string;
  type: QuestionType;
  status: QuestionStatus;
  correctIndex: number;
  book: string | null;
}

export interface LevelContent {
  number: number;
  world: number;
  isBoss: boolean;
  starThresholds: number[];
  questionIds: string[];
}
