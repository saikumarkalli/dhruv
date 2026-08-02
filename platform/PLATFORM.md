# Dhruv Platform — Architecture (Source of Truth)

> Status: **FINALIZED**. This document describes *what* the platform is.
> Rationale for each choice lives in `DECISIONS.md`. AI/agent session rules live in `AGENTS.md`.
> No code lives in `/platform`. Implementation follows this doc; this doc does not chase implementation.

---

## 1. What Dhruv is

A single-repo, multi-app Android ecosystem with a future web sync hub. Apps share a `:core`
library and a `:settings` module. Everything lives in **one repository** (`dhruv`) as Gradle
modules — there is no multi-repo split and no GitHub Packages publishing.

Apps (current + planned):

| App            | Gradle path        | Web Route      | Status   | Purpose                                              |
|----------------|--------------------|----------------|----------|------------------------------------------------------|
| Finance        | `:apps:finance`    | `/finance/*`   | active   | Tracker (net worth, expenses), calculators, converter|
| Tools          | `:apps:tools`      | `/tools/*`     | planned  | Notes, Clipboard, Timer, QR, Weather, AI assistant   |
| Vault          | `:apps:vault`      | `/vault/*`     | future   | Password manager, E2E-encrypted, biometric           |
| Health         | `:apps:health`     | `/health/*`    | future   | —                                                    |
| Relationship   | `:apps:relationship`| `/relationship/*`| future  | —                                                    |

---

## 2. Repository topology — monorepo

Single repo `dhruv`. Module boundaries do the isolation work that separate repos would have done,
without the publish/version/submodule overhead.

```
dhruv/
├── settings.gradle.kts          # includes all modules
├── build-logic/                 # Gradle convention plugins (shared Android/Compose/Koin config)
├── platform/                    # THIS folder — docs & contracts only, no code
│   ├── PLATFORM.md
│   ├── DECISIONS.md
│   ├── AGENTS.md
│   ├── versions.json
│   ├── contracts/DhruvEntity.kt
│   └── feature-flags/<app>.json
├── libs/
│   ├── core/                    # :libs:core  — DhruvEntity impl, theme tokens, security/Keystore utils
│   └── settings/                # :libs:settings — color picker, theme, font, sync, biometric toggles
└── apps/
    ├── finance/
    │   ├── app/                 # :apps:finance:app  — shell, MainActivity, hubs
    │   ├── data/                # :apps:finance:data — Room/repos/api (feature→data only)
    │   └── feature/             # Phase 4: calculator, loans, investments, tax, everyday,
    │       │                    #          currency, unit, date, time, assistant
    │       └── …                # (loans/investments/tax/everyday supersede emi/sip/loan)
    ├── tools/
    │   ├── app/
    │   └── feature/ { notes, clipboard, timer, qr, weather, assistant }
    └── vault/
        ├── app/
        └── feature/ { ... }
web/                             # Web SPA Monorepo
├── src/
│   ├── apps/                    # Route-based modules mapping to Android apps
│   │   ├── finance/
│   │   ├── tools/
│   │   └── vault/
│   └── shared/                  # Design system, auth, hooks, i18n
├── package.json
└── vite.config.ts
supabase/                        # Shared Backend config
├── migrations/                  # Schema definition (source of truth)
└── config.toml
```

Shared config (Android, Compose, Koin, detekt, test setup) is centralised in `build-logic/`
convention plugins so modules stay thin. `:libs:core` is consumed as a normal project
dependency — no Gradle composite-build dance, no published artifact.

---

## 3. Tech stack (all apps)

| Concern         | Android Choice                                                         | Web Choice                                       |
|-----------------|------------------------------------------------------------------------|--------------------------------------------------|
| UI              | Jetpack Compose                                                        | React 19 + Vanilla CSS Variables                 |
| DI / State      | Koin + StateFlow                                                       | React Context + React Query                      |
| Navigation      | Single-activity NavHost                                                | React Router v7                                  |
| DB (main)       | Room + Jetpack Security (EncryptedSharedPreferences for the key)       | In-memory + localStorage (session)               |
| DB (vault)      | SQLCipher — separate file, AES-256, separate key                       | WebCrypto API (future)                           |
| Preferences     | EncryptedDataStore                                                     | localStorage                                     |
| Biometric       | BiometricPrompt Class 3 (Strong) — convenience unlock only (see §7)    | WebAuthn (future)                                |
| Keystore        | Android Keystore (hardware-backed)                                     | —                                                |
| Network         | OkHttp + Retrofit + CertificatePinner                                  | supabase-js                                      |
| On-device AI    | Gemini Nano via ML Kit GenAI — progressive enhancement                 | —                                                |
| Online AI       | Gemini API **through a proxy** (see §6), BYO-key override              | Same                                             |
| Sync            | Tracker: Supabase-primary (ADR-0014); Calc: WorkManager offline-first  | Realtime (Supabase)                              |
| Feature flags   | Firebase Remote Config (free tier)                                     | Static JSON asset (same dhruv-finance.json)      |
| Crash / Perf    | Firebase Crashlytics + Performance (free tier)                         | errorReporter + Vercel Analytics                 |
| Auth            | Google Sign-In via Credential Manager → GoTrue                         | Google Sign-In via OAuth PKCE → GoTrue           |
| Primary DB      | Supabase (PostgreSQL) + RLS                                            | Supabase (PostgreSQL) + RLS                      |
| CI/CD           | GitHub Actions (`ci.yml`) → signed APK + GitHub Release                | GitHub Actions (`web-ci.yml`) → Vercel           |

---

## 4. Fault isolation (unchanged — the strongest part of the design)

Rule: a feature crash isolates to that feature; the app shell never crashes. Only a `:libs:core`
failure may show an app-level fallback.

- Every NavHost route is wrapped in `FeatureHost(key)`.
- `FeatureHost` catches per-feature errors, reports to `CrashReporter` tagged with the module,
  and renders `FeatureErrorCard` (never a blank crash).
- Disabled features render `FeatureDisabledCard`.

Module dependency rules — enforced by **Gradle** + **ArchUnit** tests in CI:

| Rule                          | Status     |
|-------------------------------|------------|
| `feature → feature`           | FORBIDDEN  |
| `vault → network / ai / analytics` | FORBIDDEN |
| `feature → data`              | via Repository only |
| `feature → core`              | allowed    |
| `data → core`                 | allowed    |
| `core → anything internal`    | FORBIDDEN (pure lib) |

---

## 5. Data, identity & sync

> **ADR-0014 override (tracker domain).** The rules below (DhruvEntity, HLC, offline-first sync)
> apply to **calculator/converter data and future cross-app sync**. Tracker data (net worth, assets,
> liabilities, expenses) uses **Supabase as the primary store** — no local Room, no DhruvEntity, no
> client-side conflict resolution. The server (PostgREST + RLS `user_id = auth.uid()`) is the single
> source of truth for tracker entities. Auth is Google Sign-In via Credential Manager → Supabase
> GoTrue. See ADR-0014 in `DECISIONS.md` for full rationale.

### DhruvEntity (contract in `contracts/DhruvEntity.kt`)
`id` (UUID), `userId` (`"local"` until Dhruv ID ships), `createdAt`, `updatedAt`, `isSynced`,
`isDeleted`. Vault entities do **not** implement DhruvEntity. **Tracker entities do not implement
DhruvEntity** — they live in Supabase, not Room (ADR-0014).

`userId` is **indexed from day one**. When Dhruv ID ships, a one-time WorkManager migration
rewrites every `"local"` row to the real user id — cheap because of the index.

### Conflict resolution
- **Last-Write-Wins keyed on a Hybrid Logical Clock (HLC)**, not raw client epoch millis.
  This removes the cross-device clock-skew bug. (The old "Client-Wins always / Server-Wins never"
  rule is dropped — it contradicted LWW.)
- Notes additionally use field-level merge on top of HLC-LWW.
- **Tracker domain**: no client-side conflict resolution — Supabase + RLS is the single source of
  truth (ADR-0014).

### Sync state machine (per entity)
`LOCAL_ONLY → PENDING_SYNC → SYNCED`, with `SYNC_FAILED → CONFLICT → RESOLVED`.
WorkManager `PeriodicWorkRequest`, 15-min interval, exponential backoff, CONNECTED constraint.
Applies to calculator/converter offline-first data only — tracker data is cloud-primary.

### Deletion & tombstones (also satisfies DPDP — §8)
Soft-delete is a UX state, **not** a permanent rule. Server hard-purges tombstones 90 days after
all known devices have synced past them. A user-requested erasure triggers a **guaranteed
hard-delete within the DPDP 7-day window**. Tracker erasure is fully in-app: "Delete my data"
hard-deletes all tracker rows via a `delete_my_account()` security-definer SQL function (ADR-0014).

### Backup honesty
Android Auto Backup (≤25 MB, best-effort, fresh-install only) is **not** sync. Calculator/converter
data without Supabase sync is device-local and the app says so. Tracker data lives in Supabase and
survives device changes by design. Vault is never auto-backed-up (`allowBackup="false"`).

---

## 6. AI strategy

### Online (Gemini API) — proxy + per-device quota, BYO override
- Default path: requests go through a **Cloudflare Worker proxy** that holds the Gemini key and
  enforces a per-device quota. The key is **never** in the APK (an embedded key is extractable and
  drainable).
- **BYO override**: a user can paste their own Gemini key in Settings; their calls then bypass the
  proxy quota and cost you nothing.
- A **consent screen precedes any online call** — data leaving the device is a DPDP trigger (§8).

### On-device (Gemini Nano) — progressive enhancement
Nano reaches a narrow device set (Pixel 8+, Galaxy S24+, Snapdragon 8 Gen 3+; the newest "v3" tier
is 2026 flagships only). So:
- Default assumption is **online or no AI**.
- A capability check gates Nano; absence falls back to the online path or a graceful "not available
  on this device" state. Nano is never assumed present.

---

## 7. Security (8 layers)

1. Android Keystore (hardware-backed key storage)
2. EncryptedDataStore (preferences)
3. Room + SQLCipher (data at rest)
4. OkHttp CertificatePinner (data in transit)
5. BiometricPrompt Class 3 (access control)
6. FLAG_SECURE (vault screens only)
7. **Play Integrity API** (not SafetyNet) + APK integrity — **non-fatal/warn-only**; gates vault
   only, never blocks app launch (root detection is bypassable + creates false positives)
8. ProGuard/R8 full obfuscation (release builds)

Implementation rules:
- **SQLCipher passphrase**: generate a random passphrase → wrap it with a Keystore key → store the
  wrapped blob; unwrap at DB open. SQLCipher never receives a Keystore key directly.
- **Biometric is convenience-only for vault.** The real vault decryption key is protected by the
  master password (§9), so a new fingerprint enrollment invalidating a Keystore key can never
  destroy vault data.

Launch security check returns `Pass | Fail(reason, fatal)`. Fatal blocks launch; non-fatal warns
and restricts vault only.

---

## 8. Compliance (first-class layer)

India DPDP Rules 2025 are in force (full enforcement May 2027). Binding requirements:

- **Consent before any processing** that leaves the device (no "legitimate interests" basis) —
  enforced by the pre-online-AI consent screen.
- **Children = under-18**: parental-consent rules; no profiling/targeted ads aimed at minors.
- **7-day erasure**: the guaranteed hard-delete path (§5) satisfies this.
- **Play Data Safety form** — deferred until a Play launch is planned. (The consent gate and 7-day
  erasure above are *not* Play-dependent and apply now regardless of APK distribution.)

The old "never hard delete" rule is amended to: *soft-delete UX, guaranteed hard-delete on request
or tombstone-GC timer.*

---

## 9. Vault (highest-risk module — built last)

- **Master-password-derived key** (Argon2id) is the real vault encryption key. Biometric/Keystore
  is a convenience unlock layer on top, never the sole guardian.
- **Recovery key** shown once at setup; without master password or recovery key, data is
  unrecoverable by design (true E2E). This is stated explicitly to the user.
- E2E-encrypted backup is **restorable on a new device** precisely because the key derives from a
  user secret, not a device-bound Keystore key.
- Vault DB: separate SQLCipher file, `allowBackup="false"`, manual user-initiated export only,
  never auto-sync. Observability emits `vault_module_error` and nothing else — no screen names, no
  user context.

---

## 10. Feature flags & observability (Firebase free tier)

- **Flags**: Firebase Remote Config. Priority: remote → cached last-known-good → hardcoded default.
  NavHost checks the flag before rendering any feature route; disabled → `FeatureDisabledCard`.
  Schema example in `feature-flags/<app>.json`.
- **Crashes/ANR**: Crashlytics, tagged per module via `setCustomKey("module", …)`.
- **Performance**: Firebase Performance, traced per feature (`notes_load`, `timer_start`, …).

---

## 11. CI/CD (right-sized for a solo maintainer)

**Branch strategy:**

| Branch | Purpose | Artifact | Trigger |
|--------|----------|----------|---------|
| `develop` | Default. All development, validation, APK distribution | Signed APK → GitHub Release | All PRs target here |
| `main` | Play Store only (future) | Signed AAB | PR from `develop` only |
| `feat/*` `fix/*` `chore/*` | Feature work | — | Branch from `develop`, PR back to `develop` |

Four unattended gates per PR — run on both `develop` and `main` (branch protection, you self-merge):

1. **Static analysis** — ktlint, detekt (per-module ruleset), Android lint
2. **Security scan** — OWASP dependency-check, GitLeaks, permission audit
3. **Tests** — unit (per module), integration (Room, DataStore), **ArchUnit** (dependency rules)
4. **Build** — debug + signed release artifact, size delta check
   - `develop`: signed **APK** → attached to GitHub Release on version tag
   - `main`: signed **AAB** → Play Store ready (deployment deferred)

**PR feedback** — a `pr-summary` job (`pull_request`-only, `if: always()`) posts a single sticky
comment per PR summarizing all 4 gate results, updating it in place on every push instead of
spamming new comments. It posts under a dedicated **"Dhruv Bot"** GitHub App identity (custom
avatar, `Issues: Read & write` only, installed solely on this repo), minting a short-lived token via
`actions/create-github-app-token@v1`; if that fails (secrets missing, App not installed), it falls
back to the default `GITHUB_TOKEN` (`github-actions[bot]`) so commenting never blocks merge. It is
informational only — never added to branch-protection required checks. See ADR-0012.

**Post-build jobs** (push to `develop`/`main` only, after all 4 gates pass):

- **`version-bump`** — atomically increments `MAJOR.MINOR.PATCH+1` for every active app in
  `platform/versions.json`, increments `VERSION_CODE`, and writes `VERSION_NAME` to
  `gradle.properties`. Commits all three changes back with `[skip ci]`.
- **`auto-tag`** — reads the new version from `platform/versions.json` and pushes
  `dhruv-<app>-v<version>` tag (idempotent: skips if the tag already exists). Tag push triggers
  the Release workflow. Requires a `RELEASE_TOKEN` PAT; falls back to `GITHUB_TOKEN` (tag is
  still created, but Release workflow must be started manually due to GitHub's anti-recursion rule).

Tags on `develop` → GitHub Release with APK(s).
Tags on `main` (future) → Play Store internal track.

---

## 12. Versioning

`dhruv-{app}-vMAJOR.MINOR.PATCH` (MAJOR = breaking/arch, MINOR = new feature module, PATCH = fix/merge).

- **Patch** is auto-incremented by CI on every merge to `develop`/`main` — no manual action needed.
- **Minor/Major** are bumped manually in `platform/versions.json` before merging when the change warrants it; CI will then auto-increment patch from the new baseline on the next merge.
- `versionCode` (Android build number) is also auto-incremented by CI on every merge.
- `VERSION_NAME` in `gradle.properties` is kept in sync automatically by the `version-bump` job.
- Cross-module compatibility tracked in `versions.json`.

---

## 13. Implementation order

Platform design is done. The authoritative phased plan is the **master roadmap**
(`docs/superpowers/plans/2026-07-12-master-roadmap-personal-app.md`), covering phases R0–R11 with
per-phase specs in `docs/superpowers/specs/`. Summary order: production hardening (R0) → CI cost
optimization (R1) → P1 net worth tracker (R2) → security layer (R3) → rates + notifications (R4)
→ expenses & budgets (R5) → recurring (R5b) → goals & insurance (R6) → reports (R7) → polish (R8)
→ retirement (R9) → automation (R10) → platform expansion (R11). Distribution is a signed APK via
GitHub Releases for now; Play is deferred (ADR-0008).