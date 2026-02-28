#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${RED}=== WG Tunnel Uninstaller ===${NC}"

INSTALL_DIR="$HOME/.local/opt/wgtunnel"
DATA_DIR="$HOME/.local/share/wgtunnel"

echo -e "This will remove WG Tunnel from your system."
echo -e "Your tunnels and settings are stored in: ${YELLOW}$DATA_DIR${NC}"

read -p "Type 'yes' to proceed: " confirm
if [[ "$confirm" != "yes" ]]; then
    echo "Aborted."
    exit 0
fi

echo "Stopping daemon..."
if systemctl is-active --quiet wgtunnel-daemon.service 2>/dev/null; then
    sudo systemctl stop wgtunnel-daemon.service
fi
sudo systemctl disable wgtunnel-daemon.service 2>/dev/null || true
sudo rm -f /etc/systemd/system/wgtunnel-daemon.service
sudo systemctl daemon-reload

# Remove daemon-related files
echo "Removing daemon state and IPC keys..."
sudo rm -rf /var/lib/wgtunnel /var/log/wgtunnel /run/wgtunnel 2>/dev/null || true
rm -rf "$HOME/.wgtunnel" 2>/dev/null || true

# Remove desktop entry, CLI link, and app bundle
rm -f "$HOME/.local/share/applications/com.zaneschepke.wireguardautotunnel.wgtunnel.desktop"
rm -f "$HOME/.local/share/metainfo/com.zaneschepke.wireguardautotunnel.wgtunnel.metainfo.xml"
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
rm -f "$HOME/.local/bin/wgtunnel" "$HOME/.local/bin/wgtctl"
rm -rf "$INSTALL_DIR"

echo -e "${GREEN}✓ WG Tunnel application removed${NC}"

# Optionally remove the DB and keyring entry
echo
read -p "Also permanently delete your tunnels, settings, and database? [Y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
    rm -rf "$DATA_DIR"

    # Attempt to remove DB encryption key from keyring
    if command -v secret-tool >/dev/null 2>&1; then
        secret-tool clear service wg_tunnel 2>/dev/null || true
        secret-tool clear application wg_tunnel 2>/dev/null || true
        secret-tool clear "wg_tunnel" "DB_SECRET_KEY" 2>/dev/null || true
    fi

    echo -e "${GREEN}✓ All personal data removed${NC}"
else
    echo -e "${YELLOW}Your tunnels and settings have been preserved${NC}"
fi

echo -e "\n${GREEN}=== WG Tunnel has been uninstalled ===${NC}"
echo "To reinstall: extract the new tarball and run ./install.sh"