# Deployment & CI/CD SDD (07)

> **Status:** ACTIVE
> **Scope:** Defines CI workflows, environments, observability, and monorepo tooling.

## 1. Monorepo Coexistence

The repository houses both Android (Gradle) and Web (npm) projects safely.

- **Gradle**: Ignores `web/` entirely.
- **NPM**: `package.json` lives in `web/` and commands run strictly inside that directory.
- **GitHub Actions**: Uses path-based triggers to execute only relevant workflows.

## 2. CI/CD Pipelines

### 2.1 Android Pipeline (`ci.yml`)
- Triggers on `apps/**`, `libs/**`.
- Runs Detekt, Spotless, JaCoCo, and assembles the Release APK.
- Auto-increments version and attaches APK to a GitHub Release.

### 2.2 Web Pipeline (`web-ci.yml`)
- Triggers on `web/**`, `platform/feature-flags/**`, `supabase/**`.
- **Gates**:
  1. `npm run lint` (ESLint)
  2. `npm run typecheck` (tsc)
  3. `npm test` (Vitest)
  4. `npm run build` (Vite)
- **Deploy**: Automatically deploys the build artifact to Vercel upon merging to `main`.

## 3. Environment Management

| Env | Android config | Web config | Supabase Project |
|---|---|---|---|
| **Dev** | `local.properties` (gitignored) | `.env.local` (gitignored) | `dhruv-dev` |
| **Prod** | GitHub Actions Secrets | Vercel Env Vars | `dhruv-prod` |

## 4. Observability

| Layer | Android | Web |
|---|---|---|
| **Crashlytics** | Firebase Crashlytics | `errorReporter` console log (V1), Sentry (V2) |
| **Performance** | Firebase Performance | Vercel Analytics (V1 Web Vitals) |

## 5. Versioning Matrix (`versions.json`)

The platform's version matrix (`platform/versions.json`) tracks Android auto-incremented versions alongside manually-bumped Web application versions.
