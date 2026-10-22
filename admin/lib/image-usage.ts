import "server-only";
import type { SupabaseClient } from "@supabase/supabase-js";
import type { Pack } from "./content-types";
import { getObjectText } from "./s3";

export interface ImageUsage {
  /** Questions whose image_key currently points at this image. */
  questionIds: string[];
  /** Live packs (latest version of each pack id) that ship this image to players. */
  packIds: number[];
}

/**
 * Where each image is actually referenced right now. images.used_by_question
 * only records which question an image was uploaded for, and goes stale once
 * the image is replaced or removed, so it can't decide whether deleting is safe.
 */
export async function getImageUsage(supabase: SupabaseClient): Promise<Map<string, ImageUsage>> {
  const usage = new Map<string, ImageUsage>();
  const entry = (key: string) => {
    let value = usage.get(key);
    if (!value) {
      value = { questionIds: [], packIds: [] };
      usage.set(key, value);
    }
    return value;
  };

  const { data: questions } = await supabase.from("questions").select("id, image_key").not("image_key", "is", null);
  for (const q of questions ?? []) {
    entry(q.image_key as string).questionIds.push(q.id as string);
  }

  const { data: packRows } = await supabase.from("packs").select("id, version, s3_key").order("version", { ascending: false });
  const latestByPack = new Map<number, string>();
  for (const row of packRows ?? []) {
    if (!latestByPack.has(row.id as number)) latestByPack.set(row.id as number, row.s3_key as string);
  }

  for (const [packId, s3Key] of latestByPack) {
    const text = await getObjectText(s3Key);
    if (!text) continue;
    const pack = JSON.parse(text) as Pack;
    for (const q of pack.questions) {
      if (q.imageUrl && !entry(q.imageUrl).packIds.includes(packId)) {
        entry(q.imageUrl).packIds.push(packId);
      }
    }
  }

  return usage;
}
