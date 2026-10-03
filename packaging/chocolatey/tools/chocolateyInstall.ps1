$ErrorActionPreference = 'Stop'
$packageName = $env:ChocolateyPackageName

# Placeholders pinned by publish-chocolatey.yml. Chocolatey has no ARM64 concept, so arch is
# detected manually here. PROCESSOR_ARCHITECTURE lies under x64 emulation (Windows-on-ARM);
# PROCESSOR_ARCHITEW6432, when set, holds the real architecture.
$realArch = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
$isArm64 = $realArch -eq 'ARM64'

if ($isArm64) {
  $url = 'PLACEHOLDER_URL_ARM64'
  $checksum = 'PLACEHOLDER_CHECKSUM_ARM64'
} else {
  $url = 'PLACEHOLDER_URL_AMD64'
  $checksum = 'PLACEHOLDER_CHECKSUM_AMD64'
}

$packageArgs = @{
  packageName    = $packageName
  fileType       = 'exe'
  url64bit       = $url
  checksum64     = $checksum
  checksumType64 = 'sha256'
  silentArgs     = '/S'
  validExitCodes = @(0)
  softwareName   = 'WG Tunnel*'
}

Install-ChocolateyPackage @packageArgs
