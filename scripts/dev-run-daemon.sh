#!/bin/bash
set -e

# Go to the real project root no matter where the script is called from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(dirname "$SCRIPT_DIR")"

export WGTUNNEL_VARIANT="${WGTUNNEL_VARIANT:-debug}"

echo "Building daemon as normal user (variant=$WGTUNNEL_VARIANT)..."
./gradlew :daemon:installDist

RUNTIME_DIR="/run/wgtunnel-${WGTUNNEL_VARIANT}"
CACHE_DIR="/var/lib/wgtunnel-${WGTUNNEL_VARIANT}"
if [ "$WGTUNNEL_VARIANT" = "release" ]; then
  RUNTIME_DIR="/run/wgtunnel"
  CACHE_DIR="/var/lib/wgtunnel"
fi

echo "Starting debug daemon with sudo (socket $RUNTIME_DIR/daemon.sock)..."
sudo mkdir -p "$RUNTIME_DIR" "$CACHE_DIR"
sudo JAVA_HOME="$JAVA_HOME" \
  WGTUNNEL_VARIANT="$WGTUNNEL_VARIANT" \
  ./daemon/build/install/daemon/bin/daemon "$@"
