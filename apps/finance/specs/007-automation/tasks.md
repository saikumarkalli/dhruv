---

description: "Task list for Automation (Phase 7)"
---

# Tasks: Automation (Phase 7)

**Input**: Design documents from `apps/finance/specs/007-automation/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: **REQUIRED, not optional.** Constitution Article I (Test-First) is NON-NEGOTIABLE in this
repo: RED → GREEN → REFACTOR, and every test cites the QA scenario ID it satisfies. The template's
"tests are optional" note does not apply here.

**Organization**: grouped by user story. Each story maps to a sub-phase from `plan.md`, which is the
unit that merges independently and ends green on `./gradlew regressionCheck`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallelizable — different files, no dependency on an incomplete task
- **[Story]**: US1–US8 from `spec.md`
- Every task names its exact file path

## Path conventions

| Area | Root |
|---|---|
| Feature module | `apps/finance/feature/shell/automation/src/main/java/com/dhruv/finance/automation/` |
| Feature tests | `apps/finance/feature/shell/automation/src/test/java/com/dhruv/finance/automation/` |
| Data module | `apps/finance/data/src/main/java/com/dhruv/finance/data/` |
| Data tests | `apps/finance/data/src/test/java/com/dhruv/finance/data/` |
| Shared components | `libs/core/src/main/kotlin/com/dhruv/core/ui/components/` |
| Postgres (declarative) | `supabase/schemas/finance/`, `supabase/schemas/public/` |

## QA scenario rows

Nine `AUT-*` rows exist (catalog §9). **Six do not** — the catalog predates five of the seven
2026-08-23 clarifications. QA writes each at step 2 of its own sub-phase, **before any code in that
slice** (Article II; `plan.md`'s Article II note). IDs reserved here so tasks can cite them:

| New row | Covers | Written before |
|---|---|---|
| `AUT-BR-004` | Never proposed twice, across rescan / re-enable / backlog | US1 |
| `AUT-UI-003` | Ignored list and restore | US1 |
| `AUT-BR-005` | Accept-all partition — the three skip buckets | US1 |
| `AUT-FLOW-005` | Freeze / unfreeze cycle on consent withdrawal and re-grant | US3 |
| `AUT-FLOW-006` | Periodic scan, missed window, backlog in date order | US3 |
| `AUT-BR-006` | Price-move predicate, all ten cases | US8 |

---

## Phase 1: Setup (shared infrastructure)

**Purpose**: the module and the flag must exist before any route, because Article IV requires every
route `FeatureHost`-wrapped with a flag entry. This precedes sub-phase 7a.

- [ ] T001 Create the Gradle module directory and `build.gradle.kts` at `apps/finance/feature/shell/automation/`, applying the `dhruv.android.library` convention plugin and depending on `:libs:core` and `:apps:finance:data` only
- [ ] T002 Register `include(":apps:finance:feature:automation")` and its `projectDir` remap to `apps/finance/feature/shell/automation` in `settings.gradle.kts`
- [ ] T003 Add `:apps:finance:feature:automation` to `coveredModules` in the root `build.gradle.kts` — a new module is silently excluded from the JaCoCo floor otherwise (Article X)
- [ ] T004 [P] Add `"automation": { "enabled": false, "minVersion": "1.0.0", "requiresConsent": true }` to `platform/feature-flags/dhruv-finance.json` — the key is genuinely absent today (research R12)
- [ ] T005 [P] Create `automation/di/AutomationModule.kt` with an empty Koin `module {}` and aggregate it in `apps/finance/app/src/main/java/com/dhruv/finance/app/CalculatorApplication.kt`
- [ ] T006 [P] Create `automation/ui/hub/AutomationConfig.kt` holding screen-level data — source table, bank-sender allowlist, scan cadence, default move threshold, duplicate window (Article V: never inline)
- [ ] T007 Create `apps/finance/feature/shell/automation/README.md` with the "not yet complete" preamble, and add the `automation` row to `apps/finance/FEATURES.md` as *planned*
- [ ] T008 Verify the module compiles and is picked up by the gate: `./gradlew :apps:finance:feature:automation:assembleDebug` and `./gradlew regressionCheck`

**Checkpoint**: module builds, flag exists and is off, nothing user-visible yet.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: the device-local store, the shared models and the merged-read contract. **No user story
can begin until this is complete.**

**⚠️ CRITICAL**: T009–T011 are the enum sets — they are TEXT-persisted and **append-only from birth**
(Article IX). Renaming a shipped constant silently reinterprets stored rows.

- [ ] T009 [P] Create `data/tracker/model/ProposalOrigin.kt` — `RECURRING`, `BANK_MESSAGE`, `PRICE_FEED`; `ACCOUNT_AGGREGATOR` reserved in a comment but **not** added (nothing produces it) — per data-model §4
- [ ] T010 [P] Create `data/tracker/model/ProposalKind.kt` — `TRANSACTION`, `VALUE_UPDATE` (FR-003a)
- [ ] T011 [P] Create `data/tracker/model/Proposal.kt` — the merged row model carrying origin, kind, status, source key, and both kinds' field sets per data-model §1
- [ ] T012 Create `data/automation/AutomationProposalEntity.kt` — the Room entity per data-model §1, with the unique index on `source_key`; annotate that it is device-local and never synced
- [ ] T013 [P] Create `data/automation/AutomationSeenKeyEntity.kt` — the retained-key table per data-model §1, hash and timestamp only
- [ ] T014 Create `data/automation/AutomationProposalDao.kt` with observe-pending, observe-ignored, insert-if-key-unseen, and status-transition queries
- [ ] T015 Bump `data/AppDatabase.kt` to **version 7**, register both entities, and add `MIGRATION_6_7` — do **not** touch `fallbackToDestructiveMigration`
- [ ] T016 [P] Write `FakeAutomationProposalDao` in `apps/finance/data/src/test/java/com/dhruv/finance/data/automation/FakeAutomationProposalDao.kt` — DAOs are exercised through a fake, **never** an in-memory Room database (Robolectric SQLite does not load on this toolchain)
- [ ] T017 [P] Create `data/tracker/dto/SuggestionDto.kt` and `data/tracker/mapper/ProposalMapper.kt` mapping Phase 3's `finance.suggestions` rows into `Proposal` with `origin = RECURRING`
- [ ] T018 Create `data/tracker/automation/SuggestionsApi.kt` — Retrofit interface for `finance.suggestions` read + status update, on the existing **consent-gated** `dataClient`; construct no new client (Article VIII)
- [ ] T019 Add a comment to `supabase/schemas/finance/10_tables/suggestions.sql` recording that `raw_text` **must never be written** — Phase 3 reserved it for this phase, and research R1 puts that data in Room instead so `AUT-BR-002` holds structurally

**Checkpoint**: the store and models exist; `regressionCheck` green; no screen yet.

---

## Phase 3: User Story 1 — One queue holds everything waiting (Priority: P1) 🎯 MVP

**Sub-phase 7a.** **Goal**: G2 plus the Ignored list, fed by Phase 3's recurring proposals alone — no
SMS, no permission, no new source. This discharges the deferral two phases are waiting on.

**Independent Test**: with the SMS source never enabled, seed recurring proposals, accept one and
ignore another, and confirm exactly one new transaction exists carrying the accepted values and an
audit trail naming its automated origin.

### QA — scenarios before code (Article II)

- [ ] T020 [US1] Write catalog rows `AUT-BR-004` (never twice), `AUT-UI-003` (Ignored/restore) and `AUT-BR-005` (accept-all partition) in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §9, reviewed against `spec.md` FR-008a–c and FR-009a–b

### Tests for User Story 1 — RED before GREEN ⚠️

- [ ] T021 [P] [US1] Write the failing table-driven test for `partitionForAcceptAll` in `.../automation/ui/queue/AcceptAllPartitionTest.kt` citing `AUT-BR-005` — every queue composition from contracts/review-queue.md §5, asserting zero duplicate-flagged and zero value-update rows are ever accepted
- [ ] T022 [P] [US1] Write the failing never-twice test in `apps/finance/data/src/test/java/com/dhruv/finance/data/automation/ProposalDedupeTest.kt` citing `AUT-BR-004` — a key seen in `automation_proposal` **or** `automation_seen_key` is skipped, whatever its terminal state
- [ ] T023 [P] [US1] Write the failing `ProposalRepositoryTest` in `.../data/tracker/automation/ProposalRepositoryTest.kt` citing `AUT-FLOW-001`/`AUT-FLOW-002` — accept writes exactly one transaction, ignore writes nothing, both against fakes
- [ ] T024 [P] [US1] Write the failing partial-failure test asserting a remote-store failure still emits local rows plus a retryable remote status, never an all-or-nothing error (contracts/review-queue.md §1)
- [ ] T025 [P] [US1] Write the failing `ReviewQueueViewModelTest` in `.../automation/ui/queue/ReviewQueueViewModelTest.kt` citing `AUT-UI-001` and `AUT-UI-003` — pending list, ignored list, restore round-trip, and all six screen states, using Turbine

### Implementation for User Story 1

- [ ] T026 [P] [US1] Build `SuggestedRow` in `libs/core/.../components/SuggestedRow.kt` — the dashed not-yet-accepted treatment (design-system §5.2 batch B4). **Skip if Phase 3's ledger already landed B4**; verify by symbol search first
- [ ] T027 [P] [US1] Build `DayGroupHeader` in `libs/core/.../components/DayGroupHeader.kt` — same batch, same skip-if-present rule (Phase 6 also claims it; whichever lands first owns it)
- [ ] T028 [US1] Implement `AcceptAllPartition.kt` in `.../automation/ui/queue/` as a pure function returning the accepted set plus the three skip buckets — turns T021 green
- [ ] T029 [US1] Implement the dedupe guard in `AutomationProposalDao`/repository so an insert checks both key tables — turns T022 green
- [ ] T030 [US1] Implement `data/tracker/automation/ProposalRepository.kt` — merges Room and `finance.suggestions` into one ordered list, routes every mutation back to the owning store, and models remote status separately (research R2) — turns T023 and T024 green
- [ ] T031 [US1] Implement accept for `kind = TRANSACTION`: insert `finance.transactions` with the automated source recorded, set status `ACCEPTED`. The "from an automated source" history row is written by Phase 3's existing audit trigger — do **not** write history here (FR-002)
- [ ] T032 [US1] Implement ignore and restore: status transitions, `decided_at`, and retention of the row as the Ignored list (FR-008, FR-008a, FR-008b)
- [ ] T033 [US1] Implement `ReviewQueueUiState.kt` and `ReviewQueueViewModel.kt` in `.../automation/ui/queue/`, with `crashReporter.setModule("automation")` in `init`, a `featureError` StateFlow, and one `performanceTracer.trace("automation_queue_load")` — turns T025 green
- [ ] T034 [US1] Implement `ReviewQueueScreen.kt` (G2) — day-grouped rows, per-row accept/ignore reachable without opening a detail screen (FR-012), accept-all with its result report (FR-009b), and both row kinds rendered distinguishably (FR-003a, SC-014)
- [ ] T035 [US1] Implement `IgnoredListScreen.kt` — G2's row component in a non-actionable variant plus Restore; inherits G2's states and copy rather than inventing its own (contracts/routes.md)
- [ ] T036 [US1] Wire both routes into the shell's detail-route navigation in `apps/finance/app/.../MainActivity.kt`, each wrapped in `FeatureHost("automation", …)`
- [ ] T037 [US1] Implement all six states on both screens per contracts/review-queue.md §7 — including the **remote-failure** case rendering local rows plus a retry, and offline accept that either completes or fails visibly with the row still pending
- [ ] T038 [US1] Add every user-visible string to `apps/finance/feature/shell/automation/src/main/res/values/strings.xml` — zero hardcoded literals (Article V)
- [ ] T039 [US1] QA: execute `AUT-UI-001`, `AUT-FLOW-001`, `AUT-FLOW-002`, `AUT-BR-004`, `AUT-UI-003`, `AUT-BR-005`; close them in the catalog and update its coverage-summary table
- [ ] T040 [US1] Checkpoint: `./gradlew regressionCheck` green; ratchet the JaCoCo floor in the root `build.gradle.kts` to the **measured** value, never ahead of landed tests

**Checkpoint**: US1 fully functional and independently testable. **This is the MVP.**

---

## Phase 4: User Story 2 — See every source, what it reads, switch it off (Priority: P2)

**Sub-phase 7b.** **Goal**: G1, the control surface every later source needs.

**Independent Test**: open the hub with no permissions granted; every source row states its scope and
state, at least one switches on and off, and a source switched off produces nothing new.

### Tests for User Story 2 — RED before GREEN ⚠️

- [ ] T041 [P] [US2] Write the failing `AutomationHubViewModelTest` in `.../automation/ui/hub/AutomationHubViewModelTest.kt` — source rows carry scope text, on/off state and availability state; switching a source off stops new proposals and leaves existing ones pending (FR-017)
- [ ] T042 [P] [US2] Write the failing flag-off test asserting all four routes render `FeatureDisabledCard` rather than failing the shell (Article IV, FR-052)

### Implementation for User Story 2

- [ ] T043 [US2] Implement `AutomationHubViewModel.kt` and its UI state — one row per source, each with scope copy, switch state, availability (including unavailable and unreachable) and an activity count (FR-014, FR-016, FR-018)
- [ ] T044 [US2] Implement `AutomationHubScreen.kt` (G1) — the header rule stated once and prominently (FR-013), source rows via `ListGroup`/`SwitchRow`, and the rules section placeholder that US5 fills
- [ ] T045 [US2] Populate the source table in `AutomationConfig.kt`: bank messages, account aggregator (unavailable), price feeds, recurring templates — each with its user-facing "what it reads" statement
- [ ] T046 [US2] Declare the module's own Settings entry in `.../automation/di/AutomationModule.kt` via 004's control-plane mechanism, so Settings › Modules › **Automation** appears with the module — **no central list is edited** (FR-019)
- [ ] T047 [US2] Wire the G1 route into the shell with `FeatureHost`, and repoint Phase 3's D9 recurring banner at G2 now that the shared queue exists (contracts/routes.md entry points)
- [ ] T048 [US2] Add hub strings to `strings.xml`
- [ ] T049 [US2] Checkpoint: `regressionCheck` green; ratchet the floor to measured

**Checkpoint**: US1 and US2 both work independently.

---

## Phase 5: User Story 7 — Account linking is explained before it is agreed to (Priority: P7)

**Sub-phase 7b**, same slice as US2 — G3 is a static screen reached only from the hub, so it ships
with its parent rather than waiting five phases for its priority number.

**Independent Test**: open the account-linking screen; scope, duration and purpose are all readable
before any consent-granting control is reachable, and dismissing grants nothing.

### Tests for User Story 7 — RED before GREEN ⚠️

- [ ] T050 [P] [US7] Write the failing `AccountLinkConsentScreenTest` in `.../automation/ui/hub/AccountLinkConsentScreenTest.kt` citing `AUT-FLOW-003` — all three statements present and reachable **before** any consent control; dismiss grants and enables nothing

### Implementation for User Story 7

- [ ] T051 [US7] Implement `AccountLinkConsentScreen.kt` (G3) as a **full-screen modal with close ✕** (design-system §6: scoped consent flows), stating scope, duration and purpose ahead of any control (FR-040, FR-041)
- [ ] T052 [US7] State the capability's unavailability plainly in `.../automation/ui/hub/AccountLinkConsentScreen.kt` rather than presenting a flow that fails downstream (FR-042); make the hub's aggregator row in `AutomationHubScreen.kt` tappable to reach this explanation
- [ ] T053 [US7] QA: execute and close `AUT-FLOW-003`

**Checkpoint**: the hub slice is complete — US2 and US7 both shippable.

---

## Phase 6: User Story 3 — Bank alerts become proposed entries (Priority: P3)

**Sub-phase 7c.** **Goal**: the headline capability and the riskiest slice — a runtime permission, a
background job, a parser and a consent class, landing into an already-tested queue.

**Independent Test**: with source enabled and consent granted, present a representative bank message
and confirm a pending proposal appears with correct amount, date and account, its original text on the
row — and that no transaction was created.

### QA — scenarios before code (Article II)

- [ ] T054 [US3] Write catalog rows `AUT-FLOW-005` (freeze/unfreeze cycle) and `AUT-FLOW-006` (periodic scan, missed window, date-ordered backlog) in catalog §9, reviewed against FR-026a–d and FR-027–FR-027d

### Tests for User Story 3 — RED before GREEN ⚠️

- [ ] T055 [P] [US3] Write the failing `BankSenderAllowlistTest` in `.../automation/sms/BankSenderAllowlistTest.kt` citing SC-004 — promotional messages containing `₹` and OTPs from a bank's own sender both yield **zero** proposals
- [ ] T056 [P] [US3] Write the failing `SmsTransactionParserTest` in `.../automation/sms/SmsTransactionParserTest.kt` citing SC-003 — a representative message table; ≥90% yield correct amount/date/account, the remainder yield `Unparseable(missing)` and are never dropped
- [ ] T057 [P] [US3] Write the failing money-precision assertion in the same test: amounts are `Long` paise with **no `Double` intermediate** anywhere in the parse path (Article VII)
- [ ] T058 [P] [US3] Write the failing two-gate test in `.../automation/sms/SmsGateTest.kt` citing FR-020 — permission without consent reads nothing, consent without permission reads nothing, and the hub reports which is missing
- [ ] T059 [P] [US3] Write the failing no-bypass test asserting there is no call path to `SmsInboxReader` that skips the gate check (Article VIII, plan's Article VIII note)
- [ ] T060 [P] [US3] Write the failing `SmsScanWorkerTest` using `TestListenableWorkerBuilder` citing `AUT-FLOW-006` — a message arriving with the app closed becomes a proposal; skipped intervals lose nothing; backlog lands in date order
- [ ] T061 [P] [US3] Write the failing freeze-cycle test citing `AUT-FLOW-005` — withdrawal makes `BANK_MESSAGE` rows non-actionable while other origins stay actionable; re-grant restores them with **no duplicates** and no migration
- [ ] T062 [P] [US3] Write the failing outbound-payload test citing `AUT-BR-002`/SC-005 — **zero** occurrences of message text in any payload the feature can produce
- [ ] T063 [P] [US3] Write the failing suggestion-not-transaction test citing `AUT-BR-001` — a parsed message writes a proposal, never a ledger row

### Implementation for User Story 3

- [ ] T064 [US3] Confirm `androidx.work` is present and AGP-9-verified; if Phase 6 has not landed, add `androidx.work:work-runtime-ktx` + `work-testing` to `gradle/libs.versions.toml` and verify the build (research R8)
- [ ] T065 [US3] Add `<uses-permission android:name="android.permission.READ_SMS" />` to `apps/finance/app/src/main/AndroidManifest.xml` — **`READ_SMS`, not `RECEIVE_SMS`**; a broadcast permission would contradict the periodic model clarification 5 chose
- [ ] T066 [P] [US3] Implement `BankSenderAllowlist.kt` as a pure function matching the six-character issuer code of an `XX-YYYYYY` sender id, ignoring the varying circle prefix — turns T055 green
- [ ] T067 [P] [US3] Implement `SmsTransactionParser.kt` as a pure two-stage function returning `Parsed`/`Unparseable(missing)`, emitting `Long` paise directly — turns T056 and T057 green
- [ ] T068 [US3] Implement `SmsInboxReader.kt` — the **only** `ContentResolver` touch, querying `date > watermark` filtered to allowlisted senders, with the gate checked at its entry point — turns T058 and T059 green
- [ ] T069 [US3] Implement the pre-grant explanation in `.../automation/ui/hub/SmsPermissionRationale.kt` — on-device reading, bank-senders-only, nothing-recorded-without-approval — shown **before** the system permission dialog (FR-021)
- [ ] T070 [US3] Implement the scan watermark in `.../automation/sms/SmsScanWatermark.kt` over the device settings store, seeded to 30 days ago on first enable then advancing (FR-027, research R8)
- [ ] T071 [US3] Implement `SmsScanWorker.kt` — an hourly periodic request, separate from Phase 6's daily alert worker, cancelled when the `automation` flag is off — turns T060 green
- [ ] T072 [US3] Trigger a read on open from `.../automation/ui/queue/ReviewQueueViewModel.kt` so a user who comes looking never sees a queue staler than their arrival (FR-027c)
- [ ] T073 [US3] Add the "refreshes periodically" statement to `.../automation/ui/queue/ReviewQueueScreen.kt` and `strings.xml` so a just-made purchase being absent reads as expected, not broken (FR-027b)
- [ ] T074 [US3] Implement account resolution from the message's last-4 against `finance.accounts`; no match leaves `account_id` null and the row asks the user — **never a default account**
- [ ] T075 [US3] Implement the unparseable row rendering, generalised over `missing_fields` so a row missing an account says so rather than naming a category (FR-010)
- [ ] T076 [US3] Implement frozen as **derived state** — `origin == BANK_MESSAGE && !consent.readSms`, computed at read time. **Do not add a `FROZEN` status constant** (research R7, data-model §5) — turns T061 green
- [ ] T077 [P] [US3] Build `InfoBanner` in `libs/core/.../components/InfoBanner.kt` (design-system §5.2 batch B7) for the frozen banner with its two actions: delete them, turn it back on
- [ ] T078 [US3] Implement FR-028's no-telephony branch in `.../automation/sms/SmsInboxReader.kt` and its hub row in `AutomationHubViewModel.kt` — the source states it is unavailable rather than offering an inoperable switch
- [ ] T079 [US3] Add SMS-source strings to `strings.xml`
- [ ] T080 [US3] QA: execute `AUT-BR-001`, `AUT-BR-002`, `AUT-FLOW-005`, `AUT-FLOW-006`; close them
- [ ] T081 [US3] Checkpoint: `regressionCheck` and `./gradlew checkTrackerMoneyPrecision` both green; ratchet the floor to measured

**Checkpoint**: US1–US3 and US7 all work independently.

---

## Phase 7: User Story 4 — The same spend is not recorded twice (Priority: P4)

**Sub-phase 7d.**

**Independent Test**: create a transaction manually, produce a proposal matching amount, date and
account, and confirm the row carries a duplicate callout naming the existing record.

### Tests for User Story 4 — RED before GREEN ⚠️

- [ ] T082 [P] [US4] Write the failing duplicate-detection test in `.../data/tracker/automation/DuplicateDetectionTest.kt` citing `AUT-UI-002` — exact amount, same account, within 3 days flags; outside any of the three does not
- [ ] T083 [P] [US4] Extend `.../automation/ui/queue/AcceptAllPartitionTest.kt` with the failing duplicate-exclusion cases citing SC-006a — accept-all records **zero** duplicate-flagged transactions across every queue composition
- [ ] T084 [P] [US4] Write the failing RLS test asserting a second user gets **zero** matches from `find_possible_duplicates` against the first user's transactions (Article IXa)

### Implementation for User Story 4

- [ ] T085 [US4] Author `supabase/schemas/finance/30_functions/find_possible_duplicates.sql` — takes the whole queue's candidates as one `jsonb` argument, returns matches, compares `bigint` paise in SQL
- [ ] T086 [US4] Generate the migration with `supabase db diff -f automation_duplicates`, then **hand-append `security invoker`** — `db diff` does not emit it, and a `security definer` here would let one user probe another's spending by amount (Article IXa). Read the generated file to confirm
- [ ] T087 [US4] Run `python scripts/db/gen_schema_docs.py equiv` and `docs --check`; regenerate `web/src/shared/types/database.ts` with `--schema public,finance`
- [ ] T088 [US4] Implement `data/tracker/automation/DuplicateApi.kt` and `data/tracker/dto/DuplicateMatchDto.kt` — one batched call for the visible queue, never one per row — turns T082 green
- [ ] T089 [US4] Render the callout on the row, **identifying** the matched record inline (amount, date, description). Do **not** add `OpenTransaction` — FR-029 asks the callout to identify the match, not navigate to it (contracts/routes.md)
- [ ] T090 [P] [US4] Extend `CountBadge` in `libs/core/.../components/CountBadge.kt` with the status-dot variant (design-system §5.3) — **extend, never add a second badge component** (Article VI)
- [ ] T091 [US4] Wire `skippedDuplicate` into the accept-all partition and its result report — turns T083 green
- [ ] T092 [US4] State the matching rule in `.../automation/ui/hub/AutomationConfig.kt` copy and `strings.xml`, surfaced on the callout, rather than leaving it an opaque judgement (FR-031)
- [ ] T093 [US4] QA: execute and close `AUT-UI-002`; checkpoint `regressionCheck` green and ratchet the floor

**Checkpoint**: duplicates are caught and accept-all cannot create one.

---

## Phase 8: User Story 5 — Teach it once, stop correcting it (Priority: P5)

**Sub-phase 7e.**

**Independent Test**: create a rule from a corrected proposal, produce a second matching proposal,
confirm the taught category is proposed and the count rose, then remove the rule and confirm it stops.

### Tests for User Story 5 — RED before GREEN ⚠️

- [ ] T094 [P] [US5] Write the failing `AutomationRuleRepositoryTest` in `.../data/tracker/automation/AutomationRuleRepositoryTest.kt` citing `AUT-BR-003` — rule applies to a matching proposal, `applied_count` increments, removal stops application and leaves prior transactions unchanged
- [ ] T095 [P] [US5] Write the failing decline test in `.../automation/ui/queue/TeachRuleOfferTest.kt` citing FR-032 — declining the offer leaves **no** rule behind
- [ ] T096 [P] [US5] Write the failing RLS test asserting a second user reads **zero** rows from `finance.automation_rules` (Article IXa)

### Implementation for User Story 5

- [ ] T097 [US5] Author `supabase/schemas/finance/10_tables/automation_rules.sql` per data-model §2 — RLS `user_id = auth.uid()` with select/insert/update, **no delete policy**, and the partial unique on `(user_id, match_kind, match_value)` where `deleted_at is null`
- [ ] T098 [US5] Generate the migration, then **hand-append the grant** `grant select, insert, update on finance.automation_rules to authenticated;` — custom schemas have no implicit exposure (ADR-0033) and `db diff` emits neither grants nor RLS. Read the generated file
- [ ] T099 [US5] Run `python scripts/db/gen_schema_docs.py equiv` and `docs --check`, and regenerate `web/src/shared/types/database.ts` with `--schema public,finance`, as in T087
- [ ] T100 [P] [US5] Create `data/tracker/model/AutomationRule.kt` and `RuleMatchKind.kt` (`MERCHANT_EXACT` only at birth — append-only, data-model §4)
- [ ] T101 [US5] Implement `AutomationRuleApi.kt` and `AutomationRuleRepository.kt` on the consent-gated `dataClient` — turns T094 and T095 green
- [ ] T102 [US5] Apply matching rules at propose time in the SMS path: matched category proposed, `applied_count` incremented (FR-033, FR-034)
- [ ] T103 [US5] Implement the teach-from-correction offer in `.../automation/ui/queue/ReviewQueueViewModel.kt` after a category correction, with decline as a normal no-op outcome (FR-032) — turns T095 green
- [ ] T104 [US5] Implement the rules list in `.../automation/ui/hub/AutomationHubScreen.kt` — what each matches, its applied count, and working disable and remove controls; a zero-match rule stays listed rather than being pruned (FR-034, FR-035, edge case)
- [ ] T105 [US5] Confirm the matcher stores an extracted merchant token and **never** message text (research R5, `AUT-BR-002`)
- [ ] T106 [US5] QA: execute and close `AUT-BR-003`; checkpoint `regressionCheck` green and ratchet the floor

**Checkpoint**: the queue gets lighter over time.

---

## Phase 9: User Story 8 — Metal values refresh themselves, with approval (Priority: P8)

**Sub-phase 7f**, which forks off 7b rather than following 7c — it shares nothing with the message
path except the queue.

**Independent Test**: with the feed enabled and auto-valued holdings, confirm a fetched rate appears
as a proposed value update naming the affected holdings and their old and new values, that accepting
moves those values, and that ignoring moves nothing.

### QA — scenarios before code (Article II)

- [ ] T107 [US8] Write catalog row `AUT-BR-006` (price-move predicate, all ten cases) in catalog §9, reviewed against FR-044a and FR-045/FR-045a

### Tests for User Story 8 — RED before GREEN ⚠️

- [ ] T108 [P] [US8] Write the failing parameterised `PriceMovePredicateTest` in `.../automation/price/PriceMovePredicateTest.kt` citing `AUT-BR-006` — **all ten cases** from contracts/price-feed.md §2, including case 5 (a later move does propose) and case 9 (acceptance resets the baseline)
- [ ] T109 [P] [US8] Add the failing integer-arithmetic assertion to `.../automation/price/PriceMovePredicateTest.kt` — multiply-and-compare, **no division into a percentage and no float** (Article VII)
- [ ] T110 [P] [US8] Write the failing opt-in test asserting a holding with null `auto_value_series` is **never** included
- [ ] T111 [P] [US8] Write the failing accept/ignore test — accept inserts exactly one `finance.valuations` row; ignore changes **zero** values and sets `last_ignored_price_paise` (SC-013)

### Implementation for User Story 8

- [ ] T112 [US8] Add `auto_value_series` to `supabase/schemas/finance/10_tables/holdings.sql` with its CHECK constraint per data-model §3 — one nullable column, **not** a boolean plus a series (research R4)
- [ ] T113 [US8] Generate the migration (`ALTER TABLE … ADD COLUMN`, understood by the equivalence guard as of Phase 2's T079 work), run both static guards, regenerate the typed web client
- [ ] T114 [P] [US8] Create `data/tracker/model/AutoValueSeries.kt` — `GOLD_24K`, `GOLD_22K`, `SILVER`; currency deliberately excluded (the tracker is INR-only), append-only from birth
- [ ] T115 [US8] Implement `PriceMovePredicate.kt` as the pure two-term function from contracts/price-feed.md §2 — turns T108, T109 green
- [ ] T116 [US8] Implement the per-holding opt-in control on the holding's own surface, and the move-threshold setting (default **5%**, `SliderWithPresets`, device-local store) in the Automation settings entry (FR-044b)
- [ ] T117 [US8] Implement `PriceScanWorker.kt` on the **same** periodic schedule as the message scan — one cadence, not two (FR-044d)
- [ ] T118 [US8] Implement `VALUE_UPDATE` proposal creation with `source_key = (holding_id, recorded_value_paise)`, one outstanding proposal per holding, replacing rather than stacking on a fresh qualifying move — turns T110 green
- [ ] T119 [US8] Render the value-update row: what is revalued, from → to, how far the price moved and from what (FR-044c), and `price_taken_at` so a stale proposal is visibly stale
- [ ] T120 [US8] Implement accept as a plain `finance.valuations` insert — no RPC, no second correction path; a wrong accepted price is Phase 2's `correct_valuation()` (research R3) — turns T111 green
- [ ] T121 [US8] Implement ignore setting `last_ignored_price_paise`, and re-measurement from the **new** recorded value when the user has manually revalued the holding while a proposal was pending (edge case)
- [ ] T122 [US8] Implement the unreachable-source row state (FR-046) and the on-but-silent state in `.../automation/ui/hub/AutomationHubViewModel.kt` when the threshold is set unreachably high (edge case)
- [ ] T123 [US8] Wire `skippedValueUpdate` into the accept-all partition — value updates are never accepted in bulk (FR-009a)
- [ ] T124 [US8] QA: execute and close `AUT-BR-006`; checkpoint `regressionCheck` green and ratchet the floor

**Checkpoint**: all four sources exist; every proposal kind is rendered and decided.

---

## Phase 10: User Story 6 — Told that entries are waiting (Priority: P6)

**Sub-phase 7g**, first half. Depends on Phase 6 (006) having shipped the alert pipeline — this adds
one alert **type**, not a mechanism.

**Independent Test**: with proposals pending, confirm the alert is raised, appears in the notification
centre, opens the review queue when tapped, and stops when the queue is empty.

### Tests for User Story 6 — RED before GREEN ⚠️

- [ ] T125 [P] [US6] Write the failing entries-waiting arm test in `.../automation/alerts/EntriesWaitingArmTest.kt` — raised with a count when proposals are pending, **not** raised when empty (FR-036, FR-039)
- [ ] T126 [P] [US6] Write the failing dispatch test citing SC-009 — `REVIEW_INBOX` lands on G2 with the app closed and, with the app lock engaged, only after unlock (FR-037)

### Implementation for User Story 6

- [ ] T127 [US6] Add the entries-waiting alert type to Phase 6's `apps/finance/app/.../alerts/AlertChannels.kt` and its evaluation arm — a new type on the existing mechanism, never a parallel one (FR-036)
- [ ] T128 [US6] Implement `REVIEW_INBOX` extra handling in `apps/finance/app/.../MainActivity.kt`, routed through 004's app-lock hold-and-dispatch so the lock is honoured, not bypassed — turns T126 green
- [ ] T129 [US6] Add the single alert control to the Automation settings entry in `.../automation/di/AutomationModule.kt`, also governed by the app-wide alert master switch (FR-038)
- [ ] T130 [US6] Update surface registry §3 so `REVIEW_INBOX` lists this phase as a producer alongside the recurring notification
- [ ] T131 [US6] QA: verify the alert end to end per [quickstart.md](./quickstart.md) 7g, and close its rows in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §9

**Checkpoint**: the queue gets drained because the user is told, not because they remembered.

---

## Phase 11: Polish, erasure, and closure

**Sub-phase 7g**, second half. **The feature flag is flipped on as the last action of the phase.**

- [ ] T132 Extend `supabase/schemas/public/30_functions/delete_my_data.sql` with `finance.automation_rules` in FK-safe order; generate the migration and run both static guards
- [ ] T133 Implement the **device-local erasure arm**: the in-app "Delete my data" action must clear `automation_proposal` and `automation_seen_key` in the same user action. Before this phase that flow was a single RPC call — this is a new obligation on an existing flow (research R11, FR-050)
- [ ] T134 [P] Write the failing two-arm erasure test in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/EraseMyDataTest.kt` citing SC-012, asserting **both** arms run — an erasure that reports success while leaving parsed bank messages on the device is the failure mode this prevents
- [ ] T135 [P] Sweep FR-051 masking across `ReviewQueueScreen.kt`, `IgnoredListScreen.kt` **and** the alert notifier, using 004's existing hide-amounts mechanism
- [ ] T136 Implement `ACCEPTED`-row retention: purge 30 days after `decided_at`, retaining only the key in `automation_seen_key` so purging never resurrects a proposal (data-model §1)
- [ ] T137 [P] Verify accessibility: `contentDescription` on every icon-only action, touch targets ≥48dp, list rows ≥56dp, no colour-only meaning on the duplicate callout or the frozen banner (design-system §9)
- [ ] T138 **Sec/DPDP pass** — the full review, since this is the phase that requests SMS: two gates, no-bypass, zero message text outbound, both erasure arms, consent revocability. Record the outcome in `spec.md` § Implementation record (impl plan §7 step 6)
- [ ] T139 [P] Correct §4 of `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`: **delete the *Recently deleted* line** from the Automation entry (Trash is Phase 0b's, readiness §5.2) and the *Import* line (CSV import is deferred, spec clarification 2) — FR-054
- [ ] T140 [P] Update surface registry §1's Automation row with the Ignored list, the `automation` flag key, and shell-detail-route ownership
- [ ] T141 Run every scenario in [quickstart.md](./quickstart.md), including `AUT-FLOW-004`'s **manual chain walk**: enable → real message → proposal in G2 → accept → transaction in D1, with no direct-to-ledger write anywhere
- [ ] T142 QA: close all nine original `AUT-*` rows plus the six new ones, or re-defer any with a stated reason; update the catalog's coverage-summary table (FR-053)
- [ ] T143 [P] Closure docs (Article Xa): `apps/finance/FEATURES.md` row → *enabled*; `apps/finance/feature/shell/automation/README.md` real content; `CHANGELOG.md` entry; `spec.md` § Implementation record; impl plan §7's Phase 7 row → implemented
- [ ] T144 Flip `"automation"` to `"enabled": true` in `platform/feature-flags/dhruv-finance.json` — **only after everything above passes** (FR-052, surface registry §1)
- [ ] T145 Final checkpoint: `./gradlew regressionCheck` green, coverage floor ratcheted to measured and not regressed, `python scripts/ci/doc_link_check.py` clean

---

## Dependencies & execution order

### Phase dependencies

```
Setup (P1) ──▶ Foundational (P2) ──▶ US1 ──▶ US2 ──▶ US3 ──▶ US4
                                      │       │  │           
                                      │       │  └────────▶ US5
                                      │       └▶ US8
                                      │       └▶ US7 (same slice as US2)
                                      └── US1, US4, US5, US8 ──▶ US6 ──▶ Polish
```

| Phase | Depends on | Notes |
|---|---|---|
| Setup | — | Module + flag must precede any route (Article IV) |
| Foundational | Setup | Store, models, merged-read contract |
| US1 (7a) | Foundational, Phases 2 & 3 shipped | **MVP** |
| US2 (7b) | US1 | The hub gates every later source |
| US7 (7b) | US2 | Ships in US2's slice — reached only from the hub |
| US3 (7c) | US2 | Riskiest slice, lands into a tested queue |
| US4 (7d) | US3 | Needs proposals to detect duplicates against |
| US5 (7e) | US3 | Needs a corrected proposal to teach from |
| US8 (7f) | US2 | Forks off the hub — shares nothing with the SMS path |
| US6 (7g) | US1, US4, US5, US8, **Phase 6 shipped** | Counts every kind of waiting thing |
| Polish | all | Erasure is only provable once every source exists |

### Within each user story

- QA rows written → tests RED → implementation GREEN → refactor → QA closes rows → checkpoint
- Pure functions first (`AcceptAllPartition`, `SmsTransactionParser`, `PriceMovePredicate`) — they are
  where the correctness lives and they need no Android runtime
- Models before repositories; repositories before ViewModels; ViewModels before screens

### Parallel opportunities

- Setup: T004, T005, T006 in parallel after T001–T003
- Foundational: T009, T010, T011, T013, T016, T017 in parallel
- Every story's RED tests are `[P]` — they touch different files and all must fail before any GREEN
- `libs/core` component work (T026, T027, T077, T090) is parallel to its story's other tasks
- **Cross-story**: once US2 lands, **US3 and US8 can proceed in parallel** — different sources,
  different files, no shared state beyond the queue they both feed

---

## Parallel example: User Story 1

```bash
# All five RED tests together — different files, all must fail first:
Task: "AcceptAllPartitionTest.kt — the partition table (AUT-BR-005)"
Task: "ProposalDedupeTest.kt — never twice (AUT-BR-004)"
Task: "ProposalRepositoryTest.kt — accept/ignore (AUT-FLOW-001/002)"
Task: "ProposalRepositoryTest.kt — remote partial failure"
Task: "ReviewQueueViewModelTest.kt — states and restore (AUT-UI-001/003)"

# Both libs/core components together:
Task: "SuggestedRow.kt — dashed treatment (skip if Phase 3 landed B4)"
Task: "DayGroupHeader.kt — day grouping (skip if present)"
```

---

## Implementation strategy

### MVP first (User Story 1 only)

1. Phase 1 Setup → Phase 2 Foundational
2. Phase 3 US1
3. **STOP and VALIDATE**: the queue works against Phase 3's recurring proposals alone
4. This alone discharges the deferral Phases 3 and 6 are both waiting on — it is worth shipping even
   if nothing else in this phase does

### Incremental delivery

Each slice below merges separately, ends green on `regressionCheck`, ratchets the floor to its
measured value, and closes its own scenario rows:

1. Setup + Foundational → module builds, flag off
2. **US1** → the shared queue exists (**MVP**)
3. **US2 + US7** → the control surface and the linking statement
4. **US3** → messages become proposals
5. **US4** → duplicates are caught
6. **US5** → the queue gets lighter
7. **US8** → metal values refresh themselves
8. **US6 + Polish** → the user is told, erasure covers both arms, flag on

### Parallel team strategy

After US2 lands: one developer on US3 → US4 → US5 (the message path), another on US8 (the price
path). They meet at US6.

---

## Notes

- **Tests are mandatory here** — Article I is NON-NEGOTIABLE and every test cites its scenario ID
- **Verify RED before GREEN.** A test that passes on first write is testing nothing
- Room DAOs go through fakes, never in-memory Room — Robolectric SQLite does not load on this toolchain
- `supabase db diff` emits neither grants nor `security invoker` — hand-append both and **read the
  generated migration** (T086, T098). Both omissions are silent and both are security defects
- Never add a `FROZEN` proposal status (T076) — frozen is derived, and a stored one reintroduces the
  duplicates FR-026c forbids
- Never write `finance.suggestions.raw_text` (T019) — that is the contradiction research R1 resolved
- Commit after each task or logical group; stop at any checkpoint to validate the slice independently