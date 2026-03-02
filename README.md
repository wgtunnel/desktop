# WG Tunnel - Desktop

A WireGuard and AmneziaWG client for desktop.

## Supported Platforms
- macOS (Future)
- Windows
- Linux


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



