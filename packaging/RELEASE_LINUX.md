# Linux extra channels (AUR + COPR)

GitHub Releases are the source of truth. CI already uploads:

- `wgtunnel-<ver>-linux-x64.deb`
- `wgtunnel-<ver>-linux-x64.rpm`
- `wgtunnel-<ver>-linux-x64.pacman`
- `wgtunnel-<ver>-linux-x64.tar.gz`

AUR and COPR cannot host those files as-is. They clone a recipe and fetch the GitHub artifact.

Do this **after** the GitHub release exists (the wrappers download it).

## AUR (`wgtunnel-bin`)

One-time:

```bash
git clone ssh://aur@aur.archlinux.org/wgtunnel-bin.git
```

Each release:

```bash
cd wgtunnel-bin
# copy from this repo
cp /path/to/desktop/packaging/aur/PKGBUILD .
cp /path/to/desktop/packaging/aur/wgtunnel-bin.install .

# set pkgver to the GitHub tag, then fill sha256
sed -i 's/^pkgver=.*/pkgver=1.0.2/' PKGBUILD
updpkgsums
makepkg --printsrcinfo > .SRCINFO

git add PKGBUILD wgtunnel-bin.install .SRCINFO
git commit -m "upgpkg: wgtunnel-bin 1.0.2-1"
git push
```

Users: `yay -S wgtunnel-bin`

## COPR (Fedora)

One-time (creates the project and chroots):

```bash
copr-cli create wgtunnel \
  --chroot fedora-rawhide-x86_64 \
  --chroot fedora-42-x86_64 \
  --chroot fedora-41-x86_64
```

Each release, bump `Version:` in `packaging/fedora/wgtunnel.spec` to the GitHub tag, commit, then:

```bash
# from the desktop repo after the spec is on the default branch
copr-cli buildscm <fedora-user>/wgtunnel \
  --clone-url https://github.com/wgtunnel/desktop.git \
  --commit master \
  --spec packaging/fedora/wgtunnel.spec \
  --type git \
  --method make_srpm
```

Or locally:

```bash
spectool -g -R packaging/fedora/wgtunnel.spec
rpmbuild -bs packaging/fedora/wgtunnel.spec
copr-cli build wgtunnel ~/rpmbuild/SRPMS/wgtunnel-*.src.rpm
```

Users:

```bash
sudo dnf copr enable zaneschepke/wgtunnel
sudo dnf install wgtunnel
```
