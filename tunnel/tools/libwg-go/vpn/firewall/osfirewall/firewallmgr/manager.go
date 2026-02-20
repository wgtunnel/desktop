package firewallmgr

import (
	"net/netip"
	"sync"

	"github.com/wgtunnel/desktop/tunnel/shared"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/osfirewall"
)

var (
	instance firewall.Firewall
	once     sync.Once
	initErr  error
)

func Get() (firewall.Firewall, error) {
	once.Do(func() {
		instance, initErr = osfirewall.New(
			shared.NewLogger("Firewall"),
		)
	})

	return instance, initErr
}

func GetLocalAddresses() []netip.Prefix {
	return []netip.Prefix{
		// IPv4 Private Ranges (RFC 1918)
		netip.MustParsePrefix("10.0.0.0/8"),
		netip.MustParsePrefix("172.16.0.0/12"),
		netip.MustParsePrefix("192.168.0.0/16"),

		// IPv4 Link-Local (APIPA)
		netip.MustParsePrefix("169.254.0.0/16"),

		// IPv4 Loopback
		netip.MustParsePrefix("127.0.0.0/8"),

		// IPv4 Multicast (for local discovery, e.g., mDNS)
		netip.MustParsePrefix("224.0.0.0/4"),

		// IPv6 Unique Local Addresses (ULA, RFC 4193)
		netip.MustParsePrefix("fc00::/7"),

		// IPv6 Link-Local (RFC 4291)
		netip.MustParsePrefix("fe80::/10"),

		// IPv6 Loopback
		netip.MustParsePrefix("::1/128"),

		// IPv6 Multicast (for local discovery)
		netip.MustParsePrefix("ff00::/8"),
	}
}
