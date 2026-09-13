#!/bin/bash
# Installs this extracted tarball to /opt/__APP_FSNAME__ and sets up the wgtunnel-daemon systemd
# service.
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run this with sudo." >&2
  exit 1
fi

APP_FSNAME="__APP_FSNAME__"
APP_ROOT="/opt/$APP_FSNAME"
UNIT_NAME="$APP_FSNAME-daemon.service"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$SCRIPT_DIR" != "$APP_ROOT" ]; then
  echo "Copying to $APP_ROOT..."
  systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
  mkdir -p "$APP_ROOT"
  cp -a "$SCRIPT_DIR"/. "$APP_ROOT"/
fi

ln -sf "$APP_ROOT/$APP_FSNAME" "/usr/local/bin/$APP_FSNAME"

UNIT_SRC="$APP_ROOT/$UNIT_NAME"
[ -f "$UNIT_SRC" ] || UNIT_SRC="$APP_ROOT/lib/$UNIT_NAME"
install -Dm644 "$UNIT_SRC" "/etc/systemd/system/$UNIT_NAME"
sed -i "s|^ExecStart=.*|ExecStart=$APP_ROOT/bin/wgtunnel-daemon|" "/etc/systemd/system/$UNIT_NAME"
sed -i "s|^WorkingDirectory=.*|WorkingDirectory=$APP_ROOT|" "/etc/systemd/system/$UNIT_NAME"

systemctl daemon-reload
systemctl enable --now "$UNIT_NAME"

cat <<EOF

Installed to $APP_ROOT.
Launch the app with '$APP_FSNAME', or find it in your app menu after logging out and back in.
Uninstall any time with: sudo $APP_ROOT/uninstall.sh
EOF
