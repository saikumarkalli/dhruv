# Android App SDD (03)

> **Status:** ACTIVE
> **Scope:** A short summary of the Android architecture, for **cross-platform alignment** with the
> other SDDs in this series. It is deliberately thin.
>
> **The detailed reference is [`apps/finance/ARCHITECTURE.md`](../../ARCHITECTURE.md)** — module
> graph, package layout, navigation, the data split, testing and the known tripwires. Read that for
> anything beyond alignment; this file exists so SDD 01–07 have a consistent Android entry.
>
> **Corrected 2026-08-23** against the source tree. Four claims here were wrong: the certificate
> pins (ISRG, never true in shipped code — see §4), a `BentoCard` component that has never existed,
> "Spotless" as the formatter, and a module named `expenses`.

## 1. Module Architecture

The Android app follows a strict modular structure utilizing Koin for dependency injection.

```
:apps:finance:app          (Shell, MainActivity, Navigation, Koin aggregation)
:apps:finance:data         (Repositories, Room DAOs, Retrofit API, tracker/ Supabase layer)
:apps:finance:feature:*    (Independent feature modules e.g. calculator, loans, networth, money)
:libs:settings             (EncryptedDataStore for secure preferences)
:libs:core                 (Design system, FeatureHost, NavTarget, observability)
```

**Dependency Rule:** Feature modules depend on `:libs:core` and on `:data` **through repositories
only** — never a DAO or DTO. Feature modules **never** depend on other feature modules, and
`:libs:core` depends on nothing internal. All four rules are enforced by `DependencyRulesTest`
(ArchUnit), which runs in `regressionCheck`.

## 2. Data Layer

- **Remote**: Retrofit + Moshi + OkHttp against GoTrue and PostgREST. Two clients share one
  builder: an unauthenticated `authClient` for sign-in, and a **consent-gated** `dataClient` for
  tracker data — `ConsentInterceptor` is attached only to the latter, so no code path reaches
  tracker data before DPDP consent.
- **Local**: Room (`AppDatabase` v5) for calculator history and currency-rate caching **only**.
- **Tracker data is Supabase-primary** — no local mirror, no `DhruvEntity`, no client-side conflict
  resolution; RLS (`user_id = auth.uid()`) is the source of truth (ADR-0014). Every request sends
  `Accept-Profile: finance` (ADR-0033).
- **Money**: integer paise (`Long`/`bigint`), proportions in basis points; enforced by the
  `checkTrackerMoneyPrecision` Gradle task.
- **State**: Repositories expose `Flow<T>` or suspend functions to ViewModels.

## 3. UI & State Management

- **Pattern**: MVVM with `StateFlow`.
- **Navigation**: Custom `NavigationDispatcher` and sealed `NavTarget` classes. No AndroidX
  Navigation graph. Single Activity, pager over **5 tab roots** (Home · Money · Calc · Plan ·
  Insights, ADR-0027), each hosting a nested `NavHost`. Back-press precedence is a pure function in
  `:libs:core`'s `BackContract.kt`. Cross-feature navigation is by `NavTarget` id, never a class
  reference.
- **Fault isolation**: every route wrapped in `FeatureHost` — flag-off renders
  `FeatureDisabledCard`, a thrown error renders `FeatureErrorCard` tagged with the module. Never a
  blank crash.
- **Components**: Strict usage of `:libs:core` components (`NxCard`, `MoneyText`, `NxButton`,
  `ListGroup`, `StatDeltaChip`, …) per
  [`platform/DESIGN-SYSTEM.md`](../../../../platform/DESIGN-SYSTEM.md). Zero feature-local styling.
  Check §5.1 (built) before writing a screen — §5.2 lists components that are **planned and do not
  exist**.

## 4. Security Model (8-Layer)

1. **Authentication**: Google Credential Manager (ID Token) → Supabase GoTrue. One identity across
   every Dhruv app (ADR-0031).
2. **Transport**: Certificate pinning via OkHttp, **CA-level: Google Trust Services GTS Root R1 +
   R4**. Leaf pinning would brick the app on Supabase's routine rotations. *(This line previously
   read "ISRG Root X1/X2" — that was never true of shipped code; the wrong pins threw
   `SSLPeerUnverifiedException` on a real device's first live sign-in. See ADR-0029's correction.)*
3. **App Lock**: BiometricPrompt Class 3 with fallback — design-v1 **Phase 0b**
   ([`specs/004-settings`](../../specs/004-settings/)).
4. **Obfuscation**: R8 and ProGuard in release builds.
5. **Storage**: EncryptedDataStore for tokens — never plaintext `SharedPreferences`.
6. **Screen Security**: `FLAG_SECURE` on sensitive routes — Phase 0b.
7. **Privacy Mode**: Visual masking of financial values. Deliberately **not** applied to PDF/CSV
   export, which is an explicit user act and says so in its dialog.
8. **Integrity**: Play Integrity API (warn-only, never blocks launch).

**DPDP**: consent gates every tracker call via `ConsentInterceptor`; erasure is the
`delete_my_data()` / `delete_my_account()` security-definer functions. Every new user-data table
must be added to the former in the same migration — a miss breaks the 7-day guarantee silently.

## 5. Build & CI Pipeline

- Managed via GitHub Actions (`ci.yml`). **The PR is the only full-validation pass** (ADR-0026);
  the merge push runs the release job only.
- **Gates**: ktlint + Detekt + Android lint · GitLeaks + Gradle wrapper validation + doc-link check
  (every PR, docs-only included) · `regressionCheck` (all unit tests + ArchUnit + merged JaCoCo +
  coverage floor, with `assembleDebug` on the same warm daemon).
  *(This line previously said "Spotless" — the repo has never used it.)*
- **Coverage is JaCoCo, not Kover** — Kover has no working AGP 9 integration (ADR-0013).
- **Artifacts**: CI derives the semver segment from commit types (ADR-0025) and publishes a signed
  APK to a GitHub Release from `main` only, behind an approval gate (ADR-0032). Never hand-edit
  `platform/versions.json` or `gradle.properties`.
