-- transactions — declarative state (ADR-0032). Money-tab Phase 3 (002-money-tab, data-model.md
-- "Transaction"). `amount_paise` is always positive — sign comes from `type`, never from the
-- stored value (constitution Article VII). `type`/`source` are frozen, append-only TEXT enums.
--
-- Splits (FR-004) are sibling rows sharing `split_group_id` — there is no parent row holding a
-- total, so nothing can double-count and every aggregate view already sums the parts correctly.
--
-- Mutable (accounts.category/edit), unlike `finance.transaction_events`: a transaction's category
-- legitimately changes, and that change is the thing the audit trail (below) records. Deletes are
-- soft (`deleted_at`) so the audit trail is never orphaned; hard removal only via
-- public.delete_my_data()/public.delete_my_account().
create table if not exists finance.transactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    type text not null check (type in ('EXPENSE', 'INCOME', 'TRANSFER')),
    amount_paise bigint not null check (amount_paise > 0),
    account_id uuid not null references finance.accounts (id),
    to_account_id uuid references finance.accounts (id),
    category_id uuid references finance.categories (id),
    payee text,
    note text,
    occurred_at timestamptz not null,
    cleared boolean not null default true,
    -- Device-local URI this phase (research R6) — not a Supabase Storage key yet.
    receipt_path text,
    -- Forward-compatible: `goals` arrives Phase 4, so no FK yet (data-model.md).
    goal_id uuid,
    recurring_id uuid references finance.recurring_templates (id),
    split_group_id uuid,
    source text not null default 'MANUAL'
        check (source in ('MANUAL', 'RECURRING', 'RECONCILE', 'SMS', 'IMPORT')),
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz,
    constraint transactions_transfer_shape check (
        (type = 'TRANSFER' and to_account_id is not null and to_account_id <> account_id and category_id is null)
        or
        (type in ('EXPENSE', 'INCOME') and to_account_id is null and category_id is not null)
    )
);

create index if not exists transactions_user_id_occurred_at_idx
    on finance.transactions (user_id, occurred_at desc);
create index if not exists transactions_account_id_idx on finance.transactions (account_id);
create index if not exists transactions_to_account_id_idx on finance.transactions (to_account_id);
create index if not exists transactions_category_id_idx on finance.transactions (category_id);
create index if not exists transactions_split_group_id_idx on finance.transactions (split_group_id);
create index if not exists transactions_recurring_id_idx on finance.transactions (recurring_id);

alter table finance.transactions enable row level security;

create policy "transactions_select_own"
    on finance.transactions for select
    using (user_id = auth.uid());

create policy "transactions_insert_own"
    on finance.transactions for insert
    with check (user_id = auth.uid());

create policy "transactions_update_own"
    on finance.transactions for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- No client-facing DELETE policy — soft delete only; hard erasure only via
-- public.delete_my_data()/public.delete_my_account().

grant select, insert, update on finance.transactions to authenticated;

-- finance.transaction_events' FK and RLS policies are declared here, not in transaction_events.sql,
-- because that file is applied earlier in the 10_tables/*.sql glob (alphabetical: '_' < 's') and
-- finance.transactions did not exist yet at that point. See transaction_events.sql's header note.
alter table finance.transaction_events
    add constraint transaction_events_transaction_id_fkey
    foreign key (transaction_id) references finance.transactions (id) on delete cascade;

create policy "transaction_events_select_own"
    on finance.transaction_events for select
    using (
        transaction_id in (select id from finance.transactions where user_id = auth.uid())
    );

create policy "transaction_events_insert_own"
    on finance.transaction_events for insert
    with check (
        transaction_id in (select id from finance.transactions where user_id = auth.uid())
    );

grant select, insert on finance.transaction_events to authenticated;
