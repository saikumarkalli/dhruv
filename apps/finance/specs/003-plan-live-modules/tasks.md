# Tasks: Plan Live Modules (Phase 4)

**Input**: Design documents from `apps/finance/specs/003-plan-live-modules/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/routes.md](contracts/routes.md),
[quickstart.md](quickstart.md)

**Tests**: **Required, not optional.** Constitution Article I is NON-NEGOTIABLE — RED → GREEN →
REFACTOR, and every test cites the `PLN-*`/`INS-*`/`RET-*` scenario ID it satisfies from
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §5–§7. A test with no
citation is a review-blocking finding. Article II (scenarios before code) is **pre-satisfied**: all
21 rows were written and reviewed 2026-08-09, so no task in this file writes catalog rows — they are
closed, not authored.

**Organization**: Tasks are grouped by user story so each is independently implementable and
testable. The constitution's fixed per-phase order (SA → QA → Backend → Android → QA → Sec →
Checkpoint) maps onto this structure as: SA = Phase 2 Foundational, QA = already done,
Backend/Android = per-story phases, QA-close + Sec + Checkpoint = Phase 11.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Exact file paths in every description

## Path Conventions

Android monorepo, existing Gradle layout (plan.md → Project Structure). Feature modules live under
`apps/finance/feature/plan/<name>/` with Gradle coordinates remapped via `projectDir`. Tests mirror
each module's tree under `src/test/` — there is no separate `tests/` root.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Three new Gradle modules exist, build, are measured by the coverage gate, and are
reachable behind flags.

- [ ] T001 Create `:apps:finance:feature:planning` module at `apps/finance/feature/plan/planning/` with `build.gradle.kts` (`dhruv.android.library` + `dhruv.android.compose`; deps `:apps:finance:data`, `:libs:core`, `:libs:settings`), copying `apps/finance/feature/plan/loans/build.gradle.kts` as the reference shape
- [ ] T002 [P] Create `:apps:finance:feature:insurance` module at `apps/finance/feature/plan/insurance/` with the same `build.gradle.kts` shape
- [ ] T003 [P] Create `:apps:finance:feature:retirement` module at `apps/finance/feature/plan/retirement/` with the same `build.gradle.kts` shape
- [ ] T004 Register all three modules in `settings.gradle.kts` with `include(...)` plus the `projectDir` remaps to `apps/finance/feature/plan/{planning,insurance,retirement}`
- [ ] T005 [P] Add the three module paths to `coveredModules` in the root `build.gradle.kts` (Article X — a module absent here is not measured at all)
- [ ] T006 [P] Add `"planning"`, `"insurance"`, `"retirement"` to `_FEATURES` in `scripts/ci/regression_summary.py` (without this they report as `(other)`, the gap plan.md's Coverage note documents)
- [ ] T007 [P] Add the five flags `budgets`, `goals`, `debtpayoff`, `insurance`, `retirement` to `platform/feature-flags/dhruv-finance.json`, each `{ "enabled": true, "minVersion": "1.0.0", "requiresConsent": true }` per `contracts/routes.md`
- [ ] T008 Create `di/PlanningModule.kt`, `di/InsuranceModule.kt`, `di/RetirementModule.kt` in their modules and aggregate all three in `CalculatorApplication`'s Koin graph
- [ ] T009 [P] Create `PlanningConfig.kt`, `InsuranceConfig.kt` (holding the five gap-risk categories from spec Assumptions) and `RetirementConfig.kt` (holding the three scenario presets) — screen-level data lives in Config files, never inline (Article V)
- [ ] T010 Verify `./gradlew :apps:finance:feature:planning:assembleDebug :apps:finance:feature:insurance:assembleDebug :apps:finance:feature:retirement:assembleDebug` and `./gradlew regressionCheck` are green with the three empty modules registered

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The SA step — schema, navigation vocabulary, and the one cross-story mechanism — must
land before any story's Backend or Android work begins (constitution Development Workflow, step 1
before steps 3/4).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Schema (declarative first, migration generated — ADR-0032)

- [ ] T011 [P] Author `supabase/schemas/finance/10_tables/budgets.sql` per data-model.md, with RLS SELECT/INSERT/UPDATE on `user_id = auth.uid()` and the partial unique index on `(user_id, category_id, period_month)`
- [ ] T012 [P] Author `supabase/schemas/finance/10_tables/goals.sql` per data-model.md — **no `saved_paise` column**; progress is derived
- [ ] T013 [P] Author `supabase/schemas/finance/10_tables/goal_links.sql` per data-model.md with `earmark_bps int` (research R1), the `between 1 and 10000` CHECK, the partial unique index on `(goal_id, holding_id)`, and RLS transitive through the parent goal including DELETE
- [ ] T014 [P] Author `supabase/schemas/finance/10_tables/policies.sql` per data-model.md, including `documents jsonb` (research R6) and `remind_days_before` (research R8)
- [ ] T015 [P] Author `supabase/schemas/finance/10_tables/policy_premiums.sql` per data-model.md with **SELECT + INSERT policies only** — no UPDATE, no DELETE, no `deleted_at` (Article IX, same shape as `valuations`)
- [ ] T016 [P] Author `supabase/schemas/finance/10_tables/retirement_scenarios.sql` per data-model.md with `assumptions jsonb` and no stored corpus
- [ ] T017 [P] Author `supabase/schemas/finance/20_views/v_budget_status.sql` joining `budgets` to Phase 3's `v_category_spend` — transfer and excluded-category exclusion come from that view, never re-derived (research R7)
- [ ] T018 [P] Author `supabase/schemas/finance/20_views/v_goal_progress.sql` — `Σ (latest valuation × earmark_bps / 10000)` with integer truncation last, plus the `link_count` column that lets E4 distinguish unfunded from 0%
- [ ] T019 [P] Author `supabase/schemas/finance/20_views/v_annual_income.sql` from `v_month_summary`, including the `months_observed` honesty column (research R3)
- [ ] T020 Author `supabase/schemas/finance/30_functions/fn_goal_link_earmark_guard.sql` + `BEFORE INSERT OR UPDATE` trigger on `goal_links` enforcing the per-holding 10000 bps cap across all of a user's goals (FR-024 — a CHECK cannot see other rows, and a client check races)
- [ ] T021 Extend `supabase/schemas/public/30_functions/delete_my_data.sql` to hard-delete the six new tables in FK order (`goal_links` → `goals`, `policy_premiums` → `policies`, `budgets`, `retirement_scenarios`), fully qualified as `finance.*`
- [ ] T022 Add explicit `grant usage`/per-table `grant select,insert,update` (and `delete` on `goal_links` only) to `authenticated` for all six tables and three views — ADR-0033 decision 4; `db diff` cannot express grants, so these are hand-written
- [ ] T023 Generate the migration with `supabase db diff -f plan_phase4` into `supabase/migrations/`, review the generated SQL, and hand-add the T022 grants
- [ ] T024 Run `python scripts/db/gen_schema_docs.py equiv` and `python scripts/db/gen_schema_docs.py docs --check`; regenerate `supabase/SCHEMA.md` until both guards pass
- [ ] T025 Apply the migration to `dhruv-dev` and regenerate `web/src/shared/types/database.ts` via `supabase gen types typescript --schema public,finance`

### Navigation vocabulary

- [ ] T026 Add `enum class PlanModule { BUDGETS, GOALS, DEBT_PAYOFF, INSURANCE, RETIREMENT }` and the four `NavTarget` cases `OpenPlanModule`, `OpenBudget`, `OpenGoal`, `OpenPolicy` in `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt`, keeping `PlanModule` separate from the existing `PlanTool` (contracts/routes.md)
- [ ] T027 Extend `NavTarget.tab` for the four new cases and map them to (Plan tab, nested route) in `:apps:finance:app`'s `NavigationDispatcher`
- [ ] T028 [P] Extend the `NavTarget`/`BackContract` tests in `:libs:core` to cover the four new cases — every target resolves to exactly one tab, and an unknown or foreign id resolves to the not-found state rather than crashing (surface-registry §1 code-twin rule)
- [ ] T029 Add the ten per-screen rows from `contracts/routes.md` to `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1, replacing the four combined Phase 4 rows
- [ ] T030 Extend the navigation route-registry test to assert N1/N2 for the new routes — E1 is a root with no parent, every other route has exactly one parent

### The one cross-story mechanism (research R5)

- [ ] T031 Extend `SmartInsightCard` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/` with a visible derived-insight marker, so it becomes the single carrier for every computed statement in this feature — extend, never add a parallel `DerivedInsightLabel` (Article VI)
- [ ] T032 [P] Write the single shared test asserting the derived marker renders, citing `PLN-BR-005` — one assertion covering five screens rather than five that can each be forgotten separately

**Checkpoint**: Schema is live on `dhruv-dev`, both schema guards pass, navigation vocabulary exists,
and the derived-insight carrier is ready. User stories can now begin.

---

## Phase 3: User Story 1 - Know whether this month's spending is on pace (Priority: P1) 🎯 MVP

**Goal**: Budgets (E2) states, in words, whether spending is ahead of or behind the month — with
transfers and excluded categories contributing nothing.

**Independent Test**: With a month of transactions and a budget per category, open Plan → Budgets on
a known day; the pace sentence's percentage matches a hand-computed spend-fraction vs
elapsed-day-fraction, and an over-budget category states its overage amount and days left.

### Tests for User Story 1 (RED — write first, confirm they fail)

- [ ] T033 [P] [US1] Golden-value tests for `BudgetPaceEngine` pace math across ≥3 fixtures (day N of an M-day month → pace = N/M), citing `PLN-BR-002`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/planning/BudgetPaceEngineTest.kt`
- [ ] T034 [P] [US1] Test that a category with `excluded_from_spend = true` contributes zero to every budget figure, citing `PLN-BR-003`, in the same test file
- [ ] T035 [P] [US1] `BudgetRepository` test against a fake/MockWebServer asserting `v_budget_status` is the spend source and no client-side ledger sum occurs (NFR-8), in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/BudgetRepositoryTest.kt`
- [ ] T036 [P] [US1] `BudgetsViewModel` state tests for three pace fixtures (ahead / on / behind), asserting the exact statement text per fixture, citing `PLN-UI-001`, in `apps/finance/feature/plan/planning/src/test/.../BudgetsViewModelTest.kt`
- [ ] T037 [P] [US1] `CategoryBarRow` marker test — ahead-of-pace styling renders if and only if spend-fraction > elapsed-day-fraction, citing `PLN-UI-002`, in `libs/core/src/test/.../CategoryBarRowTest.kt`

### Implementation for User Story 1

- [ ] T038 [US1] Implement `BudgetPaceEngine` in `apps/finance/data/src/main/java/com/dhruv/finance/data/planning/BudgetPaceEngine.kt` — **outside `tracker/`** (research R2); paise `Long` in, paise `Long` out, `BigDecimal` only internally
- [ ] T039 [P] [US1] Add `BudgetDto` and `BudgetStatusDto` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/dto/`
- [ ] T040 [P] [US1] Add `Budget` and `BudgetStatus` domain models (paise `Long`, `alert_pct` as `Int?`) in `.../tracker/model/`
- [ ] T041 [US1] Add the Budget mappers in `.../tracker/mapper/`
- [ ] T042 [US1] Implement `BudgetRepository` in `.../tracker/repo/BudgetRepository.kt` against PostgREST with the `finance` schema profile header (ADR-0033), including the repository-boundary validations from data-model.md (`amount_paise > 0`, `alert_pct` 1–100, `period_month` is the 1st, no budget on an excluded category)
- [ ] T043 [P] [US1] Build `PaceRing` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/Rings.kt` on `FinancialHealthRing`'s base (impl plan §3.2), with the required `contentDescription` and a minimum 48dp target
- [ ] T044 [P] [US1] Extend `CategoryBarRow` with an optional month-position marker fraction — extend, do not add a parallel bar component (Article VI)
- [ ] T045 [US1] Implement `BudgetsViewModel` (`init { crashReporter.setModule("budgets") }`, `featureError` StateFlow, one `performanceTracer.trace("budgets_load")`) in `apps/finance/feature/plan/planning/`
- [ ] T046 [US1] Implement `BudgetsScreen.kt` (E2): pace ring, percentage used / amount left / total / days remaining, per-category bars against the marker, and the over-budget statement in words and money (FR-009 — never colour alone)
- [ ] T047 [US1] Wrap E2 in `FeatureHost(featureKey = "budgets", ...)` and register its route in the Plan tab's nested `NavHost`
- [ ] T048 [US1] Render the full state matrix on E2 — empty (verb CTA "Set your first budget"), loading, error, offline, signed-out (FR-048, DESIGN-SYSTEM §7)
- [ ] T049 [US1] Accessibility pass on E2: `contentDescription` on the pace ring and every bar, ≥48dp targets, tabular money, no colour-only meaning (NFR-6)

**Checkpoint**: Budgets works end to end and is independently demonstrable. This is the MVP.

---

## Phase 4: User Story 2 - Recover from a breached budget (Priority: P2)

**Goal**: Budget detail (E3) says what got the user here and what happens if they carry on, and lets
them raise the budget or set an earlier alert.

**Independent Test**: Open an over-budget category; the recovery insight's amounts recompute exactly
from that category's own recent transactions, and raising the budget updates every derived figure.

### Tests for User Story 2 (RED)

- [ ] T050 [P] [US2] `BudgetPaceEngine` recovery-projection tests — stated average and projected overage recompute from the same transaction fixture, citing `PLN-UI-003`, in `BudgetPaceEngineTest.kt`
- [ ] T051 [P] [US2] `BudgetDetailViewModel` test asserting the recovery insight renders through `SmartInsightCard`'s derived marker, citing `PLN-BR-005`, in `apps/finance/feature/plan/planning/src/test/.../BudgetDetailViewModelTest.kt`
- [ ] T052 [P] [US2] Test that raising a budget updates the current period's row only and recomputes every derived figure (FR-013), in the same test file
- [ ] T053 [P] [US2] Test that `alert_pct` persists and reads back as set (FR-014), in `BudgetRepositoryTest.kt`

### Implementation for User Story 2

- [ ] T054 [US2] Add the recovery projection to `BudgetPaceEngine` (category average × remaining occurrences → projected overage)
- [ ] T055 [US2] Extend `BudgetRepository` with the raise-budget update and the `alert_pct` write
- [ ] T056 [US2] Implement `BudgetDetailViewModel` in `apps/finance/feature/plan/planning/`
- [ ] T057 [US2] Implement `BudgetDetailScreen.kt` (E3): spend vs budget, over/under amount, days left, last-6-months `BarChart`, that category's recent transactions, and the recovery insight via `SmartInsightCard`
- [ ] T058 [US2] Add the "Raise budget" and "Alert me at 80%" actions, with `rememberDiscardGuard` on the raise form (N4)
- [ ] T059 [US2] Add the "see these transactions" jump to the Money tab's filtered ledger via `NavTarget` — never an import (Article III, contracts/routes.md Cross-feature navigation)
- [ ] T060 [US2] Wrap E3 in `FeatureHost`, register under E2 as its single parent (N2), and render the full state matrix
- [ ] T061 [US2] **Defer `PLN-FLOW-003` explicitly** — mark the row deferred in the QA catalog citing research R8 (its notification-initiated leg needs screen B2, Phase 6); verify and close the chain from E3 onward. A row left silently unticked fails the checkpoint; a deferral with a stated reason does not

**Checkpoint**: Budgets and budget recovery both work independently.

---

## Phase 5: User Story 3 - Track goals from what you already own (Priority: P3)

**Goal**: Goals (E4) and goal detail (E5) show progress derived purely from linked holdings, with
nothing moved, locked or duplicated.

**Independent Test**: Create a goal, link one holding whole and one at a partial earmark; progress
equals the sum of both contributions and **no transaction was written**.

### Tests for User Story 3 (RED)

- [ ] T062 [P] [US3] Repository test: goal progress equals Σ of a whole link and an earmarked link, **and no transaction-write endpoint was called**, citing `PLN-BR-001`, in `apps/finance/data/src/test/.../tracker/repo/GoalRepositoryTest.kt`
- [ ] T063 [P] [US3] Repository test: linking and unlinking write no transaction and change no holding value or quantity, citing `PLN-FLOW-002` (FR-018)
- [ ] T064 [P] [US3] Repository test: a second earmark that would push a holding's total past 10000 bps is refused with a readable error, citing FR-024 — asserts the trigger's refusal surfaces, not a client-side guess
- [ ] T065 [P] [US3] `GoalsViewModel` test over three fixtures (on track / needs ₹X per month / no funding linked yet) asserting the exact status text each, citing `PLN-UI-004`
- [ ] T066 [P] [US3] `GoalDetailViewModel` test asserting a partially-earmarked holding contributes its fraction, not its full value, citing `PLN-UI-005`

### Implementation for User Story 3

- [ ] T067 [P] [US3] Add `GoalDto`, `GoalLinkDto`, `GoalProgressDto` in `.../tracker/dto/`
- [ ] T068 [P] [US3] Add `Goal`, `GoalLink`, `GoalProgress` models in `.../tracker/model/` (`earmarkBps` as `Int`, research R1)
- [ ] T069 [US3] Add the Goal mappers in `.../tracker/mapper/`
- [ ] T070 [US3] Implement `GoalRepository` in `.../tracker/repo/GoalRepository.kt` reading `v_goal_progress`, with link create/update/delete and a readable mapping of the trigger's refusal
- [ ] T071 [US3] Implement `GoalsViewModel` and `GoalsScreen.kt` (E4): total saved of total target, active count, per-goal percentage / saved of target / target date / status, and the on-screen rule footnote (FR-019)
- [ ] T072 [US3] Implement `GoalDetailViewModel` and `GoalDetailScreen.kt` (E5) — **dark hero, reads `DhruvBrand`** (impl plan §3.1): `ProgressRing`, saved of target, on-track date, still-needed / per-month / months-left, "funded by" links, `TrendSparkline` projection, contribution insight via `SmartInsightCard`
- [ ] T073 [US3] Render each earmark as a percentage plus its rupee value — **not** the design's "56 g" quantity form, which is not derivable until holdings carry a quantity (research R1); state this in the screen's own copy rather than showing a wrong unit
- [ ] T074 [US3] Implement `LinkHoldingSheet.kt` (E5-link) as a sheet that dismisses down and never navigates (N3), reusing `SelectionSheet`, with whole-vs-earmark selection
- [ ] T075 [US3] Add the "funded by" jump to holding detail via the existing `OpenHolding(id)` target
- [ ] T076 [US3] Wrap E4/E5 in `FeatureHost(featureKey = "goals")`, register parents per contracts/routes.md, and render the full state matrix — including the **empty (no goals) vs unfunded (goal with no links)** distinction that `link_count` exists to support

**Checkpoint**: Goals work independently; the no-write property is proven at the repository level.

---

## Phase 6: User Story 4 - Choose a debt payoff order and see the trade-off (Priority: P4)

**Goal**: Debt payoff (E6) compares both strategies over the user's existing liabilities and states
what each choice costs.

**Independent Test**: With three debts of differing rate and balance, toggle strategies and confirm
the ordering rule for each and that the trade-off figures agree with both projections.

### Tests for User Story 4 (RED)

- [ ] T077 [P] [US4] Golden-value tests: highest-interest-first orders by rate descending, smallest-balance-first by balance ascending, over a ≥3-debt fixture, citing `PLN-BR-004`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/planning/DebtPayoffEngineTest.kt`
- [ ] T078 [P] [US4] Test that both tie-breaks are deterministic and stable across repeated runs (rate tie → balance desc → id; balance tie → rate desc → id), citing FR-026
- [ ] T079 [P] [US4] Test the non-amortising debt: minimum payment ≤ monthly interest terminates within the 600-month cap and is reported as not clearing, citing FR-031 (research R9)
- [ ] T080 [P] [US4] `DebtPayoffViewModel` test asserting the "N months slower, ₹X more interest" statement is internally consistent with both projections and carries the derived marker, citing `PLN-UI-006`

### Implementation for User Story 4

- [ ] T081 [US4] Implement `DebtPayoffEngine` in `apps/finance/data/src/main/java/com/dhruv/finance/data/planning/DebtPayoffEngine.kt` — ordering, month-by-month amortisation with the 600-month cap, the analytic non-amortising pre-check, and both strategies' projections
- [ ] T082 [US4] Wire the engine to Phase 2's `LiabilityRepository` (`rate_bps`, `emi_paise`, outstanding balance) — **no new debt table or repository** (data-model.md); qualify the schema per the carried-over `liabilities_meta` note
- [ ] T083 [US4] Implement `DebtPayoffViewModel` with the extra-per-month input recomputing debt-free date, interest saved and months saved together, off the main thread
- [ ] T084 [US4] Implement `DebtPayoffScreen.kt` (E6): strategy toggle via the existing `SegmentedRow`, the four summary figures, the ranked pay-order list with each debt's rate and projected clear date, and the trade-off statement via `SmartInsightCard`
- [ ] T085 [US4] Add the debt-row jump to liability detail via the existing `OpenLiability(id)` target
- [ ] T086 [US4] Wrap E6 in `FeatureHost(featureKey = "debtpayoff")`, register under E1, and render the full state matrix including the no-debts and single-debt cases (spec Edge Cases)

**Checkpoint**: Debt payoff works independently over Phase 2's liabilities.

---

## Phase 7: User Story 5 - Know what is insured, what renews, what is missing (Priority: P5)

**Goal**: Insurance (E7) and policy detail (E8) surface renewals, rule-of-thumb cover and gaps.

**Independent Test**: Record one policy renewing inside the window and one outside; the banner
appears only for the first, with correct days remaining and the lapse consequence.

### Tests for User Story 5 (RED)

- [ ] T087 [P] [US5] Golden-value test: rule-of-thumb cover = 10 × annual income + outstanding loans, shortfall = rule-of-thumb − actual cover, citing `INS-BR-001`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/planning/InsuranceCoverEngineTest.kt`
- [ ] T088 [P] [US5] Test the sparse-data case: with `months_observed < 12` the engine reports the basis rather than annualising silently (research R3, spec Edge Cases)
- [ ] T089 [P] [US5] `InsuranceViewModel` test: the renewal banner renders only inside the window, with correct days remaining and lapse consequence, citing `INS-UI-001`
- [ ] T090 [P] [US5] `InsuranceViewModel` test: the gaps section names the specific missing risk category from `InsuranceConfig`'s checklist, citing `INS-UI-002`
- [ ] T091 [P] [US5] Repository test: marking a premium paid appends a `policy_premiums` row and clears that policy's renewal banner, citing `INS-FLOW-001`; and an UPDATE/DELETE against that row is refused (Article IX)

### Implementation for User Story 5

- [ ] T092 [US5] Implement `InsuranceCoverEngine` in `apps/finance/data/src/main/java/com/dhruv/finance/data/planning/InsuranceCoverEngine.kt` — rule-of-thumb, shortfall, and the gap checklist evaluation
- [ ] T093 [P] [US5] Add `PolicyDto`, `PolicyPremiumDto`, `AnnualIncomeDto` in `.../tracker/dto/`
- [ ] T094 [P] [US5] Add `Policy`, `PolicyPremium` models in `.../tracker/model/` with the append-only `kind`/`frequency` enum constants
- [ ] T095 [US5] Add the Policy mappers and implement `PolicyRepository` in `.../tracker/repo/PolicyRepository.kt`, reading `v_annual_income` for the cover comparison
- [ ] T096 [US5] Implement `InsuranceViewModel` and `InsuranceScreen.kt` (E7): renewal banner via `InfoBanner`/`StatusBadge`, life cover vs rule of thumb with the formula stated on screen, the named shortfall, LIFE/HEALTH grouping with renewal date / premium / sum assured / floater info, and the gaps section
- [ ] T097 [US5] Implement `PolicyDetailScreen.kt` (E8) with its ViewModel: sum assured, premium due date, type, policy number, premium + frequency, cover-until with the corresponding age, nominee name/relation/share, riders, documents, and premiums-paid history
- [ ] T098 [US5] Implement document attach and view against `policies.documents` as **device-local paths** (research R6), and state on E8 that documents do not follow the user to a new device this phase
- [ ] T099 [US5] Implement "Mark as paid" (appends a premium) and "Remind me" (persists `remind_days_before`; delivery is Phase 6 per research R8)
- [ ] T100 [US5] Wrap E7/E8 in `FeatureHost(featureKey = "insurance")`, register parents, render the full state matrix, and handle the lapsed-policy and incomplete-nominee cases as surfaced states rather than errors (spec Edge Cases)

**Checkpoint**: Insurance works independently.

---

## Phase 8: User Story 6 - Retirement projection with assumptions in plain sight (Priority: P6)

**Goal**: Retirement (E9) shows a projected corpus against a target with all five assumptions on the
same screen, and saves scenarios.

**Independent Test**: Vary one assumption at a time and confirm the corpus moves in the expected
direction each time, with all five assumptions readable beside the result.

### Tests for User Story 6 (RED)

- [ ] T101 [P] [US6] Golden-value tests: varying each assumption one at a time (retire age, inflation, pre-return, post-return, life expectancy) moves the projected corpus in the expected direction, citing `RET-BR-001`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/planning/RetirementProjectionEngineTest.kt`
- [ ] T102 [P] [US6] Test the contradictory-input cases — retire age ≤ current age, life expectancy < retire age, inflation above both returns — each produces a stated result, never a negative corpus presented as fact (spec Edge Cases)
- [ ] T103 [P] [US6] `RetirementViewModel` test: base / optimistic / cautious produce three distinct corpus figures (not aliased), citing `RET-UI-001`
- [ ] T104 [P] [US6] `RetirementViewModel` test: all five assumption fields are present in the same screen state as the projected corpus, citing `RET-UI-002`
- [ ] T105 [P] [US6] Repository test: a saved scenario's assumption values reappear on reload, and **no corpus figure is persisted**, citing `RET-FLOW-001` (FR-049)

### Implementation for User Story 6

- [ ] T106 [US6] Implement `RetirementProjectionEngine` in `apps/finance/data/src/main/java/com/dhruv/finance/data/planning/RetirementProjectionEngine.kt` — corpus projection, target corpus (today's spend inflated to retirement, sustained to life expectancy at the post-retirement return; spec Assumptions), required monthly, bounded horizon (research R9)
- [ ] T107 [P] [US6] Add `RetirementScenarioDto` and the `RetirementScenario` model with the six-key `assumptions` shape from data-model.md
- [ ] T108 [US6] Implement `RetirementRepository` in `.../tracker/repo/RetirementRepository.kt`, seeding `monthly_spend_today_paise` from `v_annual_income` (research R3) and validating the assumptions shape at the repository boundary
- [ ] T109 [US6] Implement `RetirementViewModel` computing all three scenarios from `RetirementConfig`'s presets, off the main thread
- [ ] T110 [US6] Implement `RetirementScreen.kt` (E9) — **dark hero, reads `DhruvBrand`**: scenario `SegmentedRow`, projected corpus / target / % of target / shortfall, `TrendSparkline` corpus projection, the gap insight via `SmartInsightCard`, and all five assumption fields on the same screen (never a drill-in)
- [ ] T111 [US6] Implement "Save this scenario" with `rememberDiscardGuard` on unsaved assumption edits (N4)
- [ ] T112 [US6] Wrap E9 in `FeatureHost(featureKey = "retirement")`, register under E1, and render the full state matrix

**Checkpoint**: Retirement works independently.

---

## Phase 9: User Story 7 - Plan tab leads with planning (Priority: P7)

**Goal**: The Plan root (E1) presents live modules first and demotes the four calculators to a strip
below (ADR-0027).

**Independent Test**: Open the Plan tab; the two live groups appear above the calculator strip, and
each row reaches its module with the Plan tab still selected.

**Note**: This is shell work in `:apps:finance:app`, not a feature module — E1 is shell-owned per the
module-standard `HOM`/`PLN` correction and impl plan §6.

### Tests for User Story 7 (RED)

- [ ] T113 [P] [US7] Test that E1 renders live modules before the calculator strip, citing `PLN-FLOW-001`, in `apps/finance/app/src/test/.../ui/plan/PlanLauncherTest.kt`
- [ ] T114 [P] [US7] Test that a module whose flag is off degrades to that one row's disabled state while the tab and the calculators keep working (FR-004)
- [ ] T115 [P] [US7] Test that opening any live row keeps the Plan tab selected with a single back path (N1/N2, FR-003)

### Implementation for User Story 7

- [ ] T116 [US7] Rewrite `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/plan/PlanLauncher.kt` to E1: a `THIS MONTH` group (budgets, goals) and a `LONG RUN` group (debt payoff, insurance, retirement), with the existing four calculators as a strip below
- [ ] T117 [US7] Wire the live summary rows — budgets: spend of budget total plus the number over; goals: active count plus total saved of total target — reading the same repositories the feature modules use (app → data is allowed; no `feature → feature` edge)
- [ ] T118 [US7] Dispatch each row through `OpenPlanModule(...)`, and each calculator through the existing `OpenPlanTool(...)`
- [ ] T119 [US7] Give each row its own no-data and flag-off text (FR-004) — "No budgets yet" and "Budgets are off" are different sentences with different actions
- [ ] T120 [US7] Keep E1 reachable while signed out (it hosts the offline calculators) and confirm it carries no `FeatureHost` key and no consent gate of its own (contracts/routes.md)

**Checkpoint**: All seven stories are independently functional.

---

## Phase 10: Polish & Cross-Cutting Concerns

- [ ] T121 [P] Verify FR-047 across E3, E4, E5, E6 and E9 — every computed sentence renders through the extended `SmartInsightCard`, covered by T032's single assertion rather than five separate ones
- [ ] T122 [P] Accessibility sweep across all nine screens: ≥48dp targets, ≥56dp list rows, 4.5:1 contrast in both themes, `contentDescription` on every ring/chart/icon-only action, money as tabular numerals that wrap or compact rather than ellipsise (NFR-6, DESIGN-SYSTEM §9)
- [ ] T123 [P] No-hardcoding audit: zero raw dp/sp/hex literals in the nine screen files; E5/E9 read `DhruvBrand`, everything else `LocalDhruvNextColors`; all screen data in the three Config files (Article V)
- [ ] T124 [P] Confirm every new user-visible string lives in `strings.xml` from birth (DESIGN-SYSTEM §10)
- [ ] T125 Run `./gradlew checkTrackerMoneyPrecision` and confirm it passes **with the four engines outside `tracker/`** — if it fails on an engine file, move the file back out; do not relax the task's regex (research R2)
- [ ] T126 **Sec step**: RLS verification against `dhruv-dev` — every one of the six tables refuses another user's rows; `policy_premiums` refuses UPDATE and DELETE; the `goal_links` earmark trigger refuses an over-cap write issued directly against PostgREST, bypassing the app
- [ ] T127 **Sec step**: DPDP pass — all five flags carry `requiresConsent: true`, no PostgREST call is reachable before the "Sync my financial records" switch is on (Article VIII), and `delete_my_data()` removes every row across all six tables (NFR-1, quickstart scenario 20)
- [ ] T128 Run the full `quickstart.md` manual scenario list (20 scenarios) and record the results
- [ ] T129 Close all 21 catalog rows in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §5–§7 — 20 closed, `PLN-FLOW-003` deferred with the research R8 citation (T061) — and update the §13 coverage-summary table
- [ ] T130 Run `python scripts/ci/regression_summary.py` and confirm all three modules appear as their own rows, not under `(other)`; record both the module totals and the engine/ViewModel logic totals side by side
- [ ] T131 Ratchet `globalLineFloor` in the root `build.gradle.kts` **once**, to just under the newly measured merged coverage — never above it (Article X)
- [ ] T132 [P] Write the five deltas back into `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`'s Phase 4 section as a dated note: research R1 (`earmark_bps` replaces `earmark_qty`), R2 (engine placement), R3 (derived income), R4 (three added columns, three views), R9 (payoff termination and tie-breaks)
- [ ] T133 [P] Update the §7 spec-kit tracking table row for Phase 4 to record tasks as generated and the phase as implemented
- [ ] T134 Final gate: `./gradlew regressionCheck` green, both schema guards green, coverage floor not regressed, every catalog row closed or explicitly deferred (constitution Development Workflow step 7)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies — starts immediately
- **Foundational (Phase 2)**: depends on Setup — **BLOCKS all user stories** (constitution step 1 before steps 3/4)
- **User Stories (Phases 3–9)**: all depend on Foundational
- **Polish (Phase 10)**: depends on every story the phase intends to ship

### External phase dependencies (outside this feature)

This feature reads Phases 2 and 3 and adds neither. If either has not landed, the affected story
cannot complete:

| Story | Blocked by | On what |
|---|---|---|
| US1, US2 | Phase 3 | `transactions`, `categories`, `v_category_spend` |
| US3 | Phase 2 | `holdings`, `valuations`, `v_latest_valuation` |
| US4 | Phase 2 | `liabilities_meta` (`rate_bps`, `emi_paise`, balance) |
| US5, US6 | Phase 3 | `v_month_summary` (feeds `v_annual_income`) |
| US5 | Phase 3 | `StatusBadge`, `InfoBanner` (batch B7) |
| all | Phase 2 | `NxTextField` error/helper state, `NxButton` sizes/loading |

US3 and US4 depend only on Phase 2, so they are the stories that can proceed if Phase 3 slips.

### User Story Dependencies

- **US1 (P1)**: after Foundational — no dependency on other stories
- **US2 (P2)**: after Foundational — shares `BudgetPaceEngine` and `BudgetRepository` with US1, so it is cheapest immediately after US1, but E3 is independently testable against a seeded budget
- **US3 (P3)**: after Foundational — fully independent of US1/US2
- **US4 (P4)**: after Foundational — fully independent
- **US5 (P5)**: after Foundational — fully independent
- **US6 (P6)**: after Foundational — independent; shares only `v_annual_income` with US5
- **US7 (P7)**: after Foundational — E1 renders whichever modules exist, so it can ship at any point; its live rows become meaningful as each preceding story lands

### Within Each User Story

- Tests are written and MUST fail before implementation (Article I, non-negotiable)
- Engine → DTO/model/mapper → repository → ViewModel → screen → `FeatureHost` + states → accessibility
- Story complete before moving to the next priority

### Parallel Opportunities

- T002/T003, T005/T006/T007, T009 in Setup
- T011–T019 in Foundational — nine independent declarative schema files
- Every `### Tests` block is fully parallel within its story (different test files)
- DTO/model tasks within a story (T039/T040, T067/T068, T093/T094)
- Component work is parallel with data work: T043/T044 alongside T038–T042
- US3, US4, US5 and US6 can be built by different people simultaneously once Foundational is done — they share no file
- T121–T124 and T132/T133 in Polish

---

## Parallel Example: User Story 1

```bash
# All US1 tests together (RED — confirm they fail before any implementation):
Task: "Golden-value pace tests (PLN-BR-002) in .../planning/BudgetPaceEngineTest.kt"
Task: "Excluded-category test (PLN-BR-003) in .../planning/BudgetPaceEngineTest.kt"
Task: "BudgetRepository view-source test in .../tracker/repo/BudgetRepositoryTest.kt"
Task: "BudgetsViewModel pace-statement tests (PLN-UI-001) in .../BudgetsViewModelTest.kt"
Task: "CategoryBarRow marker test (PLN-UI-002) in libs/core/src/test/.../CategoryBarRowTest.kt"

# Then data and component work in parallel:
Task: "Add BudgetDto and BudgetStatusDto in .../tracker/dto/"
Task: "Add Budget and BudgetStatus models in .../tracker/model/"
Task: "Build PaceRing in libs/core/.../ui/components/Rings.kt"
Task: "Extend CategoryBarRow with a month-position marker"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1 Setup
2. Phase 2 Foundational (CRITICAL — blocks everything)
3. Phase 3 User Story 1
4. **STOP and VALIDATE**: quickstart scenarios 1 and 2 pass independently
5. Demo — a budget module that states pace honestly is a shippable increment on its own

### Engine-first fast path (research R10)

The four engines are pure functions over plain data with no Android, no coroutines and no fakes.
Their test tasks — T033/T034 (budget pace), T077–T079 (payoff), T087/T088 (insurance cover),
T101/T102 (retirement) — are mutually independent and can be pulled forward as **one parallel batch
immediately after Foundational**, ahead of their stories' screens.

This is worth doing deliberately. The engines carry every correctness-critical rule in the feature,
they are the code most likely to be wrong in a way no reviewer would catch, and they are the only
code the JVM coverage gate sees completely. If the phase runs short, the right things to have
finished are the engines — a screen over a wrong projection is worse than no screen.

### Incremental Delivery

1. Setup + Foundational → schema live, navigation vocabulary ready
2. US1 → budgets on pace → demo (MVP)
3. US2 → breach recovery → demo
4. US3 → goals → demo
5. US4 → debt payoff → demo
6. US5 → insurance → demo
7. US6 → retirement → demo
8. US7 → Plan root rewrite → the tab now reads as designed
9. Polish → Sec pass, coverage ratchet, catalog closure, doc write-backs

### Parallel Team Strategy

After Foundational, US3 / US4 / US5 / US6 share no file and can be built simultaneously. US1 and US2
share `BudgetPaceEngine` and `BudgetRepository`, so one person should own both. US7 is shell work in
a different module and can proceed alongside anything.

---

## Notes

- [P] = different files, no dependencies on incomplete tasks
- Every Backend and Android task cites its catalog scenario ID; a test without one is a
  review-blocking finding (Article I)
- `PLN-FLOW-003` is the single known deferral — deferred with a stated reason (research R8), not
  skipped. A row left silently unticked fails the checkpoint
- Do not move the four engines into `tracker/` (research R2), and do not relax
  `checkTrackerMoneyPrecision` to accommodate them
- Commit after each task or logical group; stop at any checkpoint to validate a story independently