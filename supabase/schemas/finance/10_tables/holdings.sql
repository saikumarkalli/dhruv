-- holdings — declarative state (ADR-0032, moved to the `finance` schema by ADR-0033). This file is
-- the current shape of the table, not a change log — edit it, then run `supabase db diff -f <name>`
-- to generate the migration that applies the edit.
--
-- Currency-less by design — no `currency` column (functional spec open item §8.5, resolved via
-- the R5 accounts-multicurrency decision: "Option A: INR-only, validated"). Money is bigint paise,
-- never numeric/float (NFR-3, DAT-BR-008).
create table if not exists finance.holdings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null,
    kind text not null check (kind in ('ASSET', 'LIABILITY')),
    sector text not null,
    notes text,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists holdings_user_id_idx on finance.holdings (user_id);

alter table finance.holdings enable row level security;

create policy "holdings_select_own"
    on finance.holdings for select
    using (user_id = auth.uid());

create policy "holdings_insert_own"
    on finance.holdings for insert
    with check (user_id = auth.uid());

create policy "holdings_update_own"
    on finance.holdings for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- No client-facing DELETE policy — rows only disappear via public.delete_my_data()/
-- public.delete_my_account() (ADR-0029 decision 5), keeping erasure auditable and centralized
-- rather than an ordinary CRUD path.
-- NOTE: `ALTER POLICY` is not diffable by `supabase db diff` (documented caveat, ADR-0032 decision
-- 4) — a policy *edit* here must be written as a hand-authored drop+create migration, not just a
-- change to the `create policy` block above.

-- Custom schemas need explicit per-table grants (see finance/00_schema.sql's header comment).
grant select, insert, update on finance.holdings to authenticated;
