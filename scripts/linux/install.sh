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

echo -e "Installing to: ${YELLOW}$INSTALL_DIR${NC}"
read -p "Press Enter to continue (Ctrl+C to cancel)..."

mkdir -p "$INSTALL_DIR"

echo "Copying files..."
rsync -a --delete \
  --exclude="install.sh" \
  --exclude="uninstall.sh" \
  "$SCRIPT_DIR/" "$INSTALL_DIR/"

chmod +x "$INSTALL_DIR/bin/"*

# Setup CLI symlinks
mkdir -p "$HOME/.local/bin"
ln -sf "$INSTALL_DIR/bin/wgtunnel" "$HOME/.local/bin/wgtunnel"
ln -sf "$INSTALL_DIR/bin/wgtctl" "$HOME/.local/bin/wgtctl"
echo -e "${GREEN}✓ CLI (wgtctl) and app (wgtunnel) added to PATH${NC}"

# Add GUI desktop entry
mkdir -p "$HOME/.local/share/applications"
DESKTOP_SRC="$INSTALL_DIR/share/applications/com.zaneschepke.wireguardautotunnel.wgtunnel.desktop"
DESKTOP_DEST="$HOME/.local/share/applications/com.zaneschepke.wireguardautotunnel.wgtunnel.desktop"

cp "$DESKTOP_SRC" "$DESKTOP_DEST"
sed -i "s|^Exec=.*|Exec=$INSTALL_DIR/bin/wgtunnel|" "$DESKTOP_DEST"
sed -i "s|^Icon=.*|Icon=$INSTALL_DIR/share/icons/hicolor/256x256/apps/wgtunnel.png|" "$DESKTOP_DEST"

update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
echo -e "${GREEN}✓ Desktop entry installed${NC}"

# Add metainfo (app stores)
mkdir -p "$HOME/.local/share/metainfo"
cp -f "$INSTALL_DIR/share/metainfo/com.zaneschepke.wireguardautotunnel.wgtunnel.metainfo.xml" \
     "$HOME/.local/share/metainfo/" 2>/dev/null || true

# Setup the system daemon
read -p "Install system-wide wgtunnel daemon (requires sudo)? [Y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
    echo "Preparing for daemon install..."
    # Remove old, if it exists
    if systemctl is-active --quiet wgtunnel-daemon.service 2>/dev/null; then
        sudo systemctl stop wgtunnel-daemon.service
    fi
    sudo systemctl disable wgtunnel-daemon.service 2>/dev/null || true
    sudo rm -f /etc/systemd/system/wgtunnel-daemon.service
    SERVICE_SRC="$INSTALL_DIR/lib/systemd/system/wgtunnel-daemon.service"
    SERVICE_DEST="/etc/systemd/system/wgtunnel-daemon.service"
    sudo cp "$SERVICE_SRC" "$SERVICE_DEST"
    # Fix ExecStart and WorkingDirectory of the tarball
    sudo sed -i "s|^ExecStart=.*|ExecStart=$INSTALL_DIR/bin/daemon|" "$SERVICE_DEST"
    sudo sed -i "s|WorkingDirectory=.*|WorkingDirectory=$INSTALL_DIR|" "$SERVICE_DEST"

    # For SELinux systems
    if command -v chcon >/dev/null 2>&1; then
        sudo chcon -t bin_t "$INSTALL_DIR/bin/daemon" 2>/dev/null || true
    fi

    sudo systemctl daemon-reload
    sudo systemctl enable --now wgtunnel-daemon.service
    echo -e "${GREEN}✓ Daemon successfully installed and started${NC}"
    sudo systemctl status wgtunnel-daemon.service --no-pager -l
else
    echo "Daemon skipped."
fi

echo -e "\n${GREEN}=== Installation complete! ===${NC}"
echo -e "GUI: ${BLUE}wgtunnel${NC}"
echo -e "CLI: ${BLUE}wgtctl${NC}"
echo -e "Update: extract new tarball → run ./install.sh again"