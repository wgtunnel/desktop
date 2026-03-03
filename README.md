# WG Tunnel - Desktop

A WireGuard and AmneziaWG client for desktop.

# Supported Platforms
- macOS (Planned)
- Windows
- Linux

# Features
- Support for WireGuard and AmneziaWG tunnel configurations
- Independent lockdown mode (kill switch)
- Lockdown and previous tunnel restoration on boot
- Tunnel runs as a system service (daemon) independent of the application GUI
- Encrypted storage of tunnel configs with system keychain integration
- Tunnel import, export, editing, live statistics, and sorting

# Installation

## Windows

> **Note:** Only Windows 11 and 10 patch `10.0.19041.0` and greater are supported.

1. Download the `.msix` file from latest release.
2. Launch the installer by double-clicking on the download.
3. Proceed through the installation prompts (will require relaunching the installer as administrator).

## Linux

> **Note:** Only `systemd`-based Linux systems are currently supported. Also, the firewall must use `nftables` or `iptables` with the nft backend (`iptables-nft`).

### Linux Tarball Installation (Recommended)

In future versions, these scripts will be bundled with the tarball. For now, this command is the simplest way to get up and running.

**Install**
```bash 
# Download and extract the tarball, download install script, and execute
tar -xzf wgtunnel-*.tar.gz && \
cd wgtunnel-*/ && \
curl -LO https://raw.githubusercontent.com/wgtunnel/desktop/master/scripts/linux/install.sh && \
chmod +x install.sh && \
./install.sh
```

**Uninstall** (Optional)
```bash
curl -LO https://raw.githubusercontent.com/wgtunnel/desktop/master/scripts/linux/uninstall.sh && \
chmod +x uninstall.sh && \
./uninstall.sh
```

## Known issues

On Windows, switching the active network interface (like switching Ethernet → Wi-Fi or Wi-Fi → Ethernet) while a tunnel is active may cause the connection to drop.

**Workaround:** Restart the tunnel after changing network interfaces.



