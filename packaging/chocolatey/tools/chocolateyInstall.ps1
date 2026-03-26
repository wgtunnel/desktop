$ErrorActionPreference = 'Stop'
$packageName = $env:ChocolateyPackageName
$toolsDir    = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"

$url = "https://github.com/wgtunnel/desktop/releases/download/$env:ChocolateyPackageVersion/wgtunnel-$env:ChocolateyPackageVersion.x64.msix"

$msixFile = "$toolsDir\$packageName.msix"

Write-Host "Downloading wgtunnel..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $url -OutFile $msixFile

Write-Host "Installing wgtunnel..." -ForegroundColor Cyan

Add-AppxPackage -Path $msixFile