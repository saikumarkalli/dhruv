# Dhruv Finance — Design v1.0 FINAL · Technical Implementation Plan (Solution Architecture)

> **Companion spec:** `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
> (functional requirements, flows, business rules). This document covers **how**: module topology,
> data architecture, schema, navigation rebuild, component work, phase order and per-phase DoD.
>
> **Companion process docs (binding on how §7 is executed):**
> `apps/finance/docs/superpowers/specs/2026-08-09-module-standard-and-tdd-process.md` (module shape, TDD/QA gate,
> engineering roles) and `apps/finance/docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md` (every
> scenario a module must satisfy, written before its code). **No phase's Backend/Android step below
> may start until that phase's QA catalog rows exist and are reviewed — see §7.0.**
>
> **Binding constraints:** `platform/PLATFORM.md`, `platform/DECISIONS.md` (ADR-0001…0028),
> `platform/AGENTS.md`. Nothing here redesigns an accepted decision except via the ADRs named in §2.

---

## 1. Architecture position

The final design does not change the platform architecture — it **fills it in**. Every piece has a
home in the existing shape:

```
apps/finance/app         shell · 5-tab pager · nested NavHosts · detail-route overlay · DI aggregation
apps/finance/data        ONE data module (ADR-0010 §3)
   ├── (existing) Room   calculator history · currency cache · Gemini        [offline, unchanged]
   └── (new) tracker/    Supabase REST: auth · repositories · DTOs · mappers [cloud-primary, ADR-0014]
apps/finance/feature/*   screens + view models only, Repository-only access to :data
libs/core                design system: tokens · components · FeatureHost · nav vocabulary
libs/settings            theme/accent/font/consent preferences
supabase/migrations      schema source of truth (RLS on every table)
web/                     same tokens, desktop expression (parallel track, not in these phases)
```

**Rules that shape every decision below** (ArchUnit-enforced):
`feature → feature` FORBIDDEN · `feature → data` via Repository only · `core → anything internal`
FORBIDDEN · every route in `FeatureHost` · flag entry per feature.

**Consequence:** anything two features share is either a **`:libs:core` component** (visual) or a
**`:apps:finance:data` repository** (data). There is no third option, and no feature module may
`import` another. The design's cross-tab jumps (Home quick action → Plan calculator, C7 prepay →
EMI calculator, B2 notification → E3 budget detail) all go through `NavTarget` +
`NavigationDispatcher`, never a direct call.

---

## 2. ADRs this work requires

| ADR | Title | Status |
|---|---|---|
| **ADR-0027** | Navigation: 5 tab roots (Home · Money · Calc · Plan · Insights); Plan root = live modules over calculators. **Supersedes ADR-0024 §1.** | **to write in Phase 0** |
| **ADR-0028** | Brand chrome as a second, theme-invariant palette (`DhruvBrandColors`) alongside `DhruvNextColors`. Extends ADR-0024 §2 (one global accent stays). | **to write in Phase 0** |
| **ADR-0029** | Tracker data architecture: Supabase REST on Retrofit/Moshi/OkHttp; latest-valuation views; append-only valuations; paise integers. Implements ADR-0014 §2/§4/§5/§6. | **to write in Phase 1** |
| ADR-0025 / ADR-0026 | Reserved for the CI-cost spec (see the numbering-hygiene note in `DECISIONS.md`). **Do not reuse.** | reserved |

Per `AGENTS.md`, ADRs are written into `platform/DECISIONS.md` **when the decision is implemented**,
not in advance.

---

## 3. Design-system work (`:libs:core`) — precedes every screen

### 3.1 Brand chrome palette (ADR-0028)

```kotlin
// libs/core/.../ui/theme/DhruvBrandColors.kt — theme-INVARIANT; does not flip with dark mode.
object DhruvBrand {
    val navy         = Color(0xFF0D1B2A)  // ground, splash, hero gradient end, launcher bg
    val navyElevated = Color(0xFF132B4D)  // hero gradient start, glass surface
    val blueMid      = Color(0xFF1E3A6D)  // stat card gradient, orbit ring
    val accentBlue   = Color(0xFF3FA7FF)  // info / links / positive-on-navy
    val silver       = Color(0xFFC0C6D1)
    val silverLight  = Color(0xFFE6E9EF)  // star fill on navy
    val steel        = Color(0xFF8E97A6)  // meta text on navy
    val logoBg       = Color(0xFFF4F6FA)
}
```
Consumers: `SplashScreen`, `DhruvGlassCard`, the net-worth hero card, the Settings identity card,
and the dark-hero screens (C3, D2, D7, E5, E9, F3, G3). Everything else stays on
`LocalDhruvNextColors`. **No screen may read a raw hex** — feedback rule `no-hardcoding`.

### 3.2 Components to build (order = dependency order)

**The batch list is owned by `platform/DESIGN-SYSTEM.md` §5.2/§5.3** (the component library is
cross-app, ADR-0030). Reproduced here with the Finance screens each batch blocks — if the two ever
disagree, the platform doc wins and this table is the bug.

| Batch | Components | Blocks (Finance) |
|---|---|---|
| **B1 — chrome** ✅ built | `NxFab`/`NxExtendedFab`, `NxTopBar` (title + single back, N2), `SkeletonBlock`, `SyncStatusChip` | every screen |
| **B5 — state** ✅ built | `SignedOutCard`, `OfflineStateCard`, `NotConfiguredCard` (NFR-4 trio) | every network screen |
| **B2 — input** | `NxCheckbox`, `NxRadio`, `PinEntry`, `AmountKeypadSheet` (D2), `QwertyKeypad`, `DateRangeSheet`, `EnumPickerGrid` (C4 sector) | A3, C4, C5, D2, D3, D5, F5 |
| **B3 — data viz** | `DonutChart` + `RankedLegend`, `PieChart`, `AmortisationDonut`, `PaceRing` (ring + month marker) | C1, C7, E2, F1 |
| **B4 — list** | `DayGroupHeader`, `LedgerRow`, `SuggestedRow` (dashed), `ReconcileBanner`, `SectorGroupHeader` | C2, C6, D1, D6, G2 |
| **B6 — form** | `NxSelect` (dropdown), `NxTextArea` (multi-line + helper), `InputChip` (removable) | C4, D3, D5, G1 |
| **B7 — feedback** | `StatusBadge` (success/warning/error/accent), `Spinner`, `InfoBanner` | D6, D7, G1, G2 |
| **B8 — navigation** | `NxTabs` (animated indicator — **not** `SegmentedRow`) | E-tab calculators, C3 range tabs |
| **B9 — overlays** | `SelectionSheet` (picker rows + ✓) | C4 sector, D3 category/account, currency picker |

**Also required — extend existing components** (`platform/DESIGN-SYSTEM.md` §5.3; extend, never add
a parallel component): `NxButton` needs sizes + loading + block treatment (every sheet's primary
action, D2/D3/C4 forms); `NxTextField` needs an error state + helper text (**every validated form
field** — C4, C5, D2, D3 all block on this); `CountBadge` needs status-dot variants; `Chip`/`Pill`
need a removable variant.

B6–B9 and the §5.3 extensions were added by the 2026-08-09 design reconciliation, which read the
Claude Design Component Library card-by-card; the earlier list came from a headings-only pass and
under-counted. `FinancialHealthRing` is **repurposed** into `PaceRing`'s base, not deleted
(spec §3 D-4).

### 3.3 Brand assets (design D-3)
- Launcher: regenerate `mipmap-anydpi-v26/ic_launcher.xml` with `background = #0D1B2A`,
  `foreground = orbit-star vector` inside the 66 % safe zone, `monochrome = 1a monoline`.
- Raster fallbacks for `mdpi…xxxhdpi` from the 1c tile.
- Notification icon = 1a monoline, single colour.
- Verify `SplashScreen` against the Launch doc's timeline (0.0 ground → 0.2 orbit draws 0.8 s →
  0.5 star overshoot 1.12× + sheen → 1.1 wordmark rises → 1.5 tagline + ring), easing
  `cubic-bezier(.16,1,.3,1)`, hard cap 2.5 s.

---

## 4. Navigation rebuild (ADR-0027)

### 4.1 Contract changes in `:libs:core/navigation`

```kotlin
enum class TabKey { HOME, MONEY, CALC, PLAN, INSIGHTS }   // MONEY inserted at index 1

sealed interface NavTarget {
    data class SelectTab(val tab: TabKey) : NavTarget
    data class OpenPlanTool(val tool: PlanTool) : NavTarget          // existing
    data class OpenPlanModule(val module: PlanModule) : NavTarget    // budgets/goals/debt/insurance/retirement
    data class OpenHolding(val id: String) : NavTarget
    data class OpenLiability(val id: String) : NavTarget
    data class OpenTransaction(val id: String) : NavTarget
    data class OpenAccount(val id: String) : NavTarget
    data class OpenBudget(val categoryId: String) : NavTarget
    data class OpenGoal(val id: String) : NavTarget
    data class OpenPolicy(val id: String) : NavTarget
    data class OpenReport(val kind: ReportKind) : NavTarget
}
```
`pageIndexFor` already resolves **by key, not position** — inserting `MONEY` is therefore safe for
flag-driven tab hiding (NAV4). `BottomNavItems` gains the Money entry.

### 4.2 Shell shape

- Pager over the **visible** tab list (flag-filtered), each page hosting its **own nested
  `NavHost`** — Home, Money, Plan and Insights all gain sub-routes; Calc keeps its single keypad.
- The existing `detailRoute` overlay (shell-level, no-tab routes: Settings/Ask/Currency/Unit/
  Date/Time/Profile/Notifications) is kept and extended with `Search`, `Automation`, `ReviewQueue`.
- Back contract (N2): `detailRoute` pops → active tab's nested back stack pops → pager returns to
  tab 0 → app exits. Already implemented for Plan; generalise to "current tab's controller".
- Sheets (N3) are `ModalBottomSheet` state inside the owning tab, never `NavHost` destinations —
  guarantees "dismiss down, never navigate".
- Full-screen modals (C4, D3, G3) are nested routes rendered with a **close ✕**, not a back arrow.
- Confirm-on-discard (N4): a shared `rememberDiscardGuard()` in `:libs:core` used by every form.

### 4.3 Route registry
Extend the route registry (`../specs/2026-08-09-finance-surface-registries.md` §1) with one row per new
route: `id · owner tab · parent · presentation (root|push|sheet|modal) · flag · requiresSession`.
The registry is the input to a **navigation ArchUnit/unit test** asserting N1/N2 (exactly one
parent, roots have none) — the rules become executable, not prose.

---

## 5. Tracker data architecture

### 5.1 Module placement
One shared module, two disjoint stacks (ADR-0010 §3 keeps a single `:apps:finance:data`):

```
apps/finance/data/src/main/java/com/dhruv/finance/data/
  ├── (existing) AppDatabase, HistoryDao/Repo, CurrencyDao/Repo, GeminiRepository   [Room, offline]
  └── tracker/
      ├── auth/        GoTrueApi · GoogleSignInClient wrapper · SessionStore (EncryptedDataStore)
      ├── net/         SupabaseClientFactory (OkHttp + CA pinning) · AuthInterceptor · ErrorMapper
      ├── dto/         *Dto (Moshi) — wire shape only
      ├── model/       domain models (paise Longs, enums)
      ├── mapper/      Dto ↔ model
      └── repo/        HoldingRepository · ValuationRepository · LiabilityRepository
                       TransactionRepository · AccountRepository · CategoryRepository
                       BudgetRepository · GoalRepository · PolicyRepository
                       RecurringRepository · InsightsRepository · SuggestionRepository
```

**Why not a separate `:apps:finance:tracker-data` module:** ArchUnit already restricts features to
Repository-only access, so a second module buys isolation the rules give for free, while forcing a
second Koin graph and a second Gradle dependency edge on every feature. Revisit only if the data
module's build time becomes a problem.

### 5.2 Networking (ADR-0014 §6 — no new unproven Gradle plugins)
- Retrofit + Moshi + OkHttp against GoTrue (`/auth/v1/*`) and PostgREST (`/rest/v1/*`).
- `CertificatePinner` pinned at **CA level (ISRG Root X1 + X2)** — never leaf.
- `AuthInterceptor` injects `apikey` + `Authorization: Bearer <access_token>`; a 401 triggers one
  refresh-token attempt, then a forced signed-out state.
- **Consent gate is an interceptor, not a screen concern**: a `ConsentInterceptor` fails the call
  fast if the relevant A3 switch is off, so no code path can accidentally bypass DPDP (NFR-1).
- `SUPABASE_URL` / `SUPABASE_ANON_KEY` / `GOOGLE_WEB_CLIENT_ID` ride the existing `.env` secrets
  plugin. `.env.example` stays committed with empty defaults so CI debug builds succeed.

### 5.3 Auth
Credential Manager → Google ID token → `POST /auth/v1/token?grant_type=id_token` → session.
Tokens persist **only** in encrypted DataStore. `SessionStore` exposes
`StateFlow<SessionState>` = `SignedOut | Active(user) | Expired`, which the shell maps to the
NFR-4 state trio on every network-backed screen.

### 5.4 Schema (`supabase/migrations/`) — RLS `user_id = auth.uid()` on **every** table

| Table | Key columns | Notes |
|---|---|---|
| `holdings` | `id · user_id · name · kind(ASSET\|LIABILITY) · sector TEXT · notes · created_at · deleted_at` | sector enum persisted as TEXT, **append-only constants** (BR-C3) |
| `valuations` | `id · holding_id · value_paise bigint · as_of date · source TEXT · created_at · deleted_at` | **append-only**; no UPDATE policy at all (BR-C1) |
| `liabilities_meta` | `holding_id · rate_bps · emi_paise · debit_day · tenure_months · paid_months · linked_account_id · collateral_holding_id` | 1:1 extension of a LIABILITY holding |
| `accounts` | `id · user_id · name · type(BANK\|CASH\|WALLET\|CREDIT_CARD) · mask · is_primary · limit_paise · due_day · opening_balance_paise · reconciled_at` | credit balances go negative (BR-D2) |
| `categories` | `id · user_id · name · kind(EXPENSE\|INCOME) · parent_id · icon · excluded_from_spend bool` | rename keeps id (BR-D3) |
| `transactions` | `id · user_id · type(EXPENSE\|INCOME\|TRANSFER) · amount_paise · account_id · to_account_id · category_id · payee · note · occurred_at · cleared · receipt_path · goal_id · recurring_id · source(MANUAL\|SMS\|RECURRING\|IMPORT)` | TRANSFER uses both account ids and is excluded from spend (BR-D1) |
| `transaction_events` | `id · transaction_id · at · kind · detail` | D4 audit trail (BR-D5) |
| `budgets` | `id · user_id · category_id · period(month) · amount_paise · alert_pct` | |
| `goals` | `id · user_id · name · target_paise · target_date · icon` | |
| `goal_links` | `goal_id · holding_id · earmark_qty numeric NULL` | progress derives from holdings (BR-E1) |
| `policies` | `id · user_id · kind(LIFE\|HEALTH\|OTHER) · insurer · policy_no · sum_assured_paise · premium_paise · frequency · renews_on · cover_until · nominee_*` | |
| `policy_premiums` | `id · policy_id · paid_on · amount_paise` | |
| `recurring_templates` | `id · user_id · template json · rrule · next_run · paused` | posts to review queue only (BR-D4) |
| `suggestions` | `id · user_id · raw_text · parsed json · status(PENDING\|ACCEPTED\|IGNORED) · created_at` | G2 |
| `automation_rules` | `id · user_id · match · category_id · applied_count` | G1 |
| `retirement_scenarios` | `id · user_id · name · assumptions json` | E9 |

**Views (server-side aggregation — keeps the client dumb, NFR-8):**
`v_latest_valuation`, `v_net_worth_by_sector`, `v_account_balances`, `v_month_summary`,
`v_category_spend`, `v_cashflow`, `v_pnl`, `v_balance_sheet`.
Charts read pre-aggregated rows; the client never sums a full ledger to draw a screen.

**Erasure:** `delete_my_account()` security-definer function (rows + `auth.users`), callable by the
signed-in user. No Edge Function, no service-role key near the device (ADR-0014 §7).

### 5.5 Feature-flag additions (`platform/feature-flags/dhruv-finance.json`)
```
money · budgets · goals · debtpayoff · insurance · retirement · insights · automation
```
each `{ enabled, minVersion, requiresConsent }`. `networth` already exists.
`automation` ships `enabled: false` until Phase 7.

**A3 consent switch → feature flag mapping (added 2026-08-09 — found undefined during doc
re-validation; `ConsentInterceptor` implements exactly this table, not a guess):**

| A3 switch | Gates these flags |
|---|---|
| Sync my financial records | `networth`, `money`, `budgets`, `goals`, `debtpayoff`, `insurance`, `retirement`, `insights` — every tracker flag with `requiresConsent: true` |
| Read transaction SMS | additionally required, on top of the row above, before `automation`'s SMS-source path (G1) may parse anything |
| Ask Dhruv about my money | the existing `assistant` flag's consent gate (unchanged from today's `AssistantScreen` behavior) |
| Data retention/erasure block | not a flag gate — governs "Delete my data"/"Delete my account" visibility only |

---

## 6. Feature-module topology (new)

| Gradle module | Owns | Screens |
|---|---|---|
| `:apps:finance:feature:onboarding` | sign-in, consent, empty start | A2, A3, A4 |
| `:apps:finance:feature:networth` | holdings, valuations, liabilities | C1–C7 |
| `:apps:finance:feature:money` | ledger, accounts, categories, recurring | D1–D9 |
| `:apps:finance:feature:planning` | budgets, goals, debt payoff | E2–E6 |
| `:apps:finance:feature:insurance` | policies | E7, E8 |
| `:apps:finance:feature:retirement` | projection | E9 |
| `:apps:finance:feature:insights` | statements, reports, export | F1–F5 |
| `:apps:finance:feature:automation` | sources, review queue, AA consent | G1–G3 |
| `:apps:finance:app` (existing) | Home 01, B2, B3, shell, Plan root E1 | shell-owned |

Each follows the existing template exactly (`feature/loans` is the reference):
`build.gradle.kts` with `dhruv.android.library` + `dhruv.android.compose`, deps on `:data`,
`:libs:core`, `:libs:settings`; `<Name>Screen.kt` + `<Name>ViewModel.kt` + `<Name>UiState` +
`<Name>Config.kt` (screen-level data/config — never hardcoded in the screen, per the
`no-hardcoding` rule) + `di/<Name>Module.kt` aggregated in `CalculatorApplication`.

**Planning is one module, not three** — budgets, goals and debt payoff share the same repositories
and cross-link (E3 → D1, E5 → C3, E6 → C6/C7); splitting them would force either a `feature →
feature` edge (forbidden) or triplicated view-model logic.

---

## 7. Phase plan

Each phase is independently shippable, ends green on `./gradlew regressionCheck`, and bumps the
minor version in `platform/versions.json`.

**Spec-kit tracking (added 2026-08-16, ADR-0031-adjacent doc migration).** As phases are formalized
into spec-kit `spec.md`/`plan.md`/`tasks.md`, the mapping is recorded here — check this table before
creating a new phase's spec-kit directory, not by scanning `apps/finance/specs/`.

> **Audit gate (2026-08-22).** A multi-agent review of all six generated spec-kit phases against
> this plan, the functional spec, the surface registries and `platform/DESIGN-SYSTEM.md` produced
> `../reviews/2026-08-22-spec-phase-gap-register.md`. Findings are folded into each phase's own
> `tasks.md` as a trailing **"Gap remediation"** block; the security and build blockers were
> corrected **in place** in the tasks a developer actually runs, not deferred to that block.
>
> **Nothing is safely implementable until two things land**, both in Phase 2 (001):
> 1. **`security_invoker = on` on every planned view** across 001/002/003. A Postgres 15+ view runs
>    as its owner and bypasses RLS on the underlying table; PostgREST exposes all 8 planned views,
>    so without the clause each returns **every user's rows to every signed-in caller**. Fixed in
>    001 T004, 002 T007, 003 T017/T019a.
> 2. **001's three build/merge blockers** — the missing `projectDir` remap (Gradle configuration
>    fails), the hand-written migration with no declarative twin (ADR-0032's PR equivalence guard
>    fails), and the missing ADR-0033 grants. Fixed in 001 T001/T004/T004a/T004b.
>
> Three Phase 2 product decisions (001 T045–T047 — cost basis, net-worth history source,
> `liabilities_meta`'s schema) blocked work in 001, 003 and 005. **Resolved (2026-09-01, 001
> Phase 9)** — all three landed in `data-model.md`; 003/005 may read the final shapes rather than
> treating them as pending.
> Cross-cutting items with no owner anywhere — **undo** (binding in DESIGN-SYSTEM §8, specified in
> zero phases), **Trash**, **Profile**, **Custom fields**, the **credit-card screens**, **`NxTabs`**,
> the **Glance widget** and three registry intents — are enumerated in the register's §1 and §4f.
>
> **Readiness pass (2026-08-23).** Those open questions are now answered in
> `../specs/2026-08-23-phase-readiness-architecture-decisions.md` — the SA decision record covering
> DB, Backend and UI for all six phases. Its §0 lists the **six product calls** taken as
> reversible defaults and flagged for the maintainer (cost basis · net-worth history · credit-card
> screens · custom fields · Trash · widget); everything else in it is technical and determined by an
> existing ADR.
>
> **Phase 2's DB layer is authored**, not just specified: `finance.liabilities_meta`, three
> `security_invoker` views (including the new `v_net_worth_history` that gives Home's delta a
> source), and the two RPCs that make FR-004's correction and FR-002's atomicity possible at all —
> `correct_valuation()` and `create_holding_with_value()`, the first of which ADR-0029 decision 4
> named and assigned to this phase two versions ago. Declarative files under
> `supabase/schemas/finance/`, migration `20260823094500_networth_phase2.sql`.
> **It has still not been executed** (re-confirmed 2026-09-03, 001 Phase 11, T078) — the CLI is now
> installed and the project is linked, but there is no `SUPABASE_ACCESS_TOKEN`/`supabase login`
> session and Docker is still absent, so neither `db push`/`diff --linked` nor a local `db reset`
> can run from an authoring session; its first run remains its first live verification. What *was*
> completed without execution: a line-by-line check of the migration against its own three named
> risk items (`security_invoker` on all three views, the `as_of` future-date guard, both RPC
> bodies) — all three confirmed correct by static review — and three ready-to-run manual SQL
> verification scripts for RLS-on-views and both RPCs' ownership/idempotency behaviour
> (`supabase/verification/`, 001 T081–T083), authored but equally blocked on the same missing
> credentials. The CI equivalence guard was **structurally unable to pass** for any table extended
> in place; `scripts/db/gen_schema_docs.py` now understands `ALTER TABLE … ADD COLUMN`/
> `ADD CONSTRAINT` and is green (001 T079, done 2026-08-23), along with its Windows console crash
> (T080). Full state and the maintenance conventions every later phase inherits:
> `../../../specs/001-net-worth-tracker/data-model.md` § "DB readiness".

| Phase | Spec-kit directory | Status |
|---|---|---|
| 0 — Shell foundation | — (shipped before spec-kit was installed; not retrofitted) | shipped |
| 1 — Identity & consent | — (shipped before spec-kit was installed; not retrofitted) | shipped |
| 2 — Net worth + real Home | [`apps/finance/specs/001-net-worth-tracker/`](../../../specs/001-net-worth-tracker/) | **shipped, 12 phases closed (2026-09-03)** — the original 8 (Setup, Foundational, US1–US5, Polish) plus four gap-remediation rounds (9–12), `regressionCheck` green throughout. C1–C7 + Home (01) all built; `liabilities_meta` CRUD, `v_net_worth_history`-backed hero delta/sparkline, and the amortisation/prepay math all shipped with unit-test coverage. Phase 8's QA-closure pass fixed two real defects beyond the task list: `NetWorthFeatureRoot` (C1–C7) unreachable until `DetailRoute.NetWorth` + a hoisted `netWorthNavController`, and two ViewModels never reloading on return (`NW-FLOW-001`/`NW-FLOW-002`, fixed via `LifecycleResumeEffect`). Phase 9 closed a multi-agent spec audit: holding soft-delete + undo, C4 edit mode, cost-basis/notes capture, C2/C6 signed-out/consent gating (the gap this table previously carried as open), and this module's `SettingsContribution`. Phase 10 closed a UI/UX + requirements audit: full `strings.xml` extraction, chart `contentDescription`s, a new `checkDesignTokenUsage` Gradle gate, and several design-fidelity fixes (C3's header, C1's legend tag, Home's status line) — most of that audit's other findings (the two RPCs, field-rule CHECKs, post-write invalidation) turned out to already exist and only needed a citation, the same doc-drift pattern found repeatedly across this spec's history. Phase 11 closed DB readiness as far as it can go without live credentials: static confirmation the migration matches its own risk checklist, and three ready-to-run RLS/RPC verification scripts (`supabase/verification/`). Phase 12 (this row) closed FEATURES.md/README/CHANGELOG/this table. **Still open, not silently dropped**: `NW-BR-007` (XIRR) blocked on its own ADR; no edit-liability screen; the migration and the new verification scripts have never run against live `dhruv-dev` — no credentials in any authoring session yet, see `data-model.md` § "DB readiness". Every other phase depends on this one |
| 3 — Money tab | [`apps/finance/specs/002-money-tab/`](../../../specs/002-money-tab/) | spec + plan + research + data-model + routes + quickstart + tasks (83, T001–T083) done, ready for `/speckit-implement` |
| 4 — Plan live modules | [`apps/finance/specs/003-plan-live-modules/`](../../../specs/003-plan-live-modules/) | spec + plan + research + data-model + routes + quickstart + tasks (134, T001-T134) done (2026-08-19), ready for `/speckit-implement` |
| 5 — Insights | [`apps/finance/specs/005-insights/`](../../../specs/005-insights/) | spec + requirements checklist done (2026-08-21); clarified 2026-08-22 (4 Q&A, checklist 24/24). Scope settled: one period model across the whole tab, balance sheet carries an overridable date, F5 ships the XIRR and tax-summary reports, monthly-summary alert stores its preference here and is **delivered** in Phase 6 (the 003 precedent). plan + research + data-model + 2 contracts + quickstart done 2026-08-22. **Split into 6 sub-phases 5a–5f** (foundation+F1 · cashflow · balance sheet · P&L · reports+export · gated "More" reports), each independently shippable and green on `regressionCheck`. tasks (175, T001–T175) done 2026-08-22. **Ready for `/speckit-implement` on 5a–5e (T001–T146); 5f (T147–T163) blocked** on an accepted decision record fixing the investment-returns calculation (functional spec open item §8.6) — research R8 lists what it must settle, and T147 is writing it. Also depends on Phases 2 and 3 shipping first. **Four corrections to this plan** to fold into §5.4/§6 on implementation: reporting is parameterised `finance` functions, not the `v_cashflow`/`v_pnl`/`v_balance_sheet` views §5.4 names (a view cannot take a period); as-at position derives from "latest valuation ≤ date"; net worth (holdings) and cashflow balances (accounts) are two different money universes and are not required to agree; tax relevance is a user-set `categories.tax_section` column |
| 6 — Search & notifications | [`apps/finance/specs/006-search-notifications/`](../../../specs/006-search-notifications/) | spec + requirements checklist done (2026-08-22), clarified in the same session (2 Q&A, checklist 24/24). Scope settled: B3 global search over transactions/holdings/policies/goals, B2 notification centre as the app's **own durable record** (an alert is recorded whether or not the OS displayed it), and delivery of the **five ready alert types** — budget threshold, instalment due, policy renewal, value-update overdue, monthly summary — each driven by the preference an earlier phase already stored. Transactions-to-review is **not** delivered here (its review queue is Phase 7's G2); `daily_rates`/`app_updates` stay with the currency and app-details modules. Depends on Phases 2–5 shipping. plan + research (R1–R10) + data-model + 3 contracts + quickstart done 2026-08-22. **Split into 6 sub-phases 6a–6f** (search · notification centre · pipeline+budget arm · obligation arms · periodic arms · controls/masking/closure), each independently shippable and green on `regressionCheck`. **Three corrections to this plan** to fold in on implementation: §5.5's flag list gains `search` and `alerts` (neither was reserved); §6 correctly assigns B2/B3 to `:apps:finance:app`, so this phase adds **no** Gradle module; and the DB footprint is **two functions, zero tables** — the alert log is device-local Room (v5→v6), not Supabase, so `delete_my_data()` is untouched. Adds one dependency (`androidx.work`, already named in PLATFORM.md §3/§5) and **zero** new `NavTarget` cases — it is the first consumer of five existing ones, including the `OpenBudget` Phase 4 added speculatively for it. Next: `/speckit-tasks` |
| 7 — Automation (G1–G3) | [`apps/finance/specs/007-automation/`](../../../specs/007-automation/) | spec + requirements checklist done (2026-08-23), clarified in the same session (**7 Q&A**, checklist 24/24). Created **early, before Phase 3's checkpoint**, per the readiness record §5.4 — Phase 3 (D9) defers the shared review queue here and Phase 6 defers transactions-to-review here, so G2 is a live dependency of two shipping phases, not a future nicety. Scope settled: G2 as the one queue every source feeds, G1 hub with per-source scope statements and learned rules, on-device bank-SMS parsing behind its own consent class, duplicate detection, the entries-waiting alert (discharging 006's deferral), and G3's consent statement. **Clarification 1 — the price feed proposes, it does not write:** a fetched gold/silver/currency price arrives as a **proposed value update in the review queue**, so G1's "every source only suggests" header is true without exception; BR-G1 only forbids automated writes to the *ledger*, so a direct valuation write would have been legal but would have made the hub's own promise false. Consequence: **the queue carries two proposal kinds** (proposed transaction · proposed value update). **Clarification 2 — CSV import is out of scope**; A4's CTA stays present and disabled with copy naming that it arrives later, since the functional spec's open item records the column-mapping step has no design at all. **Three corrections folded in:** the proposal store is owned by Phase 3 (002 `plan.md:38`) — this row's former "`suggestions`, `automation_rules` schema" would have re-created a shipped table, and only `automation_rules` is new; **Trash / Recently deleted is NOT this phase** (readiness §5.2 moved it to Phase 0b), so surface registry §4's Automation line listing it is stale and is corrected at this phase's closure; the **`REVIEW_INBOX`** intent is this phase's, its destination being G2. **Five further clarifications shape the build and are not in the design:** ignored proposals go to a restorable **Ignored** list and are never re-proposed; withdrawing SMS consent **freezes** that source's unreviewed proposals (read-only, delete-all or re-enable) rather than purging them; messages are read on a **periodic background scan** (~1h, best-effort) — open-only reading was rejected because it would make the entries-waiting alert unable to announce anything; **accept-all takes confidently-parsed transactions with no duplicate warning only**, reporting what it skipped and why; and the price feed proposes **on a move past a user-set threshold (default 5%)**, not on a clock. Terminology: the spec says **proposal** where the design and catalog say *suggestion* — same entity, mapped in its Key Entities note. plan + research (R1–R16) + data-model + 4 contracts + quickstart done 2026-08-23. **Split into 7 sub-phases 7a–7g** (queue+Ignored · hub+G3+module · SMS source · duplicates · rules · price feed · alert+erasure+closure), each independently shippable and green on `regressionCheck`. **Two further corrections found while planning, both verified against code:** the `automation` flag key is **genuinely absent** from `platform/feature-flags/dhruv-finance.json` (§5.5 reserved the name but it was never applied — this phase applies it, `enabled: false`); and `NavTarget` today has only `SelectTab`/`OpenPlanTool`, so this phase adds **zero** cases — G1–G3 are shell detail routes like Settings/Ask, per that sealed type's own doc comment. **One shipped-document contradiction resolved:** Phase 3 reserved `finance.suggestions.raw_text` for this phase's SMS source, but `AUT-BR-002` forbids raw message text in any outbound Supabase payload — resolved by keeping bank-message proposals **entirely device-local** (Room v6→v7), leaving that column permanently unwritten. DB footprint: **one table** (`finance.automation_rules`, deliberately mutable, not append-only), one `security invoker` function, one `holdings` column, `delete_my_data()` extended. **Zero new dependencies** (SMS via platform `ContentResolver`; scheduling reuses Phase 6's `androidx.work`). Adds **one** Gradle module, `:apps:finance:feature:automation`. Depends on Phases 1–6. Next: `/speckit-tasks` |
| 0b — Settings control plane (shell foundation, **runs before Phase 2**) | [`apps/finance/specs/004-settings/`](../../../specs/004-settings/) | **All five sub-phases shipped (0b.1–0b.5, 2026-08-27–2026-08-29)** — spec + clarifications + plan + research + data-model + 2 contracts + quickstart + QA catalog §13 `SET-*` (50 rows, 42 ✅ / 2 🔴 deferred / 6 ☐ remain for later phases) + surface registry §4 + tasks, all closed. 0b.1 control plane + Appearance; 0b.3 and 0b.4 landed ahead of 0b.2 (both only depend on 0b.1); 0b.2 the real Account screen (sign-in/sign-out/consent/erasure, type-to-confirm); 0b.5 verification — removed 9 orphaned preference keys (`SET-BR-009`/SC-005 audit), coverage floor raised 0.09→0.14. **Two post-implementation passes followed (2026-08-29)** and are part of the shipped state: a five-axis SA/QA review (2 Critical — a conditionally-invoked `@Composable` corrupting the slot table, and a `ViewModel` built with `remember{}` so its scope never cancelled — plus 6 Required, incl. deduplicating Google sign-in nonce generation into `:libs:core`), then an edge-case pass against this spec's own Edge Cases list that found **FR-032 had no consumer at all** (turning a module off changed nothing outside its own settings screen — an SC-011 violation the 0b.5 inert-row audit missed) and an FR-033 hazard (the on/off control was offered for `calculator`, the Calc tab's own content). Both fixed; `SettingsContribution.optional` now gates it. Deferred to a device-equipped session: SC-001's on-device migration check, theming/text-size and TalkBack passes (T109/T113/T114). **Four consumers 0b cannot own are registered in the readiness record §5.5** — `daily_rates` delivery (recommended Phase 6), BYO AI-key consumption (the Ask Dhruv work), launcher-entry hiding, and `font_family`'s missing row (a DESIGN-SYSTEM call). See `apps/finance/specs/004-settings/spec.md`'s Implementation record for the full sub-phase-by-sub-phase account. Carries the three-tier control plane (Account · App · Modules), the mechanism modules use to declare their own settings entry, the app-wide lock checkpoint, and three shipped-surface defect fixes. **Every later phase ships its module's settings entry with the module** — enabling the module enables its entry; no phase edits a central list |

### 7.0 How every phase is executed (binding — see the module-standard doc §4 for the full process)

Every phase below runs this fixed step order; no phase may start step 3/4 before steps 1–2 for
that phase are done. Roles: **SA** (solution architect) · **QA** · **Backend** (data/Supabase
engineer) · **Android** · **Web** (parallel track) · **Sec** (security/DPDP reviewer).

```
1. SA      finalizes the phase's schema/migration + NavTarget/route-registry additions (design)
2. QA      writes/reviews that phase's rows in the QA Test Scenario Catalog — "every scenario"
           check happens HERE, against the functional spec, before any code
3. Backend RED  → failing repository/mapper/interceptor tests citing scenario IDs
           GREEN → minimal implementation
           REFACTOR
4. Android RED  → failing ViewModel/screen-state tests citing scenario IDs (may start against
           Backend's Fake before the real repository lands — vertical slice, not blocked)
           GREEN → minimal implementation
           REFACTOR
5. QA      executes Automatable:N rows manually, verifies Automatable:Y via `regressionCheck`,
           closes catalog rows (✅), updates the catalog's coverage-summary table
6. Sec     DPDP/secrets/RLS checklist pass at the checkpoint (skipped only if the phase touches
           no off-device data)
7. Checkpoint — merge gate: regressionCheck green, coverage floor not regressed, every phase
           scenario row CLOSED or explicitly deferred with a stated reason
```

Each phase heading below lists its **catalog module codes** (from
`2026-08-09-qa-test-scenario-catalog.md`) and a role table instead of a flat numbered list, so
"every phase plans all engineers" is explicit rather than implied.

### Phase 0 — Design-system + shell foundation *(no backend, no new screens)*
**Catalog modules:** `NAV` (11 rows).

| Step | Role | Work |
|---|---|---|
| 1 | SA | `TabKey.MONEY` contract, route-registry rows for the 5-tab shell, ADR-0027/ADR-0028 (both written) |
| 2 | QA | `NAV-*` rows reviewed against functional spec §4 (N1–N7) — **done**, see catalog §1 |
| 3 | Backend | none this phase (no backend surface) |
| 4 | Android | `DhruvBrand` colors; component batches **B1**/**B5**; 5-tab pager + bottom bar; `NotConfiguredCard` on Money/Insights; `resolveBackAction`/`BackContractTest` (N2, extracted pure decision fn — no Compose-UI-test spike needed for this part); icon regeneration from direction 1c; splash verified against the Launch doc timeline |
| 5 | QA | close `NAV-ARCH-*`/`NAV-UI-*` rows as their tests land; NAV-ARCH-003/NAV-FLOW-003/NAV-UI-004 already 🟢 |
| 6 | Sec | skipped — no off-device data in this phase |
| 7 | Checkpoint | 5 tabs live, dark/light identical to design, ArchUnit + nav tests green, no regression in Calc/Plan |

**Status: done**, except two items **descoped to the phase that first needs them** (not dropped —
building them now with zero consumers would be the premature abstraction the project's own
`no-hardcoding`/altitude rules warn against):
- **Nested-`NavHost` generalisation beyond Plan** → moves to **Phase 3**, when Money (D1–D9) is the
  first tab besides Plan to actually need sub-routes. Today only Plan has drill-ins; generalising
  the pattern with no second consumer is speculative.
- **`rememberDiscardGuard`** (N4, confirm-on-discard for forms) → moves to **Phase 2**, when C4 (add
  holding) is the first form that needs it. No form exists yet to guard.

Built this session: `TabKey.MONEY`, 5-tab pager, `DhruvBrand`, B1/B5 components, ADR-0027/0028,
route registry corrected and later extracted to `../specs/2026-08-09-finance-surface-registries.md` §1, `BackContract.kt` +
`BackContractTest` (N2), launcher/monochrome/notification icons regenerated to direction
1a/1c and the dead per-density launcher rasters removed (minSdk 26 makes
`mipmap-anydpi-v26` always win — confirmed against `AndroidManifest.xml`'s icon references before
deleting). Splash-vs-Launch-doc-timeline verification is a motion/feel check
(NFR-7, "Partial" automatability per the QA catalog) — not re-confirmed this session, left as a
manual QA pass whenever Phase 0 is walked end-to-end on a device/emulator.

### Phase 1 — Identity & consent *(A2, A3, A4)*
**Catalog modules:** `ONB` (13 rows), `DAT` (9 rows, foundational — auth/consent plumbing all of
Phase 1+ depends on).

**Blocking pre-step (found by the 2026-08-09 doc re-validation pass — R6 corrected):** the R6 risk
below originally said "resolve multi-currency scope before Phase 3," but `holdings`/`valuations`
(step 1 of *this* phase) already commit to a currency-less `value_paise bigint` shape. Resolving
multi-currency scope must happen **before this phase's migration is authored**, not before Phase 3
— by Phase 3 the schema decision is already live against RLS-protected user data and reversing it
means a breaking migration. SA owns closing this before step 1 below starts.

| Step | Role | Work |
|---|---|---|
| 0 | SA | **Blocking:** resolve functional-spec open item §8.5 (multi-currency scope) — confirm `holdings`/`valuations` are INR-only by design before authoring any schema |
| 1 | SA | `supabase/migrations/0001_init.sql` (`holdings`, `valuations`, RLS), `tracker/net`+`tracker/auth` contracts, ADR-0029 |
| 2 | QA | write/review `ONB-*` and `DAT-*` rows — **done**, see catalog §2/§11 |
| 3 | Backend | RED: `ConsentInterceptorTest`, `AuthInterceptorTest` (MockWebServer), `SessionStoreTest` citing `DAT-BR-001..005` → GREEN → REFACTOR. Also: build the `checkTrackerMoneyPrecision` Gradle task (§8) now, while `tracker/` first exists, and extend `DependencyRulesTest`'s DAO-only check to also match `.*Dto` once `tracker/dto/` exists (closes the module-standard doc's §1.1 correction) |
| 4 | Android | RED: `OnboardingViewModelTest` for A2/A3/A4 state transitions citing `ONB-*` → GREEN (`:feature:onboarding`, Settings→Privacy) → REFACTOR |
| 5 | QA | manual pass on `ONB-BR-008/009` and `DAT-BR-006/007` (dev-project only), close remaining rows |
| 6 | Sec | consent-before-network audit (`dhruv-security-compliance-reviewer`), GitLeaks on new secrets plumbing |
| 7 | Checkpoint | cold install → Google sign-in → consent → empty start; declining sync leaves calculators usable; zero network before consent; `delete_my_account()` verified against a dev project; `checkTrackerMoneyPrecision` and the DTO ArchUnit guard both green |

### Phase 2 — Net worth + real Home *(C1–C7, 01)*
**Catalog modules:** `NW` (§3, 14 rows — corrected count, see the §14 coverage-summary note), `HOM`
(§12, new — added in the 2026-08-09 re-validation pass).

**Scoped dependency (found by re-validation):** Home's (01) "UPCOMING" list shows both a loan-EMI
row and a credit-card-bill row in the functional spec, but the credit-card-bill row needs
`accounts.due_day`, which doesn't exist until Phase 3. **Phase 2 ships UPCOMING with loan/EMI items
only** (sourced from this phase's own `liabilities_meta.debit_day`); the credit-card-bill row is
added as an explicit Phase 3 follow-up task (below), not silently missing.

| Step | Role | Work |
|---|---|---|
| 1 | SA | migration: `liabilities_meta`, `v_latest_valuation`, `v_net_worth_by_sector` |
| 2 | QA | write/review `NW-*` and `HOM-*` rows — **done**, see catalog §3/§12 (NW-BR-007 flagged blocked on the XIRR ADR) |
| 3 | Backend | RED: `HoldingRepositoryTest`/`ValuationRepositoryTest`/`LiabilityRepositoryTest` against Fakes, citing `NW-BR-001..006` → GREEN → REFACTOR |
| 4 | Android | RED: component batches **B3** (charts for C1/C7) + **B9** (`SelectionSheet` for C4's sector picker) + **B6**'s `NxSelect`, and the §5.3 `NxTextField` error/helper extension (C4/C5 are the first validated forms — they cannot ship a field error without it) + `NxButton` sizes/loading; then `:feature:networth` C1–C7 tests citing `NW-UI-*`/`NW-FLOW-*`, plus `HOM-*` tests for the real Home screen → GREEN → REFACTOR; replace `DashboardScreen` with real 01 Home (EMI-only UPCOMING, see scoped-dependency note above) |
| 5 | QA | close rows; verify append-only holds at both the repository AND SQL layer (no UPDATE policy) |
| 6 | Sec | RLS check on the two new tables + two new views |
| 7 | Checkpoint | add holding → first valuation written; update value → new row, chart + XIRR recompute; Home total matches C1; every screen has signed-out/offline states |

### Phase 3 — Money tab *(D1–D9)*
**Catalog modules:** `MNY` (now 20 rows after the re-validation pass added D4/D8/D9 coverage — see
catalog §4/§14).

**Scoped dependencies (found by re-validation):** (a) this phase's Backend step 1 also delivers
Phase 2's deferred credit-card-bill row on Home (accounts now exists). (b) D4's "budget impact"
field (`Groceries · 68% used`) needs the `budgets` table, which doesn't exist until Phase 4. **D4
ships in Phase 3 without the budget-impact row**; Phase 4 adds it back as an explicit follow-up
task (below) once `budgets` exists — not silently missing.

**Table-list correction (found 2026-08-16 while writing `apps/finance/specs/002-money-tab/`):**
step 1's table list below omits **`recurring_templates`** and **`suggestions`**, but this phase owns
screen **D9 (Recurring)** and QA rows `MNY-BR-005` ("a `suggestions` row is created, not a
`transactions` row") and `MNY-FLOW-002` ("a `recurring_templates` row is created") — neither can
pass without both tables. Both are already specified in §5.4; only this row's enumeration was short.
Phase 7 still owns the SMS/AA *sources* and the shared review-queue screen (G1–G3); Phase 3's review
surface is recurring-only. See `apps/finance/specs/002-money-tab/research.md` R1 and R7 (recurring
occurrences are materialised client-side on open, idempotent on `(recurring_id, due_on)` — there is
no server scheduler).

| Step | Role | Work |
|---|---|---|
| 1 | SA | migration: `accounts`, `categories`, `transactions`, `transaction_events`, `v_account_balances`, `v_month_summary`, `v_category_spend` |
| 2 | QA | write/review `MNY-*` rows (now includes D4/D8/D9 coverage) — **done**, see catalog §4 |
| 3 | Backend | RED: transfer-exclusion (`MNY-BR-001`), spendable-now (`MNY-BR-002`), audit-trail (`MNY-BR-006`) tests **before any UI** → GREEN → REFACTOR; wire Home's deferred credit-card-bill UPCOMING row (Phase 2 note) now that `accounts` exists |
| 4 | Android | RED: component batches **B4** (ledger rows) + **B6** (`NxTextArea`, `InputChip` for D3/D5) + **B7** (`StatusBadge`/`InfoBanner` for D6/D7 reconciliation state) + `AmountKeypadSheet`/`DateRangeSheet`; then `:feature:money` D1–D9 tests citing `MNY-UI-*`/`MNY-FLOW-*` (incl. new D4/D8/D9 rows) → GREEN → REFACTOR; D4 ships without budget-impact (scoped-dependency note above) |
| 5 | QA | manual 3-tap timing check (`MNY-UI-001`), close rows |
| 6 | Sec | no new off-device data class beyond Phase 1's scope — light pass |
| 7 | Checkpoint | 3-tap quick add; transfers absent from every spend total; audit trail on every mutation; reconciliation flow on D7; Home's UPCOMING now shows both EMI and card-bill rows |

### Phase 4 — Plan live modules *(E1 revised, E2–E9)*
**Catalog modules:** `PLN` (12 rows), `INS` (4 rows), `RET` (4 rows).

| Step | Role | Work |
|---|---|---|
| 1 | SA | migration: `budgets`, `goals`, `goal_links`, `policies`, `policy_premiums`, `retirement_scenarios` |
| 2 | QA | write/review `PLN-*`/`INS-*`/`RET-*` rows — **done**, see catalog §5–§7 |
| 3 | Backend | RED: budget-pace/avalanche-snowball/retirement-projection **engines as pure-Kotlin tests first** (`PLN-BR-*`, `RET-BR-001`) — correctness-critical like the existing calculators → GREEN → REFACTOR |
| 4 | Android | RED: `PaceRing` + `:feature:planning`/`:feature:insurance`/`:feature:retirement` tests citing `*-UI-*`/`*-FLOW-*` → GREEN → REFACTOR; rewrite Plan root to E1 (shell-owned — see the module-standard doc's `HOM`/`PLN` correction: E1 lives in `:apps:finance:app`, not `:feature:planning`); backfill D4's deferred budget-impact row (Phase 3 note) now that `budgets` exists |
| 5 | QA | close rows |
| 6 | Sec | RLS on 6 new tables |
| 7 | Checkpoint | goal progress derives purely from linked holdings; avalanche vs snowball stated numerically; E9 assumptions on the same screen as the answer; D4 now shows budget impact |

### Phase 5 — Insights *(F1–F5)*
**Catalog modules:** `SIG` (7 rows).

| Step | Role | Work |
|---|---|---|
| 1 | SA | views `v_cashflow`, `v_pnl`, `v_balance_sheet`; Indian FY period resolution |
| 2 | QA | write/review `SIG-*` rows — **done**, see catalog §8 |
| 3 | Backend | RED: cashflow/P&L/balance-sheet reconciliation tests (`SIG-BR-001..003`) → GREEN → REFACTOR |
| 4 | Android | RED: `:feature:insights` F1–F5 + CSV/PDF export tests citing `SIG-UI-*`/`SIG-FLOW-*` → GREEN → REFACTOR |
| 5 | QA | export round-trip check, close rows |
| 6 | Sec | export path — confirm no data leaves device except the user-triggered file save |
| 7 | Checkpoint | statements reconcile on screen; export round-trips; every report readable before export |

### Phase 6 — Search & notifications *(B2, B3)*
**Catalog modules:** `SRC` (5 rows).

| Step | Role | Work |
|---|---|---|
| 1 | SA | cross-entity search repository contract |
| 2 | QA | write/review `SRC-*` rows — **done**, see catalog §10 |
| 3 | Backend | RED: search-repository tests citing `SRC-UI-001` → GREEN → REFACTOR |
| 4 | Android | RED: B2/B3 screens + deep-link dispatch tests citing `SRC-FLOW-*` → GREEN → REFACTOR |
| 5 | QA | close rows |
| 6 | Sec | none new |
| 7 | Checkpoint | search result counts correct; every result and notification type deep-links to its subject |

### Phase 7 — Automation *(G1–G3)*
**Catalog modules:** `AUT` (8 rows).

| Step | Role | Work |
|---|---|---|
| 1 | SA | `automation_rules` schema **only** — `suggestions` is Phase 3's and already exists (see the tracking-table row above); SMS-permission + AA-consent design; the review queue's second proposal kind (proposed value update, from the price feed) |
| 2 | QA | write/review `AUT-*` rows — **done**, see catalog §9 |
| 3 | Backend | RED: on-device SMS parsing (no raw SMS leaves device, `AUT-BR-002`), suggestion-only writes (`AUT-BR-001`) → GREEN → REFACTOR |
| 4 | Android | RED: G1–G3 review-queue UI + duplicate detection tests citing `AUT-UI-*`/`AUT-FLOW-*` → GREEN → REFACTOR |
| 5 | QA | close rows |
| 6 | Sec | full DPDP pass — this phase requests SMS/AA permissions; flag stays `enabled: false` until this checkpoint passes |
| 7 | Checkpoint | no automated source ever writes directly to the ledger **or changes a recorded valuation**; review queue + duplicate detection work end-to-end; surface registry §4's stale *Recently deleted* line under Automation corrected (that surface is Phase 0b's) |

### Parallel · Web
`web/` already mirrors the tokens and shares the schema. Web work follows Android **one phase
behind**, schema-sequenced (see the `web-android-parallel-dev` decision): a phase's migration lands
first, both clients then consume it. Web has no separate QA catalog — it re-uses the same
`<MODULE>-BR-*`/`<MODULE>-FLOW-*` rows (business rules and flows are platform-agnostic); only
`*-UI-*` rows are Android-Compose-specific and get a lightweight React-equivalent added by the Web
engineer when that phase's web track starts.

---

## 8. Cross-cutting engineering rules for this build

| Rule | Enforcement |
|---|---|
| No hardcoded dp/sp/colour in feature screens; screen-level data lives in `<Name>Config.kt` | detekt + review (`no-hardcoding` feedback) |
| No `feature → feature` import | ArchUnit |
| Every route wrapped in `FeatureHost` + flag entry | ArchUnit + route-registry test |
| Money is `Long` paise in every tracker path | a `checkTrackerMoneyPrecision` Gradle task (regex scan of `apps/finance/data/src/main/.../tracker/**/*.kt` for `Double`/`Float`, added to `regressionCheck`) — scheduled as a Phase 1 SA/Backend task, §7. A full custom-detekt `RuleSetProvider` module was considered and rejected as disproportionate infra for one banned-type check; the regex task is reused unchanged if a real rule set is ever justified later. |
| No network call before its consent switch | `ConsentInterceptor` + unit test |
| Every network screen has signed-out/offline/not-configured states | screenshot/Robolectric state tests |
| Coverage floor ratchets at each phase checkpoint | `./gradlew regressionCheck` |
| Enum constants persisted as TEXT are append-only | migration review checklist |

## 9. Risk register

| # | Risk | Mitigation |
|---|---|---|
| R1 | Scope: 47 missing screens is multi-month solo work | Phases 0–2 deliver a genuinely useful app (net worth tracking); 3–7 are additive and independently shippable |
| R2 | Supabase free-tier limits / RLS mistakes leak data | RLS policy test per table in CI against a dev project; no service-role key on device |
| R3 | Nav rebuild regresses the working calculators | Phase 0 ships nav-only with zero feature changes; nav tests + existing calculator tests gate it |
| R4 | Aggregation on device kills list performance | All chart/statement data comes from SQL views, never client-side reduction over the ledger |
| R5 | ADR-0024 was accepted eight months ago and the shell was just rebuilt to it | ADR-0027 supersedes only §1 (tab set); §2's single global accent and the retirement of `SectionTheme` are unchanged and keep their value |
| R6 | Design is INR-only; roadmap phase R5's multi-currency spec assumes otherwise | **Corrected 2026-08-09** (was "before Phase 3" — wrong, `holdings`/`valuations` commit to a currency-less shape in Phase 1's migration). Resolve open item §8.5 of the functional spec **before Phase 1 step 1** (`0001_init.sql`) — see Phase 1's blocking pre-step, §7 |
| R7 | SMS permission risks a Play policy problem later | Automation is last, flag-off, and gated behind its own consent + Data Safety entry (Play is deferred anyway, ADR-0008) |

## 10. Immediate next actions

1. Confirm **D-1** (5 tabs) — the only decision that changes Phase 0's shape.
2. Start **Phase 0**: `DhruvBrandColors`, component batch B1/B5, `TabKey.MONEY`, nested NavHosts,
   route registry + nav tests, icon regeneration, ADR-0027 + ADR-0028.
3. Open the two blocking spec questions in parallel (multi-currency scope; CSV import mapper design)
   so they are answered before Phase 3.
