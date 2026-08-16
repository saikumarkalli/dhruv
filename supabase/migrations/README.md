# Migrations

Generated, append-only, executed artifacts — **not** where you author schema changes. Authorship
lives in `../schemas/` (declarative, per-object files, one folder per Postgres schema —
`schemas/finance/10_tables/holdings.sql`, `schemas/public/30_functions/delete_my_data.sql`, …). See
`platform/DECISIONS.md` ADR-0032 for the declarative-authorship design and ADR-0033 for the
per-app-schema layout; this file is the day-to-day workflow.

## One Postgres schema per app (ADR-0033)

Domain tables/views/functions live under `finance.*`, not `public.*` — `supabase/schemas/finance/`
mirrors that 1:1. `public` is reserved for genuinely cross-app orchestration only (today: the two
erasure functions, `public.delete_my_data()`/`public.delete_my_account()` — `auth.users` is shared
across every Dhruv app per ADR-0031, so account-level erasure can't live in one app's schema).

Adding a new app's Supabase-backed data (Tools, ...) means:
1. A new `supabase/schemas/<app>/00_schema.sql` (`create schema if not exists <app>;` + a
   `grant usage on schema <app> to authenticated;`), then `<app>/10_tables/`, `<app>/20_views/`,
   `<app>/30_functions/` as needed — same shape as `finance/`.
2. Add `<app>` to `config.toml`'s `[api] schemas` list (a schema not listed there is unreachable
   through PostgREST regardless of grants) and add its glob lines to `[db.migrations].schema_paths`
   — **before** `./schemas/public/30_functions/*.sql`, since `public.delete_my_data()` will grow a
   call into `<app>.delete_my_data()`-equivalent rows and needs the table to already exist.
3. Add the matching glob block to `SCHEMA_GLOBS` in `scripts/db/gen_schema_docs.py`.
4. Any table with per-row ownership needs the same explicit `grant select/insert/update on
   <app>.<table> to authenticated;` pattern as `finance.holdings` — custom schemas are not
   auto-exposed to API roles the way `public` legacy behavior sometimes is.
5. If the app's data flows into "Delete my data" / DPDP erasure, add its DELETE(s) to
   `public.delete_my_data()`, same as `finance.valuations`/`finance.holdings` today.

## Workflow

1. Edit (or add) the relevant file under `supabase/schemas/<app>/` — the object's full, current
   definition, not a diff. Colocate its RLS policies with it.
2. Generate the migration: `supabase db diff -f <name>` (needs the local Supabase stack running —
   `supabase start`). This writes a new timestamp-prefixed file into this directory; the CLI's
   timestamp is what orders migrations, so always let it name the file — don't hand-name one.
3. **Read the generated SQL.** This is the actual review point — the declarative file describes
   *intent*, the generated migration is what will run against real data.
4. `supabase db reset` locally to verify it applies cleanly and RLS still behaves as expected.
5. Commit **both** the `schemas/` edit and the generated `migrations/` file in the same PR.
6. PR reviewed. On merge to `develop`, `supabase-migrate.yml` applies it to `dhruv-dev`
   automatically (ungated). A PR merge to `main` queues the same file(s) for `dhruv-prod`, gated by
   one approval click on a GitHub issue the workflow opens (native Environment reviewer rules need
   GitHub Pro on a private repo, unavailable here) — the same run's `prod-plan` job has already
   printed the pending migration list and a destructive-statement scan before that click, so it
   happens with the actual SQL in view.
7. `npx supabase gen types typescript --schema public,finance` regenerates
   `web/src/shared/types/database.ts` (CI verifies this is not stale — see `supabase-migrate.yml`;
   the `--schema` list must include every app schema or its tables lose typed-client coverage).
8. Kotlin DTOs updated manually in `apps/finance/data/`.

## One migration set, both environments

There is no dev-only or prod-only migration file, and no `IF current_database() = ...`-style
conditional. The same files apply, in the same order, to `dhruv-dev` then `dhruv-prod` — that
identity is what makes "it works in dev" a true statement about prod. A drift guard checks, before
every prod push, that prod's applied-migration history is a prefix of this directory's file list;
if it isn't, the push aborts rather than silently pushing against an unknown starting state.

**Corollary: never run SQL from the Supabase dashboard SQL editor on either project.** A hand-run
statement is invisible to the migration history and voids the guarantee above for every migration
after it.

## `supabase/seed.sql`

Dev-only fixture data. Only `supabase db reset` loads it (locally, or on a fresh `dhruv-dev`) — it
is never part of `db push` and never reaches `dhruv-prod`.

## Caveats — these can't be authored declaratively

Per Supabase's own documented limits on `db diff`, the following must be written as ordinary,
hand-authored migrations instead of edits to `schemas/`: DML statements (insert/update/delete);
view owner/grants; `security invoker` on views; materialized views; a view isn't recreated when
altering a column's type; **`ALTER POLICY`** (only `CREATE POLICY` is diffed — a policy *edit* is a
drop+create migration, not a change to the existing `create policy` block); column privileges;
schema privileges; `COMMENT ON`; partitions; `ALTER PUBLICATION … ADD TABLE`; `CREATE DOMAIN`;
grants duplicated from default privileges. Because `COMMENT ON` isn't tracked, schema documentation
lives in each `schemas/` file's header comments and in generated `supabase/SCHEMA.md`
(`scripts/db/gen_schema_docs.py`), never in `COMMENT ON`.

## RLS — no exceptions

Every table needs RLS enabled with a `user_id = auth.uid()` policy (directly, or transitively
through a parent table — see `valuations`' `holding_id` join) — no exceptions, the app servers run
no business logic of their own.
