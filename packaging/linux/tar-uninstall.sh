#!/bin/bash
# Reverses tar-install.sh: stops and removes the daemon service, the /usr/local/bin symlink,
# and the entire /opt/__APP_FSNAME__ install (including this script).
#
# Pass --purge to wipe all app data.
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "Run this with sudo." >&2
  exit 1
fi

purge=0
for arg in "$@"; do
  [ "$arg" = "--purge" ] && purge=1
done

APP_FSNAME="__APP_FSNAME__"
APP_ROOT="/opt/$APP_FSNAME"
UNIT_NAME="$APP_FSNAME-daemon.service"

systemctl disable --now "$UNIT_NAME" >/dev/null 2>&1 || true
systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
rm -f "/etc/systemd/system/$UNIT_NAME"
systemctl daemon-reload >/dev/null 2>&1 || true
systemctl reset-failed "$UNIT_NAME" >/dev/null 2>&1 || true

rm -f "/usr/local/bin/$APP_FSNAME"
rm -rf "$APP_ROOT"

if [ "$purge" -eq 1 ]; then
  echo "Removing saved tunnels, settings, and logs..."

  # Root-owned daemon state
  rm -rf "/var/lib/$APP_FSNAME" "/var/log/$APP_FSNAME" "/etc/$APP_FSNAME"

  # Per-user app data
  if [ -n "${SUDO_USER:-}" ]; then
    user_home="$(getent passwd "$SUDO_USER" | cut -d: -f6)"
    if [ -n "$user_home" ]; then
      rm -rf "$user_home/.local/share/$APP_FSNAME" "$user_home/.$APP_FSNAME"
    fi
  fi

  cat <<EOF
Note: this does not clear the saved secret in your OS keyring (service "wg_tunnel") - remove
that yourself with your keyring manager (Seahorse, KWalletManager, etc.) if you want it gone too.
EOF
elif [ -e "/var/lib/$APP_FSNAME" ]; then
  echo "Your saved tunnels, settings, and logs were left in place - re-run with --purge to remove them too."
fi

echo "WG Tunnel has been uninstalled."
