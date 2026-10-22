import "server-only";
import {
  DeleteObjectCommand,
  GetObjectCommand,
  HeadObjectCommand,
  ListObjectsV2Command,
  PutObjectCommand,
  S3Client,
} from "@aws-sdk/client-s3";
import { CloudFrontClient, CreateInvalidationCommand } from "@aws-sdk/client-cloudfront";

const s3 = new S3Client({ region: process.env.AWS_REGION });
const cloudfront = new CloudFrontClient({ region: process.env.AWS_REGION });

const BUCKET = process.env.S3_BUCKET!;

export const CACHE_CONTROL_IMMUTABLE = "public, max-age=31536000, immutable";
export const CACHE_CONTROL_MANIFEST = "public, max-age=300";

export interface UploadParams {
  key: string;
  body: Buffer;
  contentType: string;
  cacheControl: string;
}

/** Never call this twice for the same key -- packs and images are immutable once written. */
export async function uploadToS3({ key, body, contentType, cacheControl }: UploadParams): Promise<void> {
  await s3.send(
    new PutObjectCommand({
      Bucket: BUCKET,
      Key: key,
      Body: body,
      ContentType: contentType,
      CacheControl: cacheControl,
    })
  );
}

export async function objectExistsInS3(key: string): Promise<boolean> {
  try {
    await s3.send(new HeadObjectCommand({ Bucket: BUCKET, Key: key }));
    return true;
  } catch (err: unknown) {
    const status = (err as { $metadata?: { httpStatusCode?: number }; name?: string })?.$metadata?.httpStatusCode;
    const name = (err as { name?: string })?.name;
    if (status === 404 || name === "NotFound") return false;
    throw err;
  }
}

/** Returns null when the key doesn't exist, instead of throwing. */
export async function getObjectText(key: string): Promise<string | null> {
  try {
    const result = await s3.send(new GetObjectCommand({ Bucket: BUCKET, Key: key }));
    return (await result.Body?.transformToString()) ?? null;
  } catch (err: unknown) {
    const status = (err as { $metadata?: { httpStatusCode?: number }; name?: string })?.$metadata?.httpStatusCode;
    const name = (err as { name?: string })?.name;
    if (status === 404 || name === "NoSuchKey") return null;
    throw err;
  }
}

export async function deleteFromS3(key: string): Promise<void> {
  await s3.send(new DeleteObjectCommand({ Bucket: BUCKET, Key: key }));
}

export interface S3ObjectInfo {
  key: string;
  size: number;
  lastModified?: Date;
}

export async function listS3Objects(prefix: string): Promise<S3ObjectInfo[]> {
  const objects: S3ObjectInfo[] = [];
  let continuationToken: string | undefined;

  do {
    const result = await s3.send(
      new ListObjectsV2Command({ Bucket: BUCKET, Prefix: prefix, ContinuationToken: continuationToken })
    );
    for (const obj of result.Contents ?? []) {
      if (obj.Key) objects.push({ key: obj.Key, size: obj.Size ?? 0, lastModified: obj.LastModified });
    }
    continuationToken = result.NextContinuationToken;
  } while (continuationToken);

  return objects;
}

export { cdnUrl } from "./cdn";

/**
 * Publishing only ever invalidates /manifest.json -- packs and images are versioned by filename instead.
 * Best-effort: the manifest is served with a 5-minute max-age, so a failed
 * invalidation only delays the rollout and must not abort the publish.
 */
export async function invalidateManifest(): Promise<void> {
  try {
    await cloudfront.send(
      new CreateInvalidationCommand({
        DistributionId: process.env.CLOUDFRONT_DISTRIBUTION_ID!,
        InvalidationBatch: {
          // CallerReference only needs to be unique per invalidation; a UUID
          // avoids Date.now() which Next.js flags as unstable during prerender.
          CallerReference: `manifest-${crypto.randomUUID()}`,
          Paths: { Quantity: 1, Items: ["/manifest.json"] },
        },
      })
    );
  } catch (err) {
    console.warn("CloudFront invalidation of /manifest.json failed; clients pick it up within 5 minutes.", err);
  }
}
