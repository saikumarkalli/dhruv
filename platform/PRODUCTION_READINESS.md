# Dhruv — Production Readiness Audit & Remediation Log

> Status: **OPEN**. Snapshot of gaps between the finalized platform design (`PLATFORM.md`,
> `DECISIONS.md`) and the shipped implementation, found during a full-codebase audit on
> **2026-07-02** (branch `feat/fixing_all_issues`, finance app v1.2.6 / versionCode 8).
>
> This is a working tracker, not a decision doc — when an item is fixed, check it off and note the
> commit/PR. If a "gap" is intentionally accepted, move it to *Accepted / won't-fix* with a one-line
> rationale (and an ADR if it changes a locked decision). The scaffolding here is strong; these are
> the things standing between it and a genuinely production-ready release.

**Severity legend:** 🔴 Critical (blocks release / correctness / compliance) · 🟠 High
(foundational / data-loss / blind-in-prod) · 🟡 Medium (standards / quality gates) · ⚪ Low (polish).

**Scope note:** Only `:apps:finance` is active; `tools`/`vault` are planned/future and out of scope.

---

## 🔴 Critical

### C1 — AI features are dead in every distributed build
- [ ] **Fix**
- **What:** The release APK never receives a real `GEMINI_API_KEY`. CI supplies none, and the Secrets
  Gradle plugin falls back to `.env.example` → placeholder `MY_GEMINI_API_KEY`. `GeminiRepository`
  short-circuits on exactly that value and returns *"Gemini API key is not configured. Please add it
  to your .env file."* So the Assistant (shipped `enabled`, `minVersion 1.2.0`, current 1.2.6 → visible)
  and the calculator "solve" button are non-functional in production **and** leak a developer-facing
  error string to end users.
- **Evidence:** `apps/finance/data/.../GeminiRepository.kt:46-47,77-78`;
  `apps/finance/app/.../di/PlatformModule.kt:37`; `.env.example`;
  `.github/workflows/ci.yml` release job (no `GEMINI_API_KEY` env).
- **Fix direction:** Decide the key-delivery model first (see C4). Then either inject via CI secret or
  route through the proxy. Replace the ".env file" user-facing copy with a real, localized message.
- **Related:** ADR-0002, ADR-0010.

### C2 — BYO-key override is built but not wired
- [ ] **Fix**
- **What:** Settings persists `geminiApiKey` in a *separate encrypted* DataStore (the intended
  ADR-0002 "bring your own key" path), but `GeminiRepository` is constructed once from
  `BuildConfig.GEMINI_API_KEY` and never reads the user's key. The Settings input does nothing.
- **Evidence:** `libs/settings/.../SettingsRepositoryImpl.kt:237,275,288`; fixed singleton at
  `apps/finance/app/.../di/PlatformModule.kt:37`.
- **Fix direction:** Make `GeminiRepository` resolve its key at call time (e.g. a `suspend` key
  provider that prefers the user's stored key, falls back to proxy/build key), instead of a
  constructor-captured string.
- **Related:** ADR-0002.

### C3 — DPDP consent gate missing on the calculator AI path
- [ ] **Fix**
- **What:** `CalculatorViewModel.solveCurrentInput()` sends user input straight to Google Gemini with
  **no consent screen**. Only the standalone `assistant` feature gates consent. Violates the hard rule
  *"consent screen before any data leaves the device"*; the feature flag's `requiresConsent` is not
  enforced for this flow.
- **Evidence:** `apps/finance/feature/calculator/.../CalculatorViewModel.kt:118-132` (no consent check),
  vs. `apps/finance/feature/assistant/.../AssistantScreen.kt:67`, `AssistantViewModel.kt:44`.
- **Fix direction:** Gate the calculator's online-solve behind the same consent state; persist consent
  once and share it across both entry points.
- **Related:** ADR-0005, PLATFORM.md §8.

### C4 — Embedded API key contradicts "no keys in the APK"
- [ ] **Fix / decide**
- **What:** The moment a real key is placed in `.env`, it is compiled into `BuildConfig` and shipped in
  the APK (extractable / drainable). The mandated Cloudflare Worker proxy **does not exist**. Currently
  masked by C1, but the architecture is set up to embed rather than proxy.
- **Evidence:** `apps/finance/app/build.gradle.kts` (secrets plugin → `.env`);
  `di/PlatformModule.kt:37`; no proxy/Worker in repo.
- **Fix direction:** Build the proxy per ADR-0002, **or** ship AI as BYO-key-only (no embedded key) and
  amend ADR-0002. Either way, no shared key in the APK.
- **Related:** ADR-0002, AGENTS.md hard rules.

---

## 🟠 High

### H1 — Firebase is entirely unwired; the app ships blind
- [ ] **Fix**
- **What:** No `google-services` plugin, no `google-services.json`, no `firebase-crashlytics-gradle`
  plugin anywhere. `CrashlyticsReporter`/`FirebasePerformanceTracer` defensively no-op when FirebaseApp
  is uninitialized. Result: **no crash reporting, no performance traces, no Remote Config** in
  production — the whole observability layer (PLATFORM §10 + per-feature "definition of done") is inert.
  Production crashes are undiagnosable.
- **Evidence:** `libs/core/.../observability/CrashReporter.kt:32-34`; no `google-services.json` in tree;
  Firebase deps declared in `gradle/libs.versions.toml:99-115` but no plugin applied.
- **Fix direction:** Add the Google Services + Crashlytics Gradle plugins and `google-services.json`
  (per-flavor if needed); verify a test crash and a trace land. Then layer
  `FirebaseFeatureFlagResolver` on top of the hardcoded resolver.

### H2 — `fallbackToDestructiveMigration(dropAllTables = true)` = silent total data loss
- [ ] **Fix**
- **What:** Any unhandled schema change wipes the user's entire calculation history with no warning or
  backup.
- **Evidence:** `apps/finance/data/.../AppDatabase.kt:65`.
- **Fix direction:** Remove destructive fallback for release; require explicit `Migration`s. If a
  destructive path must exist, gate it and back up/export first.

### H3 — Data model does not implement the `DhruvEntity` contract
- [ ] **Fix**
- **What:** `HistoryEntity` uses a `Long` autoincrement id with no `userId`/`createdAt`/`updatedAt`/
  `isSynced`/`isDeleted`; **zero** entities implement `DhruvEntity`/`BaseEntity`. PLATFORM §5 requires an
  **indexed `userId` from day one** so Phase-2 sync avoids a painful migration — as built, sync will
  require exactly that migration.
- **Evidence:** `apps/finance/data/.../HistoryEntity.kt:14-28`; contract at
  `platform/contracts/DhruvEntity.kt`; no implementers found.
- **Fix direction:** Migrate entities onto `DhruvEntity` (UUID id + indexed `userId="local"` + sync
  fields) now, while data volume is small.

### H4 — Security layers 4 & 7 absent; DB not encrypted at rest
- [ ] **Fix / decide**
- **What:** No `CertificatePinner` anywhere (§7 layer 4 unimplemented). `PlayIntegrityWrapper` exists but
  is **never called** — dead code (§7 layer 7). Main Room DB is plaintext; `SqlCipherPassphrase` in
  `:core` is wired to nothing (§7 layer 3). *(Note: §3 is ambiguous on whether the main DB must be
  SQLCipher, and calc history is low-sensitivity — this is "decide + document," not a confirmed leak.
  Cleartext traffic IS correctly disabled via `network_security_config.xml`.)*
- **Evidence:** no `CertificatePinner` in `apps`/`libs`; `libs/core/.../integrity/PlayIntegrityWrapper.kt`
  uncalled; `libs/core/.../security/SqlCipherPassphrase.kt` unused.
- **Fix direction:** Add cert pinning to `CurrencyApiClient`; either wire Play Integrity to a real gate
  or delete the wrapper until vault ships; decide + record the main-DB encryption stance.

---

## 🟡 Medium

### M1 — OWASP dependency-check gate is a permanent no-op
- [ ] **Fix**
- **What:** The plugin is never wired; `continue-on-error: true` means Gate 2b always shows green
  regardless of vulnerable deps. Two advertised safety nets (this + H1) give zero real signal.
- **Evidence:** `.github/workflows/ci.yml:148-152`; no `dependencyCheck` config in `build-logic`/catalog.
- **Fix direction:** Wire the `org.owasp.dependencycheck` plugin in `build-logic`, then flip
  `continue-on-error` to `false`.

### M2 — Test confidence is low (~9% floor, ~1 real instrumented test)
- [ ] **Fix**
- **What:** Coverage floor is `globalLineFloor = "0.09"`; 25 JVM unit-test files but essentially one
  instrumented test (`ExampleInstrumentedTest`). Calculator *engine* is well-tested; ViewModels, DI,
  navigation, and UI are largely unexercised.
- **Evidence:** `build.gradle.kts:44`; `find src/androidTest` → 1 file.
- **Fix direction:** Add ViewModel + repository unit tests and a small instrumented smoke suite; ratchet
  the floor up as coverage lands.

### M3 — No string externalization / i18n
- [ ] **Fix**
- **What:** ~199 hardcoded `Text("…")` literals across features vs a single `strings.xml` entry
  (`app_name`). No localization is possible; user copy is scattered through Compose code.
- **Evidence:** `grep 'Text("'` across `apps/finance/feature` + app `main`; `apps/finance/app/.../res/values/strings.xml`.
- **Fix direction:** Extract user-facing strings to `strings.xml` (or per-feature resources); add lint
  gate for hardcoded UI text.

### M4 — Oversized files
- [ ] **Fix**
- **What:** `CalculatorScreen.kt` = 2,339 lines; `CalculatorViewModel.kt` = 899. Both past the
  ~1,000-line inspection boundary — hard to review/maintain.
- **Evidence:** `apps/finance/feature/calculator/.../CalculatorScreen.kt`, `CalculatorViewModel.kt`.
- **Fix direction:** Decompose into sub-components / state holders; extract pure logic.

### M5 — No privacy policy or LICENSE in the repo
- [ ] **Fix**
- **What:** Consent copy references a "data transfer," but there is no privacy policy document; no
  LICENSE either. Both expected before distribution (privacy policy is DPDP-relevant).
- **Fix direction:** Add `PRIVACY.md` (+ hosted URL) and a `LICENSE`.

---

## ⚪ Low / polish

- [ ] **L1** — `GeminiRepository.mapError` collapses all failures to `Exception(message)`, discarding
  type/stack for the crash reporter. `apps/finance/data/.../GeminiRepository.kt:118-129`.
- [ ] **L2** — `android:allowBackup="true"` auto-backs-up the history DB; make it a conscious choice and
  document it. `apps/finance/app/.../AndroidManifest.xml`.
- [ ] **L3** — Vestigial default `deviceSource = "Android Device"` in `HistoryEntity`.

---

## What's already solid (do not regress)
Module boundaries + ArchUnit `DependencyRulesTest` · `FeatureHost` fault isolation · feature-flag
asset-as-single-source loader with safe fallback · release-in-one-run CI (bump → signed build →
verify signed/size → tag → publish) · R8 minify + graceful signing fallback · calculator engine
edge-case tests · cleartext traffic disabled.

---

## Suggested order of attack
1. **C3 + H2** — smallest, self-contained, each carries live user/legal risk. Do first.
2. **C1 / C2 / C4** — resolve the AI key-delivery model, then wire it end-to-end.
3. **H1** — wire Firebase so production isn't blind.
4. **H3** — adopt `DhruvEntity` while data volume is small.
5. **M1–M5** — quality gates and standards.

## Accepted / won't-fix
_(none yet — move items here with rationale + ADR link if a locked decision changes.)_
