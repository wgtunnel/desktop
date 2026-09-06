$ErrorActionPreference = 'Stop'
$packageName = $env:ChocolateyPackageName
$toolsDir    = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$version     = $env:ChocolateyPackageVersion

$url64 = "https://github.com/wgtunnel/desktop/releases/download/$version/wgtunnel-$version-windows-x64-nsis.exe"

$packageArgs = @{
  packageName    = $packageName
  fileType       = 'exe'
  url64bit       = $url64
  silentArgs     = '/S'
  validExitCodes = @(0)
  softwareName   = 'WG Tunnel*'
}

Install-ChocolateyPackage @packageArgs
