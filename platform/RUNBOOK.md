# Dhruv Platform — Detailed Implementation Runbook

> Granular step-by-step. Every step is a single action — a command, a file edit, or a Claude Code
> session. Steps marked 🧑 are manual (you do it). Steps marked 🤖 are Claude Code sessions.
> Steps marked ✅ are verification checkpoints — don't proceed until they pass.
> Steps marked 📦 are release checkpoints — tag and ship.
>
> **Incremental delivery**: from Phase 2 onward, every phase ends with a tagged APK on GitHub
> Releases. You can stop after any phase and pick up later.

---

## PHASE 0 — Prep (~1 hr, all manual)

### 0.1 — Decide applicationId
🧑 No Play listing exists yet → use `com.dhruv.finance`.
   If you have an existing Play listing under `com.dhruv.calc` you want to keep → stay on
   `com.dhruv.calc`, change only the display name to "Dhruv Finance".

   **Write your choice down. Every later step uses this.**

### 0.2 — Rename the GitHub repo
🧑 GitHub → `dhruv-calc-android` → Settings → Repository name → change to `dhruv`.
   GitHub auto-redirects the old URL. Update your local remote:
   ```bash
   cd dhruv-calc-android
   git remote set-url origin https://github.com/<your-username>/dhruv.git
   cd .. && mv dhruv-calc-android dhruv && cd dhruv
   ```

### 0.3 — Create the restructure branch
🧑
```bash
git pull origin main
git switch -c feat/monorepo-restructure
```

### 0.4 — Commit platform docs
🧑 Copy the platform files into the repo:
```bash
mkdir -p platform/contracts platform/feature-flags platform/adr
# Copy PLATFORM.md, DECISIONS.md, AGENTS.md, IMPLEMENTATION.md, RUNBOOK.md into platform/
# Copy contracts/DhruvEntity.kt into platform/contracts/
# Copy versions.json into platform/
# Copy feature-flags/dhruv-tools.json into platform/feature-flags/
```
```bash
git add platform/
git commit -m "docs: add founding platform docs"
```

### 0.5 — Create root CLAUDE.md
🧑 Create `CLAUDE.md` at the repo root:
```markdown
# Dhruv — Android monorepo

Read before any work:
@platform/AGENTS.md
@platform/PLATFORM.md
@platform/DECISIONS.md

## Project
- Kotlin + Jetpack Compose monorepo: multiple apps sharing :libs:core and :libs:settings
- Architecture: single-activity NavHost, Hilt DI, Room + EncryptedDataStore, MVVM
- minSdk 26, targetSdk latest

## Skills (read the relevant skill BEFORE doing the task)
- New feature module → read platform/skills/dhruv-feature-scaffold/SKILL.md
- New Room entity / data layer → read platform/skills/dhruv-room-entity/SKILL.md
- New Compose screen → read platform/skills/dhruv-compose-screen/SKILL.md
- Pre-merge check → read platform/skills/dhruv-module-audit/SKILL.md
- Version bump / release → read platform/skills/dhruv-release/SKILL.md

## Build commands
- ./gradlew :apps:finance:app:assembleDebug
- ./gradlew :apps:tools:app:assembleDebug
- ./gradlew detekt
- ./gradlew test

## Hard rules
- Do not redesign architecture. Decisions locked in DECISIONS.md. Propose an ADR instead.
- Module boundaries enforced by ArchUnit (feature→feature FORBIDDEN, vault→network/ai/analytics FORBIDDEN).
- Every feature route wrapped in FeatureHost — never a blank crash.
- No secrets or API keys in the repo or APK. GitLeaks gates CI.
- Kotlin only, Compose only, Hilt only, Coroutines+Flow only.
- DPDP: consent screen before any data leaves the device.
```
```bash
git add CLAUDE.md
git commit -m "chore: add root CLAUDE.md for Claude Code sessions"
```

### 0.6 — Baseline build + tag
🧑
```bash
./gradlew assembleDebug
```
✅ Build succeeds. Install on device — app works as before.
```bash
git tag pre-monorepo
git push origin feat/monorepo-restructure --tags
```

---

## PHASE 1 — Monorepo skeleton + relocate Finance

**Goal: app compiles and runs inside the new layout. Zero behavior change.**

### 1.1 — Create build-logic convention plugins
🤖 **Claude Code** (plan mode — shift+tab):
```
We're restructuring this repo into a Gradle monorepo. Phase 1.

Read @platform/PLATFORM.md §2 for the target module layout.

Create build-logic/ composite build with convention plugins:
- dhruv.android.application
- dhruv.android.library
- dhruv.android.compose
- dhruv.hilt
- dhruv.detekt

Use the EXISTING app/build.gradle.kts as reference for compileSdk, targetSdk,
dependencies, signing config. Extract shared config into plugins.

DO NOT move any app code yet. Show the plan first.
```
✅ `build-logic/` compiles standalone.

### 1.2 — Root settings.gradle.kts
🤖 **Claude Code** (plan mode):
```
Continue Phase 1. Create root settings.gradle.kts:
- includeBuild("build-logic")
- Module includes: :apps:finance:app, :libs:core, :libs:settings
- Root project name: "dhruv"
- Copy repository config from existing settings file.

DO NOT move files yet. Build won't compile — expected.
```

### 1.3 — Relocate app into apps/finance/app
🤖 **Claude Code** (plan mode):
```
Continue Phase 1. Relocate existing app using git mv (preserve history):

1. git mv app/src → apps/finance/app/src
2. git mv app/proguard-rules.pro → apps/finance/app/
3. git mv app/google-services.json → apps/finance/app/ (if exists)
4. Create apps/finance/app/build.gradle.kts applying convention plugins
5. Comment out :libs:core and :libs:settings includes (don't exist yet)
6. Delete old app/build.gradle.kts

Goal: ./gradlew :apps:finance:app:assembleDebug MUST succeed.
Fix only paths/namespace. NO functional changes. Show the move plan first.
```

### 1.4 — Create stub libs
🤖 **Claude Code:**
```
Create minimal stub modules:
1. libs/core/build.gradle.kts (dhruv.android.library + compose), namespace com.dhruv.core
2. libs/settings/build.gradle.kts (dhruv.android.library), namespace com.dhruv.settings
Both empty — just valid modules. Uncomment includes in settings.gradle.kts.
```

### 1.5 — Clean up + verify
🤖 **Claude Code:**
```
Remove old app/ directory. Clean orphaned root build files. Fix .gitignore.
Run: ./gradlew :apps:finance:app:assembleDebug
```
✅ **All must pass:**
```bash
./gradlew :apps:finance:app:assembleDebug           # builds
adb install apps/finance/app/build/outputs/apk/debug/*.apk  # runs
git log --follow -- apps/finance/app/src/main/java/<any-file>  # shows history
```
```bash
git add -A && git commit -m "refactor: monorepo skeleton — relocate finance, add build-logic"
git push origin feat/monorepo-restructure
```
🧑 PR → self-review (restructure only) → merge to main.

---

## PHASE 2 — `:libs:core` + CI + release pipeline → 📦 `v0.1.0`

🧑 `git switch -c feat/libs-core`

### 2.1 — Identify extractable code
🤖 **Claude Code:**
```
Scan apps/finance/app/src. List every file belonging in :libs:core:
- Theme/design tokens, glassmorphism components, North Star icon
- DhruvEntity (match platform/contracts/DhruvEntity.kt — note hlc field)
- EncryptedDataStore wrapper, Keystore helpers, SQLCipher passphrase-wrap
- CrashReporter, FeatureHost, FeatureErrorCard, FeatureDisabledCard
List only — don't move yet.
```

### 2.2 — Move code to core
🤖 **Claude Code** (plan mode):
```
Move identified files from apps/finance/app into libs/core using git mv.
Update packages to com.dhruv.core.*. Add dependencies to libs/core/build.gradle.kts.
Update Finance to depend on :libs:core. Fix all imports.
Build must stay green. Show the move plan first.
```

### 2.3 — Implement missing core pieces
🤖 **Claude Code:**
```
Read platform/contracts/DhruvEntity.kt. Implement in :libs:core:
1. DhruvEntity interface + BaseEntity abstract class
2. HLC (Hybrid Logical Clock) utility for LWW ordering
3. FeatureHost composable (catches errors, shows FeatureErrorCard, reports to CrashReporter)
4. FeatureDisabledCard composable
5. FeatureFlagResolver: remote → cached → hardcoded
6. CrashReporter interface (Crashlytics impl with setCustomKey("module"))
7. Play Integrity wrapper: Pass | Fail(reason, fatal)
```

### 2.4 — ArchUnit dependency rules
🤖 **Claude Code:**
```
Add ArchUnit tests enforcing:
1. feature→feature: FORBIDDEN
2. core→app-specific: FORBIDDEN
3. vault→network/ai/analytics: FORBIDDEN
4. feature→data: only through Repository classes
Run as part of ./gradlew test.
```

### 2.5 — CI pipeline
🤖 **Claude Code:**
```
Create .github/workflows/ci.yml — 4 gates, triggered on push to main + PRs:
1. Static analysis: ktlint, detekt, lint
2. Security: GitLeaks, OWASP dependency-check
3. Tests: ./gradlew test (unit + ArchUnit)
4. Build: debug APK + signed release APK (reuse existing keystore secrets)
   + APK size check (fail if >50MB)
```

### 2.6 — GitHub Release workflow
🤖 **Claude Code:**
```
Create .github/workflows/release.yml:
- Trigger: push tag matching "v*"
- Depends on: ci.yml (all 4 gates must pass first)
- Build signed release APK for each app that exists
- Create GitHub Release with tag name + auto-generated changelog
- Attach APK(s) as release assets
- Use a BUILD_TYPE variable so APK→AAB is a one-line swap later

Also create scripts/bump-version.sh:
- Reads current version from platform/versions.json
- Accepts: major | minor | patch
- Updates versions.json + app build.gradle.kts versionName
- Auto-increments versionCode
- Git commit + tag
- Prints: "Run 'git push origin main --tags' to trigger release"
```

### 2.7 — Verify + release
✅ **All must pass:**
```bash
./gradlew :apps:finance:app:assembleDebug
./gradlew :libs:core:assembleDebug
./gradlew test                             # ArchUnit passes
./gradlew detekt                           # clean
# Push PR → all 4 CI gates green
```
🧑 Merge PR to main.

📦 **First release:**
```bash
./scripts/bump-version.sh minor            # → v0.1.0
git push origin main --tags
```
✅ GitHub Release appears with `dhruv-finance-v0.1.0-release.apk` attached.
   Download → install → runs correctly.

---

## PHASE 3 — `:libs:settings` → 📦 `v0.2.0`

🧑 `git switch -c feat/libs-settings`

### 3.1 — Settings data layer
🤖 **Claude Code:**
```
Implement libs/settings data layer:
1. SettingsRepository backed by EncryptedDataStore
2. Data: AppTheme, AccentColor, FontFamily, BiometricEnabled, SyncEnabled (stub),
   GeminiApiKey (nullable, encrypted — BYO key per ADR-0002)
3. Hilt module providing SettingsRepository as singleton
4. Depends on :libs:core
```

### 3.2 — Settings UI
🤖 **Claude Code:**
```
Implement libs/settings Compose UI:
1. SettingsScreen: Appearance (theme/color/font), Security (biometric),
   AI (BYO Gemini key field, masked), Sync (disabled stub), About
2. SettingsViewModel
3. DhruvTheme composable in :libs:core reading settings → MaterialTheme
4. Wire into Finance: Settings icon → SettingsScreen
Use glassmorphism tokens from :libs:core.
```

### 3.3 — Firebase Remote Config
🤖 **Claude Code:**
```
In :libs:core, implement FeatureFlagProvider:
1. FirebaseRemoteConfigProvider (fetches + caches)
2. LocalFlagCache (SharedPrefs last-known-good)
3. HardcodedDefaults (bundled JSON)
4. FeatureFlagResolver: remote → cached → hardcoded
5. NavHost checks flag before rendering route → FeatureDisabledCard if off
Initialize Firebase in Application class.
```

### 3.4 — Verify + release
✅ On device: change theme (applies), set color (persists), paste dummy key (masked, persists),
   airplane mode (cached flags work), fresh install (hardcoded defaults).
```bash
./gradlew test && ./gradlew detekt
```
🧑 Merge PR.

📦 **Release:**
```bash
./scripts/bump-version.sh minor            # → v0.2.0
git push origin main --tags
```
✅ GitHub Release: `dhruv-finance-v0.2.0-release.apk` — now with settings.

---

## PHASE 4 — `:apps:tools` → 📦 `v0.3.0`

🧑 `git switch -c feat/apps-tools`

### 4.1 — Tools app shell
🤖 **Claude Code:**
```
Read @platform/PLATFORM.md §4. Create apps/tools/:
1. apps/tools/app/ — MainActivity, NavHost, bottom nav, FeatureHost per route
2. applicationId: com.dhruv.tools, depends on :libs:core + :libs:settings
3. Update settings.gradle.kts
4. Build: ./gradlew :apps:tools:app:assembleDebug
```

### 4.2 — Scaffold feature stubs
🤖 **Claude Code:**
```
Create apps/tools/feature/{notes,clipboard,timer,qr,weather,assistant}:
Each: build.gradle.kts + placeholder Screen + Navigation extension.
Wire into NavHost. Set flags: timer/notes/qr/clipboard enabled, weather/assistant disabled.
Update settings.gradle.kts. Disabled features → FeatureDisabledCard.
```

### 4.3 — Implement Timer
🤖 **Claude Code:**
```
Implement apps/tools/feature/timer:
Countdown + stopwatch modes. Compose UI with glassmorphism. Foreground notification.
Crashlytics module tag + Performance trace. Unit tests for timer logic. In-memory state only.
```

### 4.4 — Implement Notes
🤖 **Claude Code:**
```
Implement apps/tools/feature/notes:
Room entity (NoteEntity implementing DhruvEntity with hlc). NoteDao, NoteRepository.
NotesListScreen (list, FAB, swipe-delete). NoteEditorScreen (auto-save).
Crashlytics tag + trace. Unit tests for DAO (in-memory Room).
Data access ONLY through Repository.
```

### 4.5 — Implement QR
🤖 **Claude Code:**
```
Implement apps/tools/feature/qr:
Scanner (CameraX + ML Kit barcode). Generator (ZXing). Camera permission handling.
Share/save. Crashlytics tag + trace. Unit tests for generation.
```

### 4.6 — Implement Clipboard
🤖 **Claude Code:**
```
Implement apps/tools/feature/clipboard:
ClipEntry Room entity. Clipboard monitor service (local only, one-time info dialog).
History list, tap to copy, swipe to delete, long-press to pin.
Crashlytics tag + trace. Unit tests.
```

### 4.7 — Fault isolation test
✅ **Critical — validates the entire pattern:**
- Force crash in TimerViewModel → FeatureErrorCard shows, other features work, shell alive
- Disable "notes" in Remote Config → FeatureDisabledCard, others unaffected
- Weather/assistant (flagged off) → FeatureDisabledCard
- `./gradlew test` → ArchUnit green
- Full CI green

🧑 Merge PR.

📦 **Release:**
```bash
./scripts/bump-version.sh minor            # → v0.3.0
git push origin main --tags
```
✅ GitHub Release: **two APKs** — `dhruv-finance-v0.3.0` + `dhruv-tools-v0.3.0`.

---

## PHASE 5 — Finance feature split + AI + consent → 📦 `v0.4.0`

🧑 `git switch -c feat/finance-ai`

### 5.1 — Split Finance into feature modules
🤖 **Claude Code** (plan mode):
```
Split Finance into apps/finance/feature/{calculator,emi,sip,loan,currency,unit,scientific,assistant}.
git mv relevant code. Each behind FeatureHost + flag. Follow the exact tools pattern.
Create platform/feature-flags/dhruv-finance.json (all enabled except assistant).
Build green. Show move plan first.
```

### 5.2 — Cloudflare Worker proxy
🤖 **Claude Code:**
```
Create worker/ at repo root — Cloudflare Worker (TypeScript):
POST /v1/chat. Reads GEMINI_API_KEY from Worker secret (never in code).
X-Device-Id header for per-device quota (KV, 50 req/day, daily reset).
Returns 429 on quota exceeded. CORS. README with setup/deploy steps.
```

### 5.3 — Android AI client
🤖 **Claude Code:**
```
In :libs:core, create AI abstraction:
1. AiProvider interface: suspend fun chat(prompt): Result<String>
2. ProxyAiProvider (calls Worker, sends X-Device-Id, handles 429)
3. BYOKeyAiProvider (reads key from Settings, calls Gemini direct)
4. NanoAiProvider (ML Kit GenAI capability check, graceful fallback)
5. AiProviderResolver: Nano → BYO → Proxy priority
Hilt module.
```

### 5.4 — DPDP consent gate
🤖 **Claude Code:**
```
In :libs:core:
1. ConsentManager (EncryptedDataStore, checks/stores consent)
2. AiConsentScreen composable (clear language, agree/decline, privacy policy link)
3. Revoke option in Settings (clears consent + cached AI responses)
4. Wire into AiProviderResolver: blocks Proxy/BYO calls until consent granted.
   Nano calls skip consent (data stays on-device).
```

### 5.5 — Finance assistant feature
🤖 **Claude Code:**
```
Implement apps/finance/feature/assistant:
Chat UI, AiProviderResolver, system prompt for finance. Shows active provider.
ConsentManager check before first call. Error handling (network, quota, unavailable).
Enable assistant flag (requiresConsent: true). Crashlytics tag + trace.
```

### 5.6 — Privacy policy
🧑 Create a privacy policy page (GitHub Pages or raw URL):
   What's collected (Gemini queries if consented), what stays on-device,
   DPDP erasure rights, contact email, children policy.

### 5.7 — Verify + release
✅ Finance modularized, ArchUnit green. Assistant works via proxy. BYO key works.
   Consent screen blocks online AI until accepted. `strings *.apk | grep gemini` → nothing.
   Privacy policy live.

🧑 Merge PR.

📦 **Release:**
```bash
./scripts/bump-version.sh minor            # → v0.4.0
git push origin main --tags
```
✅ GitHub Release: APKs with AI assistant live.

---

## PHASE 6 — `:apps:vault` → 📦 `v0.5.0`

🧑 `git switch -c feat/apps-vault`

### 6.0 — Vault crypto spec (BEFORE any code)
🤖 **Claude Code:**
```
Write platform/adr/0003-vault-crypto-spec.md:
- Argon2id: 64MB memory, 3 iterations, 1 parallelism, 16-byte salt, 256-bit output
- Recovery key: 24-word mnemonic or 128-bit hex, shown ONCE, user confirms
- Biometric: wraps vault key with Keystore key; invalidation = re-enter password, no data loss
- SQLCipher: random passphrase encrypted with vault key (AES-GCM), stored as blob
- Export: encrypt DB file with vault key → .dhruv-vault; import = password → derive → decrypt
Commit before writing code.
```

### 6.1 — Vault app shell
🤖 **Claude Code:**
```
Create apps/vault/app/: FLAG_SECURE all windows, allowBackup=false,
com.dhruv.vault, depends on :libs:core ONLY. NO network/ai/analytics.
Crashlytics emits "vault_module_error" only. Update settings.gradle.kts.
```

### 6.2 — Master password + key derivation
🤖 **Claude Code:**
```
VaultSetupScreen (password entry, strength meter, confirm).
VaultKeyManager: generateSalt, deriveKey (Argon2id), generateRecoveryKey.
RecoveryKeyScreen (shown once, user confirms 3 random words).
Store salt + verification hash only. NEVER store vault key or password.
```

### 6.3 — Biometric convenience unlock
🤖 **Claude Code:**
```
On password success: wrap vault key with Keystore key → EncryptedDataStore.
On app open: BiometricPrompt → unwrap → open DB.
On Keystore invalidation: catch InvalidKeyException → clear wrapped blob →
prompt password → re-wrap. Message: "New fingerprint detected. Enter password."
NO DATA LOSS. Toggle in settings to disable biometric.
```

### 6.4 — SQLCipher database
🤖 **Claude Code:**
```
VaultDatabase (Room + SQLCipher): separate file, SupportFactory.
Random passphrase encrypted with vault key (AES-GCM). Vault entities
(PasswordEntry, Category, Note) do NOT implement DhruvEntity.
VaultDao, VaultRepository.
```

### 6.5 — Vault UI
🤖 **Claude Code:**
```
VaultUnlockScreen, VaultHomeScreen (categories, search, favorites),
PasswordEntryScreen (copy with 30s auto-clear, show/hide),
AddEntryScreen + PasswordGeneratorBottomSheet, VaultSettingsScreen.
All FLAG_SECURE. Glassmorphism styling.
```

### 6.6 — Export/import
🤖 **Claude Code:**
```
VaultExporter: encrypt DB → .dhruv-vault file via SAF. User-initiated only.
VaultImporter: pick file → enter password → derive → decrypt → restore DB.
Test: Device A export → Device B import → all entries present.
```

### 6.7 — Verify + release
✅ Create vault → recovery key → add entries → biometric unlock works.
   New fingerprint → "enter password" → works → NO DATA LOSS.
   Export → reinstall → import → all data. Wrong password → denied, not crash.
   ArchUnit: vault has no network/ai/analytics deps. `screencap` → black (FLAG_SECURE).

🧑 Merge PR.

📦 **Release:**
```bash
./scripts/bump-version.sh minor            # → v0.5.0
git push origin main --tags
```
✅ GitHub Release: **three APKs** — Finance + Tools + Vault.

---

## What happens after Phase 6

Each of these is an independent phase you can tackle anytime. Each produces a release:

- **Tools: weather feature** — API integration, location permission, flag flip → `v0.6.0`
- **Tools: AI assistant** — reuse Finance's AiProviderResolver + consent → `v0.7.0`
- **Dhruv ID (Firebase Auth)** — SSO, userId migration from "local" → `v1.0.0`
- **Sync (Supabase)** — offline-first sync engine, HLC conflict resolution → `v1.1.0`
- **Play Store launch** — AAB, Play App Signing, Data Safety form → when ready
- **New apps** — health, relationship → whenever

---

## Quick reference

| Working on      | Read first                                  | Branch name         |
|-----------------|---------------------------------------------|---------------------|
| Phase 1         | PLATFORM.md §2                              | feat/monorepo-restructure |
| Phase 2         | contracts/DhruvEntity.kt                    | feat/libs-core      |
| Phase 3         | DECISIONS.md ADR-0002                       | feat/libs-settings  |
| Phase 4         | PLATFORM.md §4, feature-flags/dhruv-tools   | feat/apps-tools     |
| Phase 5         | DECISIONS.md ADR-0002 + ADR-0005 + ADR-0007 | feat/finance-ai     |
| Phase 6         | adr/0003-vault-crypto-spec.md               | feat/apps-vault     |
| Release         | versions.json + scripts/bump-version.sh     | main (tag only)     |
