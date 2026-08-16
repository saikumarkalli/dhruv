#!/usr/bin/env pwsh
<#
.SYNOPSIS
  One-time: apply the existing migration set to dhruv-prod so it starts in
  lockstep with dhruv-dev, BEFORE supabase-migrate.yml's drift guard becomes
  load-bearing (ADR-0032 consequence). Run this YOURSELF, once, from the repo
  root, after set-env-secrets.ps1 and after `supabase login`.

.DESCRIPTION
  Without this step, the first push to `main` touching supabase/** would find
  dhruv-prod with NO applied-migration history at all — which the CLI's own
  drift/history tracking would (correctly) refuse to silently paper over. This
  script runs the same `supabase db push` the CI workflow will run later, just
  once, by hand, with you watching the output.

  Prompts for the dhruv-prod database password (masked) rather than reading it
  from anywhere — this script is not meant to be run non-interactively.

.NOTES
  Requires: supabase CLI (`npm install -g supabase`), already logged in
  (`supabase login`) or SUPABASE_ACCESS_TOKEN set in this shell.
#>

$ErrorActionPreference = "Stop"

Write-Host "supabase --version:"
supabase --version

$prodRef = Read-Host "dhruv-prod project ref (Settings -> General -> Reference ID)"
if ([string]::IsNullOrWhiteSpace($prodRef)) {
    throw "Project ref is required."
}

$securePassword = Read-Host -Prompt "dhruv-prod database password" -AsSecureString
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $dbPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
} finally {
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}

Push-Location (Join-Path $PSScriptRoot "..\..")
try {
    Write-Host ""
    Write-Host "=== Linking dhruv-prod ==="
    supabase link --project-ref $prodRef

    Write-Host ""
    Write-Host "=== Pending migrations (dry run — nothing applied yet) ==="
    supabase db push --dry-run --password $dbPassword

    $confirm = Read-Host "Apply the above to dhruv-prod now? This is the ONE-TIME baseline. [y/N]"
    if ($confirm -ne "y") {
        Write-Host "Aborted — nothing applied."
        exit 0
    }

    Write-Host ""
    Write-Host "=== Applying to dhruv-prod ==="
    supabase db push --password $dbPassword

    Write-Host ""
    Write-Host "Done. dhruv-prod now matches dhruv-dev's migration history."
    Write-Host "From here on, supabase-migrate.yml's prod-apply job owns every further migration."
} finally {
    Pop-Location
}
