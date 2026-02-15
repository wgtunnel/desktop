//go:build !android

package killswitch

import "C"
import (
	"net/netip"

	"github.com/wgtunnel/desktop/tunnel/shared"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/osfirewall/firewallmgr"
)

var logger = shared.NewLogger("KillSwitch")

//export setKillSwitch
func setKillSwitch(enabled C.int) C.int {
	fw, err := firewallmgr.Get()
	if err != nil {
		logger.Errorf("Failed to get firewall: %v", err)
		return C.int(-1)
	}

	if enabled == 1 {
		fw.SetPersist(true)
		err := fw.Enable()
		if err != nil {
			logger.Errorf("Failed to enable kill switch: %v", err)
			return C.int(-1)
		}
		logger.Verbosef("Kill switch enabled")
	} else {
		err := fw.Disable()
		if err != nil {
			logger.Errorf("Failed to disable kill switch: %v", err)
			return C.int(-1)
		}
		logger.Verbosef("Kill switch disabled")
	}
	return enabled
}

//export getKillSwitchStatus
func getKillSwitchStatus() C.int {
	fw, err := firewallmgr.Get()
	if err != nil {
		logger.Errorf("Failed to get firewall: %v", err)
		return C.int(0)
	}

	if fw.IsEnabled() && fw.IsPersistent() {
		return C.int(1)
	}
	return C.int(0)
}

//export setKillSwitchLanBypass
func setKillSwitchLanBypass(enabled C.int) C.int {
	fw, err := firewallmgr.Get()
	if err != nil {
		logger.Errorf("Failed to get firewall: %v", err)
		return C.int(-1)
	}

	if !fw.IsEnabled() {
		logger.Errorf("Firewall is not active")
		return C.int(-1)
	}

	if enabled == 1 {
		localPrefixes := []netip.Prefix{
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
		err := fw.AllowLocalNetworks(localPrefixes)
		if err != nil {
			logger.Errorf("Failed to enable kill switch: %v", err)
			return C.int(-1)
		}
		logger.Verbosef("Kill switch enabled")
	} else {
		fw.RemoveLocalNetworks()
	}
	return enabled
}

//export getKillSwitchLanBypassStatus
func getKillSwitchLanBypassStatus() C.int {
	fw, err := firewallmgr.Get()
	if err != nil {
		logger.Errorf("Failed to get firewall: %v", err)
		return C.int(0)
	}

	if fw.IsAllowLocalNetworksEnabled() {
		return C.int(1)
	}
	return C.int(0)
}
