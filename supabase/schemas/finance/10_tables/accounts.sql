-- accounts — declarative state (ADR-0032). This file is the current shape of the table, not a
-- change log — edit it, then run `supabase db diff -f <name>` to generate the migration.
--
-- Money-tab Phase 3 (002-money-tab, data-model.md "Account"). `type` is a frozen, append-only TEXT
-- enum (constitution Article IX) — never rename a shipped constant. Current balance is NOT a stored
-- column: it is derived as opening_balance_paise + sum of signed transactions, exposed by
-- finance.v_account_balances (FR-017). Storing a derived balance would let it drift from the ledger.
--
-- `mask` stores at most the last 4 digits, never a full account/card number (spec Edge Cases,
-- FR-016).
create table if not exists finance.accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null check (length(btrim(name)) between 1 and 60),
    type text not null check (type in ('BANK', 'CASH', 'WALLET', 'CREDIT_CARD')),
    mask text check (mask is null or length(mask) <= 4),
    is_primary boolean not null default false,
    limit_paise bigint check (limit_paise is null or limit_paise >= 0),
    due_day smallint check (due_day is null or due_day between 1 and 31),
    -- CREDIT_CARD-only fields: enforced at the repository boundary (data-model.md "Validation"),
    -- not duplicated here as a CHECK, to avoid a second copy of the same type-dispatch rule.
    opening_balance_paise bigint not null default 0,
    reconciled_at timestamptz,
    -- Client-generated at the moment the user commits, so an automatic retry after a timeout
    -- collides here instead of creating a duplicate account (readiness decisions §2.7, T110).
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists accounts_user_id_idx on finance.accounts (user_id);

-- At most one primary account per user (FR-016). Partial index so multiple non-primary/deleted
-- rows never collide with the constraint.
create unique index if not exists accounts_one_primary_per_user
    on finance.accounts (user_id)
    where is_primary and deleted_at is null;

alter table finance.accounts enable row level security;

create policy "accounts_select_own"
    on finance.accounts for select
    using (user_id = auth.uid());

create policy "accounts_insert_own"
    on finance.accounts for insert
    with check (user_id = auth.uid());

create policy "accounts_update_own"
    on finance.accounts for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- No client-facing DELETE policy — soft delete only (deleted_at); hard erasure only via
-- public.delete_my_data()/public.delete_my_account() (ADR-0029 decision 5).

-- Custom schemas need explicit per-table grants (see finance/00_schema.sql's header comment).
grant select, insert, update on finance.accounts to authenticated;
