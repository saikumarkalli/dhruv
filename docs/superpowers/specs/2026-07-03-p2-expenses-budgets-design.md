# P2 — Expenses, Income & Budgets

> Status: **SPECCED** (build after P1 merges). Umbrella: `2026-07-03-tracker-roadmap-overview.md`.
> Design system: `2026-07-03-tracker-design-system.md` (micro-frontend rule: screens compose
> `:libs:core` components only). Playbook: `2026-07-04-tracker-engineering-playbook.md` (roles/gates).
> Inherits all shared invariants (Supabase REST on Retrofit/Moshi/OkHttp + RLS, paise money,
> soft-delete, FeatureHost, Koin, loans-template observability, bento UI, TDD, error taxonomy and
> auth/session/consent handling exactly as P1 §4.4).

## Goal

Daily income/expense logging with a friction-free quick-add, monthly category budgets, and a
computed savings rate. Feeds P3 goal ETAs and P5 retirement defaults. Manual entry only.

## Server schema

```sql
create table transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type text not null,                -- INCOME | EXPENSE
  category text not null,           -- see enums below
  amount_paise bigint not null,     -- always positive; sign implied by type
  occurred_at timestamptz not null,
  notes text not null default '',
  account_ref uuid,                  -- nullable -> assets.id (which account it hit)
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
create table budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category text not null,
  month_key text not null,           -- 'YYYY-MM' (IST calendar months)
  limit_paise bigint not null,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false,
  unique (user_id, category, month_key)
);
create index on transactions (user_id, occurred_at);
create index on budgets (user_id, month_key);
-- RLS: standard "own rows" policy on both (same as P1 tables).
```

## Categories (append-only enums, TEXT)

- Expense: `FOOD, TRANSPORT, RENT, UTILITIES, SHOPPING, HEALTH, ENTERTAINMENT, EDUCATION, EMI,
  INSURANCE_PREMIUM, INVESTMENT, OTHER`
- Income: `SALARY, BUSINESS, INTEREST, DIVIDEND, GIFT, OTHER`

Convention: insurance premiums logged as `INSURANCE_PREMIUM`, loan EMIs as `EMI` — avoids double
counting with P3/P4 features (documented in-app helper text).

## Feature module — `:apps:finance:feature:expenses`

| Piece | Detail |
|-------|--------|
| Screens | `TransactionListScreen` (day-grouped list, month picker, category filter chips, income/expense toggle); `QuickAddSheet` (amount-first keypad, type toggle, category chips, date default today — target < 5s per entry); `BudgetScreen` (per-category progress bars, over-budget accent, copy-last-month action); category breakdown donut |
| ViewModels | `ExpensesViewModel` (list/filter/rollup stream), `TransactionEditorViewModel` (quick-add form), `BudgetViewModel` |
| Pure logic (TDD) | `MonthlyRollupCalculator`: per-category totals, budget consumption %, savings rate = (income − expense) / income with 0-income guard, month-over-month comparison |
| Repository | `ITransactionRepository` + Supabase impl in `:data` (same remote-data-source pattern as P1) |
| Flag | `"expenses": { "enabled": true, "minVersion": "<release>", "requiresConsent": true }` (consent already granted at P1 account level — flag consistency only) |
| Home bento card | This-month spent vs budget bar + savings-rate chip |

## Chart additions (engineering ledger item)

`TrendLineChart` already lives in `:libs:core` (P1, micro-frontend rule). P2 adds `BarChart` +
`DonutChart` primitives to core following the same Canvas conventions.

## Tests

`MonthlyRollupCalculatorTest` (totals, savings rate edge cases, month boundaries IST, empty months);
repository tests with fake remote; ViewModel tests (filters, quick-add validation: amount > 0,
category required); ArchUnit green.

## Dependencies

P1 merged (auth, consent, Paise utils, bento grid slots, repository pattern).

## UI/UX detail (states per design system; all components from `:libs:core`)

| Screen | Layout & states |
|---|---|
| `TransactionListScreen` | Top bar month picker (chevrons + label); segmented income/expense/all; day-grouped list (`BentoCard` rows: category icon chip, note/merchant, amount tabular ±); sticky day headers with day total; FAB quick-add; states: empty (`EmptyStateCard` "Log your first expense"), loading, offline, error |
| `QuickAddSheet` (`DhruvModalSheet`) | Amount-first numeric keypad (auto-focus), INCOME/EXPENSE toggle, category chip row (2 lines max, most-used first), date chip (default Today → date picker), optional note; Save full-width, disabled in-flight; validation: amount > 0, category required; target < 5s per entry |
| `BudgetScreen` | Month header; per-category rows: category label, spent/limit (`formatPaise`), linear progress (error-container tint when over), "Copy last month" action; `DonutChart` category breakdown atop; empty state CTA "Set your first budget" |
| Home bento card | Half `BentoCard`: "This month" spent value + budget progress bar + savings-rate `StatDeltaChip`; tap → TransactionList |

Charts: `BarChart` (month-over-month), `DonutChart` (category split) — added to `:libs:core` in
this phase per design-system inventory.

## Rollout & rollback

Flag `expenses` kill-switch → Home card hidden + feature route shows `FeatureDisabledCard`; P1
untouched. Schema additive (2 new tables) — no changes to P1 tables. Rollback = flag off / revert
PR; server data persists.

## Risks / open questions

- Quick-add friction kills the habit — UX review is a merge gate for `QuickAddSheet`.
- Month boundary = IST calendar month; documented, not configurable in P2.
- `account_ref` is optional in P2 (no balance reconciliation yet — that's automation territory, P6).
