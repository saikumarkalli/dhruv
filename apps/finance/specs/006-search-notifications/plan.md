# Implementation Plan: Search & Notifications (Phase 6)

**Branch**: `006-search-notifications` | **Date**: 2026-08-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `apps/finance/specs/006-search-notifications/spec.md`

## Summary

Two shell-owned screens and one background pipeline. **B3 global search** is a single field over
transactions, holdings, policies and goals, served by one new parameterised `finance` function that
returns a typed union with exact per-kind counts — one round trip, no client-side fan-out, no money
arithmetic on the device. **B2 notification centre** is a device-local record in Room of every alert
this app has raised, grouped by local calendar date, with persistent read state and a 90-day
retention window. **The delivery pipeline** is a daily WorkManager job that asks a second `finance`
function which alert conditions are currently true, de-duplicates them against the local log by a
stable key, and posts what is new to the channel that owns it.

Three properties shape every decision below:

- **No new Postgres tables.** Every preference this phase consumes was already stored by Phases 3–5;
  every record it searches already exists. The database footprint is exactly **two functions** —
  `finance.search_all` and `finance.due_alerts` — both security-invoker, so RLS does the isolation
  and this phase adds no policy of its own.
- **No new Gradle module.** The implementation plan §6 assigns B2 and B3 to `:apps:finance:app`
  alongside Home and the Plan root. That is honoured: screens and the worker live in the shell, the
  repositories live in `:apps:finance:data`, and both modules are already in `coveredModules`.
- **No new `NavTarget` case.** Every destination this feature dispatches to is a sealed case an
  earlier phase already added. Phase 6 is the first real *consumer* of five of them — including
  `OpenBudget`, which Phase 4 added speculatively and explicitly for this feature.

Total footprint: 2 screens, 1 background worker, 2 SQL functions, 1 Room migration (v5 → v6), 5
notification channels, 2 feature flags, 1 new manifest permission, 0 new modules, 0 new
`NavTarget` cases, 1 new dependency (`androidx.work`).

## Technical Context

**Language/Version**: Kotlin (JDK 17 toolchain, Android Studio JBR), Jetpack Compose

**Primary Dependencies**: Existing — Compose, Koin, Coroutines/Flow, Retrofit + Moshi + OkHttp
(tracker REST), Room. **New — `androidx.work:work-runtime-ktx`** for periodic evaluation, plus
`androidx.work:work-testing` as a test dependency. WorkManager is already named as the platform's
background mechanism in `PLATFORM.md` §3 and §5; this phase is the first to actually add it. It is a
plain AndroidX library with no Gradle plugin — the same risk class as `credentials`/`googleid`, which
ADR-0029 accepted, and explicitly *not* the plugin-compatibility class that ruled out Hilt and Kover.

**Storage**:
- **Supabase Postgres** — read only, through two new `finance` functions. No new table, no new RLS
  policy, no new grant beyond `execute` on the two functions to `authenticated`.
- **Room (`AppDatabase`, v5 → v6)** — one new device-local table, `alert_log`, holding every alert
  raised on this device plus its read state and its dedupe key. This is delivery state, not a
  financial record: it never leaves the device and is never synced.

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Turbine + hand-written fakes, per the module
standard. **Room DAOs are tested through a fake, never an in-memory Room database** — Robolectric's
SQLite does not load on this Windows toolchain, an already-recorded constraint. `work-testing`'s
`TestListenableWorkerBuilder` runs the worker's logic on the JVM without a real scheduler.

**Target Platform**: Android, minSdk 26 / targetSdk latest. `POST_NOTIFICATIONS` is an API 33+
runtime permission; on API 26–32 channels exist and notifications post with no runtime grant, so the
permission path is a version branch, not a universal gate.

**Project Type**: Android app (shell-owned screens) + Postgres functions. No web work this phase —
web follows one phase behind, schema-sequenced.

**Performance Goals**: NFR-8 — search results render without blocking interaction over a year of
daily recording; all evaluation and query work off the main thread; the centre list is virtualised.

**Constraints**: Consent gates every network call structurally through the existing
`ConsentInterceptor`; integer paise end to end (the search RPC returns `bigint`, never `numeric`);
no raw dp/sp/hex in any screen; every user-visible string in `strings.xml` — including every
notification title and body, which is why the RPC returns structured values and never a
pre-formatted sentence.

**Scale/Scope**: 2 screens, 5 delivered alert types, 4 searchable record kinds, 6 sub-phases.

### What this phase needs from earlier phases

| Needs | From | Used by |
|---|---|---|
| `holdings`, `valuations`, `v_latest_valuation` | Phase 2 | search (holdings), the value-overdue arm |
| `liabilities_meta` (`debit_day`, instalment fields) | Phase 2 | the instalment-due arm |
| `transactions` (`description`/counterparty, `amount_paise`, `occurred_at`), `categories` | Phase 3 | search (transactions), the budget-breach arm's spend |
| `budgets.alert_pct`, budget period resolution | Phase 4 | the budget-breach arm — the preference stored and left unconsumed by research R8 |
| `goals` (name, target, progress) | Phase 4 | search (goals) |
| `policies` (name, insurer, renewal date), `policies.remind_days_before` | Phase 4 | search (policies), the renewal arm — the second R8 preference |
| The monthly-summary preference, `OpenReports(period)` | Phase 5 | the monthly-summary arm |
| `notifications_master`, the permission-state row, hide-amounts, the app-lock gate and its held-intent dispatch | Settings (004) | every alert; consumed, never re-specified |
| `NavTarget.OpenHolding`/`OpenLiability` (Phase 2), `OpenBudget`/`OpenPolicy`/`OpenGoal` (Phase 4), `OpenReports` (Phase 5), `OpenAccount` (Phase 3) | Phases 2–5 | every alert destination and every search result |
| `DayGroupHeader` (design system §5.2 batch B4) | Phase 3's ledger | the centre's TODAY/EARLIER headers — verified present at 6b, built into `:libs:core` if Phase 3 has not landed it |
| `SearchField`, `Chip`, `ModeChipRow`, `ListGroup`, `ListGroupRow`, `SectionLabel`, `CountBadge`, `EmptyStateCard`, `SignedOutCard`, `OfflineStateCard`, `SkeletonBlock`, `RetryErrorCard`, `NxTopBar`, `MoneyText` | built today | both screens — verified by symbol search 2026-08-22 |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design — result unchanged.*

| Article | Check | Status |
|---|---|---|
| I. Test-First | Every Backend/Android task cites an `SRC-*` (or newly-written) scenario ID; RED before GREEN. The two pieces most worth testing — the dedupe key and the alert-suppression ladder — are pure functions, so RED is genuinely cheap and lands before any worker or screen exists | PASS (enforced at task-authoring time) |
| II. Scenarios Before Code | `SRC-*` (5 rows) written and reviewed 2026-08-09 — catalog §10. **Gap found while writing this plan**: the catalog has no row for dedupe/no-duplicate, no row for the missed-window backlog, no row for per-channel masking, no row for search's offline and signed-out states, no row for the retention window, and no row for the settings-control sweep. Six rows QA must write before 6c, 6a and 6f respectively | **CONDITIONAL** — see the Article II note below |
| III. Module Boundaries | No new module and no `feature → feature` edge. Screens live in `:apps:finance:app` (impl plan §6 assigns B2/B3 to the shell); repositories live in `:apps:finance:data`; every jump to another feature's screen goes through an existing `NavTarget` case resolved by the shell's dispatcher | PASS by construction |
| IV. Fault Isolation | Both routes wrapped in `FeatureHost`. **Two new flags** — `search` and `alerts`, both `requiresConsent: true`, both mapped to the "Sync my financial records" A3 switch. Neither is reserved in impl plan §5.5, so both are additions this phase makes to that list. `alerts` off also means the worker is never enqueued — a real kill switch for a background job, not only a screen gate | PASS — flags added by 6a and 6b |
| V. No Hardcoding | Both screens read `DhruvNextType`/`Spacing`/`Radii`/`LocalDhruvNextColors`. Screen-level data — the four searchable kinds and their secondary-line shapes, the channel table, the retention window, the evaluation cadence — lives in `SearchConfig.kt` and `AlertConfig.kt`. Every notification string is a `strings.xml` resource, which is *why* the RPC returns structured values rather than sentences | PASS by construction, verified at review |
| VI. Component Reuse | Fourteen components verified present by symbol search on 2026-08-22. Two gaps, both handled by extension rather than a parallel component: **`DayGroupHeader`** is design-system §5.2 batch B4, owed by Phase 3's ledger — inherited if Phase 3 landed it, otherwise built once in `:libs:core`; the **unread dot** is design-system §5.3's stated `CountBadge` gap ("has numeric count; design also draws status-dot variants") and is closed by **extending `CountBadge`**, never by adding a second badge | PASS — see `research.md` R9 |
| VII. Money Is Exact | The search RPC returns `bigint` paise; the alert RPC returns `bigint` paise and integer percentages. No `numeric`, no float, no client-side money arithmetic — every threshold comparison happens in SQL where the period boundaries are already known | PASS |
| VIII. Consent Before Network | Both functions are called through the existing consent-gated `dataClient`; no new HTTP client is constructed. Consent off means the interceptor short-circuits, which means the worker evaluates nothing and raises nothing — the gate is structural, including in the background | PASS — inherited gate |
| IX. Append-Only History | Two new persisted enum sets — the alert type and the searchable record kind — are TEXT-persisted and append-only from birth. The dedupe key's grammar is equally append-only: changing how a key is formed would silently re-raise every past alert, which `research.md` R4 states as a rule rather than leaving it to be discovered | PASS by construction |
| X. Coverage Ratchets | No new Gradle module, so nothing to register — `:apps:finance:app` and `:apps:finance:data` are already in `coveredModules`. Floor is `0.09` today and is ratcheted at each sub-phase checkpoint, only to the measured value | PASS |
| XI. Stack Is Fixed | One new dependency, `androidx.work`. This does **not** reopen a settled stack choice: WorkManager is already the platform's named background mechanism (`PLATFORM.md` §3, §5) and is a plain AndroidX library with no Gradle plugin. Its first-build verification on AGP 9 is an explicit 6c task, not an assumption | PASS with a stated verification task — see `research.md` R2 |

**Article II note.** The `SRC-*` rows cover what the design drew: the search chip counts, the
result-to-detail jumps, the today/earlier grouping, mark-all-read persistence, and one deep-link per
alert type. They do not cover the machinery underneath, because that machinery was not designed on a
screen. Six rows are missing and are named above; QA writes them at step 2 of the sub-phase that
needs them, before any code in that sub-phase. This is recorded as CONDITIONAL rather than PASS so
the gap is a task rather than a discovery.

**Article IV note — why two flags and not one.** Search and alerts fail independently and are
switched off for different reasons: a slow search function is a UI problem, a misbehaving worker is a
battery and trust problem. One flag would mean disabling a broken background job also removes a
working search field. The registry rows for both currently read "— (shell)" in the FeatureHost-key
column, which meant "no feature *module*"; this phase adds the flag keys and updates those two rows.

**Article VIII note — the background path is the one that matters.** Every existing consent check in
this app happens with a user in front of it. The worker is the first code that talks to PostgREST
with nobody watching, so the structural gate is doing real work here for the first time: nothing in
the alert path constructs its own client, and there is no code path from the worker to PostgREST that
bypasses `ConsentInterceptor`. A test asserts this directly rather than trusting the wiring.

## Sub-phases

Six independently shippable slices. Each ends green on `./gradlew regressionCheck`, closes its own
scenario rows, ratchets the floor to its measured value, and merges separately.

```
6a  (independent)
6b  (independent) ──▶ 6c ──▶ 6d
                            └▶ 6e
6a, 6d, 6e ─────────────────────▶ 6f
```

| Sub-phase | Ships | Spec stories | QA rows | Blocked by |
|---|---|---|---|---|
| **6a** Global search (B3) | `finance.search_all`, `SearchRepository`, the `search` flag, the Home top-bar entry point, the B3 screen with chips, grouping and all five states | US1 | `SRC-UI-001`, `SRC-FLOW-001`, + 1 new states row | Phases 2, 3, 4 |
| **6b** Notification centre (B2) | Room v5→v6 `alert_log`, `AlertLogDao` + fake, `AlertRepository` read side, the `alerts` flag, the centre screen replacing today's stub, TODAY/EARLIER grouping, read state, mark-all-read, unread badge, 90-day retention | US3 | `SRC-UI-002`, `SRC-FLOW-002`, + 1 new retention row | Phase 0 shell (present) |
| **6c** Pipeline + first arm: budget breach | `androidx.work` added and verified on AGP 9, `finance.due_alerts` with its budget arm, the dedupe key, the suppression ladder, channel registration, `POST_NOTIFICATIONS` + its request path, the worker and its scheduling, posting, and deep-link dispatch through `OpenBudget` including the locked path | US2, US4 | `SRC-FLOW-003` (budget leg), **`PLN-FLOW-003`** — the row Phase 4 deferred to here — + 2 new rows (dedupe, missed window) | 6b, Phase 4 |
| **6d** Obligation arms | The instalment-due and policy-renewal arms, their two channels, their "Mark paid" quick action, dispatch through `OpenLiability` and `OpenPolicy` | US5 | `SRC-FLOW-003` (EMI and renewal legs) | 6c |
| **6e** Periodic arms | The value-overdue arm (server-evaluated) and the monthly-summary arm (device-evaluated, no RPC), their two channels, dispatch through `OpenHolding` and `OpenReports` | US6 | + 1 new row per arm | 6c, Phase 5 |
| **6f** Controls, masking, closure | Per-type control verification against the module that owns each channel, the app-wide master switch, the permission-denied banner path, the privacy-mode masking sweep across all five types, the Sec pass, and the QA closure of every `SRC-*` row | US7 | + 1 new control-sweep row, plus the full-catalogue closure | 6a, 6d, 6e |

**Why 6a is first and standalone.** It is the only slice with no worker, no permission, no Room
migration and no dependency on the alert design. It is also the half of the feature the user asks
for rather than receives, so it delivers value on its own even if the pipeline slips.

**Why the centre precedes the pipeline.** 6b ships a correct, empty centre — the same thing the user
sees today, but now backed by a real store with real read state and real retention, provable by
seeding the fake. Building it after the pipeline would mean the first alert ever raised lands in an
untested screen.

**Why one arm at a time.** Each arm is a SQL branch plus a channel plus a destination. Landing them
together would make a failing arm block four working ones, and would make the first checkpoint carry
five sets of scenario rows at once.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/006-search-notifications/
├── plan.md                       # this file
├── spec.md                       # what and why (clarified 2026-08-22)
├── research.md                   # Phase 0 — R1..R10, every decision with its rejected alternative
├── data-model.md                 # Phase 1 — the Room table, the two RPC shapes, the enum sets
├── contracts/
│   ├── routes.md                 # route rows, the two flags, the zero-new-NavTarget finding
│   ├── search-rpc.md             # finance.search_all — signature, matching, counts, caps
│   └── alert-pipeline.md         # finance.due_alerts, dedupe grammar, worker + channel contract
├── quickstart.md                 # Phase 1 — how to prove each sub-phase end to end
├── checklists/requirements.md    # spec quality gate — 24/24
└── tasks.md                      # /speckit-tasks output — NOT created by /speckit-plan
```

### Source Code (repository root)

```text
apps/finance/app/src/main/java/com/dhruv/finance/app/
├── ui/search/
│   ├── SearchScreen.kt                  # 6a — B3
│   ├── SearchViewModel.kt
│   ├── SearchUiState.kt
│   └── SearchConfig.kt                  # the four kinds, their secondary-line shapes, debounce
├── ui/notifications/
│   ├── NotificationCentreScreen.kt      # 6b — B2, replaces ui/shell/NotifScreen.kt
│   ├── NotificationCentreViewModel.kt
│   ├── NotificationCentreUiState.kt
│   └── AlertConfig.kt                   # channel table, retention window, cadence
├── alerts/
│   ├── AlertEvaluationWorker.kt         # 6c — the periodic job
│   ├── AlertScheduler.kt                # 6c — enqueue/cancel, driven by the `alerts` flag
│   ├── AlertChannels.kt                 # 6c/6d/6e — channel registration, one per type
│   ├── AlertNotifier.kt                 # 6c — builds and posts; formats and masks on device
│   └── AlertSuppression.kt              # 6c — the pure ladder: flag, consent, master, channel
├── ui/shell/NotifScreen.kt              # 6b — deleted, replaced by ui/notifications/
└── MainActivity.kt                      # 6a/6c — search entry point, alert intent extras

apps/finance/data/src/main/java/com/dhruv/finance/data/
├── AppDatabase.kt                       # 6b — v5 → v6, + MIGRATION_5_6
├── alerts/
│   ├── AlertLogEntity.kt                # 6b — device-local, never synced
│   └── AlertLogDao.kt                   # 6b — tested through a fake, not in-memory Room
└── tracker/
    ├── search/
    │   ├── SearchApi.kt                 # 6a — rpc/search_all on the consent-gated dataClient
    │   └── SearchRepository.kt
    ├── alerts/
    │   ├── DueAlertsApi.kt              # 6c — rpc/due_alerts
    │   ├── AlertRepository.kt           # 6b read side, 6c write side
    │   └── AlertKey.kt                  # 6c — the pure dedupe-key function
    ├── dto/{SearchHitDto,DueAlertDto}.kt
    ├── model/{SearchHit,SearchKind,DueAlert,AlertType}.kt
    └── mapper/{SearchMapper,AlertMapper}.kt

libs/core/src/main/kotlin/com/dhruv/core/ui/components/
├── DayGroupHeader.kt                    # 6b — only if Phase 3 has not already landed batch B4
└── CountBadge.kt                        # 6b — extended with the status-dot variant (§5.3)

supabase/
├── schemas/finance/30_functions/
│   ├── search_all.sql                   # 6a
│   └── due_alerts.sql                   # 6c, extended by 6d and 6e
└── migrations/<generated>_search_and_alerts.sql

platform/feature-flags/dhruv-finance.json    # + "search", + "alerts"
apps/finance/app/src/main/AndroidManifest.xml # + POST_NOTIFICATIONS (6c)
gradle/libs.versions.toml                     # + androidx.work runtime + testing (6c)
```

**Structure Decision**: shell-owned, per implementation plan §6, which assigns **"Home 01, B2, B3,
shell, Plan root E1"** to `:apps:finance:app`. No new Gradle module is created. Repositories and the
Room entity live in `:apps:finance:data` because that is where every other repository lives and
because the worker, the centre screen and any future widget must all read the same store. The two
SQL functions live under `supabase/schemas/finance/30_functions/` and are authored declaratively,
with `supabase db diff` generating the executed migration (ADR-0032).

## Complexity Tracking

No constitutional violation requires justification. Three decisions cost more than the obvious
alternative and are recorded here with their reason, because each will look like over-engineering to
a reader who has not read `research.md`.

| Decision | Why | Simpler alternative rejected because |
|---|---|---|
| A server-side `due_alerts` function rather than five client-side evaluators | Period arithmetic and money comparison already exist in SQL for Phases 4 and 5; one round trip; no paise math on the device (Article VII) | Five repository reads means five network calls per evaluation, duplicated period logic, and client-side money comparison — three problems this project has explicit rules against |
| A device-local dedupe log rather than server-side "last notified" columns | The log is delivery state, not a financial record; keeping it local means no new table, no new RLS policy, no new DPDP erasure obligation, and no write path from a background job into tracker data | Server-side state means new columns on four tables, a write from the background worker into the user's financial records, and a second thing `delete_my_data()` must know about |
| Two feature flags instead of one | Search and the worker fail for unrelated reasons and must be switchable independently; `alerts` off also stops the job being enqueued at all | One flag means disabling a misbehaving background job also removes a working search field |