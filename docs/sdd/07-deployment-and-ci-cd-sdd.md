# Deployment & CI/CD SDD (07)

> **Status:** ACTIVE
> **Scope:** Defines CI workflows, environments, observability, and monorepo tooling.
> **Governing decision:** `platform/DECISIONS.md` ADR-0032 (dev/prod topology, branch-promotion
> model, declarative schema authorship). This document is the day-to-day restatement; ADR-0032 is
> the source of truth for *why*.

## 1. Monorepo Coexistence

The repository houses both Android (Gradle) and Web (npm) projects safely.

- **Gradle**: Ignores `web/` entirely.
- **NPM**: `package.json` lives in `web/` and commands run strictly inside that directory.
- **GitHub Actions**: each pipeline gates on its own changed paths (job-level `if:`, never
  trigger-level `paths:` — a path-filtered trigger never creates a check run for a non-matching PR,
  which leaves a required status check permanently pending; ADR-0026).

## 2. Branch-promotion model

`develop` is **DEV**. `main` is **PROD**. There is no third environment and no third long-lived
branch. The `develop → main` PR is the only path that touches prod data, prod web, or produces an
installable/publishable artifact.

| | `develop` (DEV) | `main` (PROD) |
|---|---|---|
| Supabase project | `dhruv-dev` | `dhruv-prod` |
| Web | Vercel **Preview** deployment (auto, every push/PR) | Vercel **Production** domain |
| Android | debug APK artifact (from the PR's `tests` job) | signed release APK → GitHub Release (+ future: AAB → Play) |
| Gate | none — auto | one approval click via a `trstringer/manual-approval` GitHub issue (native Environment reviewer rules need GitHub Pro on a private repo — see ADR-0032's correction) |

## 3. CI/CD Pipelines

### 3.1 Android Pipeline (`ci.yml`)
- Gate 0 (`changes`) splits every push/PR into `android` / `web` / `db` outputs from one changed-
  file list; static-analysis, tests, and the debug-APK build below run only when `android == true`.
- **Gate 1–3** (`pull_request` only, ADR-0026): ktlint + Detekt + Android Lint, GitLeaks (all PRs,
  including docs-only — secrets hide in markdown too), `regressionCheck` (unit tests + ArchUnit +
  JaCoCo coverage floor) + debug APK assembly, uploaded as a build artifact.
- **`release-approval` job** (push to `main`, `android` paths): opens a GitHub issue and pauses
  until the maintainer comments an approval keyword — the free substitute for a native Environment
  reviewer rule (ADR-0032 correction).
- **`release` job** (`needs: release-approval`, `environment: prod` for secret scoping only):
  derives the semver segment from commit types (ADR-0025, amended by ADR-0032 — `main` is no
  longer forced to `patch`), bumps `platform/versions.json` + `gradle.properties`, builds the
  **signed** release APK against `prod` Environment secrets, verifies it (signed, size budget, no
  placeholder secrets, and — new — contains the `dhruv-prod` Supabase ref and NOT the `dhruv-dev`
  ref), tags, and publishes a GitHub Release.

### 3.2 Web Pipeline (`web-ci.yml`)
- **Gates** (job-level skip when no `web/**`/`platform/feature-flags/**` files changed):
  1. `npm run lint` (ESLint)
  2. `npm run typecheck` (tsc)
  3. `npm test` (Vitest)
  4. `npm run build` (Vite)
- **Deploy**: NOT done by this workflow. Vercel's own **Git integration** builds and deploys
  directly from GitHub on Vercel's infrastructure — root directory `web/`, Production Branch
  `main`, every other branch/PR gets an automatic Preview deployment. This costs zero GitHub Actions
  runner-minutes (ADR-0026's `≤90 min/PR` budget). `web-ci.yml` is a merge gate only.

### 3.3 Database Pipeline (`supabase-migrate.yml`)
- **`verify`** (PR, paths `supabase/**`): the ADR-0032 equivalence guard
  (`scripts/db/gen_schema_docs.py equiv` — `supabase/schemas/` declarative state must match
  `supabase/migrations/*.sql` executed history) + `supabase/SCHEMA.md` freshness + best-effort
  `web/src/shared/types/database.ts` freshness against the live `dhruv-dev` schema.
- **`apply-dev`** (push to `develop`, `environment: dev`, ungated): `supabase db push` to
  `dhruv-dev`. The CLI's own remote migration-history tracking is the drift guard — it refuses to
  push if `dhruv-dev`'s applied history has diverged from this directory's files (e.g. a hand-run
  dashboard statement).
- **`prod-plan`** (push to `main`, no environment, no secrets): git-diffs the migration files
  changed by this push and greps them for destructive statements (`DROP`/`TRUNCATE`/`ALTER … DROP`/
  `RENAME`), writing both to the job summary.
- **`prod-approval`** (`needs: prod-plan`): the same free approval-issue gate as `release-approval`
  above — the maintainer opens `prod-plan`'s already-finished summary before commenting approve.
- **`prod-apply`** (`needs: prod-plan, prod-approval`, `environment: prod` for secret scoping):
  same `db push` against `dhruv-prod`, after approval.
- One migration set, no environment branching in SQL — `supabase/migrations/*.sql` is byte-
  identical between the dev and prod applies (ADR-0032 decision 3).

### 3.4 Keep-alive (`supabase-keepalive.yml`)
Matrix over `dev`/`prod`, pinging each project's PostgREST root with its anon key every 5 days
(free-tier projects pause after ~7 days idle). Deliberately does **not** use the `dev`/`prod`
GitHub Environments at all — a plain repo secret keeps this ping simple and independent of
whichever approval mechanism `release`/`prod-apply` end up using (today: the free issue-based gate;
if GitHub Pro is purchased later: a native required-reviewer rule, which a job declaring
`environment: prod` would then inherit and which *would* stall an unattended scheduled ping). The
anon key/URL pair is safe as a plain repo secret regardless (publishable-by-design under RLS),
stored separately from the Environments' copies either way.

## 4. Environment Management

| Env | Android config | Web config | Supabase project | Secrets live in |
|---|---|---|---|---|
| **Dev** | local `.env` (gitignored, dev values) | `web/.env.local` (gitignored) or Vercel Preview/Development env vars | `dhruv-dev` | GitHub Environment `dev` |
| **Prod** | `.env` written by `ci.yml`'s `release` job from `prod` Environment secrets, immediately before `assembleRelease` | Vercel Production env vars | `dhruv-prod` | GitHub Environment `prod` |

**Android env selection is zero-code** (ADR-0032 decision 6) — no product flavors, no
`applicationIdSuffix`. The existing `secrets` Gradle plugin reads whatever `.env` is present at
build time. The accepted risk: a release APK built **locally** (not through CI) would silently
carry dev keys — mitigated by the release job's dev-ref guard (§3.1) and by the CI-built APK being
the one actually published.

`GOOGLE_WEB_CLIENT_ID` and `GEMINI_API_KEY` are identical in both environments (ADR-0031's single
cross-app identity; Gemini has no per-environment concept) and are kept as repo-level secrets
rather than duplicated per Environment.

## 5. Observability

| Layer | Android | Web |
|---|---|---|
| **Crashlytics** | Firebase Crashlytics | `errorReporter` console log (V1), Sentry (V2) |
| **Performance** | Firebase Performance | Vercel Analytics (V1 Web Vitals) |

## 6. Versioning Matrix (`versions.json`)

The platform's version matrix (`platform/versions.json`) tracks Android auto-incremented versions
(bumped only by `main` pushes now, ADR-0032) alongside manually-bumped Web application versions.
