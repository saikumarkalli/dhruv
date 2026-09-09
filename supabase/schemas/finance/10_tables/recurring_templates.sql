-- recurring_templates — declarative state (ADR-0032). Money-tab Phase 3 (002-money-tab,
-- data-model.md "Recurring definition"). A template + schedule that produces `finance.suggestions`
-- rows, never `finance.transactions` rows directly (FR-028) — there is no server scheduler
-- (research R7); the client materialises due occurrences on open, idempotently.
create table if not exists finance.recurring_templates (
    id uuid primary key default gen_random_uuid(),
    -- default auth.uid() -- see accounts.sql's identical column for why (found 2026-09-05).
    user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    -- The transaction shape to produce: type, amount, category, account, payee.
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

create policy "recurring_templates_select_own"
    on finance.recurring_templates for select
    using (user_id = auth.uid());

create policy "recurring_templates_insert_own"
    on finance.recurring_templates for insert
    with check (user_id = auth.uid());

create policy "recurring_templates_update_own"
    on finance.recurring_templates for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- No client-facing DELETE policy — soft delete only; hard erasure only via
-- public.delete_my_data()/public.delete_my_account().

grant select, insert, update on finance.recurring_templates to authenticated;
