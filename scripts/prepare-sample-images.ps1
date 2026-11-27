<#
.SYNOPSIS
  Writes a demo image into ui.content.sample as a FileVault dam:Asset: a JPEG original plus the
  cq5dam.web.<w>.<h>.jpeg renditions the hero uses (640/960/1280), with dc:description for alt text.

.DESCRIPTION
  On AEM as a Cloud Service the web renditions come from the "airline-web" processing profile. The
  local SDK has no asset microservices, so the sample package ships them pre-built. Names carry the
  real pixel size, which HeroBannerImpl reads when TIFF metadata is missing.

.EXAMPLE
  ./scripts/prepare-sample-images.ps1 -Source family-hero.png -Name family-hero.jpg `
      -Title "Family on a flight" -Description "A parent holding a smiling baby by an aircraft window while a young child looks out at the clouds"
#>
param(
    [Parameter(Mandatory)] [string] $Source,
    [Parameter(Mandatory)] [string] $Name,
    [Parameter(Mandatory)] [string] $Title,
    [Parameter(Mandatory)] [string] $Description,
    [string] $Folder = "/content/dam/airline/travel-information/infants-children",
    [int] $OriginalWidth = 1600,
    [int[]] $Widths = @(640, 960, 1280),
    [long] $Quality = 80
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$jcrRoot = Join-Path $root "ui.content.sample/src/main/content/jcr_root"
$assetDir = Join-Path $jcrRoot (($Folder.TrimStart("/") -replace "/", "\") + "\" + $Name)
$renditionDir = Join-Path $assetDir "_jcr_content\renditions"
New-Item -ItemType Directory -Force -Path $renditionDir | Out-Null

$codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" }
$encoderParams = New-Object System.Drawing.Imaging.EncoderParameters 1
$encoderParams.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter ([System.Drawing.Imaging.Encoder]::Quality), $Quality

function Save-Resized([System.Drawing.Image] $image, [int] $width, [string] $path) {
    $height = [int][Math]::Round($image.Height * $width / $image.Width)
    $bitmap = New-Object System.Drawing.Bitmap $width, $height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.DrawImage($image, 0, 0, $width, $height)
    $bitmap.Save($path, $codec, $encoderParams)
    $graphics.Dispose()
    $bitmap.Dispose()
    return $height
}

$sourceImage = [System.Drawing.Image]::FromFile((Resolve-Path $Source))
try {
    $originalPath = Join-Path $renditionDir "original"
    $originalHeight = Save-Resized $sourceImage $OriginalWidth $originalPath
    foreach ($width in $Widths) {
        $height = [int][Math]::Round($sourceImage.Height * $width / $sourceImage.Width)
        $file = Join-Path $renditionDir "cq5dam.web.$width.$height.jpeg"
        Save-Resized $sourceImage $width $file | Out-Null
        "{0} {1:N0} KB" -f (Split-Path -Leaf $file), ((Get-Item $file).Length / 1KB)
    }
} finally {
    $sourceImage.Dispose()
}

$utf8 = New-Object System.Text.UTF8Encoding $false
$escape = { param($s) [System.Security.SecurityElement]::Escape($s) }

New-Item -ItemType Directory -Force -Path (Join-Path $renditionDir "original.dir") | Out-Null
[IO.File]::WriteAllText((Join-Path $renditionDir "original.dir\.content.xml"), @"
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="nt:file">
    <jcr:content
        jcr:mimeType="image/jpeg"
        jcr:primaryType="nt:resource"/>
</jcr:root>
"@, $utf8)

[IO.File]::WriteAllText((Join-Path $assetDir ".content.xml"), @"
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:tiff="http://ns.adobe.com/tiff/1.0/" xmlns:dam="http://www.day.com/dam/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="dam:Asset">
    <jcr:content
        jcr:primaryType="dam:AssetContent">
        <metadata
            dam:Fileformat="JPEG"
            dam:MIMEtype="image/jpeg"
            dc:description="$(& $escape $Description)"
            dc:format="image/jpeg"
            dc:title="$(& $escape $Title)"
            jcr:primaryType="nt:unstructured"
            tiff:ImageLength="{Long}$originalHeight"
            tiff:ImageWidth="{Long}$OriginalWidth"/>
        <related jcr:primaryType="nt:unstructured"/>
    </jcr:content>
</jcr:root>
"@, $utf8)

"Wrote $assetDir (original ${OriginalWidth}x$originalHeight)"
