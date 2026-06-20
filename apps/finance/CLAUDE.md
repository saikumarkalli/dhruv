# :apps:finance

Finance app (`applicationId = com.dhruv.finance`, app namespace `com.example`). Single-activity,
Compose, Koin DI. Phase 4 split the former monolith into feature modules behind `FeatureHost`.

## Modules
- `:apps:finance:app` — shell: `MainActivity` (pager + bottom nav), Settings UI, `platformModule`/`appModule` Koin wiring, Converter/Finance hubs.
- `:apps:finance:data` — shared Room DB + entities + DAOs + repositories + `CurrencyApi` + `GeminiRepository` + `CurrencyFormatter`. Feature modules depend on this (Repository-only access).
- `:apps:finance:feature:*` — `calculator`, `loans`, `investments`, `tax`, `everyday`, `currency`, `unit`, `date`, `time`, `assistant`.

See [FEATURES.md](FEATURES.md) for per-module detail (screens, ViewModels, data deps, flag keys).

## Feature flags
`platform/feature-flags/dhruv-finance.json` — `com.example.di.PlatformModule.financeFeatureDefaults`
mirrors it field-for-field as a `Map<String, FeatureFlag>` (`enabled` + `minVersion` + `requiresConsent`),
passed to `HardcodedFeatureFlagResolver` with `BuildConfig.VERSION_NAME`. The resolver gates a flag on
`enabled && appVersion >= minVersion`, and exposes `requiresConsent(key)`.
- OFF: `date`, `time`.
- `assistant`: `enabled = true` but **gated to `minVersion 1.2.0`** — current app `versionName` is `1.0`,
  so it is not surfaced yet; `isEnabled("assistant")` flips to true once the app ships ≥ 1.2.0. Also
  `requiresConsent` (DPDP consent gate lives in `AssistantScreen`, state `ConsentNeeded → Idle`).

## Conventions (coding standards)
- **DI = Koin**, not Hilt. Each feature exposes `val <name>Module = module { viewModel { … } }` in its `di/` package; the app aggregates them all in `CalculatorApplication`.
- **Every route is wrapped in `FeatureHost(featureKey, isEnabled = resolver.isEnabled(key), featureError, crashReporter)`** (`:libs:core`). Disabled → `FeatureDisabledCard`; thrown error surfaced via the ViewModel's `featureError: StateFlow<Throwable?>` → `FeatureErrorCard`. Never a blank crash.
- **Every feature ViewModel**: `init { crashReporter.setModule("<key>") }`, exposes `featureError` (set by a `CoroutineExceptionHandler`), and wraps one primary operation in `performanceTracer.trace("<key>_…")`.
- **Module boundaries** (ArchUnit `DependencyRulesTest`): `feature → feature` FORBIDDEN; `feature → data` only (shared `:data`); `core → app` FORBIDDEN.
- **SOLID/altitude**: a feature ViewModel owns one concern; calculation logic is pure (`BigDecimal` for money); data access goes through repositories (exception: `AlarmViewModel`/`BootReceiver` touch Room directly — see CHANGELOG follow-up).

## Build
- `./gradlew :apps:finance:app:assembleDebug`
- Per module: `./gradlew :apps:finance:feature:<name>:assembleDebug`
- Tests: `./gradlew :apps:finance:app:testDebugUnitTest` (includes ArchUnit) and `:feature:<name>:testDebugUnitTest`
- Requires `JAVA_HOME` = Android Studio JBR.

## Phase
Phase 4 complete — feature split done. All modules + app build; unit tests + ArchUnit green.
