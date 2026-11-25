#!/bin/bash

set +e

# Runs on removal only. rpm runs this after the new version's scripts, so
# acting then would stop the daemon the new version just started and delete the unit.
# POSIX only, since the deb wraps this in a /bin/sh script.
case "${1:-}" in
  upgrade|failed-upgrade) exit 0 ;;
  '' | *[!0-9]* | 0) ;;
  *) exit 0 ;;
esac

UNIT_NAME='${executable}-daemon.service'
EARLY_UNIT_NAME='${executable}-early-boot.service'
# before-install.sh always overwrites this fresh before it's ever read, so a
# stale value here can't cause wrong behavior on a future install. Avoid leaving it behind
# indefinitely on a full uninstall.
rm -f '/run/${executable}-daemon.was-active'
if command -v systemctl >/dev/null 2>&1; then
  systemctl disable --now "$UNIT_NAME" >/dev/null 2>&1 || true
  systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
  rm -f "/lib/systemd/system/$UNIT_NAME" "/etc/systemd/system/$UNIT_NAME" \
    "/lib/systemd/system/$EARLY_UNIT_NAME" "/etc/systemd/system/$EARLY_UNIT_NAME"
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
