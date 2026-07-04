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

## Second-pass findings (full-app security/bug sweep, 2026-07-03, branch `develop` v1.2.7)

### 🟠 H5 — Migration chain starts at 2→3; no `MIGRATION_1_2`
- [ ] **Fix / verify**
- **What:** `AppDatabase` is at version 5 with migrations 2→3, 3→4, 4→5 only. Any install whose DB
  is still at schema v1 (original dhruv-calc era) falls into `fallbackToDestructiveMigration` →
  full history wipe. Compounds H2.
- **Evidence:** `apps/finance/data/.../AppDatabase.kt:24-51,64-65`.
- **Fix direction:** Verify whether a v1 schema ever shipped to users; if yes add `MIGRATION_1_2`,
  if no document that and keep the chain complete from the earliest shipped version.

### 🟠 H6 — Backup rules are untouched template stubs while `allowBackup="true"`
- [ ] **Fix**
- **What:** `backup_rules.xml` and `data_extraction_rules.xml` are the commented-out samples, so
  **everything** is auto-backed-up: the history DB, plaintext settings (including the history PIN),
  and the `secure_settings` encrypted DataStore. The Keystore key does not transfer, so a restored
  `secure_settings` file is undecryptable — the serializer silently returns defaults, i.e. the
  user's BYO Gemini key silently vanishes after a device migration. Supersedes/concretizes L2.
- **Evidence:** `apps/finance/app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`,
  `AndroidManifest.xml:9-11`; silent-default path at `libs/core/.../EncryptedDataStoreFactory.kt:55-58`.
- **Fix direction:** Write real rules: exclude `datastore/secure_settings.preferences_pb_enc` (and
  decide DB/settings inclusion consciously) in **both** `cloud-backup` and `device-transfer`.

### 🟠 H7 — History-lock PIN stored in plaintext with default `"1234"`, compared in the UI layer
- [ ] **Fix**
- **What:** `historyPinCode` lives in the **plaintext** `app_settings` DataStore (the encrypted
  store exists but only holds the Gemini key), defaults to `"1234"`, is exposed through
  ViewModel → Composable as a `StateFlow<String>`, and is checked by plain `==` inside
  `CalculatorScreen`. The lock is decorative against anyone who can read app storage or a backup.
- **Evidence:** `libs/settings/.../SettingsRepositoryImpl.kt:113-119,304`;
  `apps/finance/feature/calculator/.../CalculatorScreen.kt:2211,2311`.
- **Fix direction:** Store a salted hash (or move to the encrypted store), drop the `"1234"`
  default, verify in the ViewModel/repository (`verifyPin(entered): Boolean`) so the real PIN
  never reaches the UI.

### 🟡 M6 — Assistant consent is in-memory only; no durable DPDP consent record
- [ ] **Fix** (fold into C3)
- **What:** `AssistantViewModel` starts at `ConsentNeeded` and `grantConsent()` just flips a
  StateFlow — nothing is persisted. Consent is re-asked every process restart, is not shared with
  the calculator AI path (C3), and there is **no timestamped consent record** to demonstrate DPDP
  compliance.
- **Evidence:** `apps/finance/feature/assistant/.../AssistantViewModel.kt:19,44-48`.
- **Fix direction:** One persisted consent state (encrypted DataStore, with grant timestamp +
  policy version) consumed by both AI entry points; revocable from Settings.

### 🟡 M7 — Assistant sends user questions through the wrong prompt
- [ ] **Fix**
- **What:** `AssistantViewModel.ask()` calls `gemini.explainCalculation(prompt, "")` — the
  "explain this calculation, Expression: X, Result: " template with an empty result — instead of a
  purpose-built assistant/solve prompt. Answers are framed as explanations of a non-existent
  calculation.
- **Evidence:** `apps/finance/feature/assistant/.../AssistantViewModel.kt:72`.

### 🟡 M8 — Prompt-injection surface unbounded as AI grows
- [ ] **Fix direction, low risk today**
- **What:** `GeminiRepository.solve()`/`explainCalculation()` interpolate raw user input into the
  prompt. Today the blast radius is small (no tools, output rendered as plain Compose `Text`), but
  the platform plan adds deeper AI integration — once model output can drive actions/tools, this
  is the LLM01 boundary. Assistant input length is not capped (calculator input is, at 50 chars);
  no client-side request rate/token bound exists (LLM10 — matters for the proxy quota in ADR-0002).
- **Evidence:** `apps/finance/data/.../GeminiRepository.kt:50-60,84-100`.
- **Fix direction:** Cap input length + request rate now; when tools/agents arrive, treat model
  output as untrusted input (schema-validate, allowlist actions) and keep secrets out of prompts.

### 🟡 M9 — `exportSchema = false`
- [ ] **Fix**
- **What:** No schema history is exported, so Room migration tests are impossible — compounds
  H2/H5. **Evidence:** `AppDatabase.kt:13`.
- **Fix direction:** Set `exportSchema = true` + check in `schemas/`; add `MigrationTestHelper`
  tests for 2→3→4→5.

### ⚪ L4 — `assistant_query` performance trace measures nothing
- **What:** `performanceTracer.trace("assistant_query") { Unit }` brackets a no-op, not the Gemini
  call. `AssistantViewModel.kt:70`.
### ⚪ L5 — Browser User-Agent spoofing on the currency API
- **What:** `CURRENCY_API_USER_AGENT` impersonates Chrome on Android — ToS/etiquette risk with the
  free rate APIs; use an honest `DhruvFinance/<version>` UA. `apps/finance/app/build.gradle.kts:21-25`.
### ⚪ L6 — Currency responses accepted without sanity checks
- **What:** `rates: Map<String, Double>` is used as-is; a compromised/poisoned endpoint (no cert
  pinning yet, H4) could feed absurd rates into user calculations. Validate plausibility
  (positive, finite, base currency = requested).
### ⚪ L7 — `runBlocking` DataStore read in `SettingsRepositoryImpl` init
- **What:** Blocks first access (typically main thread during Koin graph construction) —
  startup-jank/ANR risk. `SettingsRepositoryImpl.kt:65-73`.

---

## What's already solid (do not regress)
Module boundaries + ArchUnit `DependencyRulesTest` · `FeatureHost` fault isolation · feature-flag
asset-as-single-source loader with safe fallback · release-in-one-run CI (bump → signed build →
verify signed/size → tag → publish) · R8 minify + graceful signing fallback · calculator engine
edge-case tests · cleartext traffic disabled.

---

## Remediation plan (phased, replaces the earlier "order of attack")

### Phase 0 — Decisions (blockers for later phases; need maintainer's call)
- [ ] **D1 (→C1/C2/C4):** AI key delivery model. *Recommended:* ship **BYO-key-only** first (amend
  ADR-0002 — assistant/solve show a "add your Gemini key in Settings" state when no key), build the
  Cloudflare Worker proxy in Phase 6. Alternative: build the proxy now and hold AI until it ships.
  Either way: **no shared key ever embedded in the APK.**
- [ ] **D2 (→H4):** main-DB encryption stance — document "plaintext + device encryption is accepted
  for calc history" (likely) or wire `SqlCipherPassphrase`. Record as ADR either way.
- [ ] **D3 (→H5):** check release history for whether DB schema v1 ever shipped to users.

### Phase 1 — Stop data loss & compliance holes (C3, H2, H5, H6, M6, M9)
- [ ] T1: Persisted DPDP consent (encrypted DataStore: granted flag + timestamp + policy version),
  consumed by **both** assistant and calculator-solve; revocable in Settings. (C3+M6)
- [ ] T2: Remove `fallbackToDestructiveMigration` for release; `exportSchema = true` + `schemas/`
  checked in; `MigrationTestHelper` tests for the full chain; add `MIGRATION_1_2` if D3 says v1
  shipped. (H2+H5+M9)
- [ ] T3: Real backup/data-extraction rules — exclude `secure_settings` (cloud **and**
  device-transfer); conscious documented choice on DB/settings. (H6, closes L2)
- **Checkpoint:** `regressionCheck` green; migration tests pass; consent flow manually verified on
  both AI entry points.

### Phase 2 — AI wired end-to-end per D1 (C1, C2, C4, M7, M8, L1)
- [ ] T4: `GeminiKeyProvider` (suspend, call-time): user BYO key → default (proxy/CI secret per D1);
  `GeminiRepository` stops capturing the key in its constructor. (C2)
- [ ] T5: Localized user-facing "no key configured" copy (kill the ".env file" string);
  `mapError` preserves cause/type for Crashlytics. (C1+L1)
- [ ] T6: Dedicated assistant prompt for `ask()` (stop reusing `explainCalculation`); cap assistant
  input length; light client-side rate limit on AI calls. (M7+M8)
- **Checkpoint:** BYO key works on-device for both entry points; `strings`/apktool scan of release
  APK shows no key.

### Phase 3 — Observability on (H1, L4)
- [ ] T7: `google-services` + Crashlytics Gradle plugins + `google-services.json`; verify a test
  crash and a real trace land in the console.
- [ ] T8: Trace actual Gemini latency (replace the `Unit` sentinel); then layer
  `FirebaseFeatureFlagResolver` (remote → cached → hardcoded floor).

### Phase 4 — Security hardening (H4, H7, M1, L5, L6)
- [ ] T9: `CertificatePinner` for currency hosts (+ proxy host when it exists), with backup pins +
  rotation plan.
- [ ] T10: History PIN → salted hash in encrypted store, `verifyPin()` in repository, no `"1234"`
  default, PIN value removed from UI-visible StateFlows. (H7)
- [ ] T11: Wire `org.owasp.dependencycheck` in build-logic; flip Gate 2b `continue-on-error: false`. (M1)
- [ ] T12: Play Integrity — wire the warn-only gate or delete `PlayIntegrityWrapper` until vault;
  honest `DhruvFinance/<version>` UA; sanity-validate currency rates. (H4 remainder, L5, L6)

### Phase 5 — Data model & quality (H3, M2–M5, L3, L7)
- [ ] T13: Entities adopt `DhruvEntity` (UUID id, indexed `userId="local"`, sync fields) via real
  migration + tests — prerequisite for Phase-2 sync. (H3)
- [ ] T14: ViewModel/repository test wave; ratchet `globalLineFloor`; small instrumented smoke suite. (M2)
- [ ] T15: String externalization + hardcoded-text lint gate (M3); decompose
  `CalculatorScreen`/`CalculatorViewModel` (M4); `PRIVACY.md` + `LICENSE` (M5); drop the
  `runBlocking` eager read (L7); L3 cleanup.

### Phase 6 — AI platform build-out (future integration, per PLATFORM.md §6)
- [ ] T16: Cloudflare Worker proxy — key custody, per-device quota (HMAC device token), abuse
  caps; consent + Data Safety entry for the new flow. (completes ADR-0002 if D1 chose BYO-first)
- [ ] T17: Gemini Nano progressive enhancement behind a capability check (never assumed present).
- [ ] T18: LLM security baseline before any tool-use/agent features: model output = untrusted input
  (schema-validate, allowlist actions), token/rate/loop bounds, secrets and cross-user data never
  in prompts. (extends M8)

## Accepted / won't-fix
_(none yet — move items here with rationale + ADR link if a locked decision changes.)_
