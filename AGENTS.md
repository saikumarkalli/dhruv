# AGENTS.md — Dhruv monorepo

Instructions for any AI coding agent working in this repository (Codex, Cursor, Copilot, Gemini
CLI, Jules, Claude Code, or a human reading quickly). Claude Code additionally reads `CLAUDE.md`,
which imports the same sources listed below — this file exists so agents that look for
`AGENTS.md` by convention are not left without project context.

**Read the bootstrap chain before writing code.** Everything below is a summary; the linked files
are authoritative and win any disagreement.

## Bootstrap (read in order)

1. [`platform/PLATFORM.md`](platform/PLATFORM.md) — architecture, source of truth
2. [`platform/DECISIONS.md`](platform/DECISIONS.md) — the ADR register: *why* things are as they are
3. [`platform/DESIGN-SYSTEM.md`](platform/DESIGN-SYSTEM.md) — binding design contract for every app
4. [`platform/AGENTS.md`](platform/AGENTS.md) — the full platform agent rules this file summarizes
5. [`.specify/memory/constitution.md`](.specify/memory/constitution.md) — the non-negotiable articles
6. The app you are touching: [`apps/finance/CLAUDE.md`](apps/finance/CLAUDE.md)

## What this repo is

Kotlin + Jetpack Compose Android monorepo, plus a React/Vite web SPA (`web/`) and a Supabase
backend (`supabase/`). Multiple apps share `:libs:core` and `:libs:settings`. Single-activity
NavHost, Koin DI, Room + EncryptedDataStore, MVVM. minSdk 26.

Only `:apps:finance` exists today. `:apps:tools` / `:apps:vault` are planned and are **not** in
`settings.gradle.kts`.

## Hard rules

These are not style preferences. Each has an ADR behind it and CI or ArchUnit enforcing it.

- **Do not redesign.** Decisions in `DECISIONS.md` are ACCEPTED. If you think one is wrong, propose
  a new ADR — never silently diverge. Check the highest existing ADR number before reserving one;
  three collisions have already happened from stale reservations.
- **Koin for DI, never Hilt.** Hilt's Gradle plugin is incompatible with AGP 9 (ADR-0010). Some
  older docs still say Hilt; they are wrong.
- **Kotlin only, Compose only, Coroutines + Flow only.** No Kover (AGP 9), no supabase-kt/Ktor
  (unproven on this toolchain) — Supabase is plain REST on Retrofit/Moshi/OkHttp (ADR-0029).
- **Module boundaries are enforced** by ArchUnit + Gradle: `feature → feature` forbidden;
  `vault → network/ai/analytics` forbidden; `feature → data` via Repository only; `core` depends on
  nothing internal.
- **Every feature route is wrapped in `FeatureHost`** — never a blank crash.
- **Money is integer paise** (`Long` / `bigint`) in the tracker domain; proportions are integer
  basis points. `BigDecimal` only in calculator/projection engines. Enforced by
  `checkTrackerMoneyPrecision`.
- **No secrets in the repo or the APK.** GitLeaks gates CI. `.env` is gitignored; `.env.example`
  holds empty placeholders.
- **DPDP: consent before any data leaves the device**, and every new user-data table must be added
  to `public.delete_my_data()` in the same migration — a miss breaks the 7-day erasure guarantee
  silently, with no failing test.
- **Every Postgres view is `security_invoker = on`.** A Postgres 15+ view otherwise runs as its
  owner, bypasses RLS, and returns every user's rows through PostgREST.
- **Design tokens only** in UI code — no raw hex, dp, sp, or `MaterialTheme.colorScheme` /
  `.typography` in a screen file. All user-visible strings go in `strings.xml` from birth.

## Branching

Branch from `develop`; PRs target `develop`. `develop` is DEV, `main` is PROD, and the
`develop → main` PR is the only path that ships. Never push to `main` directly (ADR-0032).

Conventional commits — CI derives the semver segment from them (`feat:` → minor, `type!:` or
`BREAKING CHANGE:` → major, else patch). Never hand-edit `platform/versions.json` or
`gradle.properties`; CI owns the bump.

## Build and test

```bash
./gradlew :apps:finance:app:assembleDebug          # build the app
./gradlew detekt                                    # lint
./gradlew :apps:finance:app:testDebugUnitTest       # unit tests incl. ArchUnit
./gradlew regressionCheck                           # the full pre-merge gate CI runs
python scripts/ci/doc_link_check.py                 # documentation link guard
python scripts/db/gen_schema_docs.py equiv          # schema/migration equivalence guard
```

`JAVA_HOME` must point at the Android Studio JBR (JDK 17+).

## Working method

**Tests first.** RED → GREEN → REFACTOR is constitutional (Article I), not a preference. Feature
work additionally cites a scenario ID from the QA catalog before any code exists (Article II).

**Specs before code.** Formalized specs live in `apps/<app>/specs/NNN-slug/` (spec-kit). The
active plan for Finance is
[`apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md`](apps/finance/docs/superpowers/plans/2026-08-08-design-v1-final-implementation-plan.md)
— check its §7 tracking table before creating a new phase directory.

**Close the loop when a phase ships.** `apps/finance/FEATURES.md` row, the module's own
`README.md`, and root `CHANGELOG.md` all move together, plus the surface registry row for any new
route. Each spec carries the closure task.

## Skills

`platform/skills/` holds repo-specific procedures — read the matching one *before* starting, not
after: new feature module, Room entity, Supabase object, Compose screen, pre-merge audit, release.
`.claude/skills/` holds general engineering skills for Claude Code.

## Where things live

| Path | Contents |
|---|---|
| `platform/` | Docs and contracts only — **no code** |
| `libs/core/`, `libs/settings/` | Shared library modules (design system lives here) |
| `apps/finance/` | The Finance app: `app/`, `data/`, `feature/<tab>/<name>/`, `docs/`, `specs/` |
| `supabase/` | `schemas/` (declarative, source of truth) → `migrations/` (executed history) |
| `web/` | React + Vite SPA |
| `scripts/ci/`, `scripts/db/` | CI and database tooling, each with local tests |

Feature modules are bucketed by owning tab on disk (`feature/plan/loans/`) while their Gradle
coordinates stay flat (`:apps:finance:feature:loans`), remapped via `projectDir` in
`settings.gradle.kts`. **A new feature module needs that remap** or Gradle configuration fails.