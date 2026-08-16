-- valuations — declarative state (ADR-0032, moved to the `finance` schema by ADR-0033). This file
-- is the current shape of the table, not a change log — edit it, then run
-- `supabase db diff -f <name>` to generate the migration that applies the edit.
--
-- Append-only (BR-C1): no user_id column (ownership is transitive through holding_id), no UPDATE
-- policy, no DELETE policy. The only two RLS policies below are SELECT and INSERT. `deleted_at`
-- ships as forward-compatible shape for a future Phase-2 correction mechanism; nothing writes to
-- it yet.
create table if not exists finance.valuations (
    id uuid primary key default gen_random_uuid(),
    holding_id uuid not null references finance.holdings (id) on delete cascade,
    value_paise bigint not null,
    as_of date not null,
    source text not null,
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
