# Dhruv Finance (`:apps:finance`)

Personal-finance tracker and calculator suite for Android. Net worth, money movement, planning and
insights over a Supabase backend, plus an offline calculator/converter suite that works with no
account at all.

`applicationId = com.dhruv.finance` · current version **2.0.4** (`versionCode` 16) — see
[`platform/versions.json`](../../platform/versions.json). Versions are **CI-owned**: never hand-edit
`versions.json` or `gradle.properties` (ADR-0025).

- **New here? Start with [ARCHITECTURE.md](ARCHITECTURE.md)** — module-by-module walkthrough,
  the data split, navigation, testing, and the tripwires that have actually caused failures
- Module index — [FEATURES.md](FEATURES.md)
- App conventions, docs map — [CLAUDE.md](CLAUDE.md)
- Design contract (global, binding) — [`platform/DESIGN-SYSTEM.md`](../../platform/DESIGN-SYSTEM.md)
- Product spec (61 screens, business rules) — [functional spec](docs/superpowers/specs/2026-08-08-design-v1-final-functional-spec.md)
- Build order — [implementation plan](docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md)

---

## Platform targets

| | |
|---|---|
| `minSdk` | 26 |
| `compileSdk` / `targetSdk` | 37 |
| AGP | 9.1.1 |
| Gradle | 9 |
| Kotlin | 2.4.10 |
| JDK | 17+ (`JAVA_HOME` must be the Android Studio JBR) |

## Stack

| Concern | Choice | Notes |
|---|---|---|
| UI | Jetpack Compose (BOM `2026.08.00`) | No XML layouts, no Views |
| DI | **Koin 3.5.6** | **Not Hilt** — its Gradle plugin is incompatible with AGP 9 (ADR-0010) |
| Async / state | Coroutines + `StateFlow` | |
| Navigation | Single-activity, pager over 5 tab roots, nested `NavHost` per tab | ADR-0027 |
| Local DB | Room 2.8.4 (`AppDatabase` **v5**) | Calculator history + currency cache **only** |
| Tracker DB | Supabase Postgres, `finance` schema | Cloud-primary, no local mirror (ADR-0014, ADR-0033) |
| Network | Retrofit 2.12.0 + Moshi 1.15.2 + OkHttp 4.10.0 | Plain REST against GoTrue + PostgREST — **not** supabase-kt/Ktor (ADR-0029) |
| Preferences | EncryptedDataStore | Session tokens never in plaintext `SharedPreferences` |
| Auth | Google Sign-In via Credential Manager + `googleid` 1.2.0 → Supabase GoTrue | One identity across every Dhruv app (ADR-0031) |
| Observability | Firebase BOM 34.12.0 — Crashlytics, Performance, Remote Config | Free tier (ADR-0006) |
| AI | Gemini via a Cloudflare Worker proxy; BYO-key override | Key never in the APK (ADR-0002) |

## Data architecture

Two storage domains, deliberately separate — see [`data/README.md`](data/README.md) before moving
anything across the line.

- **Room** — calculators and converters. Offline-first, device-local, no account needed.
- **Supabase** — the entire tracker. **Cloud-primary: no local Room mirror, no `DhruvEntity`, no
  client-side conflict resolution.** The server plus RLS (`user_id = auth.uid()`) is the single
  source of truth. This narrowly overrides `PLATFORM.md` §5's offline-first design for this domain
  only (ADR-0014).

Consequences worth knowing before you write a tracker screen:

- **Money is integer paise** (`Long` / `bigint`); proportions are integer **basis points**.
  `BigDecimal` is confined to the calculator and projection engines, and the
  `checkTrackerMoneyPrecision` Gradle task enforces the boundary.
- **Signed-out, offline and not-configured are first-class UI states**, not error dialogs.
- Every tracker request sends **`Accept-Profile: finance`**. Omitting it does not error loudly — it
  silently 404s against the empty `public` schema.
- Schema is authored declaratively in [`supabase/schemas/finance/`](../../supabase/schemas/finance/);
  `supabase db diff` generates the migration. Current shape:
  [`supabase/SCHEMA.md`](../../supabase/SCHEMA.md).

## Security & compliance

- **TLS pinning is CA-level** — Google Trust Services GTS Root R1 + R4. Leaf pinning would brick the
  app on Supabase's routine certificate rotations (ADR-0029, and its correction — the roots were
  wrong once already).
- **Consent is an interceptor, not a screen concern.** `ConsentInterceptor` is attached only to the
  PostgREST client, so no code path can reach tracker data before the DPDP switch is on — there is
  no second PostgREST-capable client anywhere in the app.
- **Erasure is in-app and server-side**: `delete_my_data()` / `delete_my_account()` security-definer
  functions. Every new user-data table must be added to the former **in the same migration** — a
  miss breaks the DPDP 7-day guarantee silently, with no failing test.
- No secrets in the repo or the APK. `.env` is gitignored; GitLeaks gates CI.

## Fault isolation

Every route is wrapped in `FeatureHost` — a flag-off renders `FeatureDisabledCard`, a thrown error
renders `FeatureErrorCard` tagged with the module. **Never a blank crash.** Every feature ViewModel
also sets `crashReporter.setModule(...)`, exposes a `featureError` StateFlow, and traces one primary
operation.

Feature flags live in
[`platform/feature-flags/dhruv-finance.json`](../../platform/feature-flags/dhruv-finance.json),
packaged as an Android asset — a single source with no second hand-written copy to drift. If it
fails to parse, the app falls back to a calculator-only safety map.

## Build & test

```bash
./gradlew :apps:finance:app:assembleDebug              # build
./gradlew :apps:finance:feature:<name>:assembleDebug   # one module
./gradlew detekt                                       # lint (detekt 1.23.7 + ktlint)
./gradlew :apps:finance:app:testDebugUnitTest          # unit tests incl. ArchUnit
./gradlew regressionCheck                              # the pre-merge gate CI runs
```

`regressionCheck` = every module's `testDebugUnitTest` + a merged JaCoCo report + the coverage
floor. Testing stack: JUnit4, `coroutines-test`, **Turbine 1.2.1** for Flow, **ArchUnit 1.3.0** for
module boundaries, **Robolectric 4.16.1**, **JaCoCo 0.8.12** (not Kover — no working AGP 9
integration, ADR-0013).

**Room DAOs are tested through fakes, never in-memory Room** — Robolectric-SQLite is a known blocker
on this toolchain, not a preference.

## Working in this app

- **Tests first.** RED → GREEN → REFACTOR is constitutional, and feature work cites a QA-catalog
  scenario ID before any code exists.
- **Module boundaries**: `feature → feature` forbidden, `feature → data` via Repository only. Both
  enforced by `DependencyRulesTest`.
- **Design tokens only** — no raw hex/dp/sp and no `MaterialTheme.colorScheme`/`.typography` in a
  screen file. All user-visible strings in `strings.xml` from birth.
- **Feature modules are bucketed on disk** (`feature/plan/loans/`) while their Gradle coordinates
  stay flat (`:apps:finance:feature:loans`), remapped via `projectDir` in `settings.gradle.kts`.
  A new module **needs that remap** or Gradle configuration fails.
- **When a phase ships**, its FEATURES.md row, module README, CHANGELOG entry and surface-registry
  row all move together — see the tracking rule in [CLAUDE.md](CLAUDE.md).

## Status

Design-v1 Phases 0–1 shipped (shell, identity, consent). Phases 2–7 are specified in
[`specs/`](specs/) and gated on Phase 2 — see the implementation plan's §7 tracking table for the
live state, and the
[phase-readiness architecture decisions](docs/superpowers/specs/2026-08-23-phase-readiness-architecture-decisions.md)
for the open maintainer calls.

## Production readiness

The production-readiness gap tracker for this app is maintained privately and is not part of this
repository. It is an internal working document — no build, CI job, or published artifact depends
on it. Ask the maintainer if you need its current status.