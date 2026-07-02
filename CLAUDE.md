# Dhruv — Android monorepo

Read before any work:
@platform/AGENTS.md
@platform/PLATFORM.md
@platform/DECISIONS.md

## Project
- Kotlin + Jetpack Compose monorepo: multiple apps sharing :libs:core and :libs:settings
- Architecture: single-activity NavHost, Koin DI, Room + EncryptedDataStore, MVVM
- minSdk 26, targetSdk latest

## Skills (read the relevant skill BEFORE doing the task)

### Project-specific (`platform/skills/`)
- New feature module → read platform/skills/dhruv-feature-scaffold/SKILL.md
- New Room entity / data layer → read platform/skills/dhruv-room-entity/SKILL.md
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
- ./gradlew :apps:finance:app:assembleDebug
- ./gradlew :apps:tools:app:assembleDebug
- ./gradlew detekt
- ./gradlew test

## Hard rules
- Do not redesign architecture. Decisions locked in DECISIONS.md. Propose an ADR instead.
- Module boundaries enforced by ArchUnit (feature→feature FORBIDDEN, vault→network/ai/analytics FORBIDDEN).
- Every feature route wrapped in FeatureHost — never a blank crash.
- No secrets or API keys in the repo or APK. GitLeaks gates CI.
- Kotlin only, Compose only, Koin only (DI), Coroutines+Flow only. (Hilt is NOT used — its Gradle plugin is incompatible with AGP 9; see ADR-0010.)
- DPDP: consent screen before any data leaves the device.