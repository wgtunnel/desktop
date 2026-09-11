# WG Tunnel - Desktop

A WireGuard and AmneziaWG client for desktop.

<div style="text-align: left;">
  <img src="assets/screenshots/main_screen.png" 
       alt="Main Screen Screenshot" 
       style="max-width: 100%; width: 800px; height: auto;" />
</div>

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

1. Download the Windows installer (`.exe`) from the latest release.
2. Run the installer. Accept the UAC prompt so the daemon can be installed as a system service.
3. Existing MSIX installs must be uninstalled first; this is a new package format.

## Linux

> **Note:** Only `systemd`-based Linux systems are currently supported. Also, the firewall must use `nftables` or `iptables` with the nft backend (`iptables-nft`).

### Debian / Ubuntu

1. Download the `.deb` file from the latest release.
2. From the directory where you downloaded the file:

```bash
sudo apt install ./wgtunnel*.deb
```

Packaged installs can also update in-app.

### Fedora / RHEL

From the GitHub release:

```bash
sudo rpm -Uvh wgtunnel*.rpm
```

Or from COPR (see `packaging/RELEASE_LINUX.md`):

```bash
sudo dnf copr enable <fedora-user>/wgtunnel
sudo dnf install wgtunnel
```

### Arch Linux

From the GitHub release:

```bash
sudo pacman -U wgtunnel*.pacman
```

Per Arch packaging guidelines, this does not enable or start the daemon for you. The app will tell
you (with the exact command to copy) if it can't reach the daemon, or if a stale daemon is still
running an older version after an upgrade.

Or from the AUR (`packaging/aur`, published as `wgtunnel-bin`), which handles both of those cases
automatically:

```bash
yay -S wgtunnel-bin
```

### Other distros (`.tar.gz`)

Use this only if none of the formats above fit your distro. The tarball bundles its own
`install.sh` / `uninstall.sh` (the same install location and systemd setup the `.deb`/`.rpm`/
`.pacman` packages use under the hood - see `packaging/linux/tar-install.sh` in the source repo if
you want to see exactly what they do before running as root):

```bash
tar -xzf wgtunnel*-linux-x64.tar.gz
cd wgtunnel*-linux-x64/
sudo ./install.sh
```

To update, just run `sudo ./install.sh` again from a newer tarball - it re-copies over the
existing install and restarts the daemon. To remove it entirely: `sudo /opt/wgtunnel/uninstall.sh`.

In-app updates aren't available for this install method.

### Removing saved data

Uninstalling normally leaves your saved tunnels, settings, and logs in place, the same way
`apt remove`/`dnf remove`/`pacman -R` leave other apps' data - so a reinstall picks up where you
left off. To wipe that too:

- **Debian/Ubuntu**: `sudo apt purge wgtunnel`
- **Fedora/RHEL, Arch (GitHub release or AUR)**: no built-in purge - remove it by hand after
  uninstalling:
  ```bash
  sudo rm -rf /var/lib/wgtunnel /etc/wgtunnel /var/log/wgtunnel
  rm -rf ~/.local/share/wgtunnel ~/.wgtunnel
  ```
- **Tarball**: `sudo /opt/wgtunnel/uninstall.sh --purge`

None of these clear the saved secret in your OS keyring (service `wg_tunnel`) - remove that
yourself with your keyring manager (Seahorse, KWalletManager, etc.) if you want it gone too.

> **Note**: Snap, Flatpak, and AppImage are not shipped. They cannot install a privileged systemd daemon (`CAP_NET_ADMIN`, lockdown, `/etc/resolv.conf`).

# Building from source

Toolchains are pinned in `.mise.toml`

```bash
# once per machine
curl https://mise.run | sh
mise trust
mise install

./gradlew :composeApp:packageGraalvmDeb      # also packageGraalvmRpm, packageGraalvmPacman, packageGraalvmTar
./gradlew :composeApp:packageGraalvmNsis     # Windows
```

If Gradle was previously running on another JDK, stop its daemon once: `./gradlew --stop`.

# Known issues

On Windows, switching the active network interface (like switching Ethernet → Wi-Fi or Wi-Fi → Ethernet) while a tunnel is active may cause the connection to drop.

**Workaround:** Restart the tunnel after changing network interfaces.



