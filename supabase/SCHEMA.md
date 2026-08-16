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
| `name` | `text` | not null |
| `kind` | `text` | not null check (kind in ('ASSET', 'LIABILITY')) |
| `sector` | `text` | not null |
| `notes` | `text` | — |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `holdings_user_id_idx`

| Policy | Command |
|---|---|
| `holdings_select_own` | select |
| `holdings_insert_own` | insert |
| `holdings_update_own` | update |

#### `finance.valuations`

RLS: **enabled**

| Column | Type | Constraints |
|---|---|---|
| `id` | `uuid` | primary key default gen_random_uuid() |
| `holding_id` | `uuid` | not null references finance.holdings (id) on delete cascade |
| `value_paise` | `bigint` | not null |
| `as_of` | `date` | not null |
| `source` | `text` | not null |
| `created_at` | `timestamptz` | not null default now() |
| `deleted_at` | `timestamptz` | — |

Indexes: `valuations_holding_id_idx`, `valuations_holding_id_as_of_idx`

| Policy | Command |
|---|---|
| `valuations_select_own` | select |
| `valuations_insert_own` | insert |

## Functions

### Schema `public`

#### `public.delete_my_account()`

Returns `void` · security **definer**

#### `public.delete_my_data()`

Returns `void` · security **definer**
