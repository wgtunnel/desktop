#!/bin/bash
# Concatenated after Nucleus's desktop-integration template.
# electron-builder substitutes ${sanitizedProductName} and ${executable}.

set +e

APP_ROOT='/opt/${sanitizedProductName}'
UNIT_NAME='${executable}-daemon.service'
EXECUTABLE='${executable}'

# Nucleus's own desktop-integration template (update-alternatives / symlink creation) is only
# concatenated into the real afterInstall slot, never into afterUpgrade. Pacman only runs
# pre_upgrade/post_upgrade (never pre_install/post_install) when replacing an installed
# package, so without this, /usr/bin/$EXECUTABLE is left dangling at the pre-upgrade path on
# upgrade. Harmless when this script runs as the real afterInstall.
BIN_LINK="/usr/bin/$EXECUTABLE"
if type update-alternatives >/dev/null 2>&1; then
  if [ -L "$BIN_LINK" ] && [ -e "$BIN_LINK" ] && [ "$(readlink "$BIN_LINK")" != "/etc/alternatives/$EXECUTABLE" ]; then
    rm -f "$BIN_LINK"
  fi
  update-alternatives --install "$BIN_LINK" "$EXECUTABLE" "$APP_ROOT/$EXECUTABLE" 100 ||
    ln -sf "$APP_ROOT/$EXECUTABLE" "$BIN_LINK"
else
  ln -sf "$APP_ROOT/$EXECUTABLE" "$BIN_LINK"
fi

UNIT_SRC="$APP_ROOT/wgtunnel-daemon.service"
if [ ! -f "$UNIT_SRC" ]; then
  UNIT_SRC="$APP_ROOT/lib/wgtunnel-daemon.service"
fi
UNIT_DST="/lib/systemd/system/$UNIT_NAME"

if [ -f "$UNIT_SRC" ]; then
  install -D -m 644 "$UNIT_SRC" "$UNIT_DST"
  sed -i "s|^ExecStart=.*|ExecStart=$APP_ROOT/bin/wgtunnel-daemon|" "$UNIT_DST"
  sed -i "s|^WorkingDirectory=.*|WorkingDirectory=$APP_ROOT|" "$UNIT_DST"
fi

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload >/dev/null 2>&1 || true
  if command -v pacman >/dev/null 2>&1; then
    # Per Arch packaging guidelines, don't auto-enable/start services on install.
    #
    # before-install.sh already stopped the daemon (if it was running) before this ran, so an
    # is-active check here would always read false so we use the marker it left behind instead.
    MARKER="/run/$EXECUTABLE-daemon.was-active"
    if [ -f "$MARKER" ]; then
      rm -f "$MARKER"
      # Already running before this install/upgrade, so keep it in sync with a restart without
      # breaking Arch convention.
      systemctl restart "$UNIT_NAME" >/dev/null 2>&1 || true
    else
      cat <<EOF

=== WG Tunnel Installed ===

    sudo systemctl enable --now $UNIT_NAME

EOF
    fi
  else
    systemctl enable "$UNIT_NAME" >/dev/null 2>&1 || true
    # `restart` both starts a stopped unit and correctly bounces an already-running one, so it's
    # correct for fresh installs and upgrades alike.
    systemctl restart "$UNIT_NAME" >/dev/null 2>&1 || true
  fi
fi

exit 0
