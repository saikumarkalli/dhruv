-- categories — declarative state (ADR-0032). Money-tab Phase 3 (002-money-tab, data-model.md
-- "Category"). Identity survives rename (FR-023) — a rename UPDATEs only `name`, never touches
-- `id`, so every linked transaction stays linked.
--
-- `kind` is a frozen, append-only TEXT enum. Reserved rows (`Uncategorised`, `Adjustment`) are
-- seeded per user at first use by the repository layer (T062) — ordinary rows the client rules
-- forbid deleting, not a separate DB mechanism, so this table carries no "is_reserved" column.
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

create policy "categories_select_own"
    on finance.categories for select
    using (user_id = auth.uid());

create policy "categories_insert_own"
    on finance.categories for insert
    with check (user_id = auth.uid());

create policy "categories_update_own"
    on finance.categories for update
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- No client-facing DELETE policy — merge (finance.merge_categories) soft-deletes the source
-- category; hard erasure only via public.delete_my_data()/public.delete_my_account().

grant select, insert, update on finance.categories to authenticated;
