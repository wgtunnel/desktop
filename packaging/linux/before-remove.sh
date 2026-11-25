#!/bin/bash
# electron-builder does not substitute template variables in beforeInstall/beforeRemove
# hooks (unlike afterInstall/afterRemove), so __APP_FSNAME__ is replaced at build time.
#
# Runs on removal only. rpm runs this after the new version's
# scripts, so acting then would stop the daemon the new version just started. pacman passes
# the old version on removal and has a separate upgrade hook.
set +e
# POSIX only, since the deb wraps this in a /bin/sh script.
case "${1:-}" in
  upgrade|failed-upgrade) exit 0 ;;
  '' | *[!0-9]* | 0) ;;
  *) exit 0 ;;
esac
UNIT_NAME='__APP_FSNAME__-daemon.service'
if command -v systemctl >/dev/null 2>&1; then
  systemctl disable --now "$UNIT_NAME" >/dev/null 2>&1 || true
  systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
fi
# A stopped daemon leaves the kill switch enforced on purpose. Nothing will take it over
# once the app is gone so we remove it.
'/opt/__APP_FSNAME__/bin/wgtunnel-daemon' --purge-firewall >/dev/null 2>&1 || true
exit 0
