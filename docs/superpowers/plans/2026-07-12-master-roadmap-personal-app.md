# Master Roadmap — Dhruv as a Production-Level Personal Application

> Status: **PROPOSED** (2026-07-12). Synthesized from a 3-agent full-application audit:
> (A) implemented-code inventory, (B) spec/plan inventory, (C) production-readiness audit.
> This document does two things:
> 1. **Gap register** — features a production personal-finance application needs that appear
>    in NO existing spec, plan, ADR, or code (`docs/superpowers/*`, `platform/*`).
> 2. **Master sequence** — one ordered roadmap merging everything already specced (P1–P6,
>    currency/metals, CI optimization, PRODUCTION_READINESS remediation) with the new gaps.
>
> Existing specs remain authoritative for their own scope. This doc only orders them and
> fills holes. New items marked **NEW** need their own spec/ADR before build (per playbook).

---

## 1. Current state (one paragraph per axis)

**Built & working:** 11 feature modules (calculator premium-grade, loans, investments, tax,
everyday, currency, unit, assistant w/ consent gate; date + time built but flag-disabled),
Room v5 data layer (history + currency cache), Gemini integration, feature-flag system
(minVersion + requiresConsent), FeatureHost isolation, theme system, settings, 25 test files,
4-gate CI with auto-versioning and APK releases.

**Specced but not built:** P1 net worth (in flight, `feat/networth-tracker`), P2 expenses/budgets,
P3 goals/debt payoff, P4 insurance registry, P5 retirement projection, P6 automation groundwork,
currency accuracy + gold/silver + daily notification, CI cost optimization + commit-type
versioning, Tools app, Vault app, Telegram bot, Cloudflare AI proxy.

**Production-readiness (verified by audit C, tracked in `platform/PRODUCTION_READINESS.md`):**
Firebase entirely unwired (Crashlytics/Perf/Remote Config inert — app ships blind); destructive
Room migration + missing MIGRATION_1_2 + `exportSchema=false`; backup rules are commented-out
stubs (encrypted DataStore becomes undecryptable after device restore); history PIN plaintext
with default `"1234"`; calculator AI path has **no consent gate**; assistant consent in-memory
only; Gemini key placeholder ships in APK (AI dead in every release build); no cert pinning;
no privacy policy; ~199 hardcoded UI strings; OWASP gate is a no-op.

---

## 2. Gap register — missing from ALL specs and code

Features neither implemented nor documented anywhere. Each gets a phase assignment in §3.

| # | Gap | Why it matters for a personal finance app | Phase |
|---|-----|------------------------------------------|-------|
| N1 | **App-level lock** (BiometricPrompt Class 3 + device-credential fallback, auto-lock timeout) | App will show complete net worth, debts, policies. Biometric is specced for Vault only; the Finance app — the one that actually holds the money picture — has nothing but a decorative history PIN. | R3 |
| N2 | **Privacy mode** (hide-balances toggle, masked `₹•••••` until tap/unlock) | Standard in every banking/finance app; needed when opening the app in public. | R3 |
| N3 | **FLAG_SECURE on tracker screens** | Specced for vault screens only; net-worth/holdings screens are equally sensitive (screenshots, app-switcher preview). | R3 |
| N4 | **In-app update check** (GitHub Releases API → "new version" notification/banner + APK download link) | Distribution is sideloaded APK via GitHub Releases. There is NO update channel at all — without this, installs silently rot. | R4 |
| N5 | **Onboarding / first-run flow** (what the app is, consent up front, seed first asset) | Tracker-first identity is new; first-run currently drops the user on an empty Home with no guidance. | R8 |
| N6 | **Accounts entity/management** (bank accounts as first-class rows) | P2's `transactions.account_ref` is a dangling nullable FK "for automation territory, P6" — but no accounts feature is specced anywhere. Decide the entity now or P2 data is permanently unlinkable. | R5 (ADR) |
| N7 | **Recurring transactions** (salary, rent, EMI, SIP auto-log with review) | P2 is manual-only and P6 automation doesn't cover rule-based recurrence. The single biggest friction reducer for a daily-use expense tracker. | R5b |
| N8 | **Quick-add surfaces**: app shortcut (long-press icon → Quick Add), optionally QS tile | P2's `<5s per entry` target dies if entry requires app-open → tab → FAB. | R5b |
| N9 | **Home-screen widget** (net worth summary; quick-add expense) — Glance | Daily-glance value without opening the app. | R8 |
| N10 | **Global search** (transactions, assets, policies, notes) | Unfindable data = unusable history once volume grows. | R8 |
| N11 | **Reports module** (monthly/yearly income-expense statement, category trends, net-worth statement view) | P2 bento cards show current month only; P5 covers retirement only. No way to answer "what happened last year?" | R7 |
| N12 | **Tracker data export** (CSV per table + PDF net-worth statement) | DPDP data-portability good practice; also the user's own escape hatch — cloud-primary store with no export is a lock-in trap. Only *deletion* is specced today. | R7 |
| N13 | **Data import** (CSV seed for opening balances/history from existing spreadsheets) | First-run migration path; entering years of data by hand is a non-starter. | R7 |
| N14 | **Multi-currency valuation** (FX-convert non-INR holdings into net worth) | Schema has `currency text default 'INR'` on every table but zero conversion logic specced; foreign assets (US stocks, crypto) silently sum wrong. Needs explicit ADR even if answer is "INR-only, validated". | R5 (ADR) |
| N15 | **Investment returns math** (XIRR/absolute return on real valuation history) | P1 stores the full valuation series; nothing ever computes returns from it. Natural P3/P5 adjunct. | R9 |
| N16 | **Bill/EMI due + budget-overrun notifications** | Only insurance renewals (P4) and daily FX rates are specced. Budget alerts are the actionable half of budgeting. | R6 |
| N17 | **Deleted-items restore UI** (trash surface + undo snackbars) | Every tracker table is soft-delete by design, but no screen ever lists or restores deleted rows. Calculator history already has a recycle-bin precedent. | R8 |
| N18 | **Localization plan** (en → hi/te at minimum) | Blocked on string externalization (M3, already tracked) but no language decision exists anywhere. Personal choice — may be explicitly declined in an ADR. | R10 |
| N19 | **Android hygiene**: predictive back, per-app language, notification-permission UX pattern | Small, but production polish. Notification permission flow arrives with R4 anyway. | R4/R10 |

Explicitly **out of scope** (personal app, single user): family/shared profiles, credit-score
integrations, Play Store launch (stays deferred per ADR-0008).

---

## 3. Master sequence

> **Spec status update (2026-07-12):** every NEW phase below now has an execution-ready spec in
> `docs/superpowers/specs/` (localization N18 deliberately excluded):
> R3 → `2026-07-12-r3-app-security-layer-design.md` ·
> R4/N4 → `2026-07-12-r4-inapp-update-check-design.md` ·
> R5 ADRs → `2026-07-12-r5-accounts-multicurrency-decisions.md` ·
> R5b → `2026-07-12-r5b-recurring-quickadd-design.md` ·
> R6/N16 → `2026-07-12-r6-budget-alerts-design.md` ·
> R7 → `2026-07-12-r7-reports-export-import-design.md` ·
> R8 → `2026-07-12-r8-daily-driver-polish-design.md` ·
> R9/N15 → `2026-07-12-r9-investment-returns-design.md`.
> Cross-spec review: `2026-07-12-spec-consistency-review.md` (F1–F16; 🔴 findings patched).
> App-wide UI/UX standard (binding, extends the tracker design system to navigation, settings,
> notifications, widget, PDF, legacy screens): `2026-07-12-app-design-standard.md`.
> BSA/PO premium daily-use review: `2026-07-12-po-premium-daily-use-review.md` — PG1–PG10 all
> ride existing phases (P2 CRUD + TRANSFER type via R5 decisions [ADR-0022]; safe-to-spend pace,
> Upcoming view, stale-valuation nudges, notification quick actions via R6; monthly digest +
> insight chips via R7; onboarding seed checklist via R8; computed loan balance = P3-build
> ADR-0023). Verdict: with these, the plan clears the premium daily-driver bar.
> Security + navigation technical review: `2026-07-12-security-navigation-technical-review.md`
> (SEC1–SEC9, NAV1–NAV6; 🔴 SEC1/SEC2 patched into R3; NAV1 = no NavHost exists in code →
> **ADR-0024** navigation architecture reserved, must precede R3/R5b implementation).
> Dev readiness (plugins/deps/connections/tooling): `2026-07-12-dev-readiness-checklist.md`.
> Planned ADR numbers reserved by these specs: ADR-0017 (accounts are assets), ADR-0018
> (INR-only validated), ADR-0019 (app lock = system auth), ADR-0020 (update channel =
> BYO-token GitHub poll), ADR-0021 (XIRR vs growth-rate split).

Ordering rules used: (1) stop-data-loss and observability before features; (2) already-approved
specs keep their internal order (P1→P2→P3/P4→P5→P6); (3) security lands immediately after the
first sensitive data ships (P1); (4) plumbing (WorkManager/notifications) lands once, then is
reused; (5) each phase ends with the standard gate: `regressionCheck` green + /dhruv-pre-merge
PASS + release build smoke.

### R0 — Production hardening (BLOCKS everything)
Source: `platform/PRODUCTION_READINESS.md` phases 0–4 (already planned there; sequenced first here).
- Decisions D1–D3 (AI key model, main-DB encryption, v1-schema check).
- Stop data loss: remove `fallbackToDestructiveMigration`, add MIGRATION_1_2, `exportSchema=true`
  + migration tests, real backup/data-extraction rules (exclude `secure_settings`).
- DPDP now: consent gate on calculator AI path (C3), persist assistant consent w/ timestamp +
  policy version (M6), fix assistant prompt (M7), input cap + rate limit (M8).
- Firebase wired for real: google-services plugin + json, Crashlytics, Performance (trace actual
  Gemini latency), `FirebaseFeatureFlagResolver` injected → kill switches become real.
- Security: history PIN → salted hash (H7), `GeminiKeyProvider` call-time resolution + BYO key
  wired (C1/C2), CA-level cert pinning (H4), OWASP gate enforced weekly (M1, folds into R1).
- PRIVACY.md + LICENSE (M5).
- **Exit:** release APK with working crash reporting, no data-loss paths, no unconsented egress.

### R1 — CI cost optimization + commit-type versioning
Source: approved spec `2026-07-04-ci-cost-optimization-commit-type-versioning-design.md` (ADR-0025/0026 — renumbered from 0015/0016, which collided with the accepted ADR-0015 Web app decision).
Separate branch; prerequisite: "require branches up to date" branch protection.
Cheap, pays for itself on every subsequent merge — do before the feature avalanche. Can run
parallel to R0 (different files).

### R2 — P1 Net Worth Tracker (in flight)
Source: `2026-07-03-p1-networth-tracker-design.md` + execution checklist. Finish per checklist
(Supabase, Google sign-in, persisted consent, bento Home, Tools tab relocation, delete-my-data/
account). G1–G20 already closed in spec.

### R3 — Personal-data security layer (**NEW — spec needed**)
Immediately after P1 because net worth data now renders on screen.
- N1 app lock: BiometricPrompt Class 3, device-credential fallback, auto-lock on background
  (configurable timeout), gate = whole app or tracker-only (decide in spec).
- N2 privacy mode: hide-balances toggle in top bar + setting; masks all money text via the
  design-system money components (single choke point — `formatPaise*` render path).
- N3 FLAG_SECURE on tracker routes (route-scoped, like vault's spec).
- Session hygiene: re-auth after N days (Supabase refresh already handles token side).
- **Exit:** cold start → biometric → Home; app-switcher shows no balances; screenshots blocked
  on tracker screens.

### R4 — Currency accuracy + metals + daily notification + update check
Source: approved `2026-07-03-currency-realtime-rates-daily-notification-design.md` plan, plus:
- Lands the shared plumbing: WorkManager + Koin worker DI, notification channels,
  POST_NOTIFICATIONS runtime flow (N19 pattern) — P4/R6 reuse all of it.
- **NEW N4:** in-app update check — daily WorkManager fetch of latest GitHub Release tag,
  compare semver vs `BuildConfig.VERSION_NAME`, notify + Settings row ("Update available →
  download"). Small: one API call, no new consent (public GET, no user data).
- **Exit:** daily rates notification at chosen time; gold/silver cards; update notification fires
  on a stale install.

### R5 — P2 Expenses, Income & Budgets (+ two ADRs first)
Source: `2026-07-03-p2-expenses-budgets-design.md`, unchanged, plus pre-work:
- **NEW N6 ADR:** accounts entity — either ship minimal `accounts` table now (name, type,
  linked asset) or formally declare `account_ref` unused until P6. No dangling FK.
- **NEW N14 ADR:** multi-currency stance — INR-only (validate + reject non-INR input) or
  FX-convert via existing rates cache. Cheapest honest answer is fine; silence is not.
- **Exit:** P2 spec's own DoD (quick-add <5s, budgets, savings rate on Home).

### R5b — Recurring + quick-add surfaces (**NEW — spec needed**)
Fast-follow on P2 (needs `transactions` live):
- N7 recurring rules: template txn + RRULE-lite schedule (monthly/weekly/salary-day), WorkManager
  materializes due rows into a confirm inbox (reuses P6's review-inbox concept early, manual-safe).
- N8 app shortcut → QuickAddSheet as standalone activity/deep-link destination.
- **Exit:** rent/salary auto-appear on due day pending one-tap confirm; long-press icon → add
  expense in <5s.

### R6 — P3 Goals & Debt Payoff ∥ P4 Insurance (parallel tracks)
Both source specs unchanged. P3 needs P2 savings rate + EMI-math extraction ADR (specced).
P4 needs only P1 + R4's notification plumbing.
- **NEW N16 (rides P4's reminder work):** budget-overrun notification (threshold 80%/100%) and
  EMI-due reminders derived from payoff plans — same WorkManager check, same channel pattern.
- **Exit:** each spec's own DoD + budget alert fires when a category crosses threshold.

### R7 — Reports, export & import (**NEW — spec needed**)
After P2 data accumulates; before P5 (reports surface the inputs P5 consumes).
- N11 reports: month/year statement screen (income vs expense, category trends via existing
  BarChart/DonutChart), net-worth statement view (period start/end/delta from valuations).
- N12 export: CSV per table (Storage Access Framework, no extra permission) + PDF net-worth
  statement; DPDP portability note in privacy policy.
- N13 import: CSV template → validate → preview → bulk insert (transactions + opening
  valuations). This is also the "migrate my existing spreadsheet" path.
- **Exit:** export → wipe (delete-my-data) → import round-trips losslessly.

### R8 — Daily-driver polish (**NEW — spec needed**)
- N5 onboarding: 3-screen first-run (identity, consent framing, add-first-asset CTA).
- N9 widget: Glance net-worth summary (respects privacy mode = shows masked) + quick-add action.
- N10 global search: server-side `ilike` across transactions/assets/policies/notes (PostgREST),
  single search screen, recent-queries local.
- N17 trash: unified "Recently deleted" screen (30-day window like calculator recycle bin) +
  undo snackbar on every delete.
- **Exit:** fresh install → guided to first asset; widget on launcher; any deleted row recoverable
  for 30 days.

### R9 — P5 Retirement Projection (+ returns math)
Source spec unchanged (needs P1–P3).
- **NEW N15 (natural adjunct):** `ReturnsCalculator` (XIRR over valuation series per asset +
  portfolio) in the same pure-math package as `RetirementProjector`; surfaces on
  HoldingDetailScreen + investments hub.
- **Exit:** spec DoD + per-asset annualized return visible.

### R10 — P6 Automation groundwork
Source spec unchanged (SMS/notification ingestion spike, AA decision doc, auto NAV/gold feeds,
Supabase Realtime, offline-cache revisit U7). Each item = own ADR + own consent screen.
- **N18 decision rides here:** localization go/no-go ADR (string externalization M3 must be done
  by now — it's in R0/R8 debt payments).
- N19 leftovers: predictive back, per-app language if N18 = go.

### R11 — Platform expansion (optional, unchanged from PLATFORM.md)
Tools app, Vault app (its full security spec already exists), Telegram bot, AI proxy Worker
(if BYO-key-only proves insufficient), Dhruv ID/web hub. Sequenced last: the personal finance
mission is complete without them; Vault is the most personally valuable — pull forward if desired.

---

### W0 — Web SDDs + Scaffold (**NEW**)
- **Scope:** 7 SDD documents, Vite+React project initialization, Supabase migrations setup.
- **Exit:** PRD and SDDs complete; `web/` directory scaffolded.

### W1 — Web Auth + Consent (**NEW**)
- **Scope:** Google OAuth PKCE, consent gate, feature flags JSON sharing, root layout.
- **Exit:** Web user can sign in and see consent gate; feature flags evaluate correctly.

### W2 — Web Dashboard + Calculators (**NEW**)
- **Scope:** Net worth dashboard (assets/liabilities CRUD), calculator tools with session-only history.
- **Exit:** Web user can manage net worth; calculators work independently of Android history.

### W3 — Web Deploy (**NEW**)
- **Scope:** Responsive design, dark/light theme, PWA manifest, Vercel deployment, `web-ci.yml`.
- **Exit:** App lives at `dhruv-finance.vercel.app` and is installable as a PWA.

### W4+ — Web Feature Parity (**NEW**)
- **Scope:** Each subsequent phase (P2 expenses, P3 goals, P4 insurance, P5 retirement, P6 automation) gets a Web counterpart phase immediately following the Android phase.
- **Exit:** Web pages ship within 1 sprint of Android merge for each feature.

---

## 4. Standing debt payments (amortized across phases, not a phase)

Per-phase tax, enforced at each phase's pre-merge gate:
- String externalization (M3): every screen a phase touches converts to `strings.xml`. (Web uses `en.json`).
- Coverage ratchet (ADR-0013): floor bumps at every phase checkpoint; pure logic (calculators, schedulers, projectors) always TDD on both platforms.
- Oversized-file decomposition (M4): CalculatorScreen/ViewModel split when next touched.
- Design-system adoption: legacy screens adopt `:libs:core` components when next touched.
- **New-tracker-table registration:** any PR creating a tracker table must register it in DPDP paths, export registries, and trash surfaces. Both Android and Web must be updated.
- **New-module coverage registration:** every new Gradle module joins root `coveredModules`.
- **ADR-0024 (navigation architecture):** must land with R3 at the latest on Android.

---

## 5. Dependency graph (summary)

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

## 6. Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| R0 scope balloons (it's 20+ tickets) | Delays P1 momentum | R0 is strictly the PRODUCTION_READINESS list, no additions; run as ticket burn-down, criticals (C*, H*) first |
| New-gap phases (R3/R5b/R7/R8) built without specs | Violates playbook, quality drift | Each NEW item: brainstorm → spec → plan before code, same as P1–P6 |
| Supabase free-tier limits as data grows (valuations fetch-all = G20) | Slow Home | Already flagged for P6 revisit; reports (R7) must paginate/aggregate server-side from day one |
| Update-check (N4) semver vs CI auto-bump edge cases | False "update available" | Compare full MAJOR.MINOR.PATCH from release tag, ignore pre-release/drafts; unit-test comparator |
| App lock (N1) locks user out (biometric change) | Personal data inaccessible | Device-credential fallback mandatory; Supabase data recoverable via re-sign-in on any device |

## 7. Open questions

Questions 1–4 now have **proposed answers in their specs** (confirm or veto at each phase's
implementation start):

1. R3 app lock scope → proposed: whole app, default off until tracker sign-in (R3 spec D1/D2).
2. R5 accounts entity → proposed: accounts ARE assets, `account_ref → assets.id` (R5 decisions spec).
3. R5 multi-currency → proposed: INR-only with server check constraint; FX revisit bar recorded
   (R5 decisions spec).
4. R8 widget scope → proposed: read-only net worth + quick-add button, single 2×2 (R8 spec).

Still genuinely open:

5. R10: localization — go/no-go, which languages (N18; spec deliberately not written yet).
6. R11: pull Vault forward? (Personally valuable; independent of tracker phases.)
