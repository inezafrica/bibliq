import { createClient } from "@/lib/supabase/server";
import { cdnUrl } from "@/lib/s3";
import { getImageUsage } from "@/lib/image-usage";
import { ImageCard } from "@/components/image-card";

export default async function ImagesPage() {
  const supabase = await createClient();
  const [{ data: images }, usage] = await Promise.all([
    supabase.from("images").select("key, width, height, bytes, created_at").order("created_at", { ascending: false }),
    getImageUsage(supabase),
  ]);

  const unusedCount = (images ?? []).filter((img) => {
    const u = usage.get(img.key as string);
    return !u?.questionIds.length && !u?.packIds.length;
  }).length;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold text-bibliq-ink">Images</h1>
        <p className="text-sm text-muted-foreground">
          {(images ?? []).length} image(s) in S3 · {unusedCount} unused
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
        {(images ?? []).map((img) => {
          const u = usage.get(img.key as string);
          return (
            <ImageCard
              key={img.key as string}
              imageKey={img.key as string}
              url={cdnUrl(img.key as string)}
              width={img.width as number}
              height={img.height as number}
              bytes={img.bytes as number}
              usedByQuestions={u?.questionIds ?? []}
              inPacks={u?.packIds ?? []}
            />
          );
        })}
        {(images ?? []).length === 0 && (
          <p className="col-span-full text-sm text-muted-foreground">
            No images yet -- upload one from an IMAGE question&apos;s editor.
          </p>
        )}
      </div>
    </div>
  );
}
