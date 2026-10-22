# BibliQ Admin

The content pipeline for BibliQ: write, translate, review and publish trivia questions and levels. The Android app never talks to this app or to Supabase directly -- it only downloads static `manifest.json` / `pack_*.json` / images from CloudFront. Publishing here is what generates those files.

Stack: Next.js (App Router, TypeScript) · Tailwind + shadcn/ui · Supabase (Postgres + Auth) · Amazon S3 + CloudFront · sharp · zod · react-hook-form · TanStack Table · Vitest.

## 1. Local setup

```bash
npm install
cp .env.example .env.local   # fill in the values below
npm run dev
```

Other useful scripts: `npm run build`, `npm test` (Vitest, unit-tests `lib/validation.ts` and `lib/pack-builder.ts`), `npm run lint`.

## 2. Supabase setup

1. Create a project at [supabase.com](https://supabase.com).
2. Run the migration. Either:
   - **Supabase CLI**: `supabase link --project-ref <your-ref>` then `supabase db push`, or
   - **SQL editor**: paste the contents of `supabase/migrations/0001_init.sql` into the Supabase dashboard's SQL editor and run it.

   This creates every table, the approval/audit triggers, RLS policies on every table, and seeds the 9 worlds.

   > This migration was written and reviewed carefully, but not executed against a live Postgres instance in this environment (no Docker available here). Run it against a scratch/dev Supabase project first and report back anything that errors -- plpgsql trigger syntax is the most likely spot for a typo to hide.

3. **Create the first admin.** The app can only invite new admins from inside `/settings`, which needs an existing admin to be signed in -- so the very first one has to be created by hand:
   - In the Supabase dashboard, go to **Authentication → Users → Add user** (or **Invite**) and create your own account.
   - Copy that user's UUID, then in the SQL editor:
     ```sql
     insert into admin_users (user_id, name, role)
     values ('<paste-the-uuid>', 'Your Name', 'admin');
     ```
   - From then on, invite everyone else from `/settings` in the app.
4. Copy **Project Settings → API** into your `.env.local`: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, and the **service_role** key (`SUPABASE_SERVICE_ROLE_KEY` -- keep this one secret, it bypasses RLS).

## 3. Amazon S3 + CloudFront setup

1. **S3 bucket** (e.g. `bibliq-content`): create it with **Block Public Access ON** (all four boxes checked). The bucket is never public -- CloudFront is the only thing allowed to read it.
2. **CloudFront distribution** in front of the bucket, using **Origin Access Control (OAC)**, not a public bucket policy or the older OAI. In the bucket policy, allow `s3:GetObject` only from that distribution (CloudFront's console offers to generate this policy for you when you attach OAC).
3. **CORS**: not needed. Uploads go through this app's server (`app/api/images/upload/route.ts`) using the AWS SDK with your secret keys, never directly from the browser to S3.
4. **Cache behavior**: this app sets `Cache-Control` per object when it uploads (`immutable, max-age=1y` for packs/images, `max-age=300` for `manifest.json`) -- you don't need to configure per-path cache policies in CloudFront itself, just make sure CloudFront is set to respect origin cache headers (the default).
5. **IAM**: create a dedicated IAM user (or role, if deploying somewhere that supports it) for this app, with a policy scoped to only this bucket and distribution:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["s3:PutObject", "s3:GetObject", "s3:ListBucket", "s3:DeleteObject"],
         "Resource": ["arn:aws:s3:::bibliq-content", "arn:aws:s3:::bibliq-content/*"]
       },
       {
         "Effect": "Allow",
         "Action": "cloudfront:CreateInvalidation",
         "Resource": "arn:aws:cloudfront::<account-id>:distribution/<distribution-id>"
       }
     ]
   }
   ```
   Put that user's access key into `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`.
6. Set `NEXT_PUBLIC_CDN_BASE_URL` to the CloudFront domain (or a custom domain like `cdn.bibliq.com` in front of it) -- this is the base URL the Android app downloads from, and what this admin app links to for previewing published files.

## 4. Environment variables

See `.env.example`. `SUPABASE_SERVICE_ROLE_KEY` and the `AWS_*` secrets are server-only: they're read only from files that either import `"server-only"` (`lib/supabase/admin.ts`, `lib/s3.ts`) or run exclusively on the server (Server Actions, Route Handlers). None of them are prefixed `NEXT_PUBLIC_` and none are imported from a Client Component.

## 5. Deploying

Platform-neutral -- this is a standard Next.js app, so either works:

- **Vercel**: connect the repo, set the environment variables in the project settings, done. `npm run build` is the default build command.
- **AWS Amplify**: connect the repo, Amplify auto-detects the Next.js build; add the same environment variables in the Amplify console.

Either way, set `middleware.ts`'s matcher aside (it's already scoped to skip static assets) and make sure the build runs with the production env vars available -- `next build` does type-check and will fail the build on a TypeScript error, so a broken deploy fails loudly rather than shipping silently broken.

## 6. What's implemented vs. simplified

Built and verified (`npm test` passes, `npm run build` succeeds, and the auth gate was smoke-tested against a running server):

- Full DB schema, RLS on every table, the approval-invariant trigger, the reviewer column-lock trigger, and the audit log trigger.
- `lib/validation.ts` and `lib/pack-builder.ts`, both pure and unit-tested.
- Auth (email/password), middleware gate, role-based page access.
- All 9 pages: dashboard, questions (filterable table + bulk actions), question editor (3-language columns, image upload, review panel with comments, live phone preview, autosave, Ctrl+S / Ctrl+Enter), levels (list + editor), images, publish (dry run + real publish pipeline), import, settings (users + general).

Known simplifications, given the scope of this build:

- **Drag-and-drop** (reordering options, assigning questions to level slots) uses up/down buttons and click-to-add instead of a DnD library -- functionally equivalent, less polished.
- **Publish dry run's "diff vs last published pack"** shows question/level counts and the previous version's count, not a line-by-line JSON diff.
- The migration SQL has been carefully re-read but **not executed against a live Postgres** (no Docker in this environment) -- treat the first run against a real Supabase project as the real test.
- `/dashboard`'s "approved, unassigned" and review-queue counts do a few separate queries rather than one aggregate RPC; fine at current content volumes, worth revisiting if it ever feels slow with thousands of questions.
