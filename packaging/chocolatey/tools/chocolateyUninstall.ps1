$ErrorActionPreference = 'Stop'

Write-Host "Uninstalling wgtunnel..." -ForegroundColor Cyan

Get-AppxPackage -Name "Wgtunnel" -AllUsers | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue

Get-AppxPackage -Name "*Wgtunnel*" -AllUsers | Remove-AppxPackage -AllUsers -ErrorAction SilentlyContinue

Get-AppxProvisionedPackage -Online | Where-Object { $_.DisplayName -like "*wgtunnel*" -or $_.DisplayName -like "*WG Tunnel*" } | Remove-AppxProvisionedPackage -Online -ErrorAction SilentlyContinue