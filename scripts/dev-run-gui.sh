#!/bin/bash
set -euo pipefail

# Go to the real project root no matter where the script is called from
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(dirname "$SCRIPT_DIR")"

export WGTUNNEL_VARIANT="${WGTUNNEL_VARIANT:-debug}"

./gradlew :composeApp:run "$@"
