---

description: "Task list for Search & Notifications (Phase 6)"
---

# Tasks: Search & Notifications (Phase 6)

**Input**: Design documents from `apps/finance/specs/006-search-notifications/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md),
[data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: **Required, not optional.** Constitution Article I is non-negotiable — RED → GREEN →
REFACTOR, and every test cites the `SRC-*` scenario ID it satisfies. Test tasks below are first-class,
not a suggested extra.

**Organization**: Tasks are grouped by user story. Each group also carries its **sub-phase** tag
(6a–6f) from `plan.md`, because the sub-phase — not the story — is the unit that ships, ends green on
`regressionCheck`, and ratchets the coverage floor.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US7)
- Every task names an exact file path

## Path Conventions

Mobile app in the existing Gradle monorepo. Sources are `src/main/java`, tests `src/test/java` — no
separate `tests/` root (existing project convention). **No new Gradle module this phase**; both
screens are shell-owned per implementation plan §6.

- Shell: `apps/finance/app/src/main/java/com/dhruv/finance/app/`
- Shared data: `apps/finance/data/src/main/java/com/dhruv/finance/data/`
- Components: `libs/core/src/main/kotlin/com/dhruv/core/`
- Schema: `supabase/schemas/finance/30_functions/`, `supabase/migrations/`
- QA catalog: `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md`
- Registries: `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`

## Prerequisite gates (read before starting)

1. **Phases 2, 3, 4 and 5 must be shipped.** Every record search returns and every preference an
   alert reads belongs to one of them. Running 6a against today's schema (only `holdings` and
   `valuations`) produces a function that compiles and returns nothing.
2. **Article II condition**: the QA catalog's five `SRC-*` rows cover what the design drew, not the
   machinery underneath. **Six rows are missing** and are written as the *first task* of the
   sub-phase that needs them — T020 (search states), T044 (retention), T059 (dedupe + missed
   window), T098 (the two periodic arms), T107 (control sweep) — before any Backend or Android work
   in that sub-phase.
3. **T058 is a hard gate.** `androidx.work` is a new dependency on AGP 9. This project has been
   burned three times by a dependency that "should" work. No arm work starts until that build passes.

---

## Phase 1: Setup — shared (both tracks)

**Purpose**: The prerequisites are verified, the flags exist, and the registries agree with the plan.
Lands as one small commit before either track starts.

- [ ] T001 Verify prerequisite phases are shipped: `finance.transactions`, `finance.categories`, `finance.budgets`, `finance.goals`, `finance.policies`, `finance.liabilities_meta` present under `supabase/schemas/finance/10_tables/`, and the `money`/`budgets`/`goals`/`insurance`/`insights` flags present in `platform/feature-flags/dhruv-finance.json`. Stop and report if any is missing — do not proceed against a partial schema
- [ ] T002 Add the `search` and `alerts` flag entries (`enabled: true`, `minVersion "1.0.0"`, `requiresConsent: true`) to `platform/feature-flags/dhruv-finance.json`
- [ ] T003 [P] Update the "Notifications (B2)" and "Search (B3)" rows in `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md` §1: FeatureHost key `alerts` / `search`, consent `requiresConsent`, per `contracts/routes.md`
- [ ] T004 [P] Add `search` and `alerts` to the flag list in `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §5.5 and to its A3-switch mapping table under "Sync my financial records"
- [ ] T005 [P] Verify by symbol search against `libs/core/src/main` which of `DayGroupHeader` and a `CountBadge` status-dot variant already exist (Phase 3 may have landed batch B4); record the outcome in this file's notes so T042/T043 either build or skip
- [ ] T006 [P] Verify the `NavTarget` cases `OpenHolding`, `OpenLiability`, `OpenBudget`, `OpenGoal`, `OpenPolicy`, `OpenReports` exist in `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt`, and record whether `OpenTransaction` exists (decides T031)
- [ ] T007 Confirm `./gradlew regressionCheck` is green as the baseline, and record the current measured line coverage so each sub-phase checkpoint can ratchet from it

**Checkpoint**: flags and registry rows agree with the plan; both tracks can now start in parallel.

---

## Phase 2: Foundational — sub-phase 6a (search data seam)

**Purpose**: The function, the model and the repository exist and are proven before any screen does.
No user-visible change.

- [ ] T008 [P] Write `supabase/schemas/finance/30_functions/search_all.sql` per `contracts/search-rpc.md` §1–§5 — `security invoker`, `stable`, the four-kind union, `count(*) over (partition by kind)` as `kind_total`, `bigint` paise, soft-deleted excluded, closed included with `is_closed`
- [ ] T009 Generate the migration with `supabase db diff -f search_all`, review the generated SQL, then run `python scripts/db/gen_schema_docs.py equiv` and `python scripts/db/gen_schema_docs.py docs --check` (the ADR-0032 guards) and commit both files
- [ ] T010 [P] Create the `SearchKind` enum (`TRANSACTION`, `HOLDING`, `POLICY`, `GOAL`, append-only per Article IX) in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/model/SearchKind.kt`
- [ ] T011 [P] Create the `SearchHit` model in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/model/SearchHit.kt` with `amountPaise: Long?` — nullable, never zero-as-absent (data-model §3)
- [ ] T012 [P] Create `SearchHitDto` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/dto/SearchHitDto.kt`
- [ ] T013 RED: write failing `SearchMapperTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/mapper/SearchMapperTest.kt` asserting `kind_total` survives a capped result set, money maps to `Long` paise, and a null amount stays null — citing `SRC-UI-001`
- [ ] T014 GREEN: implement `SearchMapper` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/mapper/SearchMapper.kt`
- [ ] T015 Create `SearchApi` (`rpc/search_all`) in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/search/SearchApi.kt`, constructed from the consent-gated `dataClient` only
- [ ] T016 RED: write failing `SearchRepositoryTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/search/SearchRepositoryTest.kt` covering the 2-character minimum (no call below it), error mapping to a retryable state, and the consent short-circuit
- [ ] T017 GREEN: implement `SearchRepository` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/search/SearchRepository.kt`
- [ ] T018 Wire `SearchApi` and `SearchRepository` into Koin in `apps/finance/data/src/main/java/com/dhruv/finance/data/di/DataModule.kt`
- [ ] T019 Write a test asserting `SearchApi` is constructible only from the `ConsentInterceptor`-bearing client (Article VIII is structural, not a convention) in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/net/ConsentGatedClientTest.kt`

**Checkpoint**: the search seam is green on `regressionCheck` with no UI.

---

## Phase 3: User Story 1 — Global search (Priority: P1) — sub-phase 6a 🎯 MVP

**Goal**: One field over transactions, holdings, policies and goals, with true per-kind counts and a
working jump to every result.

**Independent Test**: record one of each kind sharing a substring, search it, confirm the chip counts
match the listed results and each result opens its own detail screen.

- [ ] T020 [US1] QA: write the missing search-states row in `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` §10 (signed-out, offline, not-configured, disabled, short-query vs no-results are five distinct states) — **before any task below**
- [ ] T021 [P] [US1] Create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/search/SearchConfig.kt` holding the four kinds, their secondary-line shapes, `maxPerKind = 25` and the input debounce (Article V — no inline literals in the screen)
- [ ] T022 [US1] RED: write failing `SearchViewModelTest` in `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/search/SearchViewModelTest.kt` covering chip counts equalling listed results under a cap, single-kind filtering and restore, and all five states — citing `SRC-UI-001` and the T020 row
- [ ] T023 [US1] GREEN: implement `SearchViewModel` and `SearchUiState` in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/search/`
- [ ] T024 [US1] Implement `SearchScreen` in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/search/SearchScreen.kt` using `SearchField` and `NxTopBar` from `:libs:core` — no local styling, no raw dp/sp/hex
- [ ] T025 [US1] Implement the filter chips with `ModeChipRow`/`Chip` and the grouped results with `ListGroup`/`ListGroupRow`/`SectionLabel` in `SearchScreen.kt`
- [ ] T026 [US1] Implement the per-kind secondary lines and render every amount through `MoneyText` (tabular, never ellipsised) in `SearchScreen.kt`
- [ ] T027 [P] [US1] Implement the initial "type to search" state and the nothing-matched state — distinct from each other, the latter echoing the query and naming the four kinds searched (FR-008) — in `SearchScreen.kt`
- [ ] T028 [P] [US1] Implement the signed-out, offline, error and disabled states with `SignedOutCard`, `OfflineStateCard`, `RetryErrorCard` and `FeatureHost`'s `FeatureDisabledCard` in `SearchScreen.kt`
- [ ] T029 [US1] Register the `search` route wrapped in `FeatureHost("search")` and add the search entry point to the shared top bar in `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt`
- [ ] T030 [US1] RED then GREEN: dispatcher test asserting each of the four kinds resolves to its own detail screen through `NavTarget`, in `apps/finance/app/src/test/java/com/dhruv/finance/app/navigation/SearchDispatchTest.kt` — citing `SRC-FLOW-001`
- [ ] T031 [US1] If T006 found `OpenTransaction` absent, add the sealed case in `libs/core/src/main/kotlin/com/dhruv/core/navigation/NavTarget.kt` **and** the matching row in surface registry §1 in the same change (the registry's pairing rule); skip if Phase 3 already added it
- [ ] T032 [P] [US1] Add every search string to `apps/finance/app/src/main/res/values/strings.xml` — zero hardcoded literals in the screen
- [ ] T033 [US1] Checkpoint 6a: `./gradlew regressionCheck` green, coverage floor ratcheted to the measured value in the root `build.gradle.kts`, `SRC-UI-001` and `SRC-FLOW-001` and the T020 row closed in the catalog with its coverage-summary table updated

**Checkpoint**: search ships on its own. The rest of the phase can slip without losing this.

---

## Phase 4: Foundational — sub-phase 6b (alert store)

**Purpose**: A device-local record that is correct before anything writes to it.

- [ ] T034 [P] Create the `AlertType` enum (`BUDGET_BREACH`, `INSTALMENT_DUE`, `RENEWAL_DUE`, `VALUATION_STALE`, `MONTHLY_SUMMARY`, append-only) in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/model/AlertType.kt`
- [ ] T035 [P] Create `AlertLogEntity` in `apps/finance/data/src/main/java/com/dhruv/finance/data/alerts/AlertLogEntity.kt` per data-model §1 — unique index on `alert_key`, index on `raised_at DESC`, `payload_json` holding paise integers only
- [ ] T036 Create `AlertLogDao` in `apps/finance/data/src/main/java/com/dhruv/finance/data/alerts/AlertLogDao.kt` with insert-if-absent, newest-first read, mark-all-read, unread count, and purge-older-than
- [ ] T037 Bump `AppDatabase` to version 6 and add `MIGRATION_5_6` (create table + two indexes, additive only) in `apps/finance/data/src/main/java/com/dhruv/finance/data/AppDatabase.kt`, registering it in the existing `addMigrations` chain alongside `MIGRATION_4_5`
- [ ] T038 [P] Create `FakeAlertLogDao` in `apps/finance/data/src/test/java/com/dhruv/finance/data/alerts/FakeAlertLogDao.kt` — the DAO is tested through a fake, **never** in-memory Room (Robolectric's SQLite does not load on this Windows toolchain)
- [ ] T039 RED: write failing `AlertRepositoryReadTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/alerts/AlertRepositoryReadTest.kt` covering newest-first ordering, unread count, mark-all-read, purge at the 90-day boundary, and that rows with `displayed = false` are still returned (FR-016)
- [ ] T040 GREEN: implement the read side of `AlertRepository` in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/alerts/AlertRepository.kt`
- [ ] T041 Wire `AlertLogDao` and `AlertRepository` into Koin in `apps/finance/data/src/main/java/com/dhruv/finance/data/di/DataModule.kt`
- [ ] T042 [P] If T005 found `DayGroupHeader` absent, add it to `libs/core/src/main/kotlin/com/dhruv/core/ui/components/DayGroupHeader.kt` (design-system §5.2 batch B4) and move its row from §5.2 to §5.1; skip if Phase 3 landed it
- [ ] T043 [P] Extend `CountBadge` in `libs/core/src/main/kotlin/com/dhruv/core/ui/components/CountBadge.kt` with the status-dot variant and update design-system §5.3's `CountBadge` row — **extend, never add a parallel badge** (§5.3's own closing rule)

**Checkpoint**: the store is correct and green with nothing writing to it.

---

## Phase 5: User Story 3 — Notification centre (Priority: P3) — sub-phase 6b

**Goal**: Every alert the app raises, grouped by local date, with read state that survives a restart.

**Independent Test**: seed alerts dated today and earlier through the fake, confirm the grouping and
unread marks, mark all read, restart, confirm all still read.

- [ ] T044 [US3] QA: write the missing retention row in catalog §10 (a row older than 90 days is purged; one at 89 days is not) — **before any task below**
- [ ] T045 [P] [US3] Create `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/notifications/AlertConfig.kt` holding the channel table, the 90-day retention window, the evaluation cadence and the staleness window (Article V)
- [ ] T046 [US3] RED: write failing `NotificationCentreViewModelTest` in `apps/finance/app/src/test/java/com/dhruv/finance/app/ui/notifications/NotificationCentreViewModelTest.kt` covering TODAY/EARLIER grouping by **local** calendar date including a just-before-midnight fixture, and read state surviving a restart — citing `SRC-UI-002` and `SRC-FLOW-002`
- [ ] T047 [US3] GREEN: implement `NotificationCentreViewModel` and `NotificationCentreUiState` in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/notifications/`
- [ ] T048 [US3] Implement `NotificationCentreScreen` in `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/notifications/NotificationCentreScreen.kt` using `DayGroupHeader`, `ListGroup`/`ListGroupRow` and `NxTopBar`
- [ ] T049 [US3] Delete `apps/finance/app/src/main/java/com/dhruv/finance/app/ui/shell/NotifScreen.kt` and repoint the `notif` route to the new screen wrapped in `FeatureHost("alerts")` in `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt`
- [ ] T050 [US3] Surface the unread count on the shared top bar's alerts icon using the extended `CountBadge` (FR-017, design-system N5 — the icon is on every tab) in `MainActivity.kt`
- [ ] T051 [P] [US3] Implement the "Mark all read" action in `NotificationCentreScreen.kt`
- [ ] T052 [P] [US3] Implement the empty state with `EmptyStateCard` — no sample or illustrative content (FR-019) in `NotificationCentreScreen.kt`
- [ ] T053 [P] [US3] Implement the signed-out and disabled states in `NotificationCentreScreen.kt`
- [ ] T054 [P] [US3] Add every centre string to `apps/finance/app/src/main/res/values/strings.xml`
- [ ] T055 [US3] Verify `MIGRATION_5_6` on device: install a v5 build, upgrade, confirm calculator history and the currency cache are intact and `alert_log` exists (the migration is not covered by the JVM gate)
- [ ] T056 [US3] Checkpoint 6b: `regressionCheck` green, floor ratcheted, `SRC-UI-002`, `SRC-FLOW-002` and the T044 row closed

**Checkpoint**: the centre is correct and empty. The first real alert will land in a screen that already works.

---

## Phase 6: Foundational — sub-phase 6c (the pipeline)

**Purpose**: The dependency is proven, the two pure decisions are tested, and the machinery exists —
before any arm is written.

- [ ] T057 Add `androidx.work:work-runtime-ktx` and the `androidx.work:work-testing` test dependency to `gradle/libs.versions.toml` and reference them in `apps/finance/app/build.gradle.kts`
- [ ] T058 **GATE**: run `./gradlew :apps:finance:app:assembleDebug` and record an explicit pass/fail for `androidx.work` on AGP 9. **Do not start T060 or anything after it until this passes** — this project has rejected three dependencies at exactly this step (ADR-0010, ADR-0013, ADR-0029)
- [ ] T059 QA: write the missing dedupe row and missed-window row in catalog §10 (a condition true across repeated passes raises once; a five-day outage raises once, not five times) — **before any task below**
- [ ] T060 [P] RED: write failing `AlertKeyTest` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/alerts/AlertKeyTest.kt` asserting the `<type>:<subject_id>:<period_token>` grammar and the per-type token shape from `contracts/alert-pipeline.md` §2
- [ ] T061 GREEN: implement the pure `AlertKey` function in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/alerts/AlertKey.kt`
- [ ] T062 [P] RED: write failing `AlertSuppressionTest` in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/AlertSuppressionTest.kt` as a table test over **all seven** ladder branches — steps 1–5 not raised, steps 6–7 raised and recorded but not displayed (`contracts/alert-pipeline.md` §5)
- [ ] T063 GREEN: implement the pure `AlertSuppression` function in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertSuppression.kt` — no clock, no context, no I/O
- [ ] T064 Write `supabase/schemas/finance/30_functions/due_alerts.sql` with the **`BUDGET_BREACH` arm only**, per `contracts/alert-pipeline.md` §1 — `security invoker`, `stable`, `as_of` passed in (never `now()` inside), `payload` structured values only, paise as integers
- [ ] T065 Generate the migration with `supabase db diff -f due_alerts`, review it, run both `gen_schema_docs.py` guards, commit both files
- [ ] T066 [P] Create `DueAlertDto`, the `DueAlert` model and `AlertMapper` under `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/{dto,model,mapper}/`, with a RED-then-GREEN mapper test asserting paise stay `Long` and no rendered text crosses the boundary
- [ ] T067 Create `DueAlertsApi` (`rpc/due_alerts`) in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/alerts/DueAlertsApi.kt`, constructed from the consent-gated `dataClient` only
- [ ] T068 Implement the write side of `AlertRepository` — insert-if-absent keyed on `alert_key`, plus the 90-day purge — in `apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/alerts/AlertRepository.kt`
- [ ] T069 Implement idempotent channel registration for `budget_alerts` in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertChannels.kt`, reading id, name and importance from `AlertConfig.kt`
- [ ] T070 Declare `POST_NOTIFICATIONS` in `apps/finance/app/src/main/AndroidManifest.xml` and implement the runtime request path — triggered when the user first turns on an alert control or first opens the centre, **not** at launch; API 26–32 takes the no-permission branch (research R10)
- [ ] T071 Implement `AlertNotifier` in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertNotifier.kt` — builds title and body from `strings.xml` resources with `payload` values substituted, applying privacy masking through the shared formatting helpers, never from server text
- [ ] T072 Implement `AlertEvaluationWorker` in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertEvaluationWorker.kt` — ladder, `due_alerts(today)`, key formation, insert-if-absent, post, purge, in that order; never writes to Postgres
- [ ] T073 Implement `AlertScheduler` in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertScheduler.kt` — a one-day `PeriodicWorkRequest` with `NetworkType.CONNECTED` and a unique work name, enqueued when the `alerts` flag resolves enabled and **cancelled** when it resolves disabled, resolving the flag from the cached last-known-good value in the background
- [ ] T074 Add the one-shot foreground evaluation on app open in `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt`, so a user who opens the app is never told stale news
- [ ] T075 Write a test asserting the worker's repository is constructible only from the consent-gated client, in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/AlertWorkerConsentTest.kt` — the worker is the first code that talks to PostgREST with nobody watching

**Checkpoint**: the pipeline exists and raises nothing, because no arm returns rows yet.

---

## Phase 7: User Story 2 — Budget breach alert (Priority: P2) — sub-phase 6c

**Goal**: The threshold a user set in Phase 4 finally does something.

**Independent Test**: push a category past its stored threshold, run a pass, confirm exactly one
alert naming the category and its position; run three more passes and confirm still one.

- [ ] T076 [US2] RED: write failing arm tests in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/BudgetBreachArmTest.kt` using `TestListenableWorkerBuilder` — first breach raises once, repeated passes raise nothing, a new budget period raises again, a five-day gap raises once — citing the T059 rows
- [ ] T077 [US2] GREEN: complete the `BUDGET_BREACH` arm end to end across `due_alerts.sql`, `AlertMapper` and `AlertEvaluationWorker`; a budget with a null `alert_pct` is skipped
- [ ] T078 [US2] Wire the arm to the Planning module's existing budget-alert control so turning it off stops this type and nothing else (FR-035), in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertSuppression.kt` and its Koin wiring
- [ ] T079 [P] [US2] Implement `budget_alerts` masking in `AlertNotifier.kt` — amount masked under privacy mode, **percentage always shown** (surface registry §2)
- [ ] T080 [P] [US2] Add the budget-alert strings to `apps/finance/app/src/main/res/values/strings.xml`

**Checkpoint**: one alert type works end to end, but tapping it does nothing yet.

---

## Phase 8: User Story 4 — Open the app on the right thing (Priority: P4) — sub-phase 6c

**Goal**: Tapping an alert lands on its subject — cold, locked, or after the record was deleted.

**Independent Test**: tap the budget alert from a cold start, with app lock on, and after deleting
the budget; confirm the destination, the authentication order, and the not-found state.

- [ ] T081 [US4] RED: write failing dispatch tests in `apps/finance/app/src/test/java/com/dhruv/finance/app/navigation/AlertDispatchTest.kt` asserting the intent extra resolves to `NavTarget.OpenBudget`, lands on the Plan tab root and then pushes E3 (N6, FR-029) — citing `SRC-FLOW-003`'s budget leg
- [ ] T082 [US4] GREEN: implement alert intent-extra handling in `apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt`, routing through the existing `NavigationDispatcher`
- [ ] T083 [US4] Add a test asserting an unknown, foreign or deleted subject id resolves to that record's not-found state and never crashes (FR-030) in `AlertDispatchTest.kt` — a notification can outlive the record it names
- [ ] T084 [US4] Verify the locked path against `apps/finance/specs/004-settings/contracts/app-lock-gate.md` §3 — held while locked, dispatched exactly once after unlock, replaced by a later arrival, cleared on process death. **Verify, do not re-specify**; this phase is the first real producer of held intents
- [ ] T085 [P] [US4] Update surface registry §3: correct the `OPEN_BUDGETS` row to point at E3 with a category (it currently points at E2), and record this phase as its producer
- [ ] T086 [US4] Close `PLN-FLOW-003` in the QA catalog — the row Phase 4 deferred to here, citing its research R8 — and mark the deferral discharged
- [ ] T087 [US4] Checkpoint 6c: `regressionCheck` green, floor ratcheted, `SRC-FLOW-003` budget leg plus the two T059 rows closed, `PLN-FLOW-003` closed

**Checkpoint**: a complete vertical slice — stored preference, evaluation, notification, centre entry, deep link.

---

## Phase 9: User Story 5 — Obligation reminders (Priority: P5) — sub-phase 6d

**Goal**: Instalments and renewals warn ahead of the date, with the offset the user chose.

**Independent Test**: set an offset on a liability and a policy, advance to it, confirm one reminder
each carrying name and date only, and confirm each opens its own record.

- [ ] T088 [US5] Add the `INSTALMENT_DUE` and `RENEWAL_DUE` arms to `supabase/schemas/finance/30_functions/due_alerts.sql` per `contracts/alert-pipeline.md` §1 — a null `remind_days_before` skips the policy
- [ ] T089 [US5] Generate the migration with `supabase db diff`, review it, run both `gen_schema_docs.py` guards, commit both files
- [ ] T090 [US5] RED: write failing arm tests in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/ObligationArmsTest.kt` — the reminder window opens on the offset, an obligation settled before the date raises nothing, and both payloads carry **no amount**
- [ ] T091 [US5] GREEN: complete both arms across the SQL, the mapper and the worker
- [ ] T092 [P] [US5] Register the `emi_reminders` and `renewal_reminders` channels in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertChannels.kt`
- [ ] T093 [US5] Implement the "Mark paid" quick action so it routes through the **same** confirm path as the in-app action, in `apps/finance/app/src/main/java/com/dhruv/finance/app/alerts/AlertNotifier.kt` and its receiver
- [ ] T094 [US5] Add a test asserting the quick action and the in-app action are one code path — verified by reading the record afterwards, not by observing a toast — in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/QuickActionTest.kt`
- [ ] T095 [US5] Extend `AlertDispatchTest.kt` for `OpenLiability` and `OpenPolicy`, and add the `OPEN_LIABILITY(id)` row to surface registry §3 — citing `SRC-FLOW-003`'s EMI and renewal legs
- [ ] T096 [P] [US5] Add the obligation-reminder strings to `apps/finance/app/src/main/res/values/strings.xml`
- [ ] T097 [US5] Checkpoint 6d: `regressionCheck` green, floor ratcheted, both `SRC-FLOW-003` legs closed, one quick-action path confirmed rather than two

---

## Phase 10: User Story 6 — Periodic nudges (Priority: P6) — sub-phase 6e

**Goal**: Stale values get flagged; the monthly summary arrives for users who asked for it.

**Independent Test**: age a holding's valuation past the window and confirm one flag; enable the
monthly-summary preference, roll into a new month, confirm one summary opening that month's report.

- [ ] T098 [US6] QA: write the two missing arm rows in catalog §10 (valuation-stale, monthly summary) — **before any task below**
- [ ] T099 [US6] Add the `VALUATION_STALE` arm to `supabase/schemas/finance/30_functions/due_alerts.sql`, then generate the migration, review, and run both `gen_schema_docs.py` guards
- [ ] T100 [US6] RED then GREEN: valuation-stale arm tests in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/PeriodicArmsTest.kt` — names and age in the payload, **no amounts**
- [ ] T101 [US6] RED then GREEN: the monthly-summary arm in `AlertEvaluationWorker.kt`, evaluated **on the device** from the Phase 5 preference with no RPC, `period_token = YYYY-MM` of the closed month
- [ ] T102 [US6] Add a test asserting `due_alerts` **never** returns `MONTHLY_SUMMARY` in `apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/alerts/DueAlertsContractTest.kt` — this stops a future task inventing a server arm for it (research R3)
- [ ] T103 [P] [US6] Register the `stale_valuations` and `monthly_digest` channels in `AlertChannels.kt`
- [ ] T104 [US6] Extend `AlertDispatchTest.kt` for `OpenHolding` and `OpenReports(period)`, and add the `OPEN_HOLDING(id)` row to surface registry §3
- [ ] T105 [P] [US6] Add the periodic-alert strings to `apps/finance/app/src/main/res/values/strings.xml`
- [ ] T106 [US6] Checkpoint 6e: `regressionCheck` green, floor ratcheted, both T098 rows closed, the Phase 5 preference read without a migration or a re-prompt

---

## Phase 11: User Story 7 — Controls and masking (Priority: P7) — sub-phase 6f

**Goal**: Every type is controllable in exactly one place, everything is silenceable at once, and no
amount leaks under privacy mode.

**Independent Test**: turn each control off in turn, turn the master off, deny permission, turn
privacy mode on — and confirm each produces exactly its stated effect and no other.

- [ ] T107 [US7] QA: write the missing control-sweep row in catalog §10 (five channels, five controls, each in its owning module, zero duplicates, zero orphans) — **before any task below**
- [ ] T108 [US7] Sweep Settings and verify each of the five channels has exactly one control in its owning module's entry per surface registry §4 and `SET-BR-006`; fix any duplicate or orphan found, in the owning module's settings contribution
- [ ] T109 [P] [US7] Add a test asserting per-type isolation — turning one alert control off stops that type and no other (FR-035) — in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/AlertControlsTest.kt`
- [ ] T110 [P] [US7] Add a test asserting the app-wide master switch suppresses display for every type while the alerts are **still recorded** with `displayed = false` (FR-033 plus FR-016) in `AlertControlsTest.kt`
- [ ] T111 [US7] Verify 004's permission-denied banner and its route to system settings, and confirm there is no re-prompt loop (004 FR-027) — reachable for the first time now that something posts. **Verify, do not re-specify**
- [ ] T112 [US7] Add masking tests for all five types with privacy mode on and off, in `apps/finance/app/src/test/java/com/dhruv/finance/app/alerts/AlertMaskingTest.kt` — no amount legible in shade or centre; percentages, counts and dates readable (FR-025)
- [ ] T113 [P] [US7] Add a test asserting the masking **floor**: `emi_reminders`, `renewal_reminders` and `stale_valuations` carry no amount even with privacy mode off — the registry rule is a floor, not a toggle
- [ ] T114 [P] [US7] Add a test asserting no channel offers more than two action buttons (design-system §11) in `AlertMaskingTest.kt`

---

## Phase 12: Polish & Closure — sub-phase 6f

**Purpose**: The phase gate.

- [ ] T115 Sec pass — this phase adds one manifest permission and the app's first background network caller; verify no raw financial content leaves the device, no secret is added, and the consent gate holds in the background (`platform/skills/` security review, ADR-0029 §2)
- [ ] T116 Ratchet the coverage floor in the root `build.gradle.kts` to the final measured value — up only, never ahead of landed tests (Article X)
- [ ] T117 Close or explicitly defer with a stated reason **every** `SRC-*` row and every row written during 6a–6e in catalog §10, and update its coverage-summary table — a row left silently unticked fails the checkpoint
- [ ] T118 [P] Update surface registries §1 (both route rows), §2 (source phase for the five channels) and §3 (the two new intent rows plus the `OPEN_BUDGETS` correction) in `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`
- [ ] T119 [P] Update the Phase 6 row in `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §7 to "shipped", recording the three plan corrections this feature made (two flags added to §5.5; no Gradle module; two functions and zero tables)
- [ ] T120 Run [quickstart.md](quickstart.md) end to end on a device — all six sub-phase sections
- [ ] T121 Final gate: `./gradlew regressionCheck` green, coverage floor not regressed, `platform/versions.json` minor bump left to CI from the `feat:` commits (never hand-edited — ADR-0025)

---

## Dependencies & Execution Order

### Phase dependencies

```
Phase 1 (Setup, shared)
   ├──▶ Phase 2 (Found. 6a) ──▶ Phase 3 (US1) ─────────────────┐
   └──▶ Phase 4 (Found. 6b) ──▶ Phase 5 (US3)                  │
                                     │                          │
                                     ▼                          │
                              Phase 6 (Found. 6c)               │
                                     │                          │
                                     ▼                          │
                              Phase 7 (US2) ──▶ Phase 8 (US4)   │
                                                    │           │
                              ┌─────────────────────┴───────┐   │
                              ▼                             ▼   │
                       Phase 9 (US5)                Phase 10 (US6)
                              └──────────┬──────────────────┘   │
                                         ▼                      │
                              Phase 11 (US7) ◀──────────────────┘
                                         │
                                         ▼
                              Phase 12 (Polish)
```

**Three foundational blocks, not one.** The template assumes a single blocking Phase 2. This feature
has two independent tracks that never share infrastructure — search touches no Room table, no worker
and no permission; the alert pipeline touches no search code — so a single foundational phase would
force one track to wait on the other for nothing. Phases 2, 4 and 6 are each foundational **for their
own sub-phase**, matching the sub-phase-tagged structure `plan.md` defines and `005-insights` already
used.

### User story dependencies

- **US1 (P1)** — Global search. Depends on Phase 2 only. **Independent of every other story.**
- **US3 (P3)** — Notification centre. Depends on Phase 4 only. Independent of US1.
- **US2 (P2)** — Budget breach. Depends on US3's store and Phase 6's pipeline.
- **US4 (P4)** — Dispatch. Depends on US2 having something to tap.
- **US5 (P5)**, **US6 (P6)** — Both depend on US4's dispatch. **Independent of each other.**
- **US7 (P7)** — Verification across all delivered types; depends on US1, US5 and US6.

### Parallel opportunities

- **Two tracks from the start**: one developer on Phases 2–3 (search), another on Phases 4–5 (centre).
  Neither touches a file the other does.
- **US5 and US6 in parallel** once US4 lands — separate arms, separate channels, separate strings.
  Both touch `due_alerts.sql`, so their SQL edits serialise; everything else is independent.
- Within phases, all `[P]` tasks touch different files.

### Parallel example — Phase 2 (search seam)

```bash
Task: "T008 Write supabase/schemas/finance/30_functions/search_all.sql"
Task: "T010 Create SearchKind enum in .../tracker/model/SearchKind.kt"
Task: "T011 Create SearchHit model in .../tracker/model/SearchHit.kt"
Task: "T012 Create SearchHitDto in .../tracker/dto/SearchHitDto.kt"
```

### Parallel example — Phase 5 (centre screen states)

```bash
Task: "T051 Mark all read action in NotificationCentreScreen.kt"
Task: "T052 Empty state in NotificationCentreScreen.kt"
Task: "T053 Signed-out and disabled states in NotificationCentreScreen.kt"
Task: "T054 Centre strings in strings.xml"
```

---

## Implementation Strategy

### MVP — sub-phase 6a only (Phases 1–3, T001–T033)

Global search, shipped alone. It is the only slice with no worker, no permission, no Room migration
and no dependency on the alert design, and it is the half of the feature the user asks for rather than
receives. **Stop here and validate** — search is genuinely useful with nothing else built.

### Incremental delivery

1. Phase 1 → flags and registries agree with the plan.
2. Phases 2–3 → **6a ships**. Search works. (MVP)
3. Phases 4–5 → **6b ships**. The centre is correct and empty.
4. Phases 6–8 → **6c ships**. One alert type, end to end, with a working deep link. This is the slice
   that proves the whole design.
5. Phase 9 → **6d ships**. Obligations.
6. Phase 10 → **6e ships**. Periodic nudges.
7. Phases 11–12 → **6f ships**. Controls verified, masking swept, catalog closed.

Each ends green on `regressionCheck`, ratchets the floor to its measured value, and merges separately.

### Notes

- `[P]` tasks touch different files and have no incomplete dependency.
- Every test cites the scenario ID it satisfies. A test without one fails the Article I review.
- Verify RED before writing GREEN — a test that passes on first run proves nothing.
- **T058 is a stop-the-line gate.** Everything from T060 onward assumes `androidx.work` builds.
- Room DAOs go through fakes, never in-memory Room — the Robolectric-SQLite constraint on this
  toolchain is a known blocker, not a preference.
- Never hand-edit `platform/versions.json` or `gradle.properties` — CI owns the bump (ADR-0025).

---

## Phase 6g: Gap remediation (multi-agent spec audit, 2026-08-22)

**Source**: `apps/finance/docs/superpowers/reviews/2026-08-22-spec-phase-gap-register.md`.

- [ ] T122 [SA] **DPDP: purge `alert_log` on erasure.** `alert_log.payload_json` stores user money as
      paise integers and `subject_id` references, and `data-model.md:57-58` asserts it is "erased with
      the app's data … not part of `delete_my_data()`" — but the in-app erasure action deletes
      Supabase rows only, and no task in this phase wires a Room purge into it (grep for "Delete my
      data"/erasure in this file returns nothing). After a successful erasure the notification centre
      still holds readable amounts for records that no longer exist. Either purge the table in the
      same action or stop storing amounts in the payload. Paired with 004 T122
- [ ] T123 [SA] **Add the `grant execute` statements as an explicit task.** Both contracts state
      `grant execute … to authenticated` for the two functions, but no task hand-adds them — and
      `db diff` cannot emit grants (ADR-0032 caveat list). 005 makes this an explicit task at every
      sub-phase; without it both functions are unreachable
- [ ] T124 [SA] **Reconcile the search contract's transaction columns.** `contracts/search-rpc.md:42`
      and `data-model.md:136` name "description / counterparty"; Phase 3's `finance.transactions` has
      `payee` and `note` and no `description`. Paired with 002 T090 — fix on whichever side is wrong
      before coding against it
- [ ] T125 [SA] **Resolve the missing settings controls this phase verifies.** §17 requires every
      channel to have exactly one control in the Settings entry of the module that owns it, but 4 of
      the 5 owning modules (networth ×2, planning, insurance) plan no settings entry at all — only
      the monthly-summary control has a stated owner and storage location. **Sub-phase 6f is a
      verification task against controls that were never planned.** Tasks now exist in 001 (T059),
      002 (T092) and 003 (T144); this task tracks that they land before 6f runs
- [ ] T126 [SA] **Reconcile B2's row types with the design.** The functional spec draws five: budget
      overrun, EMI due, policy renewal, **price/rate alert**, **recurring-posted digest**. This phase
      ships `BUDGET_BREACH`, `INSTALMENT_DUE`, `RENEWAL_DUE`, `VALUATION_STALE`, `MONTHLY_SUMMARY` —
      two designed types are out of scope or deferred to unspecced Phase 7, and two shipped types the
      design does not draw are added. QA row `SRC-FLOW-003` still tests a rate-alert deep link that
      nothing delivers. Either restore the two, or amend the functional spec and retire that QA row
- [ ] T127 [SA] Add **loading and error** state requirements. FR-011 is search-only, and B2's states
      appear in SC-015 with no FR behind them; neither a `SkeletonBlock` loading state nor a retryable
      `RetryErrorCard` is required anywhere (DESIGN-SYSTEM §7 makes all eight binding per screen)
- [ ] T128 [SA] **Give alerts a user-initiated dismiss/delete path.** FR-014 marks read and FR-018
      auto-drops at 90 days; a user cannot remove an entry
- [ ] T129 [Android] Ship this module's **`SettingsContribution`** per
      `../004-settings/contracts/settings-contribution.md` — this phase adds no Gradle module but does
      own the alerts master control and the notification-centre entry
- [ ] T130 [SA] Depends on **003 T138** (`policies.name`). This phase's search contract returns policy
      name as the result title and its data model lists `policies (name, …)` as a column it reads; the
      column does not exist in Phase 4's table today
- [ ] T131 [SA] Claim the orphaned intents this phase is the natural owner of, or record them as
      descoped: surface registry §3's **`QUICK_ADD`** (launcher shortcut + widget), **`REVIEW_INBOX`**
      (→ G2, unspecced Phase 7) and **`OPEN_UPCOMING`** — grep across all six spec dirs returns no
      hits for any of the three. The **Glance widget** is **in scope as of 2026-08-23** (maintainer overturned the descope) and
      this phase is the recommended owner — it already holds the shell notification and intent
      surfaces. DESIGN-SYSTEM §11 fixes its conventions; it needs a value source (Phase 2's
      `v_net_worth_history`) and its own masked / signed-out / disabled states

---

## Phase 6h: Gap remediation, round 2 (UI/UX + requirements audit, 2026-08-22)

B2 and B3 both match the design as drawn, and this phase's edge-case coverage is strong. The
findings below are a contract contradiction and the enforcement gaps it shares with 001/002.

- [ ] T132 [SA] **FR-003 and SC-001 are unsatisfiable against this phase's own contract.** FR-003,
      SC-001 and the acceptance scenario all require each filter chip's count to equal the results
      listed; the Edge Cases section and `search_all` §5 require the **opposite** — `kind_total` is
      the pre-cap count, so "a chip reading `Transactions 41` is true even when 25 rows came back" —
      and `SRC-UI-001` is written to test a fixture that exceeds the cap. Pick one reading and make
      all five statements agree
- [ ] T133 [SA] **The results past the 25/kind cap are unreachable.** There is no "load more", no
      offset parameter and no FR acknowledging the cap — a user who sees `Transactions 41` has no way
      to reach the other 16. Either add paging to `search_all` or state the cap in the UI copy
- [ ] T134 [SA] **Own the four notification conventions §11 requires and this phase does not cover.**
      Currently owned: the two-action cap (rule 15 + T114) and one-control-per-channel (rule 17).
      **Unowned**: sentence case (zero occurrences of the phrase across all six specs), the ≤1-line
      collapsed / `BigTextStyle`-only-for-long-form restriction (rule 25 asserts "the shade is one
      line" as prose with no test), never a policy or account number, and **never an account name and
      an amount in the same line under privacy mode** — that last one is a privacy rule, not styling
- [ ] T135 [Android] **Add the accessibility work this phase claims but does not plan.** `spec.md:15`
      lists NFR-6 in scope and no task implements any of it: `contentDescription` on icon-only
      actions and on B2's row leading icons, ≥48dp targets and ≥56dp rows, contrast in both themes,
      no colour-only meaning on alert severity, dynamic-type safety
- [ ] T136 [Android] Add the **observability triad** (`crashReporter.setModule`,
      `performanceTracer.trace`, `featureError` StateFlow) — zero occurrences in this phase, and it
      is required of every feature ViewModel by `apps/finance/CLAUDE.md`
- [ ] T137 [QA] Verify **light and dark** render from the same tokens (N7) and the three responsive
      tiers — neither is planned here, nor anywhere else in the feature for responsiveness

---

## Phase 6i: DB obligations inherited from the Phase 2 readiness decisions (2026-08-23)

Binding conventions: `../001-net-worth-tracker/data-model.md` § "Maintenance conventions" and the
[readiness architecture decisions](../../docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md).
This phase adds **two functions and zero tables** in Postgres; its only table is device-local Room.

- [ ] T138 [SA] **The `grant execute … to authenticated` on both functions must be a hand-appended
      migration statement.** The contracts state the grant; no task adds it, and `db diff` cannot
      emit grants (ADR-0032 caveat). Without it both functions are unreachable — a silent 404, not
      an error that names the cause
- [ ] T139 [SA] **`search_all` reads `payee` and `note`**, not `description`/`counterparty`. Those
      columns do not exist on `finance.transactions`; the contract is the document that is wrong.
      Also depends on **003 T161** for `policies.name`, which this phase's contract returns as the
      result title and which does not exist today
- [ ] T140 [SA] **If `search_all` is written as a view rather than a function, it needs
      `security_invoker = on`** — the same defect Phase 2 found across all 8 planned views. As a
      `security definer` function it needs an explicit `auth.uid()` filter instead. State which it
      is; the contract currently implies a function without saying so unambiguously
- [ ] T141 [SA] **Wire the Room `alert_log` purge into the erasure action** (also T122). It stores
      user money as paise and is excluded from `delete_my_data()` by design, so server-side erasure
      leaves it intact — the one hole in an otherwise correct DPDP cascade. Room is outside
      `delete_my_data()`'s reach by construction, which is exactly why it needs its own explicit step
      rather than an assumption that "erased with the app's data" covers it

---

## Phase 6j: Closure — tracking (runs last, after the checkpoint is green)

Per the tracking rule in `apps/finance/CLAUDE.md`. **This phase creates no Gradle module** — B2 and
B3 land in `:apps:finance:app` — so it gets **no** FEATURES.md module row and belongs in that file's
"Phases that add no Gradle module" table, where it already is. Do not add a module row.

- [ ] T142 [P] Add the **root `CHANGELOG.md`** entry: global search across transactions, holdings,
      policies and goals; the notification centre as the app's own durable record; and the five
      delivered alert types. State that an alert is recorded **whether or not the OS displayed it** —
      that is the design decision a reader is most likely to mistake for a bug
- [ ] T143 [P] Update **`apps/finance/app/README.md`** — B2 and B3 are shell-owned screens, so the
      shell README is this phase's module README
- [ ] T144 [P] Update the **spec-kit tracking table** (implementation plan §7) — Phase 6 to *shipped*.
      Also correct that row's stale "Next: `/speckit-tasks`" text, which has been wrong since a
      37 KB `tasks.md` landed
- [ ] T145 [P] Add the notification channels this phase defines to the **notification registry** in
      `apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md`, each paired
      1:1 with its Settings control and the module that owns it. Correct §1's B2/B3 rows, which say
      FeatureHost key "— (shell)" and no consent while this phase assigns `alerts`/`search` and
      `requiresConsent` for both
- [ ] T146 [P] Record the outcome of the three **orphaned intents** (`QUICK_ADD`, `REVIEW_INBOX`,
      `OPEN_UPCOMING`) and the **Glance widget** in the registry — claimed, or descoped with the
      reason. Leaving them listed but unowned is how they became invisible in the first place