# Dhruv — Android monorepo

Read before any work:
@platform/AGENTS.md
@platform/PLATFORM.md
@platform/DECISIONS.md
@platform/DESIGN-SYSTEM.md

## Project
- Kotlin + Jetpack Compose monorepo: multiple apps sharing :libs:core and :libs:settings
- Architecture: single-activity NavHost, Koin DI, Room + EncryptedDataStore, MVVM
- minSdk 26, targetSdk latest

## Skills (read the relevant skill BEFORE doing the task)

### Project-specific (`platform/skills/`)
- New feature module → read platform/skills/dhruv-feature-scaffold/SKILL.md
- New Supabase object (table/view/function/RLS/migration) → read platform/skills/dhruv-supabase-object/SKILL.md
  — **tracker data is Supabase, not Room** (ADR-0014); use this, not dhruv-room-entity
- New Room entity / data layer → read platform/skills/dhruv-room-entity/SKILL.md
  — calculator/converter data only. NOTE: this skill still says "Hilt wiring", which is stale —
  DI is Koin (ADR-0010)
- New Compose screen → read platform/skills/dhruv-compose-screen/SKILL.md
- Pre-merge check → read platform/skills/dhruv-module-audit/SKILL.md
- Version bump / release → read platform/skills/dhruv-release/SKILL.md

### General engineering (`.claude/skills/`, from addyosmani/agent-skills)
ALWAYS check the installed skills and invoke the matching one via the Skill tool
BEFORE starting any task — do not skip a skill because the task "looks simple".
Match by the skill's own `description` (each says "Use when…"). Common triggers:
- Implementing logic / fixing a bug / changing behavior → `test-driven-development`
- Planning or breaking down work → `planning-and-task-breakdown`, `spec-driven-development`
- Reviewing a diff / quality pass → `code-review-and-quality`, `code-simplification`
- Debugging an error / failure → `debugging-and-error-recovery`
- Security / hardening work → `security-and-hardening`
- CI/CD, release, git workflow → `ci-cd-and-automation`, `git-workflow-and-versioning`, `shipping-and-launch`
- Performance, observability, UI, API design, docs/ADRs → the correspondingly-named skill
(full set lives in `.claude/skills/`; this list is not exhaustive — scan the skill list each task.)

## Build commands
Requires `JAVA_HOME` = Android Studio JBR (JDK 17+); AGP 9. Only `:apps:finance` exists today —
`:apps:tools` / `:apps:vault` are planned/future (PLATFORM.md) and are NOT yet in settings.gradle.kts.
- Build app:     `./gradlew :apps:finance:app:assembleDebug`
- Build module:  `./gradlew :apps:finance:feature:<name>:assembleDebug`
- Lint:          `./gradlew detekt`
- Unit tests:    `./gradlew :apps:finance:app:testDebugUnitTest` (includes ArchUnit)
- Single test:   `./gradlew :apps:finance:feature:<name>:testDebugUnitTest --tests "com.dhruv.finance.<name>.SomeTest"`
- Pre-merge gate:`./gradlew regressionCheck` (all unit tests + ArchUnit + merged JaCoCo report + coverage floor) — this is what CI runs

## Hard rules
- Do not redesign architecture. Decisions locked in DECISIONS.md. Propose an ADR instead.
- Module boundaries enforced by ArchUnit (feature→feature FORBIDDEN, vault→network/ai/analytics FORBIDDEN).
- Every feature route wrapped in FeatureHost — never a blank crash.
- No secrets or API keys in the repo or APK. GitLeaks gates CI.
- Kotlin only, Compose only, Koin only (DI), Coroutines+Flow only. (Hilt is NOT used — its Gradle plugin is incompatible with AGP 9; see ADR-0010.)
- DPDP: consent screen before any data leaves the device.