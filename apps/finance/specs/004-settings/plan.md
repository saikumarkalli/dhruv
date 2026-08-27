# Implementation Plan: Settings — application control plane

**Branch**: `004-settings` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `apps/finance/specs/004-settings/spec.md`

## Summary

Build Settings as the application's control plane: three tiers (Account · App · Modules) where the
shell owns the first two and **every module declares its own settings entry**, so a later phase ships
a module and its settings together without editing anything Settings owns.

Technical approach: a **declarative contribution contract** in `:libs:settings` — a module publishes
data describing its entry (groups, rows, each row carrying its own read `Flow` and write lambda), and
the shell renders it with `:libs:core` components. The shell resolves contributions by type from the
DI container rather than from a hardcoded list, so adding a module changes no Settings file
(FR-004/SC-004). Rows needing bespoke UI are not permitted in the module vocabulary; a module that
needs one navigates to its own screen through the existing `NavTarget` id mechanism, which keeps
feature-owned UI out of the shared surface and satisfies the no-feature-local-styling rule.

Alongside the container, Phase 0b lands the three defects the spec names — sign-in/sign-out from
Settings, an app lock that actually gates, durable assistant consent — plus the app-wide lock
checkpoint, which is shell architecture (a process-level gate + hold-and-dispatch for links arriving
while locked), not a screen.

## Technical Context

**Language/Version**: Kotlin (JVM target 17, Android `minSdk 26`), Jetpack Compose

**Primary Dependencies**: Koin 3.5.6 (DI + contribution resolution via `getAll`), AndroidX DataStore
Preferences 1.1.7 (plaintext prefs) + the existing `EncryptedDataStoreFactory` (secrets),
`androidx.biometric` 1.1.0 — **already in the catalog and already declared by `:libs:settings`, but
currently unused by any code**, Compose Material3. No new third-party dependency is required.

**Storage**: existing `app_settings` DataStore for preferences (keys are append-only — see
`SettingsKeys.kt`), existing `secure_settings` EncryptedDataStore for the personal AI key. No schema
change, no Supabase change, no Room change. Session/consent state continues to live where Phase 1
put it (`SessionStore`, `ConsentRepository` in `:apps:finance:data`).

**Testing**: JVM unit tests (JUnit4 + coroutines-test + Turbine) for the contribution registry, the
app-lock decision function, and every ViewModel; Robolectric only where a Compose/Android type is
unavoidable; ArchUnit for the new boundary rules. Gate is `./gradlew regressionCheck`.

**Target Platform**: Android 8.0+ (API 26), single-activity Compose shell

**Project Type**: Android app module + shared libraries in a Gradle monorepo

**Performance Goals**: Settings top level renders within one frame budget of the shell's existing
detail-route swap; contribution resolution happens once per Settings open, not per recomposition. The
lock gate must decide before the first frame of any content — no flash of unlocked content.

**Constraints**: no raw dp/sp/hex in any screen file; no `feature → feature` import; no persistent
preference reachable only inside a module (FR-003); no network call from Settings other than the
already-permitted GoTrue sign-in and the erasure RPCs; preference keys never renamed when a row moves.

**Scale/Scope**: ~19 existing rows migrated; **2 shell entries** — Account and App, the latter holding
Appearance, Security, Notifications and App-details as areas rather than as separate entries (FR-001)
— plus a modules tier that starts with the modules shipping today (calculator, converters, assistant)
and grows with each phase. 11 feature-flag keys exist today; the modules tier keys off them.
Delivered as five sub-phases (see Phase sequencing below), 118 tasks.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| # | Principle | Pre-design | Post-design | Notes |
|---|---|---|---|---|
| I | Test-First | PASS | PASS | Every task pair is RED→GREEN; the contribution registry and the lock decision are pure functions, so they are testable without Robolectric. |
| II | Scenarios Before Code | **FAIL** | **PASS (2026-08-19)** | Was: no `SET-*` module in the QA catalog. Now written — catalog **§13 SET**, 50 rows, all ☐, with 9 `ONB`/`DAT`/`NAV` rows reused by id rather than restated. The remaining gate item is the SA step below, which is a documentation rewrite, not a scenario gap. |
| III | Module Boundaries | PASS | PASS | Contract lives in `:libs:settings`, which every feature already depends on and which itself depends only on `:libs:core`. Modules publish data; the shell renders. No feature imports another; no feature imports the app. |
| IV | Fault Isolation | PASS | PASS | Settings is shell-owned and has no flag of its own, but a **contributed** entry is feature code running inside the shell surface — so each contributed entry renders inside a `FeatureHost` keyed on that module's flag. A module that throws while producing its rows degrades to its error card; Settings never blanks. |
| V | No Hardcoding | PASS | PASS | The declarative vocabulary makes this structural: a module supplies label/value/callback, never styling. Tokens are applied once in the shell's renderer. |
| VI | Component Reuse | PASS | PASS with one extension | Renderer is built from existing `ListGroup`/`ListGroupRow`/`SwitchRow`/`SegmentedRow`/`NxCard`. One §5.3 gap must close: `NxTextField` needs its error state for the AI-key row. Extend it — do not add a parallel field. |
| VII | Money Is Exact | PASS | PASS | Settings stores no money. Hide-amounts changes formatting only, never a stored value. |
| VIII | Consent Before Network | PASS | PASS | Settings issues no PostgREST call except the two erasure RPCs, which are already consent-gated by the existing interceptor. Sign-in uses the unauthenticated GoTrue client, which is deliberately not consent-gated (ONB-BR-001). |
| IX | Append-Only History | PASS | PASS | **Load-bearing here**: migrating a row must reuse its existing `SettingsKeys` string. A moved row that gets a new key silently resets every user's preference. Enforced by a test that asserts the key set is a superset of today's. |
| X | Coverage Ratchets | PASS | PASS | New code is mostly pure logic and is well covered; the floor moves up at the phase checkpoint, not before. |
| XI | Stack Is Fixed | PASS | PASS | No new library. Koin's `getAll` and `androidx.biometric` are both already available. |

### Gate remediation — both steps DONE 2026-08-19

1. **SA — done**: surface registry §4 rewritten from the fixed ten-section tree to this three-tier
   control plane (FR-005). Three of the old top-level sections dissolved rather than moved
   (Notifications, Features, Data), each because it was a central list of things modules own; that
   redistribution is recorded in the rewritten section. Two adjacent registry rules were corrected in
   the same pass: §2's channel↔control rule now points at the owning module rather than a central
   notifications section, and §1's Automation row names its new owner. `platform/DESIGN-SYSTEM.md`
   §11 carried the same channel↔row claim as a **global** rule and was corrected identically.
2. **QA — done**: catalog **§13 `SET-*`** written — 50 rows across contribution mechanism (7),
   registry and row behaviour (12), app lock (13), account (5), structure and presentation (13), plus
   a reuse table citing 9 existing `ONB`/`DAT`/`NAV` rows by id. The coverage summary moved to §14 to
   keep the module sections contiguous; inbound references were updated in the same change.

The constitution's step 1→2→3 ordering is satisfied: schema/registry first, scenarios second, code
third. `/speckit-tasks` is unblocked.

## Project Structure

### Documentation (this feature)

```text
apps/finance/specs/004-settings/
├── plan.md              # This file
├── research.md          # Phase 0 output — the 8 decisions this design rests on
├── data-model.md        # Phase 1 output — entities, preference keys, storage placement
├── quickstart.md        # Phase 1 output — how to verify the feature end to end
├── contracts/
│   ├── settings-contribution.md   # what a module publishes; the vocabulary and its rules
│   └── app-lock-gate.md           # the shell checkpoint + hold-and-dispatch contract
├── checklists/
│   └── requirements.md  # spec quality checklist (done)
└── tasks.md             # NOT created by /speckit-plan
```

### Source Code (repository root)

```text
libs/settings/src/main/java/com/dhruv/settings/
├── AppSettings.kt                     # existing — gains appLock fields
├── SettingsKeys.kt                    # existing — gains new keys, never renames old ones
├── SettingsRepository.kt              # existing — unchanged interface, new fields flow through observe()/update()
└── contribution/                      # NEW — the contract every module implements
    ├── SettingsContribution.kt        # entry: moduleKey, title, summary, groups
    ├── SettingsGroup.kt               # submodule grouping
    ├── SettingsRow.kt                 # sealed vocabulary: Toggle/Choice/Stepper/Action/Navigate/Info
    └── SettingsRegistry.kt            # resolves + orders contributions; pure, unit-tested

libs/core/src/main/kotlin/com/dhruv/core/security/
└── AppLockDecision.kt                 # NEW — pure: enabled + timeout + elapsed → LOCKED/UNLOCKED
                                       #   (mirrors navigation/BackContract.kt's decision/effect split)

apps/finance/app/src/main/java/com/dhruv/finance/app/
├── MainActivity.kt                    # gains the lock gate wrapper + held-intent dispatch
├── di/AppModule.kt                    # gains the registry; contributions resolve by type, not by list
└── ui/settings/
    ├── SettingsScreen.kt              # REWRITTEN — top level: quick rows + Account + App + modules tier
    ├── AccountSettingsScreen.kt       # NEW — sign in/out, consent, export, erasure
    ├── AppSettingsScreen.kt           # NEW — appearance, security, notifications master, app details
    ├── ModuleSettingsScreen.kt        # NEW — renders one contribution, FeatureHost-wrapped
    ├── SettingsRowRenderer.kt         # NEW — the one place a row type becomes a component
    ├── AppLockGate.kt                 # NEW — BiometricPrompt host; blocks content until authenticated
    ├── SettingsRows.kt / SettingsDialogs.kt / SettingsPrecisionSheet.kt / SettingsAppearanceSheet.kt
    │                                  # existing — reused, re-homed
    └── SettingsUiState.kt / SettingsViewModel.kt   # existing — split per tier

apps/finance/feature/<bucket>/<module>/src/main/java/com/dhruv/finance/<module>/
└── settings/<Module>SettingsContribution.kt   # NEW per module — data only, registered in its Koin module

apps/finance/app/src/test/java/com/dhruv/finance/app/
├── ui/settings/…                      # renderer + ViewModel tests
└── architecture/DependencyRulesTest.kt  # existing — gains the two new rules below
```

**Structure Decision**: the contract lives in **`:libs:settings`**, not `:libs:core`. Every feature
module already declares `implementation(project(":libs:settings"))`, and `:libs:settings` itself
depends only on `:libs:core` — so the contract is visible to every producer and to the consumer
without a single new edge in the dependency graph. Putting it in `:libs:core` would work too but
would push settings vocabulary into the design-system library, which owns visuals and navigation
primitives, not application configuration.

Two new ArchUnit rules enforce the design:

- No class in `com.dhruv.finance.app.ui.settings` may reference a feature module type — proving the
  modules tier is assembled, not hardcoded (FR-004).
- No `SettingsContribution` implementation may reference a Compose type — proving the vocabulary
  stayed declarative and no module smuggled its own UI into the shared surface (Principle V).

## Phase sequencing within 0b

0b is **five independently shippable sub-phases**, not one merge. 95 tasks in a single slice meant
nothing shipped until all of it worked, and a checkpoint that never arrives is not a checkpoint.
Each sub-phase below ends green on `regressionCheck`, closes its own `SET-*` rows, updates the §14
coverage summary, and merges on its own.

| Sub-phase | Delivers | Stories | Sec pass? |
|---|---|---|---|
| **0b.1** | Control plane + Appearance: contract, registry, renderer, tiers, module entries for what ships today, 19-row migration begins | US1 | light — no off-device data |
| **0b.2** | Account & identity: sign in, sign out, consent relocation, erasure | US2 | **yes** — consent and erasure |
| **0b.3** | App lock & privacy: the gate, auto-lock, hide amounts, notification master | US3 | **yes** — the security surface |
| **0b.4** | Module conventions, assistant consent, AI key, app details | US4, US5, US6 | **yes** — secret handling |
| **0b.5** | Feature-level verification, tap depth, orphan-preference audit, coverage ratchet | — | — |

Dependency shape: `0b.1 → {0b.2, 0b.3, 0b.4} → 0b.5`. The three middle sub-phases are independent of
each other and can be staffed in parallel.

**Two sequencing decisions worth stating**:

1. **Appearance ships in 0b.1**, not with the rest of the App tier. FR-002 makes each quick row a
   mirror of its owning section's row, so theme and accent must arrive with the top level or the
   requirement is unsatisfiable for two sub-phases. App lock's quick row is the deliberate exception
   — preference-only and labelled as such in 0b.1 (FR-043), enforcing in 0b.3.
2. **Recommended order is 0b.1 → 0b.3 → 0b.2 → 0b.4 → 0b.5** if shipping one at a time. A lock that
   enforces nothing is the more dangerous of the two shipped defects: it implies protection that
   does not exist, where a missing sign-out is an absence the user can see.

Roles map onto each sub-phase's own body — SA and QA-author are pre-satisfied (see Gate remediation
above): Backend and Android inside the sub-phase, then QA-close, then Sec where the table says so,
then that sub-phase's checkpoint.

## Complexity Tracking

No constitution violation requires justification. One design choice is worth recording because it
costs something:

| Choice | Why | Simpler alternative rejected because |
|---|---|---|
| Declarative row vocabulary instead of modules supplying composables | Keeps all styling in one renderer (Principle V), makes contributions unit-testable as data, and stops a module smuggling UI into the shared surface | A composable slot is less code up front but reintroduces feature-local styling in the one surface every module touches — exactly what the design system forbids — and makes contributions untestable without Compose |
| Contributions resolved by type from DI rather than an explicit list in `AppModule` | SC-004 demands zero Settings-owned changes when a module is added; an explicit list is a central list wearing a different hat | An explicit list is more obvious to read, but it is the thing FR-004 exists to remove |