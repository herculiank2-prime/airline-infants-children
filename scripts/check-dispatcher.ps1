<#
.SYNOPSIS
  Guards the Dispatcher configuration where the SDK validator is not available locally (ADR-009, R-11).
.DESCRIPTION
  - Each file in enabled_vhosts / enabled_farms must be byte-identical to its source in available_*.
    The archetype was generated on Windows without symlink support, so these are copies.
  - filters.any must not include the archetype default_filters.any (ADR-015).
  The authoritative check is the SDK validator in the Cloud Manager web-tier pipeline.
#>
$ErrorActionPreference = 'Stop'
$src = Join-Path (Split-Path -Parent $PSScriptRoot) 'dispatcher\src'
$errors = @()

$pairs = @(
    @{ enabled = 'conf.d\enabled_vhosts'; available = 'conf.d\available_vhosts'; filter = '*.vhost' },
    @{ enabled = 'conf.dispatcher.d\enabled_farms'; available = 'conf.dispatcher.d\available_farms'; filter = '*.farm' }
)
foreach ($p in $pairs) {
    Get-ChildItem (Join-Path $src $p.enabled) -File -Filter $p.filter | ForEach-Object {
        $source = Join-Path (Join-Path $src $p.available) $_.Name
        if (-not (Test-Path $source)) { $errors += "$($p.enabled)\$($_.Name) has no source in $($p.available)"; return }
        if ((Get-FileHash $_.FullName).Hash -ne (Get-FileHash $source).Hash) { $errors += "$($p.enabled)\$($_.Name) differs from $($p.available)\$($_.Name)" }
    }
}

$filters = Join-Path $src 'conf.dispatcher.d\filters\filters.any'
if (Select-String -Path $filters -Pattern '^\s*\$include\s+"\./default_filters\.any"' -Quiet) { $errors += 'filters.any includes default_filters.any' }

if ($errors) { $errors | ForEach-Object { Write-Host "FAIL $_" -ForegroundColor Red }; exit 1 }
Write-Host 'OK: enabled_* copies match available_*, no default filters included.'
