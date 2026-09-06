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

Or from the AUR (`packaging/aur`, published as `wgtunnel-bin`):

```bash
yay -S wgtunnel-bin
```

Snap, Flatpak, and AppImage are not shipped. They cannot install a privileged systemd daemon (`CAP_NET_ADMIN`, lockdown, `/etc/resolv.conf`).

# Building from source

Toolchains are pinned in `.mise.toml` (Temurin 25, Node 22, Go 1.25, .NET 8). Gradle uses the same JDK 25 via `gradle/gradle-daemon-jvm.properties` and Foojay, so a missing JDK is downloaded even without mise.

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



