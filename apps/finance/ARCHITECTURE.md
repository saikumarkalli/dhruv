# Dhruv Finance — Architecture

Onboarding reference for the `:apps:finance` codebase. Written for someone who has cloned the repo
and needs to know **where things are, why they are there, and what will bite them.**

**Scope boundary.** This describes *how the app is built*. It is not the product spec (that is
[the functional spec](docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md), 61
screens and business rules), not the design contract (that is
[`platform/DESIGN-SYSTEM.md`](../../platform/DESIGN-SYSTEM.md)), and not the decision record (that is
[`platform/DECISIONS.md`](../../platform/DECISIONS.md)). Where this file and one of those disagree,
they win and this file is a bug — fix it here rather than forking a second set of facts.

Per-module detail lives in each module's own `README.md`, indexed by [FEATURES.md](FEATURES.md).
Verified against the source tree on 2026-08-23.

---

## 1. The one-paragraph version

Single-activity Jetpack Compose app. A pager holds five tab roots, each hosting its own nested
navigation. Every screen is wrapped in a fault-isolation boundary so a feature crash can never take
down the shell. Dependency injection is Koin. Calculator data lives in a local Room database and
works offline with no account; **all tracker data lives in Supabase and does not exist locally at
all**. Feature modules never talk to each other, never see a DAO, and never contain styling.

---

## 2. Module graph

```
                    ┌──────────────────────────────┐
                    │   :apps:finance:app          │  shell, MainActivity, Koin aggregation,
                    │   (the only module that       │  shell-owned screens, ArchUnit tests
                    │    knows every other one)     │
                    └──────────────────────────────┘
                       │            │            │
            ┌──────────┘            │            └──────────────┐
            ▼                       ▼                           ▼
  ┌───────────────────┐   ┌───────────────────┐      ┌────────────────────┐
  │ :feature:<name>   │──▶│ :apps:finance:data│      │  :libs:settings    │
  │  (11 modules)     │   │  Room + Supabase  │      │  EncryptedDataStore│
  └───────────────────┘   └───────────────────┘      └────────────────────┘
            │                       │                           │
            └───────────────────────┴───────────┬───────────────┘
                                                ▼
                                    ┌────────────────────────┐
                                    │      :libs:core        │  design system, FeatureHost,
                                    │  (depends on nothing   │  NavTarget, observability,
                                    │   internal)            │  security, formatting
                                    └────────────────────────┘
```

### The rules, and what enforces them

| Rule | Status | Enforced by |
|---|---|---|
| `feature → feature` | **forbidden** | `DependencyRulesTest` (ArchUnit) |
| `feature → data` | allowed, **Repository only** — never a DAO or DTO | `DependencyRulesTest` |
| `feature → core` | allowed | — |
| `core → anything internal` | **forbidden** — `core` is a pure library | `DependencyRulesTest` |
| feature screen → another feature's screen | **forbidden** | `DependencyRulesTest` |

These are not style preferences. `DependencyRulesTest` (`apps/finance/app/src/test/kotlin/.../arch/`)
fails the build, and it runs in `regressionCheck`, which is the pre-merge gate.

**Why it matters practically:** if Loans needs to open the EMI calculator, it cannot import it. It
emits a `NavTarget` and the shell resolves it. That indirection is the entire reason a feature can
be deleted, disabled by a flag, or crash without touching anything else.

---

## 2b. Target state — where the planned phases land

The graph above is **today**. Six specs under [`specs/`](specs/) add seven modules and roughly
twenty database objects. This is the shape the codebase is heading toward, so a module you add now
should fit it.

```
                                  :apps:finance:app
      ┌──────────────┬──────────────┬───────┴───────┬──────────────┬──────────────┐
      │              │              │               │              │              │
   HOME tab      MONEY tab      CALC tab        PLAN tab      INSIGHTS tab    (shell-only)
      │              │              │               │              │              │
 ┌────┴────┐    ┌────┴────┐   ┌────┴─────┐   ┌─────┴──────┐  ┌────┴────┐   ┌─────┴──────┐
 │networth │    │  money  │   │calculator│   │  planning  │  │insights │   │ onboarding │
 │ ○ P2    │    │  ○ P3   │   │  ● live  │   │   ○ P4     │  │  ○ P5   │   │  ● live    │
 └─────────┘    └─────────┘   └──────────┘   ├────────────┤  └─────────┘   ├────────────┤
                                             │ insurance  │                │ assistant  │
   ● = built                                 │   ○ P4     │                │ currency   │
   ○ = planned, README + flag                ├────────────┤                │ unit       │
       exist, module does not                │ retirement │                │ date  ✗    │
                                             │   ○ P4     │                │ time  ✗    │
   ✗ = flag disabled                         ├────────────┤                │ automation │
                                             │ loans      │                │   ○ P7     │
                                             │ investments│                └────────────┘
                                             │ tax        │
                                             │ everyday   │  ● all live (calculators,
                                             └────────────┘     demoted to a strip below
                                                                the live Plan modules)
```

### Sub-modules — what lives inside each feature module

A "module" is a Gradle module. What a user experiences as a separate tool is usually a **mode inside
one module**, not a module of its own — the four Plan calculators each hold several, and `date`/
`time` hold sub-views as real files. Knowing which is which decides where your code goes.

Verified against the source tree; mode labels are read from each module's `*Config.kt`.

```
CALC tab
└── calculator ●                    single keypad screen + history

PLAN tab — live modules first, calculators demoted to a strip below (ADR-0027)
├── planning ○ P4                   E2 Budgets · E3 Budget detail · E4 Goals
│                                   E5 Goal detail · E6 Debt payoff
├── insurance ○ P4                  E7 Insurance · E8 Policy detail
├── retirement ○ P4                 E9 Retirement  (Base / Optimistic / Cautious scenarios)
├── loans ●                         Loan EMI · Loan Comparison
├── investments ●                   SIP Growth · ROI / CAGR · FD / RD
├── tax ●                           GST / Tax (Inclusive · Exclusive) · Salary Breakup
└── everyday ●                      Interest (Simple · Compound) · Discount
                                    Tip Split · Inflation

HOME tab
└── networth ○ P2                   C1 by sector · C2 assets · C3 holding detail
                                    C4 add/edit · C5 add valuation · C6 liabilities
                                    C7 liability detail      (01 Home itself is shell-owned)

MONEY tab
└── money ○ P3                      D1 ledger · D2 quick add · D3 full form · D4 detail
                                    D5 filter · D6 accounts · D7 account detail
                                    D8 categories · D9 recurring

INSIGHTS tab
└── insights ○ P5                   F1 monthly summary · F2 cashflow · F3 P&L
                                    F4 balance sheet · F5 reports & export

Shell — no tab of their own, reached from the top bar or a hub
├── onboarding ●                    SignInScreen · ConsentScreen · EmptyStartScreen
├── assistant ●                     Ask Dhruv chat        (consent-gated, minVersion 1.2.0)
├── currency ●                      FX converter          (rate cache in Room)
├── unit ●                          Length · Mass · Area · Temp
├── date ✗ flag off                 AddSubtractDays · AgeCalculator · BusinessWorkingDays
│                                   DateCountdown · DateDifference · TimeZoneConverter
│                                   UnixEpochConverter
├── time ✗ flag off                 Stopwatch · Timer
└── automation ○ P7                 G1 hub · G2 review queue · G3 AA consent
```

**Where does new code go?** Three questions, in order:

1. **Does it need its own feature flag, or must it be independently disableable?** → new Gradle
   module. That is the only thing a module boundary buys you that a mode does not.
2. **Is it another way of computing the same domain?** → a mode in the existing module
   (`Loan Comparison` sits beside `Loan EMI`; both are loans).
3. **Is it a distinct screen in the same domain?** → a sub-view file in the existing module, the way
   `date` holds seven.

Splitting further than this costs a Gradle module, a Koin module, a flag, a README and a
`projectDir` remap — pay it when you need isolation, not for tidiness.

**Two phases add no module at all** — their work lands in `:apps:finance:app` and `:libs:core`:
Phase 0b ([004-settings](specs/004-settings/)) builds the Settings control plane, and Phase 6
([006-search-notifications](specs/006-search-notifications/)) builds B2 notifications and B3 search
as shell screens. Do not go looking for a `search` module; there isn't one by design.

**Phase 7 (automation) has no spec-kit directory yet**, while Phase 3 and Phase 6 both defer work
into its review queue. That is a known open dependency, not an oversight you have discovered.

## 2c. Runtime structure — the shell at execution time

```
MainActivity  (single activity)
│
├── SplashScreen ─── brand sequence, <=2.5s, bare frame, no chrome
│
├── Onboarding ───── A2 sign-in ─> A3 DPDP consent ─> A4 empty start   (bare, pre-session)
│
└── Shell
    ├── NxTopBar ......... alerts · app switcher · Settings  (present on EVERY tab — nav law N5)
    │
    ├── HorizontalPager over the flag-filtered visible tabs
    │   │
    │   ├── HOME      ─ nested NavHost ─> 01 Home
    │   ├── MONEY     ─ nested NavHost ─> D1 Ledger ─> D4 detail · D6 accounts ─> D7 · D8 · D9
    │   ├── CALC      ─ nested NavHost ─> keypad ─> history
    │   ├── PLAN      ─ nested NavHost ─> E1 root ─> E2..E9 · loan/invest/tax/everyday
    │   └── INSIGHTS  ─ nested NavHost ─> F1 root ─> F2 · F3 · F4 · F5
    │
    └── Detail routes  (render OVER the tabs, back top bar, no tab bar)
        C1..C7 net worth · B2 alerts · B3 search · Settings tree · Profile · Ask
```

**Presentation classes** — the design fixes these, and picking the wrong one is a review comment:

| Class | Chrome | Used by |
|---|---|---|
| Tab root | bottom bar, no back arrow (N1) | 01, D1, Calc, E1, F1 |
| Push (detail) | back arrow to a **single** parent (N2) | C1–C3, C6, C7, D4, D6–D9, E2–E9, F2–F5, B2, B3 |
| Bottom sheet | drag handle, dismisses down, never navigates (N3) | C5 add valuation, D2 quick add, D5 filter, consent, app switcher |
| Full-screen modal | close ✕, **not** back ← | C4 add/edit holding, D3 full form, G3 AA consent |
| Bare | no chrome at all | splash, A2, A4 |

## 2d. The tracker request path

Every read or write of tracker data goes through this. The consent gate is structural — there is no
second PostgREST-capable client to route around it.

```
  Compose screen
       │  collectAsState()
       ▼
  ViewModel ──── StateFlow<UiState>          init { crashReporter.setModule("networth") }
       │                                     performanceTracer.trace("networth_load")
       │  suspend / Flow                     featureError <- CoroutineExceptionHandler
       ▼
  Repository  (:apps:finance:data/tracker/repo)   ← features may ONLY reach this far
       │
       ▼
  Retrofit + Moshi
       │
       ├──────────────── authClient ─────────────────┐   NOT consent-gated:
       │                 (GoTrue /auth/v1/*)         │   signing in is not itself
       │                                             │   data processing (ONB-BR-001)
       │
       └──────────────── dataClient ─────────────────┐
                         (PostgREST /rest/v1/*)      │
                              │                      │
                              ▼                      │
                    ┌───────────────────┐            │
                    │ ConsentInterceptor│  DPDP switch off ─> short-circuit BEFORE dispatch.
                    └─────────┬─────────┘  Nothing leaves the device.
                              ▼
                    ┌───────────────────┐  apikey + Bearer.
                    │  AuthInterceptor  │  401 ─> ONE refresh attempt.
                    └─────────┬─────────┘  second consecutive 401 ─> SessionStore = SignedOut.
                              ▼            No retry loop.
                    ┌───────────────────┐
                    │ CertificatePinner │  CA-level: GTS Root R1 + R4.
                    └─────────┬─────────┘  Never leaf — rotations would brick the app.
                              ▼
                    Accept-Profile: finance      ← omit this and you silently 404
                              │                    against the empty `public` schema
                              ▼
        ┌──────────────────────────────────────────────┐
        │  Supabase Postgres · schema `finance`        │
        │  RLS: user_id = auth.uid() on every table    │
        │  Views: security_invoker = on (or they leak) │
        └──────────────────────────────────────────────┘
```

`ErrorMapper` turns transport and PostgREST failures into domain errors, so a screen renders
`RetryErrorCard` rather than exception text (design-system §10).

## 2e. Data ownership — who owns which database object

Tables carry RLS scoped to `auth.uid()`, directly or transitively through a parent. Views are all
`security_invoker = on`. Nothing here is Room except where marked.

| Phase / spec | Tables | Views | Functions |
|---|---|---|---|
| P1 shipped | `holdings` · `valuations` | — | `delete_my_data()` · `delete_my_account()` *(in `public`, cross-app)* |
| **P2** [001](specs/001-net-worth-tracker/) | `liabilities_meta` | `v_latest_valuation` · `v_net_worth_by_sector` · `v_net_worth_history` | `correct_valuation()` · `create_holding_with_value()` |
| **P3** [002](specs/002-money-tab/) | `accounts` · `categories` · `transactions` · `transaction_events` · `recurring_templates` · `suggestions` | `v_account_balances` · `v_month_summary` · `v_category_spend` | `fn_transaction_audit` (trigger) · `merge_categories()` |
| **P4** [003](specs/003-plan-live-modules/) | `budgets` · `goals` · `goal_links` · `policies` · `policy_premiums` · `retirement_scenarios` | `v_budget_status` · `v_goal_progress` · `v_annual_income` | `fn_goal_link_earmark_guard` (trigger) |
| **P5** [005](specs/005-insights/) | — | — | `report_period_summary` · `report_cashflow` · `report_pnl` · `report_balance_sheet` · `report_category_breakdown` · `report_investment_returns`\* · `report_tax_summary`\* |
| **P6** [006](specs/006-search-notifications/) | `alert_log` **(Room, device-local)** | — | `search_all` · `due_alerts` |

\* gated on an unwritten decision record — see [readiness decisions](docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md) §2.3.

**Three invariants that cut across the whole table:**

- **`valuations` is append-only** — SELECT + INSERT policies only. A correction is
  `correct_valuation()`, never a client UPDATE.
- **`transaction_events` is the audit trail** — SELECT + INSERT only, written by a trigger, so an
  edit cannot silently skip it.
- **Every table above must appear in `public.delete_my_data()`.** `alert_log` is Room and therefore
  outside that function's reach by construction, which is exactly why it needs its own purge step.

Two views feed other views rather than screens: `v_latest_valuation` backs `v_net_worth_by_sector`,
and `v_category_spend` backs `v_budget_status`. Changing either has a blast radius beyond its own
phase.

## 2f. Screen index — screen → module → spec

Screen IDs come from the [functional spec](docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md)
§5 and are used verbatim in QA scenario IDs, task descriptions and route contracts. Learn them; they
are the shared vocabulary.

| IDs | Screens | Module | Spec |
|---|---|---|---|
| 00, A2–A4 | splash, sign-in, consent, empty start | `onboarding` | *(P1, shipped pre-spec-kit)* |
| 01 | Home | `:app` shell + `networth` | [001](specs/001-net-worth-tracker/) |
| B2, B3 | notifications, global search | `:app` shell | [006](specs/006-search-notifications/) |
| C1–C7 | net worth, assets, holding detail, add/edit, add valuation, liabilities, liability detail | `networth` | [001](specs/001-net-worth-tracker/) |
| D1–D9 | ledger, quick add, full form, detail, filter, accounts, account detail, categories, recurring | `money` | [002](specs/002-money-tab/) |
| E1–E9 | plan root, budgets, budget detail, goals, goal detail, debt payoff, insurance, policy detail, retirement | `planning` · `insurance` · `retirement` | [003](specs/003-plan-live-modules/) |
| F1–F5 | monthly summary, cashflow, P&L, balance sheet, reports | `insights` | [005](specs/005-insights/) |
| G1–G3 | automation hub, review queue, AA consent | `automation` | **no spec yet** |
| — | Settings tree, app lock | `:app` shell | [004](specs/004-settings/) |

---

## 3. `:apps:finance:app` — the shell

```
app/
├── MainActivity.kt          single activity; pager over the 5 tab roots
├── CalculatorApplication.kt Koin startup; aggregates EVERY feature's module {}
├── di/
│   ├── AppModule.kt              app-level bindings
│   ├── PlatformModule.kt         platform bindings + feature-flag loading
│   └── FeatureFlagAssetLoader.kt reads the packaged flags asset
├── navigation/
│   └── NavigationDispatcher.kt   resolves a NavTarget to an actual destination
└── ui/
    ├── shell/       tab shell, bottom bar, app-switcher sheet, detail routes
    ├── splash/      brand sequence, ≤2.5s
    ├── dashboard/   Home tab root — placeholder today, Phase 2 replaces it
    ├── onboarding/  sign-in / consent / empty-start hosting
    ├── plan/        Plan tab launcher
    └── settings/    Settings tree
```

**Nothing depends on `:app`.** It is the top of the graph. `core → app` is forbidden, which is what
stops the design system from acquiring app-specific knowledge.

**Adding a feature module is three edits**, and forgetting any one is a silent failure:
1. `settings.gradle.kts` — `include(...)` **and** the `projectDir` remap (see §8)
2. `CalculatorApplication` — aggregate its Koin `module {}`, or nothing is injectable
3. `platform/feature-flags/dhruv-finance.json` — its flag, or `FeatureHost` renders it disabled

### Navigation

Five tab roots — **Home · Money · Calc · Plan · Insights** (ADR-0027). Settings is reached from the
top bar, never a tab.

- A pager holds the tabs; each tab hosts its own nested `NavHost` for drill-ins.
- "No tab" routes (detail screens, Settings, Ask) render over the tabs with a back top bar.
- **No AndroidX Navigation graph.** Routes are a sealed `NavTarget` hierarchy in `:libs:core`,
  resolved by `NavigationDispatcher`.
- `TabKey` resolution is **by key, not position**, so hiding a tab behind a flag cannot silently
  retarget another tab.
- Back-press precedence is encoded once in `BackContract.kt` (`:libs:core`) —
  detail route → active tab's nested stack → first tab → exit. It is a pure function with its own
  test. **Never re-derive that order inline.**

**Cross-feature navigation is by id, never by class reference.** Adding a route means adding the
sealed subtype **and** its row in the
[surface registry](docs/superpowers/specs/2026-08-09-finance-surface-registries.md) — both, in the
same change. The registry has drifted behind the code before precisely because that pairing was
treated as optional.

### Fault isolation

Every route is wrapped:

```kotlin
FeatureHost(featureKey, isEnabled = resolver.isEnabled(key), featureError, crashReporter) { … }
```

Flag off → `FeatureDisabledCard`. Thrown error → `FeatureErrorCard`, tagged with the module in
Crashlytics. **Never a blank crash** — a `PLATFORM.md` §4 rule, and it applies to *every* route, not
just tab roots.

Each feature ViewModel additionally does three things (see `:libs:core`'s `FeatureViewModel`):
`crashReporter.setModule("<key>")` in `init`, a `performanceTracer.trace("<key>_…")` around one
primary operation, and a `featureError: StateFlow<Throwable?>` fed by a `CoroutineExceptionHandler`.

### Feature flags

`platform/feature-flags/dhruv-finance.json` is the **single** source — packaged as an Android asset,
parsed with Moshi into `Map<String, FeatureFlag>` (`enabled` + `minVersion` + `requiresConsent`),
handed to `HardcodedFeatureFlagResolver` with `BuildConfig.VERSION_NAME`. There is no second
hand-written copy to drift out of sync.

If the asset is missing or unparseable, it falls back to a **calculator-only safety map** and reports
via `CrashReporter` — the app degrades to its offline-safe subset rather than guessing.

A flag may be provisioned **before** its module exists (`networth` is today). That is intentional,
not an error.

---

## 4. `:apps:finance:data` — two storage domains

```
data/
├── AppDatabase.kt        Room, version 5
├── HistoryEntity/Dao/Repository.kt      calculator history
├── CurrencyRateEntity/Dao/Repository.kt currency cache  (+ ICurrencyRepository)
├── GeminiRepository.kt   AI; takes its key as a ctor arg so it can live here
├── api/CurrencyApi.kt    Retrofit
├── util/                 formatting helpers
├── di/                   Koin bindings
└── tracker/              ← the Supabase side
    ├── net/   SupabaseClientFactory · AuthInterceptor · ConsentInterceptor · ErrorMapper
    └── auth/  AuthRepository · ConsentRepository · SessionStore · SessionState
                GoTrueApi · TrackerRpcApi · TrackerAccountRepository
```

`tracker/dto/` exists; `model/`, `mapper/` and `repo/` arrive with Phase 2's first real tracker
entities.

### The split, and why it is not negotiable

**Room = calculators and converters.** Offline-first, device-local, no account. This is the domain
`PLATFORM.md` §5's `DhruvEntity` / HLC / sync design describes.

**Supabase = the entire tracker.** Cloud-primary: **no local mirror, no `DhruvEntity`, no
client-side conflict resolution.** The server plus RLS (`user_id = auth.uid()`) is the single source
of truth. This narrowly overrides §5 for this domain only (ADR-0014).

The practical consequence a new developer hits first: **tracker screens require network and a
session.** Signed-out, offline and not-configured are *designed first-class UI states*, not error
dialogs, and every tracker screen must define them.

### Three structural properties of the network layer

These are properties of the code's *shape*, not conventions to remember:

1. **Consent is an interceptor.** `ConsentInterceptor` is attached only to the PostgREST client and
   short-circuits before dispatch if the DPDP switch is off. **No code path can reach tracker data
   before consent, because no other PostgREST-capable client is constructed anywhere in the app.**
   Two Retrofit instances share one `OkHttpClient.Builder`: an unauthenticated `authClient` (GoTrue
   — sign-in itself is not consent-gated) and a consent-gated `dataClient` (PostgREST).
2. **Auth retry is bounded.** `AuthInterceptor` attaches `apikey` + bearer. A 401 triggers exactly
   **one** refresh attempt; a second consecutive 401 forces `SessionStore` to `SignedOut`. No retry
   loop. Tokens live only in `EncryptedDataStore`.
3. **Certificate pinning is CA-level** — Google Trust Services **GTS Root R1 + R4**. Leaf pinning
   would brick the app on Supabase's routine rotations. *These pins were wrong once* (ISRG X1/X2 was
   assumed from prose docs) and shipped undetected until a real device's first live sign-in threw
   `SSLPeerUnverifiedException`. **Pinning a third-party domain requires observing its actual live
   TLS chain** — see ADR-0029's correction.

### Money

**Integer paise** (`Long` / `bigint`) for every tracked amount — exact, summable, no floating point.
Proportions are integer **basis points**. `BigDecimal` is confined to the calculator and projection
engines, which live outside `tracker/`. The `checkTrackerMoneyPrecision` Gradle task enforces the
boundary, so a `Double` in a tracker DTO fails the build rather than rounding someone's net worth.

### Schema

Declarative source of truth is [`supabase/schemas/finance/`](../../supabase/schemas/finance/) — one
file per object. `supabase db diff` generates the migration into `supabase/migrations/`, which is
executed history and is **never hand-edited after being applied**. Current shape:
[`supabase/SCHEMA.md`](../../supabase/SCHEMA.md) (generated).

Four things `db diff` cannot emit and that you must hand-append: **grants**,
**`security_invoker = on` on views**, `ALTER POLICY`, and comments. Read
[`platform/skills/dhruv-supabase-object/SKILL.md`](../../platform/skills/dhruv-supabase-object/SKILL.md)
before touching any of it.

Every tracker request sends **`Accept-Profile: finance`** (`Content-Profile` on writes). Omitting it
does not error loudly — it silently 404s against the empty `public` schema.

---

## 5. `:libs:core` — the shared library

```
core/
├── ui/
│   ├── FeatureHost.kt        the fault-isolation boundary
│   ├── components/           39 files — the entire component library
│   └── theme/                DhruvNextTokens · DhruvBrandColors · DhruvFont
├── navigation/               NavTarget · NavConfig · BackContract
├── observability/            CrashReporter · PerformanceTracer · FeatureViewModel
├── security/                 Keystore + integrity helpers
├── flags/                    FeatureFlag + resolver
├── format/                   money and date formatting
├── domain/                   shared domain primitives
└── integrity/                Play Integrity (warn-only)
```

**`core` depends on nothing internal.** Compose + Material3 only. This is what lets every app in the
monorepo consume it without dragging Finance along.

### The micro-frontend rule

**Feature modules own screens and flows only. Every reusable visual lives in `:libs:core`.** Zero
feature-local styling: no raw hex, no `.dp`/`.sp` literals, no ad-hoc card shapes, no
`MaterialTheme.colorScheme`/`.typography` in a screen file.

**Two colour systems, and confusing them is the most common design error:**

| | Brand chrome | App tokens |
|---|---|---|
| Read via | `DhruvBrand.*` | `LocalDhruvNextColors.current` |
| Flips with light/dark? | **No** — theme-invariant | **Yes** |
| Carries | splash, hero gradients, deliberately-dark screens | everything else |

Before writing a component, check
[`platform/DESIGN-SYSTEM.md`](../../platform/DESIGN-SYSTEM.md) §5: §5.1 is what exists, §5.2 is
planned-but-unbuilt, §5.3 is built-but-narrower-than-the-design. **Do not write a screen against a
§5.2 component** — that section exists because a previous design system declared a component library
that was never built and screens were written against a fiction for months (ADR-0030). Closing a
§5.3 gap means *extending* the component, never adding a parallel one.

---

## 6. Feature modules

Eleven exist today; seven more are planned. The index with status and flags is
[FEATURES.md](FEATURES.md); each module's own `README.md` holds its screens, ViewModels and data
dependencies.

**On-disk layout is bucketed by owning tab; Gradle coordinates stay flat:**

```
apps/finance/feature/plan/loans/     ← directory
:apps:finance:feature:loans          ← Gradle coordinate
```

The two are reconciled by a `projectDir` remap in `settings.gradle.kts`. See §8 — this is a real
tripwire.

### Anatomy of a module

```
feature/<bucket>/<name>/
├── build.gradle.kts        dhruv.android.library + dhruv.android.compose
├── README.md               screens, ViewModels, data deps, flag key
└── src/main/java/com/dhruv/finance/<name>/
    ├── <Name>Screen.kt     Compose; stateless where possible
    ├── <Name>ViewModel.kt  StateFlow<UiState>; observability triad in init
    ├── <Name>Config.kt     screen-level constants (no-hardcoding rule)
    └── di/<Name>Module.kt  val <name>Module = module { viewModel { … } }
```

Pattern: **Screen → UiState → Content**, with the ViewModel exposing a single `StateFlow<UiState>`.
Calculation logic is pure and separately testable; data access goes through a repository.

---

## 7. Testing

```bash
./gradlew regressionCheck          # THE gate: all unit tests + ArchUnit + JaCoCo + coverage floor
./gradlew :apps:finance:app:testDebugUnitTest
./gradlew :apps:finance:feature:<name>:testDebugUnitTest --tests "com.dhruv.finance.<name>.SomeTest"
```

| Tool | Use |
|---|---|
| JUnit4 + `coroutines-test` | everything |
| Turbine | Flow assertions |
| ArchUnit | module boundaries |
| Robolectric | Android-dependent unit tests |
| JaCoCo | coverage — **not Kover**, which has no working AGP 9 integration (ADR-0013) |

**Test-first is constitutional**, not a preference: RED → GREEN → REFACTOR, and feature work cites a
scenario ID from the
[QA catalog](docs/superpowers/specs/2026-08-09-qa-test-scenario-catalog.md) *before* any code exists.

**Room DAOs are tested through fakes, never in-memory Room.** Robolectric-SQLite is a known blocker
on this toolchain — a hard constraint, not a stylistic choice.

The coverage floor only ratchets **up**, at stated checkpoints, never ahead of landed tests.

---

## 8. Tripwires

Things that have actually caused failures here, each worth reading once:

1. **A new feature module needs its `projectDir` remap.** Directory and Gradle coordinate differ by
   design; without the remap Gradle resolves the wrong path and *configuration* fails — before any
   compile error tells you why.
2. **A new module must join `coveredModules`/`_FEATURES`** in the root `build.gradle.kts`, or its
   coverage is invisible to the gate and to release notes.
3. **Every Postgres view needs `security_invoker = on`.** Without it the view runs as its owner,
   bypasses RLS, and returns every user's rows through PostgREST. Eight planned views were missing
   it.
4. **A new user-data table must be added to `public.delete_my_data()` in the same migration.** That
   function *is* the DPDP 7-day erasure guarantee, and a miss is silent — nothing fails, no test
   goes red.
5. **Append-only means no UPDATE policy — so a correction is an RPC, not a client write.** Setting
   `deleted_at` is an UPDATE. A spec saying "append-only, and the client marks the row deleted" is
   self-contradictory, and that contradiction shipped into three documents.
6. **Do not add an UPDATE policy to make a correction work.** It makes the table ordinarily mutable
   and destroys the guarantee.
7. **Cert-pinning a third-party domain requires observing its real TLS chain**, never assuming a CA
   from prose.
8. **The equivalence guard does not compare named UNIQUE/PK/FK constraints added via `ALTER`** —
   deliberate, since they have no inline declarative spelling and comparing them produced permanent
   false positives. Columns and CHECK expressions are compared.
9. **Docs saying "Hilt", "Kover", "Spotless" or `BentoCard` are stale.** DI is Koin, coverage is
   JaCoCo, formatting is ktlint, and `BentoCard` has never existed.

---

## 9. Where to look next

| Question | File |
|---|---|
| What modules exist, what is planned? | [FEATURES.md](FEATURES.md) |
| What does the app *do*? | [functional spec](docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md) |
| Why is it built this way? | [`platform/DECISIONS.md`](../../platform/DECISIONS.md) |
| How must it look and behave? | [`platform/DESIGN-SYSTEM.md`](../../platform/DESIGN-SYSTEM.md) |
| What am I allowed to change? | [`.specify/memory/constitution.md`](../../.specify/memory/constitution.md) |
| What is being built now? | [implementation plan §7](docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md) |
| Open architectural calls | [readiness decisions](docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md) |
| Conventions + build commands | [CLAUDE.md](CLAUDE.md) · [AGENTS.md](../../AGENTS.md) |