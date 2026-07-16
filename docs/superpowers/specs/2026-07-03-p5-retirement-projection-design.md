# P5 — Retirement Projection & Gap Analysis

> Status: **SPECCED** (build after P1–P3; the payoff phase). Umbrella:
> `2026-07-03-tracker-roadmap-overview.md`. Design system + playbook binding.
> Inherits all shared invariants incl. P1 §4.4 error taxonomy and auth/session/consent handling.

## Goal

Corpus projection and gap analysis over *real tracked data*: investable base from P1 net worth,
savings-rate defaults from P2, scenario what-ifs (retire age, returns, inflation, SIP step-up).
Illustration, not advice — disclaimer copy mandatory.

## Server schema

```sql
create table retirement_scenarios (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  current_age int not null,
  retire_age int not null,
  life_expectancy int not null default 85,
  monthly_expense_today_paise bigint not null,
  inflation_bps int not null default 600,             -- 6.00%
  pre_retirement_return_bps int not null default 1100, -- 11.00%
  post_retirement_return_bps int not null default 700, -- 7.00%
  monthly_sip_paise bigint not null,
  sip_stepup_bps int not null default 0,               -- annual SIP increase
  created_at timestamptz not null default now(),
  is_deleted boolean not null default false
);
-- RLS: standard "own rows".
```

All rates = integer basis points. No floats anywhere in storage.

## Feature module — `:apps:finance:feature:retirement`

| Piece | Detail |
|-------|--------|
| Screens | Scenario list; `ScenarioEditorSheet` (defaults pre-filled: SIP from P2 average savings, expenses from P2 average spend, investable base shown from P1); `ProjectionScreen` (long-horizon line chart: corpus trajectory vs required-corpus band, retirement-age slider, live recompute); two-scenario comparison view |
| ViewModels | `RetirementViewModel`, `ScenarioEditorViewModel` |
| Pure logic (TDD) | `RetirementProjector` — **BigDecimal** internals (decades of compounding overflow/precision; paise Long only at the boundary): required corpus (expenses inflated to retire age, annuitized at post-retirement real return over life expectancy), corpus trajectory (investable base + stepped-up SIP compounding at pre-retirement return), gap ₹ + FIRE number |
| Investable base | P1 net worth **excluding** `PROPERTY` and `VEHICLE` categories by default; per-asset "count as investable" override toggle |
| Repository | `IScenarioRepository` in `:data` |
| Flag | `"retirement"` |
| Home bento card | "On track" / "Gap ₹X" status chip for the primary scenario |

## Tests

`RetirementProjectorTest` — golden-value tests against hand-computed cases (fixed inputs → exact
expected corpus/gap), zero-SIP, retire-age = current-age edge, step-up compounding, negative real
return; scenario defaults derivation tests; ViewModel tests with fakes; ArchUnit green.

## Dependencies

P1 net worth + categories; P2 savings data (defaults); chart primitives in `:libs:core` (promoted
in P2). P3 goal semantics inform "goal vs retirement" copy only.

## UI/UX detail (states per design system; components from `:libs:core`)

| Screen | Layout & states |
|---|---|
| Scenario list | `BentoCard` per scenario: name, on-track/gap `StatDeltaChip`, retire age; primary scenario starred; FAB add; empty state CTA |
| `ScenarioEditorSheet` (`DhruvModalSheet`) | Age fields, expense ₹ (pre-filled from P2 avg), SIP ₹ (pre-filled from P2 savings), rates as % decimals (stored bps), step-up %; every prefill labeled "from your data"; validation: retire age > current age, expense > 0 |
| `ProjectionScreen` | `TrendLineChart` axis mode, corpus trajectory + required-corpus band; retirement-age slider with live recompute; summary cards: corpus at retirement, required, gap, FIRE number; disclaimer footer (persistent, bodySmall) |
| Comparison view | Two scenarios side-by-side deltas table |
| Home bento card | "On track / Gap ₹X" `StatDeltaChip` for primary scenario; tap → ProjectionScreen |

## Rollout & rollback

Flag `retirement` kill-switch → card hidden + `FeatureDisabledCard`. Schema additive (1 table).
Projection engine pure — no server compute.

## Risks / open questions

- Projection realism: defaults documented as assumptions, editable; disclaimer on every projection
  screen ("illustration, not financial advice").
- Investable-asset classification is a heuristic → per-asset override toggle is required scope,
  not optional.
- Two-scenario comparison may slip to a fast-follow if P5 runs long (cut line documented).

## TDD Mandate

> **Test-Driven Development (TDD) is strictly required for this phase.**
> All pure logic, calculators, reducers, and state machines MUST be written with failing tests first, followed by implementation. UI components must be tested for accessibility and rendering states on both Android and Web platforms.

