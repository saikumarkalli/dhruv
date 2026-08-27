# Implementation Plan: Automation (Phase 7)

**Branch**: `007-automation` | **Date**: 2026-08-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `apps/finance/specs/007-automation/spec.md`

## Summary

Three screens, one background scanner, and a rule that has to hold everywhere: **nothing an automated
source produces enters the user's records until the user says so** (FR-001). **G2 the review queue**
is the shared destination every source feeds, plus an Ignored list that makes dismissal recoverable.
**G1 the automation hub** is the control surface — one row per source, each stating what it reads —
with the learned-rules list underneath. **G3** states scope, duration and purpose for account
linking, which is not live. Underneath: an hourly on-device scan that turns bank messages into
proposals without their text ever leaving the phone, and a price feed that proposes a value update
only when a metal price has genuinely moved.

Four properties shape every decision below:

- **The SMS path never touches the network.** Phase 3's `finance.suggestions` carries a `raw_text`
  column reserved for this phase, and `AUT-BR-002` forbids raw message text in any outbound Supabase
  payload — a straight contradiction between two shipped documents. Resolved by keeping bank-message
  proposals **entirely device-local** (research R1), which makes AUT-BR-002 structurally true rather
  than an audit obligation. That column stays permanently unused.
- **The queue reads two stores.** Recurring proposals stay in Supabase where Phase 3 put them;
  message and price proposals are Room. One repository merges them, and each row knows its origin
  (R2).
- **No background job writes into tracker data.** The scanner produces local proposals; only an
  explicit accept writes a `transactions` row or a `valuations` row (R3).
- **One new Gradle module.** `:apps:finance:feature:automation` — unlike 006, this phase has real
  feature screens, and implementation plan §6 reserves the module.

Total footprint: 4 screens (G1, G2, G3, Ignored), 1 background worker, 1 new Postgres table, 1 new
Postgres function, 1 new `holdings` column, 1 Room migration (v6 → v7), 1 alert type, 1 feature flag,
1 new manifest permission, 1 new Gradle module, **0 new `NavTarget` cases**, 0 new dependencies.

## Technical Context

**Language/Version**: Kotlin (JDK 17 toolchain, Android Studio JBR), Jetpack Compose

**Primary Dependencies**: Existing only — Compose, Koin, Coroutines/Flow, Retrofit + Moshi + OkHttp
(tracker REST), Room, `androidx.work`. **No new dependency.** `androidx.work` is added and
AGP-9-verified by Phase 6; if 006 has not landed when 7c runs, 7c inherits that add-and-verify task
rather than assuming it. Message reading uses the platform `ContentResolver` against the system SMS
provider — no library.

**Storage**:
- **Supabase Postgres** — one new table `finance.automation_rules` (RLS `user_id = auth.uid()`,
  select/insert/update, no delete policy); one new `security invoker` function
  `finance.find_possible_duplicates`; one new column `finance.holdings.auto_value_series`;
  `public.delete_my_data()` extended.
- **Room (`AppDatabase`, v6 → v7)** — `automation_proposal`, device-local, holding bank-message and
  price proposals including the original message text that must never leave the device.
- **Device-local settings** (004's control plane) — the price move threshold, the scan watermark.

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Turbine + hand-written fakes, per the module
standard. **Room DAOs are exercised through a fake, never an in-memory Room database** — Robolectric
SQLite does not load on this Windows toolchain, an already-recorded constraint.
`work-testing`'s `TestListenableWorkerBuilder` runs the scan worker on the JVM. The three
correctness-critical pieces — the message parser (R9), the price-move predicate (R6) and the
accept-all partition (R15) — are **pure functions with table-driven tests**, so RED is cheap and
lands before any screen or worker exists.

**Target Platform**: Android, minSdk 26 / targetSdk latest. `READ_SMS` is a runtime permission on
every supported API level. A device with no telephony has no SMS provider — FR-028's unavailable
state is a real branch, not a theoretical one.

**Project Type**: Android feature module + Postgres. No web work this phase — web follows one phase
behind, and this phase's central capability (reading device SMS) has no web equivalent at all.

**Performance Goals**: the hourly scan completes well inside a WorkManager execution window on a
30-day backlog; the queue renders without blocking interaction; duplicate detection is one round trip
for the whole visible list, never per row (R10).

**Constraints**: consent gates every network call structurally through the existing
`ConsentInterceptor`; **integer paise end to end** — the parser emits `Long` paise directly and no
`Double` ever appears in the path (Article VII, `checkTrackerMoneyPrecision`); no raw dp/sp/hex in any
screen; every user-visible string in `strings.xml`.

**Scale/Scope**: 4 screens, 4 proposal sources (2 live, 1 inherited, 1 unavailable), 2 proposal kinds,
7 sub-phases.

### What this phase needs from earlier phases

| Needs | From | Used by |
|---|---|---|
| `finance.suggestions` + its `(recurring_id, due_on)` uniqueness | Phase 3 | the queue's recurring origin; the never-propose-twice key it inherits (R6) |
| `transactions`, `accounts`, `categories`, the audit trigger | Phase 3 | accepting a proposal (FR-002's "from SMS" history row is the trigger's work, not new code) |
| D9's recurring review surface | Phase 3 | replaced by the shared queue — this phase discharges that deferral |
| `holdings`, `valuations` (append-only), `v_latest_valuation` | Phase 2 | the price feed's recorded value and its accept-time insert |
| `finance.correct_valuation()` | Phase 2 | **not called here** — named so nobody adds a second correction path (R3) |
| A3's *Read transaction SMS* switch, persisted and revocable | Phase 1 | the message source's gate and the freeze state (R7) |
| The Settings module-entry mechanism, app lock + held-intent dispatch, hide-amounts masking | Settings (004) | the Automation entry, `REVIEW_INBOX` dispatch, FR-051 masking |
| Alert raising, channels, the notification centre, `androidx.work` | Phase 6 | the entries-waiting alert — a new **type**, not a new mechanism |
| `SuggestedRow`, `DayGroupHeader` (batch B4), `InfoBanner`, `StatusBadge` (batch B7) | design system §5.2 | the queue row, day grouping, the frozen banner, the duplicate callout — see R14 |

## Constitution Check

*GATE: passed before Phase 0 research. Re-checked after Phase 1 design — result unchanged.*

| Article | Check | Status |
|---|---|---|
| I. Test-First | Every Backend/Android task cites an `AUT-*` (or newly-written) scenario ID; RED before GREEN. The three pieces most worth testing are pure functions by design (R6, R9, R15), so the RED step is genuinely cheap and precedes every screen and worker | PASS (enforced at task-authoring time) |
| II. Scenarios Before Code | `AUT-*` (9 rows) written and reviewed 2026-08-09 — catalog §9. **Gap found while writing this plan**: the catalog predates five of the seven clarifications and has no row for the Ignored list or restore, none for never-proposing-twice, none for the freeze/unfreeze cycle, none for the periodic-scan and missed-window behaviour, none for accept-all's partition, and none for the price-move predicate. **Six rows QA must write** before 7a, 7c, 7f respectively | **CONDITIONAL** — see the Article II note |
| III. Module Boundaries | One new module `:apps:finance:feature:automation`, reserved by impl plan §6. No `feature → feature` edge: the queue reaches transaction/holding detail through the shell, never by importing another feature. Repositories live in `:apps:finance:data` like every other | PASS by construction |
| IV. Fault Isolation | All four routes wrapped in `FeatureHost` under one `automation` flag. **The flag is genuinely absent from `platform/feature-flags/dhruv-finance.json` today** — verified, not assumed (R12) — so this phase adds it, `enabled: false`, `requiresConsent: true`. Flag off also means the scan worker is never enqueued and an enqueued one is cancelled | PASS — flag added by 7b |
| V. No Hardcoding | Every screen reads `DhruvNextType`/`Spacing`/`Radii`/`LocalDhruvNextColors`. Screen-level data — the source table, the bank-sender allowlist, the scan cadence, the default threshold, the duplicate window — lives in `AutomationConfig.kt`. Every string a `strings.xml` resource | PASS by construction, verified at review |
| VI. Component Reuse | Two components owed by design-system batch B4 (`SuggestedRow`, `DayGroupHeader`) and two by B7 (`InfoBanner`, `StatusBadge`); the duplicate callout closes §5.3's stated `CountBadge` gap **by extending `CountBadge`**, never a second badge. Eighteen further components verified present by symbol search 2026-08-23 | PASS — see R14 |
| VII. Money Is Exact | The parser emits `Long` paise directly from the message text — no `Double` intermediate, which is the one place this phase could plausibly have introduced one. Duplicate matching compares `bigint` in SQL. The price predicate compares paise against a percentage using integer arithmetic, stated as such in `contracts/price-feed.md` | PASS |
| VIII. Consent Before Network | Two gates, not one. The existing `ConsentInterceptor` gates every PostgREST call including from the worker (Phase 6 established the background case). *Read transaction SMS* is a **second, additive** gate that the message source checks before touching the SMS provider — a device-side gate for a device-side read, since no interceptor sits in front of a `ContentResolver` | PASS — see the Article VIII note |
| IX. Append-Only History | Three new TEXT-persisted enum sets — proposal origin, proposal kind, auto-value series — append-only from birth. **`automation_rules` is deliberately NOT append-only**: it is current state, not history, so select/insert/update is correct and Article IX's SELECT+INSERT-only shape would be wrong here. `finance.valuations` stays append-only and this phase only ever inserts into it | PASS — see the Article IX note |
| IXa. Authorization Is Server-Side | `automation_rules` gets RLS scoped to `auth.uid()` and an explicit grant (custom schemas have no implicit exposure, ADR-0033). `find_possible_duplicates` is **`security invoker`** so RLS applies — a `security definer` here would let one user probe another's transactions by amount. Per-phase RLS test asserts a second user reads zero rows | PASS |
| X. Coverage Ratchets | One new module — `:apps:finance:feature:automation` must be added to `coveredModules`, a step a new module makes easy to forget and which silently excludes it from the floor. Explicit 7b task. Floor ratchets at each sub-phase checkpoint, only to the measured value | PASS with a named task |
| XI. Stack Is Fixed | **Zero new dependencies.** SMS reading uses the platform `ContentResolver`; scheduling reuses `androidx.work`. No stack choice is reopened | PASS |
| Xa. Documentation Tracks Reality | Closure tasks: FEATURES.md row, the module's README, CHANGELOG, the spec's Implementation record, registry §1/§3/§4 rows — including **deleting registry §4's stale *Recently deleted* line** (FR-054), which belongs to Phase 0b | PASS — 7g |

**Article II note.** The nine `AUT-*` rows cover what the design drew: suggestion-not-transaction,
on-device parsing, rule counts, the dashed row, the duplicate callout, accept, ignore, the AA consent
statement, and the end-to-end chain. They cannot cover the six behaviours the 2026-08-23
clarifications introduced, because those behaviours did not exist when the catalog was written. The
rows are named in the sub-phase table and QA writes each at step 2 of its slice, before any code.
Recorded CONDITIONAL so this is a task rather than a discovery.

**Article VIII note — why there are two gates and why that is not redundant.** Everywhere else in this
app, "consent before network" is the whole story, and `ConsentInterceptor` enforces it structurally.
This phase is the first where the sensitive read is **not a network call**: reading the SMS inbox is a
local `ContentResolver` query that no interceptor sits in front of. So the message source carries its
own explicit gate on *Read transaction SMS* **plus** the Android runtime permission, and FR-020
requires both. The gate is checked in the scanner's entry point, not in the screen, so there is no
path to the SMS provider that skips it — a test asserts this directly rather than trusting the wiring.

**Article IX note — one table is deliberately mutable.** This project has already shipped one
self-contradicting spec by claiming append-only for a table it then needed to update (the constitution
records it). To avoid repeating it: `finance.automation_rules` is **current state** — a rule is
disabled, removed, and its applied count increments — so it takes an UPDATE policy and that is
correct, not a violation. What stays append-only is `finance.valuations`, which this phase only ever
INSERTs into (R3), and the three new enum sets.

## Sub-phases

Seven independently shippable slices. Each ends green on `./gradlew regressionCheck`, closes its own
scenario rows, ratchets the floor to its measured value, and merges separately.

```
setup ──▶ 7a ──▶ 7b ──▶ 7c ──▶ 7d
                 │             └▶ 7e
                 └────────────────────▶ 7f
          7a, 7d, 7e, 7f ─────────────▶ 7g
```

**A shared setup slice precedes 7a** (found while writing `tasks.md`, corrected here): the Gradle
module, its `projectDir` remap, its `coveredModules` registration and the `automation` flag key are
not 7b's work as an earlier draft of this table said — **7a ships routes**, and Article IV requires
every route to be `FeatureHost`-wrapped with a flag entry, so both must exist before the first screen.
7b owns the hub, the Settings entry and G3, not the module's creation.

| Sub-phase | Ships | Spec stories | QA rows | Blocked by |
|---|---|---|---|---|
| **7a** Queue + Ignored list | Room v6→v7 `automation_proposal`, the merged two-store repository, the proposal-kind discriminator, `SuggestedRow`/`DayGroupHeader` if Phase 3 has not landed batch B4, G2 with accept / correct-and-accept / ignore / accept-all, the Ignored list with restore, all six screen states | US1 | `AUT-UI-001`, `AUT-FLOW-001`, `AUT-FLOW-002` + **2 new** (Ignored/restore, never-twice) | Phases 2, 3 |
| **7b** Hub + G3 | G1 with its source rows and header rule, the Settings › Automation entry, G3's consent statement with the source marked unavailable | US2, US7 | `AUT-FLOW-003` | 7a |
| **7c** Bank message source | `READ_SMS` + its request path, the sender allowlist, the pure parser, the hourly scan worker and its watermark, the two-gate check, the freeze/unfreeze cycle, the unparseable row | US3 | `AUT-BR-001`, `AUT-BR-002` + **2 new** (scan/missed-window, freeze cycle) | 7b |
| **7d** Duplicate detection | `finance.find_possible_duplicates`, the batched call, the callout on the row, the stated matching rule, accept-all's duplicate exclusion | US4 | `AUT-UI-002` + **1 new** (accept-all partition) | 7c |
| **7e** Learned rules | `finance.automation_rules` + RLS + grants, teach-from-correction, rule application at propose time, the hub's rule list with counts, disable and remove | US5 | `AUT-BR-003` | 7c |
| **7f** Price feed | `holdings.auto_value_series`, the per-holding opt-in, the move-threshold setting, the pure price predicate, the `VALUE_UPDATE` proposal rendering, accept → `valuations` insert, the unreachable-source row | US8 | **1 new** (price predicate table) | 7b |
| **7g** Alert, erasure, closure | The entries-waiting alert type on Phase 6's pipeline, `REVIEW_INBOX` dispatch through the app lock, `delete_my_data()` extension + the device-local erasure arm, masking sweep, the Sec/DPDP pass, registry corrections, `AUT-FLOW-004`'s manual chain walk, flag flipped on | US6 | `AUT-FLOW-004` + full closure | 7a, 7d, 7e, 7f, Phase 6 |

**Why the queue is first and the hub second.** The queue is the only slice with no permission, no new
source and no flag dependency — Phase 3 already produces recurring proposals with nowhere shared to
review them, so 7a delivers the deferral two phases are waiting on, using data that already exists.
The hub is second because it is the switch every later source needs; building a source before its
control surface means shipping something the user cannot turn off.

**Why SMS is third, not first.** It is the headline capability and the riskiest slice — a runtime
permission, a background job, a parser and a consent class. Landing it into an already-working,
already-tested queue means a parser bug shows up as a bad row on a good screen, not as a broken
feature.

**Why the price feed forks off 7b rather than following 7c.** It shares nothing with the message path
except the queue: no permission, no parser, no freeze. Sequencing it behind SMS would block a small,
self-contained slice on the largest one.

**Why erasure and the alert land last, together.** Both need every proposal source to exist to be
provable — an erasure test that runs before the price feed exists proves nothing about price
proposals, and the entries-waiting alert should be able to count every kind of thing that can wait.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/007-automation/
├── plan.md                       # this file
├── spec.md                       # what and why (7 clarifications, 2026-08-23)
├── research.md                   # Phase 0 — R1..R16, each with its rejected alternative
├── data-model.md                 # Phase 1 — the Room table, the Postgres table, the enum sets
├── contracts/
│   ├── routes.md                 # 4 routes, the flag, the zero-new-NavTarget finding, REVIEW_INBOX
│   ├── review-queue.md           # the merged-store contract, accept/ignore/restore, accept-all
│   ├── sms-source.md             # permission + consent gates, allowlist, parser result, watermark
│   └── price-feed.md             # the move predicate and its case table, accept semantics
├── quickstart.md                 # Phase 1 — how to prove each sub-phase end to end
├── checklists/requirements.md    # spec quality gate — 24/24
└── tasks.md                      # /speckit-tasks output — NOT created by /speckit-plan
```

### Source Code (repository root)

```text
apps/finance/feature/shell/automation/          # new Gradle module (:apps:finance:feature:automation)
└── src/main/java/com/dhruv/finance/automation/
    ├── ui/queue/
    │   ├── ReviewQueueScreen.kt                # 7a — G2
    │   ├── ReviewQueueViewModel.kt
    │   ├── ReviewQueueUiState.kt
    │   ├── IgnoredListScreen.kt                # 7a — the restore surface
    │   └── AcceptAllPartition.kt               # 7a — pure (R15)
    ├── ui/hub/
    │   ├── AutomationHubScreen.kt              # 7b — G1
    │   ├── AutomationHubViewModel.kt
    │   ├── AccountLinkConsentScreen.kt         # 7b — G3
    │   └── AutomationConfig.kt                 # source table, allowlist, cadence, defaults
    ├── sms/
    │   ├── SmsScanWorker.kt                    # 7c — hourly, watermarked
    │   ├── SmsInboxReader.kt                   # 7c — ContentResolver, the only provider touch
    │   ├── BankSenderAllowlist.kt              # 7c — pure
    │   └── SmsTransactionParser.kt             # 7c — pure (R9)
    ├── price/
    │   ├── PriceScanWorker.kt                  # 7f
    │   └── PriceMovePredicate.kt               # 7f — pure (R6)
    └── di/AutomationModule.kt                  # Koin

apps/finance/data/src/main/java/com/dhruv/finance/data/
├── AppDatabase.kt                              # 7a — v6 → v7, + MIGRATION_6_7
├── automation/
│   ├── AutomationProposalEntity.kt             # 7a — device-local, never synced
│   └── AutomationProposalDao.kt                # 7a — tested through a fake
└── tracker/
    ├── automation/
    │   ├── ProposalRepository.kt               # 7a — merges Room + finance.suggestions (R2)
    │   ├── SuggestionsApi.kt                   # 7a — Phase 3's table, read/update
    │   ├── DuplicateApi.kt                     # 7d — rpc/find_possible_duplicates
    │   ├── AutomationRuleApi.kt                # 7e
    │   └── AutomationRuleRepository.kt         # 7e
    ├── dto/{SuggestionDto,DuplicateMatchDto,AutomationRuleDto}.kt
    ├── model/{Proposal,ProposalKind,ProposalOrigin,AutomationRule,AutoValueSeries}.kt
    └── mapper/{ProposalMapper,AutomationRuleMapper}.kt

libs/core/src/main/kotlin/com/dhruv/core/ui/components/
├── SuggestedRow.kt                             # 7a — batch B4, if Phase 3 has not landed it
├── DayGroupHeader.kt                           # 7a — same batch, same rule (006 also claims it)
├── InfoBanner.kt                               # 7c — batch B7, the frozen banner
└── CountBadge.kt                               # 7d — extended with the status-dot variant (§5.3)

supabase/
├── schemas/finance/
│   ├── 10_tables/{automation_rules.sql, holdings.sql}   # new table; + auto_value_series column
│   └── 30_functions/find_possible_duplicates.sql        # 7d, security invoker
├── schemas/public/30_functions/delete_my_data.sql       # 7g — + automation_rules
└── migrations/<generated>_automation.sql

platform/feature-flags/dhruv-finance.json        # + "automation" (absent today — R12)
apps/finance/app/src/main/AndroidManifest.xml    # + READ_SMS (7c)
apps/finance/app/.../MainActivity.kt             # 7g — REVIEW_INBOX extra
settings.gradle.kts                              # + the module + its projectDir remap
build.gradle.kts (root)                          # 7b — + coveredModules entry (Article X)
```

**Structure Decision**: one new Gradle module, `:apps:finance:feature:automation`, physically at
`apps/finance/feature/shell/automation/` with a `projectDir` remap — the bucket scheme from the
2026-08-09 folder layout, and `shell/` is correct because G1–G3 belong to no tab (R13), exactly like
`assistant` and the converters. Repositories and the Room entity live in `:apps:finance:data` because
that is where every repository lives and because the screens and both workers must read the same
store. Postgres objects are authored declaratively under `supabase/schemas/finance/`, with
`supabase db diff` generating the executed migration (ADR-0032), and the grants and `security invoker`
clause hand-appended because `db diff` emits neither (Article IXa).

## Complexity Tracking

No constitutional violation requires justification. Four decisions cost more than the obvious
alternative and are recorded here, because each looks like over-engineering without `research.md`.

| Decision | Why | Simpler alternative rejected because |
|---|---|---|
| A device-local proposal store **in addition to** Phase 3's Supabase one | `AUT-BR-002` forbids raw message text in an outbound payload while FR-005 requires showing it on the row; local storage makes the rule structurally true instead of an audit obligation (R1) | Reusing `finance.suggestions.raw_text` puts message text in a synced table — a direct violation. Splitting the row across both stores yields a half-synced entity whose second-device copy is missing the field the user needs |
| `frozen` derived at read time, never persisted | Re-granting consent must restore rows "exactly as they were, with no duplicates" (FR-026c); derived state makes that free (R7) | A stored status needs a migration on withdrawal, a reverse one on re-grant, and a recovery path for a crash between them — three ways to produce the duplicates FR-026c forbids |
| A two-term price predicate rather than "propose when it moved" | FR-045 and FR-045a read as contradictory — a later move must propose, the refused move must not return. Two terms satisfy both and the pure function proves it (R6) | One term either re-proposes the move the user just refused, or suppresses a genuinely new one. Both are visible bugs |
| Accept-all as a pure partition returning three skip buckets | Guarantees SC-006a by unit test over every queue composition rather than by clicking one | Filtering inline in the ViewModel makes "accept-all never records a duplicate" unprovable except by UI test, and this is the one action that can corrupt data in a single tap |