#Requires -Version 5.1

$ErrorActionPreference = "Stop"

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error "This script needs an elevated (Run as Administrator) PowerShell window -- WFP/wintun require it."
    exit 1
}

if (-not $env:WGTUNNEL_VARIANT) {
    $env:WGTUNNEL_VARIANT = "debug"
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Split-Path -Parent $ScriptDir)

Write-Host "Building daemon (variant=$($env:WGTUNNEL_VARIANT))..."
$output = & .\gradlew.bat --no-watch-fs -q :daemon:printDevRunInfo
if ($LASTEXITCODE -ne 0) {
    Write-Error "printDevRunInfo failed"
    exit $LASTEXITCODE
}

$lines = $output | Where-Object { $_.Trim() -ne "" } | Select-Object -Last 3
if ($lines.Count -lt 3) {
    Write-Error "Expected 3 lines from printDevRunInfo (java path, jvm args, classpath), got $($lines.Count):`n$output"
    exit 1
}
$javaBin = $lines[0].Trim()
$jvmArgs = $lines[1].Trim() -split '\s+' | Where-Object { $_ -ne "" }
$classpath = $lines[2].Trim()

Write-Host "Starting debug daemon (elevated)..."
& $javaBin @jvmArgs -cp $classpath com.zaneschepke.wireguardautotunnel.daemon.MainKt @args
exit $LASTEXITCODE
