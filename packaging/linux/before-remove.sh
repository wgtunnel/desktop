#!/bin/bash
# electron-builder does not substitute template variables in beforeInstall/beforeRemove
# hooks (unlike afterInstall/afterRemove), so __APP_FSNAME__ is replaced at build time.
set +e
UNIT_NAME='__APP_FSNAME__-daemon.service'
if command -v systemctl >/dev/null 2>&1; then
  systemctl disable --now "$UNIT_NAME" >/dev/null 2>&1 || true
  systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
fi
exit 0
