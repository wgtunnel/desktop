#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${RED}=== WG Tunnel Uninstaller ===${NC}"

INSTALL_DIR="$HOME/.local/opt/wgtunnel"
DATA_DIR="$HOME/Library/Application Support/WGTunnel"
PLIST_DEST="/Library/LaunchDaemons/com.zaneschepke.wgtunnel.daemon.plist"

echo -e "This will remove WG Tunnel from your system."
echo -e "Your tunnels and settings are stored in: ${YELLOW}$DATA_DIR${NC}"

read -p "Type 'yes' to proceed: " confirm
if [[ "$confirm" != "yes" ]]; then
    echo "Aborted."
    exit 0
fi

echo "Stopping daemon..."
if sudo launchctl list com.zaneschepke.wgtunnel.daemon &>/dev/null; then
    sudo launchctl bootout system "$PLIST_DEST" 2>/dev/null || true
fi
sudo rm -f "$PLIST_DEST"

# Remove daemon-related files
echo "Removing daemon state and IPC keys..."
sudo rm -rf "/Library/Application Support/wgtunnel" /var/log/wgtunnel /tmp/wgtunnel 2>/dev/null || true
rm -rf "$HOME/.wgtunnel" 2>/dev/null || true

# Remove CLI symlinks and app files
sudo rm -f /usr/local/bin/wgtunnel /usr/local/bin/wgtctl
rm -rf "$INSTALL_DIR"

echo -e "${GREEN}✓ WG Tunnel application removed${NC}"

# Optionally remove the DB and keyring entry
echo
read -p "Also permanently delete your tunnels, settings, and database? [Y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]] || [[ -z $REPLY ]]; then
    rm -rf "$DATA_DIR"

    # Attempt to remove DB encryption key from macOS Keychain
    security delete-generic-password -s "wg_tunnel" 2>/dev/null || true

    echo -e "${GREEN}✓ All personal data removed${NC}"
else
    echo -e "${YELLOW}Your tunnels and settings have been preserved${NC}"
fi

echo -e "\n${GREEN}=== WG Tunnel has been uninstalled ===${NC}"
echo "To reinstall: extract the new tarball and run ./install.sh"
