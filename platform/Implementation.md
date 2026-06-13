# Dhruv Platform — Implementation Plan

> Companion to `PLATFORM.md` / `DECISIONS.md`. Phases ordered per the latest decision.
> Governing principle: **migrate to a green build before adding new architecture.** Restructure
> first, behavior-change never in the same step.
> Built for a Claude Code workflow — each phase has a ready-to-paste session prompt and a
> Definition of Done (DoD). Don't merge a phase branch until its DoD passes.

Phase order: 0 → 1 → 2 → 3 → **4 Tools** → **5 Finance features + AI** → **6 Vault** → **7 Build & distribute APK**.

---

## Phase 0 — Prep (manual, ~1 hr)

1. **applicationId.** No Play launch now, so the Play immutability constraint doesn't bind — use the
   platform-consistent `com.dhruv.finance`. Only caveat: if a `com.dhruv.calc` listing already exists
   on Play that you intend to keep *later*, stay on `com.dhruv.calc` and rename the display only.
   Record the choice: `applicationId = __________`.
2. Rename the GitHub repo `dhruv-calc-android` → `dhruv` (old URL auto-redirects).
3. `git pull && git switch -c feat/monorepo-restructure`.
4. Commit the `platform/` docs at repo root.
5. Add root `CLAUDE.md`:
   ```
   # Dhruv monorepo
   Read before any work: @platform/AGENTS.md @platform/PLATFORM.md @platform/DECISIONS.md
   ## Build
   - ./gradlew :apps:finance:app:assembleDebug
   - ./gradlew detekt archUnitTest   # must pass before commit
   ## Rules
   - Do not redesign; decisions are locked in DECISIONS.md. Propose an ADR instead.
   - Module boundaries enforced by ArchUnit (feature→feature forbidden; vault→network/ai/analytics forbidden).
   ```
6. Baseline: `./gradlew assembleDebug` (confirm current app still builds), then `git tag pre-monorepo`.

**DoD:** repo renamed, branch created, platform docs + CLAUDE.md committed, baseline build green, tag set.

---

## Phase 1 — Monorepo skeleton + relocate Finance (green build)

**Highest-risk step. Keep the PR isolated. No behavior change allowed.**

1. Create `build-logic/` composite build with convention plugins: `dhruv.android.application`,
   `dhruv.android.library`, `dhruv.android.compose`, `dhruv.hilt`, `dhruv.detekt`.
2. Create root `settings.gradle.kts` (`includeBuild("build-logic")` + module includes).
3. `git mv` existing app sources/res/manifest into `apps/finance/app/`.
4. Rewrite the moved module's `build.gradle.kts` to apply the convention plugins.
5. Fix only what's needed to compile (paths/namespace) — nothing functional.
6. `./gradlew :apps:finance:app:assembleDebug`, install, smoke-test on device.

**Claude Code prompt (use plan mode — shift+tab):**
> Read @platform/PLATFORM.md §2 for the target layout. Set up the Gradle monorepo: build-logic
> convention plugins + root settings.gradle.kts, then `git mv` the existing app into
> `apps/finance/app` preserving history. Get `:apps:finance:app:assembleDebug` green with zero
> behavior change. Show the move plan before touching files.

**DoD:** app builds and runs identically to `pre-monorepo`; `git log --follow` on a moved file shows
history; PR is restructure-only.

---

## Phase 2 — Extract `:libs:core`

1. Create `libs/core` (android library + compose).
2. Move into core: theme/design tokens, glassmorphism components, North Star icon assets,
   `DhruvEntity` impl (with `hlc`), EncryptedDataStore wrapper, Keystore helpers, SQLCipher
   passphrase-wrap helper, Play Integrity wrapper (non-fatal), `CrashReporter` abstraction,
   `FeatureHost` + `FeatureErrorCard` + `FeatureDisabledCard`.
3. Point Finance at `:libs:core`; delete the now-duplicated local copies.
4. Add ArchUnit test + rules (`feature→feature` forbidden, `core→nothing internal`); add detekt config.
5. Wire CI (GitHub Actions) — 4 gates: static analysis (ktlint/detekt/lint), security
   (gitleaks + OWASP dependency-check), tests + ArchUnit, build (debug APK + **signed release APK**,
   APK-size delta). Reuse the existing keystore secrets.

**DoD:** Finance consumes core; ArchUnit + detekt pass; all 4 CI gates green on the PR.

---

## Phase 3 — `:libs:settings`

1. Create `libs/settings` (android library).
2. EncryptedDataStore-backed settings: theme, custom color (picker), font, biometric toggle, sync
   toggle (stub), **BYO Gemini key field** (ADR-0002).
3. Compose settings UI, consumed by Finance.
4. Firebase Remote Config wrapper in `:libs:core`: resolve **remote → cached → hardcoded default**.

**DoD:** settings persist encrypted; theme/color/font apply; flag resolution order verified
(airplane-mode test falls back to cached, fresh-install falls back to defaults).

---

## Phase 4 — `:apps:tools` (validate the module pattern on greenfield)

Building a clean app here first proves the FeatureHost + flag + ArchUnit pattern before you refactor
the existing Finance code into it.

1. Create `apps/tools/app` shell (MainActivity, NavHost, FeatureHost wiring); depends on
   `:libs:core` + `:libs:settings`.
2. Scaffold feature modules, each behind a flag + FeatureHost route: `notes`, `clipboard`, `timer`,
   `qr`, `weather`, `assistant`.
3. Implement in order: `timer` → `notes` → `qr` → `clipboard`. Keep `weather` + `assistant` flagged
   **off** (`enabled:false`) for now.
4. Wire `feature-flags/dhruv-tools.json` to Firebase Remote Config.
5. Each module: crash tag (`setCustomKey "module"`) + one Performance trace.

**Claude Code prompt:**
> Read @platform/PLATFORM.md §4. Scaffold apps/tools with NavHost + FeatureHost, all six feature
> modules stubbed behind flags, then implement timer, notes, qr, clipboard. ArchUnit must stay green.

**DoD:** Tools builds/runs; flags gate routes; disabled features render `FeatureDisabledCard`; a
forced exception in one feature shows `FeatureErrorCard` without crashing the shell; ArchUnit green.

---

## Phase 5 — Finance feature split + AI proxy + consent

1. Split Finance modes into `apps/finance/feature/*` (calculator, emi, sip, loan, currency, unit,
   scientific) behind FeatureHost + flags. (Reuse the pattern validated in Phase 4.)
2. **Cloudflare Worker proxy** (`worker/` dir or separate repo): holds the Gemini key, per-install
   token, per-device quota in KV. The key is never in the APK.
3. Assistant feature calls the proxy; **Settings BYO key bypasses the proxy** (ADR-0002).
4. **DPDP consent screen** before the first online AI call; store consent in EncryptedDataStore.
5. On-device Nano capability check → graceful fallback to proxy when absent (ADR-0007).
6. *(Optional de-risk)* **Release loop check**: tag a versioned **signed-APK GitHub Release** now to
   prove the build/sign/distribute loop early. Cheap with APK — no Play setup needed.

**DoD:** Finance features modularized; assistant works via proxy with quota enforced; BYO-key path
works; no online call possible before consent; no key extractable from the APK.

---

## Phase 6 — `:apps:vault` (after the key-model spec is final)

**Pre-req:** finalize ADR-0003 detail — Argon2id params, recovery-key format, key-wrap chain,
biometric convenience-unlock flow.

1. Create `apps/vault/app`: separate SQLCipher DB file, `allowBackup="false"`, `FLAG_SECURE` on all
   screens.
2. Master-password setup → Argon2id derive → vault key; **recovery key shown once**.
3. Biometric as a convenience unlock wrapping the derived key — password remains the source of
   truth, so a new fingerprint enrollment can never destroy data.
4. Vault entities do **not** implement DhruvEntity; **no** network/ai/analytics deps (ArchUnit
   enforces). Observability emits only `vault_module_error`.
5. Manual E2E-encrypted export/import for cross-device restore.

**DoD:** create/unlock/restore works; lose-phone restore via recovery key + export verified on a
second device; ArchUnit forbids vault→network/ai/analytics; biometric re-enroll does not lock the
user out.

---

## Phase 7 — Build & distribute signed APK (Play deferred)

1. CI builds a **signed release APK** with the existing keystore (secrets already in the repo) and
   **attaches it to a GitHub Release** on each version tag. Write the build step so APK→AAB is a
   one-line swap when Play is revisited.
2. Distribute the Release link directly (and to SK Hardware staff as needed); note that users must
   enable install-from-unknown-sources.
3. Crashlytics + Performance work over direct APK installs too — 48-hour watch after each release:
   crash-free > 99.5%, ANR < 0.47%.
4. **Privacy policy + DPDP consent** must be live wherever data is processed (this is *not* Play-
   gated). The **Play Data Safety form** and staged rollout wait until you plan a Play launch.

**DoD:** tagging a version produces a signed APK on a GitHub Release that installs and runs;
monitoring thresholds green over 48h; privacy policy published and consent gate enforced.

> When you later decide to ship on Play: enable Play App Signing, flip the build to AAB, add the
> internal/production tracks + Data Safety form + staged rollout. The rest of the pipeline is unchanged.

---

## Quick reference — module dependency direction
`apps:* → libs:settings → libs:core` and `apps:* → libs:core`. Features depend on core (+ data via
Repository), never on each other. Vault depends on core only — never network/ai/analytics.