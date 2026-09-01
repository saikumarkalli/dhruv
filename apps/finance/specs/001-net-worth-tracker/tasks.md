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

- [X] T001 Create `:apps:finance:feature:networth` module skeleton — `apps/finance/feature/home/networth/build.gradle.kts` (`dhruv.android.library` + `dhruv.android.compose`, deps on `:apps:finance:data`, `:libs:core`, `:libs:settings`, same shape as `apps/finance/feature/plan/loans`); register in `settings.gradle.kts` **with the `projectDir` remap** — the module directory is `apps/finance/feature/home/networth` but its Gradle coordinate is `:apps:finance:feature:networth`, so `project(":apps:finance:feature:networth").projectDir = file("apps/finance/feature/home/networth")` is required or Gradle resolves `apps/finance/feature/networth` and configuration fails (audit 2026-08-22; see the existing remaps at `settings.gradle.kts:59-69`). In the same task add `:apps:finance:feature:networth` to `coveredModules`/`_FEATURES` in the root `build.gradle.kts` — omitted in the original draft, and reported-but-declined by three later plans, which would leave this module's coverage invisible to the JaCoCo gate and to release notes
- [X] T002 [P] Create `di/NetWorthModule.kt` Koin module stub in `apps/finance/feature/home/networth/` and aggregate it in `CalculatorApplication`
- [X] T003 [P] Create `NetWorthConfig.kt` scaffold in `apps/finance/feature/home/networth/` (screen-level constants — sector labels, chart ranges — per the no-hardcoding rule; filled in per-story below, not left empty)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema and shared components every story in this phase depends on.

**⚠️ CRITICAL**: No user story task below may start before this phase closes (constitution
Article II — SA schema + QA catalog rows, in that order, before Backend/Android code).

- [X] T004 [SA] **DONE (verified on disk, pre-dates this session — see Phase 11's note that T004/T004a/T004b's scope narrowed to review).** **Author declaratively first, then generate the migration** (ADR-0032 decision 4 —
      the original draft hand-wrote the migration only, which fails the PR equivalence guard on
      merge; audit 2026-08-22). Write `supabase/schemas/finance/10_tables/liabilities_meta.sql`
      (RLS transitive through `holding_id → holdings.user_id`, mutable per data-model.md, not
      append-only) and `supabase/schemas/finance/20_views/v_latest_valuation.sql` +
      `v_net_worth_by_sector.sql`. **Both views MUST be declared `with (security_invoker = on)`** —
      without it a Postgres 15+ view runs as its owner and bypasses RLS on `holdings`/`valuations`,
      returning every user's rows to every signed-in caller through PostgREST (this is the single
      most severe finding of the 2026-08-22 audit; 005 already applies the equivalent reasoning to
      its reporting functions at `005/plan.md:138-146`). Then run `supabase db diff -f
      networth_phase2` to generate `supabase/migrations/`, review the generated SQL, and commit both
- [X] T004a [SA] **DONE.** Hand-append to the generated migration the statements `db diff` cannot emit
      (ADR-0032's caveat list): the `DELETE FROM liabilities_meta` line inside `delete_my_data()`
      (a forgotten table here breaks DPDP erasure silently — the `0001_init.sql` comment's own
      reminder), `grant select, insert, update, delete on finance.liabilities_meta to authenticated`,
      and `grant select on finance.v_latest_valuation, finance.v_net_worth_by_sector to
      authenticated` (ADR-0033 decision 4 — custom-schema objects are unreachable without an
      explicit grant; 002 T010 and 003 T022 already do this, 001 originally did not)
- [X] T004b [SA] **Partially done, reason stated (2026-09-01 session).** Ran `python
      scripts/db/gen_schema_docs.py equiv` and `... docs --check` — both pass, `SCHEMA.md` is fresh.
      Did **not** regenerate `web/src/shared/types/database.ts` — `supabase gen types` needs a live,
      authenticated connection (`supabase login`/`SUPABASE_ACCESS_TOKEN`), unavailable in this
      unauthenticated session (same class of gap ADR-0032's runbook already names). Remains open;
      run `supabase gen types typescript --linked --schema public,finance` once credentialed
- [ ] T005 [P] [Sec] RLS policy test for `liabilities_meta` and both new views against the dev
      Supabase project — verifies no cross-user leakage, no client UPDATE/DELETE path exists where
      the schema says there shouldn't be one. **Blocked**: needs a live, authenticated Supabase
      connection, unavailable in this session (same gap as T004b)
- [X] T006 [P] **DONE (2026-09-01).** Build component batch B3 (charts) — `DonutChart` + `RankedLegend`
      (`DonutChart.kt`), `PieChart` (`PieChart.kt`), `AmortisationDonut` (`AmortisationDonut.kt`),
      `PaceRing` (`PaceRing.kt`), all in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/` —
      **flat, not under a `charts/` subdirectory**: every existing built component (`BarChart.kt`,
      `Rings.kt`, `Chips.kt`, …) already lives flat in that one package, so this follows the real
      convention over the plan's aspirational subdirectory scheme (never actually used by any
      shipped component). `FinancialHealthRing` (`Rings.kt`) is untouched, not repurposed — a
      dedicated `AmortisationDonut` reads more clearly for a 3-segment paid/paid/remaining donut than
      overloading a single-progress ring API
- [X] T007 [P] **DONE (2026-09-01).** Build B6's `NxSelect` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/NxSelect.kt` (flat package, same note as T006)
- [X] T008 [P] **DONE (2026-09-01).** Build B9's `SelectionSheet` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/SelectionSheet.kt` (flat package, same note as T006)
- [X] T009 [P] **Already done pre-session** (004-settings 0b.4 T097, design system §5.3's "closed since last pass" note) — `NxTextField` already has `errorMessage` (`libs/core/src/main/kotlin/com/dhruv/core/ui/components/NxTextField.kt`)
- [X] T010 [P] **DONE (2026-09-01).** Extended `NxButton` (`libs/core/src/main/kotlin/com/dhruv/core/ui/components/NxButton.kt`) with `NxButtonSize` (Small/Medium), `loading` (spinner replaces label, click suppressed) and `block` (fillMaxWidth) — design system §5.3

**Checkpoint (2026-09-01)**: all five component gaps (B3/B6/B9 + `NxTextField`/`NxButton`) closed
and schema authored+reviewed (T004/T004a, static guards T004b). **Still open, both blocked on a
live authenticated Supabase connection unavailable in this session**: the migration has never
actually been executed against `dhruv-dev` (T078, Phase 11) and RLS is unverified (T005) — user
story work that only touches Compose/Kotlin can proceed, but nothing should be treated as confirmed
against the real database until T005/T078 close.

---

## Phase 3: User Story 1 — Record what I own or owe, see net worth (Priority: P1) 🎯 MVP

**Goal**: Add a holding (asset or liability) with a value; see it in the net-worth total and
category breakdown immediately (spec.md Story 1).

**Independent Test**: Add one asset + one liability with values; confirm the net-worth total on
C1 equals asset value minus liability value.

### Tests for User Story 1 (write first, confirm they FAIL before implementation)

- [X] T011 [P] [US1] **DONE (2026-09-01).** `HoldingRepositoryTest` — sector enum rejected if not in the fixed list, citing
      NW-BR-004, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/HoldingRepositoryTest.kt`
- [X] T012 [P] [US1] **DONE (2026-09-01).** `HoldingRepositoryTest` — holding + first valuation written atomically
      (both-or-neither), citing NW-BR-001, same file as T011
- [X] T013 [P] [US1] **DONE (2026-09-01).** Net-worth aggregation test — total equals `v_net_worth_by_sector` output, never
      a client-side reduction, citing NW-BR-006, in
      `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/NetWorthAggregationTest.kt`

### Implementation for User Story 1

- [X] T014 [US1] **DONE (2026-09-01).** Implement `HoldingRepository.createWithFirstValuation()` — atomic write via the
      `finance.create_holding_with_value` RPC, sector validated against the `Sector` enum at this boundary,
      in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/HoldingRepository.kt`. Also added,
      as prerequisites this task needed but the plan hadn't scheduled: `FinanceSchemaInterceptor` (+ test)
      wired into `SupabaseClientFactory.dataClient` (ADR-0033's `Accept-Profile`/`Content-Profile: finance`
      headers — `dataRetrofit` had no concrete endpoint yet before this), the `Holding`/`Sector`/`HoldingKind`/
      `SectorBreakdown`/`NetWorthSummary`/`CreateHoldingRequest` domain models, DTOs, mappers, and
      `HoldingRepository.list(kind)` (needed by T017) + `NetWorthRepository` (needed by T015), plus their
      Retrofit API interfaces (`HoldingApi`, `ValuationApi`, `NetWorthApi`) and Koin registration in
      `PlatformModule.kt`
- [X] T015 [US1] **DONE (2026-09-01).** Implement `NetWorthOverviewViewModel` (C1) — reads `v_net_worth_by_sector`, citing
      NW-UI-001/NW-FLOW-001, in `apps/finance/feature/home/networth/NetWorthOverviewViewModel.kt`
- [X] T016 [US1] **DONE (2026-09-01).** Build `NetWorthOverviewScreen` (C1) — donut (T006's `DonutChart`) +
      a custom clickable per-sector row list (not `RankedLegend`, which has no click callback — building the
      list inline keeps the shared component generic rather than coupling it to app-specific navigation data),
      NET/ASSETS/LIABILITIES subtotals via `ThreeUpStatRow`, FAB add, `FeatureHost`-wrapped with the
      `networth` flag key, in `apps/finance/feature/home/networth/NetWorthOverviewScreen.kt`
- [X] T017 [US1] **DONE (2026-09-01).** Build `AssetsScreen` (C2) — sector-grouped list (ASSET kind only, per
      routes.md; liabilities get their own C6 in Phase 6), filter chips, in
      `apps/finance/feature/home/networth/AssetsScreen.kt`. FAB add omitted here — C2 reaches add-holding
      only via C1's FAB in this phase, not a second entry point; not blocking, easy follow-up
- [X] T018 [US1] **DONE (2026-09-01).** Build `AddEditHoldingScreen` (C4) — I OWN/I OWE toggle
      (`SegmentedRow`), sector picker via T008's `SelectionSheet` (never free text, NW-BR-004), value entry
      via `NxTextField`'s error state, footer stating the append-only rule, in
      `apps/finance/feature/home/networth/AddEditHoldingScreen.kt`. **Deviations**: (1) date is fixed to
      "today" (`LocalDate.now()`) — no date picker exists in `:libs:core` yet (B2's `DateRangeSheet` is still
      §5.2 Planned), so manual back-dating a first valuation is deferred, not built; (2) the modal's close
      action renders as `NxTopBar`'s back-arrow, not a literal "✕" — the component has no separate close-icon
      variant (design-system gap, not fixed here)
- [X] T019 [US1] **DONE, with a scope correction (2026-09-01).** Wired C1 sector-tap → C2 filtered navigation,
      but **not** via `NavTarget` as literally written — `NavTarget`'s own doc comment scopes it to
      cross-feature/cross-tab dispatch, it has no intra-module case, and this codebase's only precedent for a
      multi-screen module (Plan's tool `NavHost` in `MainActivity.kt`) uses a local `NavHostController`, not
      `NavTarget`, for exactly this kind of drill-down. Built `NetWorthFeatureRoot` (new file,
      `NetWorthNavHost.kt`) — this module's own `NavHost` (`overview` → `assets/{sector}` → `addHolding`),
      self-contained so Phase 7 can mount it from Home with one call once the real Home screen exists (Home
      itself is untouched this phase — its placeholder `DashboardScreen` and the Home→C1 entry point are
      Phase 7's job, per that phase's own tasks). Documented in `NetWorthFeatureRoot`'s KDoc so this isn't
      silently rediscovered as a gap later
- [X] T020 [US1] **DONE (2026-09-01).** Added `SignedOutCard`/`OfflineStateCard`-class states to C1 (signed-out,
      consent-off, loading, error, empty) and C2 (loading, error, empty) per the screen-state matrix, citing
      NW-UI-005. C1/C2's `SignedOutCard` action buttons are currently no-ops (`onAction = {}`) — navigating to
      Settings/Account from inside this module would need a `NavTarget` case that doesn't exist yet; tracked
      as a follow-up, not silently dropped
- [X] Also added (not in the original task list, needed to make the above independently testable/reviewable):
      `NetWorthOverviewViewModelTest`, `AssetsViewModelTest`, `AddEditHoldingViewModelTest` — the module had
      zero tests before this phase, which would have shown as invisible/`(other)` coverage despite T005 of
      002's plan naming exactly this failure mode

**Checkpoint (2026-09-01)**: User Story 1 fully functional and independently testable at the Compose/Kotlin
level — `regressionCheck` green, ArchUnit green. **Not yet exercised against a live database** (same T005/T078
gap Phase 2's checkpoint already named) — the RPC/view calls are correct by inspection and unit-tested against
fakes, but have not made a real network call. Reaching C1 from the app's actual Home tab is Phase 7's job, not
this phase's; until then C1-C4 are reachable only by mounting `NetWorthFeatureRoot()` directly (e.g. from a
test harness or a temporary debug entry point).

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
      test lands; update the §14 coverage-summary table. Confirm append-only holds at the SQL layer
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

---

## Phase 9: Gap remediation (multi-agent spec audit, 2026-08-22)

**Source**: `apps/finance/docs/superpowers/reviews/2026-08-22-spec-phase-gap-register.md`.

**Already folded into existing tasks — do not re-do here**: the `projectDir` remap and
`coveredModules` registration (T001), declarative-first schema authorship + `security_invoker = on`
on both views (T004), the grants and `delete_my_data()` line (T004a), and the equivalence/docs/
`database.ts` guards (T004b). Those were corrected in place because a developer running the
originals would ship an RLS bypass and a build failure.

**⚠️ T045–T047 are blocking and must be answered before US1 starts** — each is a product decision
the spec cannot make for itself, and screens are already tasked to render the results.

- [ ] T045 [SA] **Decide cost basis.** C3's `INVESTED` and `GAIN` stats have no source column
      anywhere: `finance.holdings` carries no invested amount and C4's form captures none, yet
      FR-006a is written as "for an asset holding with a known invested amount" and T023 builds the
      stat. Either (a) add `invested_paise bigint null` to
      `supabase/schemas/finance/10_tables/holdings.sql` plus a capture field in C4 and an FR, or
      (b) formally drop `INVESTED`/`GAIN`/XIRR from C3 and adjust its copy. Record the choice in
      `spec.md` Assumptions; Phase 5's `report_investment_returns` (005 R8) depends on the answer
- [ ] T046 [SA] **Decide the net-worth history source.** FR-010 mandates a `▲/▼ %` delta and an area
      sparkline on Home (01) and a delta in C1's donut centre, and C2 requires a per-holding
      sparkline plus `% change`. This phase defines only current-state views — there is no
      historical series anywhere, and "delta vs when" is undefined. Either author a
      `v_net_worth_history` (or equivalent) view in T004's declarative set with an explicit
      comparison window, or defer the trend to Phase 5's `report_balance_sheet(p_as_of)` and change
      the affected screens' copy. T017 currently drops C2's sparkline, last-updated date and
      `% change` **with no deferral recorded** — whichever way this goes, record it
- [ ] T047 [SA] **Decide `liabilities_meta`'s Postgres schema.** `data-model.md:44` declares
      `public.liabilities_meta` while `holdings`/`valuations` in the same file are `finance.*`.
      Under ADR-0033 a `public` table is unreachable through the `Accept-Profile: finance` header
      002 mandates. 002 and 003 each raise this as an unresolved carry-over and 005 silently assumes
      `finance.`. Fix the data model and T004's declarative file to agree

- [ ] T048 [SA] **Freeze the enum value lists in `data-model.md`.** The 10 `sector` values
      (`BANK`, `MUTUAL_FUND`, `STOCKS`, `PROPERTY`, `GOLD`, `EPF_PPF`, `CASH`, `VEHICLE`, `CRYPTO`,
      `OTHER`) exist only in the functional spec's prose, yet T011 tests rejection against "the fixed
      list" and BR-C3 makes them append-only forever. Do the same for `valuations.source`, currently
      documented as "**e.g.** `MANUAL`, `STATEMENT`" — Phase 5's `has_self_valued` needs the exact
      partition of that set. (`liability_type` is already frozen in the same file — match it)
- [ ] T049 [SA] Add a **`NavTarget` additions** section to `contracts/routes.md`. This is the only
      phase contract without one, yet 003 and 006 both cite `OpenHolding`/`OpenLiability` as "added
      by Phase 2" — the cases currently exist only inside T034's task line, breaking the registry's
      sealed-case-plus-registry-row pairing rule. Include the **Home → Currency** quick action:
      `NavTarget.kt:20-24` states Currency is deliberately not a NavTarget, but T036 and QA row
      `HOM-UI-002` both require all four quick actions to route "via `NavTarget`" — add the case or
      specify the alternative mechanism
- [ ] T050 [SA] Declare **C3 as a dark-hero surface** in `contracts/routes.md` and have T023 read
      `DhruvBrand.*`. Functional spec D-2 and implementation plan §3.1 name C3 theme-invariant;
      003 declares E5/E9 and 005 declares F3, but this spec never mentions dark hero or `DhruvBrand`,
      so C3 would ship on the flipping palette

- [ ] T051 [P] [Android] **RED** tests for holding **edit** and **soft-delete** —
      `HoldingRepositoryTest` + `AddEditHoldingViewModelTest`. Neither path is specified today: no
      FR, no task, no QA row and no RLS DELETE policy exist, so a mistakenly-entered holding can
      only be removed by full-account erasure, and C4 is titled "Add / **edit** holding" while T018
      builds the UI with nothing specifying its behaviour
- [ ] T052 [Backend] GREEN for T051 — add the edit path and a `deleted_at` soft-delete to
      `HoldingRepository`, and the matching RLS `UPDATE`/soft-delete policy to T004's declarative
      table file. Valuations stay append-only and untouched (BR-C1)
- [ ] T053 [Android] Wire `UndoSnackbarHost` (already built in `:libs:core` §5.1) to the holding
      soft-delete, per DESIGN-SYSTEM §8's binding soft-delete + 5s undo + recoverable-location rule.
      **Undo is currently specified in none of the six phases** — this is the first implementation,
      so keep the pattern reusable rather than local to this screen. The "recoverable location"
      (Trash) is unowned; see the register's §1 — record here whichever owner is chosen
- [ ] T054 [SA] Write FRs for the fields the design shows and this spec omits: C4's **as-of date**
      and **optional notes** (present in `data-model.md` and tasks but in no requirement), and C7's
      **collateral**, **linked account** and **payment history** (same gap). C2's **search**,
      **filter chips**, **last-updated date** and **sector grouping** likewise have no FR — the word
      "search" does not appear in this spec, yet 006 later assumes C2's asset search already exists

- [ ] T055 [SA] **Decide C7 "Record payment".** The design requires a *Record payment* action and a
      "recent payments with principal/interest split" list; no payments table exists in any phase and
      002 never links a transaction to a liability. Either add the table here, route it to Phase 3's
      ledger with a liability link, or descope both with a recorded reason. Related: T028 asserts the
      **amortisation split** "sums to total obligation" against a derivation stated nowhere — the
      original principal is not stored, and C6's "outstanding, not original" rule means outstanding
      comes from the latest valuation, a different quantity than the amortisation schedule implies.
      Define the computation or drop the donut
- [ ] T056 [SA] Resolve **C3 "Link to goal"** (T023). Goals and `goal_links` do not exist until
      Phase 4, and 003 specifies linking only from the E5 side (FR-023). Unlike the credit-card-bill
      and budget-impact deferrals, this forward dependency is flagged nowhere — either hide the
      action behind the `goals` flag or move it to Phase 4 with a reciprocal task

- [ ] T057 [SA] Add **loading, error, empty and not-configured** state requirements. FR-011 is this
      spec's only state requirement and covers signed-out + offline only; "empty" appears once, as a
      C1 Edge Case, not as an FR. DESIGN-SYSTEM §7 makes all eight states binding per screen, and
      002 FR-032 / 003 FR-048 both mandate five. Cover all 8 screens
- [ ] T058 [QA] Add the missing **RED test tasks** so the RED→GREEN gate (implementation plan §7.0,
      constitution Article I) actually holds: T016/T017/T018 (C1/C2/C4), T023 (C3), T027 (C5 live
      delta, `NW-UI-003`), T031 (`NW-UI-004`), T036, and the state-card tasks T020/T037
      (`NW-UI-005`, `HOM-FLOW-001`) all currently have no preceding failing test
- [ ] T059 [Android] Ship this module's **`SettingsContribution`** per
      `../004-settings/contracts/settings-contribution.md`. 004 declares "every later phase ships its
      module's settings entry with the module"; this phase plans none, and Phase 6's value-update-
      overdue alert control has no home without it
- [ ] T060 [P] **De-duplicate `PaceRing`.** T006 places it in `ui/components/charts/`; 003 T043
      places it in `ui/components/Rings.kt` and calls it "genuinely new (verified absent by symbol
      search)". Agree one path with 003 before either lands
- [ ] T061 [QA] Backfill **FR ids into task descriptions**. This spec cites 1 FR across 13 tasks
      (005 cites 46 of 51); tasks reference QA-catalog rows and user stories instead, so an FR with
      no catalog row — FR-009's prepay projection, for one — has no verifiable owner

---

## Phase 10: Gap remediation, round 2 (UI/UX + requirements audit, 2026-08-22)

**Source**: the same register, second pass — UI/UX fidelity to the finalized design, design-system
enforcement, and a requirements re-review. This phase was the weakest of the six on both axes:
1 of 8 screens matches the design as drawn, and it has no accessibility, `strings.xml`, motion or
observability task at all.

**⚠️ T062–T063 are correctness blockers.** Both are asserted as working in three documents each and
are impossible against the schema as committed.

- [ ] T062 [SA] **FR-004's correction path cannot work.** FR-004 requires hiding a wrong valuation;
      the only mechanism is setting `deleted_at`, which is an UPDATE — and
      `supabase/schemas/finance/10_tables/valuations.sql` has **SELECT and INSERT policies only**,
      with `grant select, insert` and an explicit comment "Deliberately no UPDATE policy … and no
      DELETE policy". `data-model.md` states that absence as the guarantee, T026 cites it as
      *enforcement*, and T025 asserts a test that will fail at RLS. ADR-0029 decision 4 already named
      the fix — a security-definer **`correct_valuation()` RPC** — and assigned it to "Phase 2's SA
      step"; no task creates it. Build it, or drop FR-004. **Do not add an UPDATE policy** — that
      destroys BR-C1's database-level append-only guarantee, which is the whole point of the table
- [ ] T063 [SA] **FR-002's atomicity is not achievable as specified.** "Holding + first valuation
      written atomically" is two PostgREST inserts over HTTP; `data-model.md:38-40` concedes it is
      "not expressible as a single-table constraint" and pushes it to "the repository layer either
      writes both or neither", which cannot be transactional across two requests. There is no RPC and
      no compensating delete — and `holdings` has no client DELETE policy — so a failed second insert
      leaves an orphan holding, violating FR-002's own invariant. Add a `create_holding_with_value()`
      RPC (one transaction, server-side) or specify the compensating path explicitly
- [ ] T064 [SA] **Guard future-dated valuations, and state C4/C5 validation.** `v_latest_valuation`
      orders `as_of DESC`, so a mistyped 2030 date becomes permanently "latest" and can never be
      superseded — and cannot be corrected until T062 lands. No FR, no repository rule and no CHECK
      exists. While there, state the field rules C4 and C5 have none of: required vs optional,
      min/max, zero, negative, and the future-date rule
- [ ] T065 [SA] **Close the intra-phase drift the round-1 edit introduced.** T004/T004a now author
      `supabase/schemas/finance/10_tables/liabilities_meta.sql` and grant on `finance.liabilities_meta`,
      while `data-model.md:44` still says `public.liabilities_meta` and `plan.md`/`quickstart.md`
      still name a hand-written `0002_networth_phase2.sql` that `db diff` will not produce. Land
      T047's decision across all four files, not just tasks.md

- [ ] T066 [Android] **Use `MoneyText` — it appears in zero tasks in this phase.** It is THE money
      renderer (DESIGN-SYSTEM §5.1), tabular numerals, and the design specifies **compact on cards**
      (`₹18.42L` on the Home hero, C1's centre) and **full in lists, sheets and history**. No task
      plans the compact/full split; money must never ellipsise
- [ ] T067 [Android] **Use `StatDeltaChip` and `ThreeUpStatRow`** — both already built in `:libs:core`
      and named in **zero** tasks across all six phases. They own every ▲/▼ delta (01, C1, C2, C3) and
      every three-stat header (C3's INVESTED·GAIN·XIRR, C6's TOTAL OUTSTANDING·MONTHLY OUTGO·DEBT-FREE
      BY). Hand-rolling them breaks both the micro-frontend rule and §1's never-colour-only rule
- [ ] T068 [Android] **Add a `strings.xml` task.** DESIGN-SYSTEM §10: "All user-visible strings land
      in `strings.xml` from birth." This phase has no such task; 003, 004, 005 and 006 all do
- [ ] T069 [Android] **Add the accessibility task this phase entirely lacks** (§9 is a gate, not an
      aspiration): `contentDescription` on every icon-only action and on **every chart, ring and
      sparkline this phase builds** — T006's `DonutChart`, `AmortisationDonut` and `PaceRing`, C3's
      trend chart, C2's per-holding sparklines — at the design's stated verbosity ("Net worth, ₹18.42
      lakh, up 6.4 percent this month"); touch targets ≥48dp and list rows ≥56dp; contrast ≥4.5:1 in
      **both** themes; no colour-only meaning; dynamic-type safety with money wrapping or compacting
      rather than ellipsising; TalkBack order following visual hierarchy
- [ ] T070 [Android] **Wrap every screen in `FeatureHost`** — only C1 is wrapped today (1 of 8), and
      NFR-2/PLATFORM.md §4 require every route. Add the per-ViewModel observability triad the repo
      convention mandates and this phase omits entirely: `crashReporter.setModule("networth")`,
      `performanceTracer.trace("networth_…")` on one primary operation, and a `featureError`
      StateFlow fed by a `CoroutineExceptionHandler`
- [ ] T071 [QA] **Verify light and dark render from the same tokens** (nav law N7) and check the
      three responsive tiers via `calculateDhruvNextResponsiveTokens` — phone, tablet ≥600dp, small
      <360dp. **No phase in the entire feature mentions responsiveness at all**; theme verification is
      planned only in 004 and partially 003
- [ ] T072 [P] **Add the token-enforcement rule the whole feature assumes and nothing provides.**
      `config/detekt/detekt.yml:34-35` sets `MagicNumber: active: false`, there is no
      `ForbiddenImport`/`ForbiddenMethodCall` rule, and `DependencyRulesTest` has five rules, none
      about tokens — so "zero `MaterialTheme.colorScheme`/`.typography` and zero raw hex/dp/sp in
      screen files" is enforced by nothing. 005 T169 says it verifies NFR-5 "by review and detekt",
      against a check that cannot fire. Add a detekt rule or an ArchUnit test here, in the first phase
      that builds tracker screens, so every later phase inherits it
- [ ] T073 [Android] **Motion (§8, NFR-7) has zero coverage in any of the six phases** — no FR, task,
      QA row or constant, and the catalog's own NFR-007 row is marked "Partial" and cited by nobody.
      This phase builds four animating surfaces; state the standard easing `cubic-bezier(.16,1,.3,1)`
      and the "charts animate in once, not on every recomposition" rule for them

- [ ] T074 [Android] **Close the per-screen fidelity gaps against the design as drawn**: C3's header
      is missing `LAST VALUED <date>` and the sector, and its 3M/6M/1Y/All range chips name no
      component while `PeriodChipRow` exists (005 T045 uses it); C6's rows are missing **rate** and
      **EMI**; 01 is missing the one-line state ("everything on track"); C1's legend is missing the
      **enum tag**; C7's prepay projection needs the §10 derived-output label that 003 gives its
      equivalents (FR-047 + T032) and this phase does not; C4 substitutes `SelectionSheet` (B9) for
      the design's **`EnumPickerGrid`** (B2) — pick one deliberately; the design draws **area** charts
      on 01, C2 and C3 and `:libs:core` has only `TrendSparkline`/`BarChart` — decide before a screen
      hand-rolls one. Drop `PieChart` from T006 unless a consumer exists (no screen in any phase uses it)
- [ ] T075 [SA] **Specify the post-write invalidation model.** SC-001 promises totals update "without
      a manual refresh" and DESIGN-SYSTEM §8 forbids pull-to-refresh, but no phase states how a write
      invalidates the server-side views it feeds, whether reads re-poll, or what the screen shows
      between write-ack and re-read. This is a day-one blocker for every mutating screen in 001–003
- [ ] T076 [QA] **Cite SC ids in tasks.** This phase cites **0 of 5**; SC-003 ("0% of sessions" — a
      production-telemetry metric with no telemetry planned anywhere) and SC-004 ("under 2 minutes")
      are also unmeasurable as written. Either give them an instrument and a fixture, or restate them
- [ ] T077 [SA] **Specify concurrency behaviour.** ADR-0014 removed all client-side conflict
      resolution, and this phase specifies nothing for two devices editing the same holding, a stale
      read, or last-write-wins. 002 solves its one race with an idempotency key and 003 with a
      trigger; this phase names none. Related and unowned feature-wide: **no phase specifies
      write-retry semantics** — no idempotency key on manual creates, no client request id, so a
      retry after a timeout silently duplicates a holding or a valuation

---

## Phase 11: DB readiness — execute, verify, and make the guard usable (2026-08-23)

**Context.** The SA schema step for this phase is **authored** — see `data-model.md` § "DB readiness"
for the artifact list and the
[readiness architecture decisions](../../docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md)
for why each object exists. What is authored is not what is *verified*: the migration has never
run, and one CI guard cannot currently pass for a structural reason. These tasks close that.

**T004/T004a/T004b's scope is now narrower**: the declarative files, the migration, the grants, the
`delete_my_data()` extension and the regenerated `SCHEMA.md` all exist on disk. Those tasks become
*review and regenerate*, not *author from scratch*.

- [ ] T078 [Backend] **Execute the migration for the first time and verify it.** It is
      **hand-authored, not `supabase db diff`-generated** (the CLI + Docker are not installed on the
      authoring machine — the same situation ADR-0033's own migration records). It has therefore
      never been run. Either `supabase db reset` locally, or let `supabase-migrate.yml`'s `apply-dev`
      job run it on the `develop` push — that execution is the first real confirmation it is
      correct. **If the CLI is available, prefer regenerating it**: edit nothing, run
      `supabase db diff -f networth_phase2`, and compare against the hand-authored file; the
      declarative files under `supabase/schemas/finance/` are the source of truth either way.
      Watch specifically for: the `security_invoker` clause surviving on all three views, the
      `as_of <= current_date` CHECK being accepted (`current_date` is STABLE, not IMMUTABLE — see
      data-model), and `alter table … add constraint` succeeding against any rows already in
      `dhruv-dev`
- [x] T079 [P] [Backend] **DONE 2026-08-23.** Taught `scripts/db/gen_schema_docs.py` about `ALTER TABLE … ADD COLUMN`
      and `ADD CONSTRAINT`.** The ADR-0032 equivalence guard is RED today and **cannot go green
      without this** — it reports `finance.holdings` and `finance.valuations` signatures differing
      when the schema is in fact consistent. Cause: the parser has rules for `create table`,
      `create index`, `enable row level security`, `set schema`, `create policy` and
      `create function`, and **none for adding a column to an existing table**, so a column
      introduced by a migration is invisible on the executed side. Every later phase that extends an
      existing table hits this identically. ADR-0033 set the precedent by teaching this same guard
      `ALTER TABLE … SET SCHEMA` for exactly this class of reason. Consider whether table signature
      comparison should be **order-insensitive** on columns while you are there — a `db diff`-
      generated migration appends columns at the end, so declarative order and executed order will
      routinely differ without meaning anything
- [x] T080 [P] [Backend] **DONE 2026-08-23.** Fixed `gen_schema_docs.py`'s Windows crash. It writes `SCHEMA.md`
      successfully and then dies printing a `✅` under cp1252
      (`UnicodeEncodeError: 'charmap' codec can't encode character '✅'`), so the work completes
      but the exit code reports failure — which reads as a broken guard to anyone running it
      locally. Either reconfigure stdout to UTF-8 inside the script or drop the emoji from its
      output. Workaround until then: `PYTHONIOENCODING=utf-8`
- [ ] T081 [Sec] **Extend the RLS test to cover views and the two RPCs**, not just tables. Assert a
      second user reads **zero rows** from `v_latest_valuation`, `v_net_worth_by_sector` and
      `v_net_worth_history` — this is the test that would have caught the missing
      `security_invoker` — and that `correct_valuation()` and `create_holding_with_value()` both
      reject a caller who does not own the target holding. Both are `security definer`, so they
      bypass the RLS that would otherwise refuse them, and their explicit ownership checks are the
      only thing standing in
- [ ] T082 [Backend] **Test `correct_valuation()` end to end**: the corrected row appears with
      `source = 'CORRECTION'`, the original is excluded from `v_latest_valuation`, both happen in one
      transaction, and a second correction of an already-corrected row is refused. This replaces
      T025/T026's assertions, which were written against a client-side `deleted_at` UPDATE that RLS
      forbids
- [ ] T083 [Backend] **Test `create_holding_with_value()` idempotency**: the same `p_request_id`
      replayed returns the original holding id and creates no second holding or valuation
- [ ] T084 [SA] **Confirm the two reversible schema choices before the migration touches
      `dhruv-dev`** — cheap now, a data migration later: `collateral text` versus a holdings FK, and
      `v_net_worth_history`'s 30-day comparison window versus calendar-month-to-date

---

## Phase 12: Closure — tracking (runs last, after the checkpoint is green)

Per the tracking rule in `apps/finance/CLAUDE.md`. All three move together; a phase is not done
until they do.

- [ ] T085 [P] Move **`networth`'s row in [`apps/finance/FEATURES.md`](../../FEATURES.md)** out of
      the "Planned" table into the shipped Modules table — `Status: enabled`, flag `networth`
      (`requiresConsent: true`), owner tab Home
- [ ] T086 [P] Rewrite **`apps/finance/feature/home/networth/README.md`** — drop the
      "(not yet created)" heading and the "does not exist yet" preamble, and write the real screens
      (C1–C7), ViewModels, repositories consumed, the two RPCs this phase introduced, and the flag
      key. Detail belongs here and **not** back in FEATURES.md
- [ ] T087 [P] Add the **root [`CHANGELOG.md`](../../../../CHANGELOG.md)** entry under the
      `finance-*` heading CI injects. This phase's notable items: the net-worth tracker itself, the
      `finance.correct_valuation()` and `finance.create_holding_with_value()` RPCs, the
      `v_net_worth_history` view, `invested_paise`, the frozen `sector`/`source` enums, and —
      because they are behaviour changes a reader would otherwise be surprised by —
      `security_invoker` on every view and the new no-future-date constraint on `valuations.as_of`
- [ ] T088 [P] Update the **spec-kit tracking table** in
      `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7:
      Phase 2's status moves to *shipped*, and its "NOT ready for `/speckit-implement`" note is
      removed once T045–T047 and T078–T084 are closed
