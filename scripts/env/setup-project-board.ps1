<#
.SYNOPSIS
    Creates the "Dhruv Delivery" GitHub Project (Projects v2) board and links it to
    the repo. Idempotent — re-running finds the existing board instead of duplicating.

.DESCRIPTION
    Projects v2 lives at the ACCOUNT level, not the repo level, so it is free on
    GitHub Free even for a private repo — unlike rulesets and Environment reviewer
    rules, which are Pro-gated (see apply-branch-protection.ps1).

    REQUIRES AN EXTRA TOKEN SCOPE. The default `gh auth login` scopes
    (gist, read:org, repo, workflow) do not include project access, and granting it
    opens a browser, so it cannot be scripted from a non-interactive session:

        gh auth refresh -s project,read:project

    Run that once, then run this script.

.PARAMETER Owner
    GitHub account that owns the board. Defaults to saikumarkalli.

.PARAMETER Repo
    owner/name to link the board to. Defaults to the current directory's repo.

.PARAMETER Title
    Board title. Defaults to "Dhruv Delivery".

.EXAMPLE
    gh auth refresh -s project,read:project
    pwsh scripts/env/setup-project-board.ps1
#>
[CmdletBinding()]
param(
    [string] $Owner = 'saikumarkalli',
    [string] $Repo,
    [string] $Title = 'Dhruv Delivery'
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) not found on PATH."
}

if (-not $Repo) {
    $Repo = (gh repo view --json nameWithOwner --jq '.nameWithOwner')
    if (-not $?) { throw "Could not resolve the repo. Pass -Repo owner/name." }
}

# --- Scope check, with the fix spelled out rather than a raw API error ---------

gh project list --owner $Owner --limit 1 2>&1 | Out-Null
if (-not $?) {
    Write-Host ""
    Write-Host "BLOCKED - your gh token lacks the project scope." -ForegroundColor Yellow
    Write-Host "  Run this once (it opens a browser), then re-run this script:"
    Write-Host ""
    Write-Host "      gh auth refresh -s project,read:project" -ForegroundColor Cyan
    Write-Host ""
    exit 2
}

# --- Create or find the board -------------------------------------------------

$existing = gh project list --owner $Owner --format json --jq ".projects[] | select(.title == `"$Title`") | .number" 2>$null

if ($existing) {
    $number = ($existing -split "`n")[0].Trim()
    Write-Host "Board '$Title' already exists (#$number) - reusing."
}
else {
    Write-Host "Creating board '$Title'..."
    $number = gh project create --owner $Owner --title $Title --format json --jq '.number'
    if (-not $?) { throw "Failed to create the project board." }
    Write-Host "  created #$number"
}

# --- Link it to the repo so new issues/PRs can be added from the repo UI ------

Write-Host "Linking board #$number to $Repo..."
gh project link $number --owner $Owner --repo $Repo 2>&1 | Out-Null
if ($?) { Write-Host "  linked" } else { Write-Host "  already linked (or link not permitted) - continuing" }

# --- Status column values -----------------------------------------------------
#
# `gh project` cannot edit single-select field OPTIONS (only item values), so the
# default Todo / In Progress / Done set has to be adjusted in the web UI if you
# want more columns. Deliberately not faked with a bespoke GraphQL mutation here:
# a hand-rolled mutation would silently drift from whatever GitHub's schema does
# next, and this board is cheap to adjust by hand once.

Write-Host ""
Write-Host "Done. Board: https://github.com/users/$Owner/projects/$number"
Write-Host ""
Write-Host "Recommended next steps (web UI, one-time):"
Write-Host "  1. Add a 'Phase' single-select field with values Phase 0 .. Phase 7 so the"
Write-Host "     board groups the same way the milestones do."
Write-Host "  2. Workflows tab -> enable 'Item added to project', 'Item closed', and"
Write-Host "     'Pull request merged' so issues/PRs move columns without manual dragging."
Write-Host "  3. Settings -> add the repo to auto-add newly opened issues."
Write-Host ""
Write-Host "Milestones already exist for Phases 0-7 (created 2026-08-17); the board is the"
Write-Host "'where is it right now' view, the milestones are the 'what is left in this phase' view."