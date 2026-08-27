---
name: dhruv-supabase-object
description: Add or change a Supabase/Postgres object (table, view, function, RLS policy, grant, migration) in the Dhruv platform. Use whenever tracker data is involved — creating a table, adding a column, writing a view, adding an RPC, changing RLS, or authoring a migration. Also triggers on "Supabase schema", "add a table", "PostgREST", "RLS policy", "db diff", "migration", "delete_my_data", "security_invoker". Tracker data is Supabase-primary (ADR-0014), NOT Room — use dhruv-room-entity only for calculator/converter data. Always use this instead of hand-writing SQL.
---

# Dhruv Supabase Object

The declarative-schema workflow for every tracker object. Tracker data lives in Supabase, not Room
(ADR-0014) — if you reached for `dhruv-room-entity` for a holding, transaction, budget, goal or
policy, you are in the wrong skill.

## Before you start

1. **Which schema?** One Postgres schema per app (ADR-0033). Finance tables are `finance.*`.
   `public` is reserved for cross-app orchestration — today only the two erasure functions.
   Vault never touches Supabase at all (ADR-0031 decision 3).
2. **Read the phase's `data-model.md`** in `apps/<app>/specs/NNN-*/`. It, not this skill, decides
   what columns exist.
3. **Read `apps/finance/specs/001-net-worth-tracker/data-model.md` § "Maintenance conventions"** —
   seven rules every phase inherits. They exist because each was found *missing* in an audit.

## The workflow (never skip a step)

```
1. Edit the declarative file      supabase/schemas/<app>/{10_tables,20_views,30_functions}/<obj>.sql
2. Generate the migration         supabase db diff -f <name>
3. Hand-append what db diff cannot emit          (see "The four blind spots" below)
4. Review the generated SQL       it is executed history from here on — never hand-edit it later
5. Regenerate docs + guards       python scripts/db/gen_schema_docs.py docs
                                  python scripts/db/gen_schema_docs.py equiv
6. Commit BOTH the schema file and the migration
```

`supabase/schemas/` is **current state**, `supabase/migrations/` is **executed history**. The
equivalence guard in `supabase-migrate.yml` fails the PR if they disagree, so both must land
together.

If the Supabase CLI/Docker is unavailable, hand-author the migration and **say so in a header
comment**, including that it has not been executed — ADR-0033's own migration set that precedent.
An unexecuted migration is not a verified one.

## The four blind spots — `db diff` will silently omit these

Hand-append every one of them to the generated migration:

1. **`GRANT`** — custom-schema objects are unreachable without one. `grant usage on schema <app> to
   authenticated` plus per-object `grant select, insert, update on <app>.<table> to authenticated`
   (and `grant execute` for functions). No `anon` grants — every tracker call is authenticated.
2. **`security_invoker = on` on views** — see below. This is the one that bites hardest.
3. **`ALTER POLICY`** — only `CREATE POLICY` diffs. A policy *edit* is written as drop + create.
4. **`COMMENT ON`, DML, materialized views, partitions, column privileges** — see ADR-0032
   decision 4's full caveat list. Schema documentation therefore lives in file-header comments and
   generated `supabase/SCHEMA.md`, never in `COMMENT ON`.

## Views: `security_invoker = on` is mandatory

```sql
create or replace view finance.v_example
with (security_invoker = on) as
select ...;

grant select on finance.v_example to authenticated;
```

A Postgres 15+ view executes as its **owner**, which bypasses RLS on the underlying tables — and
PostgREST exposes the view. Without this clause the view returns **every user's rows to every
signed-in caller**. A 2026-08-22 audit found all 8 planned views across three phases missing it.

`db diff` cannot express it, so verify by hand in the generated migration, every time.

## Tables: the checklist

```sql
create table if not exists finance.<name> (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,  -- or transitive, below
    <amount>_paise bigint not null check (<amount>_paise >= 0),
    <enum_col> text not null check (<enum_col> in ('A', 'B')),
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists <name>_user_id_idx on finance.<name> (user_id);
alter table finance.<name> enable row level security;
-- policies, then:
grant select, insert, update on finance.<name> to authenticated;
```

- **RLS on every table**, `user_id = auth.uid()`. A child table carries **no `user_id`** — ownership
  is transitive through its parent (`holding_id in (select id from finance.holdings where user_id =
  auth.uid())`).
- **No client `DELETE` policy.** Rows disappear only via `public.delete_my_data()` /
  `delete_my_account()`, which keeps erasure auditable and centralized (ADR-0029 decision 5).
- **Money is `bigint` paise. Proportions are integer basis points.** Never `numeric`, never float,
  never whole-percent (a whole-percent share column cannot represent three equal nominees).
- **Frozen enums get a `CHECK`, not prose.** Values are append-only forever (BR-C3) — never rename
  a shipped constant. A migration to add one is the correct, intended cost.
- **`request_id uuid unique`** on anything a client creates, so a retry after a timeout collides
  instead of writing a second money row.

## Append-only tables

An append-only table has **SELECT and INSERT policies only** — no UPDATE, no DELETE, and a grant of
only `select, insert`. That is what makes "history is never overwritten" true at the database layer
rather than by client discipline.

**A correction is therefore an RPC, not an UPDATE.** Setting `deleted_at` is an UPDATE; if the table
is append-only, the client cannot do it. Write a `security definer` function that soft-deletes and
appends in one transaction — see `finance.correct_valuation()`.

**Never "fix" this by adding an UPDATE policy.** That makes the table ordinarily mutable and
destroys the guarantee the design exists for. A spec that says "append-only" and also "the client
marks the row deleted" is self-contradictory, and this exact contradiction shipped into three
documents before an audit caught it.

## `security definer` functions

Use one when an operation genuinely needs to cross RLS: atomic multi-table writes, corrections to
append-only tables, erasure.

```sql
create or replace function finance.<name>(...)
returns <type>
language plpgsql
security definer
set search_path = finance, public
as $$
begin
    -- security definer BYPASSES RLS, so the ownership check is yours and it is not optional.
    -- Resolve the row and assert ownership in the SAME statement — no window between them.
    ...
end;
$$;

revoke all on function finance.<name>(...) from public;
grant execute on function finance.<name>(...) to authenticated;
```

`set search_path` is required — without it the function is search-path injectable.

## DPDP: every user-data table joins the erasure function

Add the `DELETE` to `supabase/schemas/public/30_functions/delete_my_data.sql` **in the same
migration** that creates the table, children before parents.

This is the entire 7-day erasure guarantee (ADR-0014 §7). **A miss is silent** — nothing fails, no
test goes red, and the app quietly stops honouring a legal obligation. Device-local Room tables are
outside this function's reach by construction and need their own explicit purge step.

## Client side

Every tracker request must send **`Accept-Profile: finance`** (`Content-Profile` on writes), or
supabase-js `.schema('finance')`. Omitting it does not error loudly — it silently 404s against the
empty `public` schema.

Repositories go in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/`; features never
see a DTO or a DAO (ArchUnit enforces it).

## Verify before you claim done

```bash
python scripts/db/gen_schema_docs.py docs      # regenerate supabase/SCHEMA.md
python scripts/db/gen_schema_docs.py equiv     # schemas/ vs migrations/ — CI runs this
supabase db reset                              # actually execute it, if the CLI is available
```

**Known guard limitation (deliberate):** a named `UNIQUE`/`PRIMARY KEY`/`FOREIGN KEY` added via
`ALTER TABLE … ADD CONSTRAINT` is **not** compared, because it has no comparable inline spelling on
the declarative side and folding it in produced permanent false positives. Columns (name + type,
order-insensitive) and CHECK expressions *are* compared.

## Related

- `dhruv-room-entity` — calculator/converter data only (Room, offline-first)
- ADR-0014 (Supabase-primary tracker), ADR-0029 (client architecture, append-only, erasure),
  ADR-0032 (declarative schema, dev/prod), ADR-0033 (per-app schema namespacing)