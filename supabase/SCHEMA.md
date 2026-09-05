# Dhruv Tracker — Schema Reference

> **Generated file — do not hand-edit.** Produced by `scripts/db/gen_schema_docs.py` from `supabase/schemas/`. Regenerate after any schema change: `python scripts/db/gen_schema_docs.py`. CI (`supabase-migrate.yml`) fails the build if this file is stale (ADR-0032). Objects are grouped by Postgres schema — one per app (ADR-0033); `public` holds cross-app orchestration only.

## Postgres schemas

- `finance`

## Extensions

- `pgcrypto`

## Tables

### Schema `finance`

#### `finance.accounts`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null default auth.uid() references auth.users (id) on delete cascade |
| `name` | `text` | not null check (length(btrim(name)) between 1 and 60) |
| `type` | `text` | not null check (type in ('BANK', 'CASH', 'WALLET', 'CREDIT_CARD')) |
| `mask` | `text` | check (mask is null or length(mask) <= 4) |
| `is_primary` | `boolean` | not null default false |
| `limit_paise` | `bigint` | check (limit_paise is null or limit_paise >= 0) |
| `due_day` | `smallint` | check (due_day is null or due_day between 1 and 31) |
| `opening_balance_paise` | `bigint` | not null default 0 |
| `reconciled_at` | `timestamptz` | — |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `accounts_user_id_idx`, `accounts_one_primary_per_user`

| Policy | Command |
|---|---|
| `accounts_select_own` | select |
| `accounts_insert_own` | insert |
| `accounts_update_own` | update |

#### `finance.categories`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null default auth.uid() references auth.users (id) on delete cascade |
| `name` | `text` | not null check (length(btrim(name)) between 1 and 60) |
| `kind` | `text` | not null check (kind in ('EXPENSE', 'INCOME')) |
| `parent_id` | `uuid` | references finance.categories (id) on delete set null |
| `icon` | `text` | — |
| `excluded_from_spend` | `boolean` | not null default false |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `categories_user_id_idx`, `categories_parent_id_idx`

| Policy | Command |
|---|---|
| `categories_select_own` | select |
| `categories_insert_own` | insert |
| `categories_update_own` | update |

#### `finance.holdings`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null default auth.uid() references auth.users (id) on delete cascade |
| `name` | `text` | not null check (length(btrim(name)) between 1 and 120) |
| `kind` | `text` | not null check (kind in ('ASSET', 'LIABILITY')) |
| `sector` | `text` | not null check (sector in (
        'BANK', 'MUTUAL_FUND', 'STOCKS', 'PROPERTY', 'GOLD',
        'EPF_PPF', 'CASH', 'VEHICLE', 'CRYPTO', 'OTHER'
    )) |
| `invested_paise` | `bigint` | check (invested_paise is null or invested_paise >= 0) |
| `notes` | `text` | — |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `holdings_user_id_idx`

| Policy | Command |
|---|---|
| `holdings_select_own` | select |
| `holdings_insert_own` | insert |
| `holdings_update_own` | update |

#### `finance.liabilities_meta`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `holding_id` | `uuid` | primary key references finance.holdings (id) on delete cascade |
| `liability_type` | `text` | not null check (liability_type in (
        'HOME_LOAN', 'CAR_LOAN', 'CREDIT_CARD', 'BNPL'
    )) |
| `rate_bps` | `integer` | not null check (rate_bps >= 0 and rate_bps <= 10000) |
| `emi_paise` | `bigint` | check (emi_paise is null or emi_paise >= 0) |
| `debit_day` | `smallint` | check (debit_day is null or debit_day between 1 and 31) |
| `tenure_months` | `integer` | check (tenure_months is null or tenure_months > 0) |
| `paid_months` | `integer` | not null default 0 check (paid_months >= 0) |
| `original_principal_paise` | `bigint` | check (original_principal_paise is null or original_principal_paise >= 0) |
| `collateral` | `text` | — |
| `linked_account_id` | `uuid` | references finance.accounts (id) |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `updated_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |
| `constraint` | `liabilities_meta_paid_within_tenure` | check (tenure_months is null or paid_months <= tenure_months) |

| Policy | Command |
|---|---|
| `liabilities_meta_select_own` | select |
| `liabilities_meta_insert_own` | insert |
| `liabilities_meta_update_own` | update |

#### `finance.recurring_templates`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null default auth.uid() references auth.users (id) on delete cascade |
| `template` | `jsonb` | not null |
| `rrule` | `text` | not null |
| `next_run` | `date` | not null |
| `amount_is_variable` | `boolean` | not null default false |
| `paused` | `boolean` | not null default false |
| `paused_at` | `timestamptz` | — |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `recurring_templates_user_id_idx`, `recurring_templates_next_run_idx`

| Policy | Command |
|---|---|
| `recurring_templates_select_own` | select |
| `recurring_templates_insert_own` | insert |
| `recurring_templates_update_own` | update |

#### `finance.suggestions`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null default auth.uid() references auth.users (id) on delete cascade |
| `recurring_id` | `uuid` | references finance.recurring_templates (id) |
| `due_on` | `date` | — |
| `raw_text` | `text` | — |
| `parsed` | `jsonb` | not null |
| `status` | `text` | not null default 'PENDING' check (status in ('PENDING', 'ACCEPTED', 'IGNORED')) |
| `created_at` | `timestamptz` | not null default now() |

Indexes: `suggestions_user_id_status_idx`, `suggestions_recurring_due_on_unique`

| Policy | Command |
|---|---|
| `suggestions_select_own` | select |
| `suggestions_insert_own` | insert |
| `suggestions_update_own` | update |

#### `finance.transaction_events`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `transaction_id` | `uuid` | not null |
| `at` | `timestamptz` | not null default now() |
| `kind` | `text` | not null check (kind in (
        'CREATED', 'EDITED', 'CATEGORY_CHANGED', 'DELETED', 'ACCEPTED_FROM_RECURRING', 'RECONCILED'
    )) |
| `detail` | `jsonb` | — |

Indexes: `transaction_events_transaction_id_idx`

| Policy | Command |
|---|---|
| `transaction_events_select_own` | select |
| `transaction_events_insert_own` | insert |

#### `finance.transactions`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null default auth.uid() references auth.users (id) on delete cascade |
| `type` | `text` | not null check (type in ('EXPENSE', 'INCOME', 'TRANSFER')) |
| `amount_paise` | `bigint` | not null check (amount_paise > 0) |
| `account_id` | `uuid` | not null references finance.accounts (id) |
| `to_account_id` | `uuid` | references finance.accounts (id) |
| `category_id` | `uuid` | references finance.categories (id) |
| `payee` | `text` | — |
| `note` | `text` | — |
| `occurred_at` | `timestamptz` | not null |
| `cleared` | `boolean` | not null default true |
| `receipt_path` | `text` | — |
| `goal_id` | `uuid` | — |
| `recurring_id` | `uuid` | references finance.recurring_templates (id) |
| `split_group_id` | `uuid` | — |
| `source` | `text` | not null default 'MANUAL'
        check (source in ('MANUAL', 'RECURRING', 'RECONCILE', 'SMS', 'IMPORT')) |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |
| `constraint` | `transactions_transfer_shape` | check (
        (type = 'TRANSFER' and to_account_id is not null and to_account_id <> account_id and category_id is null)
        or
        (type in ('EXPENSE', 'INCOME') and to_account_id is null and category_id is not null)
    ) |

Indexes: `transactions_user_id_occurred_at_idx`, `transactions_account_id_idx`, `transactions_to_account_id_idx`, `transactions_category_id_idx`, `transactions_split_group_id_idx`, `transactions_recurring_id_idx`

| Policy | Command |
|---|---|
| `transactions_select_own` | select |
| `transactions_insert_own` | insert |
| `transactions_update_own` | update |

#### `finance.valuations`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `holding_id` | `uuid` | not null references finance.holdings (id) on delete cascade |
| `value_paise` | `bigint` | not null check (value_paise >= 0) |
| `as_of` | `date` | not null check (as_of <= current_date) |
| `source` | `text` | not null check (source in ('MANUAL', 'STATEMENT', 'IMPORT', 'CORRECTION')) |
| `request_id` | `uuid` | unique |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `valuations_holding_id_idx`, `valuations_holding_id_as_of_idx`

| Policy | Command |
|---|---|
| `valuations_select_own` | select |
| `valuations_insert_own` | insert |

## Functions

### Schema `finance`

#### `finance.correct_valuation(p_valuation_id uuid,
    p_value_paise bigint,
    p_as_of date,
    p_note text default null)`

Returns `uuid` · security **definer**

#### `finance.create_holding_with_value(p_name text,
    p_kind text,
    p_sector text,
    p_value_paise bigint,
    p_as_of date,
    p_source text default 'MANUAL',
    p_invested_paise bigint default null,
    p_notes text default null,
    p_request_id uuid default null)`

Returns `uuid` · security **definer**

#### `finance.fn_transaction_audit()`

Returns `trigger` · security **invoker**

#### `finance.merge_categories(p_source uuid, p_target uuid)`

Returns `integer` · security **invoker**

### Schema `public`

#### `public.delete_my_account()`

Returns `void` · security **definer**

#### `public.delete_my_data()`

Returns `void` · security **definer**
