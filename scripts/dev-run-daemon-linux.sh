#!/bin/bash
set -euo pipefail

# Go to the real project root no matter where the script is called from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(dirname "$SCRIPT_DIR")"

export WGTUNNEL_VARIANT="${WGTUNNEL_VARIANT:-debug}"

echo "Building daemon as normal user (variant=$WGTUNNEL_VARIANT)..."
mapfile -t DEV_RUN_INFO < <(./gradlew -q :daemon:printDevRunInfo | tail -n 3)
JAVA_BIN="${DEV_RUN_INFO[0]}"
read -r -a JVM_ARGS <<<"${DEV_RUN_INFO[1]}"
CLASSPATH="${DEV_RUN_INFO[2]}"

RUNTIME_DIR="/run/wgtunnel-${WGTUNNEL_VARIANT}"
CACHE_DIR="/var/lib/wgtunnel-${WGTUNNEL_VARIANT}"
if [ "$WGTUNNEL_VARIANT" = "release" ]; then
  RUNTIME_DIR="/run/wgtunnel"
  CACHE_DIR="/var/lib/wgtunnel"
fi

echo "Starting debug daemon with sudo (socket $RUNTIME_DIR/daemon.sock)..."
sudo mkdir -p "$RUNTIME_DIR" "$CACHE_DIR"
sudo WGTUNNEL_VARIANT="$WGTUNNEL_VARIANT" \
  "$JAVA_BIN" "${JVM_ARGS[@]}" -cp "$CLASSPATH" \
  com.zaneschepke.wireguardautotunnel.daemon.MainKt "$@"
