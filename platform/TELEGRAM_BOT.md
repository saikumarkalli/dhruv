# Telegram Bot CI/CD — Implementation Document

**Branch this was designed on:** `feat/ai-agent-setup`  
**Status:** Designed and partially implemented — 3 files created, setup not yet done  
**Purpose:** Control the Dhruv build pipeline from Telegram. Send a message → system builds, tests, fixes, or ships the app.

---

## Part 1: What's Already on This Branch

### 1.1 Branch Summary (`feat/ai-agent-setup`)

This branch added the full Claude Code automation layer on top of the existing Android monorepo. Here's what landed:

#### CI/CD Workflows (`.github/workflows/`)
| File | Purpose |
|------|---------|
| `ci.yml` | 4-gate pipeline: Security → Tests → Build → Release. Auto-bumps patch version and publishes signed APK on every merge to `develop`. |
| `fast-feedback.yml` | Lightweight pre-PR feedback on `feat/**`, `fix/**`, `chore/**` branches. Runs compile + unit tests only. |
| `pr-agent-review.yml` | Posts a compliance table on every PR: checks FeatureHost wrapping, feature flags, Koin module, no hardcoded secrets. |
| `release.yml` | Manual re-publish tool — rebuilds and re-attaches APK to an existing GitHub Release tag. |

#### Claude Code Agents (`.claude/agents/`)
10 specialist agents, each with a focused role:

| Agent | Role |
|-------|------|
| `dhruv-orchestrator` | Decomposes multi-step goals into a task graph — read-only planner |
| `dhruv-feature-builder` | Scaffolds complete Gradle feature modules (Koin, flags, FeatureHost) |
| `dhruv-screen-designer` | Builds Jetpack Compose screens with Koin-corrected DI |
| `dhruv-data-engineer` | Room entities, DAOs, repositories, migrations |
| `dhruv-test-writer` | Unit, integration, ArchUnit, screenshot tests |
| `dhruv-debugger` | Root-cause diagnosis for failing builds/tests/crashes |
| `dhruv-module-auditor` | Read-only pre-merge compliance check |
| `dhruv-release-manager` | Version bumps, tags, GitHub Release publication |
| `dhruv-ci-engineer` | GitHub Actions workflows, build-logic, quality gates |
| `dhruv-arch-guardian` | Architecture boundaries, new ADRs |

#### Slash Commands (`.claude/commands/`)
9 commands that route to the specialist agents above:
`/feature`, `/screen`, `/data`, `/audit`, `/fix`, `/test`, `/release`, `/ci`, `/arch`

#### Session Hooks (`.claude/settings.json`)
3 automated hooks fire during every Claude Code session:
- **SessionStart** — injects branch name, last commit, dirty file count, changed feature modules
- **PostToolUse (Write/Edit)** — reminds Claude to invoke auditor after editing feature modules
- **PostToolUse (Bash test)** — auto-invokes `dhruv-debugger` if a test run fails

#### Finance App Modular Architecture (`apps/finance/`)
The monolith was split into feature modules:

| Module | Gradle path |
|--------|-------------|
| App shell | `:apps:finance:app` |
| Shared data | `:apps:finance:data` |
| Calculator | `:apps:finance:feature:calculator` |
| Loans (EMI) | `:apps:finance:feature:loans` |
| Investments (SIP/FD) | `:apps:finance:feature:investments` |
| Tax (GST/salary) | `:apps:finance:feature:tax` |
| Everyday (interest/discount/tip) | `:apps:finance:feature:everyday` |
| Currency converter | `:apps:finance:feature:currency` |
| Unit converter | `:apps:finance:feature:unit` |
| Date calculator | `:apps:finance:feature:date` |
| Time | `:apps:finance:feature:time` |
| AI Assistant | `:apps:finance:feature:assistant` |

#### Core Libraries
- `:libs:core` — DhruvEntity, FeatureHost, theme, security utils, FeatureFlagResolver (supports `minVersion` + `requiresConsent`)
- `:libs:settings` — EncryptedDataStore, Settings UI components

#### Platform Documentation (`platform/`)
- `PLATFORM.md` — architecture source of truth
- `DECISIONS.md` — ADR register (ADR-0001 through ADR-0011)
- `AGENTS.md` — agent rules and session bootstrap
- `versions.json` — current version: `finance 1.2.0`
- `feature-flags/dhruv-finance.json` — feature flag config

---

## Part 2: The Telegram Bot System

### 2.1 What It Does

A 3-layer system that bridges Telegram to the CI pipeline:

```
YOU (Telegram phone)
      │
      │  /build  /test  /fix  /enhance
      │  /release  /ship  /rollback  /audit  /status
      ▼
Cloudflare Worker          ← receives & validates Telegram messages
      │                    ← also receives GitHub events (CI pass/fail, releases)
      │  POST workflow_dispatch
      ▼
GitHub Actions             ← executes the requested operation
      │
      ├── /build    → assembleDebug APK
      ├── /test     → testDebugUnitTest
      ├── /fix      → creates GitHub Issue + sends Claude Code prompt
      ├── /enhance  → creates GitHub Issue + sends Claude Code prompt
      ├── /release  → bumps version in versions.json, opens PR
      ├── /ship     → test gate + build gate (quality check only)
      ├── /rollback → git revert, opens PR
      ├── /audit    → 4 compliance checks
      └── /status   → last 3 CI runs + current version
      │
      ▼ result (Telegram Bot API sendMessage)
YOU (Telegram phone)
```

**Also automatic (no command needed):**
```
GitHub Events → Worker → Telegram
  CI fails on develop      → ❌ CI failed. Gate: Tests. [Logs]
  GitHub Release published → 🚀 v1.2.4 released. [Download APK]
  Bot-opened PR ready      → 🤖 PR #42 ready for your review.
  CI gate fails on any PR  → ❌ Gate 2 failed on PR #42
```

### 2.2 Key Design Decisions (from brainstorming session)

#### Why Cloudflare Worker?
Already in the project architecture (ADR-0002 uses it for the Gemini API proxy). Free tier handles 100K requests/day. No new accounts needed.

#### Why `workflow_dispatch` instead of direct CI triggers?
The Worker calls GitHub's REST API to trigger the workflow. This means:
- All secrets stay in GitHub — the Worker only needs an Actions:write PAT
- The workflow runs in the full CI environment (JDK 21, Gradle cache, signing keys)
- Existing quality gates run naturally on PRs opened by the bot

#### Why /fix and /enhance create Issues instead of running AI in CI?
**Original plan:** Run Claude CLI headless in the GitHub Actions runner (costs ~$0.06/run on Anthropic API).  
**Final decision:** Create a GitHub Issue and send you the prompt to run locally in Claude Code.  
**Reason:** You already pay for Claude Code (subscription). Running it in CI would add a separate Anthropic API bill. By creating an Issue and notifying you, you fix it locally with zero extra cost. The fix quality is also better because you're in the loop.

#### Why /release opens a PR instead of pushing directly?
`ci.yml` auto-bumps patch on every merge (ADR-0011). If the bot pushed directly to `develop`, it would trigger another patch bump — double bump. The safe model: bot bumps major/minor in a PR → you merge → ci.yml does the final patch increment + builds signed APK + publishes GitHub Release.  
**Patch releases are automatic** — every merge to develop triggers one. No Telegram command needed.

#### Why /rollback uses `git revert` not `git reset`?
`git revert` creates new commits that undo previous ones — it doesn't rewrite history. This means:
- Branch protection is respected (no force-push)
- The rollback is auditable (new commit with clear message)
- You review the PR before anything merges

#### Why /ship is test + build only (not a release)?
`/ship` is a quality gate check: "is the code safe to release right now?" If both gates are green, you then decide when to send `/release minor` or `/release major`. This separates the "is it ready?" question from the "ship it now" action.

#### No Anthropic API key needed
7 out of 9 commands are entirely free (Gradle, git, gh CLI). Only `/fix` and `/enhance` use AI — and those stay local, using your existing Claude Code subscription.

### 2.3 The 9 Commands

| Command | What happens | Output |
|---------|--------------|--------|
| `/build [app]` | `./gradlew :apps:<app>:app:assembleDebug` | ✅/❌ + artifact link (3 days) |
| `/test [module]` | `./gradlew testDebugUnitTest --continue` | ✅/❌ + report link |
| `/fix <description>` | Creates GitHub Issue with acceptance criteria + Claude Code prompt | Issue link |
| `/enhance <description>` | Creates GitHub Issue with DoD checklist + Claude Code prompt | Issue link |
| `/release major\|minor` | Bumps `versions.json`, opens PR to develop | PR link |
| `/ship` | Test gate → build gate (sequenced) | ✅ Green or ❌ which gate failed |
| `/rollback [v1.2.3]` | git revert last commit (or range to tag), opens PR | PR link |
| `/audit [module]` | 4 checks: FeatureHost / flags / Koin / secrets | Compliance table |
| `/status` | Last 3 `ci.yml` runs + current version | ✅/❌ summary |

---

## Part 3: Files Created in This Session

Three new files were added to the branch. No existing files were modified.

### `cloudflare/wrangler.toml`
Cloudflare Worker deployment config. Sets the Worker name and points at the JS file. All secrets are configured separately via `wrangler secret put` — nothing sensitive in this file.

### `cloudflare/telegram-webhook.js`
The Cloudflare Worker (145 lines). Handles two URL paths:
- `/telegram?secret=<WEBHOOK_SECRET>` — receives Telegram messages, validates, dispatches to GitHub Actions
- `/github?secret=<GITHUB_WEBHOOK_SECRET>` — receives GitHub webhook events, forwards notifications to Telegram

Key logic:
- Chat ID allowlist check (before any GitHub API call)
- Command parser (strips @botname from group mentions)
- Validation: `/fix` and `/enhance` require a description; `/release` requires `major` or `minor`
- Immediate acknowledgment sent before GitHub dispatch (Worker must respond in < 5s)

### `.github/workflows/telegram-dispatch.yml`
The GitHub Actions workflow (330 lines). One job per command, each guarded by `if: github.event.inputs.command == '<cmd>'`. Triggered only via `workflow_dispatch` (not on push/PR). Uses `env` block to make `TG_TOKEN`, `TG_CHAT`, and `RUN_URL` available to all steps without repetition. Every job has both a success and a failure Telegram notification step.

---

## Part 4: What Still Needs to Be Done (Future Implementation)

### Step 1 — Create Telegram Bot (5 min)
```
Telegram → @BotFather → /newbot
  Name:     Dhruv CI
  Username: dhruvci_bot   (must end in "bot")
  → Copy token: 7123456789:AAFxxxxxx

Telegram → @userinfobot → Start
  → Copy your user ID: 123456789
```

### Step 2 — Create GitHub Fine-Grained PAT (3 min)
```
github.com → Settings → Developer settings
→ Personal access tokens → Fine-grained tokens → Generate new token

Token name:        dhruv-telegram-bot
Expiration:        1 year (add calendar reminder to rotate)
Repository access: Only → dhruv
Permissions:       Actions → Read and write  ← only this, nothing else

→ Generate → copy token (shown once only)
```

### Step 3 — Generate Webhook Secrets (1 min)
Run twice in PowerShell, save both outputs:
```powershell
[System.Convert]::ToBase64String(
  [System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
# Output 1 = WEBHOOK_SECRET         (Telegram → Worker validation)
# Output 2 = GITHUB_WEBHOOK_SECRET  (GitHub → Worker validation)
```

### Step 4 — Add GitHub Actions Secret (2 min)
```
github.com/<you>/dhruv → Settings → Secrets and variables → Actions
→ New repository secret

Name:  TELEGRAM_BOT_TOKEN
Value: 7123456789:AAFxxxxxx
```
That's the only GitHub secret needed (no Anthropic API key).

### Step 5 — Deploy Cloudflare Worker (10 min)
```powershell
npm install -g wrangler
wrangler login              # opens browser → log into Cloudflare

cd d:\Work\code-base\dhruv\cloudflare
wrangler deploy
# Output: "Published dhruv-telegram-bot (https://dhruv-telegram-bot.<account>.workers.dev)"
```

Then set all 6 Worker secrets (each prompts you to paste the value):
```powershell
wrangler secret put TELEGRAM_BOT_TOKEN     # from Step 1
wrangler secret put GITHUB_TOKEN           # PAT from Step 2
wrangler secret put WEBHOOK_SECRET         # first random string from Step 3
wrangler secret put GITHUB_WEBHOOK_SECRET  # second random string from Step 3
wrangler secret put ALLOWED_CHAT_IDS       # your Telegram user ID from Step 1
wrangler secret put NOTIFY_CHAT_ID         # same user ID
```

### Step 6 — Register Telegram Webhook (1 min)
```powershell
$TOKEN  = "7123456789:AAFxxxxxx"
$WORKER = "https://dhruv-telegram-bot.<account>.workers.dev"
$SECRET = "<WEBHOOK_SECRET from Step 3>"

Invoke-WebRequest `
  -Uri "https://api.telegram.org/bot${TOKEN}/setWebhook?url=${WORKER}/telegram?secret=${SECRET}"
# Expected: {"ok":true,"description":"Webhook was set"}
```

### Step 7 — Register GitHub Webhook (2 min)
```
github.com/<you>/dhruv → Settings → Webhooks → Add webhook

Payload URL:  https://dhruv-telegram-bot.<account>.workers.dev/github?secret=<GITHUB_WEBHOOK_SECRET>
Content type: application/json
Secret:       (leave blank — auth is in the URL query param)

Select individual events:
  ✅ Workflow runs
  ✅ Releases
  ✅ Pull requests
  ✅ Check runs

→ Add webhook
```

### Step 8 — Push the workflow to develop
The workflow file is already in the repo on `feat/ai-agent-setup`. When this branch is merged to `develop`, the workflow becomes active. Alternatively, cherry-pick just the 3 new files onto a separate branch and open a PR:
```powershell
git checkout develop
git checkout -b feat/telegram-bot
git checkout feat/ai-agent-setup -- cloudflare/ .github/workflows/telegram-dispatch.yml
git add -A
git commit -m "feat: add Telegram bot CI/CD integration"
git push origin feat/telegram-bot
# open PR to develop
```

---

## Part 5: Verification Sequence

Test in this order after setup — each test confirms the previous layer works:

| # | Send to bot | Expected | Time |
|---|-------------|----------|------|
| 1 | `/status` | Last 3 CI runs + `finance v1.2.0` | ~2 min |
| 2 | `/build finance` | ✅ Build passed + artifact link | ~5 min |
| 3 | `/test` | ✅ All tests passed | ~4 min |
| 4 | `/audit` | Compliance table for all feature modules | ~3 min |
| 5 | `/fix the status bar overlaps the header` | 🐛 Issue #N created + Claude Code prompt | ~1 min |
| 6 | `/ship` | ✅ Gates green or ❌ which gate failed | ~8 min |
| 7 | `/release minor` | 🏷️ PR ready, review and merge to ship | ~2 min |
| 8 | Force a CI failure on develop | Automatic: ❌ notification (no command) | instant |
| 9 | Merge any PR to develop | Automatic: 🚀 APK download link after release | ~10 min |

---

## Part 6: Cost Summary

| Resource | Free tier | Expected usage | Monthly cost |
|----------|-----------|----------------|-------------|
| Cloudflare Worker | 100K requests/day | < 100 requests/day | $0 |
| GitHub Actions | 2,000 min/month (private repo) | ~500 min/month | $0 |
| Telegram Bot API | Unlimited | — | $0 |
| Anthropic API | Pay-per-use | **$0 — AI stays local** | $0 |
| **Total** | | | **$0/month** |

---

## Part 7: Security Model

| Layer | What it protects against |
|-------|--------------------------|
| `WEBHOOK_SECRET` in URL | Prevents spoofed Telegram requests to the Worker |
| `ALLOWED_CHAT_IDS` | Only your Telegram user ID can trigger any command |
| `GITHUB_WEBHOOK_SECRET` in URL | Prevents spoofed GitHub events to the Worker |
| Fine-grained PAT (Actions:write only) | Worker can only trigger workflows — can't read code, delete branches, modify settings |
| PRs instead of direct pushes | `/fix`, `/enhance`, `/release`, `/rollback` always open PRs. Branch protection prevents the bot from merging its own PRs. You always review before anything lands on develop |
| ci.yml Gate 2 (GitLeaks) | Catches any accidentally committed secrets in bot-opened PRs |
| pr-agent-review.yml | Runs compliance audit automatically on every bot-opened PR |

---

## Part 8: Interaction with Existing CI (Important)

The Telegram bot is additive — it does not modify any existing workflow.

| Existing workflow | How the bot interacts with it |
|-------------------|------------------------------|
| `ci.yml` | Triggers automatically when bot-opened PRs are merged to develop |
| `pr-agent-review.yml` | Fires automatically on every PR opened by `/fix`, `/enhance`, `/release`, `/rollback` |
| `fast-feedback.yml` | Fires on `fix/telegram-*` and `feat/telegram-*` branches when pushed |
| `release.yml` | Not used by the bot — remains a manual re-publish tool |

**The release flow (end-to-end for `/release minor`):**
```
1. You send: /release minor
2. Bot opens PR: release/telegram-minor-12345 → develop
   (bumps versions.json: 1.2.x → 1.3.0)
3. fast-feedback.yml runs on the bump branch (lightweight)
4. You review the PR, merge it
5. ci.yml triggers on the merge:
   Gate 1 (static analysis) → Gate 2 (security) → Gate 3 (tests) → Gate 4 (build)
   → Release job: bumps patch again (1.3.0 → 1.3.1), builds signed APK,
     creates tag dhruv-finance-v1.3.1, publishes GitHub Release
6. GitHub webhook fires → Worker → Telegram:
   "🚀 Released dhruv-finance-v1.3.1 — [Download APK]"
```

---

## Part 9: Files Reference

```
dhruv/
├── cloudflare/
│   ├── telegram-webhook.js      ← NEW in this session
│   └── wrangler.toml            ← NEW in this session
├── .github/
│   └── workflows/
│       ├── ci.yml               ← existing (auto-bump patch on merge)
│       ├── fast-feedback.yml    ← existing (pre-PR feedback)
│       ├── pr-agent-review.yml  ← existing (compliance audit on PRs)
│       ├── release.yml          ← existing (manual re-publish)
│       └── telegram-dispatch.yml  ← NEW in this session
├── .claude/
│   ├── agents/                  ← 10 specialist agents (on this branch)
│   ├── commands/                ← 9 slash commands (on this branch)
│   └── settings.json            ← 3 hooks (on this branch)
└── platform/
    ├── versions.json            ← current: finance 1.2.0
    ├── DECISIONS.md             ← ADR-0001 through ADR-0011
    └── TELEGRAM_BOT.md          ← THIS FILE
```
