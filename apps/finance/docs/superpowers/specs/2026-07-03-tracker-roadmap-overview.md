# Dhruv Finance — Personal-Finance Tracker: Roadmap Overview

> Status: **APPROVED** (maintainer, 2026-07-03). Governing ADR: **ADR-0014** (`platform/DECISIONS.md`).
> Per-phase specs live alongside this file (`2026-07-03-p1…p6-*.md`). Each phase gets its own
> implementation plan and PR cycle; this file is the umbrella.

## Product vision

One app that tracks a complete personal financial life — assets, liabilities, net worth, expenses,
budgets, savings goals, debt payoff, insurance, retirement readiness — clean, clear, attractive.
Manual entry first; automation (SMS parsing, price feeds, account aggregators) later. The existing
calculators remain available as utilities behind a "Tools" tab but no longer define the app.

## Locked decisions (user-approved)

| # | Decision |
|---|----------|
| U1 | Tracker-first identity: Net Worth dashboard = Home; calculators demoted to one "Tools" tab (internals untouched) |
| U2 | Phase 1 = net worth foundation: assets + liabilities + manual valuations |
| U3 | INR-only display in P1 (currency column reserved server-side for later) |
| U4 | Append-only valuation history; corrections = soft-delete + append, never update |
| U5 | Visual style: Material You bento cards |
| U6 | Phase order: P1 net worth → P2 expenses/budgets → P3 goals + debt payoff → P4 insurance → P5 retirement → P6 automation |
| U7 | Supabase = primary store (cloud-first); overrides offline-first for tracker domain (ADR-0014) |
| U8 | Auth: Google sign-in via Supabase Auth |

## Companion documents (binding)

| Doc | Purpose |
|---|---|
| `platform/DESIGN-SYSTEM.md` | UI/UX source of truth: `:libs:core` component inventory, bento specs, tokens, states, a11y checklist. **Micro-frontend rule**: features own screens/flows only; all reusable visuals live in `:libs:core`; zero feature-local styling — one theme, entire app |
| `2026-07-04-tracker-engineering-playbook.md` | Roles matrix (skill/agent per stage), framework-protection rules, one-go development guarantees, deployment pipeline, secrets inventory |
| `2026-07-04-p1-gap-analysis.md` | End-to-end gap register (G1–G20) + risk resolutions — audit trail; outcomes filled at PR time |

## Shared invariants (every phase inherits)

- **Storage**: Supabase Postgres, RLS `user_id = auth.uid()` on every table, soft-delete rows
  (`is_deleted`), hard-delete paths for DPDP erasure (rows + `delete_my_account()` RPC).
- **Network stack**: Supabase consumed as plain REST (GoTrue + PostgREST) on the existing
  Retrofit + Moshi + OkHttp stack — no supabase-kt/Ktor (ADR-0014 §6); CA-level cert pinning
  (ISRG Root X1/X2); session tokens in encrypted DataStore only.
- **Money**: integer paise (`Long` / `bigint`). `BigDecimal` only inside pure calculation engines.
- **Consent**: persisted + revocable. P1 consent gate covers the Supabase account. Every NEW data
  source (P6: SMS, price feeds, aggregators) needs its own consent screen.
- **Architecture**: network/DTOs/repositories in `:apps:finance:data`; each domain = one feature
  module (`feature → data` via repository only; `feature → feature` forbidden); every route wrapped
  in `FeatureHost`; loans-template observability (`crashReporter.setModule`, `featureError`,
  `performanceTracer.trace`); Koin DI; feature flag per module.
- **UI**: Material You bento via `:libs:core` design-system components (micro-frontend rule);
  Screen → UiState → Content; `DhruvModalSheet` for entry; every screen defines
  default/loading/empty/error/offline states — never blank.
- **Testing**: TDD for pure logic (calculators/engines), ViewModel tests with fakes + Turbine,
  `./gradlew regressionCheck` green before merge; coverage floor never ratcheted ahead of tests.
- **Enums persisted as TEXT are append-only** — never rename a shipped constant.

## Home bento grid evolution

| Phase | Card added to Home |
|-------|--------------------|
| P1 | Hero net-worth card (value, month delta, sparkline) + Assets card + Liabilities card |
| P2 | This-month budget bar + savings-rate chip |
| P3 | Top goal progress ring + next debt-free date |
| P4 | Insurance cover total + next renewal countdown |
| P5 | Retirement "on track / gap ₹X" status chip |
| P6 | (no new card — automation feeds existing cards) |

## Cross-phase engineering ledger

| When | Item |
|------|------|
| P1 | Design-system components land in `:libs:core` (bento, hero, chips, `TrendLineChart`, sheets, consent scaffold, state cards) |
| P2 | Add `BarChart`, `DonutChart` primitives to `:libs:core` |
| P3 | ADR: extract EMI/amortization pure math from `:feature:loans` into `:apps:finance:data` so goals + loans share it without a feature→feature edge |
| P4 | First WorkManager background job (renewal reminders) + `POST_NOTIFICATIONS` runtime permission flow |
| P5 | BigDecimal projection engine; long-horizon chart in `:libs:core` |
| P6 | Consent-per-source model; `valuation_entries.source` column migration (`MANUAL`/`AUTO`); Supabase Realtime; offline-cache revisit of U7 |

## Phase status

| Phase | Spec | Status |
|-------|------|--------|
| P1 net worth | `2026-07-03-p1-networth-tracker-design.md` | Implementation in progress (`feat/networth-tracker`) |
| P2 expenses & budgets | `2026-07-03-p2-expenses-budgets-design.md` | Specced |
| P3 goals & debt payoff | `2026-07-03-p3-goals-debt-payoff-design.md` | Specced |
| P4 insurance registry | `2026-07-03-p4-insurance-registry-design.md` | Specced |
| P5 retirement projection | `2026-07-03-p5-retirement-projection-design.md` | Specced |
| P6 automation groundwork | `2026-07-03-p6-automation-groundwork-design.md` | Specced (research-heavy) |
