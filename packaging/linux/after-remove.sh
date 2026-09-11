#!/bin/bash

set +e

UNIT_NAME='${sanitizedProductName}-daemon.service'
if command -v systemctl >/dev/null 2>&1; then
  systemctl disable --now "$UNIT_NAME" >/dev/null 2>&1 || true
  systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
  rm -f "/lib/systemd/system/$UNIT_NAME" "/etc/systemd/system/$UNIT_NAME"
  systemctl daemon-reload >/dev/null 2>&1 || true
  systemctl reset-failed "$UNIT_NAME" >/dev/null 2>&1 || true
fi

if type update-alternatives >/dev/null 2>&1; then
  update-alternatives --remove '${executable}' '/opt/${sanitizedProductName}/${executable}' >/dev/null 2>&1 || true
else
  rm -f '/usr/bin/${executable}'
fi

# apt purge
if [ "${1:-}" = "purge" ]; then
  APP_FSNAME='${sanitizedProductName}'
  rm -rf "/var/lib/$APP_FSNAME" "/var/log/$APP_FSNAME" "/etc/$APP_FSNAME"

  # Best-effort app data purge
  for home in /home/*; do
    [ -d "$home" ] || continue
    rm -rf "$home/.local/share/$APP_FSNAME" "$home/.$APP_FSNAME"
  done
fi

exit 0
