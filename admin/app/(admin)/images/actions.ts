"use server";

import { revalidatePath } from "next/cache";
import { createClient } from "@/lib/supabase/server";
import { requireRole } from "@/lib/auth";
import { deleteFromS3 } from "@/lib/s3";
import { getImageUsage } from "@/lib/image-usage";

export async function deleteImage(key: string) {
  await requireRole(["admin", "editor"]);
  const supabase = await createClient();

  const usage = (await getImageUsage(supabase)).get(key);
  if (usage?.questionIds.length) {
    return { error: `Still used by ${usage.questionIds.join(", ")} -- remove or replace it in the question first` };
  }
  if (usage?.packIds.length) {
    return {
      error: `Players still download this image in published pack ${usage.packIds.join(", ")} -- republish that pack first`,
    };
  }

  const { error: dbError } = await supabase.from("images").delete().eq("key", key);
  if (dbError) return { error: dbError.message };

  await deleteFromS3(key);
  revalidatePath("/images");
  return { error: null };
}
