-- Dhruv Finance tracker — initial schema (design-v1 Phase 1: Identity & consent).
--
-- Scope: only what Phase 1 needs — `holdings` and `valuations` (ADR-0029). Later phases add
-- liabilities_meta, accounts, transactions, etc. (implementation plan §5.4/§7) in their own
-- migrations, each following this file's RLS shape.
--
-- Design decisions this file encodes (see platform/DECISIONS.md ADR-0029 for full rationale):
--   * Currency-less by design — no `currency` column anywhere. INR is a display convention only
--     (functional spec open item §8.5, resolved via
--     apps/finance/docs/superpowers/specs/2026-07-12-r5-accounts-multicurrency-decisions.md
--     "Option A: INR-only, validated").
--   * Money is bigint paise, never numeric/float (NFR-3, DAT-BR-008).
--   * `valuations` has SELECT + INSERT RLS policies only — no UPDATE, no DELETE — making it
--     append-only at the database layer (BR-C1, DAT-BR-007). `deleted_at` ships now as
--     forward-compatible shape for a Phase 2 correction mechanism; nothing writes to it yet.
--   * Sector/kind values are persisted as free TEXT, validated at the Kotlin repository boundary,
--     not via a DB CHECK constraint — enum constants are append-only by review convention
--     (BR-C3, NW-BR-004/005), not by a constraint that would need a migration to extend.
--   * Erasure never uses a client-facing DELETE policy — only the two security-definer functions
--     at the bottom of this file (delete_my_data / delete_my_account).

-- `with schema extensions` (not the default `public`) keeps pgcrypto's functions (digest, crypt,
-- gen_random_uuid, ...) out of PostgREST's auto-exposed api schema list (security review,
-- 2026-08-15) — Supabase's documented best practice, not load-bearing here (generic crypto
-- utilities, no table access) but removes the ambiguity for free.
create extension if not exists pgcrypto with schema extensions;

-- ── holdings ────────────────────────────────────────────────────────────────────────────────────
create table if not exists public.holdings (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null,
    kind text not null check (kind in ('ASSET', 'LIABILITY')),
    sector text not null,
    notes text,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists holdings_user_id_idx on public.holdings (user_id);

alter table public.holdings enable row level security;

create policy "holdings_select_own"
    on public.holdings for select
    using (user_id = auth.uid());

create policy "holdings_insert_own"
    on public.holdings for insert
    with check (user_id = auth.uid());

create policy "holdings_update_own"
    on public.holdings for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- No client-facing DELETE policy — rows only disappear via delete_my_data()/delete_my_account()
-- (ADR-0029 decision 5), keeping erasure auditable and centralized rather than an ordinary CRUD path.

-- ── valuations ──────────────────────────────────────────────────────────────────────────────────
-- Append-only (BR-C1): no user_id column (ownership is transitive through holding_id), no UPDATE
-- policy, no DELETE policy. The only two RLS policies below are SELECT and INSERT.
create table if not exists public.valuations (
    id uuid primary key default gen_random_uuid(),
    holding_id uuid not null references public.holdings (id) on delete cascade,
    value_paise bigint not null,
    as_of date not null,
    source text not null,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists valuations_holding_id_idx on public.valuations (holding_id);
create index if not exists valuations_holding_id_as_of_idx on public.valuations (holding_id, as_of desc);

alter table public.valuations enable row level security;

create policy "valuations_select_own"
    on public.valuations for select
    using (
        holding_id in (select id from public.holdings where user_id = auth.uid())
    );

create policy "valuations_insert_own"
    on public.valuations for insert
    with check (
        holding_id in (select id from public.holdings where user_id = auth.uid())
    );

-- Deliberately no UPDATE policy (DAT-BR-007) and no DELETE policy — see ADR-0029 decision 4.

-- ── Erasure (ADR-0014 §7, ADR-0029 decision 5) ─────────────────────────────────────────────────
-- Both functions run `security definer` so they can act on auth.users, but are only ever callable
-- by the signed-in user against their own uid — no service-role key, no Edge Function.

-- IMPORTANT: every future migration that adds a new tracker table MUST add a matching DELETE
-- here, scoped to auth.uid() (directly or transitively through a parent table, same as
-- valuations->holdings below). This function is the entire DPDP 7-day erasure guarantee
-- (ADR-0014 §7) for "Delete my data" (ONB-BR-008) — a forgotten table here breaks that guarantee
-- silently, with no test failure (DAT-FLOW-001 is Automatable: N, verified manually per migration).
create or replace function public.delete_my_data()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    delete from public.valuations
    where holding_id in (select id from public.holdings where user_id = auth.uid());

    delete from public.holdings where user_id = auth.uid();

    -- ADD NEW TABLES HERE (delete children before parents, same pattern as valuations above)
end;
$$;

revoke all on function public.delete_my_data() from public;
grant execute on function public.delete_my_data() to authenticated;

create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    calling_user_id uuid := auth.uid();
begin
    perform public.delete_my_data();
    delete from auth.users where id = calling_user_id;
end;
$$;

revoke all on function public.delete_my_account() from public;
grant execute on function public.delete_my_account() to authenticated;
