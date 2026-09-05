-- suggestions — declarative state (ADR-0032). Money-tab Phase 3 (002-money-tab, data-model.md
-- "Pending entry"). A proposed transaction awaiting accept/dismiss — never counted in any total
-- until accepted (FR-029). `raw_text` is Phase 7's SMS source, unused this phase.
--
-- The unique (recurring_id, due_on) constraint is the idempotency key that makes materialise-on-open
-- (research R7) safe to run repeatedly and from two devices without duplicating a pending entry.
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

create policy "suggestions_select_own"
    on finance.suggestions for select
    using (user_id = auth.uid());

create policy "suggestions_insert_own"
    on finance.suggestions for insert
    with check (user_id = auth.uid());

create policy "suggestions_update_own"
    on finance.suggestions for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

grant select, insert, update on finance.suggestions to authenticated;
