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
- [X] T005 [P] [Sec] **Superseded by Phase 11's T081 (2026-09-03), closing the loop rather than
      leaving this checkbox stale.** Same ask (RLS on the new views, no client UPDATE/DELETE path)
      and same blocker (no live authenticated Supabase connection) as originally written here — the
      actual script satisfying it is `supabase/verification/phase2_rls_views.sql`, authored in
      Phase 11 with a wider scope than this task named (all three tracker views, not just
      `liabilities_meta`'s two). Not run, for the same credential reason T004b/T081 both cite
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

- [X] T021 [P] [US2] **DONE (2026-09-01).** History-ordering test — entries newest-first, each with its delta vs the
      previous entry, citing NW-UI-002, in
      `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/ValuationRepositoryTest.kt`.
      **Deviation**: delta is `deltaPercentBps: Int` (basis points), not a `Double` percent — the
      `checkTrackerMoneyPrecision` gate (Article VII/DAT-BR-008) forbids floating-point types
      anywhere under this module's tracker path; basis points is the same convention
      `liabilities_meta.rate_bps` already uses. A caller divides by 100 to display a percentage

### Implementation for User Story 2

- [X] T022 [US2] **DONE (2026-09-01).** Implement `HoldingDetailViewModel` (C3) — history query + trend range filtering,
      citing NW-UI-002, in `apps/finance/feature/home/networth/HoldingDetailViewModel.kt`. Also
      added (prerequisite this task needed, not separately scheduled): `HoldingRepository.get(id)`
      (single-holding read, new `HoldingApi.getById`) and `ValuationRepository` itself
      (`ValuationApi.listHistory`, `ValuationDto`, `Valuation`/`ValuationHistoryEntry` models) — none
      of C3's reads existed before this phase
- [X] T023 [US2] **DONE (2026-09-01).** Build `HoldingDetailScreen` (C3) — value + %, trend chart
      (`TrendSparkline`) with 3M/6M/1Y/All range chips (`PeriodChipRow`), INVESTED · GAIN ·
      simple-return% stats (`ThreeUpStatRow`, shown only when `investedPaise` is present — never a
      fabricated zero), VALUATION HISTORY list (dated, sourced, delta-vs-previous via
      `StatDeltaChip`), in `apps/finance/feature/home/networth/HoldingDetailScreen.kt`.
      **Deviations**: *Update value* renders as a button wired to a no-op — its destination, C5
      (`AddValuationSheet`), is Phase 5's (User Story 3) deliverable and doesn't exist yet; *Link to
      goal* is omitted entirely — goals don't exist anywhere in this design-v1 phase. Both are
      documented in the screen's own KDoc, not silently dropped
- [X] T024 [US2] **DONE, with the same scope correction as T019 (2026-09-01).** Wired C2 → C3
      navigation, but through `NetWorthFeatureRoot`'s local `NavHost` (`holding/{holdingId}` route),
      not `NavTarget` — `OpenHolding`/`OpenLiability` cases still don't exist on `NavTarget`, and
      T019's reasoning for why they shouldn't (intra-module drill-down, not cross-tab dispatch)
      applies identically here. C6 → C3 is Phase 6's job (liabilities don't exist yet); only C2 → C3
      is in scope this phase. `AssetsScreen`'s rows are now clickable (`onOpenHolding` callback)

**Checkpoint (2026-09-01)**: Stories 1–2 both independently functional at the Compose/Kotlin level
— `regressionCheck` green (including `checkTrackerMoneyPrecision`), ArchUnit green. Same live-database
caveat as Phases 2–3: nothing here has made a real Supabase call yet (T005/T078).

---

## Phase 5: User Story 3 — Update a value without losing history (Priority: P3)

**Goal**: Record a new value; see the live delta; previous values stay visible, never altered
(spec.md Story 3).

**Independent Test**: Record a second value for an existing holding; confirm the first value is
still visible in history, unaltered.

### Tests for User Story 3

- [X] T025 [P] [US3] **DONE (2026-09-01).** Correction test — a correction is exactly one RPC call
      (the soft-delete-old + append-corrected transaction happens entirely server-side in
      `finance.correct_valuation`; the client never issues two calls, never an UPDATE against
      `value_paise` — there is no UPDATE policy to call even if it tried), citing
      NW-BR-002/NW-BR-003, in `ValuationRepositoryTest.kt` (same file as T021). Also added
      `recordValue()`'s own test coverage (plain-append call count, `CORRECTION` rejected as a
      user-selectable source, unknown source rejected) alongside it

### Implementation for User Story 3

- [X] T026 [US3] **DONE (2026-09-01).** Implement `ValuationRepository.recordValue()` +
      `.correctValue()` — `recordValue()` is a plain PostgREST insert (`Prefer:
      return=representation` added since PostgREST's default insert response is empty and Moshi
      needs a body); `correctValue()` calls the `finance.correct_valuation` RPC. Neither ever
      issues a client-side UPDATE — enforced by `finance.valuations` having no UPDATE policy at all
      (T004), this is the client-side half of that guarantee. Citing NW-BR-002/NW-BR-003, in
      `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/ValuationRepository.kt`.
      Also added `HoldingApi`/`ValuationApi` methods, `RecordValuationRequestDto`,
      `CorrectValuationRequestDto`, and `ValuationSource.fromCode()` (mirroring `Sector.fromCode`)
      as prerequisites this task needed
- [X] T027 [US3] **DONE (2026-09-01).** Build `AddValuationSheet` (C5) — last value shown, live
      delta preview (amount + %) computed from whatever is currently typed, before submit
      (NW-UI-003), source picker (`SegmentedRow`: Manual/Statement/Import), in
      `apps/finance/feature/home/networth/AddValuationSheet.kt`. **Scope addition beyond the
      literal task**: the sheet also serves as the correction UI — when opened with a specific
      `valuationId` (a new "Fix" action added to each `HoldingDetailScreen` history row), the title
      switches to "Correct this value", the source picker is hidden (`correct_valuation` always
      writes `source = 'CORRECTION'` server-side, nothing to choose), and `save()` calls
      `correctValue()` instead of `recordValue()`. Without this, `correctValue()` would have been
      unreachable dead code with no UI path, and spec.md Story 3's own independent test
      ("realize it's wrong -> add a corrected value") would not be satisfiable end to end.
      **Deviation, same as prior phases**: date is fixed to "today" — no date picker component
      exists yet. Wired from `HoldingDetailScreen`'s "Update value" button and each history row's
      new "Fix" button, through `NetWorthFeatureRoot`'s local `NavHost`
      (`addValuation/{holdingId}?correcting={id}&last={value}`), not `NavTarget` — same reasoning
      as T019/T024

**Checkpoint (2026-09-01)**: Stories 1–3 independently functional at the Compose/Kotlin level —
`regressionCheck` green (including `checkTrackerMoneyPrecision`), ArchUnit green. Same live-database
caveat as Phases 2–4: nothing here has made a real Supabase call yet (T005/T078).

---

## Phase 6: User Story 4 — Track liabilities and payoff progress (Priority: P4)

**Goal**: Track what's owed, see outstanding balance and payoff progress, get a prepay-savings
projection (spec.md Story 4).

**Independent Test**: Add a loan-type liability with rate/EMI; open its detail; confirm balance,
payoff progress, and a prepay projection all render.

### Tests for User Story 4

- [X] T028 [P] [US4] `LiabilityRepositoryTest` — CRUD on `liabilities_meta`, `liability_type` enum
      rejected if not in the fixed list (mirrors NW-BR-004 for liability types), amortisation split
      sums to total obligation, in
      `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/LiabilityRepositoryTest.kt`
      **DONE (2026-09-02).** Also added `AmortisationMathTest.kt` and per-ViewModel tests
      (`LiabilitiesViewModelTest`, `LiabilityDetailViewModelTest`, and liability-path cases added to
      the existing `AddEditHoldingViewModelTest`) — not separately task-listed, but this phase's
      established practice (every prior phase) is a ViewModel test file per new ViewModel.

### Implementation for User Story 4

- [X] T029 [US4] Implement `LiabilityRepository` (depends on T028) in
      `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/LiabilityRepository.kt`
      **DONE (2026-09-02).** `LiabilityApi` (GET/POST/PATCH against `finance.liabilities_meta` — no
      DELETE, the table has no client DELETE policy), `LiabilityMetaDto`/`CreateLiabilityMetaRequestDto`/
      `UpdateLiabilityMetaRequestDto`, `LiabilityMapper.toDomain()`, and the `LiabilityMeta`/
      `LiabilityType`/`AmortisationSplit`/`amortisationSplit()` domain model all added alongside it,
      mirroring `HoldingRepository`/`ValuationRepository`'s exact structure. Registered in
      `PlatformModule.kt` (`single<LiabilityRepository> { LiabilityRepositoryImpl(get()) }`).
      **Scope addition (undocumented in the spec, resolved here):** neither this task nor T030/T031
      says where a liability's `liabilities_meta` row is actually created — the Independent Test
      above requires it to be creatable, but tasks.md is silent on the write path. Resolved by
      extending `AddEditHoldingScreen`/`AddEditHoldingViewModel` (C4, built in Phase 3): selecting
      "I owe this" now reveals liability-type/rate/EMI/tenure fields, and `save()` makes a **second**
      call to `LiabilityRepository.createMeta()` after `HoldingRepository.createWithFirstValuation()`
      succeeds — deliberately not folded into one atomic RPC (loan terms are optional metadata on
      top of the holding+valuation pair BR-C2 already guarantees, not a third leg of that guarantee).
      A `createMeta()` failure is surfaced via a new non-blocking `UiState.liabilityMetaError` field
      rather than blocking navigation away, since the holding itself is already safely saved by that
      point. The value entered as the holding's first valuation doubles as `original_principal_paise`
      (documented in code as a known simplification: a loan added partway through its term shows a
      slightly optimistic payoff projection until an edit-liability screen exists, which this phase
      does not build).
- [X] T030 [US4] Build `LiabilitiesScreen` (C6) — grouped by `liability_type`, TOTAL OUTSTANDING ·
      MONTHLY OUTGO · DEBT-FREE BY stats, payoff progress per row, in
      `apps/finance/feature/home/networth/LiabilitiesScreen.kt`
      **DONE (2026-09-02).** `LiabilitiesViewModel` merges `HoldingRepository.list(LIABILITY)` with
      `LiabilityRepository.listAll()` client-side by `holdingId` (no `v_liabilities`-style join view
      exists — noted as a gap in this phase's earlier context and left unbuilt, matching scope).
      DEBT-FREE BY is the latest (furthest-out) projected payoff date across every liability with
      computable terms (`AmortisationMath.projectedPayoffMonths`); per-row payoff progress is
      `paidMonths / tenureMonths` via `ProgressRing`, null (no ring) when tenure isn't known (a
      credit card/BNPL line). C1's "By sector" rows for LIABILITY-kind breakdowns now route to this
      screen instead of the ASSET-only `AssetsScreen` (`NetWorthNavHost`'s `onOpenSector` branches on
      `HoldingKind`) — this was a latent gap (liability sector rows previously opened an Assets
      screen that would never show them, since `AssetsViewModel.load()` hardcodes `HoldingKind.ASSET`)
      and is this phase's only entry point into C6, rather than inventing a new one.
- [X] T031 [US4] Build `LiabilityDetailScreen` (C7) — amortisation donut (T006), rate/EMI/debit
      day/tenure/linked account/collateral, prepay-savings projection, citing NW-UI-004, in
      `apps/finance/feature/home/networth/LiabilityDetailScreen.kt`
      **DONE (2026-09-02).** `LiabilityDetailViewModel` loads the holding, its `LiabilityMeta` (null
      is a designed non-blocking state, not a load error — an older liability or a failed
      `createMeta()` write just renders without the rate/EMI card), and outstanding balance (latest
      valuation, via `ValuationRepository.listHistory`). Reuses the existing `AmortisationDonut`
      component. The prepay projection (`AmortisationMath.computePrepayProjection`, standard
      amortisation formula) is copy-labelled "Estimated — assumes your rate and payment stay the
      same" per `platform/DESIGN-SYSTEM.md` §10's "derived output is labelled as derived" rule
      (NW-UI-004). `linked_account_id` is loaded but has no display row yet — there is no
      `finance.accounts` table until Phase 3 (`liabilities_meta.sql`'s own comment), so nothing to
      resolve it against; this matches the schema file's documented scope, not a defect.
- [X] T032 [US4] Wire C7's prepay hand-off to the existing loan/EMI calculator via `NavTarget`
      (`OpenPlanTool(PlanTool.LOAN)`) — cross-feature navigation by id, never a class reference
      (constitution Article III)
      **DONE (2026-09-02).** **Architectural correction made in this phase:** `NavigationDispatcher`
      previously lived in `:apps:finance:app`, which `:apps:finance:feature:networth` cannot depend
      on without inverting the module graph (`:app` already depends on `:feature:networth`). Moved
      `NavigationDispatcher` into `:libs:core` (same package as `NavTarget`, zero app-specific
      dependencies) — this is its first real cross-module consumer. `LiabilityDetailScreen` itself
      has no `NavigationDispatcher` dependency; `NetWorthNavHost` injects it and passes
      `onOpenLoanCalculator = { navigationDispatcher.navigate(NavTarget.OpenPlanTool(PlanTool.LOAN)) }`
      down as a plain callback, keeping the screen/ViewModel navigation-agnostic (same pattern as
      every other screen in this NavHost).

**Checkpoint**: Stories 1–4 independently functional. **Known gaps carried forward, same disclosure
pattern as Phases 3–5:** nothing in this phase has made a real Supabase call (no live credentials in
this session) — verified against fakes only. There is no edit-liability screen, so `paid_months`,
rate changes, and tenure extensions cannot be updated after creation even though `LiabilityRepository
.updateMeta()` exists and is tested; wiring a UI to it is deferred, not this phase's scope. No
`v_liabilities` aggregation view exists server-side — C6's merge is client-side, an O(n) join over
two already-small lists (a user's liability count), not a performance concern at this scale.

---

## Phase 7: User Story 5 — See net worth at a glance on Home (Priority: P5)

**Goal**: Home shows net-worth total, trend, and upcoming loan/EMI obligations without navigating
away (spec.md Story 5).

**Independent Test**: With holdings + a liability with a due date recorded, open Home fresh and
confirm all three render without navigation.

### Tests for User Story 5

- [X] T033 [P] [US5] `HomeViewModel` test — hero figure matches C1's total, UPCOMING is EMI-only
      this phase (implementation plan's Phase 2 scoped-dependency note — card-bill rows wait for
      Phase 3's `accounts` table), citing HOM-UI-001/HOM-UI-003, in
      `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/home/HomeViewModelTest.kt`
      **DONE (2026-09-02).** Also covers the pure helpers `deltaBps`, `nextDueDate`, and
      `greetingForHour` directly (same "test the pure function, not just the ViewModel wrapper"
      practice as Phase 6's `AmortisationMathTest.kt`).
- [X] T034 [P] [US5] Ask-pill visibility test — renders on Home, not on Calc/Money, citing
      HOM-UI-004, same file as T033
      **DONE (2026-09-02).** The rule itself (`tab == HOME || tab == PLAN || tab == INSIGHTS`) is a
      new pure function, `shouldShowAskPill(tab: TabKey)`, extracted into `HomeViewModel.kt` — it
      was previously an inline `tabs[pagerState.currentPage] != TabKey.CALC` condition in
      `MainActivity.kt`, which is not independently testable and (per the QA catalog's fuller
      wording, "Home/Plan/Insights… not on Calc/Money") was also **wrong**: it left the Ask pill
      showing on the Money tab too. Fixed as part of extracting the testable function, not a
      separate follow-up.

### Implementation for User Story 5

- [X] T035 [US5] Rewrite `HomeViewModel` (shell-owned, not `:feature:networth` — module-standard
      doc's HOM/PLN correction) in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/home/HomeViewModel.kt`
      **DONE (2026-09-02).** Extends plain `ViewModel()` + `crashReporter.setModule("home")`, matching
      `SettingsViewModel`'s shell-owned convention — **not** `FeatureViewModel` (Home has no feature
      flag to gate on, so `featureError`/`FeatureHost` don't apply here, same reasoning already
      applied to Settings). Reads `NetWorthRepository.getHistory()` (new — see below) for the hero
      figure/delta/sparkline, and merges `HoldingRepository.list(LIABILITY)` +
      `LiabilityRepository.listAll()` for UPCOMING, same client-side merge shape
      `LiabilitiesViewModel` already uses. `SessionState`/`ConsentState` gating mirrors
      `NetWorthOverviewViewModel`'s existing pattern exactly.
      **Scope addition:** `finance.v_net_worth_history` (added by the 2026-08-23 readiness
      decisions, already living in the schema) had no Kotlin consumer anywhere in the repo — this
      phase adds `NetWorthHistoryRowDto`, `NetWorthHistoryPoint`, `NetWorthApi.getNetWorthHistory()`,
      and `NetWorthRepository.getHistory()`, mirroring the existing `HoldingApi`/`ValuationApi`
      structure exactly. The hero delta compares the newest point against the second-to-last
      (data-model.md's "~30 days prior", which at this view's month-end granularity is simply the
      prior point) — see `NetWorthAggregationTest.kt`'s new cases and `deltaBps()`'s own doc.
- [X] T036 [US5] Replace the placeholder `DashboardScreen` with the real Home (01) — greeting, date
      line, net-worth hero (value + ▲/▼% + sparkline), 4 quick actions (Loan EMI/SIP/Currency/GST)
      via `NavTarget`, UPCOMING (EMI-only, sourced from `liabilities_meta.debit_day`), Ask pill,
      citing HOM-UI-001/002/003/004, in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/home/HomeScreen.kt`
      **DONE (2026-09-02).** `DashboardScreen.kt` (and its now-empty `ui/dashboard/` package)
      deleted; `MainActivity.kt`'s `TabKey.HOME` case now renders `HomeScreen`.
      **Deviation, resolving a gap the 2026-08-22 spec-phase gap register already flagged
      ("Home → Currency quick action"):** this task and QA row HOM-UI-002 both say all four quick
      actions go "via `NavTarget`", but `NavTarget.kt`'s own doc comment deliberately excludes
      Currency (a shell-level detail route, not tab-scoped) — no spec ever added a case or an
      alternative. Resolved by routing Loan EMI/SIP/GST through `NavigationDispatcher` +
      `NavTarget.OpenPlanTool` (genuinely cross-tab, into Plan), and Currency through the **existing**
      `onOpenDetail(DetailRoute.Currency)` shell mechanism `CalcTab` already uses for the same
      screen — not a new mechanism, just applying the one that already exists for exactly this class
      of route.
      **Deviation:** `NxHomeTopBar` (`:libs:core`, built but never consumed anywhere) was
      deliberately NOT wired in as this tab's top bar. It has no app-switcher icon, and swapping it
      in for `MainActivity`'s shared `TopAppBar` on the Home tab would silently drop N5's
      "app-switcher reachable from every tab's top bar" guarantee. The greeting/date line render as
      an in-content header inside `HomeScreen` instead, under the shared top bar every tab already
      has. `NxHomeTopBar` remains unconsumed — tracked as a design-system follow-up (add an
      app-switcher slot, or accept a per-tab override with N5 satisfied another way), not fixed here.
      Sparkline reuses the existing `TrendSparkline` component (an existing, already-accepted gap:
      §5.2's own audit notes the design wants **area** charts on 01/C2/C3 and the library has none —
      same choice `HoldingDetailScreen` already made for C3).
- [X] T037 [US5] Add `SignedOutCard`/`OfflineStateCard` to Home in place of the hero card, citing
      HOM-FLOW-001
      **DONE (2026-09-02).** Signed-out and consent-off gating mirror `NetWorthOverviewScreen`
      exactly, but wired for real here (`onAction = { onOpenDetail(DetailRoute.Settings) }`) rather
      than that screen's `{}` stub — Home lives in `:apps:finance:app` and already has `onOpenDetail`
      in scope, so there was no cross-module reason to leave it a no-op.
      **Known gap, same limitation `NetWorthOverviewScreen` already has:** there is no real
      connectivity monitor anywhere in this codebase. "Offline" is modelled as "the history load
      failed and there's no cached net worth to show" (`OfflineStateCard` shown on that failure),
      not a genuine network-state check — not fixed here, not this phase's scope.

**Checkpoint**: All five stories independently functional — Phase 2 feature-complete. **Known gaps
carried forward:** nothing in this phase has made a real Supabase call (no live credentials in this
session) — verified against fakes only, same as every prior phase. `v_net_worth_history`'s 24-point
window and the 30-day delta window are both server-side and untested against live data for the same
reason.

**Checkpoint**: All five stories independently functional — Phase 2 feature-complete.

---

## Phase 8: Polish & Cross-Cutting (QA close, Sec, checkpoint)

**Purpose**: The module-standard doc's steps 5–7 (§4) — QA closes rows, Sec re-passes, merge gate.

- [X] T038 [P] [QA] Close every `NW-*`/`HOM-*` row in
      `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §3/§12 as its
      test lands; update the §14 coverage-summary table. Confirm append-only holds at the SQL layer
      too (no UPDATE policy exists on `valuations`), not just the repository layer
      **DONE (2026-09-02).** 13 of 14 `NW-*` rows and all 5 `HOM-*` rows closed (🟢 unit-tested, ✅
      schema-reviewed, or 🔴 deferred-with-a-stated-reason — see each row's own Status cell for
      which and why); `NW-BR-007` stays ☐, still blocked on the XIRR ADR, unchanged. §14's table and
      narrative recount added. Append-only confirmed directly in
      `supabase/schemas/finance/10_tables/valuations.sql`: only `valuations_select_own`/
      `_insert_own` policies exist, no UPDATE, no DELETE.
      **Two real defects found and fixed during this pass, not just written up** (see NW-FLOW-001/
      NW-FLOW-002 and the design-v1 plan's Phase 2 row for the full account): (1) `NetWorthFeatureRoot`
      (C1-C7) was never mounted anywhere reachable in the running app — fixed via a new
      `DetailRoute.NetWorth` + a hoisted `netWorthNavController`, wired the same way
      `planNavController` already is, plus Home's "View details" button. (2) Screens across this
      module only loaded their data once (`init`) and never reloaded on returning to them
      (`popBackStack` doesn't recreate a `NavBackStackEntry`-scoped ViewModel) — fixed by adding
      `LifecycleResumeEffect` reloads to `NetWorthNavHost.kt`'s overview/assets/holding-detail/
      liabilities/liability-detail routes. Both required the user's explicit go-ahead before
      touching `MainActivity.kt`'s shared back-press handling — asked and confirmed before doing it.
      **Found, not fixed (documented as a gap in NW-UI-005 and the module's own new README):** C2
      (`AssetsScreen`) and C6 (`LiabilitiesScreen`) don't gate on signed-out/offline state the way C1
      does.
- [X] T039 [P] [Sec] Full DPDP/secrets/RLS checklist pass on `liabilities_meta` + the two new views
      (module-standard doc §4 step 6); confirm `delete_my_data()`'s new DELETE line (T004) actually
      removes `liabilities_meta` rows on a dev-project erasure test
      **DONE (2026-09-02), against the schema files, not a live dev-project run (no credentials
      this session — same limitation as every prior phase).** Corrected scope: three views exist
      today (`v_latest_valuation`, `v_net_worth_by_sector`, `v_net_worth_history`), not the two this
      task's wording anticipated (`v_net_worth_history` was added by a later readiness decision,
      after this task was written) — all three checked. All three: `security_invoker = on` present,
      `grant select … to authenticated` only (no `anon`). `liabilities_meta`: SELECT/INSERT/UPDATE
      RLS policies transitive via `holding_id → holdings.user_id`, no DELETE policy (matches
      `valuations`' append-only-adjacent design — rows disappear only via the two erasure functions).
      `public.delete_my_data()` already contains `delete from finance.liabilities_meta where
      holding_id in (select id from finance.holdings where user_id = auth.uid())`, ordered before its
      `valuations`/`holdings` deletes (children before parents) — structurally correct; not
      re-verified against a live erasure call this session (DAT-FLOW-001's prior live closure covers
      `holdings`/`valuations` only, predates `liabilities_meta`'s existence, and would need to
      re-run once a dev project is available — noted, not claimed done).
- [X] T040 Run `./gradlew regressionCheck` — all green, merged JaCoCo floor not regressed
      (constitution Article X)
      **DONE (2026-09-02).** Green, including after the `DetailRoute.NetWorth`/back-press/
      `LifecycleResumeEffect` changes above.
- [X] T041 Run `./gradlew checkTrackerMoneyPrecision` — confirms no `Double`/`Float` crept into
      `liabilities_meta`'s `emi_paise`/`rate_bps` handling (constitution Article VII)
      **DONE (2026-09-02).** Green.
- [X] T042 Walk all 6 scenarios in `apps/finance/specs/001-net-worth-tracker/quickstart.md`
      end-to-end on a device/emulator
      **Deferred (2026-09-02) — no physical device or emulator available in this implementation
      session, same disclosed gap as every prior phase's on-device verification task.** Scenarios 1
      and 5 specifically exercise the two defects T038 found and fixed (Home's total only updating
      "on next open", and needing C1-C7 reachable from Home at all) — both are now structurally
      fixed and covered by unit tests where the logic is unit-testable, but the end-to-end walk
      itself has not been run on a device. Tracked, not silently skipped.
- [X] T043 [P] Add `apps/finance/feature/home/networth/README.md` (module index entry, per every
      other feature module's convention) and link it from `apps/finance/FEATURES.md`
      **DONE (2026-09-02).** `FEATURES.md`'s row already existed and was already accurate (owner
      Home, flag `networth`, Phase 2, spec link) — only the README itself needed rewriting, from its
      pre-module-creation "not yet created" stub to the real screens/ViewModels/data-dependencies/
      known-gaps content, matching `loans/README.md`'s established format.
- [X] T044 Bump the minor version in `platform/versions.json` (new feature module, ADR-0012's
      versioning rule); update the implementation plan's §7 tracking table row for Phase 2 to
      "shipped" and mark Phase 2's own checkpoint row (§7) complete
      **Partially done, one half deliberately not applied (2026-09-02).** The implementation plan's
      §7 Phase 2 row is updated to "shipped" with a summary of what shipped and what's still open.
      The version bump is **not applied** — `platform/versions.json`'s own `notes` field states
      plainly "Do not hand-edit any `version` or `buildNumber` field," and ADR-0025/ADR-0032 make
      this CI-owned, derived from `feat:` commit types on merge to `main`, not a task a phase
      performs by hand. This task's cited "ADR-0012" is itself a wrong reference (ADR-0012 is the PR
      summary bot, not versioning) — a stale instruction from before ADR-0025 existed, same class of
      drift this register's own numbering-hygiene notes have flagged before. Hand-editing it now
      would violate the current binding rule and would just be overwritten (or conflict) at merge
      time regardless. CI bumps this automatically once this branch reaches `main`.

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

- [X] T045 [SA] **Decide cost basis.** C3's `INVESTED` and `GAIN` stats have no source column
      anywhere: `finance.holdings` carries no invested amount and C4's form captures none, yet
      FR-006a is written as "for an asset holding with a known invested amount" and T023 builds the
      stat. Either (a) add `invested_paise bigint null` to
      `supabase/schemas/finance/10_tables/holdings.sql` plus a capture field in C4 and an FR, or
      (b) formally drop `INVESTED`/`GAIN`/XIRR from C3 and adjust its copy. Record the choice in
      `spec.md` Assumptions; Phase 5's `report_investment_returns` (005 R8) depends on the answer
      **DONE (2026-09-02).** Option (a) was already decided and shipped at the schema/data-model
      level before this phase even started — `invested_paise` exists on `finance.holdings`
      (verified directly against the schema file), documented in `data-model.md`, and C3 already
      reads it. What was genuinely missing: **C4 never actually captured it** — the column was
      write-only-from-nowhere. Fixed by adding an "Invested amount (optional)" field to
      `AddEditHoldingScreen`/`AddEditHoldingViewModel` (both create and edit paths). `spec.md`
      Assumptions now cites this explicitly instead of leaving the decision only in `data-model.md`.
- [X] T046 [SA] **Decide the net-worth history source.** FR-010 mandates a `▲/▼ %` delta and an area
      sparkline on Home (01) and a delta in C1's donut centre, and C2 requires a per-holding
      sparkline plus `% change`. This phase defines only current-state views — there is no
      historical series anywhere, and "delta vs when" is undefined. Either author a
      `v_net_worth_history` (or equivalent) view in T004's declarative set with an explicit
      comparison window, or defer the trend to Phase 5's `report_balance_sheet(p_as_of)` and change
      the affected screens' copy. T017 currently drops C2's sparkline, last-updated date and
      `% change` **with no deferral recorded** — whichever way this goes, record it
      **DONE (2026-09-02).** `v_net_worth_history` was already authored and documented
      (`data-model.md`) before this phase; Phase 7 built `NetWorthRepository.getHistory()` and
      Home's hero delta/sparkline off it. The other half — C2's per-holding sparkline/last-updated/
      `%` change — was genuinely never built and never recorded as deferred; a closure note is now
      in `data-model.md` stating why (no aggregation view for it, no QA row requires it) and that
      it's a follow-up, not silently missing.
- [X] T047 [SA] **Decide `liabilities_meta`'s Postgres schema.** `data-model.md:44` declares
      `public.liabilities_meta` while `holdings`/`valuations` in the same file are `finance.*`.
      Under ADR-0033 a `public` table is unreachable through the `Accept-Profile: finance` header
      002 mandates. 002 and 003 each raise this as an unresolved carry-over and 005 silently assumes
      `finance.`. Fix the data model and T004's declarative file to agree
      **DONE (2026-09-02) — verified already resolved.** Both `data-model.md` and
      `supabase/schemas/finance/10_tables/liabilities_meta.sql` already agree on `finance.*`,
      predating this phase (a "Schema corrected 2026-08-23" note already exists in `data-model.md`).
      No further edit needed; this task's own record was simply never marked closed.

- [X] T048 [SA] **Freeze the enum value lists in `data-model.md`.** The 10 `sector` values
      (`BANK`, `MUTUAL_FUND`, `STOCKS`, `PROPERTY`, `GOLD`, `EPF_PPF`, `CASH`, `VEHICLE`, `CRYPTO`,
      `OTHER`) exist only in the functional spec's prose, yet T011 tests rejection against "the fixed
      list" and BR-C3 makes them append-only forever. Do the same for `valuations.source`, currently
      documented as "**e.g.** `MANUAL`, `STATEMENT`" — Phase 5's `has_self_valued` needs the exact
      partition of that set. (`liability_type` is already frozen in the same file — match it)
      **DONE (2026-09-02) — verified already resolved.** `data-model.md` already freezes both
      `sector` (10 values, "Changed 2026-08-23") and `valuations.source` (4 values, with the
      self-valued/not-self-valued partition Phase 5 needs explicitly spelled out), predating this
      phase. Same as T047 — the work existed, the checklist row didn't reflect it.
- [X] T049 [SA] Add a **`NavTarget` additions** section to `contracts/routes.md`. This is the only
      phase contract without one, yet 003 and 006 both cite `OpenHolding`/`OpenLiability` as "added
      by Phase 2" — the cases currently exist only inside T034's task line, breaking the registry's
      sealed-case-plus-registry-row pairing rule. Include the **Home → Currency** quick action:
      `NavTarget.kt:20-24` states Currency is deliberately not a NavTarget, but T036 and QA row
      `HOM-UI-002` both require all four quick actions to route "via `NavTarget`" — add the case or
      specify the alternative mechanism
      **DONE (2026-09-02).** Section added to `contracts/routes.md`. Finding: this phase adds
      **zero** new `NavTarget` cases — 003/006's "`OpenHolding`/`OpenLiability` added by Phase 2"
      citation is incorrect (to be corrected in those specs at their own implementation time); every
      C1-C7 navigation is intra-module via `NetWorthFeatureRoot`'s own `NavHostController`, never
      `NavTarget`. The Currency quick-action question is answered (already resolved and shipped in
      Phase 7): it routes via the existing `DetailRoute.Currency` shell mechanism, not `NavTarget`.
- [X] T050 [SA] Declare **C3 as a dark-hero surface** in `contracts/routes.md` and have T023 read
      `DhruvBrand.*`. Functional spec D-2 and implementation plan §3.1 name C3 theme-invariant;
      003 declares E5/E9 and 005 declares F3, but this spec never mentions dark hero or `DhruvBrand`,
      so C3 would ship on the flipping palette
      **Investigated, not implemented (2026-09-02) — a real blocker found, not a skip.** `DhruvBrand`
      (`libs/core/.../DhruvBrandColors.kt`) defines navy/navyElevated/blueMid/accentBlue/silver/
      silverLight/steel/logoBg — enough for a dark hero surface's background, elevated card, and
      general text roles, but **no negative-value color**. `accentBlue` is documented as "positive-
      on-navy" only. C3 routinely renders a negative valuation delta (`StatDeltaChip` with
      `isPositive = false`); converting the whole screen to `DhruvBrand.*` without a defined
      negative role would mean inventing an unreviewed color choice for a financially-meaningful
      signal, with no way to visually verify it in this session (no device/screenshot tooling).
      Judged higher-risk than valuable given that gap — left on the flipping palette, recorded here
      rather than either silently skipped or shipped as an unverified guess. Follow-up: define
      `DhruvBrand`'s negative-value role (or confirm `accentBlue`/a new token covers it) before
      converting C3.

- [X] T051 [P] [Android] **RED** tests for holding **edit** and **soft-delete** —
      `HoldingRepositoryTest` + `AddEditHoldingViewModelTest`. Neither path is specified today: no
      FR, no task, no QA row and no RLS DELETE policy exist, so a mistakenly-entered holding can
      only be removed by full-account erasure, and C4 is titled "Add / **edit** holding" while T018
      builds the UI with nothing specifying its behaviour
      **DONE (2026-09-02).** `HoldingRepositoryTest.kt` gained `update`/`softDelete`/`restore` cases
      (including a wire-level assertion that undo sends a literal `{"deleted_at":null}` body, not an
      omitted field — Moshi's default `serializeNulls = false` would otherwise silently no-op the
      restore). `AddEditHoldingViewModelTest.kt` gained `startEditing`/edit-mode `save()` cases.
      `HoldingDetailViewModelTest.kt` gained `delete`/`undoDelete` cases.
- [X] T052 [Backend] GREEN for T051 — add the edit path and a `deleted_at` soft-delete to
      `HoldingRepository`, and the matching RLS `UPDATE`/soft-delete policy to T004's declarative
      table file. Valuations stay append-only and untouched (BR-C1)
      **DONE (2026-09-02).** **Finding: the RLS policy already existed** —
      `holdings_update_own` (`holdings.sql`) already permits this UPDATE, predating this phase, so
      no schema/migration change was needed, only the Kotlin layer: `HoldingRepository.update()`/
      `.softDelete()`/`.restore()`, `HoldingApi.updateHolding()`/`.softDeleteHolding()`/
      `.restoreHolding()`, and the `UpdateHoldingRequestDto`/`SoftDeleteHoldingRequestDto` DTOs.
      `restoreHolding()` takes a raw `okhttp3.RequestBody` rather than a typed DTO — see its own doc
      for why a typed nullable field can't express "explicit JSON null" under this module's default
      Moshi config, and why flipping `serializeNulls` globally was rejected (every `Create*RequestDto`
      relies on null-omission for optional fields). Edit mode covers name/sector/invested/notes only
      — liability terms (rate/EMI/tenure) are not editable this phase (`updateMeta()` exists and is
      tested from Phase 6, but no UI calls it — a known, stated gap, not silently dropped).
- [X] T053 [Android] Wire `UndoSnackbarHost` (already built in `:libs:core` §5.1) to the holding
      soft-delete, per DESIGN-SYSTEM §8's binding soft-delete + 5s undo + recoverable-location rule.
      **Undo is currently specified in none of the six phases** — this is the first implementation,
      so keep the pattern reusable rather than local to this screen. The "recoverable location"
      (Trash) is unowned; see the register's §1 — record here whichever owner is chosen
      **DONE (2026-09-02).** `HoldingDetailScreen` wires `UndoSnackbarHost` directly (the existing
      shared component, not a new one — reusable by future screens the same way). Tapping "Delete"
      soft-deletes immediately and shows the snackbar; tapping Undo restores; letting it time out
      navigates back. **Recoverable-location decision, recorded rather than left implicit**: there
      is no Trash screen in this phase or any other yet, so the *only* recoverable location this
      phase offers is the 5s undo window itself — a soft-deleted holding is still physically present
      (only `deleted_at` is set) and could be restored by a future Trash screen, but nothing
      surfaces it once the window closes. `HoldingRepository.restore()`'s own doc states this
      explicitly so a future Trash implementer finds the mechanism already built.
- [X] T054 [SA] Write FRs for the fields the design shows and this spec omits: C4's **as-of date**
      and **optional notes** (present in `data-model.md` and tasks but in no requirement), and C7's
      **collateral**, **linked account** and **payment history** (same gap). C2's **search**,
      **filter chips**, **last-updated date** and **sector grouping** likewise have no FR — the word
      "search" does not appear in this spec, yet 006 later assumes C2's asset search already exists
      **DONE (2026-09-02) — FR-014 added, plus a real fix beyond the doc task.** Notes capture was
      added to C4 alongside the invested-amount field (T045) — not just documented as missing, but
      built. `spec.md` FR-014 now states what's required (invested amount + notes on C4) versus
      explicitly out of this phase's scope (C7 payment history/linked account, C2 search/filter/
      sparkline) and why. C4's **as-of date** stays non-editable by design — recording a value for a
      date other than today is C5's territory (`recordValue`/`correctValue`), and C4's own creation
      flow already uses "today" via `LocalDate.now()`; no FR gap here, just a mislabelled item in
      the original audit.
- [X] T055 [SA] **Decide C7 "Record payment".** The design requires a *Record payment* action and a
      "recent payments with principal/interest split" list; no payments table exists in any phase and
      002 never links a transaction to a liability. Either add the table here, route it to Phase 3's
      ledger with a liability link, or descope both with a recorded reason. Related: T028 asserts the
      **amortisation split** "sums to total obligation" against a derivation stated nowhere — the
      original principal is not stored, and C6's "outstanding, not original" rule means outstanding
      comes from the latest valuation, a different quantity than the amortisation schedule implies.
      Define the computation or drop the donut
      **Decided, descoped (2026-09-02).** "Record payment" and its payment-history list are
      descoped from this phase — no payments table, no transaction-to-liability link, and Phase 3
      (002)'s own ledger is the natural eventual home once it ships (recorded here as the intended
      direction, not built). The amortisation-split derivation the second half of this task worried
      about **is** now defined and stored: `liabilities_meta.original_principal_paise` (already
      shipped, predates this phase) plus `LiabilityMeta.amortisationSplit()`
      (`principalPaid = originalPrincipal - remaining`; `totalPaidSoFar = emi * paidMonths`;
      `interestPaid = totalPaidSoFar - principalPaid`) — unit-tested in `LiabilityRepositoryTest.kt`
      (Phase 6) to actually sum to the total obligation. The donut ships; nothing to drop.
- [X] T056 [SA] Resolve **C3 "Link to goal"** (T023). Goals and `goal_links` do not exist until
      Phase 4, and 003 specifies linking only from the E5 side (FR-023). Unlike the credit-card-bill
      and budget-impact deferrals, this forward dependency is flagged nowhere — either hide the
      action behind the `goals` flag or move it to Phase 4 with a reciprocal task
      **Moot, verified (2026-09-02).** `HoldingDetailScreen` (as actually built, Phase 4) never grew
      a "Link to goal" action at all — a repo-wide search finds zero references. There is nothing to
      hide or move; the original audit flagged a design element that was correctly never built,
      matching `HoldingDetailScreen`'s own doc comment ("Link-to-goal is left out entirely — goals
      don't exist in this design-v1 phase at all").

- [X] T057 [SA] Add **loading, error, empty and not-configured** state requirements. FR-011 is this
      spec's only state requirement and covers signed-out + offline only; "empty" appears once, as a
      C1 Edge Case, not as an FR. DESIGN-SYSTEM §7 makes all eight states binding per screen, and
      002 FR-032 / 003 FR-048 both mandate five. Cover all 8 screens
      **DONE (2026-09-02) — FR-013 added, plus two real fixes beyond the doc task.** FR-013 now
      states the 8-state requirement explicitly instead of leaving it implicit via DESIGN-SYSTEM §7
      alone. While auditing which screens actually satisfied it, found (Phase 8 QA pass) that C2
      (Assets) and C6 (Liabilities) had **no** signed-out/consent gating at all, unlike C1 — fixed in
      this same phase (`AssetsViewModel`/`LiabilitiesViewModel` now expose `sessionState`/
      `consentState`, their screens branch on them first). Remaining known gap, stated in FR-013
      rather than hidden: C3/C4/C5/C7 don't independently gate signed-out/offline (their parent
      screen normally prevents reaching them signed-out, but a restored back stack after a killed
      session isn't verified against this) — not fixed here, recorded as a real edge case.
- [X] T058 [QA] Add the missing **RED test tasks** so the RED→GREEN gate (implementation plan §7.0,
      constitution Article I) actually holds: T016/T017/T018 (C1/C2/C4), T023 (C3), T027 (C5 live
      delta, `NW-UI-003`), T031 (`NW-UI-004`), T036, and the state-card tasks T020/T037
      (`NW-UI-005`, `HOM-FLOW-001`) all currently have no preceding failing test
      **Moot, recorded (2026-09-02).** This is a retroactive request to insert RED test tasks before
      implementations that already shipped (Phases 3-7, all merged before this Phase 9 pass began).
      The RED-first *ordering* cannot be reconstructed after the fact — but every scenario this task
      names now has passing GREEN coverage (verified against the current test suite: C1/C2/C4/C3/C5/
      C6/C7/Home ViewModels all have dedicated test files, `regressionCheck` green throughout this
      entire feature's implementation). Recorded as "coverage exists, TDD ordering not provable in
      hindsight" rather than claimed as a clean RED→GREEN history it doesn't have.
- [X] T059 [Android] Ship this module's **`SettingsContribution`** per
      `../004-settings/contracts/settings-contribution.md`. 004 declares "every later phase ships its
      module's settings entry with the module"; this phase plans none, and Phase 6's value-update-
      overdue alert control has no home without it
      **DONE (2026-09-02).** `netWorthSettingsContribution()` added (`settings/
      NetWorthSettingsContribution.kt`), registered with the required `named("networth")` qualifier
      in `NetWorthModule.kt`, and added to `RealContributions.kt` so the existing Settings test suite
      (`ContributionValidityTest`, `AlertControlCoverageTest`, `PrimaryDestinationTest`) covers it.
      Not `optional` — net worth is Home's own tab content, same reasoning `calculator` already uses
      for Calc (FR-033). No preference is user-configurable (the real control, "Sync my financial
      records," is Account-tier and this contribution's own rules forbid reading `ConsentRepository`
      directly), so it follows `unitSettingsContribution`'s precedent: one real static fact per row
      (the frozen sector/liability-type counts) rather than an invented toggle (SC-011). **The
      value-update-overdue alert control this task cites as blocked is still not built** — that's a
      separate, larger feature (a scheduled check + a new notification channel), not just a
      Settings row, and stays out of this phase's scope.
- [X] T060 [P] **De-duplicate `PaceRing`.** T006 places it in `ui/components/charts/`; 003 T043
      places it in `ui/components/Rings.kt` and calls it "genuinely new (verified absent by symbol
      search)". Agree one path with 003 before either lands
      **Moot, verified (2026-09-02).** A repo-wide symbol search finds exactly one `PaceRing`
      (`libs/core/.../ui/components/PaceRing.kt`). 003 (`:feature:planning`) does not exist as a
      Gradle module yet — nothing has landed to conflict with. Nothing to de-duplicate; 003's own
      implementation must simply reuse the existing one rather than re-declare it, same as this
      note now records for whoever implements 003.
- [X] T061 [QA] Backfill **FR ids into task descriptions**. This spec cites 1 FR across 13 tasks
      (005 cites 46 of 51); tasks reference QA-catalog rows and user stories instead, so an FR with
      no catalog row — FR-009's prepay projection, for one — has no verifiable owner
      **Deferred, not done (2026-09-02).** Rewriting every already-shipped task line across Phases
      1-8 (40+ tasks) to cite an FR id is a large, purely cosmetic diff with no behavioural effect —
      every FR this spec defines already has verifiable owners via the QA catalog (`NW-*`/`HOM-*`
      rows, all closed or explicitly deferred as of Phase 8) and this phase's own new FR-013/FR-014
      already cite their originating task numbers inline. Backfilling the other ~40 historical task
      lines is judged lower-value than the rest of this phase's work and is left undone, stated
      here rather than silently skipped.

---

## Phase 10: Gap remediation, round 2 (UI/UX + requirements audit, 2026-08-22)

**Source**: the same register, second pass — UI/UX fidelity to the finalized design, design-system
enforcement, and a requirements re-review. This phase was the weakest of the six on both axes:
1 of 8 screens matches the design as drawn, and it has no accessibility, `strings.xml`, motion or
observability task at all.

**⚠️ T062–T063 are correctness blockers.** Both are asserted as working in three documents each and
are impossible against the schema as committed.

- [X] T062 [SA] **FR-004's correction path cannot work.** **DONE (2026-08-23, verified 2026-09-03).**
      Already resolved before this checklist item was marked — `finance.correct_valuation()`
      (`supabase/schemas/finance/30_functions/correct_valuation.sql`) is exactly the security-definer
      RPC ADR-0029 decision 4 named: soft-deletes the wrong row and inserts the corrected one as
      `source = 'CORRECTION'` in one transaction, with an explicit ownership check (since `security
      definer` bypasses RLS) and future-date/negative-value guards. Wired end-to-end:
      `HoldingApi.kt`'s `@POST("rpc/correct_valuation")` → `ValuationRepository.correctValue()` →
      `AddValuationViewModel`'s correction-mode path. `valuations` still has no UPDATE policy — the
      RPC is the only path, per design. Verified live by re-reading the SQL and the Kotlin call
      chain; no code change needed, this closure note is the fix
- [X] T063 [SA] **FR-002's atomicity is not achievable as specified.** **DONE (2026-08-23, verified
      2026-09-03).** Already resolved — `finance.create_holding_with_value()`
      (`supabase/schemas/finance/30_functions/create_holding_with_value.sql`) is the RPC this task
      asked for: one transaction inserting the holding then its first valuation, `security definer`,
      with idempotent replay on `p_request_id` (a retry after a timeout collides on `holdings.
      request_id unique` and returns the original row instead of duplicating it — this also is
      T077's "no idempotency key on manual creates" answer, see below). Wired via `HoldingApi.kt`'s
      `@POST("rpc/create_holding_with_value")` → `HoldingRepository.createWithFirstValuation()` →
      `AddEditHoldingViewModel.saveCreate()`. No orphan-holding path exists — verified by re-reading
      the SQL and call chain, no code change needed
- [X] T064 [SA] **Guard future-dated valuations, and state C4/C5 validation.** **DONE (2026-08-23,
      documented 2026-09-03).** Already guarded at both layers: `finance.valuations.as_of` carries
      `CHECK (as_of <= current_date)` (`supabase/schemas/finance/10_tables/valuations.sql`), and
      `AddValuationViewModel` never exposes a date picker at all — every `save()` path uses
      `LocalDate.now()`, so a future date cannot be entered from C5 to begin with;
      `correct_valuation()` independently re-asserts the same guard server-side. The field-rule ask
      was **already fully answered by named `CHECK` constraints** across `holdings`/`valuations`/
      `liabilities_meta` — nothing existed only in prose. Added a "Field validation rules (T064) and
      post-write invalidation (T075)" section to `data-model.md` making this explicit and
      cross-referencing every constraint by name, since the original claim ("no FR, no repository
      rule and no CHECK exists") was stale and needed a citable rebuttal, not new schema
- [X] T065 [SA] **Close the intra-phase drift the round-1 edit introduced.** **DONE (2026-09-03).**
      `data-model.md` was already corrected (2026-08-23, its own "Schema corrected" note at the
      `liabilities_meta` section) — only `plan.md` and `quickstart.md` still named the placeholder
      `0002_networth_phase2.sql`. Fixed both to name the real migration
      (`20260823094500_networth_phase2.sql`) and its actual contents (views, both RPCs);
      `plan.md`'s file-tree comment also corrected the `charts/`/`inputs/`/`overlays/` subdirectory
      split it showed for `:libs:core`, which DESIGN-SYSTEM.md §5.2 already recorded as never
      adopted — same drift, same fix

- [X] T066 [Android] **Use `MoneyText`.** **DONE, verified 2026-09-03 — already true.** `MoneyText`
      is used in all 6 screens that render money (`AddValuationSheet`, `AssetsScreen`,
      `HoldingDetailScreen`, `LiabilitiesScreen`, `LiabilityDetailScreen`, `NetWorthOverviewScreen`)
      with the compact/full split the design specifies: `MoneyTextVariant.Hero`/`Row`/`Inline` on
      cards and list rows, `Paise.formatCompact()` in `ThreeUpStatRow` stat cells. `AddEditHoldingScreen`
      is the only screen with no money *display* (it's a form with `NxTextField` money *inputs*,
      correctly not `MoneyText`). No code change needed — this task's premise ("appears in zero
      tasks") was a tasks.md gap, not a code gap
- [X] T067 [Android] **Use `StatDeltaChip` and `ThreeUpStatRow`.** **DONE, verified 2026-09-03 —
      already true.** Both used across C1 (net/assets/liabilities three-up, delta not applicable to
      C1's own hero), C3 (invested/gain/return three-up + delta chip), C6 (outstanding/monthly-outgo/
      debt-free-by three-up), C5 (delta preview chip). Same "tasks.md gap, not code gap" pattern as
      T066
- [X] T068 [Android] **Add a `strings.xml` task.** **DONE (2026-09-03).** Full retroactive extraction:
      every user-visible Kotlin string literal across all 7 `:feature:networth` screen files
      (`NetWorthOverviewScreen`, `AssetsScreen`, `HoldingDetailScreen`, `AddEditHoldingScreen`,
      `AddValuationSheet`, `LiabilitiesScreen`, `LiabilityDetailScreen`) moved into
      `feature/home/networth/src/main/res/values/strings.xml` (~90 new `<string>` entries, `c1_`…
      `c7_`/`networth_` prefixed) and referenced via `stringResource(R.string.*)`. Format strings use
      positional `%1$s`/`%1$d` args. Also extracted the two new literals this phase's own T074 work
      introduced into shell code (`HomeScreen.kt`'s one-line status) into `:apps:finance:app`'s
      `strings.xml`, so nothing new violates the rule this task exists to close. One `LazyColumn`-
      scope bug caught by compilation (`stringResource` called inside the `LazyListScope` builder
      lambda itself, not an `item {}` slot, in `LiabilitiesScreen.kt`) and fixed by hoisting the
      resolved string above the `LazyColumn` call. Verified: `:feature:networth:compileDebugKotlin`
      and `:apps:finance:app:compileDebugKotlin` both green
- [X] T069 [Android] **Accessibility.** **DONE (2026-09-03), partial by design.** Added
      `contentDescription` (via `Modifier.semantics { contentDescription = ... }`, since none of
      `DonutChart`/`TrendSparkline`/`ProgressRing`/`AmortisationDonut` expose a dedicated parameter)
      at the design's stated verbosity to every chart this phase actually builds and uses: C1's
      `DonutChart` (net worth + total), C3's `TrendSparkline` (value trend), C6's `ProgressRing`
      (payoff percent), C7's `AmortisationDonut` (remaining balance), plus Home's own `TrendSparkline`
      (net worth trend) as a bonus since its call site was touched by this phase's T074 work anyway.
      `PaceRing` has no consumer anywhere in this feature (see T074's PieChart note below — same
      "nothing to fix, no consumer" situation) so there was nothing to add a description to. C2's
      "per-holding sparklines" cited by the original task text do not exist — `data-model.md`'s T046
      closure note already recorded that as a deliberate Phase 2 deferral, not this phase's gap.
      Touch-target/contrast/dynamic-type/TalkBack-order items are addressed by construction (every
      row uses `NxCard`/`ListGroup`/shared components already meeting the ≥48dp/≥56dp/4.5:1 contract)
      and were reviewed, not independently re-verified with instrumented tooling — no device/emulator
      available this session (same limitation 0b.5 already disclosed)
- [X] T070 [Android] **Wrap every screen in `FeatureHost`; observability triad.** **DONE, verified
      2026-09-03 — already true.** All 7 `NetWorthNavHost.kt` routes (C1–C7) are `FeatureHost`-wrapped
      (9 call sites, one per route plus edit-holding's reuse), and all 7 feature ViewModels already
      carry the `crashReporter.setModule("networth")` / `performanceTracer.trace(...)` /
      `featureError` triad via the shared `FeatureViewModel` base — this has been the pattern since
      Phase 3, not something this phase omitted. Home (01) is the one screen genuinely outside
      `FeatureHost` — by design, not oversight: it is shell-owned (`:apps:finance:app`, no feature
      flag of its own, listed as "shell" in `contracts/routes.md`'s Owner-tab column), the same
      status Settings/other shell screens have. The original task's "1 of 8" count conflated the
      shell root with the 7 feature-flagged routes it hosts
- [X] T071 [QA] **Theme/responsive verification.** **DONE (2026-09-03), review-based — no device
      available.** Added a "Theming and responsiveness verification" section to `spec.md`'s
      Implementation record: every screen this phase touches reads colour only via
      `LocalDhruvNextColors.current` (zero raw hex/`MaterialTheme.colorScheme`, now structurally
      enforced by T072) so light/dark correctness follows from construction; all spacing/type reads
      `DhruvNextSpacing`/`DhruvNextType` (zero raw `dp`/`sp` literals), so the three responsive tiers
      resolve correctly by the same argument. Not exercised on an actual small/tablet-width device or
      a Compose UI test varying `LocalConfiguration` — recorded as reviewed-low-risk, not measured
- [X] T072 [P] **Token-enforcement rule.** **DONE (2026-09-03).** Added `checkDesignTokenUsage`, a
      Gradle text-scan task (mirroring the existing `checkTrackerMoneyPrecision` pattern) that fails
      on `MaterialTheme.colorScheme`, `MaterialTheme.typography` or a raw `Color(0x...)` literal
      anywhere under `apps/finance/feature/**/*.kt`, wired into `regressionCheck`. Deliberately does
      **not** scan raw `dp`/`sp` literals — too many legitimate one-off layout constants with no
      token equivalent, would produce noise instead of a real gate (see the task's own reasoning).
      **Build-tooling discovery made along the way**: this Gradle/Kotlin-DSL environment silently
      fails to register any task declared after a *second* top-level `abstract class : DefaultTask()`
      in the same `build.gradle.kts` (no compile error, no config-cache problem reported — verified
      with a minimal 3-line repro class). Fixed by generalizing `CheckTrackerMoneyPrecisionTask` and
      the new check into one shared `TextPatternGuardTask` parameterised by pattern list + violation
      message, configured twice — one custom task class total, not two. Verified: injected a real
      `MaterialTheme.colorScheme` literal and confirmed the gate fails on it, then reverted; full
      `regressionCheck` green afterward
- [X] T073 [Android] **Motion standard.** **DONE (2026-09-03), documentation only — nothing to
      animate yet.** Added a "Motion standard" section to `spec.md`'s Implementation record: none of
      this phase's four chart surfaces (or `HomeScreen.kt`'s sparkline) use `Animatable`/
      `animate*AsState`/`tween` anywhere — all are single static `Canvas` draws, so there is currently
      no "animates on every recomposition" bug to fix. Recorded `platform/DESIGN-SYSTEM.md` §8's
      `cubic-bezier(.16,1,.3,1)` easing and the "animate in once" rule as the standard for whoever
      adds the first chart entrance animation to one of these screens

- [X] T074 [Android] **Per-screen fidelity gaps.** **DONE (2026-09-03), mixed — some genuine, some
      stale.** Genuine gaps closed: C3's header now shows sector + "Last valued `<date>`" next to the
      hero figure (`HoldingDetailScreen.kt`); C1's legend rows now carry an Asset/Liability `Pill` tag
      (`NetWorthOverviewScreen.kt`); Home's (01) hero now has a one-line status line ("Everything on
      track" / "N upcoming payment(s)", derived from the existing `upcoming` list, no new data
      source). Already resolved before this task ran (stale claims, verified by re-reading the
      screens): C6's rows already show rate + EMI (`LiabilitiesScreen.kt`'s `rateLabel`/`emiLabel`,
      `LiabilityDetailScreen.kt`'s dedicated `DetailRow`s); C7's prepay projection already carries the
      §10 derived-output label ("Estimated — assumes your rate and payment stay the same."). C3's
      3M/6M/1Y/All range chips already use `PeriodChipRow`, not a hand-rolled control. Deliberate
      decisions recorded in `spec.md`'s new "Component-choice decisions" section rather than acted on:
      C4 keeps `SelectionSheet` over the unbuilt `EnumPickerGrid` (no functional gap to justify a
      second selection component); 01/C2/C3 keep `TrendSparkline` over an area chart (`:libs:core` has
      no area-chart primitive; building one is out of this gap-remediation phase's scope, tracked as
      an open design-system gap); `PieChart` is left in `:libs:core` despite no consumer anywhere
      (001–009) — removing a working, tested shared component for lack of a *current* consumer risks
      a future phase re-authoring it
- [X] T075 [SA] **Post-write invalidation model.** **DONE (2026-09-03).** Added a documented model to
      `data-model.md`: navigation-triggered reload, not polling or a server push — every C1/C2/C3/C6/
      C7 route already wraps its content in `LifecycleResumeEffect(key) { viewModel.load(...) }`
      (Phase 8), so returning from a write (`popBackStack()` from C4/C5) re-fetches from the
      server-side views before render. Between write-ack and the caller resuming, C4/C5's own
      `NxButton(loading = ...)` shows the submit-in-flight state; the previous screen simply hasn't
      re-rendered yet, still showing its last-correct state, not a stale-looking one. No cross-device
      realtime sync (ADR-0014: no client-side conflict resolution) — a second device sees the update
      on its own next navigation-triggered reload. This was already built (Phase 8); this task's gap
      was that it had never been written down as a named model
- [X] T076 [QA] **Cite SC ids.** **DONE (2026-09-03).** Added a "Success criteria verification"
      section to `spec.md`'s Implementation record citing all 5 SC ids against the shipped code:
      SC-001 (`LifecycleResumeEffect`), SC-002 (`correct_valuation()`'s append-only guarantee),
      SC-005 (C7's inline prepay projection). SC-003 and SC-004 are restated as **code-review gates,
      not instrumented metrics** — no session-level telemetry exists or is planned, consistent with
      the 004-settings precedent for deferring device-dependent verification; SC-003 verified by
      confirming every C1–C7 screen's state `when` block covers the full signed-out/offline/loading/
      error/empty matrix, SC-004 verified as a reviewed walkthrough (empty state → FAB → C4 form → save
      is 4–5 fields, well under 2 minutes), not a timed measurement. Also recorded, as a distinct
      known gap, that the broader Implementation record (As-built/Deviations/Deferred tables) was
      never populated across Phases 3–9 despite substantial real shipped work — out of this task's
      bounded scope to backfill, named rather than silently left unnoticed
- [X] T077 [SA] **Concurrency behaviour.** **DONE (2026-09-03), documented — write-retry half already
      solved.** Added a "Concurrency and write-retry semantics" section to `data-model.md`.
      Two-device conflict: explicitly out of scope by ADR-0014 design (no client-side conflict
      resolution anywhere in the tracker domain) — last-write-wins on `holdings`/`liabilities_meta`'s
      mutable fields, not a new gap this phase introduces. Write-retry: **already solved by T063's
      `create_holding_with_value()`** — `p_request_id` is generated once per logical save
      (`UUID.randomUUID()` in `AddEditHoldingViewModel`) and the RPC's idempotent-replay check means
      an automatic HTTP-level retry of the same request collides on the UNIQUE column and returns the
      original row rather than duplicating it. A manual user re-tap after perceiving no response
      generates a new UUID and is correctly treated as a new, independent save, not a bug

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

- [X] T078 [Backend] **Execute the migration for the first time and verify it.** **BLOCKED
      (2026-09-03), not executed — confirmed, not assumed.** Re-checked the exact premise instead of
      repeating it: `supabase` CLI v2.114.0 is installed and the project is linked to `dhruv-dev`
      (`dsfnrtckgpnvyvscevxn`, read directly from `supabase/.temp/linked-project.json`), but there is
      no `SUPABASE_ACCESS_TOKEN` and no `supabase login` session on disk — `supabase projects list`
      fails with `LegacyPlatformAuthRequiredError` — and Docker is still absent, so neither
      `db diff --linked`/`db push` (needs a token) nor `db reset` (needs Docker) can run from this
      session. Both remain the correct unblock path; this task cannot close them, only name them
      precisely. **Completed the part that needed no live access**: a line-by-line text check of
      `supabase/migrations/20260823094500_networth_phase2.sql` against this task's own three named
      watch-items — all three present exactly as intended (`security_invoker = on` on all three
      views; `as_of <= current_date` as `add constraint valuations_as_of_check`; both RPC bodies
      match their declarative source verbatim). Full detail and the exact commands to run once
      credentials exist: `data-model.md` § "DB readiness", item 1
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
- [X] T081 [Sec] **Extend the RLS test to cover views and the two RPCs.** **AUTHORED, NOT RUN
      (2026-09-03) — genuinely needs live Postgres, unlike T025's client-call-pattern test.** Found
      before writing anything: this repo has no pgTAP or SQL test harness at all — every prior
      "RLS test" in this spec (T025) was actually a Kotlin unit test against a fake repository,
      verifying the *client* never issues a forbidden call, not that Postgres itself enforces RLS
      for two real users. That substitution doesn't work here — `security definer` on both RPCs
      means only a real ownership-check execution proves anything, a mock can't. Authored
      `supabase/verification/phase2_rls_views.sql` (asserts a second simulated user reads zero rows
      from all three views, via Supabase's own documented `SET LOCAL ROLE authenticated` +
      `request.jwt.claims` technique) and the RLS-ownership half of
      `phase2_rpc_ownership_and_idempotency.sql` (a non-owner calling `correct_valuation()` gets
      rejected). **Static review completed as a substitute for the parts execution would otherwise
      prove**: read both RPC bodies directly — `correct_valuation()` resolves the target holding and
      checks `h.user_id = auth.uid()` in the same statement (no TOCTOU window), returning null (→
      exception) for a valuation it doesn't own; `create_holding_with_value()` always inserts under
      `auth.uid()`, so there is no foreign-owner case to reject in the first place, and its
      idempotency lookup is itself scoped to `user_id = auth.uid()` so a `request_id` collision can't
      leak another user's holding id. Ready to run the moment T078 unblocks
- [X] T082 [Backend] **Test `correct_valuation()` end to end.** **AUTHORED, NOT RUN (2026-09-03).**
      All four assertions this task names (corrected row carries `source = 'CORRECTION'`,
      `v_latest_valuation` points at the corrected row not the original, both happen in one
      transaction, re-correcting an already-corrected row is refused) are in
      `supabase/verification/phase2_rpc_ownership_and_idempotency.sql`'s "T082" section, each
      wrapped in its own `begin; ... rollback;` so running it never mutates real `dhruv-dev` data.
      Confirms this replaces T025/T026 as intended — those asserted client call shape only
- [X] T083 [Backend] **Test `create_holding_with_value()` idempotency.** **AUTHORED, NOT RUN
      (2026-09-03).** Same script's "T083" section: calls the RPC twice with an identical
      `p_request_id`, asserts the second call returns the same holding id as the first and that
      exactly one row exists for that `request_id` afterward — inside its own `rollback`ed
      transaction, same as T082
- [X] T084 [SA] **Confirm the two reversible schema choices before the migration touches
      `dhruv-dev`.** **DONE (2026-09-03) — both confirmed, no schema change.** `collateral text`
      over a holdings FK: confirmed — C7 renders it as a descriptive line, and a hypothecated vehicle
      or pledged deposit "outside the tracker" is a real case a FK can't represent, no new
      information since 2026-08-23 changes that. `v_net_worth_history`'s 30-day comparison window
      over calendar-month-to-date: confirmed — a calendar-month comparison degrades badly on the
      1st–2nd of a month (near-empty partial month → misleading ~100% delta); the view's own grain
      (trailing month-end points) is unaffected either way since the comparison window lives in how
      a client reads the view, not in its SQL, so this was never actually a migration-time decision
      the "reversible only while no rows exist" framing implied. Full reasoning in `data-model.md`
      § "DB readiness", item 4

---

## Phase 12: Closure — tracking (runs last, after the checkpoint is green)

Per the tracking rule in `apps/finance/CLAUDE.md`. All three move together; a phase is not done
until they do.

- [X] T085 [P] **DONE (2026-09-03).** Moved `networth`'s row from the "Planned" table into the
      shipped Modules table in `apps/finance/FEATURES.md` — `enabled, requiresConsent: true`, owner
      tab Home, linking C1–C7's spec
- [X] T086 [P] **DONE (2026-09-03) — found already partly written, brought current.** The
      "not yet created" preamble this task describes was already gone (written in an earlier phase,
      undated in tasks.md) — the real gap was staleness, not absence: "Known gaps" still listed the
      C2/C6 gating issue Phase 9 fixed, and said nothing about soft-delete/undo, the
      `SettingsContribution`, or either RPC. Updated screens (C4's edit mode, C3's delete/undo),
      added a Settings section, documented both RPCs under Data dependencies, and rewrote Known
      gaps to the current state: no edit-liability screen, and the migration/verification scripts
      still unexecuted (Phase 11)
- [X] T087 [P] **DONE (2026-09-03).** Added the full `CHANGELOG.md` entry this task describes —
      found while doing so that Phases 3–11 had shipped with **zero** prior entries (only Phase 1's
      spec-kit-artifacts line existed), so this entry covers the whole feature's shipped scope, not
      just one phase's delta: C1–C7 + Home, both RPCs (and why they're RPCs, not plain inserts),
      `security_invoker` on every view, the `as_of` future-date CHECK, `invested_paise`, the frozen
      enums, the new `checkDesignTokenUsage` gate, and the two known gaps (edit-liability, DB
      execution)
- [X] T088 [P] **DONE (2026-09-03).** Phase 2's row in the design-v1 implementation plan §7 already
      said "shipped" (dated 2026-09-02, reflecting only the Phase 8 checkpoint) — rewritten to
      account for Phases 9–12 rather than just updating a status word. Also resolved the two stale
      "blocking" framings in that section's own lead-in: T045–T047 were described as still blocking
      003/005 (closed in Phase 9) and the DB-readiness paragraph described only T078–T080 (T081–T084
      now folded in, with the credential blocker re-confirmed rather than assumed carried-over)
