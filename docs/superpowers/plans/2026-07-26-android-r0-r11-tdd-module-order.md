# Android Master Roadmap — TDD Module Implementation Order (R0–R11)

> Source of the phase sequence: `docs/superpowers/plans/2026-07-12-master-roadmap-personal-app.md`
> §3/§5. R0 detail: `platform/PRODUCTION_READINESS.md`. R2 detail + the TDD pattern this doc
> replicates: `docs/superpowers/plans/2026-07-04-p1-networth-execution-checklist.md`.
> This doc adds one layer those don't have: **per-phase module build order with a TDD entry point
> for each module.** Full task/test enumeration for R3–R11 still lives in each phase's own spec —
> this file only orders them and states what to test first.

## The TDD pattern (repeat for every module, every phase)

Bottom-up, same order every time — matches the test pyramid (mostly small/unit tests, few large):

1. **Pure/domain logic** — no Android imports, plain JUnit. Write the test file first, watch it
   fail, then implement. Highest-value, cheapest to test — do this before anything else in a phase.
2. **Repository/data layer** — fake the remote/local boundary (`Fake*Repository`), medium test.
3. **ViewModel** — Turbine + `kotlinx-coroutines-test` + the fakes from step 2.
4. **Compose UI** — compose only what step 3 already exposes as state. Instrumented/screenshot
   tests stay developer-local (`regressionCheck` is JVM + Robolectric only, per ADR-0013).
5. **Navigation/DI wiring** — `DependencyRulesTest` (ArchUnit) must stay green.
6. **Docs sync + gates** — `/dhruv-pre-merge` (audit + boundaries + security + ui-review +
   coverage) then `./gradlew regressionCheck`.

Verify per-module: `./gradlew :apps:finance:<module-path>:testDebugUnitTest`.
Verify per-phase: `./gradlew regressionCheck`.

## Phase order (unchanged from the master roadmap's dependency graph)

```
R0 hardening ──┬──────────────────────────────► everything
R1 CI costs ───┘ (parallel, different files)
R2 P1 networth ──► R3 security ──► R4 rates+notif+update
R2+R4 ──► R5 P2 expenses (+2 ADRs) ──► R5b recurring/quick-add
R5 ──► R6a P3 goals ─┐
R2+R4 ─► R6b P4 ins. ┼──► R7 reports/export ──► R9 P5 retirement (+XIRR)
R5 ─────────────────┘         │
R7 ──► R8 polish
R9 ──► R10 P6 automation ──► R11 platform expansion
```

---

## R0 — Production hardening (blocks everything)

Source: `platform/PRODUCTION_READINESS.md` Phase 0–6 (tasks T1–T18). Phase 0 (D1–D3) is a
maintainer decision, no code. TDD order within the remaining phases:

**Phase 1 — stop data loss (C3, H2, H5, H6, M6, M9)**
1. Migration tests FIRST — `MigrationTestHelper` chain 2→3→4→5 (they fail today because of
   `fallbackToDestructiveMigration`) → then remove the fallback, `exportSchema = true`, add real
   `Migration` objects (+ `MIGRATION_1_2` if D3 says v1 ever shipped). (T2/H2/H5/M9)
2. `ConsentRepositoryTest` (grant/revoke/timestamp/policy version) → encrypted-DataStore
   `ConsentRepository` → wire into both `AssistantViewModel` and `CalculatorViewModel`. (T1/C3/M6)
3. Backup/data-extraction rules (T3) — not unit-testable (XML config); verify with a manual
   restore test, document the DB/settings inclusion decision.
   **Checkpoint:** `regressionCheck` green, migration tests pass, consent flow manually verified
   on both AI entry points.

**Phase 2 — AI wired end-to-end (C1, C2, C4, M7, M8, L1)**
4. `GeminiKeyProviderTest` (BYO key takes priority over default) → `GeminiKeyProvider` →
   `GeminiRepository` stops constructor-capturing the key. (T4/C2)
5. `mapError` test (preserves cause/type, no more collapsed `Exception(message)`) → fix +
   localized "no key configured" copy. (T5/C1/L1)
6. Assistant prompt test (asserts a dedicated `ask()` prompt, not reused `explainCalculation`) +
   input-length-cap / rate-limit test → implement. (T6/M7/M8)
   **Checkpoint:** BYO key works on-device on both entry points; `strings`/apktool scan of the
   release APK shows no embedded key.

**Phase 3 — observability (H1, L4)** — mostly infra, thin on unit tests.
7. T7 plugin wiring (`google-services`, Crashlytics) — verify manually: a test crash and a real
   trace land in console.
8. Latency-trace test (fake clock asserts non-zero duration) → replace the `Unit` sentinel. (T8/L4)
9. `FirebaseFeatureFlagResolverTest` (priority: remote → cached → hardcoded) → implement.

**Phase 4 — hardening (H4, H7, M1, L5, L6)**
10. `verifyPin()` test FIRST (salted-hash compare, no `"1234"` default, PIN never reaches the UI
    layer) — fully unit-testable, do this before anything else in the phase. (T10/H7)
11. Rate-sanity-check test (rejects negative/non-finite/wrong base currency) → validator wired
    into `CurrencyApiClient`. (T12/L6)
12. `CertificatePinner` (T9) — integration/manual only (pin mismatch = connection refused); smoke
    against the real host, can't meaningfully unit test.
13. OWASP gate (T11) — CI config change, no code test.

**Phase 5 — data model & quality (H3, M2–M5, L3, L7)**
14. Migration test FIRST (old schema → `DhruvEntity` shape: UUID id, indexed `userId="local"`,
    sync columns) → the migration + entity change. (T13/H3)
15. ViewModel/repository test wave to close coverage gaps (`/dhruv-coverage`), ratchet
    `globalLineFloor`. (T14/M2)
16. String externalization + `CalculatorScreen`/`CalculatorViewModel` decomposition — refactor
    under the existing test net; this is verification, not new TDD. (T15/M3/M4/L3/L7)

**Phase 6 — AI platform build-out (T16–T18)** — Cloudflare Worker proxy is a separate deploy with
its own test suite, not app-repo TDD. LLM security baseline (schema-validate model output, allow-
list actions) becomes unit-testable once tool-use actually lands — no code yet, gate is future.

**Exit:** signed APK, no data-loss paths, no unconsented egress, crash reporting live.

---

## R1 — CI cost optimization + commit-type versioning (parallel with R0)

Source: `2026-07-04-ci-cost-optimization-commit-type-versioning-design.md` (ADR-0025/0026 to be
written when built). Not app-code — workflow YAML + `scripts/ci/*.py`. Same RED→GREEN discipline
applies to the Python: (1) commit-type → bump-level classifier, pytest-first, (2) any
`regression_summary.py` extensions, tested before wiring into the workflow, (3) YAML changes
verified via a draft-PR dry run (no unit test target), (4) branch-protection "require branches up
to date" toggle [maintainer, manual] — must land before this phase merges for real.

---

## R2 — P1 Net Worth Tracker (in flight — spec + checklist already exist)

Branch `feat/networth-tracker` exists but is currently at the `develop` tip — zero code written
yet. The full TDD module order is already written, step by step:
`docs/superpowers/plans/2026-07-04-p1-networth-execution-checklist.md` (steps A→J). **Follow it
verbatim — do not re-plan.** Bottom-up summary:

1. **B** — manual Supabase project + Google OAuth setup [maintainer], blocking.
2. **C** — `Paise` util pure test first → then `SupabaseAuthApi`/`SupabaseRestApi` +
   `AuthRepository` (fake-boundary tests) → `NetWorthRepository` (Moshi DTOs, fake remote source).
3. **D** — device smoke gate (consent → sign-in → one authenticated GET returns 200). Blocks all
   UI work below it.
4. **E** — `NetWorthCalculator` pure test (10 cases: empty, latest-per-parent, assets−liabilities,
   soft-delete exclusion, month delta, trend, negative net worth) → then ViewModels (Turbine +
   fakes).
5. **F** — `:libs:core` design-system components → feature screens composing them (zero
   feature-local styling).
6. **G** — navigation (`ToolsHub`, `MainActivity` tabs, ArchUnit green).
7. **H/I/J** — docs sync → quality gates (`/code-review`, `/dhruv-security`, `/dhruv-boundaries`,
   `/dhruv-ui-review`, `/dhruv-coverage`, `regressionCheck`, full device smoke script) → ship.

---

## R3 — Personal-data security layer

Source: `2026-07-12-r3-app-security-layer-design.md`.
1. Lock-state machine — pure test (locked/unlocked/timeout transitions, no Android imports)
   FIRST → `LockStateManager`.
2. `AppLockViewModel` — Turbine test with a fake `BiometricRepository` (success/fail/fallback-to-
   device-credential).
3. Privacy-mode formatter — pure test (masks money text through the single `formatPaise*` choke
   point) → `PrivacyModeFormatter`.
4. `FLAG_SECURE` route wiring — not unit-testable (window flags); verify with a manual
   screenshot-blocked check.
5. Session re-auth policy — pure test (elapsed time > N days → re-auth required).
**Checkpoint:** cold start → biometric → Home; app-switcher shows no balances; `regressionCheck`
green.

## R4 — Currency accuracy + metals + daily notification + update check

Source: `2026-07-03-currency-realtime-rates-daily-notification-design.md` +
`2026-07-12-r4-inapp-update-check-design.md`.
1. Rate/gold/silver response → domain mapping — pure test, reusing R0's sanity-bound validator.
2. WorkManager scheduling — `TestListenableWorkerBuilder` unit test per worker (not the real
   scheduler).
3. Notification content builder — pure test (rate delta → notification text).
4. Update-check semver comparator — pure test FIRST (ignore pre-release/drafts, per the risk note
   in the master roadmap) → `UpdateChecker`, then a fake-HTTP GitHub Releases client.
**Checkpoint:** daily notification fires (device-verified); update notification fires on a stale
install (comparator unit-proven + manual device check).

## R5 — P2 Expenses, Income & Budgets (+ 2 ADRs first)

Source: `2026-07-03-p2-expenses-budgets-design.md`,
`2026-07-12-r5-accounts-multicurrency-decisions.md`.
0. Write ADR-0017 (accounts are assets) + ADR-0018 (INR-only) before schema — decisions, no code.
1. Supabase migration: `transactions`/`budgets` tables + RLS (mirrors R2's pattern) — no unit
   test target; verify via `supabase db push` + a manual RLS probe.
2. Validation rules (amount > 0, category enum, date not future) — pure test, same shape as
   SDD-05 §3 already used on web.
3. `TransactionRepository` (fake remote) → `BudgetCalculator` pure test FIRST (savings rate,
   category totals) — same "pure logic before repository" order as R2's `NetWorthCalculator`.
4. ViewModels (Turbine + fakes) → quick-add form UI → Home bento savings-rate card.
**Checkpoint:** quick-add under 5s, budgets computed correctly (property-tested against
`BudgetCalculator`), `regressionCheck` green.

## R5b — Recurring + quick-add surfaces

Source: `2026-07-12-r5b-recurring-quickadd-design.md`.
1. RRULE-lite schedule calculator — pure test (next-due-date given rule + last-run) FIRST.
2. WorkManager materializer — fake-clock test (due rows land in the confirm inbox).
3. App-shortcut → `QuickAddSheet` deep link — instrumented/manual, not unit-testable.
**Checkpoint:** rent/salary auto-appear pending one-tap confirm; `regressionCheck` green.

## R6 — P3 Goals & Debt Payoff ∥ P4 Insurance (parallel tracks)

Source: `2026-07-03-p3-goals-debt-payoff-design.md`, `2026-07-03-p4-insurance-registry-design.md`,
N16 budget-alert.
1. EMI-math extraction — reuse the existing loans-calculator engine (already tested); regression-
   test against its existing cases, don't duplicate → `DebtPayoffCalculator` pure test (snowball
   vs avalanche ordering).
2. `GoalProgressCalculator` — pure test (target vs current vs date → on-track/behind).
3. Insurance renewal-reminder logic — pure test (days-until-renewal → trigger), reuses R4's
   notification plumbing.
4. Budget-overrun threshold check (N16) — pure test (80%/100% crossing), reuses R4/R5b's
   WorkManager pattern.
5. Repos/ViewModels/UI in the established bottom-up order.
**Checkpoint:** each spec's own DoD; budget alert fires on threshold cross (unit-proven).

## R7 — Reports, export & import

Source: `2026-07-12-r7-reports-export-import-design.md`.
1. Report aggregation — prefer server-side (PostgREST) per the roadmap's pagination risk note;
   pure test on the shaping function given raw rows.
2. CSV export formatter — pure round-trip test FIRST (known rows → CSV → re-parsed = same rows).
3. CSV import validator/parser — pure test (malformed row → specific error) → preview → bulk
   insert (integration test against a fake repository).
4. PDF net-worth statement — test the data-shaping function, not the PDF bytes.
**Checkpoint:** export → delete-my-data → import round-trips losslessly — write this as an actual
integration test, not a manual step.

## R8 — Daily-driver polish

Source: `2026-07-12-r8-daily-driver-polish-design.md`.
1. Onboarding step-state machine — pure test (3-screen progression, skip/resume).
2. Global search — pure test on the query-shaping/ranking function; the PostgREST `ilike` call
   itself is fake-tested.
3. Trash (soft-delete list + restore) — repository test FIRST (30-day window filter via fake
   clock, restore flips `isDeleted`).
4. Widget (Glance) — pure test on the data-provider function (respects privacy mode); Glance
   rendering itself is instrumented/manual.
**Checkpoint:** fresh install guided to first asset; any deleted row restorable within the window
(unit-proven via fake clock).

## R9 — P5 Retirement Projection (+ returns math)

Source: `2026-07-03-p5-retirement-projection-design.md`,
`2026-07-12-r9-investment-returns-design.md`.
1. XIRR solver — pure test FIRST (known cash-flow series → known XIRR; edge cases: single flow,
   all-negative, no-solution) → `ReturnsCalculator`. Highest-risk math in this phase, most
   test-critical module — do not touch UI until this is solid.
2. `RetirementProjector` — pure test (contributions + return rate + years → corpus, inflation
   toggle).
3. Wire into `HoldingDetailScreen`/investments hub ViewModels + UI.
**Checkpoint:** per-asset annualized return visible; spec DoD.

## R10 — P6 Automation groundwork

Source: `2026-07-03-p6-automation-groundwork-design.md`. Each item needs its own ADR + consent
screen first. SMS/notification-ingestion spike and the AA decision doc are research spikes — not
TDD-able until an approach is chosen; once chosen, the parser/classifier logic is pure-test-first
like every prior phase. Localization ADR (N18) — no code until a go decision; if go, the
string-resource swap is verified by the existing string-externalization lint debt from R0.

## R11 — Platform expansion (optional)

Tools app / Vault app / Telegram bot / AI proxy Worker / Dhruv ID. Vault already has a full
security spec (pull forward if desired — flagged as most personally valuable). Each is effectively
its own R0-style bootstrap. For Vault specifically: Argon2id key derivation and vault crypto are
tested against known test vectors FIRST, before any repository/UI wiring — crypto correctness is
non-negotiable and must be proven before it ever touches Room/SQLCipher.

---

## Standing rule across every phase

Per ADR-0013 and the master roadmap §4: every phase ends at the same gate —
`./gradlew regressionCheck` green + `/dhruv-pre-merge` PASS + a release-build smoke test. Do not
open the next phase's module list until the current one clears it.

---

# Part 2 — Web, developed in parallel (compact scope)

User decision (2026-07-26): build web alongside Android, same features, **compact** — web ships a
leaner slice per phase (core CRUD + read screens), not every Android module 1:1. This is not a new
direction — `2026-07-12-master-roadmap-personal-app.md` §3/§5 already specced a web track (W0–W4+)
that "tracks Android phases," one phase behind. This section fleshes that out with a TDD module
order and states the real risks of running both at once.

## Current state

`arch/web-app-setup` = **W0 done**: Vite+React+TS scaffold, router with all 5 app routes,
`FeatureHost`/`useFeatureFlag` (reads the *same* `platform/feature-flags/dhruv-finance.json` file
Android bundles — verified in `useFeatureFlag.ts`), `supabaseClient.ts` stub, vitest + Testing
Library wired, one passing test (`App.test.tsx`). Zero Supabase migrations applied yet
(`supabase/migrations/` is intentionally empty — README says schema authorship starts at W1).

## W-phase ↔ R-phase pairing (compact scope per phase)

| Web phase | Pairs with | Compact scope (what ships on web) | Skipped on web (Android-only, no web equivalent planned) |
|---|---|---|---|
| **W1** Auth+Consent | R2 (P1, shared schema) | Google OAuth PKCE, consent gate, same 3 tables (`assets`/`liabilities`/`valuation_entries`) | Credential Manager (Android-only API) |
| **W2** Dashboard+Calculators | R2 tail + R0's consent/validation concerns | Net worth dashboard (CRUD), a handful of calculators with session-only history | Room-backed calculator history (web has no local DB story until SDD-05 §5 V3) |
| **W3** Deploy | — (infra, no Android pair) | `vite-plugin-pwa`, Vercel, `web-ci.yml`, security headers (SDD-06 §3) | — |
| **W4** | R5 (P2 expenses/budgets) | Transaction CRUD, budget totals, savings-rate card | Quick-add app shortcut (R5b, OS-level, Android-only) |
| **W5** | R6 (P3 goals ∥ P4 insurance) | Goal/policy list + progress, renewal countdown | Push notifications for renewals (SDD-04 §6: "V2, Web Push via Edge Functions" — deferred) |
| **W6** | R7 (reports/export/import) | Report screen, CSV export/import (browser file APIs, no SAF needed) | PDF generation (evaluate a JS lib vs skip for V1) |
| **W7** | R9 (retirement + XIRR) | Same `ReturnsCalculator`/`RetirementProjector` math, ported to TS, ported test vectors | — |
| — | R3 (app lock), R5b, R8 (widget/search/trash), R10, R11 | **No web equivalent planned.** Biometric app-lock, home-screen widgets, and OS quick-add are mobile-native concepts; global search/trash could get a web version later but aren't in the compact scope. | |

## TDD module order — W1 (Auth + Consent)

Bottom-up, same pure-logic-first discipline as the Android side (`docs/sdd/04-web-app-sdd.md` §3,
`06-auth-and-security-sdd.md`):

1. `shared/lib/money.ts` — `formatPaise`/`formatPaiseCompact`/`parseToPaise`, pure, vitest FIRST.
   Port the exact Kotlin `PaiseTest` cases (SDD-05 §2) so both platforms assert the same table.
2. `shared/lib/validation.ts` — the SDD-05 §3 rule table (name/value/date/amount/category), pure,
   test FIRST — same case list as Android's editor-sheet validation tests.
3. Supabase migration — apply the P1 spec §4.1 SQL (assets/liabilities/valuation_entries + RLS +
   `delete_my_account()`) via `supabase db push`. **Shared with Android R2 — see concern (1) below,
   whichever platform's PR lands first authors it, the other consumes.** No unit test target;
   verify via a manual RLS probe (sign in as user A, confirm user B's rows are invisible).
4. `npx supabase gen types typescript` → `shared/types/database.ts` (generated, not hand-tested).
5. `shared/hooks/useAuth.ts` — the 5-state machine (`not-configured`/`consent-needed`/
   `signed-out`/`loading`/`signed-in`) — test state transitions against a fake Supabase client
   (MSW intercepting GoTrue endpoints), not the real network.
6. `shared/hooks/useConsent.ts` — localStorage-backed, persisted + revocable (mirrors Android's
   R0 fix for M6 — do NOT repeat the in-memory-only mistake here either). jsdom localStorage test.
7. Auth-gate components (`SignInContent`, `ConsentGateContent`, loading/offline/error cards) —
   Testing Library component tests, one per auth state, asserting the right child renders.
**Checkpoint:** `npm test` green, manual sign-in smoke in a real browser.

## TDD module order — W2 (Dashboard + Calculators)

1. `apps/finance/api/netWorth.ts` — pure aggregation (latest-per-parent, assets−liabilities, month
   delta, trend series). Port the Android `NetWorthCalculatorTest`'s 10 cases verbatim — this is
   the single highest-value shared-behavior test to keep in lockstep between platforms.
2. `apps/finance/api/networthRepository.ts` — CRUD against `assets`/`liabilities`/
   `valuation_entries`, tested against MSW-mocked PostgREST responses (not a live Supabase call).
3. `NetWorthScreen` state-machine shell → `DashboardContent` (hero + asset/liability bento cards)
   → `HoldingListScreen` → editor/valuation forms (wired to step 2's validation.ts) →
   `HoldingDetailScreen`. Component-test each against fake repository data, same order as the
   Android checklist's F15→F16.
4. A handful of calculators (start with 1–2, not all 10 Android ones) with session-only history —
   pure calculation logic ported + tested first, UI after.
**Checkpoint:** sign in → add asset → record valuation → see net worth on dashboard, in a real
browser; `npm test` green.

## Concerns & impact of building Android and Web in parallel

1. **Schema is the one non-parallelizable chokepoint.** SDD-05 §4's workflow is strictly
   sequential (spec → migration PR → push → TS types → Kotlin DTOs → CI verifies both). Android's
   R2 checklist and this web W1 plan both need the *same* P1 tables. Whichever platform's session
   runs first should author and push the migration; the other consumes it read-only. Do **not**
   let both platforms propose competing migrations for the same feature — pick one owner per
   phase before starting.
2. **Solo-maintainer bandwidth (ADR-0001's own stated driver is cost *and time*).** Two live
   workstreams double context-switch cost, and TDD's test-first discipline is the first thing that
   erodes under interrupt-driven work. Recommend: one phase-pair (e.g. R2+W1) fully closed —
   `regressionCheck` green on Android, `npm test` green on web — before opening the next pair,
   rather than N phases open at once on both sides.
3. **Web has no CI gate yet.** `web-ci.yml` doesn't exist (it's W3 scope, currently sequenced
   *after* W1/W2). Building W1/W2 features before it exists means zero automated regression
   catcher on web — the Android side already has 4 gates + `regressionCheck`. Pull a minimal
   `web-ci.yml` (lint + `npm test` + build, path-filtered per ADR-0015) forward to run alongside
   W1, not deferred to W3.
4. **No module-boundary enforcement on web.** Android has ArchUnit (`feature → feature`
   forbidden). Web has nothing stopping `apps/finance` from importing `apps/tools` directly.
   SDD-04's structure implies the same boundary should hold. Add an eslint import-boundary rule
   (e.g. `eslint-plugin-boundaries`) before fanning out to more than one `apps/*` web module in
   parallel — otherwise drift compounds faster on web than it ever could on Android.
5. **Validation/business-logic can silently diverge.** SDD-05 §3 requires identical rules on both
   platforms but there's no shared source of test cases today — each platform's test file is
   authored independently from the same prose table. Recommend a single checked-in test-vector
   file (JSON) both suites load, for at least money formatting and `NetWorthCalculator`/
   `BudgetCalculator`/XIRR — otherwise "fixed on Android" silently doesn't fix web.
6. **Two separate OAuth client configs, easy to only finish one.** Android needs a
   SHA-1-bound Google Cloud client (execution-checklist step B4); web needs a separate
   redirect-URI-bound client (SDD-06 §1). Doing both platforms "in parallel" makes it easy to
   wire one, declare the feature done, and leave the other's sign-in silently broken. Both must
   pass their own sign-in smoke before either platform's phase is called done.
7. **Feature flags mostly shared, one gap.** The static JSON file is genuinely shared (web imports
   `platform/feature-flags/dhruv-finance.json` directly) — low risk. But Android layers Firebase
   Remote Config on top (remote → cached → hardcoded); web reads only the static file (SDD-04 has
   no remote layer specced). A remote kill-switch flip on Android will **not** reach web until a
   redeploy — worth remembering during any incident response, not just at build time.
8. **Design-system code cannot be shared** (Kotlin Compose vs TS/CSS — two by-hand
   implementations of the same tokens). `tokens.css` and `DhruvTheme`'s Material roles must be
   kept numerically in sync by hand on every design change; nothing catches drift automatically.
   The unresolved DhruvNext 4-tab-vs-3-tab nav question (`dhruvnext-design-reference.md`) is
   exactly this class of risk — resolve that ADR before building web navigation in parallel, or
   risk building the wrong nav shape once already.
9. **Web can't be verified the same way Android can** until W3 (deploy) exists — no signed-APK-
   style smoke test equivalent. Recommend pulling a bare-bones Vercel deploy forward alongside W1
   (even before PWA/full `web-ci.yml` polish) so web work has *some* real, checkable artifact
   during the parallel run instead of accumulating unverified on a branch.

**Net recommendation:** parallel is fine and already the intended design (W-track exists for
exactly this), but "parallel" should mean *phase-paired and schema-sequenced*, not *both platforms
building the same feature from a blank sheet simultaneously*. Close R2+W1 as one pair first (one
owns the migration), stand up minimal `web-ci.yml` + a bare deploy target early rather than at W3,
and resolve the nav ADR before web navigation work starts.
