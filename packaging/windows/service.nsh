!macro StopDaemon
  IfFileExists "$INSTDIR\service-wrapper.exe" 0 +3
    nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" stop --no-elevate'
    Pop $0
!macroend

; Stop the service before NSIS extracts files so updates can replace the daemon binary.
!macro customInit
  !insertmacro StopDaemon
!macroend

!macro customInstall
  DetailPrint "Installing WG Tunnel daemon service"
  IfFileExists "$INSTDIR\service-wrapper.exe" 0 no_winsw
  IfFileExists "$INSTDIR\wgtunnel-daemon.exe" 0 no_daemon
  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" install --no-elevate'
  Pop $0
  DetailPrint "WinSW install exit code: $0"
  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" start --no-elevate'
  Pop $0
  DetailPrint "WinSW start exit code: $0"
  Goto done
  no_winsw:
    DetailPrint "service-wrapper.exe missing from $INSTDIR"
    Goto done
  no_daemon:
    DetailPrint "wgtunnel-daemon.exe missing from $INSTDIR"
  done:
!macroend

!macro customUnInstall
  DetailPrint "Removing WG Tunnel daemon service"
  !insertmacro StopDaemon
  IfFileExists "$INSTDIR\service-wrapper.exe" 0 +3
    nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" uninstall --no-elevate'
    Pop $0
!macroend
