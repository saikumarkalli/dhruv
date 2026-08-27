# Implementation Plan: Insights (Phase 5)

**Branch**: `005-insights` | **Date**: 2026-08-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `apps/finance/specs/005-insights/spec.md`

**Note**: This plan is the spec-kit-shaped restatement of
`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §5/§6/§7
(Phase 5) — that document remains the source of truth for anything not repeated here. **Four places
where this plan corrects or adds to it** are called out in `research.md` (R1 reporting is
parameterised functions not views, R4 as-at derivation, R11 the two money universes, R12 the tax
basis column) and must be written back into that plan's Phase 5 section as a dated note when this
feature is implemented.

## Summary

Turn the Insights tab from a `NotConfiguredCard` into the app's reporting surface: a root that leads
with the savings rate rather than a chart, three statements that reconcile in front of the user
(cashflow, profit & loss, balance sheet), and a reports screen that reads on screen first and
exports second. One period model spans the whole tab; the balance sheet alone carries an overridable
date on top of it.

Technical approach: **six parameterised SQL reporting functions in the `finance` schema, not the
parameterless views the implementation plan assumed** (R1) — every statement here is period-scoped
and the clarification session made periods day-grained and as-at dates arbitrary, which a view
cannot express; one new Gradle module `:apps:finance:feature:insights` following the existing
template; one pure-Kotlin `PeriodResolver` placed outside `tracker/` on the precedent Phase 4 set for
its engines; export that serialises the **already-rendered statement model** rather than re-querying,
which makes "the file equals the screen" structural instead of a thing to test for; and **six
sub-phases**, each independently shippable and green on `regressionCheck`, because nine screens'
worth of work behind one checkpoint is how a phase stops being reviewable.

**This plan is written under a gate it does not resolve.** The investment-returns calculation has no
accepted decision record (spec Scope Boundaries, "Gating prerequisite"; functional spec open item
§8.6). Sub-phase **5f** is blocked on it. Sub-phases 5a–5e are not, and nothing in them depends on
it — that separation is the reason the phase is split this way rather than the split being cosmetic.

## Technical Context

**Language/Version**: Kotlin 2.2, Jetpack Compose (Material 3), minSdk 26.

**Primary Dependencies**: Koin (DI), Retrofit + Moshi + OkHttp against Supabase PostgREST (Phase 1's
`tracker/net` stack, extended not replaced), kotlinx.coroutines/Flow. **No new dependency.** CSV is
string building; PDF is `android.graphics.pdf.PdfDocument` (platform API since API 19); the file
destination is the platform's own `ACTION_CREATE_DOCUMENT` picker. See `research.md` R5.

**Storage**: Supabase (PostgreSQL + RLS), `finance` schema. **No new tables in 5a–5e** — this feature
reads what Phases 2 and 3 record and stores nothing of its own. It adds six `security invoker`
reporting functions and (5f only) one nullable column on `finance.categories`. The monthly-summary
preference is device-local settings state (encrypted DataStore), consistent with the 004 control
plane — not a table.

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine + in-memory fakes / MockWebServer at the HTTP
boundary (Robolectric-SQLite is unreliable on this Windows dev machine — standing project
constraint). `PeriodResolver` is a pure function and gets golden-value fixture tests with no Android
dependency. **Reconciliation is tested twice, deliberately**: once as a pure assertion over a
statement model built from fixtures (JVM, cheap, runs on every PR), and once against the dev Supabase
project at the Sec step, because a function that reconciles in Kotlin and not in SQL is exactly the
defect `SIG-BR-001..003` exist to catch. Every test cites its `SIG-*` scenario ID (Article I).

**Coverage**: JaCoCo merged report + `jacocoCoverageVerification` floor via `./gradlew
regressionCheck` (ADR-0013, Article X). `:apps:finance:feature:insights` is added to **both**
`coveredModules` (root `build.gradle.kts`) and `_FEATURES` (`scripts/ci/regression_summary.py`) in
**5a's setup**, not at the end of the phase — otherwise five sub-phases of work report as `(other)`.
The floor is ratcheted once per sub-phase checkpoint, only to the newly measured value.

**Target Platform**: Android (existing `:apps:finance:app` shell), API 26+.

**Project Type**: Mobile app feature module inside the existing Gradle monorepo.

**Performance Goals**: No Insights screen sums a ledger to draw itself (NFR-8). Every figure arrives
pre-aggregated from one reporting function call per statement. The client's only arithmetic is
formatting and the savings-rate division, both over a handful of already-summed rows. Export
serialises a model already in memory and runs off the main thread; a multi-year custom range is
bounded by the server's aggregation, not by a client-side loop.

**Constraints**: Money is `Long` paise on every stored and transported path (Article VII); percentages
and shares are computed for display only and never written anywhere. Feature module reaches data only
through `:apps:finance:data` repositories (Article III); the one cross-feature jump this feature could
want — a statement line to the transaction behind it — is deliberately **not built** (see Scope
Boundaries: statements are read surfaces). No raw dp/sp/hex literal in a screen file (Article V). No
PostgREST call before the "Sync my financial records" consent switch is on — inherited from Phase 1's
`ConsentInterceptor`, no new client is constructed (Article VIII). Reporting functions are
`security invoker`, never `security definer` — see the Article VIII note below, this is the one place
this feature could have silently created an RLS bypass.

**Scale/Scope**: 5 screens (F1–F5) + 6 report types, 1 new Gradle module, 0 new tables, 6 new SQL
functions, 1 new feature flag, 3 new `NavTarget` cases, 8 QA rows to satisfy (7 `SIG-*` plus the
deferred delivery row), 6 sub-phases.

**Dependency on Phases 2 and 3**: this phase reads what both produce and adds neither. It cannot
start before both ship.

| Needs | From | Used by |
|---|---|---|
| `holdings`, `valuations`, `v_latest_valuation`, `v_net_worth_by_sector` | Phase 2 | balance sheet (FR-018), the FR-020 agreement test |
| `liabilities_meta.liability_type` | Phase 2 | balance sheet liability grouping (FR-018) |
| `transactions` (`type`, `amount_paise`, `occurred_at`, `split_group_id`), `categories.excluded_from_spend` | Phase 3 | cashflow, P&L, category breakdown, savings rate |
| `accounts.opening_balance_paise`, `v_account_balances` | Phase 3 | cashflow opening and closing balance (FR-008) |
| `v_month_summary`, `v_category_spend` | Phase 3 | the month-grain cross-check that the new functions must agree with |
| `DateRangeSheet` (batch B2), `NxTabs` (B8), `DonutChart` + ranked legend (B3) | Phase 2/3/4 | custom range picker, statement tabs, category breakdown |
| `NxSelect` (B6), `SelectionSheet` (B9), `StatusBadge`/`InfoBanner` (B7) | Phase 2/3 | report picker, export format choice, derived-insight labelling |
| `PeriodChipRow`, `TrendSparkline`, `ProgressRing`, `MoneyText`, `StatDeltaChip` | built today | period selector, savings-rate ring, every money figure |

If this phase somehow starts before either, those component rows move into this phase rather than
being duplicated — the tables and views do not, because the features that own them do.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Article | Check | Status |
|---|---|---|
| I. Test-First | Every Backend/Android task in `tasks.md` cites a `SIG-*` scenario ID; RED before GREEN. `PeriodResolver` and the statement-reconciliation assertions are pure, so RED is genuinely cheap — fixtures exist before any screen does | PASS (enforced at task-authoring time) |
| II. Scenarios Before Code | `SIG-*` (7) written and reviewed 2026-08-09 — catalog §8. **Gap found while writing this plan**: the catalog has no row for the balance-sheet date override, no row for the settings entry, and no row for the two "More" reports — all three arrived in the 2026-08-22 clarification, after the catalog was written. QA writes them before 5c/5a/5f respectively | **CONDITIONAL** — see the Article II note below |
| III. Module Boundaries | `:feature:insights` depends only on `:libs:core` + `:apps:finance:data` + `:libs:settings`. It reads Phase 2's and Phase 3's data through a new `InsightsRepository` in `:data`, never by importing `:feature:networth` or `:feature:money` | PASS by construction — see Project Structure |
| IV. Fault Isolation | F1–F5 wrapped in `FeatureHost`; one new flag `insights` in `dhruv-finance.json`, `requiresConsent: true` (impl plan §5.5 already reserves the name). The tab root replaces today's `NotConfiguredCard`, which stays as the disabled-state fallback rather than being deleted | PASS — flag added by 5a |
| V. No Hardcoding | All new screens read `DhruvNextType`/`Spacing`/`Radii`/`LocalDhruvNextColors`; F3's dark-hero treatment reads `DhruvBrand` (impl plan §3.1). Screen-level data — the report list, the period presets, the statement section ordering — lives in `InsightsConfig.kt`, not inline | PASS by construction, verified at review |
| VI. Component Reuse | **Zero genuinely new components.** Verified by symbol search: `PeriodChipRow`, `ProgressRing`, `MoneyText`, `StatDeltaChip`, `TrendSparkline`, `CategoryBarRow`, `SectionLabel`, `ListGroup` all exist. The statement table is `ListGroup` + `ListGroupRow`, not a new table component. `DonutChart`/`NxTabs`/`DateRangeSheet` are §5.2 batches owed by earlier phases, not by this one | PASS — see `research.md` R7 |
| VII. Money Is Exact | Every transported amount is `bigint`/`Long` paise. Shares and percentages are display-only, computed at render from paise, never stored and never round-tripped. `PeriodResolver` does date arithmetic only — no money, no floats | PASS |
| VIII. Consent Before Network | The `insights` flag ships `requiresConsent: true`, already mapped to the "Sync my financial records" A3 switch in impl plan §5.5's table. No new HTTP client is constructed; `rpc/` calls go through the existing consent-gated `dataClient` | PASS — inherited gate; **see the RLS note below** |
| IX. Append-Only History | This feature writes nothing to any tracker table, so there is no history to break. The one persisted enum it introduces — the reporting-period kind — is device-local settings state, and its constants are append-only by the same rule | PASS by construction |
| X. Coverage Ratchets | Module registered in `coveredModules` + `_FEATURES` in 5a, not at the end; floor ratcheted at each sub-phase checkpoint, only to the measured value | PASS — with the wiring made explicit tasks; see the Coverage note below |
| XI. Stack Is Fixed | No new dependency. CSV is stdlib, PDF is a platform API, the file picker is a platform intent | PASS |

**One conditional, no violations. Complexity Tracking is empty.**

**Article II note (the one gate that is not already satisfied).** Phase 4's plan could write "PASS —
pre-satisfied" because its catalog rows predated it. This feature cannot: three of its requirements
were created by the 2026-08-22 clarification session, months after the catalog was written on
2026-08-09. Specifically, the catalog has **no row** covering FR-017's date-override precedence,
**no row** covering the FR-046/047 settings entry, and **no row** covering FR-034–038's two "More"
reports. Article II is not waived for them — it is scheduled: QA writes those rows into catalog §8
as the first task of the sub-phase that needs them (5c, 5a and 5f), before that sub-phase's Backend
or Android work starts. This is the constitution's own step order, applied per sub-phase instead of
once per phase. Recorded here rather than discovered mid-implementation.

**Article VIII note (why `security invoker` is load-bearing, not a style choice).** Every reporting
function reads RLS-protected tables. A `security definer` function runs as its owner and **silently
bypasses RLS** — every user would read every user's statements, and no test that only checks "the
numbers reconcile" would ever notice. The two existing security-definer functions in this repo
(`delete_my_data`, `delete_my_account`) are definer for a specific, argued reason (ADR-0029 §5:
deleting the caller's own `auth.users` row needs privilege the caller lacks). Reporting needs no
privilege the caller lacks, so it gets none. Every function this feature adds is
`security invoker`, and `research.md` R3 records this so nobody later "fixes" a permission error by
flipping it.

**Coverage note.** `coveredModules` and `_FEATURES` are hand-maintained, so the gate only covers
modules someone remembered to add. Re-verified while writing this plan and the gap Phase 3's and
Phase 4's plans both reported is **still open**: `:apps:finance:feature:onboarding` is in
`coveredModules` but absent from `_FEATURES`, so its coverage still reports as `(other)`. Phase 2's
`networth`, Phase 3's `money` and Phase 4's three modules are in neither list (all four phases are
spec'd, not implemented). This phase registers its own module in both and does not silently patch
the others — that belongs to whichever phase lands them. Three consecutive plans have now reported
the same one-line `_FEATURES` omission without anyone fixing it, which is itself the finding.

**Post-Phase-1 re-check.** Five design decisions were re-tested against the gates after
`data-model.md` was written:

1. **Reporting is parameterised functions, not views** (R1). Checked against Article III: a function
   called over PostgREST `rpc/` is still Repository-only access — `InsightsRepository` is the only
   caller and features never see it. Checked against NFR-8: aggregation stays server-side, which is
   the point of the rule; a view summed client-side across months would have moved it to the client.
   Not a violation; it is the stricter option.
2. **Nothing is stored** — no statement, no snapshot, no computed total. Checked against Article IX
   and the spec's append-only assumption: a stored statement is a derived figure that goes stale the
   moment a correction is appended, and later gets read as a recorded fact. This is the same reasoning
   Phase 4 used to refuse storing a projected corpus.
3. **Export serialises the loaded model, never re-queries** (R5). Checked against FR-029: making the
   file and the screen the same object removes the entire class of "export shows a different period"
   defects, rather than testing for them afterwards.
4. **The date override is local state on one ViewModel, not part of the shared period** (R6). Checked
   against FR-039, which names it the sole exception to period carry. Putting it in the shared store
   would have made the exception structural and leaked it to four screens that must not see it.
5. **The tax-relevance basis is a user-set column, not a hardcoded category list** (R12). Checked
   against Article V's spirit and FR-037's "state the basis" requirement: a name-matched list is both
   unstated and wrong the moment a user renames a category. Deferred into 5f with the returns report
   so 5a–5e alter no Phase 3 table.

Gate remains PASS, with the Article II condition scheduled rather than waived.

## Sub-phases

Nine screens' worth of surface behind one checkpoint is not reviewable, and one of the six report
types is blocked on a decision that does not exist yet. The phase is therefore split into six
sub-phases. **Each one ends green on `./gradlew regressionCheck`, ratchets the coverage floor once,
and is independently shippable** — the tab is usable and honest after every one of them, never
half-built. Each maps to the spec's own user-story priorities, so the split is the spec's, not an
invention of this plan.

| Sub-phase | Ships | Spec stories | QA rows | Blocked by |
|---|---|---|---|---|
| **5a** Foundation + monthly summary (F1) | The module, the flag, the route, the period model, the Insights tab root replacing `NotConfiguredCard`, the settings entry with the monthly-summary preference | US1, US7 | `SIG-UI-001`, `SIG-UI-002`, + new settings rows | Phases 2 and 3 |
| **5b** Cashflow statement (F2) | `report_cashflow`, the reconciling statement, the transfers footnote | US2 | `SIG-BR-001` | 5a |
| **5c** Balance sheet (F4) | `report_balance_sheet`, the date override and its precedence rules, the self-valued footnote | US3 | `SIG-BR-003`, + new override rows | 5a |
| **5d** Profit & loss (F3) | `report_pnl`, prior-period resolution, the unavailable-comparison state | US4 | `SIG-BR-002` | 5a |
| **5e** Reports & export (F5 core) | The full period picker, the report list, category breakdown, CSV + PDF export, the settings export control | US5, US6 | `SIG-FLOW-001`, `SIG-FLOW-002`, `SIG-FLOW-003` | 5b, 5c, 5d |
| **5f** Investment returns + tax summary (F5 "More") | `report_investment_returns`, `report_tax_summary`, `categories.tax_section` | US5 #9, #10 | new rows | **the returns decision record** + 5e |

**Why this order.** 5a must be first — it owns the module, the flag and the period model everything
else consumes. 5b/5c/5d are genuinely independent of each other and could be worked in parallel or
reordered; they are listed in the spec's priority order. 5e needs all three statements to exist
because its whole job is presenting and exporting them. 5f is last because it is the only one gated,
and putting it anywhere earlier would stall the five that are not.

**What each sub-phase's checkpoint requires** (constitution step 7, applied per sub-phase):
`regressionCheck` green · coverage floor not regressed · every QA row for that sub-phase CLOSED or
explicitly deferred with a stated reason · the Sec step run for any sub-phase touching a new SQL
function (5a, 5b, 5c, 5d, 5f — 5e adds no SQL and its Sec step is the export path only).

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/005-insights/
├── plan.md                          # This file
├── research.md                      # Phase 0 output
├── data-model.md                    # Phase 1 output
├── quickstart.md                    # Phase 1 output
├── contracts/routes.md              # Phase 1 output — route rows, NavTarget additions, flag
├── contracts/reporting-functions.md # Phase 1 output — the six rpc contracts
├── checklists/requirements.md
└── tasks.md                         # Phase 2 output (/speckit-tasks — not created by this command)
```

### Source Code (repository root)

**Structure Decision**: Mobile app inside the existing Gradle monorepo — one new feature module under
the Insights tab-owner bucket, no shell rewrite (the tab already exists; only its content changes),
no new table. `feature/loans` remains the reference implementation for module shape (impl plan §6);
`feature/insights/insights/` is the physical location under the bucket scheme
(`apps/finance/feature/README.md`), with the Gradle coordinate remapped via `projectDir` exactly like
every existing module.

**Insights is one module, not five.** The five screens share one period model, one repository and one
statement-rendering vocabulary; splitting them would force either a forbidden `feature → feature`
edge or five copies of the period store. This is the same reasoning that kept Phase 4's budgets,
goals and debt payoff in one `planning` module.

```text
apps/finance/
├── data/src/main/java/com/dhruv/finance/data/
│   ├── reporting/                       # NEW — pure, deliberately OUTSIDE tracker/ (R2, Phase 4's precedent)
│   │   ├── PeriodResolver.kt            #   month/quarter/FY(Apr–Mar)/custom boundaries, prior-year shift
│   │   ├── ReportingPeriod.kt           #   the period value type + its kind enum
│   │   └── StatementReconciler.kt       #   pure assertions: the arithmetic SIG-BR-001..003 name
│   └── tracker/
│       ├── dto/                         # + CashflowReportDto, PnlReportDto, BalanceSheetReportDto,
│       │                                #   PeriodSummaryDto, CategoryBreakdownDto
│       │                                #   (+ 5f: InvestmentReturnsDto, TaxSummaryDto)
│       ├── model/                       # + Statement, StatementSection, StatementLine,
│       │                                #   PeriodSummary, CategorySummary, PositionSnapshot
│       ├── mapper/                      # + one mapper per model
│       └── repo/                        # + InsightsRepository (the only caller of rpc/report_*)
├── feature/insights/insights/           # NEW MODULE — :apps:finance:feature:insights  (F1–F5)
│   ├── build.gradle.kts                 # dhruv.android.library + dhruv.android.compose
│   ├── MonthlySummaryScreen.kt          # F1 — the tab root
│   ├── CashflowScreen.kt                # F2
│   ├── ProfitLossScreen.kt              # F3 (dark hero — DhruvBrand)
│   ├── BalanceSheetScreen.kt            # F4
│   ├── ReportsScreen.kt                 # F5
│   ├── ExportFormatSheet.kt             # F5 format choice (sheet, N3)
│   ├── <Screen>ViewModel.kt             # one per screen, existing convention
│   ├── InsightsPeriodStore.kt           # the shared period; the date override is NOT here (R6)
│   ├── export/                          # CsvStatementWriter, PdfStatementWriter, one shared model in
│   ├── InsightsConfig.kt                # report list, period presets, section ordering
│   └── di/InsightsModule.kt             # Koin module, aggregated in CalculatorApplication
├── app/src/main/java/com/dhruv/finance/app/
│   └── ui/…                             # ~ Insights tab root swapped from NotConfiguredCard to F1;
│                                        #   NotConfiguredCard retained as the flag-off fallback
libs/core/src/main/kotlin/com/dhruv/core/
└── navigation/NavTarget.kt              # + OpenStatement(StatementKind), OpenReports(period),
                                         #   OpenBalanceSheet(asOf); + enum StatementKind
platform/feature-flags/dhruv-finance.json   # + insights  { enabled, minVersion, requiresConsent: true }
build.gradle.kts                         # + :apps:finance:feature:insights in `coveredModules`;
                                         #   `globalLineFloor` ratcheted at each sub-phase checkpoint
scripts/ci/regression_summary.py         # + "insights" in `_FEATURES`
supabase/
├── schemas/finance/30_functions/        # + report_period_summary, report_cashflow, report_pnl,
│                                        #   report_balance_sheet, report_category_breakdown
│                                        #   (+ 5f: report_investment_returns, report_tax_summary)
├── schemas/finance/10_tables/categories.sql  # ~ 5f ONLY: + tax_section text null
└── migrations/<timestamp>_insights_phase5.sql  # via `supabase db diff`, grants hand-added (ADR-0032)
web/src/shared/types/database.ts         # regenerated: supabase gen types --schema public,finance
```

Tests mirror this tree under each module's `src/test/` — existing project convention, no separate
`tests/` root. `PeriodResolver` and `StatementReconciler` tests live in `:apps:finance:data`'s
`src/test/` as plain JVM tests with no Android or coroutine machinery.

## Complexity Tracking

*(empty — no Constitution Check violations. The single conditional, Article II, is scheduled per
sub-phase rather than justified as an exception; see the Article II note above.)*