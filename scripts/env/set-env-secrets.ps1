#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Interactive, one-time setup of the GitHub Environment secrets ADR-0032 needs
  for dhruv-dev / dhruv-prod. Run this YOURSELF in your own terminal — it
  prompts for real credentials (masked input) and must never be run by an
  automated agent or pasted into a chat session.

.DESCRIPTION
  Sets, per Supabase project:
    - SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_PROJECT_REF, SUPABASE_DB_PASSWORD,
      SUPABASE_ACCESS_TOKEN  -> scoped to the matching GitHub Environment (dev/prod)
    - SUPABASE_{DEV,PROD}_URL, SUPABASE_{DEV,PROD}_ANON_KEY -> plain repo secrets,
      consumed only by supabase-keepalive.yml (deliberately NOT environment-scoped
      — see that workflow's own header comment for why)

  Requires: gh CLI, already authenticated (`gh auth status`).
  Does NOT set KEYSTORE_BASE64/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD (prod signing
  secrets) — those already exist as repo-level secrets from before this ADR; move
  them into the `prod` Environment yourself via the GitHub UI (Settings ->
  Environments -> prod -> Environment secrets -> Add) since this script has no way
  to read their current values back out to copy them (GitHub secrets are write-only
  by design).

.NOTES
  Repo: saikumarkalli/dhruv
#>

$ErrorActionPreference = "Stop"
$Repo = "saikumarkalli/dhruv"

function Read-Secret([string]$Prompt) {
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Set-EnvSecret([string]$Name, [string]$Value, [string]$EnvName) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        Write-Warning "$Name for '$EnvName' left blank — skipping."
        return
    }
    $Value | gh secret set $Name --repo $Repo --env $EnvName --body -
    Write-Host "  set $Name -> environment '$EnvName'"
}

function Set-RepoSecret([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        Write-Warning "$Name left blank — skipping."
        return
    }
    $Value | gh secret set $Name --repo $Repo --body -
    Write-Host "  set $Name -> repo-level"
}

Write-Host "gh auth status:"
gh auth status

foreach ($proj in @(
    @{ Label = "dhruv-dev";  EnvName = "dev";  KeySuffix = "DEV" },
    @{ Label = "dhruv-prod"; EnvName = "prod"; KeySuffix = "PROD" }
)) {
    Write-Host ""
    Write-Host "=== $($proj.Label) (GitHub Environment: $($proj.EnvName)) ==="
    Write-Host "Get these from the Supabase dashboard: that project -> Project Settings."

    $url = Read-Host "  $($proj.Label) API URL (Settings -> API -> Project URL, e.g. https://<ref>.supabase.co)"
    $anon = Read-Secret "  $($proj.Label) anon/publishable key (Settings -> API)"
    $ref = Read-Host "  $($proj.Label) project ref (Settings -> General -> Reference ID)"
    $dbPassword = Read-Secret "  $($proj.Label) database password (set at project creation; reset via Settings -> Database if lost)"

    Set-EnvSecret -Name "SUPABASE_URL" -Value $url -EnvName $proj.EnvName
    Set-EnvSecret -Name "SUPABASE_ANON_KEY" -Value $anon -EnvName $proj.EnvName
    Set-EnvSecret -Name "SUPABASE_PROJECT_REF" -Value $ref -EnvName $proj.EnvName
    Set-EnvSecret -Name "SUPABASE_DB_PASSWORD" -Value $dbPassword -EnvName $proj.EnvName

    # Repo-level duplicates for the keepalive ping only (see supabase-keepalive.yml).
    Set-RepoSecret -Name "SUPABASE_$($proj.KeySuffix)_URL" -Value $url
    Set-RepoSecret -Name "SUPABASE_$($proj.KeySuffix)_ANON_KEY" -Value $anon
}

Write-Host ""
Write-Host "=== Supabase access token (shared — same account owns both projects) ==="
Write-Host "Get from https://supabase.com/dashboard/account/tokens (generate one if none exists)."
$accessToken = Read-Secret "  SUPABASE_ACCESS_TOKEN"
Set-EnvSecret -Name "SUPABASE_ACCESS_TOKEN" -Value $accessToken -EnvName "dev"
Set-EnvSecret -Name "SUPABASE_ACCESS_TOKEN" -Value $accessToken -EnvName "prod"

Write-Host ""
Write-Host "=== Cleanup: remove the old flat repo-level Supabase secrets ==="
Write-Host "SUPABASE_URL/SUPABASE_ANON_KEY currently exist at repo level (pre-dev/prod-split)."
Write-Host "Leaving them in place means a job that forgets 'environment: dev|prod' silently"
Write-Host "falls back to these instead of failing loudly — exactly the ambiguity ADR-0032's"
Write-Host "dev-ref guard exists to catch, but better to just not have the fallback at all."
$confirm = Read-Host "Delete repo-level SUPABASE_URL and SUPABASE_ANON_KEY now? [y/N]"
if ($confirm -eq "y") {
    gh secret delete SUPABASE_URL --repo $Repo
    gh secret delete SUPABASE_ANON_KEY --repo $Repo
    Write-Host "Deleted."
} else {
    Write-Host "Left in place — delete manually later via: gh secret delete SUPABASE_URL --repo $Repo"
}

Write-Host ""
Write-Host "Done. Verify with: gh secret list --repo $Repo --env dev ; gh secret list --repo $Repo --env prod"
