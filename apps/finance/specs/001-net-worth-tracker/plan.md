# Implementation Plan: Net Worth Tracker (Phase 2)

**Branch**: `001-net-worth-tracker` | **Date**: 2026-08-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `apps/finance/specs/001-net-worth-tracker/spec.md`

**Note**: This plan is the spec-kit-shaped restatement of
`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §3.2/§5/§6/§7
(Phase 2) — that document remains the source of truth for anything not repeated here; this plan
does not diverge from it.

## Summary

Let a signed-in user record what they own/owe (holdings), record dated values for each (append-only
valuations), see a computed net-worth total broken down by category, drill into a holding's history
and trend, manage liabilities with payoff-progress and a prepay-savings projection, and see all of
it summarized on the app's Home screen. Technical approach: extend the existing
`:apps:finance:data` module with a `liabilities_meta` table and two server-side aggregation views
on top of Phase 1's `holdings`/`valuations` schema; add one new `:apps:finance:feature:networth`
module (screens C1–C7) following the existing feature-module template; replace the placeholder
`DashboardScreen` with the real Home (01); build the three still-missing design-system component
batches (B3 charts, B6 select, B9 selection-sheet) plus two component extensions (`NxTextField`
error state, `NxButton` sizes/loading) that C4/C5's forms need and nothing before them did.

## Technical Context

**Language/Version**: Kotlin 2.2, Jetpack Compose (Material 3), targeting existing minSdk 26.

**Primary Dependencies**: Koin (DI), Room (existing, unaffected by this feature), Retrofit + Moshi +
OkHttp against Supabase PostgREST (Phase 1's `tracker/net` stack, extended not replaced),
kotlinx.coroutines/Flow. No new dependency is added by this feature — see `research.md` R1.

**Storage**: Supabase (PostgreSQL + RLS), via the `holdings`/`valuations` tables Phase 1 already
shipped, plus this phase's new `liabilities_meta` table and two views (`v_latest_valuation`,
`v_net_worth_by_sector`). No local Room storage for tracker data (ADR-0014, unchanged).

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine (StateFlow assertions) + fakes for the
repository layer (Robolectric-SQLite is unreliable on this Windows dev machine, per prior project
decision — repository tests use in-memory fakes, not a real SQLite/Room instance). Every test cites
its scenario ID from the QA catalog (constitution Article I).

**Target Platform**: Android (existing `:apps:finance:app` shell), API 26+.

**Project Type**: Mobile app feature module, inside the existing Gradle monorepo.

**Performance Goals**: Net worth total and category breakdown must come from server-side
aggregation (`v_net_worth_by_sector`), never a client-side reduction over the full valuation
history — this is a stated NFR (NFR-8) and a correctness/scale requirement as holding count grows,
not a nice-to-have.

**Constraints**: Money is `Long` paise end to end on every new code path (constitution Article VII).
No `feature → feature` import; `:feature:networth` reaches data only through
`:apps:finance:data`'s repositories (constitution Article III). Every new component extends an
existing `:libs:core` component before a parallel one is considered (constitution Article VI). No
raw dp/sp/hex literal in any new screen file (constitution Article V).

**Scale/Scope**: 7 screens (C1–C7) + 1 rewritten screen (Home, 01), 1 new Gradle module, 1 new
table + 2 views, ~14+5 QA scenario rows (NW-*, HOM-*) to satisfy.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Article | Check | Status |
|---|---|---|
| I. Test-First | Every Backend/Android test in `tasks.md` must cite an `NW-*`/`HOM-*` scenario ID | PASS (enforced at task-authoring time, §7 below) |
| II. Scenarios Before Code | QA catalog rows for NW/HOM already exist and are reviewed (catalog §3/§12, marked "done") | PASS — pre-satisfied by existing catalog |
| III. Module Boundaries | New `:feature:networth` depends only on `:libs:core` + `:apps:finance:data` (Repository-only); no feature-to-feature import | PASS by construction — see Project Structure |
| IV. Fault Isolation | C1–C7 routes wrapped in `FeatureHost`; `networth` flag already exists (`enabled: true, requiresConsent: true`) in `dhruv-finance.json` | PASS — flag pre-exists from Phase 1 scaffolding |
| V. No Hardcoding | All new screens use `DhruvNextType`/`Spacing`/`Radii`/`LocalDhruvNextColors`; screen data in `<Name>Config.kt` | PASS by construction, verified at review |
| VI. Component Reuse | B3/B6/B9 batches + `NxTextField`/`NxButton` extensions build on existing components (design system §5.2/§5.3), not parallel ones | PASS — see data-model.md / research.md R2 |
| VII. Money Is Exact | `liabilities_meta` uses `bigint`/`Long` paise for `emi_paise`; no `Double`/`Float` on the tracker path | PASS — checked by existing `checkTrackerMoneyPrecision` task |
| VIII. Consent Before Network | `networth` flag already `requiresConsent: true`; `ConsentInterceptor` (Phase 1) already gates all `:data` tracker calls, this phase adds no new client | PASS — inherited, no new gate needed |
| IX. Append-Only History | `valuations` already has no UPDATE/DELETE RLS policy (Phase 1); `liabilities_meta` sector/type enums are TEXT, append-only from creation | PASS by construction |
| X. Coverage Ratchets | New module's tests must not drop the merged JaCoCo floor | PASS — checked at `regressionCheck`, not at plan time |
| XI. Stack Is Fixed | No new dependency; Retrofit/Moshi/OkHttp + Koin only | PASS |

No violations. **Complexity Tracking is empty** — this feature follows existing, already-decided
patterns exactly (feature-module template, Repository pattern, existing networking stack).

**Post-Phase-1 re-check**: `data-model.md`'s one new table (`liabilities_meta`) is intentionally
mutable, not append-only — re-verified against Article IX, which scopes append-only to persisted
enum constants and valuation history specifically, not every table; a liability's EMI/tenure
legitimately change over its life, so an UPDATE policy on this table is correct, not a violation.
No other new violation introduced by the Phase 1 design artifacts. Gate remains PASS.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/001-net-worth-tracker/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/             # Phase 1 output — route registry rows
└── tasks.md               # Phase 2 output (/speckit-tasks — not created by this command)
```

### Source Code (repository root)

**Structure Decision**: Mobile app, existing Gradle monorepo (`dhruv`) — not a fresh project
layout. This feature adds one Gradle module and extends two existing ones, following the pattern
every other Finance feature module already uses (`feature/loans` is the reference implementation,
per the implementation plan §6).

```text
apps/finance/
├── data/src/main/java/com/dhruv/finance/data/tracker/
│   ├── model/                      # + Liability domain model (extends existing Holding/Valuation)
│   ├── mapper/                     # + LiabilityMetaMapper
│   └── repo/                       # + LiabilityRepository (HoldingRepository/ValuationRepository
│                                    #   already exist from Phase 1 — extended, not replaced)
├── feature/home/networth/          # NEW MODULE — :apps:finance:feature:networth
│   ├── build.gradle.kts            # dhruv.android.library + dhruv.android.compose, deps on
│   │                                # :data, :libs:core, :libs:settings — same as feature/loans
│   ├── NetWorthOverviewScreen.kt   # C1
│   ├── AssetsScreen.kt             # C2
│   ├── HoldingDetailScreen.kt      # C3
│   ├── AddEditHoldingScreen.kt     # C4
│   ├── AddValuationSheet.kt        # C5
│   ├── LiabilitiesScreen.kt        # C6
│   ├── LiabilityDetailScreen.kt    # C7
│   ├── NetWorthViewModel.kt / ...  # one ViewModel per screen, existing project convention
│   ├── NetWorthConfig.kt           # screen-level data/config (no-hardcoding rule)
│   └── di/NetWorthModule.kt        # Koin module, aggregated in CalculatorApplication
├── app/src/main/java/com/dhruv/finance/app/
│   └── ui/home/HomeScreen.kt       # REWRITTEN — replaces the placeholder DashboardScreen (shell-
│                                    # owned per the module-standard doc's HOM/PLN correction, not
│                                    # inside :feature:networth)
libs/core/src/main/kotlin/com/dhruv/core/ui/components/
                                     # + component batch B3 (DonutChart+RankedLegend, PieChart,
                                     #   AmortisationDonut, PaceRing), B6's NxSelect (error/helper
                                     # state), B9's SelectionSheet, NxButton sizes+loading+block —
                                     # all flat in this one directory, not a charts/inputs/overlays
                                     # split (DESIGN-SYSTEM.md §5.2: that split was never adopted)
supabase/migrations/
└── 20260823094500_networth_phase2.sql  # liabilities_meta, v_latest_valuation,
                                         # v_net_worth_by_sector, v_net_worth_history,
                                         # correct_valuation(), create_holding_with_value()
                                         # (Phase 10, T065: corrected from this file's original
                                         # `0002_networth_phase2.sql` placeholder name — see
                                         # data-model.md's "Updated 2026-08-23" note)
```

Tests mirror this tree under each module's `src/test/`, per existing project convention (no
separate `tests/` root — this is not a greenfield layout).

## Complexity Tracking

*(empty — no Constitution Check violations)*
