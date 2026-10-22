// Pure string helper, safe for both client and server components --
// deliberately NOT in lib/s3.ts, which is server-only.
export function cdnUrl(key: string): string {
  const base = process.env.NEXT_PUBLIC_CDN_BASE_URL ?? "";
  return `${base.replace(/\/$/, "")}/${key}`;
}
