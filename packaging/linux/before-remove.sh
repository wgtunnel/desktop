#!/bin/bash
# electron-builder substitutes ${sanitizedProductName}.
set +e
UNIT_NAME='${sanitizedProductName}-daemon.service'
if command -v systemctl >/dev/null 2>&1; then
  systemctl disable --now "$UNIT_NAME" >/dev/null 2>&1 || true
  systemctl stop "$UNIT_NAME" >/dev/null 2>&1 || true
fi
exit 0
