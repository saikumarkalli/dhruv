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
- New feature module → read platform/skills/dhruv-feature-scaffold/SKILL.md
- New Room entity / data layer → read platform/skills/dhruv-room-entity/SKILL.md
- New Compose screen → read platform/skills/dhruv-compose-screen/SKILL.md
- Pre-merge check → read platform/skills/dhruv-module-audit/SKILL.md
- Version bump / release → read platform/skills/dhruv-release/SKILL.md

## Subagents (delegate the task; each agent reads its backing skill + the platform docs first)
- New feature module → `dhruv-feature-builder` (wraps dhruv-feature-scaffold)
- Room entity / data layer → `dhruv-data-engineer` (wraps dhruv-room-entity)
- Compose screen / UI → `dhruv-screen-designer` (wraps dhruv-compose-screen; Koin-corrected)
- Pre-merge compliance check (read-only) → `dhruv-module-auditor` (wraps dhruv-module-audit)
- Version bump / release → `dhruv-release-manager` (wraps dhruv-release)
- Architecture/boundary question or ADR → `dhruv-arch-guardian`
- CI/CD, build-logic, quality gates → `dhruv-ci-engineer`
- Multi-step / vague goal — plan & route → `dhruv-orchestrator` (read-only planner; emits a task graph the main thread executes — subagents can't spawn subagents)
- Write/extend tests (unit, ViewModel, DAO, ArchUnit, screenshot) → `dhruv-test-writer`
- Broken build / failing test / crash / regression → `dhruv-debugger`

### Role mapping (generic agent roles → Dhruv agents)
- **Planner/Orchestrator** → `dhruv-orchestrator`.
- **Code Generator** → consolidated into `dhruv-feature-builder` + `dhruv-screen-designer` + `dhruv-data-engineer` (no standalone generator — code-gen is module/screen/data-scoped here).
- **Test Writer** → `dhruv-test-writer`.
- **Debugger** → `dhruv-debugger`.
- **DevOps/Deploy** → consolidated into `dhruv-ci-engineer` (pipelines/gates) + `dhruv-release-manager` (version/tag/Release).

### Handoff contract (all agents)
Shared context every agent grounds on: `{ codebaseLanguage: Kotlin, stack: Compose/Koin/Room, projectRoot, app, module, branch, activeTask }`. Standard interface: **input** = task + module path; **execute** = bootstrap from `platform/*` docs then do the work; **output** = artifacts/edits + a report; **onError** = escalate to `dhruv-arch-guardian` (boundary), `dhruv-ci-engineer` (build/CI), or `dhruv-debugger` (failure). Every merge-bound flow ends at `dhruv-module-auditor`.

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