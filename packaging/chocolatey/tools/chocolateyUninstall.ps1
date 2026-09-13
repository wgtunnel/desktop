$ErrorActionPreference = 'Stop'

Write-Host "Uninstalling wgtunnel..." -ForegroundColor Cyan

$uninstaller = Join-Path $env:ProgramFiles 'wgtunnel\Uninstall wgtunnel.exe'
if (-not (Test-Path $uninstaller)) {
  $uninstaller = Join-Path ${env:ProgramFiles(x86)} 'wgtunnel\Uninstall wgtunnel.exe'
}

if (Test-Path $uninstaller) {
  Start-Process -FilePath $uninstaller -ArgumentList '/S' -Wait
} else {
  Write-Host "NSIS uninstaller not found; trying registry uninstall keys"
  Get-UninstallRegistryKey -SoftwareName 'WG Tunnel*' | ForEach-Object {
    if ($_.UninstallString) {
      Start-Process -FilePath $_.UninstallString.Trim('"') -ArgumentList '/S' -Wait
    }
  }
}
