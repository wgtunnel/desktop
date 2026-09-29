#!/bin/bash
set -euo pipefail

# Go to the real project root no matter where the script is called from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(dirname "$SCRIPT_DIR")"

export WGTUNNEL_VARIANT="${WGTUNNEL_VARIANT:-debug}"

echo "Building daemon as normal user (variant=$WGTUNNEL_VARIANT)..."
DEV_RUN_OUTPUT="$(./gradlew -q :daemon:printDevRunInfo | tail -n 3)"
JAVA_BIN="$(sed -n '1p' <<<"$DEV_RUN_OUTPUT")"
JVM_ARGS_LINE="$(sed -n '2p' <<<"$DEV_RUN_OUTPUT")"
CLASSPATH="$(sed -n '3p' <<<"$DEV_RUN_OUTPUT")"
read -r -a JVM_ARGS <<<"$JVM_ARGS_LINE"


FS_NAME="wgtunnel-${WGTUNNEL_VARIANT}"
if [ "$WGTUNNEL_VARIANT" = "release" ]; then
  FS_NAME="wgtunnel"
fi
RUNTIME_DIR="/tmp/${FS_NAME}"
CACHE_DIR="/Library/Application Support/${FS_NAME}"

echo "Starting debug daemon with sudo (socket $RUNTIME_DIR/daemon.sock)..."
sudo mkdir -p "$RUNTIME_DIR" "$CACHE_DIR"
sudo WGTUNNEL_VARIANT="$WGTUNNEL_VARIANT" \
  "$JAVA_BIN" "${JVM_ARGS[@]}" -cp "$CLASSPATH" \
  com.zaneschepke.wireguardautotunnel.daemon.MainKt "$@"
