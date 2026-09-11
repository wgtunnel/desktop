#!/bin/bash

set +e

APP_ROOT='/opt/${sanitizedProductName}'
UNIT_NAME='${sanitizedProductName}-daemon.service'
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
  if systemctl is-active --quiet "$UNIT_NAME" 2>/dev/null; then
    # Already running so we keep it in sync with a restart without breaking Arch convention
    systemctl restart "$UNIT_NAME" >/dev/null 2>&1 || true
  else
    cat <<EOF

=== WG Tunnel Installed ===

    sudo systemctl enable --now $UNIT_NAME

EOF
  fi
fi

exit 0
