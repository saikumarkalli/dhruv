# Dhruv — Android monorepo

Read before any work:
@platform/AGENTS.md
@platform/PLATFORM.md
@platform/DECISIONS.md

## Project
- Kotlin + Jetpack Compose monorepo: multiple apps sharing :libs:core and :libs:settings
- Architecture: single-activity NavHost, Hilt DI, Room + EncryptedDataStore, MVVM
- minSdk 26, targetSdk latest

## Build commands
- ./gradlew :apps:finance:app:assembleDebug     # finance app
- ./gradlew :apps:tools:app:assembleDebug       # tools app (after Phase 4)
- ./gradlew detekt                               # static analysis
- ./gradlew test                                 # unit tests
- ./gradlew connectedAndroidTest                 # instrumented tests

## Hard rules
- Do not redesign architecture. Decisions are locked in platform/DECISIONS.md.
- Module boundaries enforced by ArchUnit: feature→feature FORBIDDEN, vault→network/ai/analytics FORBIDDEN.
- Every feature route wrapped in FeatureHost — never a blank crash.
- No secrets or API keys in the repo or APK. GitLeaks gates CI.
- DPDP: consent screen before any data leaves the device.

## Conventions
- Kotlin, no Java
- Compose for all UI — no XML layouts
- Hilt for DI — no manual injection
- Repository pattern for data access from features
- Coroutines + Flow, no RxJava
- Package by feature, not by layer