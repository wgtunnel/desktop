#!/bin/bash
# Stop the daemon before the payload is unpacked so binaries can be replaced.
# electron-builder does not substitute template variables in beforeInstall/beforeRemove
# hooks (unlike afterInstall/afterRemove), so __APP_FSNAME__ is replaced at build time.
#
# Record whether it was active before stopping it as after-install.sh's "restart if it was
# already running" check runs after this script has already stopped it
MARKER=/run/__APP_FSNAME__-daemon.was-active
if systemctl is-active --quiet '__APP_FSNAME__-daemon.service' 2>/dev/null; then
  touch "$MARKER" 2>/dev/null || true
else
  rm -f "$MARKER" 2>/dev/null || true
fi
systemctl stop '__APP_FSNAME__-daemon.service' 2>/dev/null || true
