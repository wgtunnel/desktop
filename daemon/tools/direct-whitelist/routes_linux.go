//go:build linux && !android

package main

import (
	"context"
	"errors"
	"fmt"
	"github.com/vishvananda/netlink"

	"github.com/VirusTI/wg-tunnel-desktop-whitelist/direct-whitelist/whitelist"
	"golang.org/x/sys/unix"
	"log"
	"net"
	"net/netip"
	"slices"
	"sync"
	"syscall"
	"time"
)

// Independent of per-tunnel table 52; these priorities/table are reserved.
const directTable = 53
const directPriority = 75
const directStopPriority = 76
const directDNSPriority = 60
const directDNSStopPriority = 61

var direct struct {
	sync.Mutex
	entries     []whitelist.Entry
	prefixes    []netip.Prefix
	started     bool
	initialized bool
}

var directResolver = &net.Resolver{PreferGo: true, Dial: func(ctx context.Context, network, _ string) (net.Conn, error) {
	d := net.Dialer{Control: func(_, _ string, raw syscall.RawConn) error {
		var markErr error
		err := raw.Control(func(fd uintptr) {
			markErr = unix.SetsockoptInt(int(fd), unix.SOL_SOCKET, unix.SO_MARK, dnsMark)
		})
		return errors.Join(err, markErr)
	}}
	return d.DialContext(ctx, network, "1.1.1.1:53")
}}

func hasHosts(entries []whitelist.Entry) bool {
	for _, e := range entries {
		if e.Host != "" {
			return true
		}
	}
	return false
}

func applyDirectWhitelist(text string) error {
	entries, err := whitelist.Parse(text)
	if err != nil {
		return err
	}
	direct.Lock()
	defer direct.Unlock()
	linuxFW := &firewall
	if !direct.initialized {
		if err := cleanupDirectRules(); err != nil {
			return err
		}
		direct.initialized = true
	}
	if err := syncPhysicalRoutes(); err != nil {
		return err
	}
	dnsRule := directDNSRule()
	dnsStop := directDNSStopRule()
	if hasHosts(entries) {
		if err := addDirectRule(dnsStop); err != nil {
			return err
		}
		if err := addDirectRule(dnsRule); err != nil {
			_ = netlink.RuleDel(dnsStop)
			return err
		}
	}
	if err := linuxFW.SetDirectWhitelist(direct.prefixes, hasHosts(entries)); err != nil {
		return err
	}
	prefixes, err := resolveEntries(entries)
	if err == nil {
		err = replaceDirectPrefixes(linuxFW, prefixes, hasHosts(entries))
	}
	if err != nil {
		_ = linuxFW.SetDirectWhitelist(direct.prefixes, hasHosts(direct.entries))
		if !hasHosts(direct.entries) {
			_ = netlink.RuleDel(dnsRule)
			_ = netlink.RuleDel(dnsStop)
		}
		return err
	}
	direct.entries = entries
	direct.prefixes = prefixes
	if !hasHosts(entries) {
		_ = netlink.RuleDel(dnsRule)
		_ = netlink.RuleDel(dnsStop)
	}
	if !direct.started {
		direct.started = true
		go refreshDirectWhitelist()
	}
	return nil
}

func resolveEntries(entries []whitelist.Entry) ([]netip.Prefix, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	var prefixes []netip.Prefix
	for _, e := range entries {
		if e.Prefix.IsValid() {
			prefixes = append(prefixes, e.Prefix)
			continue
		}
		addresses, err := directResolver.LookupNetIP(ctx, "ip", e.Host+".")
		if err != nil {
			return nil, fmt.Errorf("resolve %s: %w", e.Host, err)
		}
		if len(addresses) == 0 {
			return nil, fmt.Errorf("no addresses for %s", e.Host)
		}
		for _, a := range addresses {
			a = a.Unmap()
			prefixes = append(prefixes, netip.PrefixFrom(a, a.BitLen()))
		}
	}
	slices.SortFunc(prefixes, func(a, b netip.Prefix) int {
		if n := a.Addr().Compare(b.Addr()); n != 0 {
			return n
		}
		return a.Bits() - b.Bits()
	})
	return slices.Compact(prefixes), nil
}

// Refresh routes and hooks after network/backend transitions. DNS is refreshed every minute.
func refreshDirectWhitelist() {
	ticker := time.NewTicker(time.Second)
	defer ticker.Stop()
	ticks := 0
	for range ticker.C {
		direct.Lock()
		ticks++
		if len(direct.entries) > 0 {
			err := syncPhysicalRoutes()
			if ticks%60 == 0 {
				prefixes, resolveErr := resolveEntries(direct.entries)
				if resolveErr != nil {
					log.Printf("DNS refresh: %v", resolveErr)
					prefixes = nil
					for _, e := range direct.entries {
						if e.Prefix.IsValid() {
							prefixes = append(prefixes, e.Prefix)
						}
					}
				}
				if e := replaceDirectPrefixes(&firewall, prefixes, hasHosts(direct.entries)); e != nil {
					err = e
				} else {
					direct.prefixes = prefixes
				}
			}
			if e := firewall.reconcile(); e != nil {
				err = e
			}
			if err != nil {
				log.Printf("Whitelist refresh: %v", err)
			}
		}
		direct.Unlock()
	}
}

func directRules(prefix netip.Prefix) []*netlink.Rule {
	family := netlink.FAMILY_V4
	if prefix.Addr().Is6() {
		family = netlink.FAMILY_V6
	}
	dst := &net.IPNet{IP: net.IP(prefix.Addr().AsSlice()), Mask: net.CIDRMask(prefix.Bits(), prefix.Addr().BitLen())}
	r := netlink.NewRule()
	r.Family = family
	r.Priority = directPriority
	r.Table = directTable
	r.Dst = dst
	stop := netlink.NewRule()
	stop.Family = family
	stop.Priority = directStopPriority
	stop.Type = unix.RTN_UNREACHABLE
	stop.Table = 0
	stop.Dst = dst
	// An absent physical route must not fall back to a VPN table.
	return []*netlink.Rule{stop, r}
}

func directDNSRule() *netlink.Rule {
	r := netlink.NewRule()
	r.Family = netlink.FAMILY_V4
	r.Priority = directDNSPriority
	r.Table = directTable
	r.Mark = dnsMark
	return r
}

func directDNSStopRule() *netlink.Rule {
	r := directDNSRule()
	r.Priority = directDNSStopPriority
	r.Table = 0
	r.Type = unix.RTN_UNREACHABLE
	return r
}

func addDirectRule(r *netlink.Rule) error {
	err := netlink.RuleAdd(r)
	if errors.Is(err, unix.EEXIST) {
		return nil
	}
	return err
}

func replaceDirectPrefixes(fw *LinuxFirewall, prefixes []netip.Prefix, dns bool) error {
	var added []*netlink.Rule
	rollback := func() {
		for _, r := range added {
			_ = netlink.RuleDel(r)
		}
	}
	for _, p := range prefixes {
		if slices.Contains(direct.prefixes, p) {
			continue
		}
		for _, r := range directRules(p) {
			if err := addDirectRule(r); err != nil {
				rollback()
				return fmt.Errorf("add direct route for %s: %w", p, err)
			}
			added = append(added, r)
		}
	}
	// Firewall changes are atomic and precede removal of old routing permissions.
	if err := fw.SetDirectWhitelist(prefixes, dns); err != nil {
		rollback()
		return err
	}
	for _, p := range direct.prefixes {
		if slices.Contains(prefixes, p) {
			continue
		}
		for _, r := range directRules(p) {
			if err := netlink.RuleDel(r); err != nil && !errors.Is(err, unix.ENOENT) {
				// Firewall already revoked access. Log stale routes; retry cleanup on restart.
				log.Printf("Remove direct route: %v", err)
			}
		}
	}
	return nil
}

func cleanupDirectRules() error {
	for _, family := range []int{netlink.FAMILY_V4, netlink.FAMILY_V6} {
		rules, err := netlink.RuleList(family)
		if errors.Is(err, unix.EAFNOSUPPORT) {
			continue
		}
		if err != nil {
			return err
		}
		for _, r := range rules {
			owned := r.Priority == directPriority && r.Table == directTable || r.Priority == directStopPriority && r.Type == unix.RTN_UNREACHABLE || r.Priority == directDNSPriority && r.Mark == dnsMark && r.Table == directTable ||
				r.Priority == directDNSStopPriority && r.Mark == dnsMark && r.Type == unix.RTN_UNREACHABLE
			if owned {
				if err := netlink.RuleDel(&r); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

// Copy non-VPN main routes into a private table, including connected LAN routes.
// This avoids split-tunnel routes in main attracting whitelist traffic.
func syncPhysicalRoutes() error {
	main, err := netlink.RouteListFiltered(netlink.FAMILY_ALL, &netlink.Route{Table: unix.RT_TABLE_MAIN}, netlink.RT_FILTER_TABLE)
	if err != nil {
		return err
	}
	var desired []netlink.Route
	for _, r := range main {
		if r.Type != unix.RTN_UNICAST || r.LinkIndex == 0 {
			continue
		}
		link, err := netlink.LinkByIndex(r.LinkIndex)
		if err != nil {
			continue
		}
		if link.Type() == "tuntap" || link.Type() == "wireguard" {
			continue
		}
		r.Table = directTable
		desired = append(desired, r)
	}
	old, err := netlink.RouteListFiltered(netlink.FAMILY_ALL, &netlink.Route{Table: directTable}, netlink.RT_FILTER_TABLE)
	if err != nil {
		return err
	}
	for _, r := range desired {
		if err := netlink.RouteReplace(&r); err != nil {
			return err
		}
	}
	for _, r := range old {
		found := false
		for _, d := range desired {
			if r.Equal(d) {
				found = true
				break
			}
		}
		if !found {
			if err := netlink.RouteDel(&r); err != nil && !errors.Is(err, unix.ESRCH) {
				return err
			}
		}
	}
	return nil
}
