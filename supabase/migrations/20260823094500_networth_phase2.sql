-- Phase 2 (Net worth tracker) — schema for 001-net-worth-tracker.
--
-- Implements the 2026-08-23 readiness architecture decisions
-- (apps/finance/docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md),
-- which resolve the two blocking correctness defects the 2026-08-22 spec audit found:
--   §1.1  valuation correction was impossible — `valuations` has no UPDATE policy, yet FR-004
--         required a soft-delete. Fixed by finance.correct_valuation(), NOT by adding an UPDATE
--         policy, which would destroy BR-C1's database-level append-only guarantee.
--   §1.2  holding + first valuation atomicity was unachievable across two PostgREST inserts.
--         Fixed by finance.create_holding_with_value().
-- and §1.3, which makes `security_invoker = on` mandatory on every view — without it a Postgres 15+
-- view runs as its owner, bypasses RLS, and returns every user's rows through PostgREST.
--
-- AUTHORSHIP NOTE: this file is HAND-AUTHORED, not `supabase db diff`-generated. Corrected
-- 2026-08-23: the Supabase CLI *is* installed (v2.114.0) -- an earlier draft of this header repeated
-- ADR-0033's "CLI + Docker not installed" without re-checking, which was true when that ADR was
-- written and is no longer true of the CLI. **Docker is still absent**, so `supabase db reset` (local
-- stack) cannot run, and the default migra diff engine runs in a container. `supabase db diff
-- --linked` against the linked project is the path to try first; verify it works before relying on
-- it. ADR-0032 decision 4's caveat list requires hand-authorship for several statements here
-- regardless — security-invoker views, grants, and `create or replace function` bodies are none of
-- them diffable — so a generated diff would still need hand-editing.
--
-- It has therefore NOT been executed against any database: `supabase db reset` locally, or the
-- `develop` push that runs supabase-migrate.yml's apply-dev job, is its first real execution and the
-- point at which its correctness is actually confirmed. The declarative files under
-- supabase/schemas/finance/ are the source of truth either way.

-- ---------------------------------------------------------------------------
-- 1. holdings — cost basis + idempotency key + frozen sector enum
-- ---------------------------------------------------------------------------

-- Cost basis for C3's INVESTED/GAIN. Nullable: a holding whose cost the user does not know is
-- normal (inherited gold, an old EPF balance) and C3 must omit the stat rather than show a wrong
-- zero. Funds a SIMPLE return, not XIRR — XIRR needs a dated cashflow series no phase models yet.
alter table finance.holdings
    add column if not exists invested_paise bigint;

-- Client-generated at the moment the user commits, so an automatic retry after a timeout collides
-- here instead of creating a second holding (readiness decisions §2.7).
alter table finance.holdings
    add column if not exists request_id uuid;

alter table finance.holdings
    drop constraint if exists holdings_request_id_key;
alter table finance.holdings
    add constraint holdings_request_id_key unique (request_id);

alter table finance.holdings
    drop constraint if exists holdings_invested_paise_check;
alter table finance.holdings
    add constraint holdings_invested_paise_check
    check (invested_paise is null or invested_paise >= 0);

alter table finance.holdings
    drop constraint if exists holdings_name_check;
alter table finance.holdings
    add constraint holdings_name_check
    check (length(btrim(name)) between 1 and 120);

-- `sector` is append-only (BR-C3) — never rename a shipped constant. The value set was previously
-- unconstrained at the database layer and documented only in the functional spec's prose.
alter table finance.holdings
    drop constraint if exists holdings_sector_check;
alter table finance.holdings
    add constraint holdings_sector_check
    check (sector in (
        'BANK', 'MUTUAL_FUND', 'STOCKS', 'PROPERTY', 'GOLD',
        'EPF_PPF', 'CASH', 'VEHICLE', 'CRYPTO', 'OTHER'
    ));

-- ---------------------------------------------------------------------------
-- 2. valuations — frozen source enum, no-future-date guard, idempotency key
-- ---------------------------------------------------------------------------

alter table finance.valuations
    add column if not exists request_id uuid;

alter table finance.valuations
    drop constraint if exists valuations_request_id_key;
alter table finance.valuations
    add constraint valuations_request_id_key unique (request_id);

alter table finance.valuations
    drop constraint if exists valuations_value_paise_check;
alter table finance.valuations
    add constraint valuations_value_paise_check check (value_paise >= 0);

-- A future as_of sorts first in v_latest_valuation and would become permanently "latest".
-- `current_date` is STABLE, not IMMUTABLE; safe here only because the predicate is monotonic — a
-- row valid on insert stays valid on every later restore. Do not copy to a lower-bound check.
alter table finance.valuations
    drop constraint if exists valuations_as_of_check;
alter table finance.valuations
    add constraint valuations_as_of_check check (as_of <= current_date);

-- MANUAL and CORRECTION are "self-valued" for F4's footnote; STATEMENT and IMPORT are not
-- (readiness decisions §2.1 — this partition was previously undefined and Phase 5 depends on it).
alter table finance.valuations
    drop constraint if exists valuations_source_check;
alter table finance.valuations
    add constraint valuations_source_check
    check (source in ('MANUAL', 'STATEMENT', 'IMPORT', 'CORRECTION'));

-- ---------------------------------------------------------------------------
-- 3. liabilities_meta — loan/card terms (ADR-0033: `finance`, never `public`)
-- ---------------------------------------------------------------------------

create table if not exists finance.liabilities_meta (
    holding_id uuid primary key references finance.holdings (id) on delete cascade,
    liability_type text not null check (liability_type in (
        'HOME_LOAN', 'CAR_LOAN', 'CREDIT_CARD', 'BNPL'
    )),
    rate_bps integer not null check (rate_bps >= 0 and rate_bps <= 10000),
    emi_paise bigint check (emi_paise is null or emi_paise >= 0),
    debit_day smallint check (debit_day is null or debit_day between 1 and 31),
    tenure_months integer check (tenure_months is null or tenure_months > 0),
    paid_months integer not null default 0 check (paid_months >= 0),
    -- Required to derive C7's amortisation split; without it that donut has no defined
    -- computation, which is what the audit found. Nullable for a card or BNPL line.
    original_principal_paise bigint check (original_principal_paise is null or original_principal_paise >= 0),
    collateral text,
    -- FK to finance.accounts is added by Phase 3's migration — Phase 2 must not depend on a table
    -- it does not create.
    linked_account_id uuid,
    request_id uuid unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    constraint liabilities_meta_paid_within_tenure
        check (tenure_months is null or paid_months <= tenure_months)
);

alter table finance.liabilities_meta enable row level security;

-- Ownership is transitive through the parent holding — no user_id column, same pattern as
-- finance.valuations (ADR-0029 decision 4).
drop policy if exists "liabilities_meta_select_own" on finance.liabilities_meta;
create policy "liabilities_meta_select_own"
    on finance.liabilities_meta for select
    using (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

drop policy if exists "liabilities_meta_insert_own" on finance.liabilities_meta;
create policy "liabilities_meta_insert_own"
    on finance.liabilities_meta for insert
    with check (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

drop policy if exists "liabilities_meta_update_own" on finance.liabilities_meta;
create policy "liabilities_meta_update_own"
    on finance.liabilities_meta for update
    using (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    )
    with check (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

-- No client-facing DELETE policy — rows only disappear via public.delete_my_data()/
-- public.delete_my_account(), consistent with finance.holdings (ADR-0029 decision 5).

-- ADR-0033 decision 4: custom-schema objects are unreachable without an explicit grant, and
-- `db diff` cannot emit grants.
grant select, insert, update on finance.liabilities_meta to authenticated;

-- ---------------------------------------------------------------------------
-- 4. Views — every one `security_invoker = on` (readiness decisions §1.3)
-- ---------------------------------------------------------------------------

create or replace view finance.v_latest_valuation
with (security_invoker = on) as
select distinct on (v.holding_id)
    v.holding_id,
    h.user_id,
    v.id as valuation_id,
    v.value_paise,
    v.as_of,
    v.source,
    v.created_at
from finance.valuations v
join finance.holdings h on h.id = v.holding_id
where v.deleted_at is null
  and h.deleted_at is null
order by v.holding_id, v.as_of desc, v.created_at desc;

grant select on finance.v_latest_valuation to authenticated;

create or replace view finance.v_net_worth_by_sector
with (security_invoker = on) as
select
    h.user_id,
    h.kind,
    h.sector,
    count(*) as holding_count,
    sum(lv.value_paise) as value_paise
from finance.holdings h
join finance.v_latest_valuation lv on lv.holding_id = h.id
where h.deleted_at is null
group by h.user_id, h.kind, h.sector;

grant select on finance.v_net_worth_by_sector to authenticated;

-- Trailing 24 month-ends, clamped so the newest point means "now". Derivation is
-- "latest valuation <= date" — the same rule Phase 5's report_balance_sheet(p_as_of) uses, so this
-- is not a competing mechanism and Phase 5 may read this view rather than re-derive it.
create or replace view finance.v_net_worth_history
with (security_invoker = on) as
with months as (
    select least(
        (date_trunc('month', d) + interval '1 month - 1 day')::date,
        current_date
    ) as as_of
    from generate_series(
        date_trunc('month', current_date) - interval '23 months',
        date_trunc('month', current_date),
        interval '1 month'
    ) as d
)
select
    h.user_id,
    m.as_of,
    coalesce(sum(lv.value_paise) filter (where h.kind = 'ASSET'), 0)     as assets_paise,
    coalesce(sum(lv.value_paise) filter (where h.kind = 'LIABILITY'), 0) as liabilities_paise,
    coalesce(sum(lv.value_paise) filter (where h.kind = 'ASSET'), 0)
        - coalesce(sum(lv.value_paise) filter (where h.kind = 'LIABILITY'), 0) as net_paise
from months m
join finance.holdings h
    on h.deleted_at is null
cross join lateral (
    select v.value_paise
    from finance.valuations v
    where v.holding_id = h.id
      and v.deleted_at is null
      and v.as_of <= m.as_of
    order by v.as_of desc, v.created_at desc
    limit 1
) lv
group by h.user_id, m.as_of;

grant select on finance.v_net_worth_history to authenticated;

-- ---------------------------------------------------------------------------
-- 5. Functions
-- ---------------------------------------------------------------------------

create or replace function finance.correct_valuation(
    p_valuation_id uuid,
    p_value_paise bigint,
    p_as_of date,
    p_note text default null
)
returns uuid
language plpgsql
security definer
set search_path = finance, public
as $$
declare
    v_holding_id uuid;
    v_new_id uuid;
begin
    select v.holding_id
      into v_holding_id
      from finance.valuations v
      join finance.holdings h on h.id = v.holding_id
     where v.id = p_valuation_id
       and v.deleted_at is null
       and h.deleted_at is null
       and h.user_id = auth.uid();

    if v_holding_id is null then
        raise exception 'valuation not found, already corrected, or not owned by caller'
            using errcode = 'no_data_found';
    end if;

    if p_value_paise < 0 then
        raise exception 'value_paise must be >= 0' using errcode = 'check_violation';
    end if;

    if p_as_of > current_date then
        raise exception 'as_of may not be in the future' using errcode = 'check_violation';
    end if;

    update finance.valuations
       set deleted_at = now()
     where id = p_valuation_id;

    insert into finance.valuations (holding_id, value_paise, as_of, source, request_id)
    values (v_holding_id, p_value_paise, p_as_of, 'CORRECTION', null)
    returning id into v_new_id;

    if p_note is not null then
        update finance.holdings
           set notes = coalesce(notes || E'\n', '') || p_note
         where id = v_holding_id;
    end if;

    return v_new_id;
end;
$$;

revoke all on function finance.correct_valuation(uuid, bigint, date, text) from public;
grant execute on function finance.correct_valuation(uuid, bigint, date, text) to authenticated;

create or replace function finance.create_holding_with_value(
    p_name text,
    p_kind text,
    p_sector text,
    p_value_paise bigint,
    p_as_of date,
    p_source text default 'MANUAL',
    p_invested_paise bigint default null,
    p_notes text default null,
    p_request_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = finance, public
as $$
declare
    v_holding_id uuid;
begin
    if auth.uid() is null then
        raise exception 'not authenticated' using errcode = 'insufficient_privilege';
    end if;

    if p_request_id is not null then
        select id into v_holding_id
          from finance.holdings
         where request_id = p_request_id
           and user_id = auth.uid();

        if v_holding_id is not null then
            return v_holding_id;
        end if;
    end if;

    insert into finance.holdings (user_id, name, kind, sector, invested_paise, notes, request_id)
    values (auth.uid(), btrim(p_name), p_kind, p_sector, p_invested_paise, p_notes, p_request_id)
    returning id into v_holding_id;

    insert into finance.valuations (holding_id, value_paise, as_of, source)
    values (v_holding_id, p_value_paise, p_as_of, p_source);

    return v_holding_id;
end;
$$;

revoke all on function finance.create_holding_with_value(text, text, text, bigint, date, text, bigint, text, uuid) from public;
grant execute on function finance.create_holding_with_value(text, text, text, bigint, date, text, bigint, text, uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 6. DPDP erasure — a new user-data table MUST be added here or the 7-day erasure
--    guarantee (ADR-0014 §7) breaks silently, with no test failure.
-- ---------------------------------------------------------------------------

create or replace function public.delete_my_data()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    delete from finance.liabilities_meta
    where holding_id in (select id from finance.holdings where user_id = auth.uid());

    delete from finance.valuations
    where holding_id in (select id from finance.holdings where user_id = auth.uid());

    delete from finance.holdings where user_id = auth.uid();

    -- ADD NEW TABLES HERE (delete children before parents, same pattern as finance.valuations above)
end;
$$;