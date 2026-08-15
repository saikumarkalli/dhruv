# Dev Readiness Checklist — Plugins, Dependencies, Connections

> Status: **AUDITED 2026-07-12** against `gradle/libs.versions.toml`, `.github/workflows/*`, and
> the full spec set. What must exist in the toolchain/services before each phase can build.
> Companion to the master roadmap (`2026-07-12-master-roadmap-personal-app.md`).
> Legend: ✅ already present · ➕ add at that phase · 🧍 manual human step (cannot be automated).

## 1. Gradle plugins (build-logic level)

| Plugin | Needed by | Status |
|---|---|---|
| `com.google.gms.google-services` | R0 (Firebase wiring, H1) | ➕ **missing — the reason Crashlytics/Remote Config are inert today** |
| `com.google.firebase.crashlytics` (gradle plugin, mapping upload) | R0 | ➕ missing |
| `org.owasp.dependencycheck` | R0/R1 (M1 — currently a no-op gate) | ➕ missing; wire into the weekly workflow per CI-cost spec D4 |
| Secrets plugin (`.env` mechanism) | all | ✅ present |
| JaCoCo, detekt, ktlint, convention plugins | all | ✅ present |
| OSS licenses (AboutLibraries vs `oss-licenses-plugin`) | R8/About "Licenses" row | ➕ **decision needed** — both are Gradle plugins on AGP 9 = compat-unknown class (Hilt/Kover lesson); verify on a spike branch first or drop the row |
| Baseline profiles (`androidx.baselineprofile`) | perf polish (audit C) | ⚠️ deferred — macrobenchmark plugin on AGP 9 = same compat-unknown class; not phase-blocking |

## 2. Version-catalog additions (per phase, all plain AndroidX/Google)

| Phase | Artifacts | Catalog status |
|---|---|---|
| P1 (in flight) | `androidx.credentials:credentials`, `credentials-play-services-auth`, `com.google.android.libraries.identity.googleid:googleid` | ➕ not in catalog on this branch (may exist on `feat/networth-tracker` — verify at merge) |
| R0 | `firebase-bom`, `firebase-crashlytics`, `firebase-perf`, `firebase-config` | ✅ artifacts declared (`libs.versions.toml:99-115`) — blocked only by the missing plugins above |
| R3 | `androidx.biometric:biometric` | ✅ declared at **1.1.0** (`libs.versions.toml:48`) — has `setAllowedAuthenticators`; evaluate 1.2.x at build time, not required |
| R4 / currency plan | `androidx.work:work-runtime-ktx`, `io.insert-koin:koin-androidx-workmanager` | ➕ missing |
| ADR-0024 (with R3) | `androidx.navigation:navigation-compose` | ✅ declared at 2.8.9 (`libs.versions.toml:14,69`) — unused today (no NavHost in code, NAV1) |
| R8 | `androidx.glance:glance-appwidget`, `glance-material3` | ➕ missing |
| R7/R9 | none (PdfDocument + ZipOutputStream are platform; XIRR is pure Kotlin) | — |

Rule (standing): additions go through the catalog + build-verify before dependent code
(playbook framework-protection rule 6).

## 3. External service connections (🧍 human, in order of need)

### Supabase (blocks P1 — already on the P1 execution checklist, restated)
- [ ] Create project; note **region** (named in consent copy).
- [ ] Enable `moddatetime` extension; run P1 SQL (tables, RLS, triggers, `delete_my_account()`).
- [ ] Copy `SUPABASE_URL` + `SUPABASE_ANON_KEY` → local `.env` + GitHub secrets.
- [ ] Later: R7 report RPCs deploy (M0 gate); R8 verify **pg_cron** on free tier + purge job.

### Google Cloud (blocks P1 sign-in — M0 gate)
- [ ] OAuth consent + **Web client ID** (`GOOGLE_WEB_CLIENT_ID`) wired into Supabase Google provider.
- [ ] Register SHA-1s from `signingReport` (debug + release keystore).

### Firebase (blocks R0)
- [ ] Create project, add Android app (both SHA-1s), download `google-services.json`.
- [ ] **Decision needed:** commit the json (it is not a secret by Google's definition, but the
      repo's GitLeaks posture may flag it) vs gitignore + CI-inject from a secret. Recommend:
      CI-inject — consistent with the `.env` mechanism, zero GitLeaks debate.

### GitHub (repo settings)
- [ ] Secrets present today: `KEYSTORE_BASE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`,
      `DHRUV_BOT_APP_ID`, `DHRUV_BOT_PRIVATE_KEY` (verified in workflows).
- [ ] Add at P1 release: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GOOGLE_WEB_CLIENT_ID`
      (release job writes `.env` per ADR-0014). Add `GOOGLE_SERVICES_JSON` if CI-inject chosen.
- [ ] `RELEASE_TOKEN` PAT (PLATFORM.md §11) — optional; absent today, auto-tag falls back to
      `GITHUB_TOKEN` (Release workflow then needs manual start).
- [ ] Branch protection: **"Require branches to be up to date"** — hard prerequisite for R1 (D3).
- [ ] At R4: create fine-grained PAT (this repo, Contents:Read, 1-yr) — goes into the app via
      Settings, never into the repo.

### No-key services (nothing to set up)
Frankfurter + Fawaz currency APIs (keyless by design); Gemini = BYO user key post-R0
(Cloudflare Worker proxy stays deferred unless BYO proves insufficient).

## 4. Local dev environment

- [x] `JAVA_HOME` = Android Studio JBR (JDK 17+) — known requirement.
- [ ] `.env` populated locally (gitignored); `.env.example` stays committed with blanks.
- [ ] Device matrix for R3 (SEC1): one **API 26–29** emulator/device (Keyguard credential
      fallback path) + one API 30+ (combo path). One **Play-services emulator or physical
      device** for the P1 OAuth M0 smoke.
- [x] Windows Robolectric-SQLite limitation — Room tested via fakes (established convention).

## 5. Agent / CLI tooling (this machine)

| Tool | Status | Action |
|---|---|---|
| `gh` CLI | ❌ **not installed** (verified — `gh` unrecognized) | Install + `gh auth login`. The harness's GitHub operations (PRs, releases, secrets) all route through it; today only raw git works |
| Supabase MCP server | not configured | **Recommended from P1 onward**: `claude mcp add` the official Supabase MCP (personal access token) — lets sessions inspect schema, run SQL, verify RLS/RPCs directly instead of blind-writing migration files |
| Context7 MCP (library docs) | not configured | Optional — AndroidX/Compose docs on tap; low priority, WebSearch covers it |
| Project plugins (superpowers, ui-ux-pro-max, caveman, dhruv-* agents/skills) | ✅ installed | — |
| claude.ai connectors (Notion, Make; Gmail/GCal/GDrive unauthorized) | connected/unused | Not needed for this roadmap; ignore |
| `adb` / Android Studio | assumed present | Needed for M0 smokes + R3 manual checklist |

## 6. Decisions this audit adds to the queue

1. `google-services.json`: commit vs CI-inject (recommend CI-inject) — R0.
2. Licenses plugin: AboutLibraries spike vs dropping the About›Licenses row — R8.
3. `RELEASE_TOKEN` PAT: create vs live with manual Release-workflow starts — anytime.

Everything else in the roadmap needs **no** new accounts, paid services, or unproven Gradle
plugins — consistent with the cost-first driver (ADR-0001) and the AGP-9 compatibility doctrine
(ADR-0010/0013/0014).
