$ErrorActionPreference = 'Stop'
$packageName = $env:ChocolateyPackageName
$toolsDir    = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$version     = $env:ChocolateyPackageVersion

$release = Invoke-RestMethod -Uri "https://api.github.com/repos/wgtunnel/desktop/releases/tags/$version" -UserAgent 'wgtunnel-chocolatey'
$asset = $release.assets | Where-Object { $_.name -like '*.exe' } | Select-Object -First 1

if (-not $asset) {
  throw "No .exe asset found on GitHub release '$version' for wgtunnel/desktop"
}

$url64 = $asset.browser_download_url

$packageArgs = @{
  packageName    = $packageName
  fileType       = 'exe'
  url64bit       = $url64
  silentArgs     = '/S'
  validExitCodes = @(0)
  softwareName   = 'WG Tunnel*'
}

Install-ChocolateyPackage @packageArgs
