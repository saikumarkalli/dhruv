<#
.SYNOPSIS
    Applies real, server-side branch protection to `main` and `develop` via GitHub
    repository rulesets. Idempotent — safe to re-run.

.DESCRIPTION
    `main` (PROD) and `develop` (DEV) advance only through a merged pull request
    (AGENTS.md branch rules, ADR-0032 decision 1). This script is the *real*
    enforcement of that rule.

    Repository rulesets and classic branch protection are Pro/Team features for a
    PRIVATE repo, and are FREE on a public one. GitHub's own refusal named both
    exits — verified live against this repo while it was still private:

        GET /repos/saikumarkalli/dhruv/rulesets
          403 "Upgrade to GitHub Pro or make this repository public to enable this feature."

    ADR-0034 took the second exit: the repository is public, so this script now
    works without any plan upgrade. Run it once after the visibility flip.

    Two unpaid substitutes predate it and are deliberately KEPT, since a local
    check that fails in two seconds still beats a server-side rejection after a
    push. They are no longer load-bearing:
      * scripts/hooks/pre-push          - preventive, refuses the push locally
      * .github/workflows/branch-guard.yml - detective, red-X's a bypassed push

    If the API still refuses, this script will say so plainly rather than
    reporting a success it did not achieve.

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
    Write-Host "  protection. Three exits, cheapest first:"
    Write-Host "    a) make the repository PUBLIC - rulesets are free there, and this is"
    Write-Host "       what ADR-0034 chose. Then re-run this script."
    Write-Host "    b) upgrade this account to GitHub Pro, then re-run this script; or"
    Write-Host "    c) keep the free substitutes (scripts/hooks/pre-push +"
    Write-Host "       .github/workflows/branch-guard.yml), which are already active."
    Write-Host ""
    Write-Host "  If you expected (a) to already be done, the visibility flip has not"
    Write-Host "  landed yet - check: gh api repos/$Repo --jq .visibility"
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

# The gate names contain U+00B7 MIDDLE DOT. Do NOT paste the literal character here.
# Windows PowerShell 5.1 reads a BOM-less .ps1 as ANSI (cp1252), so the UTF-8 bytes
# C2 B7 are decoded as the two characters "Â·" and this script then writes a
# required-status-check name that can never match a real check — which silently BRICKS
# merges on both protected branches (every PR waits forever on a check that will never
# report). Observed live on the first successful run, 2026-08-18. Building the character
# from its code point keeps this file pure ASCII and encoding-independent.
$dot = [char]0x00B7
$requiredChecks = @(
    "Gate 1 $dot Static Analysis",
    "Gate 2 $dot Security",
    "Gate 3+4 $dot Tests + ArchUnit + Coverage + Build",
    "Web $dot Lint + Typecheck + Test + Build"
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

# NOTE: do not use `gh api --jq '... "\(.id) \(.name)" ...'` here. Windows PowerShell 5.1
# re-quotes native-command arguments and splits a jq expression containing double quotes
# into two positional args, so gh fails with `accepts 1 arg(s), received 2`. Fetch raw JSON
# and let PowerShell parse it instead — no jq expression crosses the process boundary.
$existingMap = @{}
$existingRaw = gh api "repos/$Repo/rulesets" 2>$null
if ($LASTEXITCODE -eq 0 -and $existingRaw) {
    foreach ($rs in ($existingRaw | ConvertFrom-Json)) {
        if ($rs.name) { $existingMap[$rs.name] = $rs.id }
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
        # UTF-8 WITHOUT a BOM, written via .NET rather than Out-File. Two separate traps:
        #   * encoding must be UTF-8 at all — the required check names contain a non-ASCII
        #     middle dot, and the ANSI codepage mangles it;
        #   * `Out-File -Encoding utf8` on Windows PowerShell 5.1 emits a BOM, and GitHub's
        #     JSON parser rejects it outright with `Problems parsing JSON (HTTP 400)` —
        #     observed live on the first real run of this script, 2026-08-18.
        [System.IO.File]::WriteAllText($tmp, $json, (New-Object System.Text.UTF8Encoding($false)))

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
Write-Host "  1. branch-guard.yml is now redundant as enforcement, but ADR-0034 keeps it"
Write-Host "     deliberately - a detective check costs nothing and covers the window if a"
Write-Host "     ruleset is ever disabled. Delete it only as a conscious decision."
Write-Host "  2. The pre-push hook can stay; it just fails faster than the server does."
Write-Host "  3. A public repo also unlocks the ``prod`` Environment's required-reviewer rule."
Write-Host "     Set it, then the trstringer/manual-approval jobs in ci.yml and"
Write-Host "     supabase-migrate.yml become redundant (ADR-0032 correction; ADR-0034 d.2"
Write-Host "     keeps both for now rather than removing them in the same change)."