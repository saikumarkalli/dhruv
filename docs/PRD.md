# Dhruv — Product Requirements Document (PRD)

> **Version:** 1.0 · **Date:** 2026-07-16 · **Author:** Sai Kumar Kalli  
> **Status:** ACTIVE — this document is the central index. Individual docs are authoritative for their own scope; this PRD links, summarizes, and orders them.

---

## 1. Product Vision

**Dhruv** is a multi-platform personal-life ecosystem — starting with finance, expanding to tools, security (vault), health, and relationships. One account, one data set, every device.

| Surface | Technology | Status |
|---|---|---|
| **Android** (primary) | Kotlin · Jetpack Compose · Koin · Room + Supabase | Active — Finance v1.3.0 |
| **Web** | Vite + React SPA · TypeScript · supabase-js | Planned (this PRD) |
| iOS / Desktop | — | Future |

**Guiding principles** (from [DECISIONS.md](file:///d:/Work/code-base/dhruv/platform/DECISIONS.md)):
1. **Cost-first** — free tiers, no server costs, zero monthly spend
2. **Solo-maintainer viable** — one person builds, maintains, and ships
3. **Privacy-first** — DPDP compliance baked in, consent before any data leaves device
4. **Offline-resilient** — calculators work offline; tracker is cloud-primary with graceful degradation

---

## 2. Document Map

Every document in the ecosystem, grouped by domain. **This section IS the central index.**

### 2.1 Platform Architecture (source of truth)

| Document | Path | Purpose |
|---|---|---|
| **Platform Architecture** | [PLATFORM.md](file:///d:/Work/code-base/dhruv/platform/PLATFORM.md) | *What* the platform is — modules, stack, security, sync, AI, CI/CD, versioning |
| **Decision Register (ADRs)** | [DECISIONS.md](file:///d:/Work/code-base/dhruv/platform/DECISIONS.md) | *Why* — 14 accepted ADRs (ADR-0001 through ADR-0014) |
| **Implementation Plan** | [Implementation.md](file:///d:/Work/code-base/dhruv/platform/Implementation.md) | Phased build order (Phase 0–7) with DoDs |
| **Agent Rules** | [AGENTS.md](file:///d:/Work/code-base/dhruv/platform/AGENTS.md) | AI/agent session rules |
| **Entity Contract** | [DhruvEntity.kt](file:///d:/Work/code-base/dhruv/platform/contracts/DhruvEntity.kt) | Cross-app entity interface (id, userId, HLC, sync flags) |
| **Version Matrix** | [versions.json](file:///d:/Work/code-base/dhruv/platform/versions.json) | Per-app versions + compatibility |
| **Feature Flags (Finance)** | [dhruv-finance.json](file:///d:/Work/code-base/dhruv/platform/feature-flags/dhruv-finance.json) | 11 feature flags with enabled/minVersion/requiresConsent |
| **Feature Flags (Tools)** | [dhruv-tools.json](file:///d:/Work/code-base/dhruv/platform/feature-flags/dhruv-tools.json) | 6 feature flags (planned app) |
| **Production Readiness** | [PRODUCTION_READINESS.md](file:///d:/Work/code-base/dhruv/platform/PRODUCTION_READINESS.md) | Audit findings + remediation plan |
| **Runbook** | [RUNBOOK.md](file:///d:/Work/code-base/dhruv/platform/RUNBOOK.md) | Operational procedures |

---

### 2.2 Design System & UI/UX

| Document | Path | Purpose |
|---|---|---|
| **Brand Design System** | [DESIGN.md](file:///d:/Work/code-base/dhruv/DESIGN.md) | Colors, typography, gradients, glassmorphism, brand assets |
| **Tracker Design System** | [tracker-design-system.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-tracker-design-system.md) | Component inventory (BentoGrid, HeroStatCard, TrendLineChart, etc.), micro-frontend rule, tokens, states, a11y |
| **App-Wide Design Standard** | [app-design-standard.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-app-design-standard.md) | Extends tracker design to ALL screens — navigation, settings tree, notifications, widget, PDF, route registry |
| **Brand Kit** | [Dhruv_Master_Brand_Kit/](file:///d:/Work/code-base/dhruv/Dhruv_Master_Brand_Kit) | Multi-platform icons, wordmarks, lockups |

---

### 2.3 Feature Specifications — Finance Tracker (P1–P6)

| Phase | Document | Scope | Status |
|---|---|---|---|
| Overview | [tracker-roadmap-overview.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-tracker-roadmap-overview.md) | Umbrella: vision, locked decisions, shared invariants, home bento evolution | Approved |
| **P1** | [p1-networth-tracker-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-p1-networth-tracker-design.md) | Assets, liabilities, valuations, net worth, Google sign-in, DPDP consent | In progress |
| P1 gaps | [p1-gap-analysis.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-04-p1-gap-analysis.md) | G1–G20 gap register for P1 | Approved |
| **P2** | [p2-expenses-budgets-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-p2-expenses-budgets-design.md) | Transactions, income/expense, budgets, savings rate, quick-add | Specced |
| **P3** | [p3-goals-debt-payoff-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-p3-goals-debt-payoff-design.md) | Savings goals, debt payoff (avalanche/snowball), EMI math extraction | Specced |
| **P4** | [p4-insurance-registry-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-p4-insurance-registry-design.md) | Policy registry, renewal reminders, WorkManager, POST_NOTIFICATIONS | Specced |
| **P5** | [p5-retirement-projection-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-p5-retirement-projection-design.md) | Corpus projection, gap analysis, BigDecimal engine, scenarios | Specced |
| **P6** | [p6-automation-groundwork-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-p6-automation-groundwork-design.md) | SMS ingestion, account aggregator eval, auto price feeds, Supabase Realtime | Specced |

---

### 2.4 Feature Specifications — Refinement Phases (R0–R11)

| Phase | Document | Scope | Status |
|---|---|---|---|
| **Master Roadmap** | [master-roadmap-personal-app.md](file:///d:/Work/code-base/dhruv/docs/superpowers/plans/2026-07-12-master-roadmap-personal-app.md) | Unified sequence: R0–R11, gap register (N1–N19), dependency graph | Proposed |
| R3 Security | [r3-app-security-layer-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r3-app-security-layer-design.md) | App lock, privacy mode, FLAG_SECURE | Specced |
| R4 Updates | [r4-inapp-update-check-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r4-inapp-update-check-design.md) | In-app update check via GitHub Releases | Specced |
| R5 ADRs | [r5-accounts-multicurrency-decisions.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r5-accounts-multicurrency-decisions.md) | Accounts entity + multi-currency stance | Specced |
| R5b Recurring | [r5b-recurring-quickadd-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r5b-recurring-quickadd-design.md) | Recurring transactions + quick-add surfaces | Specced |
| R6 Alerts | [r6-budget-alerts-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r6-budget-alerts-design.md) | Budget overrun + EMI due notifications | Specced |
| R7 Reports | [r7-reports-export-import-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r7-reports-export-import-design.md) | Reports module, CSV/PDF export, CSV import | Specced |
| R8 Polish | [r8-daily-driver-polish-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r8-daily-driver-polish-design.md) | Onboarding, widget, search, trash/undo | Specced |
| R9 Returns | [r9-investment-returns-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-r9-investment-returns-design.md) | XIRR / absolute returns on valuation history | Specced |
| Currency | [currency-realtime-rates-daily-notification-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-03-currency-realtime-rates-daily-notification-design.md) | Currency accuracy, gold/silver, daily rates notification | Specced |

---

### 2.5 Reviews & Audits

| Document | Path | Purpose |
|---|---|---|
| Spec Consistency | [spec-consistency-review.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-spec-consistency-review.md) | Cross-spec findings F1–F16 |
| Security/Nav Review | [security-navigation-technical-review.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-security-navigation-technical-review.md) | SEC1–SEC9, NAV1–NAV6 |
| PO Premium Review | [po-premium-daily-use-review.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-12-po-premium-daily-use-review.md) | PG1–PG10 premium daily-driver bar |
| Dev Readiness | [dev-readiness-checklist.md](file:///d:/Work/code-base/dhruv/docs/superpowers/plans/2026-07-12-dev-readiness-checklist.md) | Plugins, deps, connections, tooling |
| CI Optimization | [ci-cost-optimization-commit-type-versioning-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-04-ci-cost-optimization-commit-type-versioning-design.md) | CI cost + commit-type versioning |

---

### 2.6 Software Design Documents (SDD) — Cross-Platform (NEW)

> These 7 documents formalize the shared web + Android architecture. To be created under `docs/sdd/`.

| # | Document | Scope |
|---|---|---|
| 01 | **System Architecture SDD** | System context, multi-app map (Android → Web), deployment topology, feature flags (both platforms), NFRs |
| 02 | **Backend & API SDD** | Supabase schema DDL, PostgREST contract, GoTrue auth, CORS, RLS, error codes, Realtime, migration workflow |
| 03 | **Android App SDD** | Module graph, data layer, auth, state management, security, CI pipeline |
| 04 | **Web App SDD** | Vite+React stack, routing, auth (PKCE), React Query, design system (CSS vars), PWA, i18n, error boundaries |
| 05 | **Shared Data Contracts SDD** | TypeScript + Kotlin types, validation rules, money formatting, category enums, migration workflow |
| 06 | **Auth & Security SDD** | Auth flows (both platforms), token storage, CSP/CORS, rate limiting, DPDP consent, erasure |
| 07 | **Deployment & CI/CD SDD** | Android CI, web CI, path-based triggers, Supabase migrations, env management, observability |

---

### 2.7 Other Project Documents

| Document | Path | Purpose |
|---|---|---|
| **README** | [README.md](file:///d:/Work/code-base/dhruv/README.md) | High-level map (defers to platform/ docs) |
| **Privacy Policy** | [PRIVACY.md](file:///d:/Work/code-base/dhruv/PRIVACY.md) | v1.0 — Android only (v2.0 will cover web) |
| **Changelog** | [CHANGELOG.md](file:///d:/Work/code-base/dhruv/CHANGELOG.md) | Chronological change history |
| **Finance Features** | [FEATURES.md](file:///d:/Work/code-base/dhruv/apps/finance/FEATURES.md) | Per-module detail (screens, VMs, data deps, flags) |
| **License** | [LICENSE](file:///d:/Work/code-base/dhruv/LICENSE) | MIT © 2026 Sai Kumar Kalli |
| **Engineering Playbook** | [tracker-engineering-playbook.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-04-tracker-engineering-playbook.md) | Roles matrix, framework-protection rules, deployment pipeline |
| **P1 Execution Checklist** | [p1-networth-execution-checklist.md](file:///d:/Work/code-base/dhruv/docs/superpowers/plans/2026-07-04-p1-networth-execution-checklist.md) | Task-level checklist for P1 |

---

## 3. Product Scope — What Dhruv Does

### 3.1 Apps (current + planned)

| App | Android | Web | Purpose |
|---|---|---|---|
| **Finance** | `:apps:finance` — Active (v1.3.0) | `web/apps/finance/` — Planned (V1) | Personal finance: tracker (net worth → expenses → goals → insurance → retirement → automation) + calculator tools |
| **Tools** | `:apps:tools` — Planned | `web/apps/tools/` — Future (V2) | Notes, clipboard, timer, QR, weather, AI assistant |
| **Vault** | `:apps:vault` — Future | `web/apps/vault/` — Future (V3) | E2E encrypted password manager |
| **Health** | `:apps:health` — Future | `web/apps/health/` — Future | Health tracking |
| **Relationship** | `:apps:relationship` — Future | `web/apps/relationship/` — Future | Relationship tracking |

### 3.2 Finance App — Feature Matrix

| Feature | Android Module | Web Route | Backend | Status |
|---|---|---|---|---|
| **Net Worth Dashboard** | `:feature:networth` | `/finance/dashboard` | Supabase `assets` + `liabilities` + `valuation_entries` | P1 (in progress) |
| **Expenses & Budgets** | `:feature:expenses` | `/finance/expenses` | Supabase `transactions` + `budgets` | P2 (specced) |
| **Goals & Debt Payoff** | `:feature:goals` | `/finance/goals` | Supabase `goals` + `goal_links` + `payoff_plans` | P3 (specced) |
| **Insurance Registry** | `:feature:insurance` | `/finance/insurance` | Supabase `policies` | P4 (specced) |
| **Retirement Projection** | `:feature:retirement` | `/finance/retirement` | Supabase `retirement_scenarios` | P5 (specced) |
| Calculator | `:feature:calculator` | `/finance/tools/calculator` | Room (local) | ✅ Built |
| Loans (EMI) | `:feature:loans` | `/finance/tools/loans` | Pure calc | ✅ Built |
| Investments (SIP/ROI/FD) | `:feature:investments` | `/finance/tools/investments` | Pure calc | ✅ Built |
| Tax (GST/Salary) | `:feature:tax` | `/finance/tools/tax` | Pure calc | ✅ Built |
| Everyday (Interest/Tip) | `:feature:everyday` | `/finance/tools/everyday` | Pure calc | ✅ Built |
| Currency Converter | `:feature:currency` | `/finance/tools/currency` | Exchange rate API + Room cache | ✅ Built |
| Unit Converter | `:feature:unit` | `/finance/tools/unit` | Pure calc | ✅ Built |
| Date Tools | `:feature:date` | `/finance/tools/date` | Pure calc | ⛔ Flag-disabled |
| Time (Stopwatch/Timer) | `:feature:time` | — (device-specific) | In-memory | ⛔ Flag-disabled |
| AI Assistant | `:feature:assistant` | `/finance/tools/assistant` | Gemini API (via proxy) | Version-gated ≥1.2.0 |

---

## 4. Architecture Summary

### 4.1 Shared Backend (Supabase)

Both platforms consume the **same Supabase project** as "dumb clients." Security is enforced server-side via RLS (`user_id = auth.uid()`).

```
Android (Retrofit/Moshi) ──┐
                           ├──► Supabase PostgREST ──► PostgreSQL (RLS)
Web (supabase-js)    ──────┘         ▲
                                     │
                              GoTrue Auth ◄── Google OAuth
```

**Key decisions:**
- Supabase is primary store for tracker data (ADR-0014)
- Calculator data stays in Room (Android) / in-memory (Web) until Phase 6+ migration
- Auth = Google sign-in via Supabase Auth on both platforms
- Money = integer paise (`Long` / `bigint`) — exact, summable, no floats
- All network access via existing Retrofit/OkHttp (Android) and supabase-js (Web)

### 4.2 Android Architecture

**Single-activity, MVVM, Compose, Koin DI, Room + EncryptedDataStore.**

```
:apps:finance:app          → Shell (MainActivity, hubs, settings, Koin wiring)
:apps:finance:data         → Room DB, DAOs, repositories, Supabase APIs
:apps:finance:feature:*    → 10+ feature modules (each behind FeatureHost + flag)
:libs:core                 → Design system, FeatureHost, security, observability
:libs:settings             → Encrypted settings, settings UI
```

Dependency: `apps:* → libs:settings → libs:core`. Feature modules depend on `:libs:core` + `:data` (via Repository only), **never on each other**.

### 4.3 Web Architecture

**Vite + React 19 SPA, TypeScript, React Query, supabase-js, CSS custom properties.**

```
web/src/
├── apps/finance/          → Finance routes + hooks + components
├── apps/tools/            → Scaffolded (V2)
├── apps/vault/            → Scaffolded (V3)
├── shared/                → Design system, auth, feature flags, Supabase client
│   ├── components/        → BentoGrid, HeroStatCard, etc. (mirrors :libs:core)
│   ├── hooks/             → useAuth, useFeatureFlag, useSupabase
│   ├── lib/               → paise utils, validation, error reporter
│   ├── styles/            → CSS tokens (Dhruv palette), themes
│   └── types/             → Auto-generated Supabase types
└── tests/
```

---

## 5. Technology Stack

| Concern | Android | Web |
|---|---|---|
| **Language** | Kotlin 2.2 | TypeScript 5.x |
| **UI Framework** | Jetpack Compose (Material 3) | React 19 + Vanilla CSS (CSS custom properties) |
| **Build** | AGP 9.1.1 · Gradle · build-logic convention plugins | Vite 6 |
| **DI / State** | Koin + StateFlow (MVVM) | React Context + React Query (TanStack) |
| **Routing** | Single-activity HorizontalPager + bottom nav | React Router v7 (lazy-loaded) |
| **Local DB** | Room + EncryptedDataStore | In-memory / sessionStorage |
| **Remote API** | Retrofit + Moshi + OkHttp | @supabase/supabase-js |
| **Auth** | Credential Manager → Google ID token → GoTrue | OAuth PKCE redirect → GoTrue |
| **Feature Flags** | Firebase Remote Config → cached → JSON asset | Static JSON import (same files) |
| **Observability** | Crashlytics + Performance | Vercel Analytics + console (→ Sentry V2) |
| **CI/CD** | GitHub Actions (4 gates → bump → tag → APK Release) | GitHub Actions (lint → test → build → Vercel deploy) |
| **Hosting** | GitHub Releases (APK); Play deferred | Vercel (free tier) |
| **Security** | 8-layer (Keystore, encryption, cert pin, biometric, R8) | CSP, CORS, RLS, PKCE, httpOnly where possible |
| **PWA** | N/A | vite-plugin-pwa (installable, offline shell) |
| **i18n** | strings.xml (English only V1) | react-intl + en.json (English only V1) |

---

## 6. Security & Compliance

### 6.1 DPDP (India Digital Personal Data Protection)

| Requirement | Implementation |
|---|---|
| Consent before processing | Consent gate before sign-in + any data leaving device (both platforms) |
| Under-18 = child | Parental consent rules; no profiling/ads |
| 7-day erasure | In-app "Delete my data" (hard-delete all tracker rows) + "Delete my account" (rows + auth user) via `delete_my_account()` SQL RPC |
| Consent revocable | "Withdraw consent" in settings → tracker re-gated |
| Data portability | CSV/PDF export (R7) |
| Privacy policy | [PRIVACY.md](file:///d:/Work/code-base/dhruv/PRIVACY.md) v1.0 (→ v2.0 covering web) |

### 6.2 Security Layers

| Layer | Android | Web |
|---|---|---|
| Auth | Google sign-in (Credential Manager) | Google sign-in (OAuth PKCE) |
| Transport | OkHttp cert pinning (ISRG Root X1/X2) | Browser TLS |
| Storage (secrets) | Android Keystore + EncryptedDataStore | N/A (no secrets on web; Supabase JWT in localStorage) |
| Storage (data) | Room (calcualtor) / Supabase (tracker) | In-memory / Supabase |
| Access control | Supabase RLS (`user_id = auth.uid()`) | Same RLS |
| Screen security | FLAG_SECURE (R3), app lock (R3) | Privacy mode (CSS masking), amounts hiding |
| Headers | N/A | CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy |
| Obfuscation | R8/ProGuard (release) | Vite minification |
| Integrity | Play Integrity (warn-only) | Supabase rate limits + Vercel Firewall |

---

## 7. Supabase Schema (All Phases)

Source of truth for table DDL: individual phase specs (§2.3). Migration files in `supabase/migrations/`.

| Table | Phase | Columns (key) | RLS |
|---|---|---|---|
| `assets` | P1 | id, user_id, name, category, notes, currency, is_deleted | own rows |
| `liabilities` | P1 | id, user_id, name, category, notes, currency, is_deleted | own rows |
| `valuation_entries` | P1 | id, user_id, parent_id, parent_type, value_paise, recorded_at, is_deleted | own rows |
| `transactions` | P2 | id, user_id, type (INCOME/EXPENSE), category, amount_paise, occurred_at, account_ref, is_deleted | own rows |
| `budgets` | P2 | id, user_id, category, month_key, limit_paise, is_deleted | own rows |
| `goals` | P3 | id, user_id, name, target_paise, target_date, icon, is_deleted | own rows |
| `goal_links` | P3 | goal_id, asset_id, user_id | own rows |
| `payoff_plans` | P3 | id, user_id, liability_id, strategy, apr_bps, min_payment_paise, is_deleted | own rows |
| `policies` | P4 | id, user_id, type, insurer, sum_assured_paise, premium_paise, renewal_date, is_deleted | own rows |
| `retirement_scenarios` | P5 | id, user_id, name, ages, expense/SIP/rates as _bps, is_deleted | own rows |
| `calculator_history` | P6+ | id, user_id, expression, result, tags, notes, is_favorite, is_deleted | own rows |

**RPCs:** `delete_my_account()` (security definer — deletes all rows + auth user).

**Category enums are TEXT, append-only** — never rename a shipped constant.

**Money is integer paise** (`bigint`) — `BigDecimal` only inside pure calculation engines.

---

## 8. Roadmap — Unified Sequence

Merges the Android master roadmap (R0–R11) with the web app phases (W0–W4+).

### Phase Dependencies

```
R0 hardening ──┬──────────────────────────────► everything
R1 CI costs ───┘ (parallel)
R2 P1 networth ──► R3 security ──► R4 rates+notif+update
                                      │ (plumbing)
R2+R4 ──► R5 P2 expenses (+ADRs) ──► R5b recurring/quick-add
R5 ──► R6a P3 goals ─┐
R2+R4 ─► R6b P4 ins. ┼──► R7 reports/export ──► R9 P5 retirement(+XIRR)
R5 ─────────────────┘         │
R7 ──► R8 polish (onboarding/widget/search/trash)
R9 ──► R10 P6 automation (+localization ADR) ──► R11 platform expansion

W0 SDDs + scaffold ──► W1 auth+consent ──► W2 dashboard+calculators ──► W3 deploy
W3 ──► W4 (P2 web) ──► W5 (P3/P4 web) ──► ... (tracks Android phases)
```

### Sequence Table

| # | Phase | Scope | Android | Web | Spec |
|---|---|---|---|---|---|
| R0 | Production hardening | Firebase wiring, Room migration safety, DPDP consent fixes, cert pinning, PRIVACY.md | ✅ | — | PRODUCTION_READINESS.md |
| R1 | CI cost optimization | Commit-type versioning, path-scoped triggers | ✅ | — | ci-cost-optimization spec |
| R2 | P1 Net Worth Tracker | Assets, liabilities, valuations, Google sign-in, bento Home | ✅ | — | p1-networth spec |
| **W0** | **SDD + Web scaffold** | **7 SDD documents, Vite+React project init, Supabase migrations, ADR-0015** | — | **✅** | **This PRD + implementation plan** |
| **W1** | **Web auth + consent** | **Google OAuth PKCE, consent gate, feature flags, layout** | — | **✅** | **SDD-04, SDD-06** |
| R3 | Security layer | App lock, privacy mode, FLAG_SECURE | ✅ | — | r3-security spec |
| **W2** | **Web dashboard + tools** | **Net worth dashboard, CRUD, calculator tools (session history)** | — | **✅** | **SDD-04, SDD-05** |
| R4 | Currency + updates | Rate accuracy, gold/silver, daily notification, in-app update check | ✅ | ✅ (currency tool) | r4-update spec |
| **W3** | **Web deploy** | **Responsive, dark/light theme, PWA, Vercel deploy, web-ci.yml** | — | **✅** | **SDD-07** |
| R5 | P2 Expenses & Budgets | Transactions, budgets, savings rate, quick-add | ✅ | — | p2-expenses spec |
| R5b | Recurring + quick-add | Recurring rules, app shortcut, review inbox | ✅ | — | r5b-recurring spec |
| **W4** | **P2 Web** | **Expenses & budgets pages on web** | — | **✅** | **SDD-04** |
| R6 | P3 Goals + P4 Insurance | Goals, payoff, insurance, budget alerts | ✅ | — | p3/p4 specs |
| **W5** | **P3/P4 Web** | **Goals, insurance pages on web** | — | **✅** | **SDD-04** |
| R7 | Reports, export, import | Monthly/yearly reports, CSV/PDF export, CSV import | ✅ | ✅ | r7-reports spec |
| R8 | Daily-driver polish | Onboarding, widget, search, trash | ✅ | ✅ (search, trash) | r8-polish spec |
| R9 | P5 Retirement | Projection engine, XIRR returns | ✅ | ✅ | p5-retirement spec |
| R10 | P6 Automation | SMS ingestion, auto feeds, Realtime, offline cache revisit | ✅ | ✅ (Realtime) | p6-automation spec |
| R11 | Platform expansion | Tools app, Vault app, Telegram bot, AI proxy | ✅ | ✅ (app modules) | PLATFORM.md |

---

## 9. Success Criteria

### V1 (Web Finance — W0 through W3)

- [ ] Sign in with Google on web → see same data as Android app
- [ ] Add/edit/delete assets, liabilities, valuations from web
- [ ] Calculator tools work (EMI, SIP, GST, currency, unit) with session-only history
- [ ] Delete data / delete account works from web
- [ ] Consent gate enforced before sign-in
- [ ] Feature flag disable → FeatureDisabledCard
- [ ] PWA installable on mobile browser
- [ ] Dark/light theme matches Android design tokens
- [ ] Responsive (mobile + tablet + desktop)
- [ ] All web CI gates green (lint, typecheck, test, build)
- [ ] PRIVACY.md v2.0 covers web
- [ ] Cross-platform: data added on Android visible on web (and vice versa)

### Ongoing (per tracker phase)

- [ ] Each tracker phase (P2–P6) has web pages shipped within 1 sprint of Android merge
- [ ] Paise formatting identical on both platforms (unit tests assert same outputs)
- [ ] Validation rules identical (same test cases on both platforms)
- [ ] Feature flag keys match between `dhruv-finance.json` and web implementation

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Supabase free-tier limits | Slow fetches as data grows | Paginate/aggregate server-side from R7; P6 revisit |
| Two platforms diverge on validation/formatting | Users see inconsistent data | SDD-05 is the shared contract; both platforms test against it |
| CORS misconfiguration | Web app can't reach Supabase | Documented in SDD-02; verified in W1 smoke test |
| localStorage token theft (XSS) | Session hijack | CSP restricts script sources; React auto-escapes; no dangerouslySetInnerHTML |
| Web app maintenance burden (solo maintainer) | Feature parity lag | Web phases track Android phases; React Query minimizes boilerplate |
| Supabase schema drift | Broken queries on one platform | `supabase/migrations/` is single source of truth; both CIs must pass |
| Calculator history not synced cross-platform | User confusion | Clear UI message; sync planned for P6+ |

---

## 11. Glossary

| Term | Definition |
|---|---|
| **Paise** | Smallest Indian currency unit (1 ₹ = 100 paise). All monetary values stored as integer paise (`bigint`). |
| **RLS** | Row Level Security — Supabase/PostgreSQL feature enforcing `user_id = auth.uid()` per row. |
| **FeatureHost** | Android: Compose wrapper for fault isolation + feature flag check. Web: React `ErrorBoundary` + flag check. |
| **DPDP** | India Digital Personal Data Protection Rules 2025. |
| **PostgREST** | Supabase's auto-generated REST API from PostgreSQL schema. |
| **GoTrue** | Supabase's authentication service (manages Google OAuth, JWTs, sessions). |
| **PKCE** | Proof Key for Code Exchange — OAuth flow for public clients (SPAs). |
| **BPS** | Basis points (1 bps = 0.01%). Used for interest rates and returns in storage. |
| **ADR** | Architecture Decision Record — *why* a decision was made (see DECISIONS.md). |
| **SDD** | Software Design Document — *how* a component is built (see §2.6). |

---

## 12. Document Governance

| Rule | Detail |
|---|---|
| **This PRD** | Central index only — never duplicates content from linked docs. Updated when new docs are added. |
| **PLATFORM.md** | Architecture source of truth. If README.md disagrees, PLATFORM.md wins. |
| **DECISIONS.md** | ADRs are append-only and ACCEPTED. New decisions = new ADR. |
| **Phase specs** | Authoritative for their own scope. Cross-spec consistency verified in spec-consistency-review.md. |
| **SDDs** | Technical implementation details for the cross-platform architecture. |
| **Feature flags JSON** | Runtime truth for feature visibility. Same files consumed by both platforms. |
| **versions.json** | Version matrix — CI-owned for Android; manual for web. |
| **PRIVACY.md** | Legal document — version-bumped on material change. |
