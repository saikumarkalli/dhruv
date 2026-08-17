<#
.SYNOPSIS
    Applies real, server-side branch protection to `main` and `develop` via GitHub
    repository rulesets. Idempotent — safe to re-run.

.DESCRIPTION
    `main` (PROD) and `develop` (DEV) advance only through a merged pull request
    (AGENTS.md branch rules, ADR-0032 decision 1). This script is the *real*
    enforcement of that rule.

    IT DOES NOT WORK ON GITHUB FREE. Repository rulesets and classic branch
    protection are both Pro/Team features for a PRIVATE repo. Verified live
    against this repo:

        GET /repos/saikumarkalli/dhruv/rulesets
          403 "Upgrade to GitHub Pro or make this repository public to enable this feature."

    Until that upgrade the rule is enforced by two unpaid substitutes, both of
    which stay useful afterwards but stop being load-bearing:
      * scripts/hooks/pre-push          - preventive, refuses the push locally
      * .github/workflows/branch-guard.yml - detective, red-X's a bypassed push

    Run this script the day the plan is upgraded. It will tell you plainly if
    the tier still refuses.

.PARAMETER Repo
    owner/name. Defaults to the repo the current directory is a clone of.

.PARAMETER AllowAdminBypass
    Adds RepositoryRole=admin as a bypass actor, letting you push straight to a
    protected branch in an emergency. OFF by default: with it on, "everything
    goes through a PR" becomes "everything goes through a PR unless I forget".
    You are never locked out either way - a repo admin can always disable the
    ruleset from Settings > Rules.

.PARAMETER WhatIf
    Print the ruleset payloads and exit without calling the API.

.EXAMPLE
    pwsh scripts/env/apply-branch-protection.ps1
    pwsh scripts/env/apply-branch-protection.ps1 -WhatIf
#>
[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string] $Repo,
    [switch] $AllowAdminBypass
)

$ErrorActionPreference = 'Stop'

# --- Preconditions ---------------------------------------------------------

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) not found on PATH. Install it: https://cli.github.com/"
}

gh auth status 2>&1 | Out-Null
if (-not $?) {
    throw "gh is not authenticated. Run: gh auth login"
}

if (-not $Repo) {
    $Repo = (gh repo view --json nameWithOwner --jq '.nameWithOwner')
    if (-not $?) { throw "Could not resolve the repo. Pass -Repo owner/name." }
}
Write-Host "Repo: $Repo"

# Fail fast and legibly if the plan still refuses, rather than emitting a raw 403.
try {
    gh api "repos/$Repo/rulesets" --silent 2>$null
    if (-not $?) { throw "rulesets probe failed" }
}
catch {
    Write-Host ""
    Write-Host "BLOCKED - the rulesets API refused." -ForegroundColor Yellow
    Write-Host "  A PRIVATE repo on GitHub Free cannot use rulesets or classic branch"
    Write-Host "  protection. Either:"
    Write-Host "    a) upgrade this account to GitHub Pro, then re-run this script; or"
    Write-Host "    b) keep the free substitutes (scripts/hooks/pre-push +"
    Write-Host "       .github/workflows/branch-guard.yml), which are already active."
    Write-Host ""
    Write-Host "  Raw response:"
    gh api "repos/$Repo/rulesets" 2>&1 | ForEach-Object { Write-Host "    $_" }
    exit 2
}

# --- Required status checks ------------------------------------------------
#
# Job *names* from .github/workflows, not job ids. `pr-summary` is deliberately
# absent: it is continue-on-error and therefore always green, so requiring it
# would be purely cosmetic (ADR-0012). Docs-only PRs skip these jobs via the
# `changes` gate, and a skipped job counts as passing - which is exactly why
# ADR-0026 uses a job-level `if:` instead of `paths-ignore`.

$requiredChecks = @(
    'Gate 1 · Static Analysis',
    'Gate 2 · Security',
    'Gate 3+4 · Tests + ArchUnit + Coverage + Build',
    'Web · Lint + Typecheck + Test + Build'
)

$bypassActors = @()
if ($AllowAdminBypass) {
    $bypassActors = @(
        @{ actor_id = 5; actor_type = 'RepositoryRole'; bypass_mode = 'always' }
    )
}

function New-RulesetPayload {
    param([string] $Branch)

    $rules = @(
        # No deleting a protected branch.
        @{ type = 'deletion' },
        # No force-push / history rewrite.
        @{ type = 'non_fast_forward' },
        @{
            type       = 'pull_request'
            parameters = @{
                # 0, not 1: a solo maintainer cannot approve their own PR, and a
                # rule nobody can satisfy is a rule that gets bypassed. The PR
                # itself is the gate here, not the approval count. Raise this the
                # moment a second maintainer exists.
                required_approving_review_count   = 0
                dismiss_stale_reviews_on_push     = $true
                require_code_owner_review         = $false
                require_last_push_approval        = $false
                required_review_thread_resolution = $true
                allowed_merge_methods             = @('merge', 'squash', 'rebase')
            }
        },
        @{
            type       = 'required_status_checks'
            parameters = @{
                # ADR-0026 calls this load-bearing: it is what makes skipping the
                # merge-push re-run safe, because the merged tree is guaranteed to
                # be the tree the PR validated.
                strict_required_status_checks_policy = $true
                do_not_enforce_on_create             = $false
                required_status_checks               = @(
                    $requiredChecks | ForEach-Object { @{ context = $_ } }
                )
            }
        }
    )

    return @{
        name          = "protect-$Branch"
        target        = 'branch'
        enforcement   = 'active'
        bypass_actors = $bypassActors
        conditions    = @{
            ref_name = @{ include = @("refs/heads/$Branch"); exclude = @() }
        }
        rules         = $rules
    }
}

# --- Apply -----------------------------------------------------------------

$existing = gh api "repos/$Repo/rulesets" --jq '.[] | "\(.id) \(.name)"' 2>$null
$existingMap = @{}
if ($existing) {
    foreach ($line in ($existing -split "`n")) {
        if ($line -match '^\s*(\d+)\s+(.+?)\s*$') { $existingMap[$Matches[2]] = $Matches[1] }
    }
}

foreach ($branch in @('main', 'develop')) {
    $payload = New-RulesetPayload -Branch $branch
    $name = $payload.name
    $json = $payload | ConvertTo-Json -Depth 10 -Compress

    if ($WhatIfPreference) {
        Write-Host ""
        Write-Host "--- would apply: $name ---"
        Write-Host ($payload | ConvertTo-Json -Depth 10)
        continue
    }

    $tmp = [System.IO.Path]::GetTempFileName()
    try {
        # -Encoding utf8 matters: gh reads the file as UTF-8 and the check names
        # contain a non-ASCII middle dot.
        $json | Out-File -FilePath $tmp -Encoding utf8 -NoNewline

        if ($existingMap.ContainsKey($name)) {
            $id = $existingMap[$name]
            Write-Host "Updating ruleset '$name' (id $id)..."
            gh api --method PUT "repos/$Repo/rulesets/$id" --input $tmp --silent
        }
        else {
            Write-Host "Creating ruleset '$name'..."
            gh api --method POST "repos/$Repo/rulesets" --input $tmp --silent
        }

        if (-not $?) { throw "Failed to apply ruleset '$name'." }
        Write-Host "  OK - $branch is now PR-only, no force-push, no deletion." -ForegroundColor Green
    }
    finally {
        Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "Done. Verify at: https://github.com/$Repo/settings/rules"
Write-Host ""
Write-Host "Follow-ups now that protection is real:"
Write-Host "  1. Delete .github/workflows/branch-guard.yml - GitHub enforces this now."
Write-Host "  2. The pre-push hook can stay; it just fails faster than the server does."
Write-Host "  3. GitHub Pro also unlocks the `prod` Environment's required-reviewer rule."
Write-Host "     Once set, the trstringer/manual-approval jobs in ci.yml and"
Write-Host "     supabase-migrate.yml become redundant (ADR-0032 correction, 2026-08-16)."