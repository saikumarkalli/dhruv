# Backend & API SDD (02)

> **Status:** ACTIVE
> **Scope:** Defines the shared Supabase schema, PostgREST API contract, and backend security rules.

## 1. Supabase Schema (PostgreSQL)

The database schema is the ultimate source of truth. All changes must be versioned in `supabase/migrations/` and applied via the Supabase CLI.

### 1.1 Tables (P1–P3)

```sql
-- P1: Net Worth
create table assets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  category text not null,
  notes text not null default '',
  currency text not null default 'INR',
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create table liabilities (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  category text not null,
  notes text not null default '',
  currency text not null default 'INR',
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create table valuation_entries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  parent_id uuid not null,
  parent_type text not null, -- 'ASSET' | 'LIABILITY'
  value_paise bigint not null,
  recorded_at timestamptz not null,
  is_deleted boolean not null default false
);

-- P2: Expenses & Budgets
create table transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null, -- 'INCOME' | 'EXPENSE'
  category text not null,
  amount_paise bigint not null,
  occurred_at timestamptz not null,
  notes text not null default '',
  account_ref uuid,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);

create table budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category text not null,
  month_key text not null, -- 'YYYY-MM'
  limit_paise bigint not null,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false,
  unique (user_id, category, month_key)
);
```
*(Additional tables for P3-P5 omitted for brevity, see specific phase specs)*

## 2. API Contract (PostgREST)

Both platforms use the Supabase REST API (PostgREST) as their data interface. 

### 2.1 Standard Query Patterns
- **Fetch**: `GET /rest/v1/assets?is_deleted=eq.false&select=*`
- **Soft Delete**: `PATCH /rest/v1/assets?id=eq.{id} { "is_deleted": true }`
- **Insert**: `POST /rest/v1/assets { ... }`

### 2.2 Error Codes to UI State
- HTTP `401 Unauthorized` → `AuthState.SignedOut` / Redirect to login.
- HTTP `403 Forbidden` (RLS violation) → `ErrorCard("Access denied")`.
- HTTP `0` (Network Error) → `OfflineBanner`.

## 3. Row Level Security (RLS)

All tables strictly enforce row-level security. The application servers are "dumb" and run no business logic.

```sql
alter table assets enable row level security;

create policy "own rows" on assets for all
  using (user_id = auth.uid()) 
  with check (user_id = auth.uid());
```
*Note: This policy is replicated for every tracked table.*

## 4. CORS Policy

Supabase PostgREST requires a strict CORS whitelist for browser-based requests.

- **Dev**: `http://localhost:5173`
- **Preview**: `https://*.vercel.app`
- **Prod**: `https://dhruv-finance.vercel.app`

## 5. Migrations & Environment Separation

- **Environments**: `dhruv-dev` and `dhruv-prod` separate Supabase projects.
- **Workflow**: PR contains SQL in `supabase/migrations/` → applied to `dev` for testing → applied to `prod` upon merge.
- **Types**: `npx supabase gen types typescript` generates the `database.ts` file for the web client. Android DTOs are updated manually.
