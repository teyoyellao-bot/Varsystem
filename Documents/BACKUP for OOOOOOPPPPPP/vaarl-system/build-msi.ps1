<#
.SYNOPSIS
    Builds a Windows MSI installer for VAARL using jpackage + WiX Toolset 3.x.

.DESCRIPTION
    Normally called by Maven (mvn clean package -Pmsi) but it can also be run by hand
    after "mvn package":   powershell -ExecutionPolicy Bypass -File build-msi.ps1
    If -AppVersion is not given, it is read from <revision> in pom.xml.
    Output: installer\<AppName>-<AppVersion>.msi
#>
[CmdletBinding()]
param(
    [string]$AppName     = 'VAARL',
    [string]$AppVersion  = '',
    [string]$JarName     = 'vaarl-system-app.jar',
    [string]$MainClass   = 'varsystem.Main',
    [string]$UpgradeUuid = '711686D1-D92A-4115-9E43-0DFEF9E317AA',
    [string]$Vendor      = 'VAARL',
    [string]$Description = 'Vehicle Assistance and Repair Locator',
    [string]$IconPath    = 'app.ico',     # optional: used only if the file exists
    [string]$OutDir      = 'installer'
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# --- 1. Version (from Maven, or from pom.xml when run by hand) -----------------
if ([string]::IsNullOrWhiteSpace($AppVersion)) {
    [xml]$pom = Get-Content -LiteralPath 'pom.xml'
    $AppVersion = [string]$pom.project.properties.revision
}
$AppVersion = $AppVersion.Trim()

# MSI rule used by jpackage: major.minor.build  (major <= 255, minor <= 255, build <= 65535)
if ($AppVersion -notmatch '^(\d+)(?:\.(\d+))?(?:\.(\d+))?$') {
    throw "Version '$AppVersion' is not valid for an MSI. Use 1, 1.2 or 1.2.3 (digits only, no -SNAPSHOT). Change <revision> in pom.xml or pass -Drevision=1.2.0"
}
$major = [int]$Matches[1]
$minor = if ($Matches[2]) { [int]$Matches[2] } else { 0 }
$build = if ($Matches[3]) { [int]$Matches[3] } else { 0 }
if ($major -gt 255 -or $minor -gt 255 -or $build -gt 65535) {
    throw "Version '$AppVersion' is out of range for an MSI (max 255.255.65535)."
}

# --- 2. The fat jar built by Maven ---------------------------------------------
$jar = Join-Path 'target' $JarName
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Cannot find $jar. Run 'mvn clean package' first (or use build-msi.bat)."
}

# --- 3. jpackage (ships with the JDK) ------------------------------------------
$jpackage = $null
$cmd = Get-Command jpackage -ErrorAction SilentlyContinue
if ($cmd) { $jpackage = $cmd.Source }
elseif ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
    $jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
}
if (-not $jpackage) {
    throw "jpackage not found. Install JDK 21 and make sure its 'bin' folder is on PATH (or set JAVA_HOME)."
}

# --- 4. WiX Toolset: the copy Maven downloaded to target\wix, else an installed one
$candle = Get-ChildItem -LiteralPath 'target\wix' -Recurse -Filter 'candle.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($candle) {
    $env:PATH = $candle.DirectoryName + ';' + $env:PATH
    Write-Host "Using WiX from $($candle.DirectoryName)"
}
elseif (Get-Command candle.exe -ErrorAction SilentlyContinue) {
    Write-Host 'Using WiX found on PATH'
}
else {
    throw "WiX Toolset 3.x not found (candle.exe / light.exe). Run 'mvn clean package -Pmsi' so Maven downloads it, or install WiX 3.14 and add it to PATH."
}
if (-not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
    throw 'light.exe (part of WiX 3.x) was not found next to candle.exe.'
}

# --- 5. Stage a clean input folder (only the app jar) --------------------------
$inputDir = 'target\jpackage-input'
if (Test-Path -LiteralPath $inputDir) { Remove-Item -LiteralPath $inputDir -Recurse -Force }
New-Item -ItemType Directory -Path $inputDir | Out-Null
Copy-Item -LiteralPath $jar -Destination $inputDir

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$msi = Join-Path $OutDir ("{0}-{1}.msi" -f $AppName, $AppVersion)
if (Test-Path -LiteralPath $msi) { Remove-Item -LiteralPath $msi -Force }

# --- 6. Run jpackage -----------------------------------------------------------
$jpArgs = @(
    '--type', 'msi',
    '--name', $AppName,
    '--app-version', $AppVersion,
    '--vendor', $Vendor,
    '--description', $Description,
    '--input', $inputDir,
    '--main-jar', $JarName,
    '--main-class', $MainClass,
    '--dest', $OutDir,
    '--win-shortcut',
    '--win-menu',
    '--win-menu-group', $AppName,
    '--win-dir-chooser',
    '--win-upgrade-uuid', $UpgradeUuid
)
if (Test-Path -LiteralPath $IconPath) { $jpArgs += @('--icon', $IconPath) }

Write-Host "Building $msi ..."
& $jpackage @jpArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

$size = [math]::Round((Get-Item -LiteralPath $msi).Length / 1MB, 1)
Write-Host "Done: $msi ($size MB)" -ForegroundColor Green