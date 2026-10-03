#!/bin/sh
# Adds the WG Tunnel apt repository and installs the stable package.
# https://apt.wgtunnel.com/install.sh
set -e

KEYRING=/usr/share/keyrings/wgtunnel-archive-keyring.gpg

wget -qO- https://apt.wgtunnel.com/wgtunnel-archive-keyring.asc | sudo gpg --dearmor -o "$KEYRING"
echo "deb [signed-by=$KEYRING] https://apt.wgtunnel.com stable main" | sudo tee /etc/apt/sources.list.d/wgtunnel.list > /dev/null
sudo apt update
sudo apt install -y wgtunnel
