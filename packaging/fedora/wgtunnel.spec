%global debug_package %{nil}
%global __brp_check_rpaths %{nil}

# Version is a placeholder: CI rewrites it (and the %changelog placeholder
# entry below) from the release tag before every Copr build.
%global tag 0.0.0
Name:           wgtunnel
Version:        0.0.0
Release:        1%{?dist}
Summary:        WireGuard and AmneziaWG VPN client with auto-tunneling, a kill switch, and proxying
License:        MIT
URL:            https://wgtunnel.com
ExclusiveArch:  x86_64 aarch64

# RPM artifacts: ${name}-${version}-linux-{x86_64,aarch64}.rpm. Both are listed unconditionally
# so the SRPM carries both, and %install picks the one for the build arch.
Source0:        https://github.com/wgtunnel/desktop/releases/download/%{tag}/wgtunnel-%{version}-linux-x86_64.rpm
Source1:        https://github.com/wgtunnel/desktop/releases/download/%{tag}/wgtunnel-%{version}-linux-aarch64.rpm

BuildRequires:  cpio
BuildRequires:  rpm
BuildRequires:  systemd-rpm-macros

Recommends:     systemd
Requires:       gtk3
Requires:       (xdg-desktop-portal-gtk or xdg-desktop-portal-kde or xdg-desktop-portal-gnome)
Requires(post): systemd
Requires(preun): systemd
Requires(postun): systemd

%description
WireGuard and AmneziaWG VPN client with auto-tunneling, a kill switch, and proxying.

This COPR package redistributes the GitHub release RPM.

%prep

%install
mkdir -p %{buildroot}
%ifarch aarch64
rpm2cpio %{SOURCE1} | cpio -idmv -D %{buildroot}
%else
rpm2cpio %{SOURCE0} | cpio -idmv -D %{buildroot}
%endif

rm -rf %{buildroot}/usr/lib/.build-id

install -d %{buildroot}%{_bindir}
ln -sf /opt/wgtunnel/wgtunnel %{buildroot}%{_bindir}/wgtunnel

if [ -f %{buildroot}/opt/wgtunnel/wgtunnel-daemon.service ]; then
  install -D -m 644 %{buildroot}/opt/wgtunnel/wgtunnel-daemon.service \
    %{buildroot}%{_unitdir}/wgtunnel-daemon.service
else
  install -D -m 644 %{buildroot}/opt/wgtunnel/lib/wgtunnel-daemon.service \
    %{buildroot}%{_unitdir}/wgtunnel-daemon.service
fi
sed -i 's|^ExecStart=.*|ExecStart=/opt/wgtunnel/bin/wgtunnel-daemon|' \
  %{buildroot}%{_unitdir}/wgtunnel-daemon.service
sed -i 's|^WorkingDirectory=.*|WorkingDirectory=/opt/wgtunnel|' \
  %{buildroot}%{_unitdir}/wgtunnel-daemon.service

# Pulled in by the daemon unit, so it needs no enabling.
early=%{buildroot}/opt/wgtunnel/wgtunnel-early-boot.service
[ -f "$early" ] || early=%{buildroot}/opt/wgtunnel/lib/wgtunnel-early-boot.service
install -D -m 644 "$early" %{buildroot}%{_unitdir}/wgtunnel-early-boot.service
sed -i 's|^ExecStart=.*|ExecStart=/opt/wgtunnel/bin/wgtunnel-daemon --early-boot-firewall|' \
  %{buildroot}%{_unitdir}/wgtunnel-early-boot.service

%post
%systemd_post wgtunnel-daemon.service

%preun
%systemd_preun wgtunnel-daemon.service
# A stopped daemon leaves the kill switch enforced on purpose. Remove it on uninstall only,
# since on upgrade the new daemon takes it over.
if [ $1 -eq 0 ]; then
  /opt/wgtunnel/bin/wgtunnel-daemon --purge-firewall >/dev/null 2>&1 || :
fi

%postun
%systemd_postun_with_restart wgtunnel-daemon.service

%files
/opt/wgtunnel
%{_bindir}/wgtunnel
%{_unitdir}/wgtunnel-daemon.service
%{_unitdir}/wgtunnel-early-boot.service
%{_datadir}/applications/wgtunnel.desktop
%{_datadir}/icons/hicolor/*/apps/wgtunnel.png

%changelog
# CI rewrites this entry in place every run. It always reflects
# the version currently being built. It is NOT a running history; add real dated entries
# below it by hand when a change is worth documenting.
* PLACEHOLDER_DATE Zane Schepke <support@wgtunnel.com> - PLACEHOLDER_VERSION-1
- Update to PLACEHOLDER_VERSION

* Sun Sep 13 2026 Zane Schepke <support@wgtunnel.com> - 2.0.2-1
- Update to 2.0.2

* Sun Sep 13 2026 Zane Schepke <support@wgtunnel.com> - 2.0.0-1
- Redistribute Nucleus GitHub RPM
