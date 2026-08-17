---
description: "Task list for Money Tab (Phase 3)"
---

# Tasks: Money Tab (Phase 3)

**Input**: Design documents from `apps/finance/specs/002-money-tab/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/routes.md, quickstart.md
(all present — see `AVAILABLE_DOCS`)

**Tests**: Included and REQUIRED, not optional — constitution Article I (Test-First) mandates
RED → GREEN → REFACTOR with every test citing a scenario ID from
`apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §4 (`MNY-*`). Those 20
catalog rows already exist and are reviewed (marked done) — tasks below cite them, they do not
recreate them (constitution Article II).

**Coverage**: measured, not assumed (constitution Article X, ADR-0013). A new Gradle module is
invisible to the coverage gate until it is added to **both** hardcoded lists — `coveredModules` in
the root `build.gradle.kts` (feeds `jacocoAggregatedReport` + `jacocoCoverageVerification`) and
`_FEATURES` in `scripts/ci/regression_summary.py` (feeds the Job Summary / PR comment / release
notes). T005 does that up front rather than at the end, so every story's tests count toward the
merged report from the first commit instead of landing outside it. The floor ratchets at the Phase 9
checkpoint (T078), never ahead of landed tests.

**Organization**: Tasks are grouped by user story (spec.md's Stories 1–6, priority P1–P6) so each
story is independently implementable, testable and demoable — matching spec.md's own "Independent
Test" per story.

**Phase-2 dependency**: this phase assumes `001-net-worth-tracker` landed. `NxTextField`'s error
state, `NxButton` sizes/loading, `SelectionSheet` and `NxSelect` are Phase 2 deliverables consumed
here (T029, T035). If Phase 3 starts first, those four move into Phase 2 (Foundational) below.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US6, matching spec.md's six user stories
- Every path below is a real path in this repo, not a placeholder

---

## Phase 1: Setup

**Purpose**: Stand up the new Gradle module, its flag, and its coverage wiring before any
story-specific code.

- [ ] T001 Create `:apps:finance:feature:money` module skeleton — `apps/finance/feature/money/money/build.gradle.kts` (`dhruv.android.library` + `dhruv.android.compose`, deps on `:apps:finance:data`, `:libs:core`, `:libs:settings`, same shape as `apps/finance/feature/plan/loans`); `include(...)` + `projectDir` remap in `settings.gradle.kts`
- [ ] T002 [P] Create `di/MoneyModule.kt` Koin module stub in `apps/finance/feature/money/money/` and aggregate it in `CalculatorApplication`
- [ ] T003 [P] Create `MoneyConfig.kt` scaffold in `apps/finance/feature/money/money/` (screen-level constants — account/category type labels, the 30-day staleness threshold, the next-30-days window, filter presets — per the no-hardcoding rule; filled per story below, never inline in a screen)
- [ ] T004 [P] Add the `money` flag (`enabled: true`, `minVersion: "1.0.0"`, `requiresConsent: true`) to `platform/feature-flags/dhruv-finance.json` — gated by the "Sync my financial records" A3 switch, per contracts/routes.md
- [ ] T005 Wire the new module into coverage measurement — add `":apps:finance:feature:money"` to `coveredModules` in `build.gradle.kts` (root) **and** `"money"` to `_FEATURES` in `scripts/ci/regression_summary.py`. Without the first, the module's classes are absent from both the merged report and the floor check (Article X passes vacuously); without the second, its coverage is reported as `(other)` instead of by name. `enableUnitTestCoverage = true` needs no action — the `dhruv.android.library` convention plugin already sets it

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, shared components and shell plumbing every story depends on.

**⚠️ CRITICAL**: No user story task may start before this phase closes (constitution Article II —
SA schema, then QA catalog rows, before any Backend/Android code).

- [ ] T006 [SA] Author the six new tables declaratively in `supabase/schemas/finance/10_tables/` — `accounts.sql`, `categories.sql`, `transactions.sql`, `transaction_events.sql`, `recurring_templates.sql`, `suggestions.sql`, each with its RLS policies, CHECK constraints and indexes exactly as data-model.md specifies. `transaction_events` gets **SELECT + INSERT policies only** (no UPDATE, no DELETE) — that policy set is what makes FR-008 true at the DB layer. `suggestions` gets the unique `(recurring_id, due_on)` idempotency key
- [ ] T007 [P] [SA] Author the three views in `supabase/schemas/finance/20_views/` — `v_account_balances.sql` (opening + signed sum, `counts_as_spendable` flag), `v_month_summary.sql`, `v_category_spend.sql`. Both summary views exclude `TRANSFER` rows (BR-D1) and `excluded_from_spend` categories (FR-025) in SQL, not in Kotlin — research R3
- [ ] T008 [P] [SA] Author `supabase/schemas/finance/30_functions/fn_transaction_audit.sql` (trigger function + `AFTER INSERT OR UPDATE OR DELETE` trigger on `finance.transactions`) and `merge_categories.sql` (invoker rights, returns the moved count) — research R4, R9
- [ ] T009 [SA] Extend `supabase/schemas/public/30_functions/delete_my_data.sql` with FK-safe DELETEs for all six new tables — a table missed here breaks the 7-day erasure guarantee (NFR-1) silently
- [ ] T010 [SA] Generate the migration (`supabase db diff -f money_phase3`), hand-add the `grant usage on schema` / per-table grants `db diff` cannot express (ADR-0032/0033), review the generated SQL, and confirm `python scripts/db/gen_schema_docs.py equiv` and `... docs --check` both pass
- [ ] T011 [P] [Sec] RLS + policy verification against the dev Supabase project: cross-user isolation on all six tables; no UPDATE/DELETE path exists on `transaction_events`; the audit trigger fires on insert, update, category change and soft-delete; `merge_categories` is atomic under an interrupted call
- [ ] T012 [P] Build component batch **B4** in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/lists/` — `DayGroupHeader`, `LedgerRow`, `SuggestedRow` (dashed until accepted), `ReconcileBanner`
- [ ] T013 [P] Build **B6** remainder in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/inputs/` — `NxTextArea` (multi-line + helper text), `InputChip` (removable, trailing ×)
- [ ] T014 [P] Build **B7** in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/states/` — `StatusBadge` (success/warning/error/accent dot) and `InfoBanner`. Extend `CountBadge` rather than duplicating it for the count case (design system §5.3)
- [ ] T015 [P] Build `DateRangeSheet` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/overlays/DateRangeSheet.kt` — consumed by D5
- [ ] T016 [P] Build `AmountKeypadSheet` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/overlays/AmountKeypadSheet.kt` as a **composition** of the existing `NumericKeypad` inside `DhruvModalSheet` with a date key — never a second keypad component (constitution Article VI, research R5)
- [ ] T017 [P] Extend `BackContractTest` (`libs/core/src/test/.../navigation/BackContractTest.kt`) with the active-tab case — RED before T018, citing NAV-ARCH-003's contract
- [ ] T018 Generalise `resolveBackAction` (`libs/core/src/main/kotlin/com/dhruv/core/navigation/BackContract.kt`) from Plan's controller to "the active tab's controller", and add the Money tab's nested `NavHost` in `apps/finance/app/src/main/java/com/dhruv/finance/app/navigation/` — the work Phase 0 explicitly descoped until a second tab needed sub-routes (depends on T017)
- [ ] T019 [P] Add `NavTarget.OpenAccount(accountId)` in `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt`, map it in the app shell's `NavigationDispatcher`, and add the matching row to `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1 (sealed case + registry row is the required pair)
- [ ] T020 Add DTOs, domain models and mappers for all six entities under `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/{dto,model,mapper}/` (paise `Long`, TEXT enums), and wire the `Accept-Profile: finance` / `Content-Profile: finance` headers into `SupabaseClientFactory` if Phase 2 has not already — omitting them 404s silently against the empty `public` schema (ADR-0033)

**Checkpoint**: schema migrated and RLS-verified, all component gaps closed, Money's nested NavHost
live. User story work can begin.

---

## Phase 3: User Story 1 — Record a spend in seconds (Priority: P1) 🎯 MVP

**Goal**: Quick-add a transaction in three taps and see it land in the ledger with the month summary
updated (spec.md Story 1).

**Independent Test**: From the ledger, tap add, enter an amount, accept the guessed category and
account, save — the row appears under today and the month summary changes by exactly that amount.

### Tests for User Story 1 (write first, confirm they FAIL before implementation)

- [ ] T021 [P] [US1] `TransactionRepositoryTest` — a TRANSFER is excluded from expense totals and category shares, citing `MNY-BR-001`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/TransactionRepositoryTest.kt`
- [ ] T022 [P] [US1] `TransactionRepositoryTest` — type invariants rejected at the repository boundary (TRANSFER needs a distinct `to_account_id` and no category; EXPENSE/INCOME need a category and no `to_account_id`), mirroring the DB CHECKs, same file as T021
- [ ] T023 [P] [US1] `QuickAddViewModelTest` — category and account are pre-guessed, both remain editable, and Save emits the new row into today's group with the summary recomputed, citing `MNY-FLOW-001`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/QuickAddViewModelTest.kt`
- [ ] T024 [P] [US1] `TransactionFormViewModelTest` — values entered in quick add carry over into the full form, and discarding an edited form prompts for confirmation (N4), in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/TransactionFormViewModelTest.kt`

### Implementation for User Story 1

- [ ] T025 [US1] Implement `TransactionRepository` — create, edit, soft-delete, split writes as sibling rows sharing `split_group_id` (no parent row, data-model.md), all amounts `Long` paise (depends on T021, T022), in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/TransactionRepository.kt`
- [ ] T026 [US1] Implement `LedgerViewModel` — today's transactions plus the pinned month summary read from `v_month_summary` (never a client-side sum, NFR-8), in `apps/finance/feature/money/money/LedgerViewModel.kt`
- [ ] T027 [US1] Build `LedgerScreen` (D1) base — day-grouped list using T012's `DayGroupHeader`/`LedgerRow`, pinned INCOME · EXPENSE · SAVED % summary, FAB, `FeatureHost`-wrapped with the `money` flag key; replaces the Money tab's `NotConfiguredCard` placeholder, in `apps/finance/feature/money/money/LedgerScreen.kt`
- [ ] T028 [US1] Build `QuickAddSheet` (D2) — amount-first via T016's `AmountKeypadSheet`, `SegmentedRow` type selector, pre-guessed category/account, optional note, "More options" hand-off to D3, citing `MNY-UI-001`, in `apps/finance/feature/money/money/QuickAddSheet.kt`
- [ ] T029 [US1] Build `TransactionFormScreen` (D3) — full-screen modal (close ✕, not back), every field per FR-004, `NxTextArea` for the note, `NxSelect`/`SelectionSheet` for category and account, `NxTextField` error state for validation, `rememberDiscardGuard` on exit. **The goal-link field is hidden this phase** — `goal_id` exists on the row but `goals` does not until Phase 4, so `MNY-FLOW-003` stays open and is closed there, not silently skipped. Recurring toggle wiring lands in US6 (T068), in `apps/finance/feature/money/money/TransactionFormScreen.kt`
- [ ] T030 [US1] Implement the category/account pre-guess rule (last-used per payee, falling back to most-used) in `TransactionRepository` + `MoneyConfig.kt` — the mechanism the three-tap target depends on

**Checkpoint**: User Story 1 fully functional — a working expense tracker on its own.

---

## Phase 4: User Story 2 — Understand where the month went (Priority: P2)

**Goal**: Read a chosen month: day grouping, pinned summary, search, live-counting filter and saved
views (spec.md Story 2).

**Independent Test**: With transactions across several days and categories, open the ledger, confirm
day grouping and month totals, then apply a filter and confirm the count and list match.

### Tests for User Story 2

- [ ] T031 [P] [US2] `LedgerViewModelTest` — rows day-grouped with per-day net; pinned header shows correct INCOME/EXPENSE/SAVED %, citing `MNY-UI-002`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/LedgerViewModelTest.kt`
- [ ] T032 [P] [US2] `LedgerFilterViewModelTest` — the live result count updates on every filter change and equals the row count after applying, citing `MNY-UI-003`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/LedgerFilterViewModelTest.kt`

### Implementation for User Story 2

- [ ] T033 [US2] Add the month selector and month-scoped paging — list and summary both follow the selector (FR-011), in `apps/finance/feature/money/money/LedgerViewModel.kt` and `apps/finance/feature/money/money/LedgerScreen.kt`
- [ ] T034 [US2] Add ledger search over payee, description and note using `SearchField` (FR-013), in `apps/finance/feature/money/money/LedgerScreen.kt`
- [ ] T035 [US2] Build `LedgerFilterSheet` (D5) — type, multi-category via `SelectionSheet` with a "+N more" summary, amount range, account; live "Show N results"; Reset (depends on T032), in `apps/finance/feature/money/money/LedgerFilterSheet.kt`
- [ ] T036 [US2] Implement saved views — name a filter combination and re-apply it later (FR-015); persisted in the existing encrypted settings DataStore, not a new table, in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/SavedViewRepository.kt`
- [ ] T037 [US2] Add the full state set to D1 — `EmptyStateCard` with a verb CTA, `SkeletonBlock`, `RetryErrorCard`, `OfflineStateCard`, `SignedOutCard` per the screen-state matrix (FR-032), in `apps/finance/feature/money/money/LedgerScreen.kt`
- [ ] T038 [US2] Verify D1's list is virtualised and month-paged so a 5,000-transaction month scrolls without stutter (SC-009, NFR-8) — measure on a device, do not assume, in `apps/finance/feature/money/money/LedgerScreen.kt`

**Checkpoint**: Stories 1–2 independently functional.

---

## Phase 5: User Story 3 — Know what is actually spendable (Priority: P3)

**Goal**: Accounts with an honest "spendable now", credit shown as owed-not-held, and account detail
with reconciliation (spec.md Story 3).

**Independent Test**: Create bank, cash and credit-card accounts with balances; confirm "spendable
now" equals bank + cash only and the card sits in a separate owed grouping.

### Tests for User Story 3

- [ ] T039 [P] [US3] `AccountRepositoryTest` — "spendable now" sums BANK/CASH/WALLET only and excludes a credit card's negative balance, citing `MNY-BR-002`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/AccountRepositoryTest.kt`
- [ ] T040 [P] [US3] `AccountDetailViewModelTest` — an account past the 30-day staleness threshold raises the reconcile banner, and reconciling clears it, citing `MNY-UI-005`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/AccountDetailViewModelTest.kt`

### Implementation for User Story 3

- [ ] T041 [US3] Implement `AccountRepository` — CRUD plus balances read from `v_account_balances` (depends on T039), in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/AccountRepository.kt`
- [ ] T042 [US3] Build `AccountsScreen` (D6) — `SPENDABLE NOW` total, `BANK` / `CASH·WALLET` / `CREDIT — OWED, NOT HELD` groups with limit, utilisation % and due date, staleness note per account, and the footnote that automatic balance refresh arrives with account linking, citing `MNY-UI-004`, in `apps/finance/feature/money/money/AccountsScreen.kt`
- [ ] T043 [US3] Build `AccountDetailScreen` (D7) — balance, masked number, primary badge, balance-trend chart, month IN/OUT, recent activity with a running balance, T012's `ReconcileBanner`, in `apps/finance/feature/money/money/AccountDetailScreen.kt`
- [ ] T044 [US3] Implement the reconcile flow — sets `reconciled_at` and writes an adjustment transaction (`source = 'RECONCILE'`, reserved `Adjustment` category, `excluded_from_spend`) for any difference; never edits `opening_balance_paise` (FR-021, research R8), in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/AccountRepository.kt` and `apps/finance/feature/money/money/AccountDetailScreen.kt`
- [ ] T045 [US3] Build the add/edit account form — name, type, mask (last 4 only, never a full number), primary flag, opening balance, and credit-only limit/due-day fields, in `apps/finance/feature/money/money/AccountFormScreen.kt`
- [ ] T046 [US3] Add the credit-card-bill row to Home's UPCOMING list alongside the existing loan/EMI rows, tapping through via `NavTarget.OpenAccount` (FR-034 — the item Phase 2 deferred until `accounts` existed), in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/home/HomeScreen.kt`
- [ ] T047 [US3] Add the full state set to D6 and D7 (FR-032), in `apps/finance/feature/money/money/AccountsScreen.kt` and `apps/finance/feature/money/money/AccountDetailScreen.kt`

**Checkpoint**: Stories 1–3 independently functional.

---

## Phase 6: User Story 4 — See a single entry in full, with its history (Priority: P4)

**Goal**: A read-first transaction detail carrying an append-only audit trail, plus Duplicate and
Make-recurring (spec.md Story 4).

**Independent Test**: Create a transaction, change its category, open its detail — both events
appear in an ordered history with old and new values.

### Tests for User Story 4

- [ ] T048 [P] [US4] `TransactionAuditTest` — every mutation path (create, edit, category change, soft-delete, accept-from-recurring) leaves exactly one matching `transaction_events` row, citing `MNY-BR-006`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/TransactionAuditTest.kt`
- [ ] T049 [P] [US4] `TransactionDetailViewModelTest` — amount/payee/datetime/cleared state render and HISTORY lists every event in order, citing `MNY-UI-006`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/TransactionDetailViewModelTest.kt`
- [ ] T050 [P] [US4] `TransactionDetailViewModelTest` — Duplicate opens an unsaved pre-filled draft (nothing written) and Make-recurring opens the form's recurring toggle pre-filled, citing `MNY-FLOW-004` and `MNY-FLOW-005`, same file as T049

### Implementation for User Story 4

- [ ] T051 [US4] Implement `TransactionDetailViewModel` — transaction + its events, rendering each event's `kind`/`detail` as plain language (depends on T048), in `apps/finance/feature/money/money/TransactionDetailViewModel.kt`
- [ ] T052 [US4] Build `TransactionDetailScreen` (D4) — read-first layout, cleared state, category, account, note, receipt, HISTORY section. **No budget-impact line this phase** — it needs `budgets` (Phase 4); leave the row out and keep `MNY-UI-006`'s budget clause deferred, not faked, in `apps/finance/feature/money/money/TransactionDetailScreen.kt`
- [ ] T053 [US4] Implement the Duplicate action — opens D3 pre-filled, writes nothing until saved, and the copy's history starts at `CREATED` (spec Edge Cases), in `apps/finance/feature/money/money/TransactionDetailViewModel.kt`
- [ ] T054 [US4] Implement receipt attach + view against a **device-local** URI in app-private storage, with the UI stating receipts stay on this device (research R6), in `apps/finance/feature/money/money/ReceiptStore.kt` (consumed by `TransactionFormScreen.kt` and `TransactionDetailScreen.kt`)

**Checkpoint**: Stories 1–4 independently functional.

---

## Phase 7: User Story 5 — Keep categories meaningful over time (Priority: P5)

**Goal**: Category list with spend and share, safe rename, irreversible-but-complete merge, and
excluded-from-spend handling (spec.md Story 5).

**Independent Test**: Rename a category and confirm its transactions are untouched; merge two and
confirm the dialog names the exact count and nothing is lost.

### Tests for User Story 5

- [ ] T055 [P] [US5] `CategoryRepositoryTest` — rename changes only the label; id and every linked transaction are unchanged, citing `MNY-BR-003`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/CategoryRepositoryTest.kt`
- [ ] T056 [P] [US5] `CategoryRepositoryTest` — merge moves N+M transactions atomically and returns the count the dialog must state; the source category ends soft-deleted, citing `MNY-BR-004`, same file as T055
- [ ] T057 [P] [US5] `CategoriesViewModelTest` — Expense/Income tab counts, per-row spend and share, `Investment · Excluded from spend`, and `Uncategorised · N need a category`, citing `MNY-UI-007`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/CategoriesViewModelTest.kt`
- [ ] T058 [P] [US5] `CategoryRepositoryTest` — an `excluded_from_spend` category contributes nothing to month expense or category share (FR-025), same file as T055

### Implementation for User Story 5

- [ ] T059 [US5] Implement `CategoryRepository` — CRUD, rename, `excluded_from_spend` toggle, and merge via the `merge_categories` RPC (never a client-side loop, research R9) (depends on T055, T056), in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/CategoryRepository.kt`
- [ ] T060 [US5] Build `CategoriesScreen` (D8) — Expense/Income tabs with counts, per-row icon/name/sub-count-or-budget/spend/share, the two special rows, and the footnote stating the rule verbatim ("Renaming keeps history. Merging moves every transaction and cannot be undone."), in `apps/finance/feature/money/money/CategoriesScreen.kt`
- [ ] T061 [US5] Wire the merge confirmation through `ConfirmDangerDialog` stating the exact transaction count that will move and that it cannot be undone (FR-024), in `apps/finance/feature/money/money/CategoriesScreen.kt`
- [ ] T062 [US5] Seed the two reserved categories per user on first use — `Uncategorised` (FR-026's target) and `Adjustment` (`excluded_from_spend = true`, T044's reconciliation target); both renameable, neither deletable, in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/CategoryRepository.kt`

**Checkpoint**: Stories 1–5 independently functional.

---

## Phase 8: User Story 6 — Let repeating money repeat, without losing control (Priority: P6)

**Goal**: Recurring definitions that produce reviewable pending entries, a next-30-days view, and
pause/resume (spec.md Story 6).

**Independent Test**: Create a recurring entry due today, confirm no ledger row is written, accept
the pending entry, and confirm the resulting transaction's history names the recurring source.

### Tests for User Story 6

- [ ] T063 [P] [US6] `RecurringRepositoryTest` — a due template's occurrence creates a `suggestions` row, never a `transactions` row, citing `MNY-BR-005`, in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/RecurringRepositoryTest.kt`
- [ ] T064 [P] [US6] `RecurringRepositoryTest` — materialising twice (two app opens, or two devices) produces exactly one pending entry, proving the `(recurring_id, due_on)` idempotency key (research R7), same file as T063
- [ ] T065 [P] [US6] `TransactionFormViewModelTest` — saving with "make it recurring" writes a `recurring_templates` row and **no** duplicate immediate transaction, citing `MNY-FLOW-002`, in the T024 file
- [ ] T066 [P] [US6] `RecurringViewModelTest` — MONTHLY IN/OUT totals, NEXT 30 DAYS ordered by date with correct auto-debit vs variable tags, and the PAUSED section showing its pause date, citing `MNY-UI-008`, in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/RecurringViewModelTest.kt`

### Implementation for User Story 6

- [ ] T067 [US6] Implement `RecurringRepository` and `SuggestionRepository` — schedule evaluation, materialise-on-open for `next_run <= today` and not paused, `next_run` advance, pause/resume (depends on T063, T064), in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/`
- [ ] T068 [US6] Add the "make it recurring" toggle + schedule editor to D3, and the Make-recurring entry point from D4 (T050's second case) (depends on T065), in `apps/finance/feature/money/money/TransactionFormScreen.kt` and `apps/finance/feature/money/money/TransactionDetailScreen.kt`
- [ ] T069 [US6] Build `RecurringScreen` (D9) — review banner with the pending count, MONTHLY IN/OUT, NEXT 30 DAYS dated list, PAUSED section, pause/resume actions, in `apps/finance/feature/money/money/RecurringScreen.kt`
- [ ] T070 [US6] Build the recurring review list — T012's `SuggestedRow` (dashed until accepted); Accept writes the transaction with `source = 'RECURRING'` and an `ACCEPTED_FROM_RECURRING` event, Dismiss writes nothing (FR-029). Scoped to recurring suggestions only — the shared queue that also handles SMS/AA sources is Phase 7 (spec.md Assumptions), in `apps/finance/feature/money/money/RecurringReviewScreen.kt`
- [ ] T071 [US6] Add the full state set to D9 and the review list (FR-032), in `apps/finance/feature/money/money/RecurringScreen.kt` and `apps/finance/feature/money/money/RecurringReviewScreen.kt`

**Checkpoint**: all six stories independently functional — Phase 3 feature-complete.

---

## Phase 9: Polish & Cross-Cutting (QA close, coverage ratchet, Sec, checkpoint)

**Purpose**: the module-standard doc's steps 5–7 (§4) — QA closes rows, coverage is measured and
ratcheted, Sec re-passes, merge gate.

- [ ] T072 [P] [QA] Close every `MNY-*` row in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §4 as its test lands, and update the §13 coverage-summary table. Two rows close **partially, with a stated reason**: `MNY-UI-006`'s budget-impact clause (needs Phase 4's `budgets`) and `MNY-FLOW-003` (goal link — needs Phase 4's `goals`). Run the manual 3-tap timing check for `MNY-UI-001`
- [ ] T073 [P] [Sec] DPDP/secrets/RLS pass: verify `delete_my_data()` removes rows from all six new tables on a dev-project erasure test, that `mask` never stores more than the last 4 digits, and that no receipt path or payee text reaches any log or crash report
- [ ] T074 [Sec] Consent-off test — with "Sync my financial records" off, zero PostgREST requests leave the device from any Money surface (NFR-1, constitution Article VIII)
- [ ] T075 Run `./gradlew regressionCheck` — all tests green, `jacocoAggregatedReport` produced, `jacocoCoverageVerification` passing at the current floor (Article X)
- [ ] T076 Run `./gradlew checkTrackerMoneyPrecision` — no `Double`/`Float` on any money-bearing field in `tracker/`, citing `MNY-NFR-001` (Article VII)
- [ ] T077 Read the measured numbers out of `build/reports/jacoco/jacocoAggregatedReport/jacocoAggregatedReport.xml` (or `python scripts/ci/regression_summary.py`) and record, in the QA catalog's §13 coverage-summary table: the merged line-coverage %, `:apps:finance:feature:money`'s own %, and `:apps:finance:data`'s % before vs after this phase. Repository/ViewModel logic is the JVM-testable surface and is where this phase's tests land; Compose screen files are not exercised by the JVM gate, so a feature module's own number is expected to sit well below its logic-only coverage — record both rather than reporting one and implying the other
- [ ] T078 Ratchet `globalLineFloor` in `build.gradle.kts` (root) up to just under the newly measured merged coverage, and extend its explanatory comment with this phase's number the way the existing comment already tracks its history (baseline ~6.7% → ~9.9% → this phase). **Never above measured** — Article X / ADR-0013: the floor is a non-regression ratchet, not a target. If merged coverage did **not** rise, leave the floor untouched and state why in the checkpoint note rather than forcing it
- [ ] T079 Confirm the module is named in coverage reporting, not lumped into `(other)` — run `python scripts/ci/regression_summary.py` locally and check `:apps:finance:feature:money` appears as its own row (this is T005's second half paying off; `onboarding` is the existing counter-example — it is in `coveredModules` but missing from `_FEATURES`, so it reports as `(other)` today)
- [ ] T080 [P] Run `python scripts/db/gen_schema_docs.py equiv` and `... docs --check`, and regenerate `web/src/shared/types/database.ts` with `supabase gen types typescript --schema public,finance` (a schema omitted from that flag silently loses typed coverage, ADR-0033)
- [ ] T081 Walk all 12 scenarios in `apps/finance/specs/002-money-tab/quickstart.md` end-to-end on a device/emulator
- [ ] T082 [P] Add `apps/finance/feature/money/money/README.md` (screens, ViewModels, data deps, flag key — the convention every other feature module follows) and link it from `apps/finance/FEATURES.md`; update `apps/finance/CLAUDE.md`'s module list so `money` no longer reads "not yet created"
- [ ] T083 Bump the minor version in `platform/versions.json` (new feature module) and update the implementation plan's §7 tracking table row for Phase 3 to "shipped"

**Checkpoint**: `regressionCheck` green, coverage measured and the floor ratcheted (or explicitly
held with a reason), every catalog row closed or explicitly deferred with a stated reason
(constitution Development Workflow step 7) — Phase 3 merge-ready.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T005 (coverage wiring) must land before any story's tests,
  or those tests run outside the merged report and the floor check measures nothing.
- **Foundational (Phase 2)**: depends on Setup — BLOCKS every user story. T006 → T007/T008 → T009 →
  T010 is a hard chain (views and functions reference the tables; the migration is generated last).
  T011 depends on T010 having been applied to dev.
- **User Stories (Phases 3–8)**: all depend on Foundational.
  - US1 (P1) has no dependency on the other five.
  - US2 (P2) extends US1's ledger screen — same files, so it follows US1 rather than running beside it.
  - US3 (P3) is fully parallel to US1/US2: accounts share no screen or ViewModel with the ledger.
    (US1 needs *an* account to exist; test fixtures satisfy that — not a hard dependency on D6.)
  - US4 (P4) needs transactions to exist (US1) but is otherwise independent.
  - US5 (P5) is parallel to everything except that its share/spend figures read US1's data.
  - US6 (P6) depends on US1's D3 form (T029) for its toggle, and on US4 for the Make-recurring entry
    point (T050/T068).
- **Polish (Phase 9)**: depends on however many of Phases 3–8 are in this merge. Within it,
  T075 → T077 → T078 is ordered (measure before ratcheting), and T077 needs the report T075 produces.

### Parallel Opportunities

- T012–T016 (five component tasks) plus T017 and T019 run in parallel — seven different files.
- T007 and T008 run in parallel after T006.
- Within each story, all `[P]` test tasks run in parallel with each other, before that story's
  implementation tasks start.
- US1 (Phase 3) and US3 (Phase 5) can be built in parallel by two engineers once Phase 2 closes.
- US5 (Phase 7, categories) is the most isolated story — it touches one screen and one repository.

## Implementation Strategy

### MVP first

Phase 1 → Phase 2 → Phase 3 (US1) → **stop, validate** against spec.md Story 1's Independent Test.
That alone is a usable expense tracker: record and see. Everything after it improves reading,
accounts, trust and automation, none of which is required to demo.

### Incremental delivery

Phase 1+2 → US1 (MVP) → US2 → US3 → US4 → US5 → US6 → Phase 9. Each story lands as its own
reviewable increment; the implementation plan's Phase 3 checkpoint (§7) is satisfied only once
Phase 9 closes.

### Suggested cut lines if the phase must ship smaller

US6 (recurring) is the cleanest cut — it is the only story that writes on the user's behalf and the
only one whose absence loses no already-entered data. US5 (categories) is the second: rename/merge
can wait if the seeded category set is adequate. Cutting US3 (accounts) is **not** viable — the
ledger has nowhere to record against without it, and Home's deferred card-bill row (FR-034) rides
on it. Cutting a story does **not** license skipping T005/T077/T078 — a smaller phase still measures
and ratchets what it actually landed.