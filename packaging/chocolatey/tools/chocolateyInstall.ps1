$ErrorActionPreference = 'Stop'
$packageName = $env:ChocolateyPackageName

# URL and checksum are placeholders: publish-chocolatey.yml pins both to the exact release
# asset at packaging time.
$packageArgs = @{
  packageName    = $packageName
  fileType       = 'exe'
  url64bit       = 'PLACEHOLDER_URL'
  checksum64     = 'PLACEHOLDER_CHECKSUM'
  checksumType64 = 'sha256'
  silentArgs     = '/S'
  validExitCodes = @(0)
  softwareName   = 'WG Tunnel*'
}

Install-ChocolateyPackage @packageArgs
