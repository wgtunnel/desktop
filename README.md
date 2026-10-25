<h1 align="center">
WG Tunnel - Desktop
</h1>

<div align="center">

An FOSS desktop client for [WireGuard](https://www.wireguard.com/)
and [AmneziaWG](https://docs.amnezia.org/documentation/amnezia-wg/)
<br />
<br />
<a href="https://github.com/wgtunnel/desktop/issues/new?assignees=zaneschepke&labels=bug&title=%5BBUG%5D+-+Problem+with+app">Report a Bug</a>
·
<a href="https://github.com/wgtunnel/desktop/issues/new?assignees=zaneschepke&labels=enhancement&title=%5BFEATURE%5D+-+New+feature+request">Request a Feature</a>
·
<a href="https://github.com/wgtunnel/desktop/discussions">Ask a Question</a>

</div>

<br/>

<div align="center">

[<img  src="https://img.shields.io/badge/Telegram-26A5E4.svg?style=for-the-badge&logo=Telegram&logoColor=white">](https://t.me/wgtunnel)
[<img src="https://img.shields.io/badge/Matrix-000000.svg?style=for-the-badge&logo=Matrix&logoColor=white">](https://matrix.to/#/#wg-tunnel-space:matrix.org)
</div>

<details open="open">
<summary>Table of Contents</summary>

- [About](#about)
- [Supported Platforms](#supported-platforms)
- [Features](#features)
- [Deferred Endpoint Bootstrapping](#deferred-endpoint-bootstrapping)
- [Installation](#installation)
- [Development](#development)
- [Contributing](#contributing)

</details>

<div style="text-align: left;">

## About

WG Tunnel is an alternative desktop client for WireGuard and AmneziaWG that brings the
[Android app's](https://github.com/wgtunnel/android) feature set to the desktop.

</div>

<div style="text-align: left;">
  <img src="assets/screenshots/main_screen.png"
       alt="Main Screen Screenshot"
       style="max-width: 100%; width: 800px; height: auto;" />
</div>

## Supported Platforms

- Windows
- Linux
- macOS (Apple Silicon)

## Features

This app shares its tunnel/DNS/recovery engine with the
[Android app](https://github.com/wgtunnel/android#features), so most android feature apply here too:

- **Auto-Tunneling:** Automatically activate tunnels based on the machine's active network details.
- **Deferred Endpoint Bootstrapping:** Safely resolves endpoints and updates peers after the tunnel is up for better reliability and leak protection on startup.
- **Handshake Monitoring:** Real-time handshake monitoring for instant tunnel health feedback.
- **AmneziaWG Support:** Full support for AmneziaWG 2.0 through 3.1, providing robust censorship protection.
- **Split & Encrypted DNS:** Resolve DNS through the tunnel using plain DNS, DoT, or DoH, and optionally split by domain suffix (tunnel or system).
- **Local Proxy Mode:** Expose WireGuard tunnels over a local SOCKS5 or HTTP proxy to browsers or other apps.
- **Kill switch:** A system-wide, tunnel-independent, kill switch that blocks all traffic and is layerable on top of either mode.
- **Dynamic DNS Handling:** Automatically detect and update endpoints on server IP changes without requiring a restart.
- **IPv6 Endpoints:** Automatically upgrade to IPv6 endpoints or fall back to IPv4 based on network conditions without requiring a restart.

Desktop-specific:

- **Native Application:** Ships as a GraalVM native image (not a JVM/Electron style app) for fast startup and a low memory footprint.
- **System Service Daemon:** The tunnel runs as an independent system service, so it keeps running (and enables restore on boot features) even when the GUI is not.
- **Kill switch & Tunnel Restoration on Boot:** Reapplies the kill switch and reconnects the last active tunnel automatically after a reboot.
- **Encrypted At Rest:** Tunnel configs and other sensitive data are stored encrypted (AES-256-GCM) in the local database while the encryption key lives in the OS keychain/credential store.
- **Tunnel Management:** Import, export, editing, live statistics (including uptime and, in Local Proxy mode, the active proxy address/auth status), and sorting.
- **System Tray Integration:** Minimize to tray with a live status badge.
- **App Updater:** In-app updater and update notifications (only for `.deb`/`.rpm` and Windows installs).

## Deferred Endpoint Bootstrapping

Most WireGuard clients resolve peer endpoints before bringing the tunnel up. This has several drawbacks:
1. If resolution fails or is slow (a flaky network, a DNS hiccup, a blocked resolver) the tunnel
simply doesn't come up, and there's no protection until it does.
2. During that resolution window, you're leaking until resolution completes successfully.

WG Tunnel splits these apart and removes peer resolution from the critical path. The TUN interface, 
routes, and firewall rules come up first, prioritizing user protection. Peer endpoint resolution then 
happens separately, in the background, retrying indefinitely until it succeeds.

This has significant benefits:

- **Immediate protection:** The user is protected the moment the tunnel is toggled on, regardless of
  whether or when endpoint resolution succeeds. This dramatically reduces a leak window that other clients
  suffer from.
- **Reliability:** The tunnel never fails to come up because of a flaky network or
  resolution failures. The tunnel is already up and blocking traffic while DNS keeps retrying in the
  background until it connects.

## Installation

### Windows

> [!WARNING]
> Before installing v2.x.x and greater on Windows, it is required to uninstall the v1.x.x if you have it installed. The architecture
> of the app has changed significantly in the latest version and requires the old version to be removed.

> [!NOTE]
> Requires Windows 10 version 1803 (build 17134) or later for UDS support. The installer checks this and will refuse
> to install on an older build.

<details open>
<summary><b>Package Manager</b></summary>
</br>

Via [Chocolatey](https://community.chocolatey.org/packages/wgtunnel):

```powershell
choco install wgtunnel
```

Via [WinGet](https://github.com/microsoft/winget-pkgs/tree/master/manifests/w/WGTunnel/WGTunnel):

```powershell
winget install -e --id WGTunnel.WGTunnel
```

</details>

<details open>
<summary><b>Direct</b></summary>
</br>

1. Download the Windows installer (`.exe`) from the latest release.
2. Run the installer. Accept the Admin rights prompt (UAC) so the daemon can be installed as a system service.
3. Launch the app.

</details>

### Linux

> [!NOTE]
> The firewall must use `nftables` or `iptables` with the nft backend (`iptables-nft`). On `systemd`-based
> distros the daemon starts automatically at boot. On other init systems, it installs but needs to be
> started manually or integrated with your init system (see the note printed after install).

> [!Note]
> Direct installations from GitHub releases for `.deb` and `.rpm` packages can use the in-app updater while `.pacman` and tarball direct installs
> will require manual updates.

#### Debian / Ubuntu

<details open>
<summary><b>Package Manager</b></summary>
</br>

Via our [apt repository](https://apt.wgtunnel.com). Review the
script [here](https://raw.githubusercontent.com/wgtunnel/desktop/master/packaging/apt/install.sh) before running it:

```bash
wget -qO- https://apt.wgtunnel.com/install.sh | sh
```

</details>

<details open>
<summary><b>Direct</b></summary>
</br>

1. Download the `.deb` directly from the latest GitHub release.

```bash
sudo apt install ./wgtunnel*.deb
```

</details>

#### Fedora / RHEL

<details open>
<summary><b>Package Manager</b></summary>
</br>

Via [COPR](https://copr.fedorainfracloud.org/coprs/zaneschepke/wgtunnel/):

```bash
sudo dnf copr enable zaneschepke/wgtunnel
sudo dnf install wgtunnel
```

</details>

<details open>
<summary><b>Direct</b></summary>
</br>

1. Download the `.rpm` directly from the latest GitHub release.

```bash
sudo rpm -Uvh wgtunnel*.rpm
```

</details>

#### Arch Linux

> [!Note]
> Per Arch packaging guidelines, the installation does not enable or start the daemon for you. You will
> need to start the daemon yourself after install.

<details open>
<summary><b>Package Manager</b></summary>
</br>

Via the [AUR](https://aur.archlinux.org/packages/wgtunnel-bin):

```bash
yay -S wgtunnel-bin
```

</details>

<details open>
<summary><b>Direct</b></summary>
</br>

1. Download the `.pacman` directly from the latest GitHub release.

```bash
sudo pacman -U wgtunnel*.pacman
```

Then, enable and start the daemon:

```bash
sudo systemctl enable --now wgtunnel-daemon.service
```

</details>

#### Other distros (`.tar.gz`)

Use this only if none of the formats above fit your distro. The tarball bundles its own
`install.sh` / `uninstall.sh` (the same install location the `.deb`/`.rpm`/`.pacman` packages use
under the hood). On `systemd`-based systems it also enables and starts the daemon automatically.
On other init systems, the daemon is installed but needs to be started manually (the script prints
the exact command). See `packaging/linux/tar-install.sh` in the source repo if you want to see the
source script:

```bash
tar -xzf wgtunnel*-linux-x64.tar.gz
cd wgtunnel*-linux-x64/
sudo ./install.sh
```

To update, just run `sudo ./install.sh` again from a newer tarball. It copies over the
existing install and restarts the daemon. To remove it entirely: `sudo /opt/wgtunnel/uninstall.sh`.

#### Removing saved data

Uninstalling normally leaves your saved tunnels, settings, and logs in place, so a reinstall picks
up where you left off. To wipe everything:

- **Debian/Ubuntu**: `sudo apt purge wgtunnel`
- **Fedora/RHEL, Arch (GitHub release or AUR)**: there is no built-in purge so to remove after
  uninstalling:
  ```bash
  sudo rm -rf /var/lib/wgtunnel /etc/wgtunnel /var/log/wgtunnel
  rm -rf ~/.local/share/wgtunnel ~/.wgtunnel
  ```
- **Tarball**: `sudo /opt/wgtunnel/uninstall.sh --purge`

None of these clear the saved secret in your OS keyring (service `wg_tunnel`). You will need to
remove that yourself with your keyring manager.

> [!Note]
> Snap, Flatpak, and AppImages are not shipped and there is no plan currently to add them. They
> simply are not a good fit for the app due to their sandboxing limitations.

### macOS

> [!NOTE]
> Requires macOS 13 (Ventura) or later, on Apple Silicon. Intel Macs are not currently supported.

<details open>
<summary><b>Package Manager</b></summary>
</br>

Via our [Homebrew tap](https://github.com/wgtunnel/homebrew-wgtunnel):

```bash
brew tap wgtunnel/wgtunnel
brew trust wgtunnel/wgtunnel
brew install --cask wg-tunnel
```

</details>

<details open>
<summary><b>Direct</b></summary>
</br>

1. Download the `.dmg` directly from the latest release.
2. Open the `.dmg` and drag **WG Tunnel** into Applications.
3. Launch the app.
4. On first launch, macOS will ask you to approve the background service. The app prompts you
   with a button straight to **System Settings → General → Login Items & Extensions**.

</details>

To fully remove the background service before uninstalling, use **Settings → General → Remove
background service** in the app first. Apple doesn't provide the ability to automatically remove a daemon on app uninstall.

To wipe saved tunnels/settings/logs after uninstalling:

```bash
rm -rf ~/Library/Application\ Support/wgtunnel
sudo rm -rf "/Library/Application Support/wgtunnel"
```

This does not clear the saved secret in Keychain (service `wg_tunnel`). You will need to remove that 
yourself via Keychain Access if desired.

## Development

### Requirements

- Toolchains pinned in `.mise.toml` (JDK, Node, Go, .NET) - installed for you by `mise install` below.
- A C toolchain for the host platform.
- **MinGW-w64** (`x86_64-w64-mingw32-gcc`)

### Run locally

```bash
# once per machine
curl https://mise.run | sh
mise trust
mise install
```

Start the daemon - it needs elevated privileges to manage routes/firewall, so use the dev
script.

```bash
./scripts/dev-run-daemon-linux.sh
```

Then, in another terminal, run the GUI:

```bash
./gradlew :composeApp:run
```

## Contributing

Any contributions in the form of feedback, issues, code, or translations are welcome and much
appreciated!

If your PR requires [core](https://github.com/wgtunnel/core) changes, please link the associated PR.

### Translators

<!-- CROWDIN-CONTRIBUTORS-START -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/zaneschepke"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/16294570/medium/965e75d46c8e69124d9ad93b6648d70a.jpg" />
          <br />
          <sub><b>Zane Schepke</b></sub>
          <br />
          <sub><b>(zaneschepke)</b></sub></a>
        <br />
        <sub><b>249913 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/martinellimarco"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17936585/medium/3e4a64d969fa19dca9cda5134eb75ace.png" />
          <br />
          <sub><b>Marco Martinelli</b></sub>
          <br />
          <sub><b>(martinellimarco)</b></sub></a>
        <br />
        <sub><b>4792 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/Pida88"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17799913/medium/ac2cec66927300857724bef7bd432cd3.jpg" />
          <br />
          <sub><b>Petr Sedl</b></sub>
          <br />
          <sub><b>(Pida88)</b></sub></a>
        <br />
        <sub><b>2211 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/Matthaiks"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/13139078/medium/27126ff13e6785d239e4cf1d47016adc.png" />
          <br />
          <sub><b>Matthaiks</b></sub></a>
        <br />
        <sub><b>1992 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/Surfoo"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/12385147/medium/2d69c468ab233ce1d371a4323309190e_default.png" />
          <br />
          <sub><b>Surfoo</b></sub></a>
        <br />
        <sub><b>1984 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/solokot"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/12939368/medium/23417b511371f5884018368baa1de8f3.jpg" />
          <br />
          <sub><b>solokot</b></sub></a>
        <br />
        <sub><b>1254 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/EvanTechDev"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17786633/medium/75db2d8a253d39f149683c270378d8c0.jpeg" />
          <br />
          <sub><b>Evan Huang</b></sub>
          <br />
          <sub><b>(EvanTechDev)</b></sub></a>
        <br />
        <sub><b>1120 words</b></sub>
      </td>
    </tr>
    <tr>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/alis36"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17816977/medium/be84bd9763853d31c10c09f3708fb72b.jpeg" />
          <br />
          <sub><b>Ali Saeedi</b></sub>
          <br />
          <sub><b>(alis36)</b></sub></a>
        <br />
        <sub><b>1050 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/justcontributor"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/13442389/medium/a6e71d4e535abf3b6d5c954658a93c70.jpg" />
          <br />
          <sub><b>그냥기여자</b></sub>
          <br />
          <sub><b>(justcontributor)</b></sub></a>
        <br />
        <sub><b>965 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/LEDPixel"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17784119/medium/fb3350aedffaf9c73b3f9de283b92b3e_default.png" />
          <br />
          <sub><b>LEDPixel</b></sub></a>
        <br />
        <sub><b>950 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/cyanwolfg"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/15465504/medium/c294e2b66970ece817e0639c12319b48.png" />
          <br />
          <sub><b>CyanWolf</b></sub>
          <br />
          <sub><b>(cyanwolfg)</b></sub></a>
        <br />
        <sub><b>891 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/tehb29"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17950253/medium/73287160ada318e24dae684912e5f3d5.png" />
          <br />
          <sub><b>Den</b></sub>
          <br />
          <sub><b>(tehb29)</b></sub></a>
        <br />
        <sub><b>460 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/pixnet"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17785035/medium/ba0625fb3822f186f2a2892f443d5af9_default.png" />
          <br />
          <sub><b>pixnet</b></sub></a>
        <br />
        <sub><b>378 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/zlronron159"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/13014680/medium/54f9e03c69b4f779ba7ef0f6e2ea70c7.jpg" />
          <br />
          <sub><b>ron</b></sub>
          <br />
          <sub><b>(zlronron159)</b></sub></a>
        <br />
        <sub><b>320 words</b></sub>
      </td>
    </tr>
    <tr>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/ppac"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/14481388/medium/a640e3856ebc6794ff424b663627bf6f.png" />
          <br />
          <sub><b>Philip</b></sub>
          <br />
          <sub><b>(ppac)</b></sub></a>
        <br />
        <sub><b>273 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/YuryKomar"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17782179/medium/687a5b3ebd16654bef045ab89c24d237_default.png" />
          <br />
          <sub><b>YuryKomar</b></sub></a>
        <br />
        <sub><b>271 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/georgybog"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/14779108/medium/da357a1603284460ec743f3c8cb51ba5.png" />
          <br />
          <sub><b>georgybog</b></sub></a>
        <br />
        <sub><b>257 words</b></sub>
      </td>
      <td align="center" valign="top">
        <a href="https://crowdin.com/profile/darbrzozo"><img alt="logo" style="width: 100px" src="https://crowdin-static.cf-downloads.crowdin.com/avatar/17844297/medium/80791831ba4d2a563c6044832cd1a9e7.png" />
          <br />
          <sub><b>Dariusz</b></sub>
          <br />
          <sub><b>(darbrzozo)</b></sub></a>
        <br />
        <sub><b>125 words</b></sub>
      </td>
    </tr>
  </tbody>
</table>
<!-- CROWDIN-CONTRIBUTORS-END -->
