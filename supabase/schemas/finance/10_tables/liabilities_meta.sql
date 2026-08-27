-- liabilities_meta — declarative state (ADR-0032). Loan/card terms for a holding whose
-- `kind = 'LIABILITY'`. One row per liability holding.
--
-- Lives in the `finance` schema, not `public` (ADR-0033). The Phase 2 spec's data-model originally
-- said `public.liabilities_meta`; that is unreachable, because every tracker call sends
-- `Accept-Profile: finance` and would 404 against `public`. Resolved by the 2026-08-23 readiness
-- decisions §1.4.
--
-- Unlike `valuations` this table is mutable: loan terms genuinely change (a rate reset, a tenure
-- extension), and there is no history requirement on them. The *outstanding balance* is NOT a
-- column here — it is the liability holding's latest valuation (C6's "outstanding, not original"
-- rule). 003's data-model cites `liabilities_meta (… outstanding balance)`; that citation is wrong
-- and is corrected by readiness decisions §1.4.
--
-- Money is bigint paise; rates are integer basis points (NFR-3, DAT-BR-008) — never numeric/float.
create table if not exists finance.liabilities_meta (
    holding_id uuid primary key references finance.holdings (id) on delete cascade,
    liability_type text not null check (liability_type in (
        'HOME_LOAN', 'CAR_LOAN', 'CREDIT_CARD', 'BNPL'
    )),
    rate_bps integer not null check (rate_bps >= 0 and rate_bps <= 10000),
    emi_paise bigint check (emi_paise is null or emi_paise >= 0),
    -- Day of month the EMI/bill is auto-debited. Feeds Home's UPCOMING list.
    debit_day smallint check (debit_day is null or debit_day between 1 and 31),
    tenure_months integer check (tenure_months is null or tenure_months > 0),
    paid_months integer not null default 0 check (paid_months >= 0),
    -- The original sanctioned principal. Required to derive C7's amortisation split
    -- (principal paid / interest paid / left) — without it that donut has no defined computation,
    -- which is what the 2026-08-22 audit found. Nullable for a card or a BNPL line, which has no
    -- sanctioned principal.
    original_principal_paise bigint check (original_principal_paise is null or original_principal_paise >= 0),
    collateral text,
    -- The account the debit lands on. Null until Phase 3 creates finance.accounts; the FK is added
    -- by Phase 3's migration, not here, so Phase 2 does not depend on a table it cannot create.
    linked_account_id uuid,
    request_id uuid unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    constraint liabilities_meta_paid_within_tenure
        check (tenure_months is null or paid_months <= tenure_months)
);

alter table finance.liabilities_meta enable row level security;

-- Ownership is transitive through the parent holding — this table carries no user_id of its own,
-- the same pattern `valuations` uses (ADR-0029 decision 4).
create policy "liabilities_meta_select_own"
    on finance.liabilities_meta for select
    using (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

create policy "liabilities_meta_insert_own"
    on finance.liabilities_meta for insert
    with check (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

create policy "liabilities_meta_update_own"
    on finance.liabilities_meta for update
    using (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    )
    with check (
        holding_id in (select id from finance.holdings where user_id = auth.uid())
    );

-- No client-facing DELETE policy — consistent with `holdings`, rows only disappear via
-- public.delete_my_data()/public.delete_my_account() (ADR-0029 decision 5).
-- NOTE: `ALTER POLICY` is not diffable by `supabase db diff` (ADR-0032 decision 4's caveat list) —
-- a policy *edit* must be a hand-authored drop+create migration.

-- Custom schemas need explicit per-table grants (see finance/00_schema.sql's header comment).
-- `db diff` cannot emit grants either — hand-append them to the generated migration.
grant select, insert, update on finance.liabilities_meta to authenticated;