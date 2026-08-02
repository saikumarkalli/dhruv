# :apps:finance

Finance app (`applicationId = com.dhruv.finance`, app namespace `com.dhruv.finance.app`). Single-activity,
Compose, Koin DI. Phase 4 split the former monolith into feature modules behind `FeatureHost`.

## Modules
- `:apps:finance:app` — shell: `MainActivity` (pager + bottom nav), Settings UI, `platformModule`/`appModule` Koin wiring, Converter/Finance hubs.
- `:apps:finance:data` — shared Room DB + entities + DAOs + repositories + `CurrencyApi` + `GeminiRepository` + `CurrencyFormatter`. Feature modules depend on this (Repository-only access).
- `:apps:finance:feature:*` — `calculator`, `loans`, `investments`, `tax`, `everyday`, `currency`, `unit`, `date`, `time`, `assistant`, `networth` (scaffolded, screens pending).

See [FEATURES.md](FEATURES.md) for per-module detail (screens, ViewModels, data deps, flag keys).

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
- `networth`: `enabled = true`, `requiresConsent = true`. Module scaffolded; screens pending (R2).

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
All screens use the **DhruvNext design system** (ADR-0024): `LocalDhruvNextColors`, `DhruvNextType`,
`DhruvNextSpacing`, `DhruvNextRadii` tokens + `NxCard`/`NxButton`/`NxTextField`/`SegmentedRow`/
`SectionLabel`/`ListGroup` components from `:libs:core`. Zero `MaterialTheme.colorScheme`/
`.typography` refs remain in screen files. See [FEATURES.md](FEATURES.md) design system section.

## Phase
Phase 4 complete — feature split done. DhruvNext design system overhaul complete (all 17 production
screen files migrated to tokens + components). All modules + app build; unit tests + ArchUnit green.
