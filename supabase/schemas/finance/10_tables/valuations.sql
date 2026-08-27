-- valuations — declarative state (ADR-0032, moved to the `finance` schema by ADR-0033). This file
-- is the current shape of the table, not a change log — edit it, then run
-- `supabase db diff -f <name>` to generate the migration that applies the edit.
--
-- Append-only (BR-C1): no user_id column (ownership is transitive through holding_id), no UPDATE
-- policy, no DELETE policy. The only two RLS policies below are SELECT and INSERT.
--
-- `deleted_at` is written by exactly one thing: finance.correct_valuation() (30_functions/), the
-- security-definer RPC ADR-0029 decision 4 named. A correction soft-deletes the wrong row and
-- appends a corrected one in a single transaction. There is deliberately still no UPDATE policy —
-- adding one would make the table ordinarily mutable and destroy the database-level append-only
-- guarantee this design exists for (readiness decisions §1.1).
--
-- `source` is a frozen, append-only TEXT enum (BR-C3, readiness decisions §2.1). MANUAL and
-- CORRECTION are "self-valued" for F4's footnote; STATEMENT and IMPORT are not.
create table if not exists finance.valuations (
    id uuid primary key default gen_random_uuid(),
    holding_id uuid not null references finance.holdings (id) on delete cascade,
    value_paise bigint not null check (value_paise >= 0),
    -- A future as_of would sort first in v_latest_valuation and become permanently "latest".
    -- `current_date` is STABLE, not IMMUTABLE, which Postgres permits in a CHECK but flags as a
    -- dump/restore hazard in the general case. It is safe *here* specifically because the predicate
    -- is monotonic: a row that satisfied `as_of <= current_date` on insert still satisfies it on
    -- every later restore, since current_date only advances. Do not copy this pattern to a
    -- lower-bound check, where the same reasoning inverts and a restore would fail.
    as_of date not null check (as_of <= current_date),
    source text not null check (source in ('MANUAL', 'STATEMENT', 'IMPORT', 'CORRECTION')),
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists valuations_holding_id_idx on finance.valuations (holding_id);
create index if not exists valuations_holding_id_as_of_idx on finance.valuations (holding_id, as_of desc);

alter table finance.valuations enable row level security;

create policy "valuations_select_own"
    on finance.valuations for select
    using (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

create policy "valuations_insert_own"
    on finance.valuations for insert
    with check (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

-- Deliberately no UPDATE policy (DAT-BR-007) and no DELETE policy — see ADR-0029 decision 4.

-- Custom schemas need explicit per-table grants (see finance/00_schema.sql's header comment).
grant select, insert on finance.valuations to authenticated;
