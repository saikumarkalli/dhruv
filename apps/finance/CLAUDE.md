# :apps:finance

Finance app (`applicationId = com.dhruv.finance`, app namespace `com.dhruv.finance.app`). Single-activity,
Compose, Koin DI. Phase 4 split the former monolith into feature modules behind `FeatureHost`.

## Docs (read before app-specific work, same spirit as root `CLAUDE.md`'s `platform/` list)
Everything specific to this app's own build — tracker specs, phase plans, the design-v1
functional spec/implementation plan/module standard/QA catalog, this app's own SDD — lives under
`apps/finance/docs/` (`sdd/`, `superpowers/specs/`, `superpowers/plans/`), not the repo-global
`docs/`. Start with `apps/finance/docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md`
(current design source of truth) and `apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`
(current build order). `docs/PRD.md` §2 is still the full cross-app index if you need something else.

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
