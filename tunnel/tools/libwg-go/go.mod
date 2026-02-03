module github.com/wgtunnel/desktop/tunnel

go 1.25.5

require (
	github.com/AdguardTeam/dnsproxy v0.78.2
	github.com/amnezia-vpn/amneziawg-go v0.2.16
	github.com/artem-russkikh/wireproxy-awg v1.0.12
	github.com/cenkalti/backoff/v5 v5.0.3
	github.com/godbus/dbus/v5 v5.1.1-0.20230522191255-76236955d466
	github.com/google/nftables v0.3.0
	github.com/vishvananda/netlink v1.3.1
	go4.org/netipx v0.0.0-20231129151722-fdeea329fbba
	golang.zx2c4.com/wireguard/windows v0.5.3
	inet.af/wf v0.0.0-20221017222439-36129f591884
	tailscale.com v1.94.1
)

require (
	github.com/AdguardTeam/golibs v0.35.7 // indirect
	github.com/BurntSushi/toml v1.6.0 // indirect
	github.com/ameshkov/dnscrypt/v2 v2.4.0 // indirect
	github.com/ameshkov/dnsstamps v1.0.3 // indirect
	github.com/google/go-cmp v0.7.0 // indirect
	github.com/mdlayher/netlink v1.8.0 // indirect
	github.com/mdlayher/socket v0.5.1 // indirect
	github.com/quic-go/qpack v0.6.0 // indirect
	github.com/quic-go/quic-go v0.59.0 // indirect
	github.com/vishvananda/netns v0.0.5 // indirect
	golang.org/x/exp v0.0.0-20260112195511-716be5621a96 // indirect
	golang.org/x/exp/typeparams v0.0.0-20260112195511-716be5621a96 // indirect
	golang.org/x/mod v0.32.0 // indirect
	golang.org/x/text v0.33.0 // indirect
	golang.org/x/tools v0.41.0 // indirect
	honnef.co/go/tools v0.7.0-0.dev.0.20251022135355-8273271481d0 // indirect
)

require (
	github.com/MakeNowJust/heredoc/v2 v2.0.1 // indirect
	github.com/go-ini/ini v1.67.0 // indirect
	github.com/google/btree v1.1.3 // indirect
	github.com/miekg/dns v1.1.72
	github.com/things-go/go-socks5 v0.1.0 // indirect
	golang.org/x/crypto v0.47.0 // indirect
	golang.org/x/net v0.49.0
	golang.org/x/sync v0.19.0 // indirect
	golang.org/x/sys v0.40.0
	golang.org/x/time v0.14.0 // indirect
	golang.zx2c4.com/wintun v0.0.0-20230126152724-0fa3db229ce2 // indirect
	gvisor.dev/gvisor v0.0.0-20250205023644-9414b50a5633 // indirect; ind
)

//replace github.com/amnezia-vpn/amneziawg-go => github.com/wgtunnel/amneziawg-go v0.0.0-20251225080458-6a08ea62878d

//replace github.com/artem-russkikh/wireproxy-awg => github.com/wgtunnel/wireproxy-awg v0.0.0-20251215030122-ffaf05dda47f

// local dev
replace github.com/amnezia-vpn/amneziawg-go => ../../../../amneziawg-go

//
replace github.com/artem-russkikh/wireproxy-awg => ../../../../wireproxy-awg
