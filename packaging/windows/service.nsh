!macro StopDaemon
  IfFileExists "$INSTDIR\service-wrapper.exe" 0 +3
    DetailPrint "Stopping WG Tunnel daemon service"
    nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" stop --no-elevate'
    Pop $0
    DetailPrint "WinSW stop exit code: $0"
!macroend

!macro UninstallDaemonService
  IfFileExists "$INSTDIR\service-wrapper.exe" 0 +3
    DetailPrint "Removing existing WG Tunnel daemon service registration"
    nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" uninstall --no-elevate'
    Pop $0
    DetailPrint "WinSW uninstall exit code: $0"
!macroend

!macro customInit
  ; Real elevation check. UserInfo::GetAccountType only reports Administrators-
  ; group *membership*, not whether this process token is actually elevated --
  ; it silently passes on the common single-admin-account/default-UAC setup
  ; even when the installer itself isn't elevated. Use the UAC plugin instead,
  ; which is what actually reflects the running process's integrity level.
  ${IfNot} ${UAC_IsAdmin}
    !insertmacro UAC_RunElevated
    ${Switch} $0
      ${Case} 0
        ; Successfully relaunched elevated in a new process -- let that one
        ; continue; this (non-elevated) instance is done.
        Quit
      ${Case} 1223
        ; User declined the UAC prompt.
        MessageBox MB_ICONSTOP "Administrator rights are required to install the WG Tunnel daemon."
        SetErrorLevel 740 ; ERROR_ELEVATION_REQUIRED
        Quit
      ${Default}
        MessageBox MB_ICONSTOP "Unable to elevate (error $0). Administrator rights are required to install the WG Tunnel daemon."
        SetErrorLevel 740
        Quit
    ${EndSwitch}
  ${EndIf}

  ; Stop the currently-installed daemon (if any) *before* extraction, so the
  ; running service-wrapper.exe / wgtunnel-daemon.exe files aren't locked when
  ; the installer tries to overwrite them. This matters as much for upgrades
  ; as for fresh installs -- customInit runs on every install, not just first.
  !insertmacro StopDaemon
!macroend

!macro customInstall
  DetailPrint "Installing WG Tunnel daemon service"
  IfFileExists "$INSTDIR\service-wrapper.exe" 0 no_winsw
  IfFileExists "$INSTDIR\wgtunnel-daemon.exe" 0 no_daemon

  ; Defensive re-registration for full-tree updates: files have already been
  ; extracted by this point, so make sure nothing from the previous version
  ; is still registered against stale config/binaries before (re)installing.
  ; Don't rely on WinSW's "install" being idempotent across versions -- always
  ; uninstall + reinstall so a changed service-wrapper.xml, updated WinSW
  ; binary, or renamed dependency all take effect cleanly on every upgrade.
  !insertmacro StopDaemon
  !insertmacro UninstallDaemonService

  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" install --no-elevate'
  Pop $0
  DetailPrint "WinSW install exit code: $0"
  ${If} $0 != 0
    DetailPrint "WinSW install failed (exit $0); forcing uninstall and retrying once"
    !insertmacro UninstallDaemonService
    nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" install --no-elevate'
    Pop $0
    DetailPrint "WinSW install retry exit code: $0"
  ${EndIf}

  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" start --no-elevate'
  Pop $0
  DetailPrint "WinSW start exit code: $0"
  ${If} $0 != 0
    MessageBox MB_ICONEXCLAMATION|MB_OK "WG Tunnel daemon service failed to start (exit code $0). You may need to start it manually from services.msc, or reboot."
  ${EndIf}
  Goto done

  no_winsw:
    DetailPrint "service-wrapper.exe missing from $INSTDIR -- daemon service was not installed"
    Goto done
  no_daemon:
    DetailPrint "wgtunnel-daemon.exe missing from $INSTDIR -- daemon service was not installed"
  done:
!macroend

!macro customUnInstall
  DetailPrint "Removing WG Tunnel daemon service"
  !insertmacro StopDaemon
  !insertmacro UninstallDaemonService
!macroend