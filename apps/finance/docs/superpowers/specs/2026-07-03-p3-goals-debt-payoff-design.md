# P3 — Savings Goals & Debt Payoff Plans

> Status: **SPECCED** (build after P2). Umbrella: `2026-07-03-tracker-roadmap-overview.md`.
> Design system + playbook binding (micro-frontend rule; roles/gates per playbook).
> Inherits all shared invariants incl. P1 §4.4 error taxonomy and auth/session/consent handling.

## Goal

Named savings goals funded by linked assets (progress = real valuations, not manual %), and debt
payoff strategies (avalanche/snowball) over P1 liabilities with an interest-saved comparison.

## Server schema

```sql
create table goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  target_paise bigint not null,
  target_date date,
  icon text not null default 'flag',
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
create table goal_links (            -- many-to-many goal <-> asset
  goal_id uuid not null references goals(id) on delete cascade,
  asset_id uuid not null,            -- assets.id
  user_id uuid not null references auth.users(id) on delete cascade,
  primary key (goal_id, asset_id)
);
create table payoff_plans (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  liability_id uuid not null,        -- liabilities.id
  strategy text not null,            -- AVALANCHE | SNOWBALL | CUSTOM
  apr_bps int not null,              -- interest rate as basis points (manual entry; P1 liabilities carry no rate)
  min_payment_paise bigint not null,
  extra_monthly_paise bigint not null default 0,
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
-- RLS: standard "own rows" on all three (goal_links via user_id).
```

## Prerequisite ADR (blocking)

Extract EMI/amortization pure math out of `:apps:finance:feature:loans` into
`:apps:finance:data` (e.g. `com.dhruv.finance.data.financemath`) so both loans and goals consume
it — `feature → feature` stays forbidden. Loans feature refactored to delegate; behavior identical
(golden tests before move).

## Feature module — `:apps:finance:feature:goals`

| Piece | Detail |
|-------|--------|
| Screens | `GoalsScreen` (progress cards: funded % = Σ linked assets' latest valuations / target; ETA from P2 savings rate); `GoalEditorSheet` (name, target, date, asset multi-select); `PayoffScreen` (debts ordered per strategy, payoff timeline chart, interest-saved vs minimum-only baseline); `PayoffPlanSheet` (APR, min payment, extra monthly) |
| ViewModels | `GoalsViewModel`, `GoalEditorViewModel`, `PayoffViewModel` |
| Pure logic (TDD) | `GoalProgressCalculator` (funded %, ETA); `PayoffScheduleEngine` (amortization schedule per debt, strategy ordering, debt-free date, interest-saved delta) — consumes extracted finance-math |
| Repository | `IGoalRepository`, `IPayoffRepository` in `:data` |
| Flag | `"goals"` |
| Home bento card | Top goal progress ring + next debt-free date |

## Tests

`GoalProgressCalculatorTest` (multi-asset funding, over-funded cap at 100%, no-savings-rate ETA
fallback); `PayoffScheduleEngineTest` (avalanche vs snowball ordering, extra-payment acceleration,
zero-APR edge, golden amortization values); ViewModel tests with fakes; ArchUnit green.

## Dependencies

P1 valuations, P2 savings rate (for ETAs), EMI-math extraction ADR.

## UI/UX detail (states per design system; components from `:libs:core`)

| Screen | Layout & states |
|---|---|
| `GoalsScreen` | `BentoGrid` of goal cards: progress ring (funded %), name, funded/target compact, ETA line ("on track · Mar 2028" from P2 savings rate); FAB add; empty (`EmptyStateCard` "Create your first goal"), loading/offline/error standard |
| `GoalEditorSheet` (`DhruvModalSheet`) | Name, target ₹, target date (optional), icon picker, linked-assets multi-select (checkbox list of P1 assets with latest values; badge "linked to N goals" on already-linked); validation: name, target > 0 |
| `PayoffScreen` | Strategy segmented control (Avalanche/Snowball/Custom); ordered debt list (`BentoCard`: name, APR, balance, payoff date); timeline `TrendLineChart` (axis mode); summary row: debt-free date + interest saved vs minimum-only (`StatDeltaChip`) |
| `PayoffPlanSheet` | APR (bps as %, decimal input), min payment ₹, extra monthly ₹; validation: APR ≥ 0, min payment > 0 |
| Home bento card | Top goal ring + next debt-free date; tap → GoalsScreen |

## Rollout & rollback

Flag `goals` kill-switch → card hidden + `FeatureDisabledCard`. Schema additive (3 tables). The
EMI-math extraction refactor is guarded by golden tests on loans before/after — loans behavior
identical or the refactor does not merge.

## Risks / open questions

- Same asset linked to multiple goals double-counts funding — allowed, but UI badges the asset
  ("linked to 2 goals") so it is a visible choice.
- APR/min-payment are manual: P1 liabilities carry no rate data. Acceptable for a manual tracker;
  automation may enrich later.

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

