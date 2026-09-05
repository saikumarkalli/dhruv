-- Money Tab (Phase 3) — schema for 002-money-tab.
--
-- Six new tables (finance.accounts, categories, transactions, transaction_events,
-- recurring_templates, suggestions), three views (v_account_balances, v_month_summary,
-- v_category_spend, all `security_invoker = on` per constitution Article IXa), the
-- fn_transaction_audit trigger (BR-D5), merge_categories (FR-024), the finance.accounts FK on
-- liabilities_meta.linked_account_id deferred from Phase 2 (readiness decisions §1.4), and the
-- delete_my_data() extension for DPDP erasure (NFR-1).
--
-- AUTHORSHIP NOTE: hand-authored, not `supabase db diff`-generated — Docker is unavailable in this
-- environment (`supabase db reset`/local diff needs it) and no SUPABASE_ACCESS_TOKEN is available
-- for `db diff --linked` either. Several statements here are undiffable regardless (ADR-0032
-- decision 4's caveat list: security-invoker views, grants, function bodies, ALTER TABLE ADD
-- COLUMN against an existing table). Declarative files under supabase/schemas/finance/ are the
-- source of truth; this file has NOT been executed against any database — `supabase db reset`
-- locally, or the `develop` push that runs supabase-migrate.yml's apply-dev job, is its first real
-- execution and the point at which its correctness is actually confirmed.
--
-- File-ordering note carried from the declarative schemas/finance/10_tables/ files: within that
-- directory's glob, "transaction_events" sorts before "transactions" alphabetically ('_' < 's'), so
-- transaction_events is created there without its transaction_id FK or RLS policies, which are
-- added after finance.transactions exists (step 6 below). This migration reproduces that same
-- ordering for consistency with the declarative source, not because a single hand-written file
-- requires it.

-- ---------------------------------------------------------------------------
-- 1. accounts
-- ---------------------------------------------------------------------------

create table if not exists finance.accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null check (length(btrim(name)) between 1 and 60),
    type text not null check (type in ('BANK', 'CASH', 'WALLET', 'CREDIT_CARD')),
    mask text check (mask is null or length(mask) <= 4),
    is_primary boolean not null default false,
    limit_paise bigint check (limit_paise is null or limit_paise >= 0),
    due_day smallint check (due_day is null or due_day between 1 and 31),
    opening_balance_paise bigint not null default 0,
    reconciled_at timestamptz,
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists accounts_user_id_idx on finance.accounts (user_id);

create unique index if not exists accounts_one_primary_per_user
    on finance.accounts (user_id)
    where is_primary and deleted_at is null;

alter table finance.accounts enable row level security;

drop policy if exists "accounts_select_own" on finance.accounts;
create policy "accounts_select_own"
    on finance.accounts for select
    using (user_id = auth.uid());

drop policy if exists "accounts_insert_own" on finance.accounts;
create policy "accounts_insert_own"
    on finance.accounts for insert
    with check (user_id = auth.uid());

drop policy if exists "accounts_update_own" on finance.accounts;
create policy "accounts_update_own"
    on finance.accounts for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, update on finance.accounts to authenticated;

-- ---------------------------------------------------------------------------
-- 2. categories
-- ---------------------------------------------------------------------------

create table if not exists finance.categories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    name text not null check (length(btrim(name)) between 1 and 60),
    kind text not null check (kind in ('EXPENSE', 'INCOME')),
    parent_id uuid references finance.categories (id) on delete set null,
    icon text,
    excluded_from_spend boolean not null default false,
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists categories_user_id_idx on finance.categories (user_id);
create index if not exists categories_parent_id_idx on finance.categories (parent_id);

alter table finance.categories enable row level security;

drop policy if exists "categories_select_own" on finance.categories;
create policy "categories_select_own"
    on finance.categories for select
    using (user_id = auth.uid());

drop policy if exists "categories_insert_own" on finance.categories;
create policy "categories_insert_own"
    on finance.categories for insert
    with check (user_id = auth.uid());

drop policy if exists "categories_update_own" on finance.categories;
create policy "categories_update_own"
    on finance.categories for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, update on finance.categories to authenticated;

-- ---------------------------------------------------------------------------
-- 3. recurring_templates
-- ---------------------------------------------------------------------------

create table if not exists finance.recurring_templates (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    template jsonb not null,
    rrule text not null,
    next_run date not null,
    amount_is_variable boolean not null default false,
    paused boolean not null default false,
    paused_at timestamptz,
    request_id uuid unique,
    created_at timestamptz not null default now(),
    deleted_at timestamptz
);

create index if not exists recurring_templates_user_id_idx on finance.recurring_templates (user_id);
create index if not exists recurring_templates_next_run_idx on finance.recurring_templates (next_run)
    where not paused and deleted_at is null;

alter table finance.recurring_templates enable row level security;

drop policy if exists "recurring_templates_select_own" on finance.recurring_templates;
create policy "recurring_templates_select_own"
    on finance.recurring_templates for select
    using (user_id = auth.uid());

drop policy if exists "recurring_templates_insert_own" on finance.recurring_templates;
create policy "recurring_templates_insert_own"
    on finance.recurring_templates for insert
    with check (user_id = auth.uid());

drop policy if exists "recurring_templates_update_own" on finance.recurring_templates;
create policy "recurring_templates_update_own"
    on finance.recurring_templates for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, update on finance.recurring_templates to authenticated;

-- ---------------------------------------------------------------------------
-- 4. suggestions
-- ---------------------------------------------------------------------------

create table if not exists finance.suggestions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    recurring_id uuid references finance.recurring_templates (id),
    due_on date,
    raw_text text,
    parsed jsonb not null,
    status text not null default 'PENDING' check (status in ('PENDING', 'ACCEPTED', 'IGNORED')),
    created_at timestamptz not null default now()
);

create index if not exists suggestions_user_id_status_idx on finance.suggestions (user_id, status);

create unique index if not exists suggestions_recurring_due_on_unique
    on finance.suggestions (recurring_id, due_on)
    where recurring_id is not null and due_on is not null;

alter table finance.suggestions enable row level security;

drop policy if exists "suggestions_select_own" on finance.suggestions;
create policy "suggestions_select_own"
    on finance.suggestions for select
    using (user_id = auth.uid());

drop policy if exists "suggestions_insert_own" on finance.suggestions;
create policy "suggestions_insert_own"
    on finance.suggestions for insert
    with check (user_id = auth.uid());

drop policy if exists "suggestions_update_own" on finance.suggestions;
create policy "suggestions_update_own"
    on finance.suggestions for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, update on finance.suggestions to authenticated;

-- ---------------------------------------------------------------------------
-- 5. transaction_events (table only — FK + RLS policies deferred to step 6, after
--    finance.transactions exists; see AUTHORSHIP NOTE above)
-- ---------------------------------------------------------------------------

create table if not exists finance.transaction_events (
    id uuid primary key default gen_random_uuid(),
    transaction_id uuid not null,
    at timestamptz not null default now(),
    kind text not null check (kind in (
        'CREATED', 'EDITED', 'CATEGORY_CHANGED', 'DELETED', 'ACCEPTED_FROM_RECURRING', 'RECONCILED'
    )),
    detail jsonb
);

create index if not exists transaction_events_transaction_id_idx
    on finance.transaction_events (transaction_id);

alter table finance.transaction_events enable row level security;

-- ---------------------------------------------------------------------------
-- 6. transactions, then transaction_events' deferred FK/policies/grant
-- ---------------------------------------------------------------------------

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
    receipt_path text,
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

drop policy if exists "transactions_select_own" on finance.transactions;
create policy "transactions_select_own"
    on finance.transactions for select
    using (user_id = auth.uid());

drop policy if exists "transactions_insert_own" on finance.transactions;
create policy "transactions_insert_own"
    on finance.transactions for insert
    with check (user_id = auth.uid());

drop policy if exists "transactions_update_own" on finance.transactions;
create policy "transactions_update_own"
    on finance.transactions for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, update on finance.transactions to authenticated;

alter table finance.transaction_events
    drop constraint if exists transaction_events_transaction_id_fkey;
alter table finance.transaction_events
    add constraint transaction_events_transaction_id_fkey
    foreign key (transaction_id) references finance.transactions (id) on delete cascade;

drop policy if exists "transaction_events_select_own" on finance.transaction_events;
create policy "transaction_events_select_own"
    on finance.transaction_events for select
    using (
        transaction_id in (select id from finance.transactions where user_id = auth.uid())
    );

drop policy if exists "transaction_events_insert_own" on finance.transaction_events;
create policy "transaction_events_insert_own"
    on finance.transaction_events for insert
    with check (
        transaction_id in (select id from finance.transactions where user_id = auth.uid())
    );

grant select, insert on finance.transaction_events to authenticated;

-- ---------------------------------------------------------------------------
-- 7. liabilities_meta — the finance.accounts FK deferred from Phase 2 (readiness decisions §1.4,
--    T111): "this phase owns adding the FK in its own migration, and nothing else will do it."
-- ---------------------------------------------------------------------------

alter table finance.liabilities_meta
    drop constraint if exists liabilities_meta_linked_account_id_fkey;
alter table finance.liabilities_meta
    add constraint liabilities_meta_linked_account_id_fkey
    foreign key (linked_account_id) references finance.accounts (id);

-- ---------------------------------------------------------------------------
-- 8. Views — every one `security_invoker = on` (constitution Article IXa)
-- ---------------------------------------------------------------------------

create or replace view finance.v_account_balances
with (security_invoker = on) as
select
    a.id as account_id,
    a.user_id,
    a.type,
    a.type in ('BANK', 'CASH', 'WALLET') as counts_as_spendable,
    a.opening_balance_paise
        + coalesce(sum(
            case
                when t.account_id = a.id and t.type = 'INCOME' then t.amount_paise
                when t.account_id = a.id and t.type = 'EXPENSE' then -t.amount_paise
                when t.account_id = a.id and t.type = 'TRANSFER' then -t.amount_paise
                when t.to_account_id = a.id and t.type = 'TRANSFER' then t.amount_paise
                else 0
            end
        ), 0) as balance_paise
from finance.accounts a
left join finance.transactions t
    on (t.account_id = a.id or t.to_account_id = a.id)
    and t.deleted_at is null
where a.deleted_at is null
group by a.id, a.user_id, a.type, a.opening_balance_paise;

grant select on finance.v_account_balances to authenticated;

create or replace view finance.v_month_summary
with (security_invoker = on) as
select
    t.user_id,
    date_trunc('month', t.occurred_at)::date as month,
    coalesce(sum(t.amount_paise) filter (where t.type = 'INCOME'), 0) as income_paise,
    coalesce(sum(t.amount_paise) filter (
        where t.type = 'EXPENSE'
          and not coalesce(c.excluded_from_spend, false)
    ), 0) as expense_paise,
    coalesce(sum(t.amount_paise) filter (
        where t.type = 'EXPENSE' and coalesce(c.excluded_from_spend, false)
    ), 0) as excluded_paise,
    coalesce(sum(t.amount_paise) filter (where t.type = 'TRANSFER'), 0) as transfer_paise
from finance.transactions t
left join finance.categories c on c.id = t.category_id
where t.deleted_at is null
group by t.user_id, date_trunc('month', t.occurred_at)::date;

grant select on finance.v_month_summary to authenticated;

create or replace view finance.v_category_spend
with (security_invoker = on) as
select
    t.user_id,
    date_trunc('month', t.occurred_at)::date as month,
    t.category_id,
    c.name as category_name,
    c.kind as category_kind,
    c.excluded_from_spend,
    sum(t.amount_paise) as spend_paise,
    case
        when c.kind = 'EXPENSE' and ms.expense_paise > 0
            then round((sum(t.amount_paise)::numeric / ms.expense_paise) * 100, 1)
        when c.kind = 'INCOME' and ms.income_paise > 0
            then round((sum(t.amount_paise)::numeric / ms.income_paise) * 100, 1)
        else 0
    end as share_percent
from finance.transactions t
join finance.categories c on c.id = t.category_id
join finance.v_month_summary ms
    on ms.user_id = t.user_id and ms.month = date_trunc('month', t.occurred_at)::date
where t.deleted_at is null
  and t.type in ('EXPENSE', 'INCOME')
group by t.user_id, date_trunc('month', t.occurred_at)::date, t.category_id, c.name, c.kind,
    c.excluded_from_spend, ms.expense_paise, ms.income_paise;

grant select on finance.v_category_spend to authenticated;

-- ---------------------------------------------------------------------------
-- 9. Functions: fn_transaction_audit (+ trigger), merge_categories
-- ---------------------------------------------------------------------------

create or replace function finance.fn_transaction_audit()
returns trigger
language plpgsql
security invoker
set search_path = finance, public
as $$
declare
    v_kind text;
    v_detail jsonb;
begin
    if TG_OP = 'INSERT' then
        v_kind := case NEW.source
            when 'RECURRING' then 'ACCEPTED_FROM_RECURRING'
            when 'RECONCILE' then 'RECONCILED'
            else 'CREATED'
        end;
        insert into finance.transaction_events (transaction_id, kind, detail)
        values (NEW.id, v_kind, jsonb_build_object(
            'type', NEW.type,
            'amount_paise', NEW.amount_paise,
            'account_id', NEW.account_id,
            'category_id', NEW.category_id,
            'source', NEW.source
        ));
        return NEW;
    end if;

    if TG_OP = 'UPDATE' then
        if OLD.deleted_at is null and NEW.deleted_at is not null then
            insert into finance.transaction_events (transaction_id, kind, detail)
            values (NEW.id, 'DELETED', jsonb_build_object('deleted_at', NEW.deleted_at));
            return NEW;
        end if;

        if OLD.category_id is distinct from NEW.category_id then
            insert into finance.transaction_events (transaction_id, kind, detail)
            values (NEW.id, 'CATEGORY_CHANGED', jsonb_build_object(
                'old_category_id', OLD.category_id,
                'new_category_id', NEW.category_id
            ));
            return NEW;
        end if;

        v_detail := '{}'::jsonb;
        if OLD.amount_paise is distinct from NEW.amount_paise then
            v_detail := v_detail || jsonb_build_object('amount_paise',
                jsonb_build_object('old', OLD.amount_paise, 'new', NEW.amount_paise));
        end if;
        if OLD.payee is distinct from NEW.payee then
            v_detail := v_detail || jsonb_build_object('payee',
                jsonb_build_object('old', OLD.payee, 'new', NEW.payee));
        end if;
        if OLD.note is distinct from NEW.note then
            v_detail := v_detail || jsonb_build_object('note',
                jsonb_build_object('old', OLD.note, 'new', NEW.note));
        end if;
        if OLD.occurred_at is distinct from NEW.occurred_at then
            v_detail := v_detail || jsonb_build_object('occurred_at',
                jsonb_build_object('old', OLD.occurred_at, 'new', NEW.occurred_at));
        end if;
        if OLD.cleared is distinct from NEW.cleared then
            v_detail := v_detail || jsonb_build_object('cleared',
                jsonb_build_object('old', OLD.cleared, 'new', NEW.cleared));
        end if;
        if OLD.account_id is distinct from NEW.account_id then
            v_detail := v_detail || jsonb_build_object('account_id',
                jsonb_build_object('old', OLD.account_id, 'new', NEW.account_id));
        end if;

        if v_detail = '{}'::jsonb then
            return NEW;
        end if;

        insert into finance.transaction_events (transaction_id, kind, detail)
        values (NEW.id, 'EDITED', v_detail);
        return NEW;
    end if;

    return NEW;
end;
$$;

drop trigger if exists trg_transaction_audit on finance.transactions;
create trigger trg_transaction_audit
    after insert or update on finance.transactions
    for each row execute function finance.fn_transaction_audit();

create or replace function finance.merge_categories(p_source uuid, p_target uuid)
returns integer
language plpgsql
security invoker
set search_path = finance, public
as $$
declare
    v_moved integer;
begin
    if p_source = p_target then
        raise exception 'cannot merge a category into itself' using errcode = 'check_violation';
    end if;

    if not exists (
        select 1 from finance.categories
        where id = p_source and user_id = auth.uid() and deleted_at is null
    ) or not exists (
        select 1 from finance.categories
        where id = p_target and user_id = auth.uid() and deleted_at is null
    ) then
        raise exception 'source or target category not found or not owned by caller'
            using errcode = 'no_data_found';
    end if;

    update finance.transactions
       set category_id = p_target
     where category_id = p_source
       and user_id = auth.uid()
       and deleted_at is null;

    get diagnostics v_moved = row_count;

    update finance.categories
       set deleted_at = now()
     where id = p_source
       and user_id = auth.uid();

    return v_moved;
end;
$$;

revoke all on function finance.merge_categories(uuid, uuid) from public;
grant execute on function finance.merge_categories(uuid, uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- 10. DPDP erasure — extend delete_my_data() with this phase's six tables (T009, NFR-1)
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

    delete from finance.transaction_events
    where transaction_id in (select id from finance.transactions where user_id = auth.uid());

    delete from finance.suggestions where user_id = auth.uid();

    delete from finance.transactions where user_id = auth.uid();

    delete from finance.recurring_templates where user_id = auth.uid();

    delete from finance.categories where user_id = auth.uid();

    delete from finance.accounts where user_id = auth.uid();

    -- ADD NEW TABLES HERE (delete children before parents, same pattern as finance.valuations above)
end;
$$;
