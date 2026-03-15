# Linux Release Checklist

A simple checklist for Linux releases.

## AUR (Arch)

1. Update PKGBUILD: pkgver, sha256sums
2. Test locally
3. Update .SRCINFO
4. Commit & push to AUR

**Test**
```bash
makepkg -si
```

**Generate .SRCINFO**
```bash
makepkg --printsrcinfo > .SRCINFO
```

**Push to AUR**
```bash
git add PKGBUILD wgtunnel-bin.install .SRCINFO
git commit -m "upgpkg: wgtunnel-bin <pkgver>-<pkgrel>"
git push origin master
```

## COPR (Fedora)

1. Bump version in `wgtunnel.spec` to match the GitHub release.
2. Update `Source0` to the new release tarball url.
3. Commit changes.
4. Manually trigger COPR build.
