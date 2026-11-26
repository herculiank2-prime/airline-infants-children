<#
.SYNOPSIS
  Copies Mermaid sources from docs/diagrams/*.mmd into the marked blocks of the design documents.
.DESCRIPTION
  A block looks like:
    <!-- diagram:<name> -->
    ...replaced...
    <!-- /diagram:<name> -->
  Run after editing any .mmd file so the design documents never drift from the diagram source.
  Use -Check in CI: it fails if any document is out of date.
#>
param([switch]$Check)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$docs = @('SOLUTION_DESIGN.md', 'docs\02-design.md') | ForEach-Object { Join-Path $root $_ }
$sources = Get-ChildItem (Join-Path $root 'docs\diagrams') -Filter *.mmd
$stale = @()

foreach ($doc in $docs) {
    $original = [IO.File]::ReadAllText($doc)
    $text = $original
    foreach ($src in $sources) {
        $name = $src.BaseName
        $body = [IO.File]::ReadAllText($src.FullName).TrimEnd()
        $pattern = "(?s)<!-- diagram:$([regex]::Escape($name)) -->.*?<!-- /diagram:$([regex]::Escape($name)) -->"
        $replacement = "<!-- diagram:$name -->`n``````mermaid`n$body`n```````n<!-- /diagram:$name -->"
        $text = [regex]::Replace($text, $pattern, { param($m) $replacement })
    }
    if ($text -ne $original) {
        if ($Check) { $stale += $doc } else {
            [IO.File]::WriteAllText($doc, $text, (New-Object Text.UTF8Encoding $false))
            Write-Host "Synced diagrams into $doc"
        }
    }
}
if ($Check -and $stale.Count -gt 0) { Write-Error "Diagram blocks out of date: $($stale -join ', ')" }
