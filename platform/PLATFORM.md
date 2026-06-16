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

| App            | Gradle path        | Status   | Purpose                                              |
|----------------|--------------------|----------|------------------------------------------------------|
| Finance        | `:apps:finance`    | active   | EMI, SIP, Loan, Currency, Unit/Scientific calc, AI assistant (migrated from dhruv-calc) |
| Tools          | `:apps:tools`      | planned  | Notes, Clipboard, Timer, QR, Weather, AI assistant   |
| Vault          | `:apps:vault`      | future   | Password manager, E2E-encrypted, biometric           |
| Health         | `:apps:health`     | future   | —                                                    |
| Relationship   | `:apps:relationship`| future  | —                                                    |
| Web sync hub   | (separate, later)  | future   | Official site + cross-device sync                    |

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
```

Shared config (Android, Compose, Koin, detekt, test setup) is centralised in `build-logic/`
convention plugins so modules stay thin. `:libs:core` is consumed as a normal project
dependency — no Gradle composite-build dance, no published artifact.

---

## 3. Tech stack (all apps)

| Concern         | Choice                                                                 |
|-----------------|------------------------------------------------------------------------|
| UI              | Jetpack Compose                                                        |
| DI              | Koin (Hilt deferred — Gradle plugin incompatible with AGP 9; see ADR-0010) |
| Navigation      | Single-activity NavHost                                                |
| DB (main)       | Room + Jetpack Security (EncryptedSharedPreferences for the key)       |
| DB (vault)      | SQLCipher — separate file, AES-256, separate key                       |
| Preferences     | EncryptedDataStore                                                     |
| Biometric       | BiometricPrompt Class 3 (Strong) — convenience unlock only (see §7)    |
| Keystore        | Android Keystore (hardware-backed)                                     |
| Network         | OkHttp + Retrofit + CertificatePinner                                  |
| On-device AI    | Gemini Nano via ML Kit GenAI / `com.google.android.gms.ai` — **progressive enhancement, not baseline** |
| Online AI       | Gemini API **through a proxy** (see §6), BYO-key override              |
| Sync            | WorkManager, offline-first (Phase 2; contract designed now)            |
| Feature flags   | Firebase Remote Config (free tier)                                     |
| Crash / Perf    | Firebase Crashlytics + Performance (free tier)                         |
| Future auth     | Firebase Auth (Dhruv ID SSO)                                           |
| Future sync     | Supabase (RLS) + Cloudflare R2                                         |
| CI/CD           | GitHub Actions: `develop` → signed APK + GitHub Release; `main` → signed AAB (Play-ready, deferred) |
| minSdk / target | minSdk 26 · targetSdk = latest Play-required (bump yearly)             |

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

### DhruvEntity (contract in `contracts/DhruvEntity.kt`)
`id` (UUID), `userId` (`"local"` until Dhruv ID ships), `createdAt`, `updatedAt`, `isSynced`,
`isDeleted`. Vault entities do **not** implement DhruvEntity.

`userId` is **indexed from day one**. When Dhruv ID ships, a one-time WorkManager migration
rewrites every `"local"` row to the real user id — cheap because of the index.

### Conflict resolution
- **Last-Write-Wins keyed on a Hybrid Logical Clock (HLC)**, not raw client epoch millis.
  This removes the cross-device clock-skew bug. (The old "Client-Wins always / Server-Wins never"
  rule is dropped — it contradicted LWW.)
- Notes additionally use field-level merge on top of HLC-LWW.

### Sync state machine (per entity)
`LOCAL_ONLY → PENDING_SYNC → SYNCED`, with `SYNC_FAILED → CONFLICT → RESOLVED`.
WorkManager `PeriodicWorkRequest`, 15-min interval, exponential backoff, CONNECTED constraint.

### Deletion & tombstones (also satisfies DPDP — §8)
Soft-delete is a UX state, **not** a permanent rule. Server hard-purges tombstones 90 days after
all known devices have synced past them. A user-requested erasure triggers a **guaranteed
hard-delete within the DPDP 7-day window**.

### Backup honesty
Android Auto Backup (≤25 MB, best-effort, fresh-install only) is **not** sync. Until Supabase
ships, non-vault data is device-local and the app says so. Vault is never auto-backed-up
(`allowBackup="false"`).

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

Tags on `develop` → GitHub Release with APK(s).
Tags on `main` (future) → Play Store internal track.

---

## 12. Versioning

`dhruv-{app}-vMAJOR.MINOR.PATCH` (MAJOR = breaking/arch, MINOR = new feature module, PATCH = fix).
`versionCode` auto-incremented by CI only. Cross-module compatibility tracked in `versions.json`.

---

## 13. Implementation order

Platform design is done. **`IMPLEMENTATION.md` is the authoritative phased plan (Phase 0–7).**
Summary order: skeleton + relocate Finance → `:libs:core` → `:libs:settings` → Tools app → Finance
feature split + AI/consent → Vault → build & distribute signed APK. Distribution is a signed APK via
GitHub Releases for now; Play is deferred.