# 0010 — Initial repo state (snapshot)

> A point-in-time **snapshot** of the repository structure, kept separate from the decision register
> in [`../DECISIONS.md`](../DECISIONS.md). It records *what existed*, not *why* — rationale lives in
> the ADRs (ADR-0001…0010) and the source-of-truth architecture in [`../PLATFORM.md`](../PLATFORM.md).

## At initialization (`e0e38af` — "initialize finance app structure")

- Monorepo skeleton: `build-logic/` convention plugins (`dhruv.android.application`,
  `dhruv.android.library`, `dhruv.android.compose`), `platform/` docs + contracts, `libs/core`,
  `libs/settings`.
- A single **Finance** app (`applicationId com.dhruv.finance`, namespace `com.example`) as a
  near-monolith: one `FinanceViewModel` / `FinanceScreen` plus a 14-tool `ConverterScreen`; Koin DI;
  Room DB; single-activity Compose NavHost.
- CI pipeline (4 gates: static analysis, security scan, tests, build) and feature-flag JSON
  scaffolding under `platform/feature-flags/`.
- DI is **Koin** (Hilt deferred — its Gradle plugin is incompatible with AGP 9; see ADR-0010).
- `FeatureFlagResolver` was **boolean-only** (`Map<String, Boolean>`); the `minVersion` /
  `requiresConsent` fields already present in the flag JSON were not yet honored in code.

## After Phase 4

- **Finance split into 10 feature modules** under `apps/finance/feature/` — `calculator`, `loans`,
  `investments`, `tax`, `everyday`, `currency`, `unit`, `date`, `time`, `assistant` — plus a shared
  **`:apps:finance:data`** module (Room DB, DAOs, repositories, `CurrencyApi`, `GeminiRepository`,
  `CurrencyFormatter`). Per-module detail in [`../../apps/finance/FEATURES.md`](../../apps/finance/FEATURES.md).
- Every route wrapped in `FeatureHost`; **Converter / Finance hub** screens preserve the original
  pager + bottom-nav UX, each sub-feature independently flag-gated and crash-isolated (ADR-0010).
- **Feature-flag resolver now honors `minVersion` + `requiresConsent`** (was boolean-only):
  `:libs:core` adds a `FeatureFlag` model + a lenient `SemVer`; `HardcodedFeatureFlagResolver` takes
  `Map<String, FeatureFlag>` + the running `versionName` and gates on `enabled && appVersion >= minVersion`.
  `PlatformModule.financeFeatureDefaults` mirrors `dhruv-finance.json` field-for-field.
  - `date` / `time` ship `enabled = false`.
  - `assistant` ships `enabled = true` but **gated to `minVersion 1.2.0`** (current `versionName` is
    `1.0`, so it stays hidden until the app ships ≥ 1.2.0) and `requiresConsent` (DPDP consent gate in
    `AssistantScreen`). Covered by `HardcodedFeatureFlagResolverTest`.
- ArchUnit `DependencyRulesTest` enforces `feature → feature` isolation; each feature VM does
  `crashReporter.setModule(...)` + one `performanceTracer.trace(...)` and exposes `featureError`.
- Docs reconciled to Koin (not Hilt); `GeminiRepository` moved to `:data` and takes its API key as a
  constructor arg so it is shared by `calculator` and `assistant` without a `feature → feature` edge.

_Source of truth: [`../PLATFORM.md`](../PLATFORM.md). Decisions: [`../DECISIONS.md`](../DECISIONS.md)
(esp. ADR-0010). Change log: [`../../CHANGELOG.md`](../../CHANGELOG.md)._
