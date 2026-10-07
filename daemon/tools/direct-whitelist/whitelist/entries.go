// Package whitelist validates direct-access destinations without touching the OS.
package whitelist

import (
	"fmt"
	"golang.org/x/net/idna"
	"net/netip"
	"strings"
)

const MaxEntries = 256

type Entry struct {
	Prefix netip.Prefix
	Host   string
}

func Parse(text string) ([]Entry, error) {
	var entries []Entry
	seen := make(map[string]bool)
	for _, line := range strings.Split(text, "\n") {
		value := strings.TrimSpace(line)
		if value == "" {
			continue
		}
		var e Entry
		if p, err := netip.ParsePrefix(value); err == nil {
			if p.Addr().Is4In6() || p.Addr().Zone() != "" || p.Bits() == 0 {
				return nil, fmt.Errorf("invalid whitelist network: %s", value)
			}
			e.Prefix = p.Masked()
		} else if ip, err := netip.ParseAddr(value); err == nil {
			if ip.Zone() != "" {
				return nil, fmt.Errorf("scoped IPs are not supported: %s", value)
			}
			ip = ip.Unmap()
			e.Prefix = netip.PrefixFrom(ip, ip.BitLen())
		} else {
			host, err := idna.Lookup.ToASCII(strings.ToLower(strings.TrimSuffix(value, ".")))
			if err != nil || !validHost(host) {
				return nil, fmt.Errorf("invalid IP, CIDR or hostname: %s", value)
			}
			e.Host = host
		}
		key := e.Host
		if e.Prefix.IsValid() {
			key = e.Prefix.String()
		}
		if seen[key] {
			continue
		}
		seen[key] = true
		entries = append(entries, e)
		if len(entries) > MaxEntries {
			return nil, fmt.Errorf("at most %d entries are allowed", MaxEntries)
		}
	}
	return entries, nil
}

func validHost(host string) bool {
	if len(host) > 253 || !strings.Contains(host, ".") {
		return false
	}
	numeric := true
	for _, label := range strings.Split(host, ".") {
		if len(label) == 0 || len(label) > 63 || label[0] == '-' || label[len(label)-1] == '-' {
			return false
		}
		for _, c := range label {
			if !(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-') {
				return false
			}
			if c < '0' || c > '9' {
				numeric = false
			}
		}
	}
	return !numeric
}
