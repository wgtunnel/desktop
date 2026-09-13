%global debug_package %{nil}
%global __brp_check_rpaths %{nil}

Name:           wgtunnel
Version:        2.0.0
Release:        1%{?dist}
Summary:        WireGuard and AmneziaWG VPN client with auto-tunneling, lockdown and proxying
License:        MIT
URL:            https://wgtunnel.com
ExclusiveArch:  x86_64

# Nucleus artifact: ${name}-${version}-linux-x86_64.rpm
Source0:        https://github.com/wgtunnel/desktop/releases/download/%{version}/wgtunnel-%{version}-linux-x86_64.rpm

BuildRequires:  cpio
BuildRequires:  rpm
BuildRequires:  systemd-rpm-macros

Requires:       systemd
Requires:       gtk3
Requires:       libsecret
Requires(post): systemd
Requires(preun): systemd
Requires(postun): systemd

%description
WireGuard and AmneziaWG VPN client with auto-tunneling, lockdown, and proxying.

This COPR package redistributes the Nucleus-built GitHub release RPM.

%prep

%install
mkdir -p %{buildroot}
rpm2cpio %{SOURCE0} | cpio -idmv -D %{buildroot}

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

%post
%systemd_post wgtunnel-daemon.service

%preun
%systemd_preun wgtunnel-daemon.service

%postun
%systemd_postun_with_restart wgtunnel-daemon.service

%files
/opt/wgtunnel
%{_bindir}/wgtunnel
%{_unitdir}/wgtunnel-daemon.service
%{_datadir}/applications/wgtunnel.desktop
%{_datadir}/icons/hicolor/*/apps/wgtunnel.png

%changelog
* Sun Sep 13 2026 Zane Schepke <support@wgtunnel.com> - 2.0.0-1
- Redistribute Nucleus GitHub RPM
