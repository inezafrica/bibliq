"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { requireRole } from "@/lib/auth";
import { Manifest, Pack, QuestionStatus, QuestionType } from "@/lib/content-types";
import {
  findDuplicateQuestionAssignments,
  validateLevel,
  validateQuestionContent,
  ValidationIssue,
} from "@/lib/validation";
import { buildManifest, buildPack, LevelRow, QuestionRow, serializePack, sha256Of } from "@/lib/pack-builder";
import {
  CACHE_CONTROL_IMMUTABLE,
  CACHE_CONTROL_MANIFEST,
  getObjectText,
  invalidateManifest,
  objectExistsInS3,
  uploadToS3,
} from "@/lib/s3";

export interface PublishInput {
  mode: "new" | "republish";
  packId?: number;
  levelNumbers: number[];
  nextDropDate: string;
}

export interface PublishDryRunResult {
  pack: Pack;
  packJson: string;
  manifest: Manifest;
  packFile: string;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
  missingImages: string[];
  packId: number;
  packVersion: number;
  isRepublish: boolean;
  previousQuestionCount: number | null;
}

async function prepare(input: PublishInput) {
  const supabase = await createClient();

  const { data: levelRows } = await supabase
    .from("levels")
    .select("number, world_id, is_boss, star_thresholds")
    .in("number", input.levelNumbers)
    .order("number");

  const { data: levelQuestionRows } = await supabase
    .from("level_questions")
    .select("level_number, question_id, position")
    .in("level_number", input.levelNumbers)
    .order("position");

  const questionIds = Array.from(new Set((levelQuestionRows ?? []).map((r) => r.question_id as string)));

  const { data: questionRows } = await supabase
    .from("questions")
    .select("id, type, difficulty, text, options, correct_index, reference, image_key, status, book")
    .in("id", questionIds.length > 0 ? questionIds : ["__none__"]);

  const levels: LevelRow[] = (levelRows ?? []).map((l) => ({
    number: l.number as number,
    world: l.world_id as number,
    isBoss: l.is_boss as boolean,
    starThresholds: l.star_thresholds as number[],
    questionIds: (levelQuestionRows ?? [])
      .filter((lq) => lq.level_number === l.number)
      .map((lq) => lq.question_id as string),
  }));

  const questionRowById = new Map((questionRows ?? []).map((q) => [q.id as string, q]));

  const questions: QuestionRow[] = (questionRows ?? []).map((q) => ({
    id: q.id as string,
    type: q.type as QuestionType,
    difficulty: q.difficulty as number,
    text: q.text as Record<string, string>,
    options: q.options as Record<string, string[]>,
    correctIndex: q.correct_index as number,
    imageKey: q.image_key as string | null,
    reference: q.reference as Record<string, string>,
  }));

  const errors: ValidationIssue[] = [];
  const warnings: ValidationIssue[] = [];

  for (const level of levels) {
    const summaries = level.questionIds.map((id) => {
      const q = questionRowById.get(id);
      return {
        id,
        type: (q?.type as QuestionType) ?? "MCQ",
        status: (q?.status as QuestionStatus) ?? "draft",
        correctIndex: (q?.correct_index as number) ?? 0,
        book: (q?.book as string | null) ?? null,
      };
    });
    const result = validateLevel(
      { number: level.number, world: level.world, isBoss: level.isBoss, starThresholds: level.starThresholds, questionIds: level.questionIds },
      summaries
    );
    errors.push(...result.errors.map((e) => ({ ...e, message: `Level ${level.number}: ${e.message}` })));
    warnings.push(...result.warnings.map((w) => ({ ...w, message: `Level ${level.number}: ${w.message}` })));
  }

  const { data: allAssignments } = await supabase.from("level_questions").select("level_number, question_id");
  errors.push(
    ...findDuplicateQuestionAssignments(
      (allAssignments ?? []).map((a) => ({ levelNumber: a.level_number as number, questionId: a.question_id as string }))
    )
  );

  const missingImages: string[] = [];
  for (const q of questions) {
    let imageExists: boolean | undefined;
    if (q.imageKey) {
      imageExists = await objectExistsInS3(q.imageKey);
      if (!imageExists) missingImages.push(q.imageKey);
    }
    const issues = validateQuestionContent(
      {
        type: q.type,
        difficulty: q.difficulty,
        text: q.text,
        options: q.options,
        correctIndex: q.correctIndex,
        imageKey: q.imageKey,
        reference: q.reference,
      },
      { imageExistsInS3: imageExists }
    );
    errors.push(...issues.map((i) => ({ path: `${q.id}.${i.path}`, message: `${q.id}: ${i.message}` })));
  }

  return { levels, questions, errors, warnings, missingImages };
}

export async function dryRunPublish(input: PublishInput): Promise<PublishDryRunResult> {
  await requireRole(["admin"]);
  const supabase = await createClient();
  const { levels, questions, errors, warnings, missingImages } = await prepare(input);

  let packId: number;
  let packVersion: number;
  let isRepublish = false;
  let previousQuestionCount: number | null = null;

  if (input.mode === "republish" && input.packId) {
    packId = input.packId;
    isRepublish = true;
    const { data: existing } = await supabase
      .from("packs")
      .select("version, question_count")
      .eq("id", packId)
      .order("version", { ascending: false })
      .limit(1);
    packVersion = ((existing?.[0]?.version as number) ?? 0) + 1;
    previousQuestionCount = (existing?.[0]?.question_count as number) ?? null;
  } else {
    const { data: maxPack } = await supabase.from("packs").select("id").order("id", { ascending: false }).limit(1);
    packId = ((maxPack?.[0]?.id as number) ?? 0) + 1;
    packVersion = 1;
  }

  const pack = buildPack(packId, levels, questions);
  const packJson = serializePack(pack);
  const packSha256 = sha256Of(packJson);
  const packFile = `packs/pack_${String(packId).padStart(3, "0")}_v${packVersion}.json`;

  const manifestText = await getObjectText("manifest.json");
  const previousManifest: Manifest | null = manifestText ? (JSON.parse(manifestText) as Manifest) : null;

  const levelNumbers = levels.map((l) => l.number);
  const manifest =
    levelNumbers.length > 0
      ? buildManifest({
          previousManifest,
          packId,
          packFile,
          packVersion,
          levelRange: [Math.min(...levelNumbers), Math.max(...levelNumbers)],
          packSha256,
          nextDropDate: input.nextDropDate,
        })
      : previousManifest ?? {
          schemaVersion: 1,
          minAppVersion: 1,
          latestLevel: 0,
          nextDropDate: input.nextDropDate,
          packs: [],
        };

  if (levelNumbers.length === 0) {
    errors.push({ path: "levelNumbers", message: "Select at least one level to publish" });
  }

  return {
    pack,
    packJson,
    manifest,
    packFile,
    errors,
    warnings,
    missingImages,
    packId,
    packVersion,
    isRepublish,
    previousQuestionCount,
  };
}

export async function publishPack(input: PublishInput) {
  const admin = await requireRole(["admin"]);
  const dryRun = await dryRunPublish(input);
  if (dryRun.errors.length > 0) {
    return { error: `${dryRun.errors.length} validation error(s) remain -- fix them before publishing.`, result: dryRun };
  }

  const supabase = await createClient();

  // 1. Images already confirmed to exist during the dry run above.
  // 2. Upload the pack -- immutable, never overwritten (packFile embeds the version).
  await uploadToS3({
    key: dryRun.packFile,
    body: Buffer.from(dryRun.packJson, "utf8"),
    contentType: "application/json",
    cacheControl: CACHE_CONTROL_IMMUTABLE,
  });

  // 3. Upload the manifest LAST, with a short cache lifetime.
  await uploadToS3({
    key: "manifest.json",
    body: Buffer.from(JSON.stringify(dryRun.manifest, null, 2), "utf8"),
    contentType: "application/json",
    cacheControl: CACHE_CONTROL_MANIFEST,
  });

  // 4. Invalidate /manifest.json only.
  await invalidateManifest();

  // 5. Mark questions/levels published, record the pack, write audit_log
  // (the audit trigger fires automatically on these inserts/updates).
  const levelNumbers = dryRun.pack.levels.map((l) => l.number);
  const questionIds = dryRun.pack.questions.map((q) => q.id);

  await supabase.from("questions").update({ status: "published" }).in("id", questionIds);
  await supabase.from("levels").update({ status: "published", pack_id: dryRun.packId }).in("number", levelNumbers);
  await supabase.from("packs").insert({
    id: dryRun.packId,
    version: dryRun.packVersion,
    level_from: Math.min(...levelNumbers),
    level_to: Math.max(...levelNumbers),
    s3_key: dryRun.packFile,
    sha256: sha256Of(dryRun.packJson),
    question_count: dryRun.pack.questions.length,
    published_by: admin.userId,
  });
  await supabase.from("settings").update({ value: input.nextDropDate }).eq("key", "next_drop_date");

  revalidatePath("/publish");
  revalidatePath("/dashboard");
  revalidatePath("/levels");
  revalidatePath("/questions");

  return { error: null, result: dryRun };
}
