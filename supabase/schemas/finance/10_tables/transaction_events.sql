-- transaction_events — declarative state (ADR-0032). Money-tab Phase 3 (002-money-tab,
-- data-model.md "Transaction history entry"). Append-only (FR-008, constitution Article IX): no
-- user_id column (ownership transitive through transaction_id, same pattern as `valuations` ->
-- `holdings`), SELECT + INSERT policies only — no UPDATE, no DELETE. That absence is what makes
-- "history entries MUST NOT be editable or removable by any path" true at the database layer.
--
-- NOTE on file ordering: this file is authored (and applied) BEFORE transactions.sql within the
-- `10_tables/*.sql` glob (config.toml schema_paths) because "transaction_events" sorts
-- alphabetically before "transactions" ('_' < 's'). Both the `transaction_id` foreign key AND the
-- RLS policies below need `finance.transactions` to already exist (a policy's USING/WITH CHECK
-- expression is parsed against real tables, same as a view) — neither can be declared here. RLS is
-- enabled (blocking all access) but its policies, the FK, and the grant are added at the bottom of
-- transactions.sql, once both tables exist. Do not "fix" this by moving them back inline; it will
-- break `db reset`.
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

-- RLS is enabled here (default-deny with zero policies); its policies are added at the bottom of
-- transactions.sql, once finance.transactions exists for them to reference. Deliberately no UPDATE
-- policy and no DELETE policy anywhere — FR-008 / constitution Article IX.
alter table finance.transaction_events enable row level security;
