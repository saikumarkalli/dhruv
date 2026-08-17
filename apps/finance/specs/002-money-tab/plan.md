# Implementation Plan: Money Tab (Phase 3)

**Branch**: `002-money-tab` | **Date**: 2026-08-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `apps/finance/specs/002-money-tab/spec.md`

**Note**: This plan is the spec-kit-shaped restatement of
`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §4/§5/§6/§7
(Phase 3) — that document remains the source of truth for anything not repeated here. Two places
where this plan **adds** to it (rather than diverging) are called out explicitly in `research.md`
R1 and R7 and were written back into that plan's Phase 3 section as a dated note.

## Summary

Give the Money tab real content: a day-grouped monthly ledger with search/filter/saved views, a
three-tap quick-add sheet plus a full transaction form, a read-first transaction detail carrying an
append-only audit trail, an accounts screen whose "spendable now" excludes credit, an account detail
with reconciliation, category management with rename/merge rules, and recurring definitions that
produce reviewable pending entries instead of silently posting.

Technical approach: six new tables and three new views in the `finance` Postgres schema (ADR-0033),
with the two rules that must not be bypassable — audit-on-every-mutation and transfer-exclusion —
enforced **in the database** (a trigger and the view definitions) rather than by client discipline;
one new Gradle module `:apps:finance:feature:money` (D1–D9) following the existing feature template;
the nested-`NavHost` generalisation that Phase 0 explicitly descoped until a second tab needed
sub-routes (Money is that tab); component batches B4/B7 plus the remaining B6 items in `:libs:core`;
and the credit-card-bill row on Home that Phase 2 deferred until `accounts` existed.

## Technical Context

**Language/Version**: Kotlin 2.2, Jetpack Compose (Material 3), minSdk 26.

**Primary Dependencies**: Koin (DI), Retrofit + Moshi + OkHttp against Supabase PostgREST (Phase 1's
`tracker/net` stack, extended not replaced), kotlinx.coroutines/Flow. **No new dependency** — see
`research.md` R2.

**Storage**: Supabase (PostgreSQL + RLS), `finance` schema. New: `accounts`, `categories`,
`transactions`, `transaction_events`, `recurring_templates`, `suggestions`, and views
`v_account_balances`, `v_month_summary`, `v_category_spend`. Receipt **images** are device-local this
phase (R6). No local Room storage for tracker data (ADR-0014, unchanged).

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine + in-memory fakes / MockWebServer at the
HTTP boundary (Robolectric-SQLite is unreliable on this Windows dev machine — standing project
constraint). SQL-layer rules (trigger, RLS, view exclusion) are verified against the dev Supabase
project at the Sec step, not in JVM unit tests. Every test cites its `MNY-*` scenario ID
(constitution Article I).

**Coverage**: JaCoCo merged report + `jacocoCoverageVerification` floor via `./gradlew
regressionCheck` (ADR-0013, Article X). A new Gradle module contributes nothing until it is added to
**both** `coveredModules` (root `build.gradle.kts` — report and gate) and `_FEATURES`
(`scripts/ci/regression_summary.py` — per-module reporting); this phase does both in Setup (T005),
not at the end, so every story's tests count from the first commit. The floor is ratcheted once, at
the Phase 9 checkpoint (T078), to just under the newly measured merged coverage — never above it,
never ahead of landed tests. Expect the module's own percentage to sit well below its
repository/ViewModel logic coverage: Compose screen files are not exercised by the JVM gate, so both
numbers get recorded rather than one implying the other (T077).

**Target Platform**: Android (existing `:apps:finance:app` shell), API 26+.

**Project Type**: Mobile app feature module inside the existing Gradle monorepo.

**Performance Goals**: SC-009 — a 5,000-transaction month opens and scrolls without stutter. Month
summary, category spend and account balances are read from server-side views; the client never sums
a ledger to draw a screen (NFR-8). Ledger list is virtualised and paged by month.

**Constraints**: Money is `Long` paise on every new path (Article VII, `checkTrackerMoneyPrecision`).
`:feature:money` reaches data only through `:apps:finance:data` repositories, no `feature → feature`
edge (Article III). New visuals extend existing `:libs:core` components before a parallel one is
proposed (Article VI). No raw dp/sp/hex literal in a screen file (Article V). No PostgREST call
before the "Sync my financial records" consent switch is on — inherited from Phase 1's
`ConsentInterceptor`, no new client is constructed (Article VIII).

**Scale/Scope**: 9 screens (D1–D9) + 1 Home addition, 1 new Gradle module, 6 new tables + 3 views +
1 trigger + 1 SQL function, 20 `MNY-*` QA rows to satisfy.

**Dependency on Phase 2**: this phase assumes `001-net-worth-tracker` has landed —
specifically `NxTextField`'s error/helper state, `NxButton` sizes/loading, `SelectionSheet` (B9) and
`NxSelect` (B6), which D3/D5 consume, and the PostgREST `finance`-schema profile-header handling
(ADR-0033). If Phase 3 starts first, those items move into this phase rather than being duplicated.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Article | Check | Status |
|---|---|---|
| I. Test-First | Every Backend/Android task in `tasks.md` cites an `MNY-*` scenario ID; RED before GREEN | PASS (enforced at task-authoring time) |
| II. Scenarios Before Code | `MNY-*` rows (20) already written and reviewed — catalog §4, marked done | PASS — pre-satisfied |
| III. Module Boundaries | `:feature:money` depends only on `:libs:core` + `:apps:finance:data` + `:libs:settings`; Home's credit-card-bill row is shell-owned and reads the same repository, not `:feature:money` | PASS by construction — see Project Structure |
| IV. Fault Isolation | D1–D9 wrapped in `FeatureHost`; **new** `money` flag added to `dhruv-finance.json` (`enabled: true, requiresConsent: true`) | PASS — flag added by this phase (§5.5 of the impl plan) |
| V. No Hardcoding | All new screens read `DhruvNextType`/`Spacing`/`Radii`/`LocalDhruvNextColors`; screen data in `MoneyConfig.kt` | PASS by construction, verified at review |
| VI. Component Reuse | B4/B7 + B6 remainder are genuinely absent from `:libs:core` (verified by symbol search, design-system §13 method); `AmountKeypadSheet` composes the existing `NumericKeypad` + `DhruvModalSheet` rather than adding a second keypad | PASS — see research.md R5 |
| VII. Money Is Exact | Every amount column is `bigint` paise; `rate`/share/utilisation percentages are integers or basis points, never `Double` on a write path | PASS — `checkTrackerMoneyPrecision` covers `tracker/**` |
| VIII. Consent Before Network | `money` flag ships `requiresConsent: true`; already mapped to the "Sync my financial records" A3 switch in the impl plan's consent table; no new HTTP client is built | PASS — inherited gate |
| IX. Append-Only History | `transaction_events` gets SELECT+INSERT policies only (no UPDATE/DELETE), same shape as `valuations`; transaction/account/category `type`/`kind`/`source` enums persist as TEXT and are append-only constants | PASS by construction |
| X. Coverage Ratchets | Module registered in `coveredModules` + `_FEATURES` **in Setup** (T005) so its coverage is actually measured; merged floor ratcheted once at the Phase 9 checkpoint (T078), only to the measured value | PASS — with the wiring made an explicit task rather than assumed; see the Coverage note below |
| XI. Stack Is Fixed | No new dependency; Retrofit/Moshi/OkHttp + Koin only | PASS |

No violations. **Complexity Tracking is empty.**

**Coverage note (why Article X needed a task, not a promise).** `coveredModules` and `_FEATURES` are
hand-maintained lists, so "the gate covers everything" is only true of modules someone remembered to
add. Verified while writing this plan: `:apps:finance:feature:onboarding` is in `coveredModules`
but **missing from `_FEATURES`**, so its coverage reports as `(other)` in the Job Summary, PR
comment and release notes today — the exact failure mode T005 exists to prevent for `money`. Same
gap applies to Phase 2's `:feature:networth`, whose task list does not add it to either list; noted
here rather than silently patched into another feature's tasks.

**Post-Phase-1 re-check.** Three design decisions were re-tested against the gates after
`data-model.md` was written:

1. **A database trigger writes audit rows** (`data-model.md` §Trigger). Checked against Article I
   (test-first): the trigger is still covered by tests — a repository-level test asserts an event
   exists after each mutation (`MNY-BR-006`), and the SQL-layer assertion runs at the Sec step
   against the dev project. Not a violation; it is the same "make the rule structural, not
   disciplinary" reasoning ADR-0029 used for `ConsentInterceptor`.
2. **`accounts`, `categories`, `transactions` are mutable** (UPDATE policies exist). Article IX
   scopes append-only to persisted enum constants and history tables specifically — a transaction's
   category legitimately changes, and that change *is* the thing the audit trail records. Correct,
   not a violation. `transaction_events` itself remains strictly append-only.
3. **Deletes are soft** (`deleted_at`), so an audit trail is never orphaned by a hard delete; hard
   erasure stays exclusively in `delete_my_data()`/`delete_my_account()` (ADR-0029 §5), which this
   phase extends to cover the new tables. Consistent with Article IX and DPDP (NFR-1).

Gate remains PASS.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/002-money-tab/
├── plan.md               # This file
├── research.md           # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/routes.md   # Phase 1 output — route registry rows
├── checklists/requirements.md
└── tasks.md              # Phase 2 output (/speckit-tasks — not created by this command)
```

### Source Code (repository root)

**Structure Decision**: Mobile app inside the existing Gradle monorepo — one new feature module,
two extended modules, one new migration. `feature/loans` remains the reference implementation for
module shape (impl plan §6); `feature/money/money/` is the physical location under the tab-owner
bucket scheme (`apps/finance/feature/README.md`), with the Gradle coordinate remapped via
`projectDir` exactly like every other module.

```text
apps/finance/
├── data/src/main/java/com/dhruv/finance/data/tracker/
│   ├── dto/                        # + AccountDto, CategoryDto, TransactionDto,
│   │                                #   TransactionEventDto, RecurringTemplateDto, SuggestionDto
│   ├── model/                      # + Account, Category, Transaction, TransactionEvent,
│   │                                #   RecurringTemplate, PendingEntry (paise Longs, enums)
│   ├── mapper/                     # + one mapper per model
│   └── repo/                       # + AccountRepository, CategoryRepository,
│                                    #   TransactionRepository, RecurringRepository,
│                                    #   SuggestionRepository (pending entries)
├── feature/money/money/            # NEW MODULE — :apps:finance:feature:money
│   ├── build.gradle.kts            # dhruv.android.library + dhruv.android.compose; deps on
│   │                                # :apps:finance:data, :libs:core, :libs:settings
│   ├── LedgerScreen.kt             # D1 (Money tab root)
│   ├── QuickAddSheet.kt            # D2
│   ├── TransactionFormScreen.kt    # D3 (full-screen modal, close ✕)
│   ├── TransactionDetailScreen.kt  # D4
│   ├── LedgerFilterSheet.kt        # D5
│   ├── AccountsScreen.kt           # D6
│   ├── AccountDetailScreen.kt      # D7
│   ├── CategoriesScreen.kt         # D8
│   ├── RecurringScreen.kt          # D9 (+ recurring review list)
│   ├── <Screen>ViewModel.kt        # one per screen, existing convention
│   ├── MoneyConfig.kt              # screen-level data/config (no-hardcoding rule)
│   └── di/MoneyModule.kt           # Koin module, aggregated in CalculatorApplication
├── app/src/main/java/com/dhruv/finance/app/
│   ├── ui/home/HomeScreen.kt       # + credit-card-bill row in UPCOMING (Phase 2 deferral)
│   └── navigation/                 # Money tab nested NavHost; nested-host pattern generalised
│                                    # from Plan to "current tab's controller" (Phase 0 descope)
libs/core/src/main/kotlin/com/dhruv/core/
├── ui/components/lists/            # + B4: DayGroupHeader, LedgerRow, SuggestedRow, ReconcileBanner
├── ui/components/inputs/           # + B6 remainder: NxTextArea, InputChip
├── ui/components/states/           # + B7: StatusBadge, InfoBanner
├── ui/components/overlays/         # + DateRangeSheet, AmountKeypadSheet (composes NumericKeypad)
└── navigation/NavTarget.kt         # + OpenAccount(accountId) — Home's card-bill row hand-off
platform/feature-flags/dhruv-finance.json   # + "money" flag
build.gradle.kts                    # + ":apps:finance:feature:money" in `coveredModules`;
                                    #   `globalLineFloor` ratcheted at the Phase 9 checkpoint
scripts/ci/regression_summary.py    # + "money" in `_FEATURES` (per-module coverage reporting)
supabase/
├── schemas/finance/10_tables/      # + accounts, categories, transactions, transaction_events,
│                                    #   recurring_templates, suggestions (declarative, ADR-0032)
├── schemas/finance/20_views/       # + v_account_balances, v_month_summary, v_category_spend
├── schemas/finance/30_functions/   # + fn_transaction_audit (trigger fn), merge_categories
├── schemas/public/30_functions/    # ~ delete_my_data() extended to the new tables
└── migrations/<timestamp>_money_phase3.sql   # generated via `supabase db diff`, grants hand-added
web/src/shared/types/database.ts    # regenerated: supabase gen types --schema public,finance
```

Tests mirror this tree under each module's `src/test/` — existing project convention, no separate
`tests/` root.

## Complexity Tracking

*(empty — no Constitution Check violations)*
