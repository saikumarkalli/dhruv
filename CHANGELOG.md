# Changelog

All notable changes to the **Dhruv** monorepo are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**Headings are namespaced per component** — `finance-*` is the Android app, `web-finance-*` is the
web SPA. Each component versions **independently**: a web-only release does not move the Android
version. The heading for a release is injected by CI; the prose underneath is written by hand.
See [`platform/VERSIONING.md`](platform/VERSIONING.md).

## [Unreleased]

### Added
- Settings control plane, sub-phase 0b.1 (`apps/finance/specs/004-settings/`): a declarative
  `SettingsContribution` contract (`:libs:settings`) modules register into by Koin qualifier, a
  `SettingsRegistry` resolving/filtering/ordering them, and a shell renderer
  (`SettingsRowRenderer`/`ModuleSettingsScreen`) building every row from existing `:libs:core`
  components. Settings' top level now shows quick rows (theme/accent/app-lock) → Account → App →
  a modules tier assembled with zero Settings-file edits per new module (verified live).
- Calculator, currency and unit each ship their first `SettingsContribution`, reachable from the
  new modules tier.
- Two new ArchUnit rules (`DependencyRulesTest`): Settings may not reference a feature-module type;
  a `SettingsContribution` factory may not reference a Compose type.
- Settings control plane, sub-phase 0b.3 (`apps/finance/specs/004-settings/`): a real, enforcing
  app lock — a `BiometricPrompt` (Class 3, device-credential fallback) gate wrapping the entire
  app above the pager and every tab, resolved before the first content frame so no unlocked frame
  ever appears on cold start. Auto-lock timeout (Immediate/1/5/15 min). Hide-amounts masking at the
  shared money-formatting path (`Paise`, `MoneyText`, `CurrencyFormatter`) — screen surface only for
  now. An app-wide notifications master switch with a system-permission-denied banner. Links,
  notification taps and shortcuts arriving while locked are held and dispatched once after unlock.
  `MainActivity` migrated `ComponentActivity` → `FragmentActivity` for `BiometricPrompt`.
- Settings control plane, sub-phase 0b.4 (`apps/finance/specs/004-settings/`): every optional
  module now offers a real on/off control (`module_enabled_<moduleKey>`) that retains its other
  stored preferences when off. A module whose controls need an ungranted consent states which one
  and offers the route to grant it, instead of showing inert controls. The currency converter ships
  the first real notification alert control (daily rates, plus its own Android notification
  channel) — a new `NotificationChannelRegistry` (`:libs:core`) keeps the channel-to-control mapping
  1:1. The assistant gets its own Settings entry: consent status/re-consent, and a personal Gemini
  API key row (`SettingsRow.SecretText`, a new closed row type) that's always shown masked and
  removable in one action — the masked value is a fixed constant, never derived from the real key,
  so it structurally cannot leak length or characters. App details replaces the old "About Dhruv
  Finance" row: version + build number, privacy policy/licences/source (linking this now-public
  repo's real files), and a pluggable update-check surface (no update channel is wired yet, so the
  row is absent, not inert). A new ArchUnit rule enforces that a module's own Settings row can never
  reach app lock, consent or the secret-key store directly — only through `SettingsRepository`.
- Settings control plane, sub-phase 0b.2 (`apps/finance/specs/004-settings/`): Account now has a
  real, working sign-in (Google, via Credential Manager, wired directly to the auth repository —
  never through first-run onboarding) and a real sign-out, fixing the two defects this sub-phase
  existed for (sign-in previously only lived inside first-run setup; there was no sign-out at all).
  Account erasure now requires typing "DELETE" to confirm (`ConfirmDangerDialog` gained a
  `typeToConfirmText` option). The non-functional "Export my data" placeholder row is gone entirely
  — no financial-records repository exists yet to export from.
- Settings control plane, sub-phase 0b.5 (`apps/finance/specs/004-settings/`): a repo-wide
  preference audit found and removed 9 orphaned `SettingsKeys` (5 per-section accent colors and 4
  per-tab enable flags, both retired when the design system moved to a single global accent and a
  4/5-tab shell — zero consumers remained anywhere in the app). The merged JVM test-coverage floor
  was raised from 9% to 14% to match measured coverage (`:libs:settings` 38%, `:libs:core` 15%).

- **Net-worth tracker** (`:apps:finance:feature:networth`, design-v1 Phase 2 —
  [001-net-worth-tracker](apps/finance/specs/001-net-worth-tracker/)): Home's net-worth overview
  (C1, donut + ranked-by-sector legend), assets (C2) and liabilities (C6) lists, holding detail
  (C3, value-history trend + simple return), add/edit holding (C4) and add/correct-value (C5)
  forms, and liability detail (C7, amortisation split + prepay-savings projection). Soft-delete +
  5-second undo for holdings; a Settings entry stating the frozen sector/liability-type counts.
  Behind the `networth` flag (`requiresConsent: true`, already provisioned in Phase 1).
  - **Two security-definer RPCs**, not plain PostgREST inserts, because the operations they replace
    aren't safely expressible as one: `finance.create_holding_with_value()` writes a holding and
    its first valuation in one transaction (no orphan holding on a failed second insert) and
    replays idempotently on `request_id`; `finance.correct_valuation()` is the *only* way a
    valuation is ever amended — it soft-deletes the wrong row and appends the corrected one
    (`source = 'CORRECTION'`) in one transaction, because `finance.valuations` carries no UPDATE
    policy or grant at all. This is a genuine behaviour surprise for anyone assuming client-side
    UPDATEs work against this table: they don't, by design, and never will.
  - `finance.valuations.as_of` gained a `CHECK (as_of <= current_date)` — a future-dated valuation
    is rejected server-side, not just left unvalidated.
  - Every view PostgREST exposes over this schema (`v_latest_valuation`, `v_net_worth_by_sector`,
    and the new `v_net_worth_history` trend view) carries `security_invoker = on` — without it, a
    Postgres 15+ view runs as its owner and bypasses RLS, silently returning every user's rows to
    every signed-in caller. Also worth knowing: `holdings.invested_paise` (nullable cost basis,
    funds C3's "Simple return", explicitly not XIRR) and the frozen `sector`/`liability_type`/
    `source` enums are DB `CHECK` constraints, not client-side validation — adding a new value to
    any of them is a migration, by design (BR-C3).
  - New Gradle verification gate `checkDesignTokenUsage`: fails on raw
    `MaterialTheme.colorScheme`/`.typography` or a hex `Color(0x...)` literal anywhere under
    `apps/finance/feature/**`, wired into `regressionCheck`.
  - **Known gaps, not fixed in this feature's own scope**: no edit-liability screen
    (`LiabilityRepository.updateMeta()` exists and is tested, nothing in the UI calls it yet); the
    schema migration and the RLS/RPC verification scripts authored for it
    (`supabase/verification/`) have never run against a live Supabase project — no credentials in
    any authoring session so far. See `apps/finance/specs/001-net-worth-tracker/data-model.md`
    § "DB readiness" for the exact unblock steps.
  - Home's greeting (`HOM-UI-001`) now appends the signed-in user's first name when the Google
    profile provides one (`Good Evening, Sai`, name rendered in the user's own selected accent
    color — `LocalDhruvNextColors.current.acc`, ADR-0024 §2 — never a hardcoded color), falling
    back to the bare greeting when signed out or unavailable — `firstNameFrom` (`HomeViewModel.kt`),
    first token of `SessionState.Active.displayName` only.

### Fixed
- The app-lock preference toggle previously wrote a setting nothing read — enabling it changed no
  app behaviour at all. It is now a real, enforcing gate (see Added, sub-phase 0b.3).
- A module's Settings entry could corrupt Compose's slot table: the consent flag was read by a
  `@Composable` call placed inside a `when` branch condition behind a short-circuiting `&&`, so it
  was invoked a different number of times depending on whether the module was turned off. Both
  reads are now hoisted above the branch.
- `AppDetailsViewModel` was constructed with `remember {}` rather than resolved as a ViewModel, so
  its `viewModelScope` was never cancelled — a coroutine leak the moment a real update-checker is
  wired. It is now provided by Koin.
- The "app lock turned off — no screen lock is set" notice could repeat on a quick
  background/foreground cycle, because the condition guarding it read a preference whose reset was
  still in flight.
- Google Sign-In nonce generation was duplicated between first-run sign-in and Settings › Account.
  Both now share one implementation in `:libs:core`, with tests pinning the raw-vs-SHA-256
  distinction that silently breaks the token exchange when inverted.
- Turning a module off did nothing beyond changing its own settings screen — the preference had no
  reader, so the module stayed in the app. A turned-off module is now genuinely removed from
  content. The control is also no longer offered for the calculator, which is the Calc tab's own
  content and must not be user-hideable.
- The "notifications are turned off in system settings" banner never refreshed after the user
  followed it to system settings and came back — the one journey it exists to support.
- "Delete my data" / "Delete my account" were tappable while signed out, producing a failure that
  looked like a server problem. They now show a signed-out state instead.
- The personal AI key row claimed it "bypasses the shared quota"; the saved key is stored securely
  but not yet used by the assistant. The row now says so rather than promising otherwise.
- `DependencyRulesTest`'s test-class exclusion (`ImportOption.DoNotIncludeTests()`) never matched
  this project's actual unit-test output path — every existing ArchUnit rule had silently been
  importing test classes all along. Replaced with a working exclusion predicate.
- The assistant's DPDP consent flag was held in memory only and forgotten on every restart, so
  users were re-asked forever. It now persists through `SettingsRepository` and survives a
  force-stop (sub-phase 0b.4).

## [finance-2.0.4] - 2026-08-16

### Changed
- `kotlin-and-compose` dependency group bumped (7 updates).

## [finance-2.0.3] - 2026-08-16

### Added
- GitHub spec-kit (v0.16.4) installed; Dhruv constitution written.
- Phase 2 net-worth spec-kit artifacts (spec, plan, tasks).

### Changed
- `compileSdk` / `targetSdk` raised to 37, unblocking the androidx/Compose dependency updates.
- Dependency bumps: roborazzi, `@testing-library/jest-dom`, the web `tooling` and `react` groups,
  `actions/create-github-app-token`, `actions/upload-artifact`.

### Removed
- Orphaned Finance P1–P6 / R0–R11 specs retired; the forward plan consolidated onto design-v1
  (see the doc-retirement note in `platform/DECISIONS.md`).

## [finance-2.0.2] - 2026-08-01

### Added
- Calculator engine scenarios and feature view models with observability support.
- Core finance architecture and the initial feature modules with screen definitions.
- Temperature and Area conversion engines (TDD).
- DhruvNext strong chip variant and `NxTopBar` trailing action in `:libs:core`.
- Unit-converter and calculator-engine test suites.

### Changed
- DhruvNext §6.6 rebuilds of the unit and currency screens; Calc keypad restyle with a DhruvNext
  title bar (§6.3).

### Fixed
- `Pill` strong-variant font weight now matches spec.

## [finance-2.0.1] - 2026-07-27

First tagged release of the 2.x line. Content is the DhruvNext shell rebuild described under
`finance-2.0.0` below — that entry was written before the release was cut, and the tag landed as
`2.0.1`. No `dhruv-finance-v2.0.0` tag exists.

## [web-finance-0.1.0] - 2026-07-25

### Added
- Vite + React SPA scaffold under `web/` (ADR-0015), deployed to Vercel. Shares
  `platform/feature-flags/dhruv-finance.json` with the Android app and the Supabase backend.

This is the web app's own version line and is unrelated to `finance-*` above (VERSIONING.md D1).

## [finance-2.0.0] - 2026-07-26 - DhruvNext 4-tab shell rebuild

Major/breaking architecture release — the navigation model itself changed. See ADR-0024
(`platform/DECISIONS.md`).

### Added
- **DhruvNext 4-tab shell** (Home · Calc · Plan · Insights) replacing the old 3-tab pager. Plan's
  loan/invest/tax/everyday drill-in now lives in its own nested `NavHost`, keeping the Plan tab
  highlighted while sub-routes navigate.
- **`NavTarget`/`TabKey`/`PlanTool` cross-tab navigation contract** (`:libs:core/navigation`) +
  **`NavigationDispatcher`** (`:apps:finance:app/navigation`, Koin singleton) — the sole
  cross-feature navigation mechanism (NAV1); resolves a target's tab by stable key, not pager
  position, so a flag flip can't point navigation at the wrong page (NAV4).
- Shell-level **detail-route overlay** (Settings/Ask/Currency/Units/Date/Time/Profile/
  Notifications) rendered full-screen with a back top bar and no tab bar, per DhruvNext §5's
  OWNER model, plus an app-switcher bottom sheet (Finance OPEN, Tools/Vault SOON).
- **DhruvNext component library** (`:libs:core/ui/components`) and token set
  (`DhruvNextColors`/`DhruvNextRadii`/`DhruvNextSpacing`) — `NxCard`, `ListGroup`, `BottomBar`,
  `AskPill`, `EmptyStateCard`, `DhruvModalSheet`, and others, all theme-driven via
  `LocalDhruvNextColors`.
- Settings restyled to the new IA (Account / Appearance / Money / Privacy & data / App), including
  a global 4-swatch accent picker (orange/green/blue/purple).

### Changed
- Default global accent color → DhruvNext orange `#F05A28` (was Dhruv gold `#D4AF37`, which was
  already dead behind the old per-tab `SectionTheme` override and never actually rendered).

### Removed
- **`SectionTheme`/`getAccentColor`** (`:libs:core`) — the per-domain accent system (ADR-0014 §8),
  retired in favor of one global theme accent (ADR-0024 decision 2). Zero remaining callers,
  verified before deletion.
- **`ui/hub/FeatureHubs.kt`** (`ConverterHub`/`FinanceHub`) — superseded by the new shell's Plan tab
  and Calc-tab detail routes.
- Four Settings toggles ("Show Converter/Date & Time/Finance/Time Tools in bottom navigation")
  that no longer mapped to anything in the new nav model — DhruvNext's tabs are fixed system
  destinations, not user-toggleable, so the controls were actively misleading rather than merely
  unused.

## [Unversioned] - Phase 4: Finance feature split

> This work shipped across the 1.2.x line but never got its own entry, and the heading sat as
> `[Unreleased]` *below* an already-released version. Retitled rather than assigned a number:
> guessing which release contained it would be fabrication. Left in chronological position.

### Added
- Split the Finance monolith into **10 feature modules** under `apps/finance/feature/` (`calculator`, `loans`, `investments`, `tax`, `everyday`, `currency`, `unit`, `date`, `time`, `assistant`) plus a shared **`:apps:finance:data`** module (Room DB, DAOs, repositories, `CurrencyApi`, `GeminiRepository`, `CurrencyFormatter`).
- Every route wrapped in `FeatureHost` (disabled → `FeatureDisabledCard`, error → `FeatureErrorCard`); `FeatureFlagResolver` gating from `platform/feature-flags/dhruv-finance.json`; Converter & Finance **hub** screens in the app shell.
- `PerformanceTracer` (`:libs:core`) Firebase Performance wrapper; each feature VM does `crashReporter.setModule(...)` + one `performanceTracer.trace(...)` + exposes `featureError`.
- `platform/skills/dhruv-feature-scaffold/SKILL.md` (Koin-based scaffold procedure).
- ArchUnit `DependencyRulesTest` now enforces real `feature → feature` isolation via package slices.
- **CI auto-tagging** (`ci.yml` `auto-tag` job): after a merge to `develop`/`main`, reads each active app's version from `platform/versions.json` and creates `dhruv-<app>-v<version>` idempotently — develop and main share the **same** version tag (created once, reused on promotion). The tag push triggers the Release workflow. Requires a PAT in the `RELEASE_TOKEN` secret (tags pushed with the default `GITHUB_TOKEN` do not trigger other workflows); falls back to `GITHUB_TOKEN` (tag created, Release run started manually).
- **PR CI summary comment** (`ci.yml` `pr-summary` job): posts/updates a single sticky comment on every PR run summarizing all 4 gate results (previously only GitLeaks ever commented, and only on a detected secret). Branded under a dedicated **"Dhruv Bot"** GitHub App identity (`actions/create-github-app-token@v1`, `DHRUV_BOT_APP_ID`/`DHRUV_BOT_PRIVATE_KEY` secrets, `Issues: Read & write` only); falls back to the default `github-actions[bot]` token if the App token mint fails. Informational only — not a required check. See ADR-0012.

### Changed
- **CI auto-version-bump now increments the patch version on every merge** (`ci.yml` `version-bump` job). Previously the job only incremented `versionCode` (build number) and left the semantic version in `platform/versions.json` static, which caused `auto-tag` to skip tag creation after the first merge (the tag already existed). Now every merge to `develop`/`main` atomically increments `MAJOR.MINOR.PATCH+1` for all active apps, syncs `VERSION_NAME` into `gradle.properties` (so the APK and `BuildConfig.VERSION_NAME` are always accurate), and commits all changes back with `[skip ci]`. Major and minor versions are still bumped manually. See ADR-0011.

### Changed
- **DI corrected to Koin everywhere** (the Hilt Gradle plugin is incompatible with AGP 9). Stale "Hilt only" wording updated in `CLAUDE.md` and `PLATFORM.md`; see ADR-0010.
- `GeminiRepository` moved to `:apps:finance:data` and now takes the API key as a constructor parameter (app supplies `BuildConfig.GEMINI_API_KEY` via Koin) so it no longer depends on the app's `BuildConfig` — keeps it shareable by `calculator` and `assistant` without a `feature → feature` edge.
- Thematic grouping of the 10 original Finance calculators into `loans`/`investments`/`tax`/`everyday` (supersedes the originally-proposed separate `emi`/`sip`/`loan` modules).
- **Feature-flag resolver now honors `minVersion` + `requiresConsent`** (previously boolean-only). `:libs:core` adds a `FeatureFlag` model + lenient `SemVer`; `HardcodedFeatureFlagResolver` takes `Map<String, FeatureFlag>` + the running `versionName` and gates on `enabled && appVersion >= minVersion`. `PlatformModule.financeFeatureDefaults` now mirrors `dhruv-finance.json` field-for-field. Effect: `assistant` is `enabled = true` but stays hidden until the app ships ≥ `1.2.0`. Covered by `HardcodedFeatureFlagResolverTest`.

### Removed / relocated tests
- Relocated `CalculatorEngine` tests → `:feature:calculator`; converter/formatter tests → `:feature:unit`.
- Removed `FinanceViewModelTest`, `FinanceViewModelEdgeCaseTest`, `SddSpecFirstTests` (bound to the now-split monolithic `FinanceViewModel`). **Follow-up:** re-author per-module calc tests using `NoOpCrashReporter` + `NoOpPerformanceTracer`.

### Fixed
- **Launch crash when Firebase is not configured.** With no `google-services.json`, `FirebaseApp` is uninitialized, so the first `CrashlyticsReporter.setModule(...)` (invoked while Koin built `SettingsRepositoryImpl` at startup) threw `IllegalStateException` and killed the app before any UI. `CrashlyticsReporter` and `FirebasePerformanceTracer` (`:libs:core`) now resolve Firebase defensively (`runCatching`) and degrade to no-op when it's absent — observability never crashes the app (PLATFORM.md §4). Add `google-services.json` to enable real Crashlytics/Performance.

### Removed
- **Alarm sub-feature** dropped from the `time` module (Stopwatch and Timer remain). Removed `AlarmScreen`/`AlarmViewModel`/`AddAlarmSheet`/`MathPuzzleActivity`, the `service/alarm/` scheduler/receiver/service classes, and the shared `AlarmEntity`/`AlarmDao` (Room migration `MIGRATION_4_5` drops the now-unused `alarms` table). This also removes the module's only AndroidManifest component registrations (`Activity`/`BroadcastReceiver`s/`Service`), its only Room dependency, and six device permissions (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `USE_FULL_SCREEN_INTENT`) — and with it, the `BootReceiver`/`AlarmViewModel` repository-bypass exception noted below.

### Follow-ups / known debt
- `date` and `time` ship flag-disabled (code preserved); `assistant` is `enabled = true` but version-gated (`minVersion 1.2.0`) + `requiresConsent`, so it surfaces only once the app ships ≥ 1.2.0.
- `detekt` is not wired into the build (`./gradlew detekt` task is absent — the `dhruv.detekt` convention plugin is applied by no module). Pre-existing; wire up separately.
- The legacy `dhruv-compose-screen` SKILL is stale (shows Hilt / `DhruvTheme.colors` / `crashReporter.report`); `dhruv-feature-scaffold` is authoritative for the real Koin patterns.
- App `build.gradle.kts` still lists unused Room/network/Moshi/KSP deps (harmless) — prune in a cleanup pass.

## [finance-1.1.0] - 2026-06-07

### Added

- **Automated CI/CD Pipeline**: GitHub Actions workflow to automatically build, sign, and release APK files upon pushing tags to the repository.
- **Testing Gate**: CI/CD pipeline now strictly requires all unit tests (`./gradlew testDebugUnitTest`) to pass before building or allowing a merge to `main`.

- **Dynamic Versioning & Naming**: CI/CD dynamically generates `VERSION_NAME` based on the previous released tag history (`git describe`). Built APK files are automatically renamed dynamically based on this version (e.g., `DhruvCalc-v1.2.0-beta.apk`) using `base.archivesName.set()`.
- **Security Protocols**: Implemented Base64 Keystore injection via GitHub Secrets to prevent credential leakage. Added `*.jks` to `.gitignore`.
- **Knowledge Hub**: Added `06_ci_cd_deployment.md` outlining the CI/CD and deployment strategy.

### Changed

- **AAB Generation Disabled**: The `bundleRelease` task has been removed from the CI workflow as a fallback action until the App Bundle is fully ready for Play Store deployment.
- Refactored `build.gradle.kts` release signing config to read version details securely from Gradle project properties `-PVERSION_CODE` and `-PVERSION_NAME`.

### Fixed

- **CI/CD Build Crash**: Resolved `packageRelease` pipeline failure caused by a missing keystore password. The release signing config in `build.gradle.kts` now safely aborts signing and produces an unsigned APK if the `STORE_PASSWORD` is omitted, rather than crashing the CI.

## [finance-1.0.0] - Initial Structure
- Initial project scaffolding and foundational architecture setup.
- Basic functional capabilities documented in `knowledge_hub`.
