---

description: "Task list for 008-money-tab-ux-remediation"
---

# Tasks: Money tab — usability remediation

**Input**: Design documents from `apps/finance/specs/008-money-tab-ux-remediation/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: **REQUIRED, not optional.** Constitution Article I is non-negotiable — RED → GREEN →
REFACTOR, no implementation code before its failing test exists, and every test cites the QA catalog
scenario id it satisfies. Article II additionally requires the catalog rows to exist *before* any
task starts, which is why Phase 2 is what it is.

**Organization**: Grouped by user story. Phase order follows [plan.md](plan.md)'s slice order, which
sequences by user impact and respects two hard prerequisites (the audit-trigger migration before
US5; the picker components before US9).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different files, no dependency on an incomplete task
- **[Story]**: US1..US10, matching [spec.md](spec.md)
- Every task names its exact file path

## Path conventions

| Area | Path |
|---|---|
| Money screens and view models | `apps/finance/feature/money/money/src/main/java/com/dhruv/finance/money/` |
| Money tests | `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/` |
| Shared fakes | `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/FakeMoneyRepositories.kt` |
| Tracker data layer | `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/` |
| Data-layer tests | `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/` |
| Shared components | `libs/core/src/main/kotlin/com/dhruv/core/ui/components/` |
| Navigation contract | `libs/core/src/main/kotlin/com/dhruv/core/navigation/` |
| App shell | `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt` |
| Declarative schema | `supabase/schemas/finance/30_functions/` |
| QA catalog | `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` |

---

## Phase 1: Setup

**Purpose**: establish the baseline this work is measured against.

- [ ] T001 Record the pre-change baseline — run `./gradlew regressionCheck` and note the merged line-coverage percentage and test count in this file's Notes section, so the coverage floor can be raised deliberately at closure rather than guessed
- [ ] T002 [P] Confirm the current coverage floor value in `build.gradle.kts` (root) and leave it unchanged until T137

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Article II's gate — QA scenario rows exist and are reviewed against the spec before any
implementation task starts.

**⚠️ CRITICAL**: No user story work may begin until T003 is complete. This is the constitutional gate,
not a formality: `002` shipped six requirements that were never built, and rows-before-code is the
check that would have caught it.

- [ ] T003 Add `MNY-*` scenario rows for every requirement in [spec.md](spec.md) (FR-201..FR-241) to `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §4, reviewed against the spec's acceptance scenarios — every later test task cites one of these ids
- [ ] T004 [P] Extend `FakeMoneyRepositories.kt` with the fakes the new contracts need: a failure-injecting create that can simulate a duplicate rejection ([contracts/write-identity.md](contracts/write-identity.md) WRITE-4), a batch create recorder (WRITE-6), and a seeding recorder ([contracts/first-run-seeding.md](contracts/first-run-seeding.md) SEED-3)

**Checkpoint**: catalog rows exist, fakes support the new contracts — user stories may begin.

---

## Phase 3: User Story 1 — Recording money never dead-ends (P1) 🎯 MVP

**Goal**: every save attempt produces a visible outcome — saved, a named missing field, or a
retryable failure. No silent no-ops.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 1 — attempt a save with a missing field,
then offline, then successfully; confirm three distinct visible results and exactly one transaction
after the retry.

### Tests for User Story 1 (write first, confirm they FAIL)

- [ ] T005 [P] [US1] Test: save with a missing field sets a named validation message and writes nothing (WRITE-1) in `QuickAddViewModelTest.kt`
- [ ] T006 [P] [US1] Test: a failed save surfaces a retryable failure and preserves every entered value (WRITE-2) in `QuickAddViewModelTest.kt`
- [ ] T007 [P] [US1] Test: a retry rejected as a duplicate reports **saved** and resolves to the existing record (WRITE-4) in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/TransactionRepositoryTest.kt`
- [ ] T008 [P] [US1] Test: a duplicate-rejected retry never overwrites the existing record (WRITE-5) in `TransactionRepositoryTest.kt`
- [ ] T009 [P] [US1] Test: the transfer type cannot be selected in quick entry and no transfer save is ever attempted from it (FR-205) in `QuickAddViewModelTest.kt`

### Implementation for User Story 1

- [ ] T010 [US1] Add `validationError` to `QuickAddUiState` and replace `save()`'s silent early-returns with named messages, mirroring `TransactionFormViewModel`'s existing wording, in `QuickAddViewModel.kt`
- [ ] T011 [US1] Add a `saveError` field to `QuickAddUiState` set on failure, so a failed save is surfaced by the sheet rather than only reported to a `featureError` flow nothing collects, in `QuickAddViewModel.kt`
- [ ] T012 [US1] Render the validation and failure messages plus a retry action in `QuickAddSheet.kt`, using `NxTextField`'s existing error treatment and the design system's error copy rules (say what to do, never exception text)
- [ ] T013 [US1] Show the transfer segment as visibly unavailable with helper text pointing at the full form, and make it unselectable, in `QuickAddSheet.kt` (FR-205)
- [ ] T014 [US1] Add duplicate-rejection-means-saved handling in one shared place on the tracker write path, resolving to the already-written row by its request id, in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/TransactionRepository.kt` (WRITE-4, research R8)
- [ ] T015 [US1] Collect `QuickAddViewModel.featureError` where the sheet is hosted so a thrown error reaches a surface, in `LedgerScreen.kt` — today it is collected nowhere
- [ ] T016 [US1] Move the note field above the keypad, or otherwise ensure the keypad and Save stay reachable while the note field holds focus, in `QuickAddSheet.kt` (FR-223)
- [ ] T017 [US1] Add a content description conveying the amount's meaning to the hero amount in `QuickAddSheet.kt` (FR-222)

**Checkpoint**: the module's primary flow can no longer fail invisibly. This alone is a shippable
increment.

---

## Phase 4: User Story 3 — Every screen says where you are and how to leave (P1)

**Goal**: a visible back control and title on every drill-in, and the phone's back gesture behaving
identically to it — including raising the unsaved-changes confirmation.

**⚠️ Do not split this phase.** Shipping the visible control without the guard makes the silent
discard easier to reach, because it invites use of screens people currently avoid
([contracts/back-navigation.md](contracts/back-navigation.md), anti-requirement).

**Independent Test**: [quickstart.md](quickstart.md) Scenario 2.

### Tests for User Story 3 (write first, confirm they FAIL)

- [ ] T018 [P] [US3] Test: on a dirty form the back trigger raises the discard confirmation and does not leave (BACK-3) in `TransactionFormViewModelTest.kt`
- [ ] T019 [P] [US3] Test: confirming discard leaves; dismissing keeps the input intact (BACK-4) in `TransactionFormViewModelTest.kt`
- [ ] T020 [P] [US3] Test: on a clean form the back trigger leaves immediately with no confirmation (BACK-5) in `TransactionFormViewModelTest.kt`
- [ ] T021 [P] [US3] Test: the account form raises the same confirmation — it currently has none by any route (BACK-7) in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/AccountFormViewModelTest.kt`

### Implementation for User Story 3

- [ ] T022 [P] [US3] Add `NxTopBar` with a title and back control to `AccountsScreen.kt`, and accept an `onBack` callback (FR-208)
- [ ] T023 [P] [US3] Same for `AccountDetailScreen.kt`
- [ ] T024 [P] [US3] Same for `CategoriesScreen.kt`
- [ ] T025 [P] [US3] Same for `RecurringScreen.kt`
- [ ] T026 [P] [US3] Same for `RecurringReviewScreen.kt`
- [ ] T027 [US3] Wire the five new `onBack` callbacks to `popBackStack()` in `MainActivity.kt`'s Money nav graph
- [ ] T028 [US3] Add a conditionally-enabled back handler to `TransactionFormScreen.kt` that calls the same `DiscardGuard.attemptDismiss()` its close control calls — enabled only while dirty, so a clean form still returns to its parent (FR-208b, research R1)
- [ ] T029 [US3] Add a discard guard plus the same back handler to `AccountFormScreen.kt`, which has no confirmation today by any route (FR-208c)
- [ ] T030 [US3] Verify back with a picker or confirmation open dismisses only that overlay, not the screen, in `TransactionFormScreen.kt` and `QuickAddSheet.kt` (BACK-6)

**Checkpoint**: no route out of a Money screen can discard unsaved work without asking.

---

## Phase 5: User Story 2 — A signed-out person can sign in (P1)

**Goal**: the three dead "Sign in" buttons reach the real sign-in flow.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 3.

### Tests for User Story 2 (write first, confirm they FAIL)

- [ ] T031 [P] [US2] Test: the ledger's signed-out state exposes a sign-in action that invokes its callback (FR-206) in `LedgerViewModelTest.kt`
- [ ] T032 [P] [US2] Test: after a session becomes available the screen reloads without an explicit refresh call from the caller (FR-207) in `LedgerViewModelTest.kt`

### Implementation for User Story 2

- [ ] T033 [US2] Pass a real sign-in callback to `LedgerScreen`'s `onSignInRequested`, to `AccountsScreen`'s `onSignIn`, and to `AccountDetailScreen`'s `onSignIn` in `MainActivity.kt` — all three are currently empty lambdas or unset defaults
- [ ] T034 [US2] Route those callbacks to the app's existing sign-in entry point (the same one Settings' Account screen uses), without duplicating the sign-in flow, in `MainActivity.kt`
- [ ] T035 [US2] Ensure the originating screen reloads on session change so a signed-in person sees data without restarting, in `LedgerViewModel.kt`, `AccountsViewModel.kt` and `AccountDetailViewModel.kt`

**Checkpoint**: a signed-out person is no longer trapped on the first screen of the module.

---

## Phase 6: User Story 10 — A brand-new account records its first expense immediately (P1)

**Goal**: seeded starter categories and one cash account, so first use cannot fail for want of setup.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 4 — **requires a fresh account**; it
cannot be run twice on the same one, by design.

### Tests for User Story 10 (write first, confirm they FAIL)

- [ ] T036 [P] [US10] Test: seeding creates the starter categories and one cash account for a person with none (SEED-1) in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/repo/CategoryRepositoryTest.kt`
- [ ] T037 [P] [US10] Test: running seeding twice produces no duplicates (SEED-3) in the same file
- [ ] T038 [P] [US10] Test: a deleted seeded row is not recreated on the next run (SEED-4) in the same file
- [ ] T039 [P] [US10] Test: a person with existing rows has nothing seeded and nothing changed (SEED-5) in the same file
- [ ] T040 [P] [US10] Test: seeding interrupted after some rows completes the remainder without duplicating (SEED-6) in the same file
- [ ] T041 [P] [US10] Test: seeding does not run before consent (SEED-8) in the same file

### Implementation for User Story 10

- [ ] T042 [US10] Add the starter category names and the cash account name as string resources in `apps/finance/feature/money/money/src/main/res/values/strings.xml`, referenced from `MoneyConfig.kt` — product data, never inline (Article V)
- [ ] T043 [US10] Implement seeding with a deterministic per-person request id per seeded row, alongside the existing reserved-category call rather than as a parallel mechanism, in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/CategoryRepository.kt` and `AccountRepository.kt` (research R5)
- [ ] T044 [US10] Call seeding from the same entry points that already ensure the reserved categories, in `QuickAddViewModel.kt` and `TransactionFormViewModel.kt`
- [ ] T045 [US10] Confirm seeded rows are ordinary — renameable, editable, deletable, with no special-casing anywhere — by inspection of `CategoriesScreen.kt` and `AccountsScreen.kt` (SEED-7, FR-239)

**Checkpoint**: all four P1s are closed. This is the point at which the module is genuinely usable by
someone who has never opened it.

---

## Phase 7: User Story 4 — Setup lives in Settings (P2)

**Goal**: accounts and categories managed from Settings; recurring stays in the Money tab; the
ledger's content row keeps only search and filter.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 5.

### Tests for User Story 4 (write first, confirm they FAIL)

- [ ] T046 [P] [US4] Test: the Money settings contribution exposes navigable accounts and categories rows, not an informational count (FR-209a) in `apps/finance/feature/money/money/src/test/java/com/dhruv/finance/money/settings/MoneySettingsContributionTest.kt`
- [ ] T047 [P] [US4] Test: the two new navigation targets resolve to the accounts and categories destinations in `libs/core/src/test/kotlin/com/dhruv/core/navigation/NavTargetTest.kt`

### Implementation for User Story 4

- [ ] T048 [P] [US4] Add `OpenAccounts` and `OpenCategories` cases to `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt` (research R4)
- [ ] T049 [US4] Replace the categories `SettingsRow.Info` with `SettingsRow.Navigate` and add an accounts row, with their string resources, in `settings/MoneySettingsContribution.kt` and `res/values/strings.xml`
- [ ] T050 [US4] Resolve the two new navigation targets to the existing Money screens in `MainActivity.kt` — the screens do not move (Article III)
- [ ] T051 [US4] Add a top bar to the Money tab root carrying the tab name and an overflow holding recurring only, and remove the accounts, categories and recurring icon buttons from the content row, in `LedgerScreen.kt` (FR-209, FR-210)
- [ ] T052 [US4] Confirm the tab root shows no back control (navigation law N1) in `LedgerScreen.kt`
- [ ] T053 [US4] Make the Money empty states that cannot proceed for want of an account or category navigate straight to setup rather than describing it, in `LedgerScreen.kt` (FR-209b)
- [ ] T054 [US4] Verify at the narrowest supported width that the search field shows its full placeholder and nothing is clipped, in `LedgerScreen.kt` (FR-211)

**Checkpoint**: accounts and categories have exactly one place they are managed from.

---

## Phase 8: User Story 5 — Fixing a mistake does not mean destroying the record (P2)

**Goal**: edit a transaction, an account and a recurring rule, with history that records what changed.

**⚠️ Prerequisite**: T055 (the migration) must land before T060, or a type change appends no history
and FR-212 is false for exactly the edits this story adds (research R9).

**Independent Test**: [quickstart.md](quickstart.md) Scenario 6.

### Schema (blocking, do first)

- [ ] T055 [US5] Extend the transaction audit trigger to track `type`, `to_account_id` and receipt attach/replace/remove in `supabase/schemas/finance/30_functions/fn_transaction_audit.sql` — adding to the existing `EDITED` detail, introducing no new event kind and renaming no shipped constant (Article IX)
- [ ] T056 [US5] Generate the migration from the edited schema object per ADR-0032, review the generated SQL, and commit both files under `supabase/migrations/`
- [ ] T057 [US5] Run `python scripts/db/gen_schema_docs.py equiv` and `docs --check`, and regenerate `supabase/SCHEMA.md`

### Tests for User Story 5 (write first, confirm they FAIL)

- [ ] T058 [P] [US5] Test: editing a transaction updates it and creates no second transaction (FR-212) in `TransactionFormViewModelTest.kt`
- [ ] T059 [P] [US5] Test: changing type to transfer requires a destination, clears the category, and blocks save until complete (FR-228) in `TransactionFormViewModelTest.kt`
- [ ] T060 [P] [US5] Test: changing type from transfer requires a category and clears the destination (FR-228) in `TransactionFormViewModelTest.kt`
- [ ] T061 [P] [US5] Test: editing an account applies under the same validation as creation (FR-214) in `AccountFormViewModelTest.kt`
- [ ] T062 [P] [US5] Test: editing a recurring rule leaves entries it already produced unchanged (FR-215, FR-216) in `RecurringViewModelTest.kt`

### Implementation for User Story 5

- [ ] T063 [US5] Add an edit affordance to `TransactionDetailScreen.kt` that opens the full form pre-filled, discoverable without entering a destructive menu (FR-212)
- [ ] T064 [US5] Support edit mode in `TransactionFormViewModel.kt` — load an existing transaction, update rather than create, and reuse the existing dirty tracking
- [ ] T065 [US5] Implement the type-change field swap so the required fields change with the type and the discarded field's value is not silently retained, in `TransactionFormViewModel.kt` and `TransactionFormScreen.kt` (FR-228)
- [ ] T066 [US5] Add an edit affordance to `AccountDetailScreen.kt` and pass the existing account into the form route in `MainActivity.kt` — the form already supports an `existing` argument and every route passes null today (FR-214)
- [ ] T067 [US5] Add an edit action to the recurring row menu in `RecurringScreen.kt`, wired to the existing `RecurringViewModel.edit(...)` (FR-215)

**Checkpoint**: a mistake can be corrected without deleting the record or leaving a wrong row behind.

---

## Phase 9: User Story 7 — The module tells the truth about its own state (P3)

**Goal**: consistent offline and signed-out states, human dates, and destructive actions that look
destructive.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 7.

### Tests for User Story 7 (write first, confirm they FAIL)

- [ ] T068 [P] [US7] Test: the ledger exposes an offline state distinct from a generic error (FR-219) in `LedgerViewModelTest.kt`
- [ ] T069 [P] [US7] Test: the recurring review queue exposes offline and signed-out states (FR-219) in `RecurringReviewViewModelTest.kt`

### Implementation for User Story 7

- [ ] T070 [US7] Add an `Offline` case to `LedgerUiState` and render `OfflineStateCard`, matching the treatment `AccountsUiState` already has, in `LedgerViewModel.kt` and `LedgerScreen.kt`
- [ ] T071 [US7] Add `Offline` and `SignedOut` cases to `RecurringReviewUiState` and render them in `RecurringReviewViewModel.kt` and `RecurringReviewScreen.kt`
- [ ] T072 [P] [US7] Format the due date as a human date rather than the raw stored value in `RecurringReviewScreen.kt` (FR-220)
- [ ] T073 [US7] Separate the destructive account action from the everyday actions with its own labelled section in `AccountDetailScreen.kt` — the confirmation dialog is already correct and stays (FR-221)

**Checkpoint**: every Money screen answers honestly about connectivity, session and consequence.

---

## Phase 10: User Story 9 — Record what happened, when it happened (P2)

**Goal**: date and time entry, and receipt attachment.

**⚠️ Prerequisite**: T074/T075 (the shared pickers) before T078.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 8.

### Shared components (blocking, do first)

- [ ] T074 [P] [US9] Add `NxDatePickerSheet` wrapping the Material 3 date picker behind project tokens in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/NxDatePickerSheet.kt` (research R2, Article VI)
- [ ] T075 [P] [US9] Add `NxTimePickerSheet` the same way in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/NxTimePickerSheet.kt`

### Tests for User Story 9 (write first, confirm they FAIL)

- [ ] T076 [P] [US9] Test: a set date and time is what gets recorded, not the save instant (FR-229) in `TransactionFormViewModelTest.kt`
- [ ] T077 [P] [US9] Test: a future date is refused with a message; a same-day earlier time is accepted (FR-230) in `TransactionFormViewModelTest.kt`

### Implementation for User Story 9

- [ ] T078 [US9] Add `occurredAt` to `TransactionFormUiState` with a setter, replace the hardcoded save-time value, and render date and time fields using the two new sheets, in `TransactionFormViewModel.kt` and `TransactionFormScreen.kt` (FR-229)
- [ ] T079 [US9] Add future-date rejection to the form's validation in `TransactionFormViewModel.kt` (FR-230)
- [ ] T080 [US9] Confirm a back-dated transaction lands in its own day group and month, and that a date change across months updates both summaries, in `LedgerViewModel.kt` (FR-231)
- [ ] T081 [US9] Add a receipt field to the form using the system photo picker — no permission, no manifest change — writing through the existing `ReceiptStore`, in `TransactionFormScreen.kt` and `TransactionFormViewModel.kt` (FR-232, research R3)
- [ ] T082 [US9] Support replacing and removing a receipt, deleting the stored image on removal via `ReceiptStore.delete`, in `TransactionFormViewModel.kt` (FR-233)
- [ ] T083 [P] [US9] Test: a receipt path never appears in an outbound tracker payload (FR-234) in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/mapper/TransactionMapperTest.kt`

**Checkpoint**: a transaction can describe when it actually happened, with proof attached.

---

## Phase 11: User Story 6 — A filter you use often can be kept (P3)

**Goal**: save, list, apply, rename and remove named filter views.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 9.

### Tests for User Story 6 (write first, confirm they FAIL)

- [ ] T084 [P] [US6] Test: saving a filter under a name lists it for re-use (FR-217) in `LedgerViewModelTest.kt`
- [ ] T085 [P] [US6] Test: applying a saved view produces the same result set as the hand-built filter (FR-218) in `LedgerViewModelTest.kt`
- [ ] T086 [P] [US6] Test: renaming and removing a saved view persists and affects no transaction (FR-217) in `LedgerViewModelTest.kt`

### Implementation for User Story 6

- [ ] T087 [US6] Add saved-view state and actions to `LedgerViewModel.kt`, consuming the existing `SavedViewRepository` unchanged (research R10)
- [ ] T088 [US6] Add save, apply and manage affordances to `LedgerFilterSheet.kt` — the sheet builds a candidate filter today and discards it on dismiss
- [ ] T089 [US6] Handle a saved view whose category was since merged or deleted by degrading honestly rather than silently returning a different set, in `LedgerViewModel.kt`

**Checkpoint**: a repeated filter is a one-tap action.

---

## Phase 12: User Story 8 — One payment, more than one category (P3)

**Goal**: divide a payment across categories, written atomically, counted once.

**Independent Test**: [quickstart.md](quickstart.md) Scenario 10.

### Tests for User Story 8 (write first, confirm they FAIL)

- [ ] T090 [P] [US8] Test: a split posts one write and a rejected write leaves zero parts (WRITE-6) in `TransactionRepositoryTest.kt`
- [ ] T091 [P] [US8] Test: a split whose parts do not sum to the total cannot be saved (WRITE-7) in `TransactionFormViewModelTest.kt`
- [ ] T092 [P] [US8] Test: a one-part split is written as an ordinary transaction with no split identity (WRITE-8) in `TransactionFormViewModelTest.kt`
- [ ] T093 [P] [US8] Test: a retried split reuses one identity for the whole split, never one per part (WRITE-9) in `TransactionFormViewModelTest.kt`

### Implementation for User Story 8

- [ ] T094 [US8] Add an array-body batch create to `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/net/MoneyApi.kt` and a batch create to `TransactionRepository.kt`, so a split is one statement and therefore one transaction (research R6)
- [ ] T095 [US8] Add split parts to `TransactionFormUiState` with add, remove and edit actions in `TransactionFormViewModel.kt`
- [ ] T096 [US8] Enforce the balance rule — parts sum exactly to the stated total, shortfall or excess shown, save blocked until balanced — in `TransactionFormViewModel.kt` (FR-225)
- [ ] T097 [US8] Render the split editor in `TransactionFormScreen.kt` using existing components, adding no parallel component (Article VI)
- [ ] T098 [US8] Show a part's membership of a larger payment and offer to show the siblings, in `TransactionDetailScreen.kt` (FR-226)
- [ ] T099 [US8] Handle a split part edited into a transfer — a transfer holds no category, so it cannot remain part of a category split; tell the person what happens to the rest (spec Edge Cases)
- [ ] T100 [US8] Verify each part counts once against its account and each category is charged only its part, across the ledger, month summary and running balance (SC-211)

**Checkpoint**: all ten user stories are independently functional.

---

## Phase 13: Retry identity residuals

**Purpose**: close the two paths `002` FR-036 explicitly left open. Small, independent, and easy to
lose track of — hence its own phase rather than being folded into a story.

- [ ] T101 [P] Test: a retry reuses the first attempt's identity on the "make it recurring" write (WRITE-3) in `TransactionFormViewModelTest.kt`
- [ ] T102 [P] Test: a retry reuses the first attempt's identity when accepting a pending entry (WRITE-3) in `RecurringReviewViewModelTest.kt`
- [ ] T103 Add a `requestId` parameter to `RecurringRepository.createFromTransaction(...)` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/RecurringRepository.kt` — the table already has the unique column, so no migration (research R7)
- [ ] T104 Add a `requestId` parameter to `SuggestionRepository.accept(...)` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/repo/SuggestionRepository.kt`
- [ ] T105 Mint and reuse the identity per user action at both call sites, in `TransactionFormViewModel.kt` and `RecurringReviewViewModel.kt`

---

## Phase 14: Polish & Closure

**Purpose**: the tracking rule this module's documentation requires. Its absence in `002` is why six
requirements went undelivered while the phase read as shipped — so this phase is the point, not
paperwork.

### Accessibility and responsive pass

- [ ] T106 [P] Screen-reader pass over every Money screen: every icon-only action, state card and the quick-entry amount announced meaningfully (SC-209)
- [ ] T107 [P] Render every Money screen at the narrowest supported width and the largest supported text size; confirm no clipping or overlap (SC-210)

### Device validation

- [ ] T108 Run [quickstart.md](quickstart.md) Scenarios 1–4 (the four P1s) on a device and record the outcome
- [ ] T109 Run [quickstart.md](quickstart.md) Scenario 4 on a **fresh account** — it cannot be re-run on a seeded one
- [ ] T110 Run [quickstart.md](quickstart.md) Scenarios 5–10 on a device and record the outcome

### Documentation closure

- [ ] T111 Fill in `apps/finance/specs/002-money-tab/spec.md` § "Implementation record" — what shipped, what deviated, what was deferred, each row naming the requirement whose behaviour was not delivered (FR-235)
- [ ] T112 Fill in this spec's own § "Implementation record" in [spec.md](spec.md)
- [ ] T113 [P] Update `apps/finance/FEATURES.md` and `apps/finance/feature/money/money/README.md`
- [ ] T114 [P] Add an entry to the root `CHANGELOG.md`
- [ ] T115 [P] Add `NxDatePickerSheet` and `NxTimePickerSheet` to `platform/DESIGN-SYSTEM.md` §5.1 — **only now that they exist**, per that section's own rule
- [ ] T116 [P] Update the design-v1 implementation plan §7's tracking table row for this spec in `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`
- [ ] T117 Mark the `MNY-*` catalog rows added in T003 as closed or explicitly deferred, with a recount, in the QA catalog

### Gate

- [ ] T118 Run `./gradlew regressionCheck` and confirm green
- [ ] T119 Raise the coverage floor in the root `build.gradle.kts` to the newly measured value, deliberately and only after the tests above have landed (ADR-0013's ratchet rule)

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: no dependencies
- **Phase 2 (Foundational)**: **blocks every story** — Article II's rows-before-code gate
- **Phases 3–12 (Stories)**: all depend on Phase 2; otherwise independent of each other, with the
  exceptions below
- **Phase 13 (Retry residuals)**: independent; can run any time after Phase 2
- **Phase 14 (Closure)**: depends on every story that is actually being shipped

### Hard prerequisites within stories

| Blocked | Blocker | Why |
|---|---|---|
| T063–T067 (US5 edit surfaces) | T055–T057 (audit trigger migration) | Without it a type change appends no history and FR-212 is false |
| T078–T082 (US9 date and receipt) | T074, T075 (picker components) | Article VI — a screen may not hand-roll them |
| T051 (Money top bar) | T048–T050 (navigation targets and Settings rows) | The overflow loses two entries only once Settings gains them |
| T094 (batch write) | — | Independent; must land before T095–T097 are meaningful |

### Do not split

**Phase 4 in its entirety.** T022–T027 (visible back controls) and T028–T029 (the guard) ship
together. The control alone makes the silent discard easier to reach.

### Parallel opportunities

- T005–T009 (US1 tests) — five different assertions, two files
- T022–T026 (five screens' top bars) — five different files, fully parallel
- T036–T041 (US10 tests) — same file, so serialize; the *implementation* T042/T043 is parallel
- T074, T075 (the two picker components) — different files
- T090–T093, T101–T102 (test batches) — different files
- T113–T116 (documentation) — different files
- Whole stories: US2, US7, US6 have no cross-story dependency and can proceed alongside others

### Within each story

Tests first and failing (Article I) → data layer → view model → screen → wiring.

---

## Parallel Example: User Story 3

```bash
# The five screens' top bars are five separate files — fully parallel:
Task: "Add NxTopBar with title and back to AccountsScreen.kt"
Task: "Add NxTopBar with title and back to AccountDetailScreen.kt"
Task: "Add NxTopBar with title and back to CategoriesScreen.kt"
Task: "Add NxTopBar with title and back to RecurringScreen.kt"
Task: "Add NxTopBar with title and back to RecurringReviewScreen.kt"

# Then serially, because they share MainActivity.kt and the guard files:
Task: "Wire the five onBack callbacks in MainActivity.kt"
Task: "Add the conditionally-enabled back handler to TransactionFormScreen.kt"
```

---

## Implementation Strategy

### MVP — the four P1s

Phases 1, 2, then 3 (US1), 4 (US3), 5 (US2), 6 (US10). **Stop and validate.**

That closes every defect a person hits in their first sixty seconds: a save that fails silently, a
back gesture that eats their typing, a sign-in button that does nothing, and a first run that cannot
save at all. Everything after this point is improvement rather than repair.

### Incremental delivery

Each phase is independently shippable and leaves `regressionCheck` green. A reasonable order after
the MVP: US4 (setup consolidation) → US5 (correction, once the migration lands) → US7 (honest states)
→ US9 (date and receipt) → US6 (saved views) → US8 (split).

Phase 13 can be slotted anywhere; it touches files the stories mostly do not.

### If work has to stop early

Stop **after a phase**, never inside Phase 4. Any completed phase leaves the module strictly better
than it was and fully green. Phase 14's documentation tasks are the exception — T111 in particular
should happen even if scope is cut, because an unfilled Implementation record is precisely how this
situation arose.

---

## Notes

- **T001 baseline (fill in when run)**: merged line coverage `____%`, `____` tests passing.
- `[P]` means different files with no incomplete dependency.
- Every test cites a QA catalog id from T003 — that citation is the Article I/II link, not decoration.
- Commit per task or per logical group; each phase checkpoint is a valid stopping point.
- Every finding this feature addresses was verified against shipped code, schema or navigation
  wiring — see [spec.md](spec.md) § Traceability and [research.md](research.md).
