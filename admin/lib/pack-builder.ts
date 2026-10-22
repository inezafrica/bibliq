import { createHash } from "node:crypto";
import { Manifest, ManifestPackEntry, Pack, PackLevel, PackQuestion, QuestionType } from "./content-types";

export interface QuestionRow {
  id: string;
  type: QuestionType;
  difficulty: number;
  text: Record<string, string>;
  options: Record<string, string[]>;
  correctIndex: number;
  imageKey: string | null;
  reference: Record<string, string>;
}

export interface LevelRow {
  number: number;
  world: number;
  isBoss: boolean;
  starThresholds: number[];
  /** Question ids in position order, already resolved from level_questions. */
  questionIds: string[];
}

/**
 * Pure DB-rows -> pack JSON transform. Only includes questions actually
 * referenced by the given levels, in first-appearance order, so the pack
 * never ships orphaned content.
 */
export function buildPack(packId: number, levels: LevelRow[], questions: QuestionRow[]): Pack {
  const questionsById = new Map(questions.map((q) => [q.id, q]));
  const usedIds = new Set<string>();
  const packQuestions: PackQuestion[] = [];

  for (const level of levels) {
    for (const id of level.questionIds) {
      if (usedIds.has(id)) continue;
      const row = questionsById.get(id);
      if (!row) {
        throw new Error(`Level ${level.number} references unknown question id "${id}"`);
      }
      usedIds.add(id);
      packQuestions.push(toPackQuestion(row));
    }
  }

  const packLevels: PackLevel[] = levels
    .slice()
    .sort((a, b) => a.number - b.number)
    .map((level) => ({
      number: level.number,
      world: level.world,
      isBoss: level.isBoss,
      starThresholds: [level.starThresholds[0], level.starThresholds[1], level.starThresholds[2]],
      questionIds: level.questionIds,
    }));

  return { packId, levels: packLevels, questions: packQuestions };
}

function toPackQuestion(row: QuestionRow): PackQuestion {
  const packQuestion: PackQuestion = {
    id: row.id,
    type: row.type,
    difficulty: row.difficulty,
    text: row.text as PackQuestion["text"],
    options: row.options as PackQuestion["options"],
    correctIndex: row.correctIndex,
    reference: row.reference as PackQuestion["reference"],
  };
  if (row.imageKey) {
    packQuestion.imageUrl = row.imageKey;
  }
  return packQuestion;
}

/** Deterministic content hash, used for manifest.packs[].sha256 and S3 integrity checks. */
export function sha256Of(content: string): string {
  return createHash("sha256").update(content, "utf8").digest("hex");
}

export function serializePack(pack: Pack): string {
  return JSON.stringify(pack, null, 2);
}

export interface BuildManifestParams {
  previousManifest: Manifest | null;
  packId: number;
  packFile: string;
  packVersion: number;
  levelRange: [number, number];
  packSha256: string;
  nextDropDate: string;
  schemaVersion?: number;
  minAppVersion?: number;
}

/**
 * Builds the next manifest.json. Republishing the same packId REPLACES its
 * entry in place (new file/version/sha256) rather than appending a second
 * one -- the old pack file on S3 is left untouched, only the manifest moves on.
 */
export function buildManifest(params: BuildManifestParams): Manifest {
  const {
    previousManifest,
    packId,
    packFile,
    packVersion,
    levelRange,
    packSha256,
    nextDropDate,
    schemaVersion,
    minAppVersion,
  } = params;

  const newEntry: ManifestPackEntry = {
    id: packId,
    file: packFile,
    version: packVersion,
    levels: levelRange,
    sha256: packSha256,
  };

  const existingPacks = previousManifest?.packs ?? [];
  const alreadyPresent = existingPacks.some((p) => p.id === packId);
  const packs = alreadyPresent
    ? existingPacks.map((p) => (p.id === packId ? newEntry : p))
    : [...existingPacks, newEntry];

  packs.sort((a, b) => a.id - b.id);

  const latestLevel = Math.max(...packs.map((p) => p.levels[1]));

  return {
    schemaVersion: schemaVersion ?? previousManifest?.schemaVersion ?? 1,
    minAppVersion: minAppVersion ?? previousManifest?.minAppVersion ?? 1,
    latestLevel,
    nextDropDate,
    packs,
  };
}
