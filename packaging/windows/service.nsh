!macro customInstall
  DetailPrint "Installing WG Tunnel daemon service"
  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" install --no-elevate'
  Pop $0
  DetailPrint "WinSW install exit code: $0"
  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" start --no-elevate'
  Pop $0
  DetailPrint "WinSW start exit code: $0"
!macroend

!macro customUnInstall
  DetailPrint "Removing WG Tunnel daemon service"
  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" stop --no-elevate'
  Pop $0
  nsExec::ExecToLog '"$INSTDIR\service-wrapper.exe" uninstall --no-elevate'
  Pop $0
!macroend
