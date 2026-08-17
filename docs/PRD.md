# Dhruv — Product Requirements Document (PRD)

> **Version:** 1.1 · **Date:** 2026-08-15 · **Author:** Sai Kumar Kalli  
> **Status:** ACTIVE — this document is the central index. Individual docs are authoritative for their own scope; this PRD links, summarizes, and orders them.
>
> **2026-08-15 — roadmap consolidation.** The P1–P6 tracker specs and R0–R11 refinement-phase specs
> (§2.3/§2.4 in v1.0) were superseded and removed. **The single finalized forward plan is
> `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`** (Phases
> 0–7) with its companion functional spec, module-standard/TDD process doc, QA scenario catalog, and
> surface registries — all four live under `apps/finance/docs/superpowers/`. Reviews/audits scoped to
> the deleted specs were removed with them. Every app's own docs now live under that app's `docs/`
> folder exclusively (`apps/finance/docs/`, `web/docs/`) — this root `docs/` and `platform/` hold
> only genuinely cross-app material, verified by content scan on this date.

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

> **Global vs app-level split (2026-08-09).** `docs/` and `platform/` hold only documents that are
> genuinely cross-app or architecture-level — platform decisions (ADRs), the system/backend/shared-
> contracts/auth/deployment SDDs, this PRD. Everything specific to one app's own build — tracker
> specs, phase plans, the design-v1 functional spec/implementation plan/QA catalog, that app's own
> SDD — lives under that app's own `docs/` folder (`apps/finance/docs/`, `web/docs/`), mirroring the
> same `sdd/`, `superpowers/specs/`, `superpowers/plans/` shape this root `docs/` uses. Same
> reasoning as the `apps/finance/feature/` module reorg the same day: a doc's location should say
> who owns it. This index still links every doc regardless of where it lives — it's the map, not a
> gate.

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
| **Production Readiness** | *maintained privately — not part of this repo* | Audit findings + remediation plan for `:apps:finance` |
| **Runbook** | [RUNBOOK.md](file:///d:/Work/code-base/dhruv/platform/RUNBOOK.md) | Operational procedures |

---

### 2.2 Design System & UI/UX

> **One design system, globally (ADR-0030, 2026-08-09).** `platform/DESIGN-SYSTEM.md` is the single
> design contract for every Dhruv app and the web SPA — there is no per-app design system. It
> replaced three competing documents: the root `DESIGN.md` (a pre-DhruvNext token extraction that
> mis-stated the typography), `tracker-design-system.md` (specced a `BentoGrid`/`HeroStatCard`
> component library that was never built, against the retired `SectionTheme`), and
> `app-design-standard.md` (whose sound app-wide law was folded into the global doc and whose
> Finance-specific registries moved to the Finance surface-registries doc). All three are retired.

| Document | Path | Purpose |
|---|---|---|
| **Design System (global)** ⭐ | [DESIGN-SYSTEM.md](file:///d:/Work/code-base/dhruv/platform/DESIGN-SYSTEM.md) | **BINDING for all apps + web.** Brand chrome vs app tokens, typography, spacing/radii/responsive tiers, logo directions, the built component library (+ planned batches), navigation law N1–N7, screen-state matrix, interaction/motion/a11y/copy standards, non-Compose surface conventions, web parity |
| **Brand Kit** | [Dhruv_Master_Brand_Kit/](file:///d:/Work/code-base/dhruv/Dhruv_Master_Brand_Kit) | Multi-platform icons, wordmarks, lockups (the rendered assets; DESIGN-SYSTEM.md §4 is the spec) |
| **Finance — Design v1.0 FINAL Functional Spec** | [design-v1-final-functional-spec.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md) | Finance **product** spec: 61 screens (groups A–G), business rules, user flows, as-is audit, open items. Cites the global design system rather than restating it |
| **Finance — Surface Registries** | [finance-surface-registries.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md) | Finance's per-surface indexes: route registry, notification channels, intent actions, settings IA tree |
| **Finance — Implementation Plan** | [design-v1-final-implementation-plan.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md) | Solution architecture: module topology, Supabase schema, nav rebuild, component batches, phases 0–7, risks |

---

### 2.3 Finance — Forward Plan (the one active roadmap)

The P1–P6 tracker specs and R0–R11 refinement specs formerly indexed here are **retired** — folded
into, or superseded by, the design-v1 FINAL functional spec and implementation plan. Do not recreate
a second roadmap; extend these four documents instead.

| Document | Path | Purpose |
|---|---|---|
| **Functional Spec** | [design-v1-final-functional-spec.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md) | 61 screens (A–G), business rules, flows — the *what* |
| **Implementation Plan** | [design-v1-final-implementation-plan.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md) | Module topology, Supabase schema, nav rebuild, Phases 0–7 with DoDs — the *how* |
| **Module Standard & TDD Process** | [module-standard-and-tdd-process.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/specs/2026-08-09-module-standard-and-tdd-process.md) | Binding on how every phase step is executed (RED/GREEN/REFACTOR, roles) |
| **QA Test Scenario Catalog** | [qa-test-scenario-catalog.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md) | Every scenario a module must satisfy, written before its code |
| **Surface Registries** | [finance-surface-registries.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/superpowers/specs/2026-08-09-finance-surface-registries.md) | Route/notification/intent/settings registries |

Build state: Phase 0 (shell) and Phase 1 (identity & consent) shipped. Phase 2 (net worth) next —
see the implementation plan §7 and §10.

**Spec-kit** (2026-08-16 on): each phase above is formalized as a spec-kit feature under
`apps/finance/specs/NNN-slug/` (spec.md/plan.md/tasks.md) — never repo-root `specs/`, reserved for
cross-app work. Phase→directory mapping: implementation plan §7's tracking table. Directory
convention: `.specify/memory/constitution.md` Spec-Kit Directory Rule.

---

### 2.6 Software Design Documents (SDD) — Cross-Platform

> All 7 exist now (this table predates their creation). **Location split (2026-08-09):** the
> cross-app ones (01, 02, 05, 06, 07) live in `docs/sdd/` — global, apply regardless of which app.
> 03 (Android, entirely Finance-app content today) moved to `apps/finance/docs/sdd/`; 04 (Web)
> moved to `web/docs/sdd/` — each next to the app it actually documents. See §2.7's app-level-docs
> note.

| # | Document | Scope | Location |
|---|---|---|---|
| 01 | **System Architecture SDD** | System context, multi-app map (Android → Web), deployment topology, feature flags (both platforms), NFRs | [docs/sdd/01-system-architecture-sdd.md](file:///d:/Work/code-base/dhruv/docs/sdd/01-system-architecture-sdd.md) |
| 02 | **Backend & API SDD** | Supabase schema DDL, PostgREST contract, GoTrue auth, CORS, RLS, error codes, Realtime, migration workflow | [docs/sdd/02-backend-api-sdd.md](file:///d:/Work/code-base/dhruv/docs/sdd/02-backend-api-sdd.md) |
| 03 | **Android App SDD** | Module graph, data layer, auth, state management, security, CI pipeline | [apps/finance/docs/sdd/03-android-app-sdd.md](file:///d:/Work/code-base/dhruv/apps/finance/docs/sdd/03-android-app-sdd.md) |
| 04 | **Web App SDD** | Vite+React stack, routing, auth (PKCE), React Query, design system (CSS vars), PWA, i18n, error boundaries | [web/docs/sdd/04-web-app-sdd.md](file:///d:/Work/code-base/dhruv/web/docs/sdd/04-web-app-sdd.md) |
| 05 | **Shared Data Contracts SDD** | TypeScript + Kotlin types, validation rules, money formatting, category enums, migration workflow | [docs/sdd/05-shared-data-contracts-sdd.md](file:///d:/Work/code-base/dhruv/docs/sdd/05-shared-data-contracts-sdd.md) |
| 06 | **Auth & Security SDD** | Auth flows (both platforms), token storage, CSP/CORS, rate limiting, DPDP consent, erasure | [docs/sdd/06-auth-and-security-sdd.md](file:///d:/Work/code-base/dhruv/docs/sdd/06-auth-and-security-sdd.md) |
| 07 | **Deployment & CI/CD SDD** | Android CI, web CI, path-based triggers, Supabase migrations, env management, observability | [docs/sdd/07-deployment-and-ci-cd-sdd.md](file:///d:/Work/code-base/dhruv/docs/sdd/07-deployment-and-ci-cd-sdd.md) |

---

### 2.7 Other Project Documents

| Document | Path | Purpose |
|---|---|---|
| **README** | [README.md](file:///d:/Work/code-base/dhruv/README.md) | High-level map (defers to platform/ docs) |
| **Privacy Policy** | [PRIVACY.md](file:///d:/Work/code-base/dhruv/PRIVACY.md) | v1.0 — Android only (v2.0 will cover web) |
| **Changelog** | [CHANGELOG.md](file:///d:/Work/code-base/dhruv/CHANGELOG.md) | Chronological change history |
| **Finance Features** | [FEATURES.md](file:///d:/Work/code-base/dhruv/apps/finance/FEATURES.md) | Per-module detail (screens, VMs, data deps, flags) |
| **License** | [LICENSE](file:///d:/Work/code-base/dhruv/LICENSE) | MIT © 2026 Sai Kumar Kalli |
| **CI Optimization** | [ci-cost-optimization-commit-type-versioning-design.md](file:///d:/Work/code-base/dhruv/docs/superpowers/specs/2026-07-04-ci-cost-optimization-commit-type-versioning-design.md) | CI cost + commit-type versioning (implemented, ADR-0025/0026) |

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

Module names and phase numbers below are the design-v1 implementation plan's (§6–§7), the single
active roadmap — not the retired P1–P6 naming.

| Feature | Android Module | Backend | Status |
|---|---|---|---|
| **Onboarding & consent** | `:feature:onboarding` | Supabase GoTrue + `holdings`/`valuations` RLS | ✅ Phase 1 shipped |
| **Net worth (holdings, valuations, liabilities)** | `:feature:networth` (planned) | Supabase `holdings` + `valuations` + `liabilities_meta` | Phase 2 — next |
| **Money (ledger, accounts, categories, recurring)** | `:feature:money` (planned) | Supabase `accounts` + `transactions` + `categories` | Phase 3 |
| **Planning (budgets, goals, debt payoff)** | `:feature:planning` (planned) | Supabase `budgets` + `goals` + `goal_links` | Phase 4 |
| **Insurance registry** | `:feature:insurance` (planned) | Supabase `policies` + `policy_premiums` | Phase 4 |
| **Retirement projection** | `:feature:retirement` (planned) | Supabase `retirement_scenarios` | Phase 4 |
| **Insights (statements, reports, export)** | `:feature:insights` (planned) | Supabase views (`v_cashflow`, `v_pnl`, …) | Phase 5 |
| **Automation (SMS/AA suggestions, review queue)** | `:feature:automation` (planned) | Supabase `suggestions` + `automation_rules` | Phase 7, flag-off until then |
| Calculator | `:feature:calculator` | Room (local) | ✅ Built |
| Loans (EMI) | `:feature:loans` | Pure calc | ✅ Built |
| Investments (SIP/ROI/FD) | `:feature:investments` | Pure calc | ✅ Built |
| Tax (GST/Salary) | `:feature:tax` | Pure calc | ✅ Built |
| Everyday (Interest/Tip) | `:feature:everyday` | Pure calc | ✅ Built |
| Currency Converter | `:feature:currency` | Exchange rate API + Room cache | ✅ Built |
| Unit Converter | `:feature:unit` | Pure calc | ✅ Built |
| Date Tools | `:feature:date` | Pure calc | ⛔ Flag-disabled |
| Time (Stopwatch/Timer) | `:feature:time` | In-memory | ⛔ Flag-disabled |
| AI Assistant | `:feature:assistant` | Gemini API (via proxy) | Version-gated ≥1.2.0 |

Web routes for the new tracker modules are not yet defined — the web track picks up each phase's
routes one phase behind Android (`web-android-parallel-dev` decision), see §2.3.

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
| **CI/CD** | GitHub Actions (gates on PR → `main`-only bump/tag/APK Release, gated by `prod` Environment approval — ADR-0032) | GitHub Actions (lint → typecheck → test → build gate only); **deploy is Vercel's own Git integration**, not Actions |
| **Hosting** | GitHub Releases (APK); Play deferred | Vercel (free tier) — Preview per branch/PR, Production on `main` |
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

## 7. Supabase Schema

Authored declaratively, one Postgres schema per app (ADR-0033), in `supabase/schemas/<app>/`
(`supabase db diff` generates the actual executed migration into `supabase/migrations/` —
currently `0001_init.sql` plus `20260816211500_move_tracker_to_finance_schema.sql`: `finance.
holdings`, `finance.valuations`, Phase 1's auth/consent tables; ADR-0032/ADR-0033), plus the full
target schema table in the implementation plan §5.4
(all Phase 1–7 tables: `accounts`, `transactions`, `categories`, `budgets`, `goals`, `policies`,
`retirement_scenarios`, `suggestions`, `automation_rules`, and the server-side views — all land
under `finance.*`, not `public.*`). Not reproduced here — this PRD links, per §12's governance rule
against duplicating linked content.

**`public` is reserved for cross-app orchestration only** (ADR-0033) — today, the two erasure
functions below. A future app's own tables get their own schema (`tools.*`, …), never `public.*`
or a prefix inside `finance`.

**RPCs:** `public.delete_my_account()` (security definer — deletes all rows across every app schema
+ the shared `auth.users` row, ADR-0014 §7).

**Category enums are TEXT, append-only** — never rename a shipped constant.

**Money is integer paise** (`bigint` / Kotlin `Long`) — `BigDecimal` only inside pure calculation
engines (calculator tools, retirement projection), never on the tracker write path
(`checkTrackerMoneyPrecision`, implementation plan §8).

---

## 8. Roadmap

**Single active sequence: the design-v1 implementation plan's Phases 0–7** (§2.3). The former
dual R0–R11 / W0–W5 numbering is retired — it tracked the P1–P6 specs, which no longer exist.

| Phase | Scope | Status |
|---|---|---|
| 0 | Design-system + 5-tab shell foundation | ✅ shipped |
| 1 | Identity & consent (Google sign-in, `holdings`/`valuations` schema) | ✅ shipped |
| 2 | Net worth (C1–C7) + real Home | next |
| 3 | Money tab — ledger, accounts, categories, recurring (D1–D9) | planned |
| 4 | Plan live modules — budgets, goals, debt payoff, insurance, retirement (E1–E9) | planned |
| 5 | Insights — statements, reports, export (F1–F5) | planned |
| 6 | Search & notifications (B2, B3) | planned |
| 7 | Automation — SMS/AA suggestions, review queue (G1–G3), flag-off until this checkpoint | planned |

Full per-phase role tables (SA/QA/Backend/Android/Sec), schema, checkpoints, and risk register:
implementation plan §7–§9. Web tracks Android one phase behind, schema-sequenced (`web-android-
parallel-dev` decision) — no separate web phase numbering.

Platform-wide items outside this app's phase sequence — Tools app, Vault app, Telegram bot —
are tracked in [PLATFORM.md](file:///d:/Work/code-base/dhruv/platform/PLATFORM.md) §13, not here.

---

## 9. Success Criteria

### V1 (Web Finance — scaffold through deploy)

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

- [ ] Each tracker phase (2–7, implementation plan §7) has web pages shipped within 1 sprint of Android merge
- [ ] Paise formatting identical on both platforms (unit tests assert same outputs)
- [ ] Validation rules identical (same test cases on both platforms)
- [ ] Feature flag keys match between `dhruv-finance.json` and web implementation

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Supabase free-tier limits | Slow fetches as data grows | Server-side views (implementation plan §5.4) aggregate from Phase 2 onward; revisit at Phase 5 |
| Two platforms diverge on validation/formatting | Users see inconsistent data | SDD-05 is the shared contract; both platforms test against it |
| CORS misconfiguration | Web app can't reach Supabase | Documented in SDD-02; verified on the web track's first auth phase |
| localStorage token theft (XSS) | Session hijack | CSP restricts script sources; React auto-escapes; no dangerouslySetInnerHTML |
| Web app maintenance burden (solo maintainer) | Feature parity lag | Web phases track Android phases one behind; React Query minimizes boilerplate |
| Supabase schema drift | Broken queries on one platform | `supabase/migrations/` is single source of truth; both CIs must pass |
| Calculator history not synced cross-platform | User confusion | Clear UI message; sync revisited at Phase 7 (automation) |

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
| **Finance forward plan** | The design-v1 functional spec + implementation plan (§2.3) are authoritative for Finance's own scope; retired phase specs are not recreated. |
| **SDDs** | Technical implementation details for the cross-platform architecture. |
| **Feature flags JSON** | Runtime truth for feature visibility. Same files consumed by both platforms. |
| **versions.json** | Version matrix — CI-owned for Android; manual for web. |
| **PRIVACY.md** | Legal document — version-bumped on material change. |
