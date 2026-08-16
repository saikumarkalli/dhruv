---
description: "Task list for Net Worth Tracker (Phase 2)"
---

# Tasks: Net Worth Tracker (Phase 2)

**Input**: Design documents from `apps/finance/specs/001-net-worth-tracker/`

**Prerequisites**: plan.md, spec.md, data-model.md, contracts/routes.md, research.md, quickstart.md
(all present — see `AVAILABLE_DOCS`)

**Tests**: Included and REQUIRED, not optional — constitution Article I (Test-First) mandates
RED → GREEN → REFACTOR with every test citing a scenario ID from
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3 (`NW-*`) / §12
(`HOM-*`). Those catalog rows already exist and are reviewed (marked "done") — tasks below cite
them, they do not recreate them (constitution Article II).

**Organization**: Tasks are grouped by user story (spec.md's Stories 1–5, priority P1–P5) so each
story is independently implementable, testable, and demoable — matching spec.md's own
"Independent Test" per story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US5, matching spec.md's five user stories
- Every path below is a real path in this repo, not a placeholder

---

## Phase 1: Setup

**Purpose**: Stand up the new Gradle module before any story-specific code.

- [ ] T001 Create `:apps:finance:feature:networth` module skeleton — `apps/finance/feature/home/networth/build.gradle.kts` (`dhruv.android.library` + `dhruv.android.compose`, deps on `:apps:finance:data`, `:libs:core`, `:libs:settings`, same shape as `apps/finance/feature/plan/loans`); register in `settings.gradle.kts`
- [ ] T002 [P] Create `di/NetWorthModule.kt` Koin module stub in `apps/finance/feature/home/networth/` and aggregate it in `CalculatorApplication`
- [ ] T003 [P] Create `NetWorthConfig.kt` scaffold in `apps/finance/feature/home/networth/` (screen-level constants — sector labels, chart ranges — per the no-hardcoding rule; filled in per-story below, not left empty)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema and shared components every story in this phase depends on.

**⚠️ CRITICAL**: No user story task below may start before this phase closes (constitution
Article II — SA schema + QA catalog rows, in that order, before Backend/Android code).

- [ ] T004 [SA] Write `supabase/migrations/0002_networth_phase2.sql` — `liabilities_meta` table (RLS
      transitive through `holding_id → holdings.user_id`, mutable per data-model.md, not
      append-only) + `v_latest_valuation` + `v_net_worth_by_sector` views, per data-model.md; add
      the matching `DELETE FROM liabilities_meta` line to `delete_my_data()` in this same migration
      (the `0001_init.sql` comment's own reminder — a forgotten table here breaks DPDP erasure
      silently)
- [ ] T005 [P] [Sec] RLS policy test for `liabilities_meta` and both new views against the dev
      Supabase project — verifies no cross-user leakage, no client UPDATE/DELETE path exists where
      the schema says there shouldn't be one
- [ ] T006 [P] Build component batch B3 (charts) in
      `libs/core/src/main/kotlin/com/dhruv/core/ui/components/charts/` — `DonutChart` +
      `RankedLegend`, `PieChart`, `AmortisationDonut`, `PaceRing` (repurposes existing
      `FinancialHealthRing` per design system §5.2 note — extend, do not delete)
- [ ] T007 [P] Build B6's `NxSelect` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/inputs/NxSelect.kt`
- [ ] T008 [P] Build B9's `SelectionSheet` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/overlays/SelectionSheet.kt`
- [ ] T009 [P] Extend `NxTextField` (`libs/core/src/main/kotlin/com/dhruv/core/ui/components/inputs/NxTextField.kt`) with an error state + helper text — design system §5.3, first consumer is C4/C5 below
- [ ] T010 [P] Extend `NxButton` (`libs/core/src/main/kotlin/com/dhruv/core/ui/components/actions/NxButton.kt`) with sizes + loading + block (full-width) treatment — design system §5.3

**Checkpoint**: Schema migrated, RLS verified, all five component gaps (B3/B6/B9 +
`NxTextField`/`NxButton`) closed. User story work can begin.

---

## Phase 3: User Story 1 — Record what I own or owe, see net worth (Priority: P1) 🎯 MVP

**Goal**: Add a holding (asset or liability) with a value; see it in the net-worth total and
category breakdown immediately (spec.md Story 1).

**Independent Test**: Add one asset + one liability with values; confirm the net-worth total on
C1 equals asset value minus liability value.

### Tests for User Story 1 (write first, confirm they FAIL before implementation)

- [ ] T011 [P] [US1] `HoldingRepositoryTest` — sector enum rejected if not in the fixed list, citing
      NW-BR-004, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/HoldingRepositoryTest.kt`
- [ ] T012 [P] [US1] `HoldingRepositoryTest` — holding + first valuation written atomically
      (both-or-neither), citing NW-BR-001, same file as T011
- [ ] T013 [P] [US1] Net-worth aggregation test — total equals `v_net_worth_by_sector` output, never
      a client-side reduction, citing NW-BR-006, in
      `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/NetWorthAggregationTest.kt`

### Implementation for User Story 1

- [ ] T014 [US1] Implement `HoldingRepository.createWithFirstValuation()` — atomic write, sector
      enum validated at this boundary (depends on T011, T012) in
      `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/HoldingRepository.kt`
- [ ] T015 [US1] Implement `NetWorthOverviewViewModel` (C1) — reads `v_net_worth_by_sector`, citing
      NW-UI-001/NW-FLOW-001, in `apps/finance/feature/home/networth/NetWorthOverviewViewModel.kt`
- [ ] T016 [US1] Build `NetWorthOverviewScreen` (C1) — donut + ranked legend (T006's `DonutChart`),
      NET/ASSETS/LIABILITIES subtotals, FAB add, `FeatureHost`-wrapped with the `networth` flag key,
      in `apps/finance/feature/home/networth/NetWorthOverviewScreen.kt`
- [ ] T017 [US1] Build `AssetsScreen` (C2) — sector-grouped list, filter chips, FAB add, in
      `apps/finance/feature/home/networth/AssetsScreen.kt`
- [ ] T018 [US1] Build `AddEditHoldingScreen` (C4) — I OWN/I OWE toggle, sector picker via T008's
      `SelectionSheet` (never free text, NW-BR-004), value + date, T009's `NxTextField` error state
      for validation, footer stating the append-only rule verbatim (spec.md Story 3), in
      `apps/finance/feature/home/networth/AddEditHoldingScreen.kt`
- [ ] T019 [US1] Wire C1 sector-tap → C2 filtered navigation via `NavTarget`, citing NW-UI-001
- [ ] T020 [US1] Add `SignedOutCard`/`OfflineStateCard` to C1 and C2 per the screen-state matrix,
      citing NW-UI-005

**Checkpoint**: User Story 1 fully functional and independently testable — this alone is a working
(minimal) net-worth tracker.

---

## Phase 4: User Story 2 — Review a holding's detail and value history (Priority: P2)

**Goal**: Open a holding, see current value, trend, and full dated history (spec.md Story 2).

**Independent Test**: With one holding recorded, open it and confirm value, a range-selectable
trend, and a newest-first history list all render.

### Tests for User Story 2

- [ ] T021 [P] [US2] History-ordering test — entries newest-first, each with its delta vs the
      previous entry, citing NW-UI-002, in
      `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/ValuationRepositoryTest.kt`

### Implementation for User Story 2

- [ ] T022 [US2] Implement `HoldingDetailViewModel` (C3) — history query + trend range filtering,
      citing NW-UI-002, in `apps/finance/feature/home/networth/HoldingDetailViewModel.kt`
- [ ] T023 [US2] Build `HoldingDetailScreen` (C3) — value + %, trend chart with 3M/6M/1Y/All range
      chips, INVESTED · GAIN · simple-return% stats (FR-006a — placeholder %, not IRR, per spec.md
      Assumptions), VALUATION HISTORY list (dated, sourced, delta-vs-previous), *Update value* /
      *Link to goal* actions, in `apps/finance/feature/home/networth/HoldingDetailScreen.kt`
- [ ] T024 [US2] Wire C2/C6 → C3 navigation via `NavTarget` (`OpenHolding`/`OpenLiability`)

**Checkpoint**: Stories 1–2 both independently functional.

---

## Phase 5: User Story 3 — Update a value without losing history (Priority: P3)

**Goal**: Record a new value; see the live delta; previous values stay visible, never altered
(spec.md Story 3).

**Independent Test**: Record a second value for an existing holding; confirm the first value is
still visible in history, unaltered.

### Tests for User Story 3

- [ ] T025 [P] [US3] Correction test — wrong entry soft-deleted (`deleted_at` set) + new entry
      appended, no UPDATE ever issued against `value_paise`, citing NW-BR-002/NW-BR-003, in
      `ValuationRepositoryTest.kt` (same file as T021)

### Implementation for User Story 3

- [ ] T026 [US3] Implement `ValuationRepository.recordValue()` + `.correctValue()` (soft-delete +
      append, never UPDATE — enforced by T004's RLS having no UPDATE policy at all, this is the
      client-side half of that guarantee), citing NW-BR-002/NW-BR-003, in
      `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/ValuationRepository.kt`
- [ ] T027 [US3] Build `AddValuationSheet` (C5) — last value + date shown, live delta preview as the
      user types (amount + %, before submit), date + source picker, citing NW-UI-003, in
      `apps/finance/feature/home/networth/AddValuationSheet.kt`

**Checkpoint**: Stories 1–3 independently functional.

---

## Phase 6: User Story 4 — Track liabilities and payoff progress (Priority: P4)

**Goal**: Track what's owed, see outstanding balance and payoff progress, get a prepay-savings
projection (spec.md Story 4).

**Independent Test**: Add a loan-type liability with rate/EMI; open its detail; confirm balance,
payoff progress, and a prepay projection all render.

### Tests for User Story 4

- [ ] T028 [P] [US4] `LiabilityRepositoryTest` — CRUD on `liabilities_meta`, `liability_type` enum
      rejected if not in the fixed list (mirrors NW-BR-004 for liability types), amortisation split
      sums to total obligation, in
      `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/LiabilityRepositoryTest.kt`

### Implementation for User Story 4

- [ ] T029 [US4] Implement `LiabilityRepository` (depends on T028) in
      `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/LiabilityRepository.kt`
- [ ] T030 [US4] Build `LiabilitiesScreen` (C6) — grouped by `liability_type`, TOTAL OUTSTANDING ·
      MONTHLY OUTGO · DEBT-FREE BY stats, payoff progress per row, in
      `apps/finance/feature/home/networth/LiabilitiesScreen.kt`
- [ ] T031 [US4] Build `LiabilityDetailScreen` (C7) — amortisation donut (T006), rate/EMI/debit
      day/tenure/linked account/collateral, prepay-savings projection, citing NW-UI-004, in
      `apps/finance/feature/home/networth/LiabilityDetailScreen.kt`
- [ ] T032 [US4] Wire C7's prepay hand-off to the existing loan/EMI calculator via `NavTarget`
      (`OpenPlanTool(PlanTool.LOAN)`) — cross-feature navigation by id, never a class reference
      (constitution Article III)

**Checkpoint**: Stories 1–4 independently functional.

---

## Phase 7: User Story 5 — See net worth at a glance on Home (Priority: P5)

**Goal**: Home shows net-worth total, trend, and upcoming loan/EMI obligations without navigating
away (spec.md Story 5).

**Independent Test**: With holdings + a liability with a due date recorded, open Home fresh and
confirm all three render without navigation.

### Tests for User Story 5

- [ ] T033 [P] [US5] `HomeViewModel` test — hero figure matches C1's total, UPCOMING is EMI-only
      this phase (implementation plan's Phase 2 scoped-dependency note — card-bill rows wait for
      Phase 3's `accounts` table), citing HOM-UI-001/HOM-UI-003, in
      `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/home/HomeViewModelTest.kt`
- [ ] T034 [P] [US5] Ask-pill visibility test — renders on Home, not on Calc/Money, citing
      HOM-UI-004, same file as T033

### Implementation for User Story 5

- [ ] T035 [US5] Rewrite `HomeViewModel` (shell-owned, not `:feature:networth` — module-standard
      doc's HOM/PLN correction) in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/home/HomeViewModel.kt`
- [ ] T036 [US5] Replace the placeholder `DashboardScreen` with the real Home (01) — greeting, date
      line, net-worth hero (value + ▲/▼% + sparkline), 4 quick actions (Loan EMI/SIP/Currency/GST)
      via `NavTarget`, UPCOMING (EMI-only, sourced from `liabilities_meta.debit_day`), Ask pill,
      citing HOM-UI-001/002/003/004, in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/home/HomeScreen.kt`
- [ ] T037 [US5] Add `SignedOutCard`/`OfflineStateCard` to Home in place of the hero card, citing
      HOM-FLOW-001

**Checkpoint**: All five stories independently functional — Phase 2 feature-complete.

---

## Phase 8: Polish & Cross-Cutting (QA close, Sec, checkpoint)

**Purpose**: The module-standard doc's steps 5–7 (§4) — QA closes rows, Sec re-passes, merge gate.

- [ ] T038 [P] [QA] Close every `NW-*`/`HOM-*` row in
      `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3/§12 as its
      test lands; update the §13 coverage-summary table. Confirm append-only holds at the SQL layer
      too (no UPDATE policy exists on `valuations`), not just the repository layer
- [ ] T039 [P] [Sec] Full DPDP/secrets/RLS checklist pass on `liabilities_meta` + the two new views
      (module-standard doc §4 step 6); confirm `delete_my_data()`'s new DELETE line (T004) actually
      removes `liabilities_meta` rows on a dev-project erasure test
- [ ] T040 Run `./gradlew regressionCheck` — all green, merged JaCoCo floor not regressed
      (constitution Article X)
- [ ] T041 Run `./gradlew checkTrackerMoneyPrecision` — confirms no `Double`/`Float` crept into
      `liabilities_meta`'s `emi_paise`/`rate_bps` handling (constitution Article VII)
- [ ] T042 Walk all 6 scenarios in `apps/finance/specs/001-net-worth-tracker/quickstart.md`
      end-to-end on a device/emulator
- [ ] T043 [P] Add `apps/finance/feature/home/networth/README.md` (module index entry, per every
      other feature module's convention) and link it from `apps/finance/FEATURES.md`
- [ ] T044 Bump the minor version in `platform/versions.json` (new feature module, ADR-0012's
      versioning rule); update the implementation plan's §7 tracking table row for Phase 2 to
      "shipped" and mark Phase 2's own checkpoint row (§7) complete

**Checkpoint**: `regressionCheck` green, every catalog row closed or explicitly deferred with a
stated reason (constitution's Development Workflow step 7) — Phase 2 merge-ready.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup — BLOCKS every user story (schema + shared
  components every story reads from).
- **User Stories (Phase 3–7)**: all depend on Foundational. US1 has no dependency on the other
  four; US2/US3 read holdings US1 creates but are independently testable once a holding exists
  from any source (including test fixtures — not a hard dependency on US1's screens specifically);
  US4 is fully parallel to US1–US3 (liabilities are independent of assets); US5 (Home) is the one
  story that meaningfully depends on US1 + US4 existing, since it summarizes both.
- **Polish (Phase 8)**: depends on however many of Phases 3–7 are in scope for this merge.

### Parallel Opportunities

- T006–T010 (all five component/extension tasks) run in parallel — five different files, zero
  shared dependency.
- Within each story, its test task(s) marked [P] run in parallel with each other, before that
  story's implementation tasks start.
- US1 and US4 (Phases 3 and 6) can be built in parallel by two engineers once Phase 2 closes —
  assets and liabilities share no screen or ViewModel.

## Implementation Strategy

### MVP first
Phase 1 → Phase 2 → Phase 3 (US1) → **stop, validate** (spec.md Story 1's Independent Test) →
this alone is a working net-worth tracker for assets. Liabilities (US4) and Home (US5) are the
next-highest-value additions, not required for a demoable MVP.

### Incremental delivery
Phase 1+2 → US1 (MVP) → US2 → US3 → US4 → US5 → Phase 8 (polish/checkpoint). Each story lands as
its own reviewable increment; the implementation plan's own Phase 2 checkpoint (§7) is satisfied
only once Phase 8 closes.
