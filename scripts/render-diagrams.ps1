<#
.SYNOPSIS
  Renders docs/diagrams/*.mmd to docs/diagrams/rendered/*.svg (and .png) with mermaid-cli.
.DESCRIPTION
  Uses the Node installed by the Maven build (ui.frontend/node) and the system Edge or Chrome,
  so Puppeteer never downloads a browser. Run scripts/sync-diagrams.ps1 as well after editing a source.
#>
param([switch]$Png)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$tools = Join-Path $root 'tools\diagrams'
$nodeDir = Join-Path $root 'ui.frontend\node'
if (-not (Test-Path (Join-Path $nodeDir 'node.exe'))) { throw "Node not found in $nodeDir. Run 'mvn -pl ui.frontend generate-resources' first." }
$env:PATH = "$nodeDir;$env:PATH"
$env:PUPPETEER_SKIP_DOWNLOAD = 'true'

$browser = @(
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Google\Chrome\Application\chrome.exe'
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $browser) { throw 'No Edge or Chrome installation found.' }

Push-Location $tools
try {
    if (-not (Test-Path 'node_modules\.bin\mmdc.cmd')) { & (Join-Path $nodeDir 'npm.cmd') install --no-audit --no-fund | Out-Host }
    $puppeteerConfig = Join-Path $tools 'puppeteer-config.json'
    @{ executablePath = $browser; args = @('--no-sandbox') } | ConvertTo-Json | Set-Content -Path $puppeteerConfig -Encoding ascii

    $out = Join-Path $root 'docs\diagrams\rendered'
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    $mmdc = Join-Path $tools 'node_modules\.bin\mmdc.cmd'
    Get-ChildItem (Join-Path $root 'docs\diagrams') -Filter *.mmd | ForEach-Object {
        $formats = @('svg'); if ($Png) { $formats += 'png' }
        foreach ($f in $formats) {
            $target = Join-Path $out "$($_.BaseName).$f"
            & $mmdc -i $_.FullName -o $target -p $puppeteerConfig -b white -w 1600 -q
            if ($LASTEXITCODE -ne 0) { throw "mermaid-cli failed for $($_.Name)" }
            Write-Host "Rendered $target"
        }
    }
} finally { Pop-Location }
