# Implementation Plan: Money tab — usability remediation

**Branch**: `008-money-tab-ux-remediation` | **Date**: 2026-09-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `apps/finance/specs/008-money-tab-ux-remediation/spec.md`

## Summary

`002-money-tab` shipped with six of its own requirements delivered in name only and four defects that
stop a real user completing the module's primary task. This feature closes them.

The dominant shape is **reconnection, not construction**: editing a transaction, editing an account,
editing a recurring rule, saving a filter view, splitting a payment and attaching a receipt all have
working, in some cases unit-tested, data layers and no screen that reaches them. Research confirmed
this quantitatively — **zero new dependencies, zero new tables or columns, one migration** (a trigger
extension), two shared components, two navigation targets.

Two findings changed the plan's shape rather than its size. First, the missing back control is not
cosmetic: with no on-screen control and no back-press handler anywhere in the module, the phone's back
gesture is both the only way out of five screens and the one route that bypasses the unsaved-changes
confirmation. Second, the history trigger does not record a transaction's `type`, so the type-change
editing this feature introduces would have appended no history at all — falsifying a requirement the
feature itself is written to deliver.

## Technical Context

**Language/Version**: Kotlin 2.x, JDK 17 (Android Studio JBR), AGP 9

**Primary Dependencies**: Jetpack Compose (BOM `2026.08.00`), Koin (DI — never Hilt, ADR-0010),
Retrofit + Moshi + OkHttp against Supabase PostgREST (ADR-0029), `androidx.activity` `1.13.0`,
Coil `2.7.0`. **No new dependency is added by this feature** (research R2, R3).

**Storage**: Supabase Postgres with RLS `user_id = auth.uid()` for all tracker data (ADR-0014). Room
is the calculators' store only and is untouched. Receipts are app-private device storage via the
existing `ReceiptStore` and are never uploaded (FR-234).

**Testing**: JUnit4 + `kotlinx-coroutines-test` + Turbine + hand-written fakes, per the repo's
existing money-module tests. `./gradlew regressionCheck` is the gate: all unit tests + ArchUnit +
merged JaCoCo + the coverage floor. Robolectric-SQLite is unusable on Windows here, so data access is
tested through fakes — the convention already in place.

**Target Platform**: Android, minSdk 26, targetSdk latest. Phone-first; tablet and small-width tiers
per DESIGN-SYSTEM §3.

**Project Type**: Android feature module inside a Gradle monorepo — `:apps:finance:feature:money`,
with wiring in `:apps:finance:app` and two components added to `:libs:core`.

**Performance Goals**: No new performance target. App-wide scroll and render benchmarking is
deliberately deferred to the future performance spec (implementation plan §7a); this feature adds no
list that does not already exist. One structural note carried from the review: the filter sheet
recomputes its result count synchronously in composition — in scope to *not make worse*, not to
measure.

**Constraints**: Consent gates every tracker network call structurally via `ConsentInterceptor`
(Article VIII) — seeding therefore runs after sign-in and after consent. Money is integer paise end
to end (Article VII), enforced by `checkTrackerMoneyPrecision`. No raw dp/sp/hex in any screen
(Article V). No `feature → feature` dependency (Article III).

**Scale/Scope**: 10 user stories, 45 requirements, 4 of them P1. Nine existing screens touched, two
new `:libs:core` components, one migration, one Settings contribution rewritten.

## Constitution Check

*GATE: evaluated before Phase 0, re-evaluated after Phase 1 design.*

| Article | Gate | Status | Notes |
|---|---|---|---|
| I — Test-First | Every test cites its scenario id; no implementation before a failing test | **PASS** | Every FR here is behavioural and testable at the view-model level with fakes. The QA catalog rows are written before code, per Article II. |
| II — Scenarios Before Code | QA catalog rows exist and are reviewed before any task starts | **PASS (action required)** | `MNY-*` catalog rows for the new requirements are authored as the first task of the feature, not retrofitted. |
| III — Module Boundaries | No `feature → feature`; `feature → data` via repository; cross-feature nav by `NavTarget` id | **PASS** | Confirmed by R4: "management in Settings" is a contributed `SettingsRow.Navigate` pointing at a `NavTarget`; **no screen code moves into `:libs:settings`**. This was the single most likely way to violate Article III and it is designed out. |
| IV — Fault Isolation | Every route wrapped in `FeatureHost`, flag entry present | **PASS** | All Money routes are already wrapped. New routes reuse the same `money` key. One live defect is *fixed* here: `QuickAddViewModel`'s `featureError` is currently collected by nothing, so its failures reach no surface (FR-202). |
| V — No Hardcoding | Tokens only; screen data in `<Name>Config.kt` | **PASS (action required)** | The seeded category list is screen-level product data and belongs in `MoneyConfig`, not inline — and its labels are strings resources, since seeded content is user-visible text (spec Assumptions). |
| VI — Component Reuse | Extend `:libs:core`, never a parallel component; nothing listed as built until it exists | **PASS** | Date and time pickers are added to `:libs:core` and consumed (R2). `NxButton`/`NxTextField` gain no sibling. They enter DESIGN-SYSTEM §5.1 only after the code exists. |
| VII — Money Is Exact | `Long` paise end to end on tracker paths | **PASS** | Split parts are paise; FR-225's "parts must add up" is exact integer arithmetic with no rounding step, which is the whole reason paise was chosen. |
| VIII — Consent Before Network | No tracker call before consent, enforced by interceptor | **PASS** | Seeding writes through the same consent-gated client and therefore cannot run before consent (R5). Receipts add **no** network flow at all (R3). |
| IX — Append-Only History | Shipped TEXT enum constants are never renamed; ADRs are append-only | **PASS** | The trigger migration (R9) **adds** tracked fields to an existing event kind and introduces no rename. No ADR is superseded by this feature. |

**No violations. Complexity Tracking is therefore empty and omitted.**

Two gates carry an *action*, recorded here so they are not discovered late: the QA catalog rows
(Article II) and the seeded-content placement (Article V).

### Post-Phase-1 re-evaluation

Re-checked after the design artifacts below were written. **Still no violations.** The design made
two constitution-relevant choices worth naming:

- **R6's batch split write** keeps Article VII exact and avoids a half-written split, without adding a
  security-definer function that would have needed its own RLS reasoning.
- **R5's deterministic request id** avoids a device-local "already seeded" flag. That flag would not
  have violated an article, but it would have been silently wrong on a second device — the class of
  defect Article Xa's documentation rules exist to surface.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/008-money-tab-ux-remediation/
├── spec.md              # Feature specification (10 stories, 45 FRs, 17 SCs)
├── plan.md              # This file
├── research.md          # Phase 0 — R1..R10, all verified against code
├── data-model.md        # Phase 1 — entities touched, no new entity
├── quickstart.md        # Phase 1 — device validation scenarios
├── contracts/           # Phase 1 — back-navigation, seeding, split-write contracts
├── checklists/
│   └── requirements.md  # Spec quality checklist (24/24)
└── tasks.md             # Phase 2 — NOT created by /speckit-plan
```

### Source Code (repository root)

```text
apps/finance/feature/money/money/src/main/java/com/dhruv/finance/money/
├── LedgerScreen.kt                 # top bar + overflow; content row reduced to search + filter
├── LedgerViewModel.kt              # saved-view state
├── LedgerFilterSheet.kt            # save / apply / manage named views
├── QuickAddSheet.kt                # validation surface, transfer shown unavailable, IME fix
├── QuickAddViewModel.kt            # validation state, failure surfacing, 409-as-saved
├── TransactionFormScreen.kt        # date/time fields, receipt, split editor, back guard
├── TransactionFormViewModel.kt     # occurredAt, receiptPath, split parts, type-change field swap
├── TransactionDetailScreen.kt      # edit affordance, sibling-split display
├── AccountsScreen.kt               # top bar + title + back
├── AccountDetailScreen.kt          # top bar + title + back; edit; destructive action separated
├── AccountFormScreen.kt            # discard guard (currently has none by any route)
├── CategoriesScreen.kt             # top bar + title + back
├── RecurringScreen.kt              # top bar + title + back; edit affordance
├── RecurringReviewScreen.kt        # top bar + title + back; offline + signed-out states; date format
├── MoneyConfig.kt                  # seeded starter categories (Article V)
└── settings/MoneySettingsContribution.kt   # Info -> Navigate rows for accounts and categories

apps/finance/data/src/main/java/com/dhruv/finance/data/tracker/
├── repo/TransactionRepository.kt   # batch split create; 409-as-saved
├── repo/RecurringRepository.kt     # requestId parameter
├── repo/SuggestionRepository.kt    # requestId parameter
├── repo/CategoryRepository.kt      # seeding alongside ensureReservedCategories
└── net/MoneyApi.kt                 # array-body create

libs/core/src/main/kotlin/com/dhruv/core/
├── ui/components/NxDatePickerSheet.kt      # new (R2)
├── ui/components/NxTimePickerSheet.kt      # new (R2)
└── navigation/NavTarget.kt                  # + OpenAccounts, OpenCategories (R4)

apps/finance/app/src/main/java/com/dhruv/finance/app/
└── MainActivity.kt                          # back wiring, NavTarget resolution, onSignIn callbacks

supabase/
├── schemas/finance/30_functions/fn_transaction_audit.sql   # + type, to_account_id, receipt
└── migrations/<generated>.sql                              # per ADR-0032: edit schema, db diff
```

**Structure Decision.** No new Gradle module. The feature edits one existing feature module, the
shared data module, two files in `:libs:core`, the app shell's wiring, and one database function. That
matches the spec's own framing — the capabilities exist and the affordances do not.

The one thing that could have gone wrong structurally, and does not: moving "management" into Settings
does **not** move screen code into `:libs:settings`. `SettingsRow.Navigate` + `NavTarget` is the
sanctioned path (R4), and the screens stay where they are.

## Implementation slices

Ordered by user impact, matching the spec's priorities. Each slice is independently shippable and
leaves `regressionCheck` green.

| Slice | Delivers | Why here |
|---|---|---|
| **0 — QA catalog** | `MNY-*` rows for every new requirement | Article II: rows exist before any code |
| **1 — Never fail silently** | US1: FR-201..FR-205 | The primary flow. Includes 409-as-saved (R8) and surfacing `QuickAddViewModel`'s currently-orphaned errors |
| **2 — Back and titles** | US3: FR-208..FR-208c | P1 and device-confirmed. Guard and control ship together (R1) — the arrow alone makes the data loss easier to hit |
| **3 — Sign-in works** | US2: FR-206, FR-207 | Three dead buttons; smallest P1 in the set |
| **4 — First run** | US10: FR-238..FR-241 | P1 cold-start block. Small, once R5's deterministic id is in place |
| **5 — Setup in Settings** | US4: FR-209..FR-211 | Nav restructure; supersedes the Money overflow for two of three destinations |
| **6 — Correction** | US5: FR-212..FR-216, FR-228 | Needs the trigger migration (R9) landed first |
| **7 — Honest states** | US7: FR-219..FR-223 | Offline state, date format, destructive placement, accessibility |
| **8 — When it happened** | US9: FR-229..FR-234 | Needs the two new `:libs:core` pickers (R2) |
| **9 — Saved views** | US6: FR-217, FR-218 | Pure surface work; repository already complete (R10) |
| **10 — Split** | US8: FR-224..FR-227 | Largest new capability; needs the batch write (R6) |
| **11 — Closure** | FR-235 + tracking rule | `002`'s Implementation record, this spec's record, FEATURES.md, README, CHANGELOG |

Slices 6 and 8 have hard prerequisites (the migration; the picker components). Slice 2's two halves
must not be split. Everything else is independent.

## Risks

| Risk | Mitigation |
|---|---|
| Slice 2 ships the visible back arrow without the guard | They are one slice, stated here and in R1. A visible control on a screen people currently avoid makes the silent discard *more* reachable, not less |
| The trigger migration is written but never executed | ADR-0032's flow: edit the declarative object, `db diff`, review, commit both; the equivalence guard runs in CI. It is first executed by the `develop` push, which is where its correctness is actually confirmed |
| The seeded category list grows into a taxonomy | Spec Assumptions caps it: a small everyday starting point people edit, translated like any other string, and adding to it later does not reach already-seeded people |
| Split's batch write regresses the single-transaction path | The array body is an additional call, not a replacement; the existing single create and its retry tests are untouched |
| Scope is large enough to stall | Eleven slices, each independently shippable and green. Slices 1–4 alone close all four P1s |
