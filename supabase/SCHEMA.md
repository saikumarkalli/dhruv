# Dhruv Tracker — Schema Reference

> **Generated file — do not hand-edit.** Produced by `scripts/db/gen_schema_docs.py` from `supabase/schemas/`. Regenerate after any schema change: `python scripts/db/gen_schema_docs.py`. CI (`supabase-migrate.yml`) fails the build if this file is stale (ADR-0032). Objects are grouped by Postgres schema — one per app (ADR-0033); `public` holds cross-app orchestration only.

## Postgres schemas

- `finance`

## Extensions

- `pgcrypto`

## Tables

### Schema `finance`

#### `finance.holdings`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `user_id` | `uuid` | not null references auth.users (id) on delete cascade |
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
| `linked_account_id` | `uuid` | — |
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

### Schema `public`

#### `public.delete_my_account()`

Returns `void` · security **definer**

#### `public.delete_my_data()`

Returns `void` · security **definer**
