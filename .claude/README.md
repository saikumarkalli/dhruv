# Dhruv — Claude Automation Setup

This folder contains the Claude Code configuration for the Dhruv monorepo.
Everything here automates compliance, code quality, and agent routing so you
don't have to remember the rules on every change.

---

## What's in this folder

```
.claude/
├── README.md          ← this file
├── settings.json      ← hooks that fire automatically during Claude sessions
├── agents/            ← specialist agent definitions (read by Claude automatically)
│   ├── dhruv-arch-guardian.md
│   ├── dhruv-ci-engineer.md
│   ├── dhruv-data-engineer.md
│   ├── dhruv-debugger.md
│   ├── dhruv-feature-builder.md
│   ├── dhruv-module-auditor.md
│   ├── dhruv-orchestrator.md
│   ├── dhruv-release-manager.md
│   ├── dhruv-screen-designer.md
│   └── dhruv-test-writer.md
└── commands/          ← custom slash commands (type /name in Claude)
    ├── arch.md
    ├── audit.md
    ├── ci.md
    ├── data.md
    ├── feature.md
    ├── fix.md
    ├── release.md
    ├── screen.md
    └── test.md
```

---

## Automation layers

### 1. Session hooks (`settings.json`)

Fires automatically — no manual action needed.

| Hook | Trigger | What it does |
|------|---------|--------------|
| `SessionStart` | Every new Claude session | Injects current branch, last commit, uncommitted file count, and any in-progress feature modules into context |
| `PostToolUse Write\|Edit` | After any file is written or edited | If the file is under `apps/*/feature/**`, reminds Claude to invoke `dhruv-module-auditor` before finishing |
| `PostToolUse Bash` | After any `./gradlew *test*` command | If tests failed, automatically invokes `dhruv-debugger` with the error output |

**Note:** The `Write|Edit` hook is a lightweight PowerShell check (< 1 second, zero tokens).
The `Bash` hook spawns a full agent only when tests actually fail.

---

### 2. Custom slash commands (`commands/`)

Type these directly in any Claude session to route to the right specialist agent instantly.

| Command | Agent invoked | Use for |
|---------|--------------|---------|
| `/feature <name>` | `dhruv-feature-builder` | Scaffold a new Gradle feature module |
| `/screen <description>` | `dhruv-screen-designer` | Build or refine a Compose screen |
| `/data <description>` | `dhruv-data-engineer` | Room entity, DAO, repository, migration |
| `/audit [module path]` | `dhruv-module-auditor` | Pre-merge compliance check (read-only) |
| `/fix <error or description>` | `dhruv-debugger` | Diagnose and fix a failing build/test/crash |
| `/test <module or class>` | `dhruv-test-writer` | Write or extend unit/integration/screenshot tests |
| `/release <minor\|patch>` | `dhruv-release-manager` | Version bump and GitHub Release |
| `/ci <description>` | `dhruv-ci-engineer` | Fix or improve CI workflows and build-logic |
| `/arch <question or ADR topic>` | `dhruv-arch-guardian` | Architecture questions, boundary checks, new ADRs |

**Example usage:**
```
/feature loans
/screen add a SIP calculator screen to the investments module
/audit apps/finance/feature/loans
/fix ./gradlew build fails with unresolved reference: LoanRepository
```

---

### 3. Pre-push git hook (`.git/hooks/pre-push`)

Runs **automatically before every `git push`** on branches with feature module changes.
Uses deterministic bash — zero AI tokens, instant (< 1 second).

**What it checks:**
1. Every NavHost route wrapped in `FeatureHost`
2. Feature flag entry exists in `platform/feature-flags/<app>.json`
3. Koin `module {}` declared in the module
4. No hardcoded API keys or secrets

**Behaviour:**
- If no feature modules changed → silent, push proceeds
- If all checks pass → prints `AUDIT PASSED`, push proceeds
- If any check fails → prints violations with file paths, **blocks the push**

**To bypass in an emergency** (not recommended):
```sh
git push --no-verify
```

**To re-install on a new machine** (hooks live in `.git/` which is not committed):
```sh
cp platform/scripts/pre-push .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

> **When to use `/audit` instead:** For a thorough AI-powered review before a big PR, run
> `/audit` manually in Claude. The pre-push hook catches structural violations fast; `/audit`
> catches nuanced issues the bash checks can't.

---

### 4. CI workflow (`.github/workflows/pr-agent-review.yml`)

Runs **automatically on every PR** to `develop` or `main` that touches `apps/*/feature/**`.
No API key required — pure bash, same 4 checks as the pre-push hook.

**What it does:**
- Detects which feature modules changed in the PR
- Runs the 4 compliance checks on each module
- Posts a markdown table as a **PR comment** — never blocks the pipeline
- Reviewer reads the comment and resolves any violations before merging

**Sample comment output:**
```
## Module Compliance Audit — PR #42

| Module                        | FeatureHost | Feature Flag | Koin Module | No Secret Keys | Status   |
|-------------------------------|-------------|--------------|-------------|----------------|----------|
| apps/finance/feature/loans    | ✅          | ❌           | ✅          | ✅             | ❌ FAIL  |
|                               | missing feature flag in platform/feature-flags/finance.json |

1 passed, 1 failed. Please review and resolve the violations above before merging.
```

---

### 5. Scheduled nightly audit (cloud)

A cloud agent runs every night at **midnight IST (18:30 UTC)** against the GitHub repo.
It performs a full 8-point compliance check on all feature modules using `dhruv-module-auditor`.

**View / manage:** https://claude.ai/code/routines/trig_019UNr8EzsbUjjEgsRK7BpPz

**Checks run nightly:**
1. No `feature → feature` dependency edges
2. No `vault → network/ai/analytics` dependency
3. Every NavHost route wrapped in `FeatureHost`
4. Feature flag entry in `platform/feature-flags/<app>.json`
5. Koin `module {}` declared and registered
6. `DhruvEntity` contract satisfied for Room entities
7. No hardcoded API keys or secrets
8. Crashlytics `setCustomKey("module", ...)` present

---

## Agent roster quick reference

These agents are available in every Claude session. The slash commands above invoke them,
or Claude routes to them automatically based on your request.

| Agent | Trigger phrase examples |
|-------|------------------------|
| `dhruv-orchestrator` | "build X end to end", "plan this out", multi-step goals |
| `dhruv-feature-builder` | "add a feature module", "scaffold loans" |
| `dhruv-screen-designer` | "add a screen", "build the UI", "create composable" |
| `dhruv-data-engineer` | "add Room entity", "create a table", "add repository" |
| `dhruv-debugger` | "build is broken", "this test fails", paste a stack trace |
| `dhruv-test-writer` | "write tests", "add test coverage", "test this ViewModel" |
| `dhruv-module-auditor` | "is this ready to merge?", "audit", "compliance check" |
| `dhruv-release-manager` | "release", "bump version", "tag a release" |
| `dhruv-ci-engineer` | "fix CI", "add workflow", "update build-logic" |
| `dhruv-arch-guardian` | "is this allowed?", "propose ADR", "architecture question" |

---

## Flow for a typical feature

```
1. /feature <name>          ← scaffold module + feature flag + Koin wiring
2. /screen <description>    ← build the Compose UI
3. /data <description>      ← add Room entity + repository (if needed)
4. /test <module>           ← write unit + integration tests
5. git push                 ← pre-push hook runs 4 checks automatically
6. Open PR → develop        ← CI posts compliance comment automatically
7. /audit                   ← optional AI deep-check before merge
```
