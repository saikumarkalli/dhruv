# Dhruv — Android Monorepo

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat-square)](#)
[![Language](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg?style=flat-square)](#)
[![UI](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4.svg?style=flat-square)](#)
[![DI](https://img.shields.io/badge/DI-Koin-orange.svg?style=flat-square)](#)
[![minSdk](https://img.shields.io/badge/minSdk-26-blue.svg?style=flat-square)](#)
[![Finance](https://img.shields.io/badge/finance-v1.2.5-blue.svg?style=flat-square)](#)

A single-repository, **multi-app Android ecosystem** in Kotlin + Jetpack Compose. Multiple apps
share a common `:libs:core` and `:libs:settings`, with strict module-boundary isolation, per-feature
fault containment, feature flags, security/compliance baked in, and a fully automated
build → version-bump → tag → GitHub-Release pipeline.

> **This README is the high-level map.** The *authoritative* sources of truth are the docs under
> [`platform/`](platform/). If this README ever disagrees with them, the `platform/` docs win.
> - [`platform/PLATFORM.md`](platform/PLATFORM.md) — architecture (the *what*)
> - [`platform/DECISIONS.md`](platform/DECISIONS.md) — decision register / ADRs (the *why*)
> - [`platform/AGENTS.md`](platform/AGENTS.md) — rules for any AI/agent session
> - [`platform/Implementation.md`](platform/Implementation.md) — phased implementation plan (Phase 0–7)
> - [`CHANGELOG.md`](CHANGELOG.md) — chronological change history

---

## Table of contents

1. [What Dhruv is](#1-what-dhruv-is)
2. [Current status snapshot](#2-current-status-snapshot)
3. [Repository topology](#3-repository-topology)
4. [Architecture](#4-architecture)
5. [The Finance app — features](#5-the-finance-app--features)
6. [Module reference](#6-module-reference)
7. [Tech stack](#7-tech-stack)
8. [Security (8 layers)](#8-security-8-layers)
9. [Compliance (DPDP)](#9-compliance-dpdp)
10. [AI strategy](#10-ai-strategy)
11. [Build & run](#11-build--run)
12. [CI/CD pipeline](#12-cicd-pipeline)
13. [Versioning & releases](#13-versioning--releases)
14. [Feature flags](#14-feature-flags)
15. [Testing](#15-testing)
16. [Conventions for adding code](#16-conventions-for-adding-code)
17. [Roadmap](#17-roadmap)
18. [Known debt & follow-ups](#18-known-debt--follow-ups)

---

## 1. What Dhruv is

Dhruv is a **single repo** (`dhruv`) housing a family of Android apps built on a shared platform.
Module boundaries (enforced by Gradle + ArchUnit) provide the isolation that separate repos would —
without the publish/version/submodule overhead ([ADR-0001](platform/DECISIONS.md)).

| App            | Gradle path          | Status   | Purpose                                                                 |
|----------------|----------------------|----------|-------------------------------------------------------------------------|
| **Finance**    | `:apps:finance`      | **active** | Calculator, converters, loans/investments/tax/everyday planners, currency, units, date/time, AI assistant |
| Tools          | `:apps:tools`        | planned  | Notes, Clipboard, Timer, QR, Weather, AI assistant                      |
| Vault          | `:apps:vault`        | future   | Password manager, E2E-encrypted, biometric                              |
| Health         | `:apps:health`       | future   | —                                                                       |
| Relationship   | `:apps:relationship` | future   | —                                                                       |
| Web sync hub   | (separate, later)    | future   | Official site + cross-device sync                                       |

Only **Finance** has code today. Tools and Vault are reserved in `platform/versions.json` and the
roadmap; their scaffolding is the next planned work.

---

## 2. Current status snapshot

- **Active app:** Finance — `applicationId = com.dhruv.finance`, version **1.2.5** (`versionCode 7`).
- **Phase reached:** Phase 4 complete (the Finance monolith has been split into feature modules).
  See [Roadmap](#17-roadmap) for the full phase map.
- **Build:** all modules + app build; unit tests + ArchUnit dependency rules green.
- **Distribution:** signed release APK attached to a GitHub Release on every merge to `develop`
  (Play Store / AAB deferred — [ADR-0008](platform/DECISIONS.md)).
- **Default branch:** `develop`. `main` is reserved for a future Play launch.

---

## 3. Repository topology

```
dhruv/
├── settings.gradle.kts          # includes every module
├── build.gradle.kts             # thin root
├── gradle.properties            # VERSION_CODE / VERSION_NAME (CI-owned)
├── gradle/libs.versions.toml    # central version catalog
├── build-logic/                 # Gradle convention plugins (shared Android/Compose/Koin/detekt config)
│   └── src/main/kotlin/
│       ├── dhruv.android.application.gradle.kts
│       ├── dhruv.android.library.gradle.kts
│       ├── dhruv.android.compose.gradle.kts
│       ├── dhruv.detekt.gradle.kts            # (defined; not yet applied — see Known debt)
│       └── dhruv.hilt.gradle.kts              # (defined; NOT applied — Koin is used, see ADR-0010)
├── config/detekt/detekt.yml
├── platform/                    # DOCS & CONTRACTS ONLY — no code lives here
│   ├── PLATFORM.md  DECISIONS.md  AGENTS.md  Implementation.md  RUNBOOK.md
│   ├── CLAUDE-MD-TEMPLATES.md  TELEGRAM_BOT.md
│   ├── versions.json            # per-app version + compatibility matrix
│   ├── contracts/DhruvEntity.kt # the cross-app entity contract
│   ├── feature-flags/<app>.json # flag schema mirrored in Firebase Remote Config
│   ├── adr/                     # individual ADR files
│   └── skills/                  # task playbooks (feature-scaffold, room-entity, compose-screen, module-audit, release)
├── libs/
│   ├── core/                    # :libs:core      — design system, FeatureHost, flags, security, observability, DhruvEntity
│   └── settings/                # :libs:settings  — encrypted settings + settings UI sections
├── apps/
│   ├── finance/
│   │   ├── app/                 # :apps:finance:app   — shell, MainActivity, hubs, settings, Koin wiring
│   │   ├── data/                # :apps:finance:data  — Room DB, DAOs, repositories, CurrencyApi, GeminiRepository
│   │   └── feature/             # 10 feature modules (see Module reference)
│   └── shared/assets/brand/     # shared brand assets
├── Dhruv_Master_Brand_Kit/      # full multi-platform icon/wordmark set (Android/iOS/web/win/macOS)
├── scripts/
│   ├── bump-version.sh
│   └── hooks/pre-push           # local pre-push compliance checks
└── .github/workflows/           # ci.yml, release.yml, fast-feedback.yml
```

**Dependency direction:** `apps:* → libs:settings → libs:core` and `apps:* → libs:core`. Feature
modules depend on `:libs:core` (+ `:apps:finance:data` via Repository only), **never on each other**.
`:libs:core` depends on nothing internal.

---

## 4. Architecture

**Single-activity, MVVM, Compose, Koin DI, Room + EncryptedDataStore.**

### Fault isolation (the strongest part of the design)

A feature crash isolates to that feature; the app shell never goes blank. Only a `:libs:core`
failure may show an app-level fallback.

- Every visible route is wrapped in **`FeatureHost(key, isEnabled, featureError, crashReporter)`** (`:libs:core`).
- `FeatureHost` reports per-feature errors to `CrashReporter` tagged with the module and renders
  **`FeatureErrorCard`** instead of crashing.
- Disabled features render **`FeatureDisabledCard`**.
- Each feature ViewModel: `init { crashReporter.setModule("<key>") }`, exposes
  `featureError: StateFlow<Throwable?>` (set by a `CoroutineExceptionHandler`), and wraps one primary
  operation in `performanceTracer.trace("<key>_…")`.

### Module dependency rules — enforced by ArchUnit (`DependencyRulesTest`) + Gradle

| Rule                               | Status              |
|------------------------------------|---------------------|
| `feature → feature`                | **FORBIDDEN**       |
| `vault → network / ai / analytics` | **FORBIDDEN**       |
| `feature → data`                   | via Repository only |
| `feature → core`                   | allowed             |
| `data → core`                      | allowed             |
| `core → anything internal`         | **FORBIDDEN** (pure lib) |

### Navigation & shell

`MainActivity` ([apps/finance/app/.../MainActivity.kt](apps/finance/app/src/main/java/com/dhruv/finance/app/MainActivity.kt))
builds a `HorizontalPager` + bottom `NavigationBar`. Tabs are assembled at runtime from feature-flag
state: `Dashboard` (default), `Calc`, `Converter` (hub: currency + unit), `Date`*,
`Finance` (hub: loans/investments/tax/everyday), `Time`*, `Assistant`*. Hubs wrap each sub-feature
in its own `FeatureHost`. `Settings` is reachable via the top-bar gear icon, not the bottom nav.
A branded `SplashScreen` overlays the already-composed UI so there is no blank hand-off gap.
(*flag-gated — currently hidden, see [Feature flags](#14-feature-flags).)

### DI wiring

Each feature exposes a Koin `val <name>Module = module { … }` in its `di/` package; the app
aggregates them all in
[`CalculatorApplication`](apps/finance/app/src/main/java/com/dhruv/finance/app/CalculatorApplication.kt)
via `startKoin { modules(…) }`. **Koin only — Hilt is not used** (its Gradle plugin is incompatible
with AGP 9; [ADR-0010](platform/DECISIONS.md)).

---

## 5. The Finance app — features

Per-module detail (screens, ViewModels, data deps, flag keys) lives in
[`apps/finance/FEATURES.md`](apps/finance/FEATURES.md). Summary:

| Feature module | What it does | Flag |
|---|---|---|
| **calculator** | Standard + scientific calculator, date-grouped history (favourites/tags/notes/recycle bin), live preview, locale-aware formatting, in-screen AI "explain this calculation" | ✅ on |
| **loans** | Loan EMI + side-by-side two-loan comparison | ✅ on |
| **investments** | SIP growth, ROI/CAGR, FD/RD maturity | ✅ on |
| **tax** | GST/tax add-or-extract, CTC salary breakup (gross/PF/tax/take-home) | ✅ on |
| **everyday** | Simple/compound interest, discount/markup, tip & bill split, inflation | ✅ on |
| **currency** | Live currency converter with cached-rate offline fallback + sync status banner | ✅ on |
| **unit** | Length & mass unit conversions | ✅ on |
| **date** | Date difference, add/subtract days, age, business days, timezone, unix epoch | ⛔ off (code preserved) |
| **time** | Stopwatch + countdown timer | ⛔ off (code preserved) |
| **assistant** | Standalone online AI assistant with DPDP consent gate before any Gemini call | gated: `minVersion 1.2.0` + `requiresConsent` |

> **Money math uses `BigDecimal`.** Calculation logic in each ViewModel is pure and unit-tested.

---

## 6. Module reference

### Shared libraries

**`:libs:core`** — the pure foundation (depends on nothing internal):
- `domain/` — `DhruvEntity` + `BaseEntity` (the cross-app entity contract: `id`, `userId`, `createdAt`,
  `updatedAt`, `isSynced`, `isDeleted`), `HlcClock` (Hybrid Logical Clock for conflict-free LWW sync).
- `flags/` — `FeatureFlag` model, `SemVer` (lenient), `FeatureFlagResolver` /
  `HardcodedFeatureFlagResolver` (gates on `enabled && appVersion >= minVersion`, exposes `requiresConsent`).
- `ui/` — `FeatureHost` + `FeatureErrorCard` + `FeatureDisabledCard`; theme (`AppTheme`, `Color`,
  `Type`, `Theme`, `Responsive`, `ThemeColorConfig`, `DhruvFont`); brand components (`DhruvBrand`, `Glassmorphism`).
- `security/` — `KeystoreHelper`, `EncryptedDataStoreFactory`, `SqlCipherPassphrase` (passphrase wrap/unwrap).
- `observability/` — `CrashReporter` (Crashlytics) + `PerformanceTracer` (Firebase Performance), both
  **degrade to no-op when Firebase is absent** so they never crash the app.
- `integrity/` — `PlayIntegrityWrapper` (non-fatal/warn-only).

**`:libs:settings`** — `EncryptedDataStore`-backed settings (`AppSettings`, `SettingsKeys`,
`SettingsRepository`/`Impl`, `settingsModule`) + Compose settings sections (`AppearanceSection`,
`SecuritySection`, `SyncSection`, `AiAssistantSection`, `AboutSection`). Includes the **BYO Gemini key**
field ([ADR-0002](platform/DECISIONS.md)).

### Finance modules

**`:apps:finance:app`** — shell: `MainActivity`, settings UI, splash, Converter/Finance hubs,
`platformModule`/`appModule` Koin wiring. `applicationId = com.dhruv.finance`, namespace `com.dhruv.finance.app`.

**`:apps:finance:data`** — shared data layer: `AppDatabase` (Room), `HistoryEntity`/`HistoryDao`/`HistoryRepository`,
`CurrencyRateEntity`/`CurrencyRateDao`/`CurrencyRepository` (+`ICurrencyRepository`), `CurrencyApi` (Retrofit),
`GeminiRepository` (online Gemini; **takes the API key as a constructor arg** so it lives in `:data` and is
shared by `calculator` + `assistant` without a `feature → feature` edge — [ADR-0010](platform/DECISIONS.md)),
and `CurrencyFormatter`. Feature modules access it Repository-only.

**`:apps:finance:feature:*`** — the 10 feature modules listed in [§5](#5-the-finance-app--features).
Each is a `dhruv.android.library` + `dhruv.android.compose` Koin module, namespace `com.dhruv.finance.<name>`,
depending on `:apps:finance:data`, `:libs:core`, `:libs:settings`.

---

## 7. Tech stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.2 · Jetpack Compose (Material 3) |
| Build | AGP 9.1.1 · Gradle (config cache + parallel) · `build-logic` convention plugins · `libs.versions.toml` catalog |
| DI | **Koin** (Hilt deferred — incompatible with AGP 9, [ADR-0010](platform/DECISIONS.md)) |
| Navigation | Single-activity; `HorizontalPager` + bottom nav (Finance) |
| DB (main) | Room + Jetpack Security (EncryptedSharedPreferences for the key) |
| DB (vault, future) | SQLCipher — separate file, AES-256, separate key |
| Preferences | EncryptedDataStore |
| Network | OkHttp + Retrofit + (CertificatePinner planned) + Moshi |
| AI (online) | Gemini API via **Cloudflare Worker proxy** + per-device quota; **BYO-key** override |
| AI (on-device) | Gemini Nano — **progressive enhancement only**, capability-gated ([ADR-0007](platform/DECISIONS.md)) |
| Sync (future) | WorkManager, offline-first, HLC-based LWW |
| Flags / Crash / Perf | Firebase Remote Config + Crashlytics + Performance (free tier) |
| Min / target SDK | minSdk **26** · targetSdk **35** (compileSdk 35) |
| JVM | source/target Java 11; CI runs on JDK 21 |

Versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

---

## 8. Security (8 layers)

1. Android Keystore (hardware-backed key storage)
2. EncryptedDataStore (preferences)
3. Room + SQLCipher (data at rest; SQLCipher for vault)
4. OkHttp CertificatePinner (data in transit)
5. BiometricPrompt Class 3 (access control)
6. `FLAG_SECURE` (vault screens only)
7. Play Integrity API — **non-fatal/warn-only**, gates vault only, never blocks launch
8. ProGuard/R8 full obfuscation (release builds)

Key rules: SQLCipher passphrase is random → wrapped by a Keystore key → stored as a blob (SQLCipher
never receives a Keystore key directly). **Biometric is convenience-only for vault** — the real vault
key derives from the master password, so re-enrolling a fingerprint can never destroy vault data
([ADR-0003](platform/DECISIONS.md)). **No secrets/keys in the repo or APK** — GitLeaks gates CI; the
online AI key lives in the Worker proxy.

---

## 9. Compliance (DPDP)

India DPDP Rules 2025 are treated as a first-class layer ([ADR-0005](platform/DECISIONS.md)):

- **Consent before any data leaves the device** — enforced by the pre-online-AI consent gate
  (`requiresConsent` flag → consent screen in `AssistantScreen`).
- **Children = under-18**: parental-consent rules; no profiling/targeted ads at minors.
- **7-day guaranteed hard-delete** on user request (soft-delete is UX state, not a permanent rule;
  tombstone GC purges 90 days post-sync).
- **Play Data Safety form** is deferred until a Play launch; the consent gate + erasure apply *now*
  regardless of distribution channel.

---

## 10. AI strategy

- **Online (default):** requests route through a **Cloudflare Worker proxy** holding the Gemini key
  and enforcing a per-device quota — the key is never in the APK. A user may paste their **own Gemini
  key** in Settings to bypass the quota at zero cost ([ADR-0002](platform/DECISIONS.md)). A consent
  screen precedes any online call.
- **On-device (Gemini Nano):** progressive enhancement only (narrow device set). A capability check
  gates it; absence falls back to the online path or a graceful "not available" state
  ([ADR-0007](platform/DECISIONS.md)).

> The Worker proxy is **designed but not yet deployed** — see [Known debt](#18-known-debt--follow-ups)
> and [`platform/TELEGRAM_BOT.md`](platform/TELEGRAM_BOT.md) for related automation notes.

---

## 11. Build & run

### Prerequisites
- Android Studio (Koala / Ladybug+); JDK 17+ (`JAVA_HOME` = the Android Studio JBR).
- Gradle wrapper handles the rest.

### Common commands

```bash
# Build the Finance app (debug)
./gradlew :apps:finance:app:assembleDebug

# Build a single feature module
./gradlew :apps:finance:feature:calculator:assembleDebug

# Unit tests + ArchUnit dependency rules (ArchUnit lives in the debug variant)
./gradlew :apps:finance:app:testDebugUnitTest
./gradlew :apps:finance:feature:<name>:testDebugUnitTest

# All unit tests across modules
./gradlew testDebugUnitTest --continue

# Signed release APK (needs keystore env vars — see CI)
./gradlew :apps:finance:app:assembleRelease
```

> `./gradlew detekt` runs across all modules (the `dhruv.detekt` convention plugin is applied via
> `dhruv.android.library` / `dhruv.android.application`). Pre-existing feature-module complexity is
> grandfathered by per-module `detekt-baseline.xml`, so the gate enforces *no new* violations.
> `ktlint` formatting is enforced with Compose-aware relaxations in `.editorconfig`.

### Secrets / API keys
The `secrets-gradle-plugin` reads `.env` (falling back to `.env.example`) for keys such as
`GEMINI_API_KEY`, surfaced via `BuildConfig`. `.env` and `*.jks` are git-ignored. Never commit real keys.

---

## 12. CI/CD pipeline

Defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) (+ `release.yml`, `fast-feedback.yml`).
Runs on every push to `develop`/`main` and every PR targeting them. Right-sized for a solo maintainer
([PLATFORM.md §11](platform/PLATFORM.md), ADR-0009/0011/0012).

**Four gates:**

| Gate | Job | Notes |
|---|---|---|
| 1 · Static analysis | `static-analysis` | ktlint + detekt + Android lint — **enabled**; gates the build job (in its `needs`) |
| 2 · Security | `security` | **GitLeaks** secret scan — *blocking*, no `continue-on-error` |
| 2b · OWASP | `owasp` | dependency-check — non-blocking, off critical path, NVD DB cached month-by-month |
| 3 · Tests | `tests` | `testDebugUnitTest --continue` incl. ArchUnit; also the Gradle build-cache **writer** |
| 4 · Build | `build` | `assembleDebug`; uploads the debug APK artifact (7-day retention) |

**Post-build jobs:**
- **`pr-summary`** (PRs only, `if: always()`) — posts/updates one **sticky** comment summarizing all
  gate results, branded under a dedicated **"Dhruv Bot"** GitHub App (falls back to `github-actions[bot]`
  if the App token can't be minted). Informational only — never a required check ([ADR-0012](platform/DECISIONS.md)).
- **`release`** (push to `develop`/`main` only, after gates pass) — does the *whole* release in one run
  so it can't half-complete:
  1. bump patch version (`MAJOR.MINOR.PATCH+1`) in `platform/versions.json` + `VERSION_CODE`/`VERSION_NAME`
     in `gradle.properties`;
  2. build the **signed release APK at the bumped version**, verify it is signed and within the **50 MB**
     budget;
  3. commit the bump with `[skip ci]`; create + push the idempotent `dhruv-finance-v<version>` tag;
  4. publish a GitHub Release with the APK attached.

**Required secrets:** `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (signing);
optional `DHRUV_BOT_APP_ID` / `DHRUV_BOT_PRIVATE_KEY` (PR-summary branding). No secret ever lives in the
repo or APK.

**Branch strategy** ([ADR-0009](platform/DECISIONS.md)): branch from `develop` → PR back to `develop`.
`develop` builds a signed **APK**; `main` is reserved for a future Play launch (signed **AAB**). Never
push directly to `main`.

---

## 13. Versioning & releases

Scheme: `dhruv-{app}-vMAJOR.MINOR.PATCH` (MAJOR = breaking/arch, MINOR = new feature module, PATCH =
fix/merge).

- **Patch** is auto-incremented by CI on every merge to `develop`/`main` ([ADR-0011](platform/DECISIONS.md)).
- **Minor/Major** are bumped *manually* in `platform/versions.json` before merging when warranted; CI
  auto-increments patch from the new baseline next merge.
- `versionCode` and `VERSION_NAME` (in `gradle.properties`) are **CI-owned** — do **not** edit them by hand.
- Cross-module compatibility is tracked in [`platform/versions.json`](platform/versions.json).

Releases (APK + notes) are produced automatically by the `release` job and appear on the repo's GitHub
Releases page. Users installing the APK directly must enable install-from-unknown-sources.

---

## 14. Feature flags

Source of truth: [`platform/feature-flags/dhruv-finance.json`](platform/feature-flags/dhruv-finance.json),
also mirrored in Firebase Remote Config. The app packages this file as an Android asset
(`assets.srcDirs` in `apps/finance/app/build.gradle.kts`) and loads it at runtime via
`loadFinanceFeatureFlags()` (`com.dhruv.finance.app.di.FeatureFlagAssetLoader`), parsed with Moshi —
there is no hand-duplicated Kotlin literal to drift out of sync. A parse/IO failure falls back to a
calculator-only safety map and reports the failure via `CrashReporter`. Runtime priority:
**remote → cached last-known-good → hardcoded default**. The NavHost checks the flag before rendering a
route; disabled → `FeatureDisabledCard`.

Each flag carries `enabled`, `minVersion`, and optional `requiresConsent`. The resolver gates on
`enabled && appVersion >= minVersion`.

Current state:
- **On:** `calculator`, `loans`, `investments`, `tax`, `everyday`, `currency`, `unit`.
- **Off (code preserved, hidden):** `date`, `time`.
- **`assistant`:** `enabled = true` but `minVersion 1.2.0` + `requiresConsent` — surfaces only once the
  app's running `versionName` reaches ≥ 1.2.0, and shows a DPDP consent gate before any Gemini call.

---

## 15. Testing

- **Unit tests** — JVM, per module (e.g. `CalculatorEngine*Test`, `ConverterAndFormatterTest`,
  `SettingsModelsTest`, `HardcodedFeatureFlagResolverTest`).
- **ArchUnit** — `apps/finance/app/src/test/kotlin/.../arch/DependencyRulesTest.kt` enforces the
  module-boundary rules (incl. real `feature → feature` isolation via package slices). Lives in the
  **debug** variant, so `testDebugUnitTest` runs it.
- **Screenshot tests** — Robolectric + Roborazzi (`GreetingScreenshotTest`, baseline under
  `src/test/screenshots/`).
- **Locale/format tests** — `LocaleSeparatorFormatTest`.

> Tests bound to the now-split monolithic `FinanceViewModel` were removed in Phase 4. Per-module
> ViewModel tests (using `NoOpCrashReporter` + `NoOpPerformanceTracer`) now cover the
> loans/investments/tax/everyday planners and currency conversion; `calculator` and `date`
> ViewModel coverage remains a tracked follow-up.

---

## 16. Conventions for adding code

Read the relevant **skill** under [`platform/skills/`](platform/skills/) *before* starting:

| Task | Skill |
|---|---|
| New feature module | `dhruv-feature-scaffold/SKILL.md` (authoritative Koin pattern) |
| New Room entity / data layer | `dhruv-room-entity/SKILL.md` |
| New Compose screen | `dhruv-compose-screen/SKILL.md` *(legacy — partly stale, prefer feature-scaffold)* |
| Pre-merge check | `dhruv-module-audit/SKILL.md` |
| Version bump / release | `dhruv-release/SKILL.md` |

**Hard rules** (from [`CLAUDE.md`](CLAUDE.md) / [`AGENTS.md`](platform/AGENTS.md)):
- Do not redesign architecture — decisions are locked in `DECISIONS.md`; propose a new ADR instead.
- No code in `platform/` (docs & contracts only).
- Kotlin only, Compose only, **Koin** only (DI), Coroutines+Flow only.
- Every feature route wrapped in `FeatureHost`; every feature VM sets its module + a perf trace +
  exposes `featureError`.
- Module boundaries enforced by ArchUnit — never add a `feature → feature` edge.
- No secrets/keys in the repo or APK.
- Any new off-device data flow needs a consent gate (DPDP).
- Contract changes go through `platform/contracts/` first, then `:libs:core` implements.

---

## 17. Roadmap

From [`platform/Implementation.md`](platform/Implementation.md) (Phase 0–7):

| Phase | Scope | State |
|---|---|---|
| 0 | Prep (applicationId, repo rename, platform docs, baseline build) | ✅ done |
| 1 | Monorepo skeleton + relocate Finance (green build) | ✅ done |
| 2 | Extract `:libs:core` + ArchUnit + CI gates | ✅ done |
| 3 | `:libs:settings` (encrypted settings, BYO key, flag resolution) | ✅ done |
| 4 | Finance feature split + AI/consent wiring | ✅ done |
| (Tools) | `:apps:tools` greenfield app (validate the pattern) | ⏳ planned |
| 5 | Cloudflare Worker AI proxy + on-device Nano fallback | ⏳ designed, not deployed |
| 6 | `:apps:vault` (master-password key model, recovery key, biometric convenience) | ⏳ future |
| 7 | Build & distribute signed APK via GitHub Releases | ✅ active (Play/AAB deferred) |

> Note: the Implementation plan lists "Tools" as Phase 4 and "Finance split" as Phase 5; in practice
> the Finance split (this repo's Phase 4) landed first. Tools scaffolding is the next greenfield step.

---

## 18. Known debt & follow-ups

- **Static analysis is enabled** (Gate 1: ktlint + detekt + Android lint, gating the build). detekt is
  wired via the convention plugins with a Compose-aware `config/detekt/detekt.yml`; pre-existing
  feature-module complexity (notably the `calculator` engine/ViewModel) is grandfathered in per-module
  `detekt-baseline.xml` files, so the gate enforces *no new* violations. Burning down those baselines
  (refactoring the grandfathered complexity) is the main remaining static-analysis follow-up.
- **Per-module ViewModel tests** now cover loans/investments/tax/everyday + currency; `calculator` and
  `date` ViewModel coverage is still pending.
- **Cloudflare Worker AI proxy** is designed but **not deployed** — the assistant currently relies on
  the BYO/`BuildConfig` key path; the proxy + per-device quota are pending.
- Legacy `dhruv-compose-screen` skill is partly stale (shows Hilt / old theme APIs);
  `dhruv-feature-scaffold` is authoritative.
- `date` / `time` ship flag-disabled (code preserved); `assistant` is version-gated to ≥ 1.2.0.
- OWASP gate's row in the PR-summary comment always shows ✅ regardless of findings
  (`continue-on-error` masks them) — accepted limitation ([ADR-0012](platform/DECISIONS.md)).

---

## License

MIT (see project engineering guidelines). Brand assets in `Dhruv_Master_Brand_Kit/` are project-owned.
</content>
</invoke>
