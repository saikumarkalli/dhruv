# :apps:finance:app

The Finance application shell. Owns the single activity, the tab shell, navigation resolution, Koin
aggregation, and the screens that belong to no feature module.

- **Gradle coordinate:** `:apps:finance:app`
- **`applicationId`:** `com.dhruv.finance` · **namespace:** `com.dhruv.finance.app`
- **Depends on:** every feature module, `:apps:finance:data`, `:libs:core`, `:libs:settings`.
  Nothing depends on it — `core → app` is forbidden and ArchUnit enforces it.

## What lives here

- **`MainActivity`** — single activity, pager over the visible tabs, each hosting its own nested
  `NavHost` for drill-in routes.
- **Shell-owned screens** — splash, the Home tab root (01), Settings, Profile, the app-switcher
  sheet. Phase 6 adds the notification centre (B2) and global search (B3) here too, which is why
  that phase creates no Gradle module.
- **Hubs** — Converter and Finance hub screens, each sub-feature wrapped in `FeatureHost`.
- **`CalculatorApplication`** — aggregates every feature's Koin `module {}`. A new feature module is
  not wired until its module object is added here.
- **`platformModule` / `appModule`** — platform bindings and app-level dependencies.
- **ArchUnit tests** (`src/test/kotlin/.../arch/`) — `DependencyRulesTest` is the enforcement point
  for every module-boundary rule in `PLATFORM.md` §4.

## Navigation

Five tab roots: **Home · Money · Calc · Plan · Insights** (ADR-0027). Settings is reached from the
top bar, never a tab (design-system §6, N5).

`TabKey` resolution is by key, not position, so hiding a tab behind a flag cannot shift another
tab's target. Back-press precedence is encoded once in `resolveBackAction`
(`:libs:core` `navigation/BackContract.kt`) — detail route → active tab's nested stack → first tab →
exit. Never re-derive that order inline.

**Cross-feature navigation is by id, never by class reference.** A feature never imports another
feature's screen; a `NavTarget` names *where* and this shell resolves it. Adding a route means
adding the sealed subtype **and** the row in the surface registry — both, in the same change.

## Fault isolation

Every route is wrapped in `FeatureHost(key, isEnabled, featureError, crashReporter)`. A flag-off
renders `FeatureDisabledCard`; a thrown error renders `FeatureErrorCard` tagged with the module.
**Never a blank crash** — this is a `PLATFORM.md` §4 rule, not a nicety, and it applies to every
route rather than only to tab roots.

## Feature flags

`platform/feature-flags/dhruv-finance.json` is the single source of truth. It is packaged as an
Android asset and loaded by `loadFinanceFeatureFlags()`, parsed with Moshi into
`Map<String, FeatureFlag>` (`enabled` + `minVersion` + `requiresConsent`), then passed to
`HardcodedFeatureFlagResolver` with `BuildConfig.VERSION_NAME`. There is no second hand-written copy
to drift. If the asset is missing or unparseable it falls back to a calculator-only safety map and
reports via `CrashReporter`.

## Build

```
./gradlew :apps:finance:app:assembleDebug
./gradlew :apps:finance:app:testDebugUnitTest    # includes ArchUnit
./gradlew regressionCheck                        # the pre-merge gate CI runs
```

Requires `JAVA_HOME` = Android Studio JBR (JDK 17+).

Module index: [../FEATURES.md](../FEATURES.md).