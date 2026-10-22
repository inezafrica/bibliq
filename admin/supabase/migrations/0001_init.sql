-- BibliQ admin -- initial schema, RLS and triggers.
-- Run with the Supabase CLI: supabase db push
-- or paste into the SQL editor in the Supabase dashboard.

-- Supabase projects have this enabled already; kept here so the migration
-- is self-contained against a plain Postgres instance too.
create extension if not exists pgcrypto;

-- ===========================================================================
-- Tables
-- ===========================================================================

create table admin_users (
  user_id    uuid primary key references auth.users (id) on delete cascade,
  name       text not null,
  role       text not null check (role in ('admin', 'editor', 'reviewer')),
  created_at timestamptz not null default now()
);

create table worlds (
  id    int primary key,
  name  jsonb not null, -- {"en": "...", "fr": "...", "es": "..."}
  books text not null,
  color text not null,
  sort  int not null
);

create table levels (
  number           int primary key,
  world_id         int not null references worlds (id),
  is_boss          boolean not null default false,
  star_thresholds  int[] not null default '{7,9,10}',
  status           text not null default 'draft' check (status in ('draft', 'ready', 'published')),
  pack_id          int null,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);

create sequence question_id_seq start 1;

create table questions (
  id              text primary key default ('q_' || lpad(nextval('question_id_seq')::text, 4, '0')),
  type            text not null check (type in ('MCQ', 'TRUE_FALSE', 'IMAGE', 'WHO_SAID', 'WHICH_BOOK', 'COMPLETE_VERSE', 'ORDER')),
  difficulty      int not null check (difficulty between 1 and 5),
  text            jsonb not null default '{}'::jsonb,
  options         jsonb not null default '{}'::jsonb,
  correct_index   int not null default 0,
  reference       jsonb not null default '{}'::jsonb,
  image_key       text null,
  book            text null,
  chapter_from    int null,
  chapter_to      int null,
  notes           text null,
  source          text not null default 'manual' check (source in ('manual', 'import')),

  check_bible     boolean not null default false,
  check_bible_by  uuid null references admin_users (user_id),
  check_bible_at  timestamptz null,
  check_en        boolean not null default false,
  check_en_by     uuid null references admin_users (user_id),
  check_en_at     timestamptz null,
  check_fr        boolean not null default false,
  check_fr_by     uuid null references admin_users (user_id),
  check_fr_at     timestamptz null,
  check_es        boolean not null default false,
  check_es_by     uuid null references admin_users (user_id),
  check_es_at     timestamptz null,

  status          text not null default 'draft' check (status in ('draft', 'in_review', 'approved', 'published', 'retired')),
  created_by      uuid null references admin_users (user_id),
  updated_by      uuid null references admin_users (user_id),
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

create table level_questions (
  level_number int not null references levels (number) on delete cascade,
  question_id  text not null references questions (id) on delete restrict,
  position     int not null,
  primary key (level_number, position),
  unique (level_number, question_id)
);

create table images (
  key             text primary key,
  width           int not null,
  height          int not null,
  bytes           int not null,
  sha256          text not null,
  uploaded_by     uuid null references admin_users (user_id),
  created_at      timestamptz not null default now(),
  used_by_question text null references questions (id)
);

create table packs (
  id              int not null,
  version         int not null,
  level_from      int not null,
  level_to        int not null,
  s3_key          text not null,
  sha256          text not null,
  question_count  int not null,
  published_by    uuid null references admin_users (user_id),
  published_at    timestamptz not null default now(),
  primary key (id, version)
);

create table settings (
  key   text primary key,
  value jsonb not null
);

create table comments (
  id          uuid primary key default gen_random_uuid(),
  question_id text not null references questions (id) on delete cascade,
  author      uuid null references admin_users (user_id),
  body        text not null,
  resolved    boolean not null default false,
  created_at  timestamptz not null default now()
);

create table audit_log (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid null references admin_users (user_id),
  action     text not null,
  table_name text not null,
  row_id     text not null,
  before     jsonb null,
  after      jsonb null,
  created_at timestamptz not null default now()
);

-- ===========================================================================
-- Indexes
-- ===========================================================================

create index idx_questions_status on questions (status);
create index idx_questions_type on questions (type);
create index idx_questions_book on questions (book);
create index idx_questions_difficulty on questions (difficulty);
create index idx_levels_world on levels (world_id);
create index idx_levels_status on levels (status);
create index idx_level_questions_question on level_questions (question_id);
create index idx_images_used_by on images (used_by_question);
create index idx_comments_question on comments (question_id);
create index idx_audit_log_row on audit_log (table_name, row_id);
create index idx_audit_log_user on audit_log (user_id);

-- ===========================================================================
-- Seed: worlds 1-9
-- ===========================================================================

insert into worlds (id, name, books, color, sort) values
  (1, '{"en":"Creation & the Garden","fr":"La Création et le Jardin","es":"La Creación y el Jardín"}', 'Genesis 1-11', '#4E9A2E', 1),
  (2, '{"en":"The Patriarchs","fr":"Les Patriarches","es":"Los Patriarcas"}', 'Genesis 12-50', '#D99A12', 2),
  (3, '{"en":"Exodus & the Desert","fr":"L''Exode et le Désert","es":"El Éxodo y el Desierto"}', 'Exodus-Deuteronomy', '#F5B82E', 3),
  (4, '{"en":"The Promised Land","fr":"La Terre Promise","es":"La Tierra Prometida"}', 'Joshua, Judges, Ruth', '#8FD16E', 4),
  (5, '{"en":"Kings & Kingdoms","fr":"Rois et Royaumes","es":"Reyes y Reinos"}', 'Samuel, Kings, Chronicles', '#2B4C9B', 5),
  (6, '{"en":"Poets & Prophets","fr":"Poètes et Prophètes","es":"Poetas y Profetas"}', 'Psalms, Proverbs, Isaiah, Daniel', '#5A3220', 6),
  (7, '{"en":"The Gospels","fr":"Les Évangiles","es":"Los Evangelios"}', 'Matthew-John', '#8EC5FF', 7),
  (8, '{"en":"The Early Church","fr":"L''Église des Origines","es":"La Iglesia Primitiva"}', 'Acts & Letters', '#F2665A', 8),
  (9, '{"en":"Revelation","fr":"L''Apocalypse","es":"El Apocalipsis"}', 'Revelation', '#1E3670', 9);

insert into settings (key, value) values
  ('next_drop_date', '"2026-10-16"'),
  ('min_app_version', '1'),
  ('schema_version', '1');

-- ===========================================================================
-- Helper: current admin role (security definer so RLS policies that call it
-- don't recurse into admin_users' own RLS).
-- ===========================================================================

create or replace function current_admin_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select role from admin_users where user_id = auth.uid();
$$;

create or replace function is_admin_user()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (select 1 from admin_users where user_id = auth.uid());
$$;

-- ===========================================================================
-- Trigger: questions -- approval invariant + reviewer column lock
-- ===========================================================================

create or replace function questions_before_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  actor_role text := current_admin_role();
begin
  -- Reviewers may only touch the four review-check columns (and whatever
  -- side effects this trigger itself applies to status/timestamps).
  if actor_role = 'reviewer' then
    if NEW.type is distinct from OLD.type
      or NEW.difficulty is distinct from OLD.difficulty
      or NEW.text is distinct from OLD.text
      or NEW.options is distinct from OLD.options
      or NEW.correct_index is distinct from OLD.correct_index
      or NEW.reference is distinct from OLD.reference
      or NEW.image_key is distinct from OLD.image_key
      or NEW.book is distinct from OLD.book
      or NEW.chapter_from is distinct from OLD.chapter_from
      or NEW.chapter_to is distinct from OLD.chapter_to
      or NEW.notes is distinct from OLD.notes
    then
      raise exception 'Reviewers can only update review checks and comments';
    end if;
  end if;

  -- Editing an approved question's content resets the affected checks and
  -- drops it back to in_review. correct_index/type changes affect every
  -- language's "is this biblically + linguistically correct" check.
  if OLD.status = 'approved' then
    if NEW.correct_index is distinct from OLD.correct_index or NEW.type is distinct from OLD.type then
      NEW.check_bible := false;
      NEW.check_en := false;
      NEW.check_fr := false;
      NEW.check_es := false;
    end if;

    if NEW.reference is distinct from OLD.reference then
      NEW.check_bible := false;
    end if;

    if (NEW.text ->> 'en') is distinct from (OLD.text ->> 'en')
      or (NEW.options -> 'en') is distinct from (OLD.options -> 'en') then
      NEW.check_bible := false;
      NEW.check_en := false;
    end if;
    if (NEW.text ->> 'fr') is distinct from (OLD.text ->> 'fr')
      or (NEW.options -> 'fr') is distinct from (OLD.options -> 'fr') then
      NEW.check_bible := false;
      NEW.check_fr := false;
    end if;
    if (NEW.text ->> 'es') is distinct from (OLD.text ->> 'es')
      or (NEW.options -> 'es') is distinct from (OLD.options -> 'es') then
      NEW.check_bible := false;
      NEW.check_es := false;
    end if;

    if not (NEW.check_bible and NEW.check_en and NEW.check_fr and NEW.check_es) then
      NEW.status := 'in_review';
    end if;
  end if;

  -- Stamp who/when a check flipped, clear it when a check flips back off.
  if NEW.check_bible and not OLD.check_bible then
    NEW.check_bible_by := auth.uid(); NEW.check_bible_at := now();
  elsif not NEW.check_bible and OLD.check_bible then
    NEW.check_bible_by := null; NEW.check_bible_at := null;
  end if;
  if NEW.check_en and not OLD.check_en then
    NEW.check_en_by := auth.uid(); NEW.check_en_at := now();
  elsif not NEW.check_en and OLD.check_en then
    NEW.check_en_by := null; NEW.check_en_at := null;
  end if;
  if NEW.check_fr and not OLD.check_fr then
    NEW.check_fr_by := auth.uid(); NEW.check_fr_at := now();
  elsif not NEW.check_fr and OLD.check_fr then
    NEW.check_fr_by := null; NEW.check_fr_at := null;
  end if;
  if NEW.check_es and not OLD.check_es then
    NEW.check_es_by := auth.uid(); NEW.check_es_at := now();
  elsif not NEW.check_es and OLD.check_es then
    NEW.check_es_by := null; NEW.check_es_at := null;
  end if;

  -- The approval invariant: status can never sit at 'draft'/'in_review' while
  -- every check is true, and can never be 'approved' while any check is false.
  -- Checked against NEW.status (not OLD.status) so this holds no matter what
  -- a caller explicitly tries to set status to in the same statement -- e.g.
  -- a client re-sending "in_review" on an already-approved row gets silently
  -- corrected back to 'approved' instead of actually regressing it.
  if NEW.check_bible and NEW.check_en and NEW.check_fr and NEW.check_es then
    if NEW.status in ('draft', 'in_review') then
      NEW.status := 'approved';
    end if;
  elsif NEW.status = 'approved' then
    raise exception 'A question can only become approved once all four checks are true';
  end if;

  NEW.updated_by := auth.uid();
  NEW.updated_at := now();
  return NEW;
end;
$$;

create trigger trg_questions_before_update
  before update on questions
  for each row execute function questions_before_update();

create or replace function questions_before_insert()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  NEW.created_by := auth.uid();
  NEW.updated_by := auth.uid();
  return NEW;
end;
$$;

create trigger trg_questions_before_insert
  before insert on questions
  for each row execute function questions_before_insert();

-- Importing a pack (e.g. the original pack_001.json) inserts explicit
-- "q_0001"-style ids, which bypasses question_id_seq entirely -- call this
-- afterwards so the NEXT auto-generated id doesn't collide with one just
-- imported.
create or replace function sync_question_id_sequence()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  max_num int;
begin
  select coalesce(max(substring(id from 3)::int), 0) into max_num
  from questions
  where id ~ '^q_[0-9]+$';
  perform setval('question_id_seq', max_num);
end;
$$;

-- ===========================================================================
-- Trigger: generic audit log on every write to the content tables
-- ===========================================================================

create or replace function audit_log_trigger()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  rid text;
  before_json jsonb;
  after_json jsonb;
begin
  if TG_OP = 'DELETE' then
    before_json := to_jsonb(OLD);
    after_json := null;
    rid := coalesce(before_json ->> 'id', before_json ->> 'key', before_json ->> 'number', before_json ->> 'level_number', before_json ->> 'user_id');
  elsif TG_OP = 'INSERT' then
    before_json := null;
    after_json := to_jsonb(NEW);
    rid := coalesce(after_json ->> 'id', after_json ->> 'key', after_json ->> 'number', after_json ->> 'level_number', after_json ->> 'user_id');
  else
    before_json := to_jsonb(OLD);
    after_json := to_jsonb(NEW);
    rid := coalesce(after_json ->> 'id', after_json ->> 'key', after_json ->> 'number', after_json ->> 'level_number', after_json ->> 'user_id');
  end if;

  insert into audit_log (user_id, action, table_name, row_id, before, after)
  values (auth.uid(), TG_OP, TG_TABLE_NAME, rid, before_json, after_json);

  if TG_OP = 'DELETE' then
    return OLD;
  end if;
  return NEW;
end;
$$;

create trigger trg_audit_questions after insert or update or delete on questions for each row execute function audit_log_trigger();
create trigger trg_audit_levels after insert or update or delete on levels for each row execute function audit_log_trigger();
create trigger trg_audit_level_questions after insert or update or delete on level_questions for each row execute function audit_log_trigger();
create trigger trg_audit_images after insert or update or delete on images for each row execute function audit_log_trigger();
create trigger trg_audit_packs after insert or update or delete on packs for each row execute function audit_log_trigger();
create trigger trg_audit_settings after insert or update or delete on settings for each row execute function audit_log_trigger();
create trigger trg_audit_admin_users after insert or update or delete on admin_users for each row execute function audit_log_trigger();

-- ===========================================================================
-- Row Level Security
-- ===========================================================================

alter table admin_users enable row level security;
alter table worlds enable row level security;
alter table levels enable row level security;
alter table questions enable row level security;
alter table level_questions enable row level security;
alter table images enable row level security;
alter table packs enable row level security;
alter table settings enable row level security;
alter table comments enable row level security;
alter table audit_log enable row level security;

-- admin_users: any admin can see the roster; only admins manage it.
create policy admin_users_select on admin_users for select using (is_admin_user());
create policy admin_users_write on admin_users for all using (current_admin_role() = 'admin') with check (current_admin_role() = 'admin');

-- worlds: readable by any admin user, writable by admin/editor.
create policy worlds_select on worlds for select using (is_admin_user());
create policy worlds_write on worlds for all
  using (current_admin_role() in ('admin', 'editor'))
  with check (current_admin_role() in ('admin', 'editor'));

-- levels: readable by any admin user, writable by admin/editor.
create policy levels_select on levels for select using (is_admin_user());
create policy levels_write on levels for all
  using (current_admin_role() in ('admin', 'editor'))
  with check (current_admin_role() in ('admin', 'editor'));

-- questions: readable by any admin user. Insert/delete by admin/editor only.
-- Update allowed for admin/editor/reviewer -- the BEFORE UPDATE trigger above
-- is what actually stops a reviewer from touching anything but the checks.
create policy questions_select on questions for select using (is_admin_user());
create policy questions_insert on questions for insert with check (current_admin_role() in ('admin', 'editor'));
create policy questions_update on questions for update
  using (current_admin_role() in ('admin', 'editor', 'reviewer'))
  with check (current_admin_role() in ('admin', 'editor', 'reviewer'));
create policy questions_delete on questions for delete using (current_admin_role() in ('admin', 'editor'));

-- level_questions: readable by any admin user, writable by admin/editor.
create policy level_questions_select on level_questions for select using (is_admin_user());
create policy level_questions_write on level_questions for all
  using (current_admin_role() in ('admin', 'editor'))
  with check (current_admin_role() in ('admin', 'editor'));

-- images: readable by any admin user, writable by admin/editor.
create policy images_select on images for select using (is_admin_user());
create policy images_write on images for all
  using (current_admin_role() in ('admin', 'editor'))
  with check (current_admin_role() in ('admin', 'editor'));

-- packs: readable by any admin user. Only ever inserted by the publish flow
-- (admin only); no update/delete policy at all -- a published pack record
-- is immutable history.
create policy packs_select on packs for select using (is_admin_user());
create policy packs_insert on packs for insert with check (current_admin_role() = 'admin');

-- settings: readable by any admin user, writable by admin only.
create policy settings_select on settings for select using (is_admin_user());
create policy settings_write on settings for all
  using (current_admin_role() = 'admin')
  with check (current_admin_role() = 'admin');

-- comments: readable/insertable by any admin user; resolving/editing by
-- admin/editor or the comment's own author.
create policy comments_select on comments for select using (is_admin_user());
create policy comments_insert on comments for insert with check (is_admin_user());
create policy comments_update on comments for update
  using (current_admin_role() in ('admin', 'editor') or author = auth.uid())
  with check (current_admin_role() in ('admin', 'editor') or author = auth.uid());

-- audit_log: admin-only read. No client-facing write policy -- rows are only
-- ever inserted by audit_log_trigger(), which runs security definer and so
-- bypasses RLS for that insert.
create policy audit_log_select on audit_log for select using (current_admin_role() = 'admin');
