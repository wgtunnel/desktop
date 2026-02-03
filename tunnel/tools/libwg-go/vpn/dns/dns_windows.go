//go:build windows

package dns

import (
	"fmt"
	"net/netip"
	"os/exec"
	"strings"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"golang.org/x/net/nettest"
	"golang.org/x/sys/windows"
	"golang.zx2c4.com/wireguard/windows/tunnel/winipcfg"
)

func SetDNS(luid winipcfg.LUID, dns []netip.Addr, searchDomains []string, fullTunnel bool, logger *device.Logger) error {
	if fullTunnel {
		// se global search domains
		if len(searchDomains) > 0 {
			pscmd := "Set-DnsClientGlobalSetting -SuffixSearchList @('" + strings.Join(searchDomains, "','") + "')"
			cmd := exec.Command("powershell", "-Command", pscmd)
			if err := cmd.Run(); err != nil {
				logger.Errorf("set global search: %v", err)
			}
		}
	}

	// set DNS on interface
	v4dns, v6dns := []netip.Addr{}, []netip.Addr{}
	for _, d := range dns {
		if d.Is4() {
			v4dns = append(v4dns, d)
		} else if d.Is6() && nettest.SupportsIPv6() {
			v6dns = append(v6dns, d)
		}
	}

	// v4
	if len(v4dns) > 0 || len(searchDomains) > 0 {
		err := luid.SetDNS(windows.AF_INET, v4dns, searchDomains)
		if err != nil {
			return fmt.Errorf("set v4 dns: %w", err)
		}
	}

	// v6
	if len(v6dns) > 0 || len(searchDomains) > 0 {
		err := luid.SetDNS(windows.AF_INET6, v6dns, searchDomains)
		if err != nil {
			return fmt.Errorf("set v6 dns: %w", err)
		}
	}

	return nil
}

func RevertDNS(luid winipcfg.LUID, fullTunnel bool, originalSearchDomains []string, logger *device.Logger) error {
	if fullTunnel && originalSearchDomains != nil {
		// restore original global search
		pscmd := "Set-DnsClientGlobalSetting -SuffixSearchList @('" + strings.Join(originalSearchDomains, "','") + "')"
		cmd := exec.Command("powershell", "-Command", pscmd)
		if err := cmd.Run(); err != nil {
			logger.Errorf("restore global search: %v", err)
		}
		originalSearchDomains = nil
	} else if fullTunnel {
		// clear if no original
		pscmd := "Set-DnsClientGlobalSetting -SuffixSearchList @()"
		cmd := exec.Command("powershell", "-Command", pscmd)
		cmd.Run()
	}

	// clear DNS interface
	luid.FlushDNS(windows.AF_INET)
	luid.FlushDNS(windows.AF_INET6)

	return nil
}
