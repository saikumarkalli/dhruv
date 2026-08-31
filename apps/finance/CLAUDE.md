# :apps:finance

Finance app (`applicationId = com.dhruv.finance`, app namespace `com.dhruv.finance.app`). Single-activity,
Compose, Koin DI. Phase 4 split the former monolith into feature modules behind `FeatureHost`.

## Docs (read before app-specific work, same spirit as root `CLAUDE.md`'s `platform/` list)
Everything specific to this app's own build — tracker specs, phase plans, the design-v1
functional spec/implementation plan/module standard/QA catalog, this app's own SDD — lives under
`apps/finance/docs/` (`sdd/`, `superpowers/specs/`, `superpowers/plans/`), not the repo-global
`docs/`. Start with `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
(current design source of truth) and `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`
(current build order, §7 has the per-phase spec-kit tracking table). `docs/PRD.md` §2 is still the
full cross-app index if you need something else.

**Spec-kit** (`/speckit-*` skills): this app's formalized specs live in `apps/finance/specs/NNN-slug/`
(one directory per phase — see the implementation plan §7's tracking table for which phase maps to
which number), never the repo-root `specs/` (reserved for genuinely cross-app work — see
`.specify/memory/constitution.md`'s Spec-Kit Directory Rule). Pass
`SPECIFY_FEATURE_DIRECTORY=apps/finance/specs/<dir>` explicitly when running `/speckit-specify`.

[ARCHITECTURE.md](ARCHITECTURE.md) is the detailed engineering reference — module graph and what
enforces it, package layout of `:app` / `:data` / `:libs:core`, navigation and fault isolation, the
Room-vs-Supabase split, the testing stack, and a tripwire list of failures this repo has actually
hit. Read it before a first change to an unfamiliar module.

## Modules
- `:apps:finance:app` — shell: `MainActivity` (pager + bottom nav), Settings UI, `platformModule`/`appModule` Koin wiring, Converter/Finance hubs.
- `:apps:finance:data` — shared Room DB + entities + DAOs + repositories + `CurrencyApi` + `GeminiRepository` + `CurrencyFormatter`. Feature modules depend on this (Repository-only access).
- `:apps:finance:feature:*` — `calculator`, `loans`, `investments`, `tax`, `everyday`, `currency`, `unit`, `date`, `time`, `assistant`. `networth` and the other design-v1 tracker modules (`money`, `planning`, `insurance`, `retirement`, `insights`, `automation`, `onboarding`) are **not yet created** — none is in `settings.gradle.kts`. Planned module topology + build order: `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md` §6–§7.

**Folder layout (2026-08-09):** feature modules are grouped by owning tab under
`apps/finance/feature/<home|money|calc|plan|insights|onboarding|shell>/<name>/` — e.g. `loans` now
lives at `apps/finance/feature/plan/loans/`. **Gradle coordinates are unchanged**
(`:apps:finance:feature:loans` is still `:apps:finance:feature:loans`, remapped via `projectDir` in
`settings.gradle.kts`) — every command below still works exactly as written. See
[apps/finance/feature/README.md](feature/README.md) for the full bucket scheme and rationale.

[FEATURES.md](FEATURES.md) is the module index; each module's own `README.md` (linked from there)
is where the actual detail lives (screens, ViewModels, data deps, flag keys) — one source, not two.

**Tracking rule — every phase closes these three, and its spec carries the task:**
1. **[FEATURES.md](FEATURES.md)** — the module's row moves from *planned* to *enabled*/*disabled*.
   Every module appears there from the moment a phase **plans** it, so "built", "planned" and
   "unowned" are always distinguishable. A phase that adds no Gradle module (0b, 6) gets no row and
   is listed in that file's no-module table instead.
2. **The module's own `README.md`** — drop the "not yet created" preamble, write the real screens,
   ViewModels, data dependencies and flag key. Detail lives only there, never copied back into
   FEATURES.md (that duplication was removed in 2026-08-09 because it drifted).
2a. **The module's `SettingsContribution`** — a module that registers a feature flag ships its own
   Settings entry with it (`specs/004-settings/contracts/settings-contribution.md`, FR-003/FR-004).
   Phase 0b's control plane assembles the Modules tier purely from registered contributions, so a
   module without one is simply **absent from Settings** — silently, with no error and nothing to
   notice in review. That is the failure this line exists to prevent: the 2026-08-22 spec audit
   found four planned phases (001, 002, 003, 006) with no contribution at all. Registration is
   `single(qualifier = named(moduleKey)) { … }` — the qualifier is required, not optional.
3. **Root [`CHANGELOG.md`](../../CHANGELOG.md)** — an entry under the `finance-*` release heading.
   CI injects the heading; the prose under it is hand-written.

4. **The spec's own `spec.md` § "Implementation record"** — what actually shipped, what deviated
   from the spec and why, what was deferred and to where. The spec stops describing the future at
   this point and starts describing the system.

Shared-library work (`:libs:core`, `:libs:settings`) gets a CHANGELOG entry only — it is not a
Finance module and never gets a FEATURES.md row.

**Docs stay current after the phase ships** (constitution Article Xa). Any later change to shipped
behaviour — a defect fix, a functional change, a schema migration, a removal — adds a row to the
owning spec's Implementation record **in the same PR that changes the behaviour**, plus a CHANGELOG
entry, plus any registry row it touches. A defect row names the **FR whose stated behaviour was not
actually delivered**; that is what distinguishes a fix from an undocumented behaviour change.

If a change makes a doc wrong and there is no time to fix it properly, mark it stale with a date.
A confidently wrong document is worse than a missing one — this repo has shipped certificate pins
the code never used and a component library that never existed, both because a doc was trusted.

## Feature flags
`platform/feature-flags/dhruv-finance.json` is the single source of truth — it's packaged as an
Android asset (`assets.srcDirs` in `apps/finance/app/build.gradle.kts`) and loaded at runtime by
`loadFinanceFeatureFlags()` (`PlatformModule.kt` / `di/FeatureFlagAssetLoader.kt`), parsed with
Moshi into a `Map<String, FeatureFlag>` (`enabled` + `minVersion` + `requiresConsent`), then passed
to `HardcodedFeatureFlagResolver` with `BuildConfig.VERSION_NAME`. There is no second hand-written
copy to drift — if the asset is missing or fails to parse, it falls back to a calculator-only
safety map and reports the failure via `CrashReporter`. The resolver gates a flag on
`enabled && appVersion >= minVersion`, and exposes `requiresConsent(key)`.
- OFF: `date`, `time`.
- `assistant`: `enabled = true`, **gated to `minVersion 1.2.0`** — current `versionName` is
  `2.0.2`, so visible. Also `requiresConsent` (DPDP consent gate in `AssistantScreen`).
- `networth`: `enabled = true`, `requiresConsent = true`. **Not yet built** — flag exists, module does not (see the "Modules" note above). Design-v1 Phase 2 builds it.

## Conventions (coding standards)
- **DI = Koin**, not Hilt. Each feature exposes `val <name>Module = module { viewModel { … } }` in its `di/` package; the app aggregates them all in `CalculatorApplication`.
- **Every route is wrapped in `FeatureHost(featureKey, isEnabled = resolver.isEnabled(key), featureError, crashReporter)`** (`:libs:core`). Disabled → `FeatureDisabledCard`; thrown error surfaced via the ViewModel's `featureError: StateFlow<Throwable?>` → `FeatureErrorCard`. Never a blank crash.
- **Every feature ViewModel**: `init { crashReporter.setModule("<key>") }`, exposes `featureError` (set by a `CoroutineExceptionHandler`), and wraps one primary operation in `performanceTracer.trace("<key>_…")`.
- **Module boundaries** (ArchUnit `DependencyRulesTest`): `feature → feature` FORBIDDEN; `feature → data` only (shared `:data`); `core → app` FORBIDDEN.
- **SOLID/altitude**: a feature ViewModel owns one concern; calculation logic is pure (`BigDecimal` for money); data access goes through repositories.

## Build
- `./gradlew :apps:finance:app:assembleDebug`
- Per module: `./gradlew :apps:finance:feature:<name>:assembleDebug`
- Tests: `./gradlew :apps:finance:app:testDebugUnitTest` (includes ArchUnit) and `:feature:<name>:testDebugUnitTest`
- Requires `JAVA_HOME` = Android Studio JBR.

## Design system
**`platform/DESIGN-SYSTEM.md` is the binding contract** — global for every Dhruv app (ADR-0030).
Read it before touching any UI. In short: `LocalDhruvNextColors` / `DhruvNextType` /
`DhruvNextSpacing` / `DhruvNextRadii` tokens + the `:libs:core` component library (`NxCard`,
`NxButton`, `NxTextField`, `SegmentedRow`, `SectionLabel`, `ListGroup`, …); `DhruvBrand` for
theme-invariant brand chrome. Zero `MaterialTheme.colorScheme`/`.typography` refs and zero raw
hex/dp/sp literals in screen files.

Finance's own product spec (screens, business rules, flows) is
`docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`; its route/notification/
intent/settings registries are `docs/superpowers/specs/2026-08-09-finance-surface-registries.md`.

## Phase
Phase 4 complete — feature split done. DhruvNext design system overhaul complete (all 17 production
screen files migrated to tokens + components). All modules + app build; unit tests + ArchUnit green.

Phase 0b (Settings control plane, [004-settings](specs/004-settings/)) — **all five sub-phases
shipped (0b.1-0b.5)**. 0b.1: control plane, contribution mechanism, Appearance, calculator/currency/
unit module entries. 0b.3 and 0b.4 were implemented ahead of 0b.2 at the maintainer's request (both
only depend on 0b.1). 0b.3: a real enforcing app lock (`BiometricPrompt` gate over the whole app,
not just a preference), auto-lock timeout, hide-amounts (screen surface; widget/notification
surfaces deferred — nothing to mask there yet), and the app-wide notifications master switch. 0b.4:
module on/off convention, consent-gated module entries, the first real alert control (currency
daily-rates), durable assistant consent (fixes the old in-memory-flag defect), a personal AI key
row, and App details (version/privacy/licences/source, pluggable update check — no update channel
wired yet). 0b.2: the real Account screen — sign-in/sign-out wired directly to `AuthRepository`
(never onboarding, `SET-ARCH-003`), the three consent switches, erasure with type-to-confirm on
account deletion; replaces 0b.1's `SettingsAccountBody` stand-in (deleted). 0b.5: verification pass
— found and removed 9 orphaned preference keys (per-section accent colors and per-tab enable flags,
both retired by ADR-0024, zero consumers); coverage floor raised 0.09 → 0.14 (measured merged
14.91%, `:libs:settings` 38.38%, `:libs:core` 15.02%). SC-001's on-device migration check and the
theming/TalkBack passes remain deferred — no physical device or emulator was available in the
implementation session; see spec.md's Implementation record.
