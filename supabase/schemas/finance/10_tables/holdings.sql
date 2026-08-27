-- holdings — declarative state (ADR-0032, moved to the `finance` schema by ADR-0033). This file is
-- the current shape of the table, not a change log — edit it, then run `supabase db diff -f <name>`
-- to generate the migration that applies the edit.
--
-- Currency-less by design — no `currency` column (functional spec open item §8.5, resolved via
-- the R5 accounts-multicurrency decision: "Option A: INR-only, validated"). Money is bigint paise,
-- never numeric/float (NFR-3, DAT-BR-008).
-- `sector` is a frozen, append-only TEXT enum (BR-C3) — never rename a shipped constant. The full
-- value set is fixed by the 2026-08-23 readiness decisions §2.1 and enforced as a CHECK here as
-- well as at the Kotlin repository boundary.
--
-- `invested_paise` is the holding's cost basis, nullable because a holding whose cost the user does
-- not know is normal (inherited gold, an old EPF balance). C3 renders INVESTED/GAIN only when it is
-- present — it must never show a wrong zero. It funds a *simple* return, NOT XIRR: XIRR needs a
-- dated cashflow series that no phase models yet (readiness decisions §2.3, 005 research R8).
create table if not exists finance.holdings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null check (length(btrim(name)) between 1 and 120),
    kind text not null check (kind in ('ASSET', 'LIABILITY')),
    sector text not null check (sector in (
        'BANK', 'MUTUAL_FUND', 'STOCKS', 'PROPERTY', 'GOLD',
        'EPF_PPF', 'CASH', 'VEHICLE', 'CRYPTO', 'OTHER'
    )),
    invested_paise bigint check (invested_paise is null or invested_paise >= 0),
    notes text,
    -- Client-generated at the moment the user commits (not at send time), so an automatic retry
    -- after a timeout collides here and returns the existing row instead of duplicating a holding
    -- (readiness decisions §2.7).
    request_id uuid unique,
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
