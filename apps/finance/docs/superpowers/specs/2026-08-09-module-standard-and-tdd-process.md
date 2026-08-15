# Dhruv Finance — Module Standard & TDD/QA Process (Solution Architecture)

> **Companion docs:** functional spec (`2026-08-08-design-v1-final-functional-spec.md`), the
> implementation plan (`2026-08-08-design-v1-final-implementation-plan.md`), and the QA scenario
> catalog this process feeds (`2026-08-09-qa-test-scenario-catalog.md`).
>
> **Purpose:** two things the maintainer asked to be explicit and binding: (1) every Gradle module
> added for the design-v1 build follows **one** structural standard, not ad hoc per-feature choices;
> (2) **no implementation code is written before its test scenarios exist** — QA scenarios first,
> failing tests second, minimal code third. This is not new architecture — it formalizes the
* `apps/finance/feature/loans` template already in the repo and makes it a checked standard.

---

## 1. The module standard (SA)

### 1.1 Canonical Gradle module shape

Every feature module — existing (`loans`, `investments`, `tax`, `everyday`, `currency`, `unit`,
`calculator`, `date`, `time`, `assistant`) and new (`onboarding`, `networth`, `money`, `planning`,
`insurance`, `retirement`, `insights`, `automation`) — has exactly this shape. This is not a
suggestion; `dhruv-module-auditor` checks it pre-merge.

**Folder location (added 2026-08-09):** every module's directory lives under
`apps/finance/feature/<bucket>/<name>/`, grouped by the tab that owns it — see
`apps/finance/feature/README.md` for the full bucket scheme (`home`/`money`/`calc`/`plan`/
`insights`/`onboarding`/`shell`) and §2.2's table below for each module code's bucket. The Gradle
coordinate stays flat (`:apps:finance:feature:<name>`, never `:apps:finance:feature:<bucket>:
<name>`) — only the physical path is bucketed, via `projectDir` remaps in `settings.gradle.kts`.
New modules: add the `include(...)` line as before, then add its `projectDir` remap alongside the
existing ones rather than letting Gradle default it to the flat (now-wrong) location.

```
apps/finance/feature/<bucket>/<name>/
├── README.md                     # what this module does, owner tab, flag, screens, QA-catalog link — required, see §1.1a
├── build.gradle.kts              # dhruv.android.library + dhruv.android.compose; deps: :data, :libs:core, :libs:settings
├── consumer-rules.pro
├── detekt-baseline.xml           # only if pre-existing debt is grandfathered in — new modules start clean
└── src/
    ├── main/java/com/dhruv/finance/<name>/
    │   ├── <Name>Screen.kt       # @Composable, Screen→UiState→Content pattern, zero business logic
    │   ├── <Name>ViewModel.kt    # StateFlow<UiState>, featureError: StateFlow<Throwable?>, CoroutineExceptionHandler
    │   ├── <Name>UiState.kt      # data class, one state shape for the whole screen (or sealed for signed-out/offline/loaded)
    │   ├── <Name>Config.kt       # screen-level constants/copy — NEVER hardcoded in Screen.kt (no-hardcoding rule)
    │   └── di/<Name>Module.kt    # Koin `module { viewModel { ... } }`, aggregated in CalculatorApplication
    └── test/java/com/dhruv/finance/<name>/
        ├── <Name>ViewModelTest.kt      # unit — state transitions, one test per scenario ID (§2)
        └── <Name>EngineTest.kt         # unit — pure calculation/business-rule logic, if the module has one
```

#### 1.1a `README.md` content (required, not optional)

Written as part of building the module, same as `build.gradle.kts` — not a follow-up task, and
never skipped for a "small" module. Fixed shape (see any existing module's `README.md` for a
worked example, e.g. `apps/finance/feature/plan/loans/README.md`):

```markdown
# <name>
<one-line description>
- **Gradle module:** `:apps:finance:feature:<name>`
- **Owner tab:** <tab name, or "none — shell", or "none — pre-tab">
- **Flag:** `<key>` in platform/feature-flags/dhruv-finance.json — <enabled/disabled/gated state>
## Screens
## ViewModels
## Data dependencies
```

Before the module exists (i.e., during the phase-0/QA-scenario step, before any code lands), the
README still gets created — with "not yet created," the target screen IDs, the QA-catalog section
to write failing tests against, and the business rules to implement against, in place of the
ViewModels/Screens-as-built sections (see e.g. `apps/finance/feature/home/networth/README.md` for
the worked not-yet-built shape). This gives every module a real README from the moment its bucket
folder is created, not just from the moment code lands in it.

Data-owning modules (`:apps:finance:data`) add one more layer per bounded context:

```
apps/finance/data/src/main/java/com/dhruv/finance/data/
  tracker/<context>/              # e.g. tracker/networth, tracker/money, tracker/planning
    ├── dto/<Name>Dto.kt          # Moshi wire shape only — no business logic
    ├── model/<Name>.kt           # domain model — paise Long, enums, no Android/Moshi imports
    ├── mapper/<Name>Mapper.kt    # Dto ↔ model, pure functions
    └── repo/<Name>Repository.kt  # interface + impl; impl is the ONLY thing touching Retrofit
apps/finance/data/src/test/java/com/dhruv/finance/data/tracker/<context>/
    ├── Fakes.kt                  # in-memory fake of the repository interface (Windows/Robolectric-SQLite
    │                             #   constraint, ADR-0013 — see §1.3) — real query semantics, no network
    ├── <Name>MapperTest.kt       # unit — Dto↔model round-trip, edge cases (nulls, paise rounding)
    └── <Name>RepositoryTest.kt   # unit against the Fake — never against a live Supabase project
```

**Rule:** a feature module NEVER imports another feature module (ArchUnit `DependencyRulesTest`,
enforced today). A feature module NEVER imports a DAO **or DTO** directly — only its
`tracker/<context>` repository interface. `:libs:core` never imports app/feature code.
**Correction (found by the 2026-08-09 doc re-validation pass):** only the DAO half of the middle
rule is currently enforced — `DependencyRulesTest`'s `feature modules must not import DAOs
directly` test matches `.*Dao` only, there is no `.*Dto` check. Since Phase 1 introduces
`tracker/dto/*Dto.kt`, **Phase 1's Backend step must extend that test to also match `.*Dto`**
before any feature module gets a chance to import one directly (tracked as an explicit Phase-1 task
in the implementation plan §7). Until that lands, this rule is a standard to build to, not yet a
enforced guardrail — do not claim it is enforced in review.

### 1.2 Test-layer standard (maps the TDD skill's pyramid onto this codebase)

| Layer | What it tests | Size | Tooling | CI gate |
|---|---|---|---|---|
| **Unit — pure logic** | `<Name>Engine`/calculation functions (EMI, budget pace, avalanche/snowball, XIRR, retirement projection) | Small | JUnit4, no I/O | `regressionCheck` |
| **Unit — ViewModel** | State transitions, `featureError` on thrown exceptions, one test per QA scenario ID | Small | JUnit4 + `kotlinx-coroutines-test` (in active use today) + Turbine for StateFlow-sequence assertions (declared in `libs.versions.toml`/ADR-0013 but **not yet used anywhere in the repo** — this is where design-v1's multi-state ViewModels, e.g. signed-out→loading→loaded, are meant to start using it) | `regressionCheck` |
| **Unit — repository** | Query semantics (filtering, ordering, append-only enforcement) against a **Fake**, never real SQLite/Supabase | Small–Medium | JUnit4 + in-memory `Fake*` (see `data/src/test/.../Fakes.kt`) | `regressionCheck` |
| **Unit — mapper** | Dto↔model correctness, paise rounding, null handling | Small | JUnit4 | `regressionCheck` |
| **Architecture** | Module boundaries, DAO-access rule, screen-to-screen import rule | N/A | ArchUnit (`DependencyRulesTest`) | `regressionCheck` |
| **Contract/interceptor** | `ConsentInterceptor` blocks calls pre-consent, `AuthInterceptor` attaches/refreshes tokens | Small | JUnit4 + OkHttp `MockWebServer` (no live network) | `regressionCheck` |
| **Compose/UI smoke** | A screen renders its signed-out/offline/not-configured/loaded states without throwing | Medium | Robolectric Compose test (new to this repo — see §1.3) | `regressionCheck` once introduced |
| **Instrumented (manual/local)** | Real Room/SQLCipher, real Google Sign-In, real biometric prompt | Large | `connectedAndroidTest`, developer-local only | **not** in CI (ADR-0013) |
| **RLS policy** | Each Supabase table's `user_id = auth.uid()` policy actually blocks cross-user reads | Medium | SQL test against a `dhruv-dev` Supabase project, run manually per migration | pre-merge checklist, not automated CI yet |

**Why Robolectric-Compose is new, not already used:** existing feature tests stop at the ViewModel
layer (`LoansViewModelTest` pattern) — no `Name>ScreenTest.kt` exists anywhere in the repo today.
Design-v1 introduces the signed-out/offline/not-configured trio (NFR-4) as **designed states**, and
those are exactly the kind of behavior a ViewModel-only test cannot see (did the right *card*
render for the right *state*?). Phase 0 adds one Robolectric Compose test as a spike
(`StateCardsScreenTest` for `SignedOutCard`/`OfflineStateCard`/`NotConfiguredCard`) before any
feature module relies on the pattern — if Robolectric Compose doesn't work cleanly on Windows (the
same class of problem that killed Robolectric-SQLite, ADR-0013), the fallback is a plain
`ComposeTestRule` semantic-tree assertion without Robolectric, decided at that spike, not assumed.

### 1.3 Fakes, not mocks, for the data layer
Per the TDD skill's preference order (real > fake > stub > mock) and the existing
`FakeHistoryDao`/`FakeCurrencyRateDao` convention: every new repository gets a hand-written
in-memory `Fake<Name>Repository` implementing the same interface, replicating real query semantics
(soft-delete filtering, append-only rejection, RLS-shaped `user_id` filtering). This is not a
mocking-framework mock — it is a second, test-only implementation, exactly like
`apps/finance/data/src/test/java/com/dhruv/finance/data/Fakes.kt` already does. `MockWebServer`
(OkHttp) is the one exception, used only at the true network boundary (interceptor tests) where a
fake HTTP server is cheaper and more honest than faking Retrofit internals.

---

## 2. QA test-scenario process (the TDD gate)

### 2.1 The rule

> **No task in the implementation plan may move from "planned" to "in progress" for its Android/
> Backend engineering steps until its module's section in the QA Test Scenario Catalog
> (`2026-08-09-qa-test-scenario-catalog.md`) exists, is derived from the functional spec's BR-*/
> NFR-*/F-* IDs, and has been reviewed.**

This is the project's TDD cycle applied at the planning level, not just the code level:

```
SPEC (functional spec, already written)
   │
   ▼
QA SCENARIO CATALOG  ← RED, at the acceptance level: scenario exists, no code satisfies it yet
   │  (one row per BR/NFR/flow-step; Given/When/Then; test ID)
   ▼
FAILING UNIT/INTEGRATION TEST  ← RED, at the code level: <Name>ViewModelTest references the
   │                              scenario ID in a comment, test compiles, fails (function missing
   │                              or returns wrong state)
   ▼
MINIMAL IMPLEMENTATION  ← GREEN
   │
   ▼
REFACTOR  ← tests stay GREEN
   │
   ▼
QA marks the scenario row CLOSED in the catalog (owner + date), citing the test file:line
```

A scenario row is **never** marked closed by the engineer who wrote the code — closure is a QA
role action (see §3), even when the same person holds both hats on a solo-maintainer project: it
is a distinct checklist pass, not a rename of the same commit.

### 2.2 Test ID scheme

`<MODULE>-<KIND>-<NNN>`, module codes matching the feature-module topology. **Folder bucket**
(added 2026-08-09) is where that module's directory physically lives under
`apps/finance/feature/` — see `apps/finance/feature/README.md`.

| Code | Module | Folder bucket |
|---|---|---|
| `NAV` | shell/navigation (5-tab, back contract, route registry) | — (`:libs:core`/`:apps:finance:app`, not a feature module) |
| `ONB` | onboarding (A2–A4) | `onboarding/onboarding/` |
| `NW` | networth (C1–C7) | `home/networth/` |
| `MNY` | money (D1–D9) | `money/money/` |
| `HOM` | Home tab (01) — shell-owned, not a feature module (same as `PlanLauncher`'s E1) | — (shell-owned, no module) |
| `PLN` | planning — budgets/goals/debt (E2–E6). **E1 is shell-owned**, not `:feature:planning` — see implementation plan §6: `:apps:finance:app` owns "Home 01, B2, B3, shell, Plan root E1"; this matches `PlanLauncher.kt`'s existing location under `apps/finance/app/.../ui/plan/`, not a feature module. | `plan/planning/` |
| `INS` | insurance (E7–E8) | `plan/insurance/` |
| `RET` | retirement (E9) | `plan/retirement/` |
| `SIG` | insights (F1–F5) | `insights/insights/` |
| `AUT` | automation (G1–G3) | `shell/automation/` |
| `SRC` | search + notifications (B2–B3) | — (shell-owned, no module) |
| `DAT` | tracker data layer (auth, consent gate, repos, mappers, RLS) — cross-cuts every feature module | — (`:apps:finance:data/tracker/`) |

`KIND` = `BR` (business rule), `FLOW` (multi-screen user flow), `NFR` (non-functional), `UI`
(screen-state rendering, e.g. signed-out card shown), `ARCH` (module-boundary/architecture).

Example: `NW-BR-003` = Net Worth module, business-rule scenario #3.

### 2.3 Scenario row format (used verbatim in the catalog)

```markdown
#### <ID> — <one-line title>
- **Given** <precondition/state>
- **When** <action>
- **Then** <expected outcome>
- **Source:** <BR-C1 / NFR-4 / F-2 step 3 — the functional-spec anchor>
- **Size:** S | M | L   **Automatable:** Y | N (N = manual/instrumented, e.g. real biometric)
- **Test file (once written):** `<module>/src/test/.../<File>.kt#<testMethodName>`
- **Status:** ☐ scenario defined → ☐ test written (RED) → ☐ implemented (GREEN) → ☐ QA closed
```

---

## 3. Roles and responsibilities (RACI, every phase)

Solo-maintainer project, so one person holds multiple hats — but the **hats are still distinct
steps**, not skipped. Each phase in the implementation plan (§4 below) lists these as swimlanes so
nothing silently collapses into "just write the code."

| Role | Responsible for |
|---|---|
| **SA (Solution Architect)** | Module boundaries, Supabase schema + RLS + views, `NavTarget`/route-registry contract, ADRs. Signs off before Backend/Android work starts on a phase. |
| **QA Engineer** | Owns the scenario catalog: derives rows from the functional spec, reviews they're complete ("every scenario") before engineering starts, closes rows after verifying the test + a manual pass on NFR-4 states, DPDP consent gates, and any `Automatable: N` row. |
| **Backend/Data Engineer** | Migrations, repositories, mappers, interceptors (`ConsentInterceptor`/`AuthInterceptor`), Fakes. Works TDD against `DAT-*` and the phase's `<MODULE>-BR-*` scenarios. |
| **Android Engineer** | ViewModels, Screens, Koin wiring, component-library additions. Works TDD against `<MODULE>-UI-*`/`FLOW-*` scenarios, consuming Backend's repository interfaces (which may be Fake-backed until Backend's row lands — vertical slicing, not blocked serial handoff). |
| **Web Engineer** (parallel track) | Mirrors a phase's schema/tokens into `web/` one phase behind, per the existing `web-android-parallel-dev` decision — not gating Android's phase completion. |
| **Security/DPDP Reviewer** | Runs `dhruv-security-compliance-reviewer` at the phase checkpoint: consent-before-network, no secrets, RLS present on every new table. |

---

## 4. How this changes the implementation plan's phases

The phase list in `2026-08-08-design-v1-final-implementation-plan.md` §7 keeps its scope and order.
Each phase's **internal** step order is now fixed to:

```
1. SA      — finalize the phase's schema/migration + NavTarget/route additions (design, not code)
2. QA      — write/complete that phase's rows in the scenario catalog; review for completeness
             against the functional spec section covering those screens
3. Backend — RED: write failing repository/mapper/interceptor tests citing scenario IDs
             GREEN: minimal implementation
             REFACTOR
4. Android — RED: write failing ViewModel/screen-state tests citing scenario IDs (may start against
             Backend's Fake before the real repository lands — see §1.1's vertical-slice note)
             GREEN: minimal implementation
             REFACTOR
5. QA      — verify: run `regressionCheck`, execute Automatable:N rows manually, close scenario rows
6. Security — DPDP/secrets/RLS pass at the phase checkpoint (skip if the phase touched no
              off-device data — e.g. Phase 0)
7. Checkpoint — merge gate: regressionCheck green, coverage floor not regressed, every phase
              scenario row is CLOSED or explicitly deferred with a reason
```

No phase may reorder this — in particular, step 4 (Android UI) may not start before step 3 produces
at least a repository **interface** (even Fake-backed) and step 2 produces the scenario rows it is
implementing against. This is what makes "tests before code" true at the phase level, not just
inside one file.

---

## 5. What this does NOT change

- The functional spec (`2026-08-08-design-v1-final-functional-spec.md`) is unchanged — it is the
  source every scenario row cites.
- The implementation plan's module topology, Supabase schema, and phase **scope** (§5–§7 of that
  doc) are unchanged — this doc only fixes the *order of work* and the *module shape* within each
  phase.
- ADR-0027/ADR-0028 (5-tab nav, brand chrome) stand as written.
- Existing modules (`loans`, `currency`, etc.) are not retroactively required to have a QA catalog
  entry — this process applies to design-v1 build-out (Phase 0 onward), not a rewrite of shipped code.
