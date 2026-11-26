<#
.SYNOPSIS
  Copies Mermaid sources from docs/diagrams/*.mmd into the marked blocks of docs/02-design.md.
.DESCRIPTION
  A block looks like:
    <!-- diagram:<name> -->
    ...replaced...
    <!-- /diagram:<name> -->
  Run after editing any .mmd file so the design document never drifts from the diagram source.
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$doc = Join-Path $root 'docs\02-design.md'
$text = [IO.File]::ReadAllText($doc)

Get-ChildItem (Join-Path $root 'docs\diagrams') -Filter *.mmd | ForEach-Object {
    $name = $_.BaseName
    $source = [IO.File]::ReadAllText($_.FullName).TrimEnd()
    $pattern = "(?s)<!-- diagram:$([regex]::Escape($name)) -->.*?<!-- /diagram:$([regex]::Escape($name)) -->"
    $replacement = "<!-- diagram:$name -->`n``````mermaid`n$source`n```````n<!-- /diagram:$name -->"
    $text = [regex]::Replace($text, $pattern, { param($m) $replacement })
}

[IO.File]::WriteAllText($doc, $text, (New-Object Text.UTF8Encoding $false))
Write-Host "Synced diagrams into $doc"
