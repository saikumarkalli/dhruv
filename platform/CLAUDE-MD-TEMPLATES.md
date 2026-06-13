# Dhruv — Module CLAUDE.md Templates

> Drop these into each module directory. Claude Code auto-loads the nearest CLAUDE.md
> when working in that subtree, giving it module-specific context without you pasting anything.

---

## libs/core/CLAUDE.md

```markdown
# :libs:core

Pure shared library. Implements platform contracts from @platform/contracts/DhruvEntity.kt.

## What lives here
- DhruvEntity interface + BaseEntity
- HlcClock (Hybrid Logical Clock for LWW sync)
- Theme tokens, glassmorphism components, DhruvTheme
- FeatureHost, FeatureErrorCard, FeatureDisabledCard
- CrashReporter interface + Crashlytics impl
- EncryptedDataStore wrapper
- Keystore helpers, SQLCipher passphrase-wrap util
- FeatureFlagResolver (remote → cached → hardcoded)
- AiProviderResolver, ConsentManager
- Play Integrity wrapper

## Rules
- Depends on NOTHING app-specific. Pure library.
- Contract changes go through platform/contracts/ PR FIRST, then implement here.
- No feature-level business logic.
```

---

## libs/settings/CLAUDE.md

```markdown
# :libs:settings

Standalone settings module. Depends on :libs:core only.

## What lives here
- SettingsRepository (EncryptedDataStore-backed)
- Settings UI: theme, color picker, font, biometric toggle, sync stub, BYO Gemini key
- All preferences encrypted at rest

## Rules
- BYO Gemini key field stores encrypted, nullable. See platform/DECISIONS.md ADR-0002.
- Biometric toggle checks BiometricManager enrollment status.
- Sync toggle is a stub (always false) until Phase: Supabase sync.
```

---

## apps/finance/CLAUDE.md

```markdown
# :apps:finance

Financial calculator + AI assistant app. Migrated from dhruv-calc.

## Modules
- app/ — shell, MainActivity, NavHost
- feature/calculator, emi, sip, loan, currency, unit, scientific, assistant

## Rules
- Each feature behind FeatureHost + flag (platform/feature-flags/dhruv-finance.json)
- Assistant requires DPDP consent before online AI calls
- AI provider priority: Nano → BYO key → Proxy (see ADR-0002)
- applicationId: com.dhruv.finance (or com.dhruv.calc if preserving Play listing)

## Build
./gradlew :apps:finance:app:assembleDebug
```

---

## apps/tools/CLAUDE.md

```markdown
# :apps:tools

Utility app: notes, clipboard, timer, QR, weather, assistant.

## Modules
- app/ — shell, MainActivity, NavHost, bottom nav
- feature/{notes, clipboard, timer, qr, weather, assistant}

## Rules
- Each feature behind FeatureHost + flag (platform/feature-flags/dhruv-tools.json)
- weather + assistant flagged OFF until implemented
- Notes: NoteEntity implements DhruvEntity (with hlc)
- Clipboard: local-only monitoring, one-time info dialog on first use

## Build
./gradlew :apps:tools:app:assembleDebug
```

---

## apps/vault/CLAUDE.md

```markdown
# :apps:vault

Password manager. E2E encrypted. Highest security requirements.

Read platform/adr/0003-vault-crypto-spec.md before ANY vault work.

## Critical rules (violations = security bugs)
- Depends on :libs:core ONLY. NO network, ai, analytics dependencies.
- Entities do NOT implement DhruvEntity. No sync, ever.
- FLAG_SECURE on ALL screens. allowBackup="false".
- Biometric is convenience unlock only — master password is the source of truth.
- Crashlytics emits ONLY "vault_module_error" — no screen names, no user context.
- SQLCipher passphrase: random → wrapped with vault key → stored encrypted.
- Never store the vault key or master password. Only salt + verification hash.

## Key model
Master password → Argon2id → vault key
Recovery key shown once at setup → allows cross-device restore
Biometric wraps vault key with Keystore key → re-enter password on invalidation

## Build
./gradlew :apps:vault:app:assembleDebug
```
