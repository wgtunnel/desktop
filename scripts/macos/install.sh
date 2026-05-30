#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== WG Tunnel Installer ===${NC}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="wgtunnel"
INSTALL_DIR="$HOME/.local/opt/$APP_NAME"
PLIST_NAME="com.zaneschepke.wgtunnel.daemon.plist"
PLIST_DEST="/Library/LaunchDaemons/$PLIST_NAME"
DAEMON_DATA_DIR="/Library/Application Support/wgtunnel"
LOG_DIR="/var/log/wgtunnel"

echo -e "Installing to: ${YELLOW}$INSTALL_DIR${NC}"
read -p "Press Enter to continue (Ctrl+C to cancel)..."

mkdir -p "$INSTALL_DIR"

echo "Copying files..."
rsync -a --delete \
  --exclude="install.sh" \
  --exclude="uninstall.sh" \
  --exclude="$PLIST_NAME" \
  "$SCRIPT_DIR/" "$INSTALL_DIR/"

chmod +x "$INSTALL_DIR/bin/"*

# Setup CLI symlinks
sudo mkdir -p /usr/local/bin
sudo ln -sf "$INSTALL_DIR/bin/wgtunnel" /usr/local/bin/wgtunnel
sudo ln -sf "$INSTALL_DIR/bin/wgtctl"   /usr/local/bin/wgtctl
echo -e "${GREEN}✓ CLI (wgtctl) and app (wgtunnel) added to PATH${NC}"

# Setup the system daemon
read -p "Install system-wide wgtunnel daemon (requires sudo)? [Y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
    echo "Preparing for daemon install..."

    # Stop and remove old daemon if it exists
    if sudo launchctl list com.zaneschepke.wgtunnel.daemon &>/dev/null; then
        sudo launchctl bootout system "$PLIST_DEST" 2>/dev/null || true
    fi
    sudo rm -f "$PLIST_DEST"

    # Create required directories
    sudo mkdir -p "$DAEMON_DATA_DIR"
    sudo chmod 755 "$DAEMON_DATA_DIR"
    sudo chown root:wheel "$DAEMON_DATA_DIR"

    sudo mkdir -p "$LOG_DIR"
    sudo chmod 755 "$LOG_DIR"
    sudo chown root:wheel "$LOG_DIR"

    # Install plist with correct daemon path
    sudo cp "$SCRIPT_DIR/$PLIST_NAME" "$PLIST_DEST"
    sudo sed -i '' "s|INSTALL_PATH|$INSTALL_DIR|g" "$PLIST_DEST"
    sudo chmod 644 "$PLIST_DEST"
    sudo chown root:wheel "$PLIST_DEST"

    sudo launchctl bootstrap system "$PLIST_DEST"
    echo -e "${GREEN}✓ Daemon successfully installed and started${NC}"
    sudo launchctl print system/com.zaneschepke.wgtunnel.daemon
else
    echo "Daemon skipped."
fi

echo -e "\n${GREEN}=== Installation complete! ===${NC}"
echo -e "GUI: ${BLUE}wgtunnel${NC}"
echo -e "CLI: ${BLUE}wgtctl${NC}"
echo -e "Update: extract new tarball → run ./install.sh again"
