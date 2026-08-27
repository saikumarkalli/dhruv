---

description: "Task list for Insights (Phase 5)"
---

# Tasks: Insights (Phase 5)

**Input**: Design documents from `apps/finance/specs/005-insights/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: **Required, not optional.** Constitution Article I is non-negotiable — RED → GREEN →
REFACTOR, and every test cites the `SIG-*` scenario ID it satisfies. Test tasks below are therefore
first-class, not a suggested extra.

**Organization**: Tasks are grouped by user story. Each group also carries its **sub-phase** tag
(5a–5f) from `plan.md`, because the sub-phase — not the story — is the unit that ships, ends green on
`regressionCheck`, and ratchets the coverage floor.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names an exact file path

## Path Conventions

Mobile app in the existing Gradle monorepo. Sources are `src/main/java`, tests `src/test/java` — no
separate `tests/` root (existing project convention).

- Feature module: `apps/finance/feature/insights/insights/`
- Shared data: `apps/finance/data/src/main/java/com/dhruv/finance/data/`
- Schema: `supabase/schemas/finance/30_functions/`, `supabase/migrations/`
- QA catalog: `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`

## Prerequisite gates (read before starting)

1. **Phases 2 and 3 must be shipped.** This feature reads their tables and adds none. Nothing below
   runs against an empty schema.
2. **Sub-phase 5f is blocked** on an accepted decision record fixing the investment-returns
   calculation (research R8). Do not start Phase 10 without it.
3. **Article II condition**: three requirements arrived after the QA catalog was written. Their
   catalog rows are written as the **first task** of the sub-phase that needs them — T011 (settings),
   T079 (date override), T148 (the two "More" reports) — before any Backend or Android work in that
   sub-phase.

---

## Phase 1: Setup — sub-phase 5a (Shared Infrastructure)

**Purpose**: The module exists, is on the build, is measured, and is reachable.

- [ ] T001 Create the feature module directory and manifest at `apps/finance/feature/insights/insights/src/main/AndroidManifest.xml`
- [ ] T002 Create `apps/finance/feature/insights/insights/build.gradle.kts` applying `dhruv.android.library` + `dhruv.android.compose`, with deps on `:apps:finance:data`, `:libs:core`, `:libs:settings` (copy `apps/finance/feature/plan/loans/build.gradle.kts` as the reference template)
- [ ] T003 Register `:apps:finance:feature:insights` in `settings.gradle.kts` with `projectDir` remapped to `apps/finance/feature/insights/insights` per the bucket scheme
- [ ] T004 [P] Add `:apps:finance:feature:insights` to `coveredModules` in the root `build.gradle.kts`
- [ ] T005 [P] Add `"insights"` to `_FEATURES` in `scripts/ci/regression_summary.py`
- [ ] T006 [P] Add the `insights` flag entry (`enabled: true`, `minVersion 1.0.0`, `requiresConsent: true`) to `platform/feature-flags/dhruv-finance.json`
- [ ] T007 Create `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/di/InsightsModule.kt` with an empty Koin `module {}` and aggregate it in `CalculatorApplication`
- [ ] T008 [P] Create `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/InsightsConfig.kt` as the home for report list, period presets and section ordering (no-hardcoding rule, Article V)
- [ ] T009 [P] Create the module README at `apps/finance/feature/insights/insights/README.md` and link it from `apps/finance/FEATURES.md`
- [ ] T010 Verify `./gradlew :apps:finance:feature:insights:assembleDebug` and `./gradlew regressionCheck` are both green with the empty module in place

---

## Phase 2: Foundational — sub-phase 5a (Blocking Prerequisites)

**Purpose**: The period model, the repository seam, the routes and the SQL the first screen needs.

**⚠️ CRITICAL**: No user story work begins until this phase is complete.

### QA — scenarios before code (Article II)

- [ ] T011 Write the missing `SIG-*` catalog rows for the Insights **settings entry** (FR-046, FR-047, FR-048, FR-049) into §8 of `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`, each citing its spec anchor

### SA — schema and routes

- [ ] T012 [P] Author `supabase/schemas/finance/30_functions/report_period_summary.sql` — `security invoker`, `stable`, args `(p_from date, p_to date)`, per `contracts/reporting-functions.md`
- [ ] T013 [P] Author `supabase/schemas/finance/30_functions/report_category_breakdown.sql` — `security invoker`, `stable`, args `(p_from, p_to, p_prev_from, p_prev_to date)`
- [ ] T014 Generate the migration with `supabase db diff -f insights_phase5_5a` and hand-add the `grant execute … to authenticated` statements `db diff` cannot emit (ADR-0032 caveat list)
- [ ] T015 Regenerate `web/src/shared/types/database.ts` via `supabase gen types --schema public,finance` and run `python scripts/db/gen_schema_docs.py --check`
- [ ] T016 [P] Add `OpenStatement(StatementKind)`, `OpenReports(ReportingPeriodRef?)`, `OpenBalanceSheet(LocalDate?)` and `enum StatementKind` to `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt` per `contracts/routes.md`
- [ ] T017 Add the eight per-screen route rows to §1 of `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` (the registry is the authoritative flat index; `contracts/routes.md` is not a second source)

### Backend — period model (RED → GREEN → REFACTOR)

- [ ] T018 [P] RED: write `apps/finance/data/src/test/java/com/dhruv/finance/data/reporting/PeriodResolverTest.kt` — golden-value fixtures for month, quarter (FY-aligned), financial year (Apr–Mar), and custom-range boundaries; assert `start <= end`, no future `end` on custom, and rejection of `end < start` (FR-023, FR-024, FR-025)
- [ ] T019 [P] RED: write `apps/finance/data/src/test/java/com/dhruv/finance/data/reporting/PeriodResolverPriorTest.kt` — `priorYear` shifts exactly one year and clamps 29 Feb to 28 Feb; `previousComparable` shifts by the period's own length; the two are never equal for a month (FR-013, FR-005)
- [ ] T020 GREEN: implement `apps/finance/data/src/main/java/com/dhruv/finance/data/reporting/ReportingPeriod.kt` (value type + `kind` enum, append-only constants)
- [ ] T021 GREEN: implement `apps/finance/data/src/main/java/com/dhruv/finance/data/reporting/PeriodResolver.kt` — placed outside `tracker/` deliberately (research R2); do not move it under `tracker/`
- [ ] T022 REFACTOR: extract the label formatting so no screen formats a period itself, and confirm `PeriodResolver` has zero Android imports

### Backend — reconciliation primitives and repository seam

- [ ] T023 [P] RED: write `apps/finance/data/src/test/java/com/dhruv/finance/data/reporting/StatementReconcilerTest.kt` — a section's subtotal equals the sum of its lines, and a violating fixture fails (FR-012)
- [ ] T024 GREEN: implement `apps/finance/data/src/main/java/com/dhruv/finance/data/reporting/StatementReconciler.kt` as pure assertions over the domain model
- [ ] T025 [P] Create the domain models `Statement`, `StatementSection`, `StatementLine`, `PeriodSummary`, `CategorySummary`, `PositionSnapshot` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/model/` per `data-model.md` — `savingsRateBps` and `shareBps` are nullable `Int`, never `0` as a stand-in
- [ ] T026 [P] Create `PeriodSummaryDto` and `CategoryBreakdownDto` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/dto/`
- [ ] T027 [P] Create their mappers in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/mapper/`
- [ ] T028 Create `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/InsightsRepository.kt` — the **only** caller of `rpc/report_*`; it uses the existing consent-gated `dataClient` and constructs no client of its own (Article VIII)

### Shell — tab root and states

- [ ] T029 Swap the Insights tab root in `apps/finance/app/src/main/java/com/dhruv/finance/app/` from `NotConfiguredCard` to the `FeatureHost`-wrapped F1 route, **retaining** `NotConfiguredCard` as the not-configured state (`contracts/routes.md`)
- [ ] T030 Wire the NFR-4 state trio plus `FeatureDisabledCard` for the Insights route and confirm each renders — no `OfflineBanner` variant, since nothing is cached (`contracts/routes.md`)

**Checkpoint**: Foundation ready. `regressionCheck` green with `PeriodResolver` and `StatementReconciler` covered.

---

## Phase 3: User Story 1 — Answer "did I actually save this month" (Priority: P1) 🎯 MVP — sub-phase 5a

**Goal**: The Insights tab root shows the savings rate first, the three headline figures, where the money went, and a comparative insight — all for a selectable period.

**Independent Test**: With one month of transactions, confirm the savings rate recomputes by hand as surplus ÷ income, that income − expense = surplus, and that each category's change against last month recomputes from the two months' totals.

### Tests for User Story 1 ⚠️ (write first, confirm they FAIL)

- [ ] T031 [P] [US1] RED: `PeriodSummaryMappingTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/mapper/` — DTO → model preserves exact paise and maps zero income to a **null** savings rate, not `0` (`SIG-UI-001`, FR-003)
- [ ] T032 [P] [US1] RED: `InsightsRepositoryPeriodSummaryTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/` using MockWebServer — asserts the `rpc/report_period_summary` request carries `Content-Profile: finance` and the resolver-produced dates (`SIG-UI-001`)
- [ ] T033 [P] [US1] RED: `InsightsRepositoryConsentTest` — with consent off, no request is dispatched at all (NFR-001, Article VIII)
- [ ] T034 [P] [US1] RED: `MonthlySummaryViewModelTest` in `apps/finance/feature/insights/insights/src/test/java/com/dhruv/finance/insights/` using Turbine — savings rate equals surplus ÷ income for a known fixture (`SIG-UI-001`)
- [ ] T035 [P] [US1] RED: `MonthlySummaryZeroIncomeTest` — zero income yields the "cannot be computed" state, never `0%` (FR-003, spec Edge Cases)
- [ ] T036 [P] [US1] RED: `MonthlySummaryTransferExclusionTest` — a transfer between own accounts changes none of income, expense, surplus, savings rate or any category figure (FR-005 note, BR-D1)
- [ ] T037 [P] [US1] RED: `CategoryMovementTest` — per-category vs-previous-period movement matches a recomputation; a near-zero movement resolves to the "flat" state (`SIG-UI-002`, FR-005)
- [ ] T038 [P] [US1] RED: `ComparativeInsightTest` — with fewer than twelve months the insight names the number of months averaged and is flagged derived (FR-006, FR-044)
- [ ] T039 [P] [US1] RED: `MonthlySummaryStatesTest` (Robolectric-Compose) — signed-out, offline, not-configured, empty and disabled each render their designed component (NFR-004, FR-041, FR-045)

### Implementation for User Story 1

- [ ] T040 [US1] GREEN: implement `report_period_summary` fetch + mapping in `InsightsRepository` (depends on T031–T033)
- [ ] T041 [US1] GREEN: implement `report_category_breakdown` fetch + mapping in `InsightsRepository`
- [ ] T042 [US1] GREEN: create `InsightsPeriodStore.kt` in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/` — holds only the `ReportingPeriod`; the balance-sheet date override is explicitly **not** here (research R6)
- [ ] T043 [US1] GREEN: implement `MonthlySummaryViewModel.kt` + its `UiState`, with `crashReporter.setModule("insights")` and a `featureError` flow per the module convention
- [ ] T044 [US1] GREEN: implement `MonthlySummaryScreen.kt` — savings-rate ring first (`ProgressRing`), then the three figures (`MoneyText`), then statement shortcuts, then "where it went" (`CategoryBarRow`), then the comparative insight (`SmartInsightCard`)
- [ ] T045 [US1] GREEN: wire the period selector using the existing `PeriodChipRow`, reading presets from `InsightsConfig.kt` (Article V, Article VI)
- [ ] T046 [US1] Add a `performanceTracer.trace("insights_summary_load")` around the primary load, per the module convention
- [ ] T047 [US1] Add `contentDescription` to the savings-rate ring, every delta chip and every icon-only action ("Savings rate, 34 percent") — NFR-006
- [ ] T048 [US1] Move every user-visible string to `apps/finance/feature/insights/insights/src/main/res/values/strings.xml` (design system §10 — no hardcoded literals from birth)
- [ ] T049 [US1] REFACTOR: confirm zero raw dp/sp/hex literals and zero `MaterialTheme.colorScheme`/`.typography` references in the screen file (Article V)

### QA and Sec for sub-phase 5a

- [ ] T050 [US1] Sec: assert `report_period_summary(month)` equals `v_month_summary` and `report_category_breakdown(month)` equals `v_category_spend` for the same month, against `dhruv-dev` (`contracts/reporting-functions.md` equivalence table)
- [ ] T051 [US1] Sec: sign in as a **second** test account and assert neither function returns any row belonging to the first — the real check that `security invoker` is in force (research R3)
- [ ] T052 [US1] QA: close `SIG-UI-001` and `SIG-UI-002` in the catalog and update its coverage-summary table

**Checkpoint**: User Story 1 is fully functional and testable independently.

---

## Phase 4: User Story 7 — Control Insights from Settings (Priority: P7) — sub-phase 5a

**Goal**: One Insights entry in Settings, present only when the module is, holding the monthly-summary preference.

**Independent Test**: With the module enabled, confirm an Insights entry appears holding its controls and nothing else; disable the module and confirm the entry is absent, not greyed out.

**Note on ordering**: P7 by priority, but it ships in **5a** because the settings control plane requires a module to declare its entry *with* the module (004 FR-005). Shipping it later would mean a later phase editing an existing module's entry.

### Tests for User Story 7 ⚠️

- [ ] T053 [P] [US7] RED: `InsightsSettingsEntryTest` in `apps/finance/feature/insights/insights/src/test/java/com/dhruv/finance/insights/` — the entry is contributed when the module is present and enabled (FR-046, catalog rows from T011)
- [ ] T054 [P] [US7] RED: `InsightsSettingsAbsentTest` — with the flag off, or the version gate unmet, the entry is **absent**, not disabled (FR-046)
- [ ] T055 [P] [US7] RED: `MonthlySummaryPreferenceTest` — the preference persists and reads back as set across a process restart (FR-048)
- [ ] T056 [P] [US7] RED: `MonthlySummaryPreferenceUniquenessTest` — the control appears exactly once across the whole settings tree (FR-049)

### Implementation for User Story 7

- [ ] T057 [US7] GREEN: add the monthly-summary preference key to `libs/settings/src/main/java/com/dhruv/settings/SettingsKeys.kt` and its accessor to `SettingsRepository`/`SettingsRepositoryImpl`
- [ ] T058 [US7] GREEN: declare the Insights settings entry via the contribution mechanism from `apps/finance/specs/004-settings/contracts/settings-contribution.md` — never by editing a central list
- [ ] T059 [US7] GREEN: render the preference row inside the Insights entry in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/settings/InsightsSettingsEntry.kt`; the statement-export control is **not** added here (it arrives in 5e with the export that backs it, per 004 FR-018's "not until it can do its job")
- [ ] T060 [US7] Record in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/settings/InsightsSettingsEntry.kt` that delivery of the monthly-summary alert ships with the notifications phase, so the preference is honest about what it currently does (FR-048, FR-050)
- [ ] T061 [US7] QA: close the settings catalog rows written in T011; mark FR-050's master-switch and privacy-masking rows **deferred with the stated reason**, not closed (spec FR-050)
- [ ] T062 [US7] Ratchet `globalLineFloor` in the root `build.gradle.kts` once, to just under the newly measured merged coverage — **sub-phase 5a checkpoint**

**Checkpoint**: Sub-phase 5a ships. The Insights tab is a working monthly summary with its settings entry.

---

## Phase 5: User Story 2 — Cashflow statement that reconciles (Priority: P2) — sub-phase 5b

**Goal**: A cashflow statement the user can check by eye, top to bottom, with transfers kept out of spend.

**Independent Test**: With a period of fixture transactions including a transfer, confirm opening + money in − money out − net moved-not-spent = closing, and that removing the transfer changes only the moved-not-spent section.

### SA and QA

- [ ] T063 [US2] Author `supabase/schemas/finance/30_functions/report_cashflow.sql` — `security invoker`, `stable`, six sections, with **opening and closing each computed independently from the ledger**, never closing = opening + net (`contracts/reporting-functions.md`)
- [ ] T064 [US2] Generate the migration with `supabase db diff -f insights_phase5_5b`, hand-add the `grant execute`, regenerate `database.ts`, run `gen_schema_docs.py --check`

### Tests for User Story 2 ⚠️

- [ ] T065 [P] [US2] RED: `CashflowReconciliationTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/reporting/` — the identity holds exactly on **three independent period fixtures** (`SIG-BR-001`, SC-001)
- [ ] T066 [P] [US2] RED: `CashflowTransferPlacementTest` — transfers and excluded-category rows land in moved-not-spent and appear nowhere in money out (FR-010)
- [ ] T067 [P] [US2] RED: `CashflowSectionSubtotalTest` — each section's lines sum to its stated subtotal, via `StatementReconciler` (FR-012)
- [ ] T068 [P] [US2] RED: `CashflowSplitTransactionTest` — a split transaction's parts are each counted once and never double-counted (Phase 3 split model)
- [ ] T069 [P] [US2] RED: `CashflowEmptyPeriodTest` — a period with no transactions yields the empty state, not a statement of zeroes (FR-045)
- [ ] T070 [P] [US2] RED: `CashflowScreenStatesTest` (Robolectric-Compose) — the full state matrix for F2 (NFR-004)

### Implementation for User Story 2

- [ ] T071 [P] [US2] GREEN: `CashflowReportDto` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/dto/` and its mapper
- [ ] T072 [US2] GREEN: add the `report_cashflow` call to `InsightsRepository`
- [ ] T073 [US2] GREEN: implement `CashflowViewModel.kt` in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/`
- [ ] T074 [US2] GREEN: implement `CashflowScreen.kt` using `ListGroup` + `ListGroupRow` with subtotal rows — no new table component (Article VI, research R7)
- [ ] T075 [US2] GREEN: render the transfers footnote from `strings.xml` (FR-011)
- [ ] T076 [US2] REFACTOR: confirm the client performs **no** summing — every displayed total comes from the function (NFR-8, `contracts/reporting-functions.md` caller rules)
- [ ] T077 [US2] Sec: RLS check with the second account against `report_cashflow`
- [ ] T078 [US2] QA: close `SIG-BR-001`; ratchet `globalLineFloor` — **sub-phase 5b checkpoint**

**Checkpoint**: User Stories 1 and 2 both work independently.

---

## Phase 6: User Story 3 — Position as at a date, agreeing with net worth (Priority: P3) — sub-phase 5c

**Goal**: A balance sheet at any date whose net worth matches the net-worth screen, with a date override that never leaks to the rest of the tab.

**Independent Test**: With holdings, valuations and liabilities recorded, confirm assets − liabilities = displayed net worth, and that it equals the net-worth screen for the same date.

### QA — scenarios before code (Article II)

- [ ] T079 [US3] Write the missing catalog rows for the **date override** (FR-017, FR-017a, FR-019, FR-029 balance-sheet clause) into §8 of `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`, citing their spec anchors

### SA

- [ ] T080 [US3] Author `supabase/schemas/finance/30_functions/report_balance_sheet.sql` — `security invoker`, `stable`, latest non-deleted valuation with `as_of <= p_as_of` per holding, prior-month column, `has_self_valued` flag (research R4)
- [ ] T081 [US3] Generate the migration with `supabase db diff -f insights_phase5_5c`, hand-add the `grant execute`, regenerate `database.ts`, run `gen_schema_docs.py --check`

### Tests for User Story 3 ⚠️

- [ ] T082 [P] [US3] RED: `BalanceSheetIdentityTest` — assets total − liabilities total = displayed net worth, on **three independent date fixtures** (`SIG-BR-003`, SC-002)
- [ ] T083 [P] [US3] RED: `BalanceSheetAsOfDerivationTest` — the value used is the latest valuation on or before the date, and a later valuation does not leak into an earlier date (research R4)
- [ ] T084 [P] [US3] RED: `BalanceSheetBeforeFirstRecordTest` — a date before the first record yields "no position existed", not zeroes (FR-022)
- [ ] T085 [P] [US3] RED: `BalanceSheetSelfValuedFootnoteTest` — a user-supplied valuation source raises the footnote and names the holding (FR-021)
- [ ] T086 [P] [US3] RED: `BalanceSheetDefaultDateTest` — opening with a period selected renders at that period's **end date** with no user choice (FR-017)
- [ ] T087 [P] [US3] RED: `BalanceSheetOverrideTest` — overriding re-anchors every figure and the change column to the new date, and the screen states both the inherited period and the shown date (FR-017, FR-017a, FR-019)
- [ ] T088 [P] [US3] RED: `BalanceSheetOverridePersistenceTest` — the override survives leaving and returning while the period is unchanged (FR-017)
- [ ] T089 [P] [US3] RED: `BalanceSheetOverrideResetTest` — changing the period discards the override and reverts to the new period's end (FR-017)
- [ ] T090 [P] [US3] RED: `BalanceSheetOverrideIsolationTest` — the override never propagates to the shared period store and no other screen observes it (FR-039)
- [ ] T091 [P] [US3] RED: `BalanceSheetFutureDateTest` — a future override date is rejected or corrected with a stated reason (spec Edge Cases)

### Implementation for User Story 3

- [ ] T092 [P] [US3] GREEN: `BalanceSheetReportDto` + mapper + `PositionSnapshot` assembly, with the `netWorthPaise` invariant asserted at construction
- [ ] T093 [US3] GREEN: add the `report_balance_sheet` call to `InsightsRepository`
- [ ] T094 [US3] GREEN: implement `BalanceSheetViewModel.kt` — holds `dateOverride: LocalDate?` **privately**, collects the period flow, and clears the override on period change (research R6)
- [ ] T095 [US3] GREEN: implement `BalanceSheetScreen.kt` and the date sheet `BalanceSheetDateSheet.kt` (sheet, N3)
- [ ] T096 [US3] GREEN: render the divergence statement in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/BalanceSheetScreen.kt` whenever the shown date differs from the period end (FR-017a)
- [ ] T097 [US3] Sec: assert `report_balance_sheet(today)` net worth equals `v_net_worth_by_sector` net worth (FR-020, `contracts/reporting-functions.md`); plus the second-account RLS check
- [ ] T098 [US3] QA: close `SIG-BR-003` and the override rows from T079; ratchet `globalLineFloor` — **sub-phase 5c checkpoint**

**Checkpoint**: User Stories 1, 2 and 3 all work independently.

---

## Phase 7: User Story 4 — Compare a period against the same period last year (Priority: P4) — sub-phase 5d

**Goal**: A profit & loss statement against the same period one year earlier, that works for a quarter and a financial year as well as a month.

**Independent Test**: With two years of fixtures, open P&L on a month and then a quarter; confirm each line's share of income and year-on-year movement recompute by hand and that the comparison is one year prior, not the preceding period.

### SA

- [ ] T099 [US4] Author `supabase/schemas/finance/30_functions/report_pnl.sql` — `security invoker`, `stable`, args `(p_from, p_to, p_prior_from, p_prior_to)`, matching lines by `category_id` not by name
- [ ] T100 [US4] Generate the migration with `supabase db diff -f insights_phase5_5d`, hand-add the `grant execute`, regenerate `database.ts`, run `gen_schema_docs.py --check`

### Tests for User Story 4 ⚠️

- [ ] T101 [P] [US4] RED: `PnlPriorPeriodTest` — the comparison range is the same period one year earlier for a month, a quarter, a financial year and a shifted custom range; never the immediately preceding period (`SIG-BR-002`, FR-013)
- [ ] T102 [P] [US4] RED: `PnlSubtotalTest` — group lines sum to their subtotals and income subtotal − expense subtotal = net surplus (`SIG-BR-002`, FR-014)
- [ ] T103 [P] [US4] RED: `PnlLineFiguresTest` — each line carries name, amount, share of that period's income, and year-on-year movement, all recomputable (FR-015)
- [ ] T104 [P] [US4] RED: `PnlNoPriorDataTest` — no prior-year period yields "comparison unavailable", never zero and never a movement figure (FR-016)
- [ ] T105 [P] [US4] RED: `PnlCategoryRenameTest` — a category renamed between the two periods stays one line and keeps its prior amount (FR-014, BR-D3)
- [ ] T106 [P] [US4] RED: `PnlNonMonthPeriodTest` — a quarter, financial year or custom range renders without degrading to a month and without disabling itself (US4 scenario 7)
- [ ] T107 [P] [US4] RED: `PnlLeapDayRangeTest` — a custom range shifted back across a leap day states the compared range on screen (spec Edge Cases)

### Implementation for User Story 4

- [ ] T108 [P] [US4] GREEN: `PnlReportDto` + mapper
- [ ] T109 [US4] GREEN: add the `report_pnl` call to `InsightsRepository`, passing the prior range from `PeriodResolver.priorYear` — never assembled at the call site (`contracts/reporting-functions.md` caller rules)
- [ ] T110 [US4] GREEN: implement `ProfitLossViewModel.kt`
- [ ] T111 [US4] GREEN: implement `ProfitLossScreen.kt` as a dark-hero surface reading `DhruvBrand` (implementation plan §3.1)
- [ ] T112 [US4] GREEN: render the resolved comparison range in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/ProfitLossScreen.kt` (FR-016 and the leap-day case)
- [ ] T113 [US4] Sec: RLS check with the second account against `report_pnl`
- [ ] T114 [US4] QA: close `SIG-BR-002`, verified on both a month and a non-month period; ratchet `globalLineFloor` — **sub-phase 5d checkpoint**

**Checkpoint**: All three statements work independently.

---

## Phase 8: User Story 5 — Choose a period, read the report, then export it (Priority: P5) — sub-phase 5e

**Goal**: The reports screen with the full period picker, the report list, and export that cannot disagree with the screen.

**Independent Test**: Select each period type in turn, confirm every report changes consistently and the financial year runs April–March; export a report and confirm the file's totals equal what was on screen.

### Tests for User Story 5 ⚠️

- [ ] T115 [P] [US5] RED: `PeriodPickerTest` — month, quarter, financial year and custom are all selectable and the selection is stated on screen (`SIG-FLOW-001`, FR-023)
- [ ] T116 [P] [US5] RED: `FinancialYearBoundaryTest` — the FY period resolves to 1 April – 31 March in every report (`SIG-FLOW-001`, FR-024)
- [ ] T117 [P] [US5] RED: `CustomRangeValidationTest` — end before start is rejected or corrected with a stated reason; a future end is refused (FR-025)
- [ ] T118 [P] [US5] RED: `ReportListPeriodConsistencyTest` — every report in the list presents the same selected period (`SIG-FLOW-001`, FR-026)
- [ ] T119 [P] [US5] RED: `ExportGatingTest` — export is not offered until the report has rendered for the selected period (FR-027)
- [ ] T120 [P] [US5] RED: `CsvExportFidelityTest` — the CSV's totals and line items equal the rendered `Statement` (`SIG-FLOW-002`, FR-029)
- [ ] T121 [P] [US5] RED: `PdfExportFidelityTest` — same assertion for the PDF (`SIG-FLOW-002`, FR-029)
- [ ] T122 [P] [US5] RED: `ExportBalanceSheetOverrideTest` — exporting the balance sheet with an override covers the **shown date**, not the period end (FR-029)
- [ ] T123 [P] [US5] RED: `ExportHeaderTest` — every export states its period, generation date and app version (FR-030)
- [ ] T124 [P] [US5] RED: `ExportPrivacyModeTest` — with privacy mode on, the file contains unmasked amounts and the sheet said so beforehand (FR-031, research R9)
- [ ] T125 [P] [US5] RED: `ExportFailureTest` — a cancelled or refused write is reported as failed, presents no partial file as complete, and stays retryable (FR-032)
- [ ] T126 [P] [US5] RED: `OpenReportsDeepLinkTest` — `OpenReports(periodRef)` opens Reports at that period; an unparseable or out-of-range ref falls back to the current month without crashing (FR-033, `contracts/routes.md`)

### Implementation for User Story 5

- [ ] T127 [P] [US5] GREEN: `CategoryBreakdownDto` mapping into a `Statement` for the category-breakdown report (the function already exists from 5a)
- [ ] T128 [US5] GREEN: implement `ReportsViewModel.kt` and `ReportsScreen.kt` with the report list read from `InsightsConfig.kt`
- [ ] T129 [US5] GREEN: implement the custom-range sheet using the `DateRangeSheet` component (batch B2 — if unbuilt when this lands, build it in `:libs:core`, never locally; research R7)
- [ ] T130 [P] [US5] GREEN: implement `export/CsvStatementWriter.kt` — serialises the in-memory `Statement`, never re-queries (research R5)
- [ ] T131 [P] [US5] GREEN: implement `export/PdfStatementWriter.kt` using `android.graphics.pdf.PdfDocument`, typeset per design system §11; no new dependency (Article XI)
- [ ] T132 [US5] GREEN: implement `ExportFormatSheet.kt` and the `ACTION_CREATE_DOCUMENT` destination flow; state the privacy-mode exemption at the point of export
- [ ] T133 [US5] GREEN: run export off the main thread and surface progress and failure states in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/ReportsViewModel.kt` (FR-032)
- [ ] T134 [US5] GREEN: add the statement-export control to `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/settings/InsightsSettingsEntry.kt`, now that it can produce a file (FR-047, 004 FR-018's ordering rule)
- [ ] T135 [US5] GREEN: register `OpenReports` handling in the shell's navigation dispatcher, treating the extra as untrusted input (`contracts/routes.md`)
- [ ] T136 [US5] REFACTOR: confirm both writers in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/export/` consume one shared statement model and that neither can be constructed from a fresh query (research R5)
- [ ] T137 [US5] Add `contentDescription` coverage for the period picker, report rows and export actions (NFR-006)
- [ ] T138 [US5] Move all F5 strings to `strings.xml`
- [ ] T139 [US5] Sec: confirm nothing leaves the device except the user-initiated file write — no analytics, no upload, no telemetry on the export path (implementation plan §7 Phase 5 step 6)
- [ ] T140 [US5] QA: close `SIG-FLOW-001` and `SIG-FLOW-002`

**Checkpoint**: Reports and export work; the tab is feature-complete except the two gated reports.

---

## Phase 9: User Story 6 — Move between statements without losing the period (Priority: P6) — sub-phase 5e

**Goal**: The month-end review chain keeps its period from the first screen to the exported file.

**Independent Test**: Open the summary at a period, navigate through each statement to reports in sequence, and confirm at each step that the period is the one selected at the start and that an export taken at the end covers it.

### Tests for User Story 6 ⚠️

- [ ] T141 [P] [US6] RED: `PeriodCarryTest` — a period selected on any Insights screen is the period every other Insights screen opens with, without re-selection (FR-039)
- [ ] T142 [P] [US6] RED: `PeriodCarryOverrideExceptionTest` — a balance-sheet override is the sole exception: local, non-propagating, and it never mutates the carried period (FR-039)
- [ ] T143 [P] [US6] RED: `BackContractTest` additions in `libs/core` — F1 is a tab root with no back arrow; F2–F5 each have exactly one parent (F1) and one back arrow (N1, N2, FR-040)

### Implementation for User Story 6

- [ ] T144 [US6] GREEN: ensure every Insights ViewModel reads the period from `InsightsPeriodStore` rather than holding its own copy
- [ ] T145 [US6] QA: execute the manual chain walk from `quickstart.md` §5e — F1 → F2 → F3 → F4 → F5 → export, then repeat with an override in force — and close `SIG-FLOW-003` (Automatable: N)
- [ ] T146 [US6] Ratchet `globalLineFloor` — **sub-phase 5e checkpoint**

**Checkpoint**: Sub-phases 5a–5e ship. Insights is complete except the two gated reports.

---

## Phase 10: User Story 5 (More) — Investment returns and tax summary — sub-phase 5f ⛔ GATED

**Goal**: The two reports F5 draws under "More".

> **⛔ DO NOT START.** This sub-phase is blocked on an accepted decision record fixing the
> investment-returns calculation (spec Scope Boundaries "Gating prerequisite"; research R8 lists the
> five questions it must answer). Without it there is no correct implementation to write and no test
> that could distinguish one from a wrong one. Nothing in Phases 1–9 depends on this sub-phase.

- [ ] T147 [US5] Write the investment-returns decision record into `platform/DECISIONS.md`, answering all five questions in research R8. **Take its number from the register at the moment of writing** — never reserve one in advance (three prior collisions are recorded in that file's numbering-hygiene notes)
- [ ] T148 [US5] Write the missing catalog rows for both "More" reports (FR-034 – FR-038) into §8 of `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`, citing the decision record for the returns definition (Article II)
- [ ] T149 [US5] Author `supabase/schemas/finance/30_functions/report_investment_returns.sql` to the shape the decision record fixes — `security invoker`, `stable`
- [ ] T150 [US5] Add `tax_section text null` to `supabase/schemas/finance/10_tables/categories.sql` — append-only TEXT constants, same convention as `sector` (research R12, Article IX)
- [ ] T151 [US5] Author `supabase/schemas/finance/30_functions/report_tax_summary.sql` — `security invoker`, `stable`, excluding rows with a null `tax_section`
- [ ] T152 [US5] Generate the migration with `supabase db diff -f insights_phase5_5f`, hand-add the `grant execute`, regenerate `database.ts`, run `gen_schema_docs.py --check`
- [ ] T153 [P] [US5] RED: `InvestmentReturnsReproducibilityTest` — the figure recomputes by hand from exactly the movements the report names, on three fixtures (FR-034)
- [ ] T154 [P] [US5] RED: `InvestmentReturnsDisclosureTest` — the screen states the movement set, the date span and the method (FR-034)
- [ ] T155 [P] [US5] RED: `InvestmentReturnsNoSolutionTest` — a period with no investment movements, or one admitting no solution, states so plainly; never zero, a dash, or an arbitrary value (FR-036)
- [ ] T156 [P] [US5] RED: `TaxSummaryBasisTest` — the report states which categories it treats as tax-relevant and on what basis (FR-037)
- [ ] T157 [P] [US5] RED: `TaxSummaryProvenanceTest` — every figure traces to a category the report named; none comes from outside that set (SC-015)
- [ ] T158 [P] [US5] RED: `TaxSummaryRenameTest` — a tax-marked category renamed keeps its line, matched by identity (FR-037, BR-D3)
- [ ] T159 [US5] GREEN: DTOs, mappers and `InsightsRepository` calls for both reports
- [ ] T160 [US5] GREEN: `InvestmentReturnsScreen.kt`, `TaxSummaryScreen.kt` and `TaxCategorySheet.kt` in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/`
- [ ] T161 [US5] GREEN: both screens in `apps/finance/feature/insights/insights/src/main/java/com/dhruv/finance/insights/` obey every 5e rule — readable in full on screen before export, exportable in both formats, period-consistent (FR-038)
- [ ] T162 [US5] Sec: RLS check with the second account against both new functions
- [ ] T163 [US5] QA: close the rows from T148, referencing the decision record by id; ratchet `globalLineFloor` — **sub-phase 5f checkpoint**

---

## Phase 11: Polish & Cross-Cutting Concerns

- [ ] T164 Write the four research corrections (R1 parameterised functions not views, R4 as-at derivation, R11 the two money universes, R12 the tax basis column) back into the Phase 5 section of `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` as a dated note
- [ ] T165 [P] Update `apps/finance/CLAUDE.md`'s Modules section — `insights` now exists; remove it from the "not yet created" list
- [ ] T166 [P] Update `apps/finance/FEATURES.md` with the Insights module row
- [ ] T167 Run `/dhruv-ui-review` over all five screens against the design system's screen-state matrix (§7) and accessibility gate (§9)
- [ ] T168 Verify NFR-8 by review: no Insights screen sums a ledger, and no client code aggregates across two calls to build a wider period (`contracts/reporting-functions.md` caller rules)
- [ ] T169 [P] Verify NFR-5 by review and detekt: zero raw dp/sp/hex literals and zero `MaterialTheme.colorScheme`/`.typography` references across the module
- [ ] T170 Confirm every user-visible string in the module lives in `strings.xml`
- [ ] T171 Run the full `quickstart.md` validation end to end, including the manual chain walk
- [ ] T172 Confirm the QA catalog's coverage-summary table reflects every `SIG-*` row as closed or explicitly deferred with a stated reason
- [ ] T173 Confirm `./gradlew regressionCheck` is green and the coverage floor was ratcheted once per sub-phase, never ahead of landed tests
- [ ] T174 Raise the standing `_FEATURES` omission for `:apps:finance:feature:onboarding` as its own small change — reported by three consecutive plans and still open (research R14). **Do not fix it inside this feature's diff**
- [ ] T175 Bump the minor version in `platform/versions.json` per the phase convention

---

## Dependencies & Execution Order

### Sub-phase dependencies (the unit that ships)

```
5a (Setup + Foundational + US1 + US7)
 ├─→ 5b (US2 cashflow)      ─┐
 ├─→ 5c (US3 balance sheet) ─┼─→ 5e (US5 + US6 reports & export)
 └─→ 5d (US4 profit & loss) ─┘         │
                                        └─→ 5f (US5 More) ⛔ also gated on the decision record
```

- **5a blocks everything.** It owns the module, the flag, the period model and the repository seam.
- **5b, 5c and 5d are genuinely independent of each other** — different SQL functions, different
  screens, no shared state beyond the period store. They can be worked in parallel or reordered.
- **5e needs all three**, because its job is presenting and exporting them.
- **5f needs 5e** and the decision record. It is the only sub-phase that can be dropped entirely
  without leaving the tab incoherent.

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies beyond Phases 2 and 3 of the product being shipped
- **Phase 2 (Foundational)**: depends on Phase 1 — BLOCKS all user stories
- **Phases 3–4 (US1, US7)**: depend on Phase 2; together they complete sub-phase 5a
- **Phases 5, 6, 7 (US2, US3, US4)**: each depends only on sub-phase 5a
- **Phases 8–9 (US5, US6)**: depend on Phases 5, 6 and 7
- **Phase 10 (US5 More)**: depends on Phase 8 **and** the decision record
- **Phase 11 (Polish)**: depends on all desired sub-phases being complete

### Within each sub-phase (constitution step order, non-negotiable)

`SA (schema + routes) → QA (catalog rows) → Backend RED → GREEN → REFACTOR → Android RED → GREEN →
REFACTOR → QA close → Sec → checkpoint`

No Backend or Android task starts before that sub-phase's catalog rows exist (Article II). Tests are
written and **fail** before implementation (Article I).

### Parallel Opportunities

- T004, T005, T006 (three different files) run together
- T012, T013 (two SQL files) run together; T016 runs alongside them
- T018, T019, T023 — all resolver and reconciler tests, different files
- T025, T026, T027 — models, DTOs and mappers, different files
- **T031–T039** — all nine US1 tests, different files
- **T053–T056** — all four US7 tests
- **T065–T070** — all six US2 tests
- **T082–T091** — all ten US3 tests
- **T101–T107** — all seven US4 tests
- **T115–T126** — all twelve US5 tests
- T130, T131 — the two export writers, different files
- **Whole sub-phases**: with more than one engineer, 5b, 5c and 5d run in parallel once 5a lands

---

## Parallel Example: sub-phase 5a, User Story 1

```bash
# All nine US1 tests together — different files, no shared state:
Task: "RED: PeriodSummaryMappingTest — zero income maps to null savings rate"
Task: "RED: InsightsRepositoryPeriodSummaryTest — Content-Profile header and resolver dates"
Task: "RED: InsightsRepositoryConsentTest — consent off dispatches nothing"
Task: "RED: MonthlySummaryViewModelTest — savings rate equals surplus over income"
Task: "RED: MonthlySummaryZeroIncomeTest — never renders 0%"
Task: "RED: MonthlySummaryTransferExclusionTest — transfers move nothing"
Task: "RED: CategoryMovementTest — movement recomputes; near-zero reads flat"
Task: "RED: ComparativeInsightTest — names the months averaged, flagged derived"
Task: "RED: MonthlySummaryStatesTest — the full state matrix"
```

---

## Implementation Strategy

### MVP: sub-phase 5a only

1. Phase 1 (Setup) → Phase 2 (Foundational) → Phase 3 (US1) → Phase 4 (US7)
2. **STOP and VALIDATE**: run `quickstart.md` §5a end to end
3. The Insights tab is a working monthly summary with its own settings entry — honest and shippable,
   not a half-drawn reports screen

### Incremental delivery

1. **5a** → the tab answers "did I save this month" → ship
2. **5b** → cashflow explains that answer → ship
3. **5c** → position as at a date → ship
4. **5d** → year-on-year comparison → ship
5. **5e** → arbitrary periods, all reports, export → ship
6. **5f** → the two gated reports, once the decision record exists → ship

Each adds value without breaking the previous. The tab is never in a state where a link goes nowhere:
no sub-phase ships a route to a screen it did not build.

### Parallel team strategy

1. Everyone completes 5a together — it is the shared foundation
2. Then: Developer A on 5b, Developer B on 5c, Developer C on 5d
3. Regroup for 5e, which integrates all three
4. 5f whenever its decision record lands

---

## Notes

- `[P]` = different files, no dependencies on incomplete tasks
- `[Story]` maps a task to its spec user story; Setup, Foundational and Polish carry no story label
- Verify every test **fails** before implementing against it (Article I)
- Commit after each task or logical group
- Stop at any sub-phase checkpoint to validate independently
- One QA row is **expected to be deferred, not closed**: FR-050's master-switch suppression and
  privacy masking of the monthly-summary alert cannot be exercised until the notifications phase
  ships delivery. Deferring it with that reason is correct; closing it would be false
- Do not move `PeriodResolver` under `tracker/` (research R2), do not flip a reporting function to
  `security definer` (research R3), and do not derive cashflow's closing balance from opening plus
  net change (data-model) — all three are recorded because each is a plausible wrong "fix"

---

## Phase 5g: Gap remediation (multi-agent spec audit, 2026-08-22)

**Source**: `apps/finance/docs/superpowers/reviews/2026-08-22-spec-phase-gap-register.md`.

This is the strongest-traced spec of the six (46 of 51 FRs cited in tasks, against 1 of 13 in 001),
so its findings are mostly about what earlier phases owe it.

- [ ] T176 [SA] **Restate BR-E3 (categories excluded from spend) as an FR.** FR-010 covers investment
      contributions in cashflow only; FR-001/FR-002/FR-005 (savings rate, expense total, "where it
      went") and FR-013..FR-015 (P&L) carry no exclusion clause, and no `SIG-*` row tests one. The
      spec currently relies on an Assumption ("inherited, not redefined") rather than a requirement —
      an inherited rule with no local assertion is how a reporting surface silently double-counts
- [ ] T177 [SA] Add **loading and error** state requirements. FR-041/FR-045 mandate
      signed-out/offline/not-configured/disabled/empty, but neither a `SkeletonBlock` loading state
      nor a retryable `RetryErrorCard` is required anywhere, and DESIGN-SYSTEM §7 makes all eight
      states binding per screen
- [ ] T178 [SA] **Freeze the `categories.tax_section` value list** in `data-model.md`. It is declared
      "append-only TEXT constants, same convention as `sector`" — and, like `sector`, never
      enumerated, while F5's tax summary groups by it and FR-037 requires the report to state on what
      basis a category is tax-relevant
- [ ] T179 [SA] Name the function call behind **F1's "comparative insight vs the 12-month average"**.
      Presumably a second `report_period_summary` over a 12-month range, but the data model does not
      say so and no task states the call pattern
- [ ] T180 [SA] Confirm **`NxTabs`** has an owner before 5b starts. This spec needs it for statement
      tabs and records it as "owed by earlier phases"; no phase builds it (003 T146 now tracks the
      decision). If it is still unowned when 5b begins, build it in `:libs:core` — never hand-roll it
      inside the feature module
- [ ] T181 [SA] Depends on **001 T045** (cost basis). `report_investment_returns` is "deliberately
      unspecified" pending an ADR, and research R8 records that the underlying data may not support
      any candidate answer — "Phase 3's `transactions.goal_id` exists but there is no holding link".
      The blocker is therefore a missing schema path, not only an undecided formula; 5f cannot be
      scoped until 001 T045 lands
- [ ] T182 [SA] Depends on **001 T046** (net-worth history). If Phase 2 defers its Home/C1 trend to
      this phase's `report_balance_sheet(p_as_of)`, that becomes a requirement here and needs an FR
      and a task — currently neither phase owns the series
- [ ] T183 [SA] Depends on **001 T047**. This spec assumes `finance.liabilities_meta`; 001 declares
      `public.liabilities_meta`. Re-verify after 001 decides

---

## Phase 5h: Gap remediation, round 2 (UI/UX + requirements audit, 2026-08-22)

This phase is the best-traced of the six (46 of 51 FRs cited) and its edge-case coverage is the
strongest. The findings below are correspondingly narrow.

- [ ] T184 [SA] **SC-014 and SC-015 measure work that may never ship.** Both measure "More" reports
      (investment returns, tax summary) which live in 5f, hard-gated on an unwritten decision record —
      and the spec itself says the feature "ships nothing under 'More'" if that never lands. Neither
      SC carries conditional wording, so a 5a–5e ship is permanently two SCs short. Restate them as
      conditional on 5f, or move them into 5f's own criteria
- [ ] T185 [SA] **T169 verifies NFR-5 "by review and detekt" — detekt cannot check this today.**
      `config/detekt/detekt.yml:34-35` sets `MagicNumber: active: false`, there is no
      `ForbiddenImport`/`ForbiddenMethodCall` rule for `MaterialTheme.colorScheme`/`.typography`, and
      `DependencyRulesTest` has five rules, none about tokens. Either depend on **001 T072** (which
      now adds the rule) or state the check as review-only and stop naming a tool that will not fire
- [ ] T186 [Android] **PDF export is missing two §11 conventions.** "No logo beyond the wordmark" has
      zero occurrences in this spec, and T131's "typeset per design system §11" is a single
      unelaborated phrase with no test behind the type-hierarchy mapping. The header (period, date,
      app version) and the unmasked-export rule are both correctly owned already
- [ ] T187 [QA] Verify **light and dark** render from the same tokens (N7) and the three responsive
      tiers — statements and reports are the most table-dense screens in the app and the most likely
      to break at the small tier, and no phase plans responsive verification at all
- [ ] T188 [Android] Add **F1's export affordance**. The design draws "Month selector **+ export**" on
      F1; T045 wires the period selector and export lands only on F5 in 5e (T132)
- [ ] T189 [Android] **Use `MoneyText`, `StatDeltaChip` and `ThreeUpStatRow`** for F1's
      `INCOME · EXPENSE · SURPLUS` row and its "change vs last month" values (`+31%`, `flat`) — the
      chips are already built and named in zero tasks feature-wide, and §1's never-colour-only rule
      applies to every one of those deltas

---

## Phase 5i: DB obligations inherited from the Phase 2 readiness decisions (2026-08-23)

Binding conventions: `../001-net-worth-tracker/data-model.md` § "Maintenance conventions" and the
[readiness architecture decisions](../../docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md).
This phase's reporting objects are **functions**, not views, and research R3's reasoning for keeping
them `security invoker` is correct and unchanged — Phase 2 has now applied the same reasoning to
views, which is where it was missing.

- [ ] T190 [SA] **Reuse `finance.v_net_worth_history` rather than re-deriving the position series.**
      Phase 2 now ships it, using the identical "latest valuation ≤ date" rule this phase's
      `report_balance_sheet(p_as_of)` uses. Two implementations of one derivation is how the
      as-at-date semantics drift apart
- [ ] T191 [SA] **`has_self_valued` is now definable.** `valuations.source` is frozen to
      `MANUAL · STATEMENT · IMPORT · CORRECTION`, and the partition is **`MANUAL` and `CORRECTION`
      are self-valued; `STATEMENT` and `IMPORT` are not**. F4's footnote depended on a partition
      that did not exist when this spec was written
- [ ] T192 [SA] **`invested_paise` exists on `finance.holdings`** (Phase 2). It funds a **simple
      return**, not XIRR — it is a single scalar and XIRR needs a dated cashflow series, which still
      requires the holding↔transaction link research R8 correctly says no phase models. This narrows
      5f's blocking decision record; it does not close it
- [ ] T193 [SA] **`liabilities_meta` is `finance.liabilities_meta`** — this spec already assumed
      that, and Phase 2 now agrees. No change beyond re-verifying after 001 T047 lands
- [ ] T194 [SA] Confirm the `grant execute … to authenticated` on every reporting function is a
      **hand-appended** statement in the migration — `db diff` emits no grants (ADR-0032 caveat), and
      a function without one is simply unreachable. This phase already does this per sub-phase;
      the task is to keep it true for the 5f functions if they land

---

## Phase 5j: Closure — tracking (runs last, after the checkpoint is green)

Per the tracking rule in `apps/finance/CLAUDE.md`. Each sub-phase 5a–5f merges separately, so run
this block **per sub-phase** for the CHANGELOG entry, and once at the end for the FEATURES.md row.

- [ ] T195 [P] Move **`insights`'s row in [`apps/finance/FEATURES.md`](../../FEATURES.md)** out of
      the "Planned" table into the shipped Modules table — owner tab Insights, flag `insights`
- [ ] T196 [P] Rewrite **`apps/finance/feature/insights/insights/README.md`** with the real F1–F5
      screens, the six `finance.report_*` functions it consumes, `PeriodResolver`, and the flag key.
      State plainly which reports actually shipped — if 5f stayed blocked, "More" ships empty, and a
      reader must not have to infer that from a task list
- [ ] T197 [P] Add the **root `CHANGELOG.md`** entry per merged sub-phase: monthly summary, cashflow,
      balance sheet, P&L, reports and export. Note that **PDF/CSV export is deliberately unmasked**
      even under privacy mode — an explicit user act, stated in the export dialog — since that is
      the kind of thing a reader will otherwise report as a bug
- [ ] T198 [P] Update the **spec-kit tracking table** (implementation plan §7) — Phase 5 to *shipped*,
      and record 5f's outcome explicitly (shipped, or still blocked on the investment-returns
      decision record)
- [ ] T199 [P] Add this phase's routes to surface registry §1 — `F5-export`, `F5-range` and `F4-date`
      are three of the five rows the registry is currently behind by