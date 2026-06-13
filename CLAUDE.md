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