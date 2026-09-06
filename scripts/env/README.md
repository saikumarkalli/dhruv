# Dev/Prod environment setup — runbook

Companion to `platform/DECISIONS.md` ADR-0032. Everything file-based (workflows, schema
restructure, docs) is already committed. What's left is credentialed/interactive setup that can't
be done from an unauthenticated session — this is that checklist, in order.

## Already done (2026-08-16, this session — `gh` was authenticated, so this ran live)

- [x] `gh`, `supabase`, `vercel` CLIs installed (`npm install -g supabase vercel`; `gh` was already
      present via winget, just not on this shell's PATH — now added to the persistent User PATH).
- [x] GitHub Environments `dev` and `prod` created (no protection rule on either — see below for
      why `prod`'s approval gate is a workflow job instead).
- [x] Repo variables `SUPABASE_DEV_PROJECT_REF=dsfnrtckgpnvyvscevxn`,
      `SUPABASE_PROD_PROJECT_REF=ikyzgxnktlccjvjedgal` set (used by the release job's dev-ref
      guard, ADR-0032 decision 7).

## Known constraint: no native required-reviewer gate on this billing tier

Confirmed live: creating `prod` with a `reviewers` protection rule 422s —
*"Please ensure the billing plan supports the required reviewers protection rule."* Classic branch
protection 403s the same way. This repo is private on GitHub Free; both are Pro/Team-gated for
private repos. **Substitute already built and committed:** `release-approval` (`ci.yml`) and
`prod-approval` (`supabase-migrate.yml`) use `trstringer/manual-approval@v1` — pushing to `main`
opens a GitHub issue, the job pauses until you comment `approved` (or `approve`/`lgtm`/`yes`) on
it, then proceeds. No setup needed for this — it works out of the box with the existing
`GITHUB_TOKEN`. Upgrading to GitHub Pro later (small monthly cost) would unlock the native
mechanisms and also fix branch protection, which — separately — was likely never actually enforced
either (same tier gate). Not required for anything below to work.

## Branch protection (`main` / `develop` are PR-only)

Same billing wall, re-confirmed live on 2026-08-17 — **both** GitHub mechanisms refuse:

```
GET /repos/saikumarkalli/dhruv/rulesets
  403 "Upgrade to GitHub Pro or make this repository public to enable this feature."
PUT /repos/saikumarkalli/dhruv/branches/main/protection
  403 (Pro required)
```

Auto-merge (`allow_auto_merge`) silently stays `false` for the same reason. So the rule is enforced
in two unpaid halves, both committed:

| Layer | File | What it does |
|---|---|---|
| Preventive | `scripts/hooks/pre-push` | Refuses a push to `main`/`develop` on your machine, before it reaches GitHub. Prints the branch-and-PR commands to use instead. |
| Detective | `.github/workflows/branch-guard.yml` | On any push to `main`/`develop`, checks every pushed commit resolves to a merged PR. If not: red X on the commit, failing run, job summary naming the offending commits. Also flags force-pushes. |

**The hook is not optional** — it is the only thing that actually *stops* a direct push:

```powershell
git config core.hooksPath scripts/hooks
```

Emergency override (still reported by `branch-guard`):
`DHRUV_ALLOW_PROTECTED_PUSH=1 git push ...`

### When you upgrade to GitHub Pro

```powershell
pwsh ./scripts/env/apply-branch-protection.ps1 -WhatIf   # preview the rulesets
pwsh ./scripts/env/apply-branch-protection.ps1           # apply
```

Idempotent. Creates/updates a `protect-main` and `protect-develop` ruleset: PR required, no
force-push, no deletion, required checks = the four CI gate job names, and
`strict_required_status_checks_policy: true` — which is the *"require branches to be up to date"*
setting ADR-0026 calls load-bearing and which has never actually been enforced on this tier.
Approval count is deliberately **0** (a solo maintainer cannot approve their own PR; the PR itself
is the gate). Afterwards, delete `branch-guard.yml` — GitHub enforces it natively at that point.

## Repo hardening applied 2026-08-17 (all free-tier)

| # | Setting | State | How to re-verify |
|---|---|---|---|
| 1 | Dependency graph + **Dependabot alerts** + automated security fixes | **on** — was off; 308 packages now graphed, **2 open high alerts** in `web/package-lock.json` (react-router CSRF, brace-expansion DoS). Security-update PRs open automatically. | `gh api repos/saikumarkalli/dhruv/dependabot/alerts?state=open --jq length` |
| 2 | Third-party actions **SHA-pinned** | 8 call sites: `gitleaks-action`, `action-gh-release` ×2, `setup-cli` ×3, `manual-approval` ×2 | `grep -rn "uses: \(gitleaks\|softprops\|supabase/setup-cli\|trstringer\)" .github/workflows/` |
| 3 | Actions barred from approving PRs → **on**. Default `GITHUB_TOKEN` permissions → **`write`** (tried `read`, reverted — see below) | `can_approve_pull_request_reviews: false` kept; the `read` default was reverted the same day after it broke both open Dependabot PRs | `gh api repos/saikumarkalli/dhruv/actions/permissions/workflow` |
| 4 | Milestones **Phase 0–7** | 8 created, all open | `gh api repos/saikumarkalli/dhruv/milestones --jq '.[].title'` |
| 5 | `.github/release.yml` | auto-categorised release notes (honoured because the release step sets `generate_release_notes: true`) | — |
| 6 | Homepage → `https://dhruv-finance.vercel.app` | verified HTTP 200 | `gh api repos/saikumarkalli/dhruv --jq .homepage` |
| 7 | Description + 20 topics + `delete_branch_on_merge` | applied 2026-08-17 | `gh repo view --json description,repositoryTopics` |

### Do NOT set the default workflow permissions to `read` (2026-08-17)

It was set to `read` as a least-privilege measure and **broke `Gate 2 · Security` on every open
Dependabot PR** (#36, #37) within minutes:

```
RequestError [HttpError]: Resource not accessible by integration
    at async Object.ScanPullRequest (.../gitleaks-action/v2/dist/index.js)
  status: 403
Secret source: Dependabot
```

**Why.** For a **Dependabot-triggered** event the repository's *default* workflow permissions act as
a hard **cap** on `GITHUB_TOKEN` — a job-level `permissions:` block cannot exceed it the way it can
on a normal PR. `ci.yml`'s `security` job asks for `pull-requests: write`; with the default at
`read` that request is capped away and `gitleaks-action`'s PR-scan path 403s. Normal PRs are
unaffected, which is why only the two Dependabot PRs failed.

**Confirmed by single-variable test**, not inference: same commit, same action — flipping the repo
default `read → write` and re-running the identical job took `Gate 2` from `fail (9s)` to
`pass (15s)` on both PRs.

**Why reverting costs nothing.** Every job in every workflow already declares its own
`permissions:` block — verified across all eight workflows, **zero** jobs inherit the repo default.
The setting is therefore dead config for normal runs, and its only observable effect was capping the
Dependabot token. It bought no real privilege reduction and broke a blocking gate.

**A fix cannot be validated before merge.** Dependabot PRs run the workflow from their own branch
(based on `develop`), so a workflow change sitting in a feature-branch PR has no effect on them. Any
future attempt at this has to land on `develop` first and be verified afterwards, with the open
Dependabot PRs as the canary — which is why it was not retried blind.

`can_approve_pull_request_reviews: false` is unrelated, kept, and caused none of this.

### Action-pinning policy

**Third-party actions are pinned to a full commit SHA with the tag as a trailing comment.** A
mutable tag can be repointed by its owner at any commit; this repo's `release` job holds the
**signing keystore**, so a repointed tag is a path to a signed APK built from someone else's code.
`actions/*` stay on major tags — GitHub-owned, and their release tags are the org's own protected
pointers.

Dependabot's `github-actions` ecosystem (already configured, monthly) reads the trailing `# v2`
comment and bumps the SHA for you, so pinning does not mean going stale.

When adding a new third-party action:

```sh
gh api repos/<owner>/<action>/commits/<tag> --jq .sha
# uses: <owner>/<action>@<sha> # <tag>
```

### Project board (one interactive step, then scripted)

Projects v2 is account-level, so it is free even on a private repo — but it needs a token scope
the default login does not grant:

```powershell
gh auth refresh -s project,read:project    # opens a browser, once
pwsh ./scripts/env/setup-project-board.ps1
```

### Still blocked by GitHub Free

Rulesets / branch protection / Environment required reviewers / auto-merge — see the
[Branch protection](#branch-protection-main--develop-are-pr-only) section above. Code scanning
(CodeQL) and GitHub secret scanning on a private repo are **GitHub Advanced Security**, not Pro —
a separate enterprise product. GitLeaks (`ci.yml` Gate 2) and detekt already cover that ground; do
not budget for it.

## Remaining steps

### 1. Supabase — set environment secrets

**Confirmed still not done, 2026-08-31.** `apply-dev` (`supabase-migrate.yml`, triggered by the
2026-08-27 `develop` push, run `33080504883` / job `98546096527`) failed:

```
supabase link --project-ref ""
Cannot find project ref. Have you run supabase link?
```

`gh api repos/saikumarkalli/dhruv/environments/dev/secrets` returns `{"total_count":0,"secrets":[]}`
— the `dev` Environment has **zero** secrets, confirming `SUPABASE_ACCESS_TOKEN`,
`SUPABASE_PROJECT_REF`, `SUPABASE_DB_PASSWORD` were never actually set there despite this section
already existing. The same push also tripped `branch-guard.yml` (see the note below) — unrelated
cause, same commit. Re-run `apply-dev` after completing this step to confirm it's clean; it will
keep failing on every `develop` push carrying a `supabase/**` change until then.

**Re-confirmed 2026-09-03, still 0 secrets on both `dev` and `prod` Environments** — but
`supabase migration list --linked` against `dhruv-dev` (run with a one-time personal access token,
manually, outside CI) shows all three local migration files already applied remotely
(`local == remote` for `0001`, `20260816211500`, `20260823094500`). **This means CI's `apply-dev`
run history is not a reliable signal of actual DB state** — the migrations reached `dhruv-dev`
through some out-of-band manual `db push` from an earlier session, not through this pipeline, and
`apply-dev` has in fact never once succeeded end-to-end. Don't infer "not applied" from a red
`apply-dev` run without independently checking `supabase migration list --linked` — and don't infer
"this pipeline works" from the DB happening to be in the right state either. Completing this step
(env secrets) is what makes future migrations reach `dhruv-dev`/`dhruv-prod` through the actual
gated pipeline instead of by hand.

Run **yourself**, in your own terminal (it prompts for real credentials — never paste secret
values into a chat session, including this one):

```powershell
./scripts/env/set-env-secrets.ps1
```

Prompts for each project's URL, anon key, project ref, and DB password, then sets:
- `SUPABASE_URL` / `SUPABASE_ANON_KEY` / `SUPABASE_PROJECT_REF` / `SUPABASE_DB_PASSWORD` — scoped
  to the `dev` and `prod` GitHub Environments respectively (same secret names, different
  environment — that's what makes `environment: dev|prod` on a job select the right project).
- `SUPABASE_ACCESS_TOKEN` — same value in both environments (one Supabase account owns both
  projects).
- `SUPABASE_DEV_URL` / `SUPABASE_DEV_ANON_KEY` / `SUPABASE_PROD_URL` / `SUPABASE_PROD_ANON_KEY` —
  plain repo secrets, consumed only by `supabase-keepalive.yml` (deliberately not
  environment-scoped — see that workflow's header comment).
- Offers to delete the old flat `SUPABASE_URL`/`SUPABASE_ANON_KEY` repo secrets (set before the
  dev/prod split existed) so there's no silent fallback to a value that's ambiguous about which
  project it belongs to.

**Not touched by this script, and don't need to be:** `KEYSTORE_BASE64`, `STORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`, `GEMINI_API_KEY`, `GOOGLE_WEB_CLIENT_ID` already exist as repo-level
secrets and stay that way — there is exactly one signing identity and one cross-app AI/OAuth
identity (ADR-0031), not a dev/prod pair, so the `release`/`prod-apply` jobs' `environment: prod`
correctly falls back to these repo-level values (GitHub resolves environment secrets first, then
repo secrets — no environment-scoped copy needed for a secret that has only one value anyway).

### 1a. Supabase — expose the `finance` schema on each project's Data API (manual dashboard step)

**Found 2026-09-03**, debugging a live HTTP 406 on the net-worth screen. `supabase/config.toml`'s
`[api] schemas = ["public", "graphql_public", "finance"]` (ADR-0033) is **local-CLI config only** —
it does not sync to a hosted project's Data API settings, whether via `db push` or any other CLI
command. A hosted project's exposed-schema list is a separate Studio setting, and `FinanceSchemaInterceptor`
(`apps/finance/data/.../tracker/net/FinanceSchemaInterceptor.kt`) sends `Accept-Profile: finance` on
every tracker request unconditionally (ADR-0033) — so until this step is done, every tracker call
fails with `406 Not Acceptable` ("The schema must be one of the following: public, graphql_public"),
independent of whether the migration itself has been applied (step 1) or not.

**Status: `dhruv-dev` fixed 2026-09-03. `dhruv-prod` still pending** — do this before `dhruv-prod`
takes any tracker traffic, i.e. before the first `develop → main` promotion that includes the
tracker feature.

Two ways to do it, per project. Either the Studio dashboard (Project Settings → Data API → Exposed
schemas → add `finance` → save), or the Management API directly — this is what was actually used
to fix `dhruv-dev`, with a one-time personal access token (https://supabase.com/dashboard/account/tokens),
revoked immediately after:

```powershell
# read current value first — don't blind-overwrite, db_schema is a full replace not a merge
curl -H "Authorization: Bearer $env:SUPABASE_ACCESS_TOKEN" `
  "https://api.supabase.co/v1/projects/<project-ref>/postgrest"

curl -X PATCH -H "Authorization: Bearer $env:SUPABASE_ACCESS_TOKEN" -H "Content-Type: application/json" `
  -d '{"db_schema":"public,graphql_public,finance"}' `
  "https://api.supabase.co/v1/projects/<project-ref>/postgrest"
```

`<project-ref>` = `dsfnrtckgpnvyvscevxn` (dev, already done) / `ikyzgxnktlccjvjedgal` (prod,
pending). `db_schema` is a full string replace, not additive — always read the current value first
so an unrelated schema someone else added isn't dropped.

Do this *after* the migration itself has actually applied (exposing a schema whose tables don't yet
exist still leaves a `relation does not exist` error on the actual query — just a different error
than 406). Verify with an unauthenticated probe: `406` means the schema still isn't exposed; `401`
means the schema check passed and it's now failing at the (expected, unauthenticated) apikey check.

### 2. Supabase — baseline dhruv-prod

`dhruv-prod` currently has no applied-migration history. Before `supabase-migrate.yml`'s drift
guard can be trusted, prod needs the existing migration set applied once, by hand, so it starts in
lockstep with `dhruv-dev`:

```powershell
supabase login   # opens a browser — one-time
./scripts/env/baseline-prod-db.ps1
```

Shows the pending SQL (dry run) before asking you to confirm. After this, every further migration
goes through the normal PR → `develop` (auto to dev) → `main` (approval-gated to prod) flow —
this script is a one-time bootstrap, not something you run again.

### 3. Vercel — connect the Git integration

Dashboard steps (ADR-0032 decision 5 — Git integration, not CLI-driven deploys, so there's no
meaningful CLI shortcut here):

1. [vercel.com/new](https://vercel.com/new) → **Import Git Repository** → authorize the Vercel
   GitHub App for `saikumarkalli/dhruv` if not already installed.
2. Root Directory: **`web`**. Framework preset: Vite (auto-detected).
3. Project Settings → Git → **Production Branch: `main`** (not the Vercel default of whatever your
   GitHub default branch is — `develop` is this repo's default, and it must stay Preview, not
   Production).
4. Project Settings → Environment Variables:
   - `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` → **Production** environment: `dhruv-prod`'s
     values.
   - Same two keys → **Preview** and **Development** environments: `dhruv-dev`'s values.
5. Done — every push to `main` deploys Production, every other branch/PR gets an automatic Preview
   URL. No GitHub Actions involvement (`web-ci.yml` is a gate only, per ADR-0032 decision 5).

### 4. Google Cloud Console — add both Supabase callback URLs

One Google OAuth Web Client is shared across `dhruv-dev`/`dhruv-prod` (ADR-0031 — this is
unchanged by ADR-0032, just needs both projects' callback URLs registered):

1. [Google Cloud Console](https://console.cloud.google.com/apis/credentials) → the Dhruv OAuth 2.0
   Web Client → **Authorized redirect URIs** → add both:
   - `https://dsfnrtckgpnvyvscevxn.supabase.co/auth/v1/callback` (dhruv-dev)
   - `https://ikyzgxnktlccjvjedgal.supabase.co/auth/v1/callback` (dhruv-prod)
2. In **both** Supabase projects (Authentication → URL Configuration):
   - dhruv-dev: Site URL / Redirect URLs → your Vercel Preview pattern, e.g.
     `https://dhruv-*.vercel.app/**` (Preview URLs are per-deployment; a wildcard covers them).
   - dhruv-prod: Site URL / Redirect URLs → the Vercel Production domain from step 3.
3. In **both** Supabase projects (Authentication → Providers → Google): enable, paste the same
   Web Client ID + secret.

### 5. Verify

```powershell
gh secret list --repo saikumarkalli/dhruv --env dev
gh secret list --repo saikumarkalli/dhruv --env prod
gh variable list --repo saikumarkalli/dhruv
```

Then open a small PR touching `supabase/**` to see `verify` run, merge to `develop` to see
`apply-dev` run, and — when ready — merge `develop → main` to see `release-approval` /
`prod-plan` → `prod-approval` open their GitHub issues.

## Historical note: one direct push to `develop` (2026-08-27)

`branch-guard.yml`'s `Guard · Direct push to protected branch` failed on the same 2026-08-27 push
(run `33080504999`):

```
Commit 686a2b3 ('feat: initialize finance app architecture, Supabase schema modules, and phase 2
tracker database migrations') was pushed directly to 'develop' — no merged pull request contains
it.
```

That commit is already in `develop`'s history (merged transitively via PR #43's later merge commit
`ee1c143`), so this is a **retroactive flag, not an open item** — nothing to fix, no action
possible without a history rewrite. Recorded here only so a future read of failing `develop` runs
doesn't re-investigate it: it is the guard correctly doing its job on a one-time bypass, not a bug
in the guard or a sign the hook (`scripts/hooks/pre-push`) isn't working now.
