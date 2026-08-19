# Implementation Plan: Plan Live Modules (Phase 4)

**Branch**: `003-plan-live-modules` | **Date**: 2026-08-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `apps/finance/specs/003-plan-live-modules/spec.md`

**Note**: This plan is the spec-kit-shaped restatement of
`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §3/§4/§5/§6/§7
(Phase 4) — that document remains the source of truth for anything not repeated here. Five places
where this plan **adds to or corrects** it are called out explicitly in `research.md` (R1 earmark
representation, R2 engine placement, R3 derived-income source, R4 extra columns, R9 payoff
termination) and must be written back into that plan's Phase 4 section as a dated note when this
feature is implemented.

## Summary

Turn the Plan tab from a calculator launcher into live planning: a budget module that states whether
spending is on pace rather than leaving it to be inferred, a budget detail that says what to do
about a breach, goals whose progress is nothing but the current value of holdings the user linked to
them, a debt payoff comparison that shows the trade-off instead of steering, an insurance module
that names renewals and uncovered risks, a retirement projection with its assumptions on the same
screen as its answer, and a Plan root that leads with all of that and demotes the four calculators
to a strip below (ADR-0027).

Technical approach: six new tables and three new views in the `finance` Postgres schema (ADR-0033);
**three pure-Kotlin calculation engines placed deliberately outside `tracker/`** so the tracker
money-precision guard stays blunt and correct (R2); three new Gradle modules
(`:feature:planning`, `:feature:insurance`, `:feature:retirement`) following the existing template,
with the Plan root E1 staying shell-owned in `:apps:finance:app`; one genuinely new `:libs:core`
component (`PaceRing`) plus three extensions of existing ones; and one shared derived-insight
mechanism rather than five per-screen ones, because FR-047 cuts across every screen in this feature.

## Technical Context

**Language/Version**: Kotlin 2.2, Jetpack Compose (Material 3), minSdk 26.

**Primary Dependencies**: Koin (DI), Retrofit + Moshi + OkHttp against Supabase PostgREST (Phase 1's
`tracker/net` stack, extended not replaced), kotlinx.coroutines/Flow. **No new dependency** — the
three engines are plain Kotlin stdlib + `java.math.BigDecimal`; see `research.md` R2.

**Storage**: Supabase (PostgreSQL + RLS), `finance` schema. New tables: `budgets`, `goals`,
`goal_links`, `policies`, `policy_premiums`, `retirement_scenarios`. New views: `v_budget_status`,
`v_goal_progress`, `v_annual_income`. Policy **documents** are device-local this phase, same
decision and reasoning as Phase 3's receipts (R6). No local Room storage for tracker data
(ADR-0014, unchanged).

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine + in-memory fakes / MockWebServer at the
HTTP boundary (Robolectric-SQLite is unreliable on this Windows dev machine — standing project
constraint). The three engines are pure functions and get **golden-value fixture tests** with no
Android dependency at all — `RET-BR-001` explicitly asks for this shape. SQL-layer rules (RLS, the
`goal_links` over-earmark constraint, view definitions) are verified against the dev Supabase
project at the Sec step, not in JVM unit tests. Every test cites its `PLN-*`/`INS-*`/`RET-*`
scenario ID (constitution Article I).

**Coverage**: JaCoCo merged report + `jacocoCoverageVerification` floor via `./gradlew
regressionCheck` (ADR-0013, Article X). Each of the three new modules contributes nothing until it
is added to **both** `coveredModules` (root `build.gradle.kts`) and `_FEATURES`
(`scripts/ci/regression_summary.py`); this phase does all six registrations in Setup, not at the
end. This phase should measurably *raise* the merged number rather than dilute it: the engines are
pure logic with no Compose in them, which is the highest-yield code the JVM gate can see. The floor
is ratcheted once, at the phase checkpoint, to just under the newly measured merged coverage.

**Target Platform**: Android (existing `:apps:finance:app` shell), API 26+.

**Project Type**: Mobile app feature modules inside the existing Gradle monorepo.

**Performance Goals**: No screen in this feature sums a ledger to draw itself (NFR-8) — budget
consumption comes from `v_budget_status` (built on Phase 3's `v_category_spend`), goal progress from
`v_goal_progress`, annual income from `v_annual_income`. The two iterative projections (debt payoff
amortisation, retirement corpus) are in-memory loops over a bounded horizon, run off the main
thread, with a hard month cap (R9) so a pathological input cannot hang the screen.

**Constraints**: Money is `Long` paise on every stored and transported path (Article VII). The
engines use `BigDecimal` internally for rates and compounding and return paise `Long` — that is
exactly the split Article VII describes, and it is the reason for the placement decision in R2.
Feature modules reach data only through `:apps:finance:data` repositories; every cross-feature jump
in this phase (E3 → ledger, E5 → holding detail, E6 → liability detail) goes through `NavTarget`,
never an import (Article III). New visuals extend an existing `:libs:core` component before a new
one is proposed (Article VI). No raw dp/sp/hex literal in a screen file (Article V). No PostgREST
call before the "Sync my financial records" consent switch is on — inherited from Phase 1's
`ConsentInterceptor`, no new client is constructed (Article VIII).

**Scale/Scope**: 9 screens (E1 revised + E2–E9), 3 new Gradle modules, 6 new tables + 3 views, 5 new
feature flags, 4 new `NavTarget` cases, 21 QA rows to satisfy (13 `PLN-*`, 4 `INS-*`, 4 `RET-*`).

**Dependency on Phases 2 and 3**: this phase reads what both produce and adds neither.

| Needs | From | Used by |
|---|---|---|
| `holdings`, `valuations`, `v_latest_valuation` | Phase 2 | goal progress (FR-017) |
| `liabilities_meta` (`rate_bps`, `emi_paise`, balance) | Phase 2 | debt payoff (FR-025) — no debt table is created here |
| `NxTextField` error + helper state, `NxButton` sizes/loading | Phase 2 | every assumption and amount field (E2 raise, E9 assumptions) |
| `transactions`, `categories`, `v_category_spend`, `v_month_summary` | Phase 3 | budget consumption (FR-010), recovery insight (FR-012), annual income (R3) |
| `StatusBadge`, `InfoBanner` (batch B7) | Phase 3 | renewal banner and lapse status (FR-034) |
| `SelectionSheet` (B9), `NxSelect` (B6) | Phase 2/3 | holding picker, policy type, scenario picker |

If this phase somehow starts before either, those component rows move into this phase rather than
being duplicated — the tables and views do not, because the features that own them do.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Article | Check | Status |
|---|---|---|
| I. Test-First | Every Backend/Android task in `tasks.md` cites a `PLN-*`/`INS-*`/`RET-*` scenario ID; RED before GREEN. The three engines are pure functions, so RED is genuinely cheap here — golden-value fixtures exist before any screen does | PASS (enforced at task-authoring time) |
| II. Scenarios Before Code | `PLN-*` (13), `INS-*` (4), `RET-*` (4) already written and reviewed 2026-08-09 — catalog §5–§7 | PASS — pre-satisfied |
| III. Module Boundaries | `:feature:planning`/`:insurance`/`:retirement` each depend only on `:libs:core` + `:apps:finance:data` + `:libs:settings`. E1 is shell-owned (`:apps:finance:app`, module-standard `HOM`/`PLN` correction) and reads the same repositories. E3 → ledger, E5 → holding detail, E6 → liability detail are `NavTarget` dispatches, not imports | PASS by construction — see Project Structure and `contracts/routes.md` |
| IV. Fault Isolation | E2–E9 wrapped in `FeatureHost`; **five new flags** (`budgets`, `goals`, `debtpayoff`, `insurance`, `retirement`) added to `dhruv-finance.json`, each `requiresConsent: true`. E1 renders each root row's state from its own flag, so a disabled module degrades to one row, not a broken tab | PASS — flags added by this phase (impl plan §5.5) |
| V. No Hardcoding | All new screens read `DhruvNextType`/`Spacing`/`Radii`/`LocalDhruvNextColors`; E5/E9 dark-hero surfaces read `DhruvBrand` (impl plan §3.1). Screen data in `PlanningConfig.kt`/`InsuranceConfig.kt`/`RetirementConfig.kt` — including the insurance gap-category checklist and the three scenario presets, which are exactly the kind of screen-level data the rule exists for | PASS by construction, verified at review |
| VI. Component Reuse | Only `PaceRing` is genuinely new (verified absent by symbol search; `FinancialHealthRing` is its base per impl plan §3.2). `CategoryBarRow` gains a month-position marker, `SmartInsightCard` becomes the single derived-insight carrier, `TrendSparkline` draws both projections — three extensions, zero parallel components | PASS — see `research.md` R5 |
| VII. Money Is Exact | Every stored and transported amount is `bigint`/`Long` paise. Rates are `int` basis points; earmarks are `int` basis points (R1). The engines' internal `BigDecimal` lives **outside `tracker/`** (R2) so it is neither on a write path nor in the guard's scan set | PASS — see the Article VII note below |
| VIII. Consent Before Network | All five flags ship `requiresConsent: true`, already mapped to the "Sync my financial records" A3 switch in the impl plan's consent table; no new HTTP client is built | PASS — inherited gate |
| IX. Append-Only History | `policy_premiums` gets SELECT+INSERT policies only, same shape as `valuations`. Policy `kind`/`frequency`, scenario `kind`, and the `PlanModule` enum persist as TEXT and are append-only constants | PASS by construction |
| X. Coverage Ratchets | Three modules registered in `coveredModules` + `_FEATURES` in Setup, not at the end; merged floor ratcheted once at the checkpoint, only to the measured value | PASS — with the wiring made explicit tasks; see the Coverage note below |
| XI. Stack Is Fixed | No new dependency; Retrofit/Moshi/OkHttp + Koin + stdlib/`BigDecimal` only | PASS |

No violations. **Complexity Tracking is empty.**

**Article VII note (why placement, not an exemption).** `checkTrackerMoneyPrecision` scans
`apps/finance/data/src/main/**/tracker/**/*.kt` for the bare words `Double` and `Float`. That regex
is deliberately blunt and it should stay blunt. A retirement projection genuinely needs fractional
inflation and return arithmetic, so putting the engines under `tracker/` would have forced one of
two bad outcomes: weakening the guard's pattern for every future tracker file, or contorting the
engine to avoid a word. Neither is acceptable, and neither is necessary — Article VII's own wording
already separates "pure calculation engines" from "a tracker write path". The engines therefore live
at `data/.../planning/`, a sibling of `tracker/`, taking paise `Long` in and returning paise `Long`
out. `research.md` R2 records this so nobody later "tidies" them into `tracker/` and trips CI.

**Coverage note.** `coveredModules` and `_FEATURES` are hand-maintained lists, so the gate only
covers modules someone remembered to add. Re-verified while writing this plan, and the gap Phase 3's
plan reported has **not** been closed: `:apps:finance:feature:onboarding` is in `coveredModules` but
still missing from `_FEATURES`, so its coverage reports as `(other)` today. Phase 2's `networth` and
Phase 3's `money` are in neither list yet (both phases are spec'd, not implemented). This phase adds
its own three modules to both lists and does not silently patch the other three — that belongs to
whichever phase lands them.

**Post-Phase-1 re-check.** Four design decisions were re-tested against the gates after
`data-model.md` was written:

1. **`goal_links.earmark_bps` replaces the implementation plan's `earmark_qty numeric`** (R1).
   Checked against Article VII: basis points are integers, so this *removes* a `numeric` from the
   schema rather than adding one. Checked against Article IX: no shipped constant is renamed — the
   column has never existed. Not a violation; it is the stricter option.
2. **Debt payoff creates no debt table** — it reads Phase 2's `liabilities_meta`. Checked against
   Article III: `:feature:planning` reaches it through `LiabilityRepository` in `:data`, the same
   way `:feature:networth` does. Two features reading one repository is not a `feature → feature`
   edge.
3. **A trigger enforces the per-holding earmark cap** (FR-024). Same reasoning ADR-0029 used for
   `ConsentInterceptor` and Phase 3 used for its audit trigger: a rule that must not be bypassable
   belongs in the layer that cannot be bypassed. Article I is still satisfied — a repository-level
   test asserts the refusal (`FR-024`), and the SQL assertion runs at the Sec step.
4. **Projections are never stored** — `retirement_scenarios` persists assumptions only, never a
   computed corpus. Checked against Article IX and spec FR-049: storing a derived figure alongside
   recorded facts is how a stale number later gets read as a real one. Correct, not a violation.

Gate remains PASS.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/003-plan-live-modules/
├── plan.md               # This file
├── research.md           # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/routes.md   # Phase 1 output — route registry rows, NavTarget additions, flags
├── checklists/requirements.md
└── tasks.md              # Phase 2 output (/speckit-tasks — not created by this command)
```

### Source Code (repository root)

**Structure Decision**: Mobile app inside the existing Gradle monorepo — three new feature modules
under the Plan tab-owner bucket, one shell rewrite, one new migration. `feature/loans` remains the
reference implementation for module shape (impl plan §6); `feature/plan/<name>/` is the physical
location under the bucket scheme (`apps/finance/feature/README.md`), with each Gradle coordinate
remapped via `projectDir` exactly like every existing module.

**Planning is one module, not three** (impl plan §6): budgets, goals and debt payoff share
repositories and cross-link, so splitting them would force either a forbidden `feature → feature`
edge or triplicated ViewModel logic. Insurance and retirement are separate because they share
nothing with planning but the tab.

```text
apps/finance/
├── data/src/main/java/com/dhruv/finance/data/
│   ├── planning/                   # NEW — pure engines, deliberately OUTSIDE tracker/ (R2)
│   │   ├── BudgetPaceEngine.kt     #   pace fraction, ahead/behind, recovery projection
│   │   ├── DebtPayoffEngine.kt     #   avalanche/snowball order + amortisation, month cap (R9)
│   │   ├── RetirementProjectionEngine.kt  # corpus, target, required monthly
│   │   └── InsuranceCoverEngine.kt #   rule-of-thumb cover, shortfall, gap checklist
│   └── tracker/
│       ├── dto/                    # + BudgetDto, BudgetStatusDto, GoalDto, GoalLinkDto,
│       │                            #   GoalProgressDto, PolicyDto, PolicyPremiumDto,
│       │                            #   RetirementScenarioDto, AnnualIncomeDto
│       ├── model/                  # + Budget, BudgetStatus, Goal, GoalLink, GoalProgress,
│       │                            #   Policy, PolicyPremium, RetirementScenario
│       ├── mapper/                 # + one mapper per model
│       └── repo/                   # + BudgetRepository, GoalRepository, PolicyRepository,
│                                    #   RetirementRepository  (debt reads Phase 2's
│                                    #   LiabilityRepository — no new debt repository)
├── feature/plan/planning/          # NEW MODULE — :apps:finance:feature:planning  (E2–E6)
│   ├── build.gradle.kts            # dhruv.android.library + dhruv.android.compose
│   ├── BudgetsScreen.kt            # E2
│   ├── BudgetDetailScreen.kt       # E3
│   ├── GoalsScreen.kt              # E4
│   ├── GoalDetailScreen.kt         # E5 (dark hero — DhruvBrand)
│   ├── LinkHoldingSheet.kt         # E5 holding picker (sheet, N3)
│   ├── DebtPayoffScreen.kt         # E6
│   ├── <Screen>ViewModel.kt        # one per screen, existing convention
│   ├── PlanningConfig.kt           # screen-level data/config (no-hardcoding rule)
│   └── di/PlanningModule.kt        # Koin module, aggregated in CalculatorApplication
├── feature/plan/insurance/         # NEW MODULE — :apps:finance:feature:insurance  (E7, E8)
│   ├── InsuranceScreen.kt          # E7
│   ├── PolicyDetailScreen.kt       # E8
│   ├── InsuranceConfig.kt          # gap-category checklist lives here, not inline
│   └── di/InsuranceModule.kt
├── feature/plan/retirement/        # NEW MODULE — :apps:finance:feature:retirement  (E9)
│   ├── RetirementScreen.kt         # E9 (dark hero — DhruvBrand)
│   ├── RetirementConfig.kt         # the three scenario presets live here
│   └── di/RetirementModule.kt
└── app/src/main/java/com/dhruv/finance/app/ui/plan/
    └── PlanLauncher.kt             # ~ REWRITTEN to E1: live modules first, calculator strip below
libs/core/src/main/kotlin/com/dhruv/core/
├── ui/components/                  # + PaceRing (B3 — new; FinancialHealthRing is its base)
│                                    # ~ CategoryBarRow gains a month-position marker
│                                    # ~ SmartInsightCard becomes the derived-insight carrier
└── navigation/NavTarget.kt         # + OpenPlanModule(PlanModule), OpenBudget(categoryId),
                                     #   OpenGoal(id), OpenPolicy(id); + enum PlanModule
platform/feature-flags/dhruv-finance.json   # + budgets, goals, debtpayoff, insurance, retirement
build.gradle.kts                    # + the three modules in `coveredModules`;
                                    #   `globalLineFloor` ratcheted at the checkpoint
scripts/ci/regression_summary.py    # + "planning", "insurance", "retirement" in `_FEATURES`
supabase/
├── schemas/finance/10_tables/      # + budgets, goals, goal_links, policies, policy_premiums,
│                                    #   retirement_scenarios (declarative, ADR-0032)
├── schemas/finance/20_views/       # + v_budget_status, v_goal_progress, v_annual_income
├── schemas/finance/30_functions/   # + fn_goal_link_earmark_guard (trigger fn)
├── schemas/public/30_functions/    # ~ delete_my_data() extended to the six new tables
└── migrations/<timestamp>_plan_phase4.sql   # via `supabase db diff`, grants hand-added (ADR-0032)
web/src/shared/types/database.ts    # regenerated: supabase gen types --schema public,finance
```

Tests mirror this tree under each module's `src/test/` — existing project convention, no separate
`tests/` root. The four engines' tests live in `:apps:finance:data`'s `src/test/`, as plain JVM
tests with no Android or coroutine machinery.

## Complexity Tracking

*(empty — no Constitution Check violations)*