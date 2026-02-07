// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 WG Tunnel.
// Adapted from Tailscale

//go:build windows

package osrouter

import (
	"errors"
	"fmt"
	"net/netip"
	"os/exec"
	"slices"
	"sort"
	"strings"
	"syscall"

	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
	"github.com/wgtunnel/desktop/tunnel/vpn/dns"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/firewall/osfirewall"
	"github.com/wgtunnel/desktop/tunnel/vpn/router"
	"go4.org/netipx"
	"golang.org/x/net/nettest"
	"golang.org/x/sys/windows"
	"golang.zx2c4.com/wireguard/windows/tunnel/winipcfg"
)

type windowsRouter struct {
	iface                 string
	fw                    *osfirewall.WindowsFirewall
	logger                *device.Logger
	prevConfig            *router.Config
	v6Available           bool
	nativeTun             *tun.NativeTun
	luid                  winipcfg.LUID
	rawLuid               uint64
	originalSearchDomains []string
}

func New(iface string, fw firewall.Firewall, tunnel tun.Device, logger *device.Logger) (router.Router, error) {
	nativeTun := tunnel.(*tun.NativeTun)
	// get windows interface id
	rawLuid := nativeTun.LUID()
	return &windowsRouter{
		iface:       iface,
		fw:          fw.(*osfirewall.WindowsFirewall),
		logger:      logger,
		v6Available: nettest.SupportsIPv6(),
		nativeTun:   nativeTun,
		rawLuid:     rawLuid,
		luid:        winipcfg.LUID(rawLuid),
	}, nil
}

func (r *windowsRouter) Set(c *router.Config) error {
	newC := c
	if newC == nil {
		newC = &router.Config{}
	}
	prevC := r.prevConfig
	if prevC == nil {
		prevC = &router.Config{}
	}

	if newC.Equal(prevC) {
		r.logger.Verbosef("Config unchanged, skipping")
		return nil
	}

	err := r.configureInterface(newC)
	if err != nil {
		r.logger.Errorf("ConfigureInterface: %v", err)
		return err
	}

	// dns
	isPrevFull := prevC.HasAnyDefaultRoute()
	isNewFull := newC.HasAnyDefaultRoute()
	if !slices.Equal(newC.DNS, prevC.DNS) || !slices.Equal(newC.SearchDomains, prevC.SearchDomains) || isNewFull != isPrevFull {
		if isNewFull && r.originalSearchDomains == nil {
			var err error
			r.originalSearchDomains, err = r.getGlobalSearchDomains()
			if err != nil {
				r.logger.Errorf("Failed to get original search domains: %v", err)
			}
		}
		if err := dns.SetDNS(r.luid, newC.DNS, newC.SearchDomains, isNewFull, r.logger); err != nil {
			return err
		}
	}

	if err := r.syncFirewallState(newC, isNewFull); err != nil {
		return err
	}

	if err := flushCaches(); err != nil {
		r.logger.Errorf("flush dns: %v", err)
	}

	r.prevConfig = newC.Clone()
	return nil
}

func (r *windowsRouter) syncFirewallState(newC *router.Config, requiresKS bool) error {

	if !requiresKS && !r.fw.IsEnabled() {
		// not full tun and independent ks is not enabled, do nothing
		return nil
	} else if newC.Equal(&router.Config{}) && r.fw.IsEnabled() {
		// tunnel down: cleanup
		if r.fw.IsPersistent() {
			if err := r.fw.RemoveTunnelRules(); err != nil {
				return fmt.Errorf("remove tunnel bypasses: %w", err)
			}
		} else {
			if err := r.fw.Disable(); err != nil {
				return fmt.Errorf("disable firewall: %w", err)
			}
		}
		return nil
	}

	// enable kill switch for full tun when it isn't already enabled independently
	if requiresKS && !r.fw.IsEnabled() {
		// set persist to false as this kill switch is only required for this tun
		r.fw.SetPersist(false)
		if err := r.fw.Enable(); err != nil {
			return fmt.Errorf("enable firewall: %w", err)
		}
	}

	// If kill switch is active (independent or just enabled), always add tunnel bypasses
	if r.fw.IsEnabled() {
		if err := r.fw.BypassTunnel(r.rawLuid, newC.ListenPort); err != nil {
			return fmt.Errorf("add firewall bypasses: %w", err)
		}
	}

	return nil
}

// getBit returns the value of the i-th bit
func getBit(addr netip.Addr, i int) bool {
	if i < 0 || i >= addr.BitLen() {
		return false
	}
	if addr.Is4() {
		b := addr.As4()
		byteIdx := i / 8
		bitIdx := 7 - (i % 8) // MSB first
		return (b[byteIdx] & (1 << bitIdx)) != 0
	}
	b := addr.As16()
	byteIdx := i / 8
	bitIdx := 7 - (i % 8)
	return (b[byteIdx] & (1 << bitIdx)) != 0
}

// setBit returns a new Addr with the i-th bit set to value
func setBit(addr netip.Addr, i int, value bool) netip.Addr {
	if addr.Is4() {
		b := addr.As4()
		byteIdx := i / 8
		bitIdx := 7 - (i % 8)
		if value {
			b[byteIdx] |= 1 << bitIdx
		} else {
			b[byteIdx] &^= 1 << bitIdx
		}
		return netip.AddrFrom4(b)
	}
	b := addr.As16()
	byteIdx := i / 8
	bitIdx := 7 - (i % 8)
	if value {
		b[byteIdx] |= 1 << bitIdx
	} else {
		b[byteIdx] &^= 1 << bitIdx
	}
	return netip.AddrFrom16(b)
}

// flipBit returns a new Addr with the i-th bit flipped
func flipBit(addr netip.Addr, i int) netip.Addr {
	return setBit(addr, i, !getBit(addr, i))
}

func (r *windowsRouter) Close() error {
	if r.prevConfig != nil {
		dns.RevertDNS(r.luid, r.prevConfig.HasAnyDefaultRoute(), r.originalSearchDomains, r.logger)
	}

	r.Set(nil)

	r.logger.Verbosef("Router closed")
	return nil
}

// configureInterface uses the split route specificity approach to prevent routing loops
func (r *windowsRouter) configureInterface(cfg *router.Config) error {
	iface, err := interfaceFromLUID(r.luid, winipcfg.GAAFlagIncludeAllInterfaces)
	if err != nil {
		return fmt.Errorf("getting interface: %v", err)
	}

	_, err = r.setPrivateNetwork()
	if err != nil {
		r.logger.Verbosef("**WARNING** failed to set private network: %v", err)
	}

	ipif4, err := r.luid.IPInterface(windows.AF_INET)
	if err != nil && !errors.Is(err, windows.ERROR_NOT_FOUND) {
		return fmt.Errorf("getting AF_INET interface: %v", err)
	}
	ipif6, err := r.luid.IPInterface(windows.AF_INET6)
	if err != nil && !errors.Is(err, windows.ERROR_NOT_FOUND) {
		return fmt.Errorf("getting AF_INET6 interface: %v", err)
	}

	// Set up local tunnel addresses and gateways
	var localAddr4, localAddr6 netip.Addr
	var gatewayAddr4, gatewayAddr6 netip.Addr
	addresses := make([]netip.Prefix, 0, len(cfg.TunnelAddrs))
	for _, addr := range cfg.TunnelAddrs {
		if (addr.Addr().Is4() && ipif4 == nil) || (addr.Addr().Is6() && ipif6 == nil) {
			continue
		}
		addresses = append(addresses, addr)
		if addr.Addr().Is4() && !gatewayAddr4.IsValid() {
			localAddr4 = addr.Addr()
			gatewayAddr4 = netip.MustParseAddr("192.0.2.1")
		} else if addr.Addr().Is6() && !gatewayAddr6.IsValid() {
			localAddr6 = addr.Addr()
			gatewayAddr6 = netip.MustParseAddr("fc00::1")
		}
	}

	var routes []*routeData
	foundDefault4 := false
	foundDefault6 := false

	for _, route := range cfg.Routes {
		if (route.Addr().Is4() && ipif4 == nil) || (route.Addr().Is6() && ipif6 == nil) {
			continue
		}

		// Initialize IPv6 gateway if needed
		if route.Addr().Is6() && !gatewayAddr6.IsValid() {
			ip := netip.MustParseAddr("fc00::dead:beef")
			addresses = append(addresses, netip.PrefixFrom(ip, ip.BitLen()))
			gatewayAddr6 = ip
		}

		var gateway, localAddr netip.Addr
		if route.Addr().Is4() {
			localAddr = localAddr4
			gateway = gatewayAddr4
		} else if route.Addr().Is6() {
			localAddr = localAddr6
			gateway = gatewayAddr6
		}

		// split route for higher specificity over default route
		if route.Bits() == 0 {
			var splits []netip.Prefix
			if route.Addr().Is4() {
				splits = []netip.Prefix{
					netip.MustParsePrefix("0.0.0.0/1"),
					netip.MustParsePrefix("128.0.0.0/1"),
				}
				foundDefault4 = true
			} else {
				splits = []netip.Prefix{
					netip.MustParsePrefix("::/1"),
					netip.MustParsePrefix("8000::/1"),
				}
				foundDefault6 = true
			}

			for _, p := range splits {
				routes = append(routes, &routeData{
					RouteData: winipcfg.RouteData{
						Destination: p,
						NextHop:     gateway,
						Metric:      0,
					},
				})
			}
			continue
		}

		// non-default routes
		if route.Addr().Unmap() == localAddr {
			continue
		}
		if route.IsSingleIP() {
			gateway = localAddr
		}

		routes = append(routes, &routeData{
			RouteData: winipcfg.RouteData{
				Destination: route,
				NextHop:     gateway,
				Metric:      0,
			},
		})
	}

	err = syncAddresses(iface, addresses)
	if err != nil {
		return fmt.Errorf("syncAddresses: %v", err)
	}

	slices.SortFunc(routes, (*routeData).Compare)

	var deduplicatedRoutes []*routeData
	for i := 0; i < len(routes); i++ {
		if i > 0 && routes[i].Destination == routes[i-1].Destination {
			continue
		}
		deduplicatedRoutes = append(deduplicatedRoutes, routes[i])
	}

	iface, err = interfaceFromLUID(r.luid, winipcfg.GAAFlagIncludeAllInterfaces)
	if err != nil {
		return fmt.Errorf("getting interface after syncAddresses: %v", err)
	}

	var errAcc error
	err = syncRoutes(iface, deduplicatedRoutes, cfg.TunnelAddrs)
	if err != nil {
		errAcc = errors.Join(errAcc, err)
	}

	if ipif4 != nil {
		ipif4, err = r.luid.IPInterface(windows.AF_INET)
		if err != nil {
			return fmt.Errorf("getting AF_INET interface: %v", err)
		}
		if foundDefault4 {
			ipif4.UseAutomaticMetric = false
			ipif4.Metric = 0
		}
		ipif4.NLMTU = uint32(cfg.MTU)
		err = ipif4.Set()
		if err != nil {
			errAcc = errors.Join(errAcc, err)
		}
	}

	if ipif6 != nil {
		ipif6, err = r.luid.IPInterface(windows.AF_INET6)
		if err != nil {
			return fmt.Errorf("getting AF_INET6 interface: %v", err)
		}
		if foundDefault6 {
			ipif6.UseAutomaticMetric = false
			ipif6.Metric = 0
		}
		ipif6.NLMTU = uint32(cfg.MTU)
		ipif6.DadTransmits = 0
		ipif6.RouterDiscoveryBehavior = winipcfg.RouterDiscoveryDisabled
		err = ipif6.Set()
		if err != nil {
			errAcc = errors.Join(errAcc, err)
		}
	}

	return errAcc
}

func isIPv6LinkLocal(a netip.Prefix) bool {
	return a.Addr().Is6() && a.Addr().IsLinkLocalUnicast()
}

// ipAdapterUnicastAddressToPrefix converts windows IpAdapterUnicastAddress to netip.Prefix
func ipAdapterUnicastAddressToPrefix(u *windows.IpAdapterUnicastAddress) netip.Prefix {
	ip, _ := netip.AddrFromSlice(u.Address.IP())
	return netip.PrefixFrom(ip.Unmap(), int(u.OnLinkPrefixLength))
}

// unicastIPNets returns all unicast net.IPNet for ifc interface.
func unicastIPNets(ifc *winipcfg.IPAdapterAddresses) []netip.Prefix {
	var nets []netip.Prefix
	for addr := ifc.FirstUnicastAddress; addr != nil; addr = addr.Next {
		nets = append(nets, ipAdapterUnicastAddressToPrefix(addr))
	}
	return nets
}

func syncAddresses(ifc *winipcfg.IPAdapterAddresses, want []netip.Prefix) error {
	got := unicastIPNets(ifc)
	add, del := deltaNets(got, want)
	var erracc error
	ll := make([]netip.Prefix, 0)
	for _, a := range del {
		if isIPv6LinkLocal(a) {
			ll = append(ll, a)
			continue
		}
		err := ifc.LUID.DeleteIPAddress(a)
		if err != nil {
			erracc = errors.Join(erracc, fmt.Errorf("deleting IP %q: %v", a, err))
		}
	}
	for _, a := range add {
		err := ifc.LUID.AddIPAddress(a)
		if err != nil {
			erracc = errors.Join(erracc, fmt.Errorf("adding IP %q: %v", a, err))
		}
	}
	for _, a := range ll {
		mib, err := ifc.LUID.IPAddress(a.Addr())
		if err != nil {
			erracc = errors.Join(erracc, fmt.Errorf("setting skip-as-source on IP %q: unable to retrieve MIB: %v", a, err))
			continue
		}
		if !mib.SkipAsSource {
			mib.SkipAsSource = true
			if err := mib.Set(); err != nil {
				erracc = errors.Join(erracc, fmt.Errorf("setting skip-as-source on IP %q: unable to set MIB: %v", a, err))
			}
		}
	}
	return erracc
}

// routeData wraps winipcfg.RouteData with an additional field that permits
// caching of the associated MibIPForwardRow2; by keeping it around, we can
// avoid unnecessary lookups of information that we already have.
type routeData struct {
	winipcfg.RouteData
	Row *winipcfg.MibIPforwardRow2
}

func (rd *routeData) Compare(other *routeData) int {
	v := rd.Destination.Addr().Compare(other.Destination.Addr())
	if v != 0 {
		return v
	}
	b1, b2 := rd.Destination.Bits(), other.Destination.Bits()
	if b1 != b2 {
		if b1 > b2 {
			return -1
		}
		return 1
	}
	v = rd.NextHop.Compare(other.NextHop)
	if v != 0 {
		return v
	}
	if rd.Metric < other.Metric {
		return -1
	} else if rd.Metric > other.Metric {
		return 1
	}
	return 0
}

func deltaRouteData(a, b []*routeData) (add, del []*routeData) {
	add = make([]*routeData, 0, len(b))
	del = make([]*routeData, 0, len(a))
	slices.SortFunc(a, (*routeData).Compare)
	slices.SortFunc(b, (*routeData).Compare)

	i, j := 0, 0
	for i < len(a) && j < len(b) {
		switch a[i].Compare(b[j]) {
		case -1:
			del = append(del, a[i])
			i++
		case 0:
			i++
			j++
		case 1:
			add = append(add, b[j])
			j++
		}
	}
	del = append(del, a[i:]...)
	add = append(add, b[j:]...)
	return
}

// getInterfaceRoutes returns all the interface's routes.
func getInterfaceRoutes(ifc *winipcfg.IPAdapterAddresses, family winipcfg.AddressFamily) (matches []*winipcfg.MibIPforwardRow2, err error) {
	routes, err := winipcfg.GetIPForwardTable2(family)
	if err != nil {
		return nil, err
	}
	for i := range routes {
		if routes[i].InterfaceLUID == ifc.LUID {
			matches = append(matches, &routes[i])
		}
	}
	return
}

func getAllInterfaceRoutes(ifc *winipcfg.IPAdapterAddresses) ([]*routeData, error) {
	routes4, err := getInterfaceRoutes(ifc, windows.AF_INET)
	if err != nil {
		return nil, err
	}

	routes6, err := getInterfaceRoutes(ifc, windows.AF_INET6)
	if err != nil {
		// TODO: what if v6 unavailable?
		return nil, err
	}

	rd := make([]*routeData, 0, len(routes4)+len(routes6))
	for _, r := range routes4 {
		rd = append(rd, &routeData{
			RouteData: winipcfg.RouteData{
				Destination: r.DestinationPrefix.Prefix(),
				NextHop:     r.NextHop.Addr(),
				Metric:      r.Metric,
			},
			Row: r,
		})
	}
	for _, r := range routes6 {
		rd = append(rd, &routeData{
			RouteData: winipcfg.RouteData{
				Destination: r.DestinationPrefix.Prefix(),
				NextHop:     r.NextHop.Addr(),
				Metric:      r.Metric,
			},
			Row: r,
		})
	}
	return rd, nil
}

func filterRoutes(routes []*routeData, dontDelete []netip.Prefix) []*routeData {
	ddm := make(map[netip.Prefix]bool)
	for _, dd := range dontDelete {
		ddm[dd] = true
	}
	for _, r := range routes {
		nr := r.Destination
		if !nr.IsValid() {
			continue
		}
		if nr.IsSingleIP() {
			continue
		}
		lastIP := netipx.RangeOfPrefix(nr).To()
		ddm[netip.PrefixFrom(lastIP, lastIP.BitLen())] = true
	}
	filtered := make([]*routeData, 0, len(routes))
	for _, r := range routes {
		rr := r.Destination
		if rr.IsValid() && ddm[rr] {
			continue
		}
		filtered = append(filtered, r)
	}
	return filtered
}

// syncRoutes incrementally sets multiples routes on an interface.
// This avoids a full ifc.FlushRoutes call.
// dontDelete is a list of interface address routes that the
// synchronization logic should never delete.
func syncRoutes(ifc *winipcfg.IPAdapterAddresses, want []*routeData, dontDelete []netip.Prefix) error {
	existingRoutes, err := getAllInterfaceRoutes(ifc)
	if err != nil {
		return err
	}
	got := filterRoutes(existingRoutes, dontDelete)

	add, del := deltaRouteData(got, want)

	var errs []error
	for _, a := range del {
		var err error
		if a.Row == nil {
			// DeleteRoute requires a routing table lookup, so only do that if
			// a does not already have the row.
			err = ifc.LUID.DeleteRoute(a.Destination, a.NextHop)
		} else {
			// delete the row directly.
			err = a.Row.Delete()
		}
		if err != nil {
			dstStr := a.Destination.String()
			if dstStr == "169.254.255.255/32" {
				// Issue 785 (Tailscale). Ignore these routes
				// failing to delete. Harmless.
				continue
			}
			errs = append(errs, fmt.Errorf("deleting route %v: %v", dstStr, err))
		}
	}

	for _, a := range add {
		err := ifc.LUID.AddRoute(a.Destination, a.NextHop, a.Metric)
		if err != nil {
			errs = append(errs, fmt.Errorf("adding route %v: %v", &a.Destination, err))
		}
	}

	return errors.Join(errs...)
}

// deltaNets returns the changes to turn a into b.
func deltaNets(a, b []netip.Prefix) (add, del []netip.Prefix) {
	add = make([]netip.Prefix, 0, len(b))
	del = make([]netip.Prefix, 0, len(a))
	sortNets(a)
	sortNets(b)

	i, j := 0, 0
	for i < len(a) && j < len(b) {
		switch netCompare(a[i], b[j]) {
		case -1:
			del = append(del, a[i])
			i++
		case 0:
			i++
			j++
		case 1:
			add = append(add, b[j])
			j++
		default:
			panic("unexpected compare result")
		}
	}
	del = append(del, a[i:]...)
	add = append(add, b[j:]...)
	return
}

func netCompare(a, b netip.Prefix) int {
	aip, bip := a.Addr().Unmap(), b.Addr().Unmap()
	v := aip.Compare(bip)
	if v != 0 {
		return v
	}
	if a.Bits() == b.Bits() {
		return 0
	}
	if a.Bits() > b.Bits() {
		return -1
	}
	return 1
}

func sortNets(s []netip.Prefix) {
	sort.Slice(s, func(i, j int) bool {
		return netCompare(s[i], s[j]) < 0
	})
}

func (r *windowsRouter) getGlobalSearchDomains() ([]string, error) {
	cmd := exec.Command("powershell", "-Command", "(Get-DnsClientGlobalSetting).SuffixSearchList")
	output, err := cmd.Output()
	if err != nil {
		return nil, err
	}
	lines := strings.Split(strings.TrimSpace(string(output)), "\r\n")
	var domains []string
	for _, line := range lines {
		trimmed := strings.TrimSpace(line)
		if trimmed != "" {
			domains = append(domains, trimmed)
		}
	}
	return domains, nil
}

func (r *windowsRouter) setPrivateNetwork() (bool, error) {
	alias := r.iface

	// Check if visible and get current category
	cmd := exec.Command("powershell", "-Command", fmt.Sprintf(`Get-NetConnectionProfile -InterfaceAlias "%s" | Select-Object -ExpandProperty NetworkCategory`, alias))
	output, err := cmd.CombinedOutput()
	if err != nil {
		r.logger.Verbosef("setPrivateNetwork: Get-NetConnectionProfile failed: %v", err)
		return false, err
	}

	category := strings.TrimSpace(string(output))
	if category == "" {
		r.logger.Verbosef("setPrivateNetwork: Adapter not found")
		return false, nil
	}

	if category == "Private" || category == "DomainAuthenticated" {
		r.logger.Verbosef("setPrivateNetwork: Already private/domain, skipping")
		return true, nil
	}

	// Set to Private
	cmd = exec.Command("powershell", "-Command", fmt.Sprintf(`Set-NetConnectionProfile -InterfaceAlias "%s" -NetworkCategory Private`, alias))
	output, err = cmd.CombinedOutput()
	if err != nil {
		r.logger.Errorf("setPrivateNetwork: Set-NetConnectionProfile failed: %v (output: %s)", err, output)
		return false, err
	}

	r.logger.Verbosef("setPrivateNetwork: Success")
	return true, nil
}

// interfaceFromLUID returns IPAdapterAddresses with specified LUID.
func interfaceFromLUID(luid winipcfg.LUID, flags winipcfg.GAAFlags) (*winipcfg.IPAdapterAddresses, error) {
	addresses, err := winipcfg.GetAdaptersAddresses(windows.AF_UNSPEC, flags)
	if err != nil {
		return nil, err
	}
	for _, addr := range addresses {
		if addr.LUID == luid {
			return addr, nil
		}
	}
	return nil, fmt.Errorf("interfaceFromLUID: interface with LUID %v not found", luid)
}

// Flush clears the local resolver cache.
// Only Windows has a public dns.Flush, needed in router_windows.go. Other
// platforms like Linux need a different flush implementation depending on
// the DNS manager. There is a FlushCaches method on the manager which
// can be used on all platforms.
func flushCaches() error {
	cmd := exec.Command("ipconfig", "/flushdns")
	cmd.SysProcAttr = &syscall.SysProcAttr{
		CreationFlags: windows.DETACHED_PROCESS,
	}
	out, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("%v (output: %s)", err, out)
	}
	return nil
}
