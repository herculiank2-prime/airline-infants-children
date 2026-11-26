<#
.SYNOPSIS
  Checks that integration numbers (I-xx) agree across the integration register, the diagrams and the docs.
.DESCRIPTION
  - Every I-xx used anywhere must be defined in the register (SOLUTION_DESIGN.md section 5).
  - Every registered I-xx must appear in at least one diagram source.
  Exits 1 on any mismatch.
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$design = [IO.File]::ReadAllText((Join-Path $root 'SOLUTION_DESIGN.md'))

$registered = [regex]::Matches($design, '(?m)^\| (I-\d{2}) \|') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
if (-not $registered) { Write-Error 'No integration register rows found in SOLUTION_DESIGN.md'; exit 1 }

$diagramText = (Get-ChildItem (Join-Path $root 'docs\diagrams') -Filter *.mmd | ForEach-Object { [IO.File]::ReadAllText($_.FullName) }) -join "`n"
$inDiagrams = [regex]::Matches($diagramText, 'I-\d{2}') | ForEach-Object Value | Sort-Object -Unique

$docFiles = @('SOLUTION_DESIGN.md', 'docs\02-design.md', 'docs\04-decision-log.md', 'README.md', 'AUTHORING.md') |
    ForEach-Object { Join-Path $root $_ } | Where-Object { Test-Path $_ }
$docText = ($docFiles | ForEach-Object { [IO.File]::ReadAllText($_) }) -join "`n"
$used = ([regex]::Matches($docText + $diagramText, 'I-\d{2}') | ForEach-Object Value | Sort-Object -Unique)

$errors = @()
foreach ($id in $used) { if ($registered -notcontains $id) { $errors += "$id is used but not in the integration register" } }
foreach ($id in $registered) { if ($inDiagrams -notcontains $id) { $errors += "$id is registered but appears in no diagram" } }

if ($errors) { $errors | ForEach-Object { Write-Host "FAIL $_" -ForegroundColor Red }; exit 1 }
Write-Host "OK: $($registered.Count) integrations, all used IDs registered, all registered IDs drawn."
