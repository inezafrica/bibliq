import { createHash } from "node:crypto";
import { NextRequest, NextResponse } from "next/server";
import sharp from "sharp";
import { createClient } from "@/lib/supabase/server";
import { CACHE_CONTROL_IMMUTABLE, uploadToS3 } from "@/lib/s3";

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;
const MAX_OUTPUT_BYTES = 150 * 1024;
const ACCEPTED_TYPES = ["image/png", "image/jpeg", "image/webp"];

export async function POST(request: NextRequest) {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const form = await request.formData();
  const file = form.get("file");
  const questionId = form.get("questionId");

  if (!(file instanceof File) || typeof questionId !== "string" || !questionId) {
    return NextResponse.json({ error: "file and questionId are required" }, { status: 400 });
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return NextResponse.json({ error: "File too large (max 10 MB)" }, { status: 400 });
  }
  if (!ACCEPTED_TYPES.includes(file.type)) {
    return NextResponse.json({ error: "Only PNG, JPG or WebP are accepted" }, { status: 400 });
  }

  const inputBuffer = Buffer.from(await file.arrayBuffer());

  let outputBuffer: Buffer;
  try {
    outputBuffer = await sharp(inputBuffer)
      .rotate() // auto-orient from EXIF
      .resize({ width: 800, height: 600, fit: "cover" })
      .webp({ quality: 80 })
      .toBuffer(); // .withMetadata() is never called, so EXIF/ICC metadata is stripped by default
  } catch {
    return NextResponse.json({ error: "Could not process this image" }, { status: 400 });
  }

  if (outputBuffer.byteLength > MAX_OUTPUT_BYTES) {
    return NextResponse.json(
      { error: "Image is too complex to compress under 150 KB -- try a simpler image" },
      { status: 400 }
    );
  }

  const { data: existingImages } = await supabase
    .from("images")
    .select("key")
    .like("key", `images/${questionId}_v%`);

  const versions = (existingImages ?? []).map((row) => {
    const match = /_v(\d+)\.webp$/.exec(row.key as string);
    return match ? Number(match[1]) : 0;
  });
  const nextVersion = versions.length > 0 ? Math.max(...versions) + 1 : 1;
  const key = `images/${questionId}_v${nextVersion}.webp`;

  await uploadToS3({
    key,
    body: outputBuffer,
    contentType: "image/webp",
    cacheControl: CACHE_CONTROL_IMMUTABLE,
  });

  const sha256 = createHash("sha256").update(outputBuffer).digest("hex");
  const metadata = await sharp(outputBuffer).metadata();

  const { error: insertError } = await supabase.from("images").insert({
    key,
    width: metadata.width ?? 0,
    height: metadata.height ?? 0,
    bytes: outputBuffer.byteLength,
    sha256,
    uploaded_by: user.id,
    used_by_question: questionId,
  });
  if (insertError) {
    return NextResponse.json({ error: insertError.message }, { status: 500 });
  }

  return NextResponse.json({ key, width: metadata.width, height: metadata.height, bytes: outputBuffer.byteLength });
}
